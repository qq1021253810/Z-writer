package com.zwriter.workflow;

import com.zwriter.entity.ChapterContent;
import com.zwriter.entity.NovelInfo;
import com.zwriter.repository.ChapterContentRepository;
import com.zwriter.repository.NovelInfoRepository;
import com.zwriter.service.ContextCompressionService;
import com.zwriter.service.ContextService;
import com.zwriter.tool.BannedWordTool;
import com.zwriter.tool.WordCountTool;
import com.zwriter.workflow.base.BaseWorkflow;
import com.zwriter.workflow.base.BaseWorkflowResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 续写章节工作流
 * 流程：检索上下文→节奏设计→分镜→生成→润色→合规→存储向量
 */
@Slf4j
@Service
public class ContinueChapterWorkflow extends BaseWorkflow<
        ContinueChapterWorkflow.ContinueChapterRequest,
        ContinueChapterWorkflow.WorkflowResult> {

    @Autowired
    private NovelInfoRepository novelInfoRepository;

    @Autowired
    private ChapterContentRepository chapterContentRepository;

    @Autowired
    private ContextCompressionService contextCompressionService;

    @Autowired
    private ContextService contextService;

    @Override
    protected WorkflowResult doExecute(ContinueChapterRequest request) throws Exception {
        Long novelId = request.getNovelId();

        // 1. 检索上下文（使用向量检索增强）
        String query = request.getChapterOutline() != null
                ? request.getChapterOutline()
                : "续写下一章内容";
        String context = contextService.getChapterContextWithVectorSearch(
                novelId, request.getVolumeNumber(), request.getChapterNumber(), query);

        // 2. 获取小说基础信息
        NovelInfo novel = novelInfoRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("小说不存在"));

        // 3. 节奏设计（爽点规划）
        String rhythmDesign = designRhythm(novelId, context, novel.getGenre());

        // 4. 章节分镜
        String storyboard = generateStoryboard(novelId, context, rhythmDesign, request.getChapterOutline());

        // 5. 正文生成
        String chapterContent = generateChapter(novelId, storyboard, novel.getGenre(), request.getWordCount());

        // 6. 润色
        String polishedContent = polishChapter(novelId, chapterContent);

        // 7. 合规检查
        String complianceResult = checkCompliance(novelId, polishedContent);

        // 8. 保存章节
        ChapterContent chapter = saveChapter(novelId, request.getVolumeNumber(),
                request.getChapterNumber(), request.getChapterTitle(), polishedContent);

        // 9. 存储到向量库（用于后续检索）
        contextService.storeChapterToVector(novelId, request.getChapterNumber(), polishedContent);

        // 构建结果
        WorkflowResultBuilder builder = new WorkflowResultBuilder();
        builder.success(true);
        performContentChecks(polishedContent, builder);
        builder.chapterId(chapter.getId())
               .content(polishedContent)
               .contextUsed(context)
               .rhythmDesign(rhythmDesign)
               .storyboard(storyboard)
               .complianceResult(complianceResult);

        return builder.build();
    }

    @Override
    protected String getWorkflowName() {
        return "续写章节工作流";
    }

    @Override
    protected String formatRequestInfo(ContinueChapterRequest request) {
        return String.format("novelId=%d, volume=%d, chapter=%d",
                request.getNovelId(), request.getVolumeNumber(), request.getChapterNumber());
    }

    @Override
    protected WorkflowResult createFailureResult(String errorMessage, long duration) {
        WorkflowResultBuilder builder = new WorkflowResultBuilder();
        builder.success(false).errorMessage(errorMessage).durationMs(duration);
        return builder.build();
    }

    // ==================== 业务步骤 ====================

    private String designRhythm(Long novelId, String context, String genre) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "rhythm");
        params.put("outline", context);
        return callAgent(novelId, "plot", params, "节奏设计失败");
    }

    private String generateStoryboard(Long novelId, String context, String rhythmDesign, String chapterOutline) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "storyboard");
        params.put("chapterOutline", chapterOutline != null ? chapterOutline : context);
        return callAgent(novelId, "writing", params, "分镜生成失败");
    }

    private String generateChapter(Long novelId, String storyboard, String genre, Integer wordCount) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "chapter");
        params.put("storyboard", storyboard);
        params.put("writingStyle", genre);
        params.put("wordCount", wordCount != null ? wordCount.toString() : "3000");
        return callAgent(novelId, "writing", params, "正文生成失败");
    }

    private String polishChapter(Long novelId, String content) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "polish");
        params.put("chapterContent", content);
        String result = callAgent(novelId, "polish", params, null);
        return result != null ? result : content;
    }

    private String checkCompliance(Long novelId, String content) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "check");
        params.put("chapterContent", content);
        return callAgent(novelId, "compliance", params, "合规检查失败");
    }

    private ChapterContent saveChapter(Long novelId, Integer volumeNumber,
                                       Integer chapterNumber, String title, String content) {
        ChapterContent chapter = new ChapterContent();
        chapter.setNovelId(novelId);
        chapter.setVolumeNumber(volumeNumber);
        chapter.setChapterNumber(chapterNumber);
        chapter.setTitle(title != null ? title : "第" + chapterNumber + "章");
        chapter.setContent(content);
        chapter.setWordCount(content.length());
        return chapterContentRepository.save(chapter);
    }

    // ==================== 请求参数 ====================

    public static class ContinueChapterRequest {
        private Long novelId;
        private Integer volumeNumber;
        private Integer chapterNumber;
        private String chapterTitle;
        private String chapterOutline;
        private Integer wordCount = 3000;

        public Long getNovelId() { return novelId; }
        public void setNovelId(Long novelId) { this.novelId = novelId; }

        public Integer getVolumeNumber() { return volumeNumber; }
        public void setVolumeNumber(Integer volumeNumber) { this.volumeNumber = volumeNumber; }

        public Integer getChapterNumber() { return chapterNumber; }
        public void setChapterNumber(Integer chapterNumber) { this.chapterNumber = chapterNumber; }

        public String getChapterTitle() { return chapterTitle; }
        public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }

        public String getChapterOutline() { return chapterOutline; }
        public void setChapterOutline(String chapterOutline) { this.chapterOutline = chapterOutline; }

        public Integer getWordCount() { return wordCount; }
        public void setWordCount(Integer wordCount) { this.wordCount = wordCount; }
    }

    // ==================== 工作流结果 ====================

    public static class WorkflowResult extends BaseWorkflowResult {
        private Long chapterId;
        private String content;
        private String contextUsed;
        private String rhythmDesign;
        private String storyboard;
        private String complianceResult;

        public Long getChapterId() { return chapterId; }
        public void setChapterId(Long chapterId) { this.chapterId = chapterId; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getContextUsed() { return contextUsed; }
        public void setContextUsed(String contextUsed) { this.contextUsed = contextUsed; }

        public String getRhythmDesign() { return rhythmDesign; }
        public void setRhythmDesign(String rhythmDesign) { this.rhythmDesign = rhythmDesign; }

        public String getStoryboard() { return storyboard; }
        public void setStoryboard(String storyboard) { this.storyboard = storyboard; }

        public String getComplianceResult() { return complianceResult; }
        public void setComplianceResult(String complianceResult) { this.complianceResult = complianceResult; }

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

        public WorkflowResultBuilder chapterId(Long chapterId) {
            result.setChapterId(chapterId);
            return self();
        }

        public WorkflowResultBuilder content(String content) {
            result.setContent(content);
            return self();
        }

        public WorkflowResultBuilder contextUsed(String contextUsed) {
            result.setContextUsed(contextUsed);
            return self();
        }

        public WorkflowResultBuilder rhythmDesign(String rhythmDesign) {
            result.setRhythmDesign(rhythmDesign);
            return self();
        }

        public WorkflowResultBuilder storyboard(String storyboard) {
            result.setStoryboard(storyboard);
            return self();
        }

        public WorkflowResultBuilder complianceResult(String complianceResult) {
            result.setComplianceResult(complianceResult);
            return self();
        }

        @Override
        public WorkflowResult build() {
            applyTo(result);
            return result;
        }
    }
}
