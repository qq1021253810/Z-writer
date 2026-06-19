package com.zwriter.workflow;

import com.zwriter.tool.BannedWordTool;
import com.zwriter.tool.WordCountTool;
import com.zwriter.workflow.base.BaseWorkflow;
import com.zwriter.workflow.base.BaseWorkflowResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 卡文修复工作流
 * 流程：描述卡点→分析原因→生成方案→改写段落
 */
@Slf4j
@Service
public class FixWriterBlockWorkflow extends BaseWorkflow<
        FixWriterBlockWorkflow.FixWriterBlockRequest,
        FixWriterBlockWorkflow.WorkflowResult> {

    @Override
    protected WorkflowResult doExecute(FixWriterBlockRequest request) throws Exception {
        Long novelId = request.getNovelId();

        // 1. 分析卡点原因
        String analysis = analyzeBlockPoint(novelId, request.getBlockDescription(),
                request.getPreviousContent(), request.getExpectedDirection());

        // 2. 生成修复方案（多条路径）
        String solutions = generateSolutions(novelId, analysis, request.getBlockType());

        // 3. 改写段落
        String rewrittenContent = rewriteContent(novelId, request.getPreviousContent(),
                solutions, request.getSelectedSolution());

        // 4. 润色优化
        String polishedContent = polishContent(novelId, rewrittenContent);

        // 构建结果
        WorkflowResultBuilder builder = new WorkflowResultBuilder();
        builder.success(true);
        performContentChecks(polishedContent, builder);
        builder.analysis(analysis)
               .solutions(solutions)
               .rewrittenContent(rewrittenContent)
               .polishedContent(polishedContent);

        return builder.build();
    }

    @Override
    protected String getWorkflowName() {
        return "卡文修复工作流";
    }

    @Override
    protected String formatRequestInfo(FixWriterBlockRequest request) {
        return String.format("novelId=%d", request.getNovelId());
    }

    @Override
    protected WorkflowResult createFailureResult(String errorMessage, long duration) {
        WorkflowResultBuilder builder = new WorkflowResultBuilder();
        builder.success(false).errorMessage(errorMessage).durationMs(duration);
        return builder.build();
    }

    // ==================== 业务步骤 ====================

    private String analyzeBlockPoint(Long novelId, String blockDescription,
                                     String previousContent, String expectedDirection) {
        String prompt = String.format("""
                请分析以下卡文问题：
                
                卡文描述: %s
                前文内容: %s
                期望方向: %s
                
                请分析：
                1. 卡点的根本原因（逻辑断裂？节奏问题？角色动机不清？）
                2. 前文埋下的伏笔或限制
                3. 可能的突破方向
                """, blockDescription, previousContent, expectedDirection);

        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "analysis");
        params.put("prompt", prompt);
        return callAgent(novelId, "plot", params, "分析失败");
    }

    private String generateSolutions(Long novelId, String analysis, String blockType) {
        String prompt = String.format("""
                基于以下卡文分析，生成 3 个修复方案：
                
                卡文分析:
                %s
                
                卡文类型: %s
                
                每个方案需要包含：
                1. 方案名称
                2. 具体情节走向
                3. 优缺点分析
                4. 对后续剧情的影响
                """, analysis, blockType != null ? blockType : "通用");

        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "solutions");
        params.put("prompt", prompt);
        return callAgent(novelId, "plot", params, "方案生成失败");
    }

    private String rewriteContent(Long novelId, String previousContent,
                                  String solutions, String selectedSolution) {
        String prompt = String.format("""
                请基于以下信息改写卡文段落：
                
                前文内容:
                %s
                
                可选方案:
                %s
                
                选定方案: %s
                
                要求：
                1. 与前文自然衔接
                2. 解决卡点问题
                3. 保持文风一致
                4. 约 500-1000 字
                """, previousContent, solutions, selectedSolution != null ? selectedSolution : "方案一");

        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "rewrite");
        params.put("prompt", prompt);
        return callAgent(novelId, "writing", params, "改写失败");
    }

    private String polishContent(Long novelId, String content) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "polish");
        params.put("chapterContent", content);
        String result = callAgent(novelId, "polish", params, null);
        return result != null ? result : content;
    }

    // ==================== 请求参数 ====================

    public static class FixWriterBlockRequest {
        private Long novelId;
        private String blockDescription;
        private String previousContent;
        private String expectedDirection;
        private String blockType;
        private String selectedSolution;

        public Long getNovelId() { return novelId; }
        public void setNovelId(Long novelId) { this.novelId = novelId; }

        public String getBlockDescription() { return blockDescription; }
        public void setBlockDescription(String blockDescription) { this.blockDescription = blockDescription; }

        public String getPreviousContent() { return previousContent; }
        public void setPreviousContent(String previousContent) { this.previousContent = previousContent; }

        public String getExpectedDirection() { return expectedDirection; }
        public void setExpectedDirection(String expectedDirection) { this.expectedDirection = expectedDirection; }

        public String getBlockType() { return blockType; }
        public void setBlockType(String blockType) { this.blockType = blockType; }

        public String getSelectedSolution() { return selectedSolution; }
        public void setSelectedSolution(String selectedSolution) { this.selectedSolution = selectedSolution; }
    }

    // ==================== 工作流结果 ====================

    public static class WorkflowResult extends BaseWorkflowResult {
        private String analysis;
        private String solutions;
        private String rewrittenContent;
        private String polishedContent;

        public String getAnalysis() { return analysis; }
        public void setAnalysis(String analysis) { this.analysis = analysis; }

        public String getSolutions() { return solutions; }
        public void setSolutions(String solutions) { this.solutions = solutions; }

        public String getRewrittenContent() { return rewrittenContent; }
        public void setRewrittenContent(String rewrittenContent) { this.rewrittenContent = rewrittenContent; }

        public String getPolishedContent() { return polishedContent; }
        public void setPolishedContent(String polishedContent) { this.polishedContent = polishedContent; }

        /**
         * 兼容旧接口：从wordCountResult获取总字数
         */
        public Integer getWordCount() {
            return getWordCountResult() != null ? getWordCountResult().totalWords() : null;
        }

        /**
         * 兼容旧接口：从wordCountResult获取字数详情Map
         */
        public Map<String, Object> getWordCountDetail() {
            return getWordCountResult() != null ? getWordCountResult().toMap() : null;
        }
    }

    // ==================== 扩展Builder ====================

    private static class WorkflowResultBuilder extends BaseWorkflowResult.Builder<WorkflowResultBuilder> {
        private final WorkflowResult result = new WorkflowResult();

        public WorkflowResultBuilder analysis(String analysis) {
            result.setAnalysis(analysis);
            return self();
        }

        public WorkflowResultBuilder solutions(String solutions) {
            result.setSolutions(solutions);
            return self();
        }

        public WorkflowResultBuilder rewrittenContent(String rewrittenContent) {
            result.setRewrittenContent(rewrittenContent);
            return self();
        }

        public WorkflowResultBuilder polishedContent(String polishedContent) {
            result.setPolishedContent(polishedContent);
            return self();
        }

        @Override
        public WorkflowResult build() {
            applyTo(result);
            return result;
        }
    }
}
