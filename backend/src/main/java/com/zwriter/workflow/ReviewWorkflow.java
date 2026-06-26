package com.zwriter.workflow;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.review.ReviewAgent;
import com.zwriter.workspace.Workspace;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 质量审计工作流
 * 调用 ReviewAgent 对章节进行逻辑检查、角色一致性、文风统一、智商门禁
 */
@Slf4j
@Component
public class ReviewWorkflow {

    @Autowired
    private ReviewAgent reviewAgent;

    @Autowired
    private WorkspaceManager workspaceManager;

    /**
     * 审计指定章节
     */
    public WorkflowResult reviewChapter(String novelName, int chapterNum, String reviewType) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);
        String content = workspace.readChapter(chapterNum);

        if (content == null || content.isBlank()) {
            return new WorkflowResult(false, "第 " + chapterNum + " 章内容为空", null, null);
        }

        log.info("[审计工作流] 小说: {}, 第 {} 章, 类型: {}", novelName, chapterNum, reviewType);

        String subTask = switch (reviewType) {
            case "logic" -> "logic_check";
            case "character" -> "character_consistency";
            case "style" -> "style_audit";
            case "iq" -> "iq_gate";
            default -> "logic_check";
        };

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("subTask", subTask);
        params.put("chapterContent", content);

        AgentContext ctx = new AgentContext(
                workspace.getRoot(),
                "请对第 " + chapterNum + " 章进行" + getReviewTypeName(subTask) + "。",
                null,
                params
        );

        AgentResult result = reviewAgent.execute(ctx);

        if (!result.isSuccess()) {
            return new WorkflowResult(false, "审计失败: " + result.getErrorMessage(), null, null);
        }

        log.info("[审计工作流] 完成，审计类型: {}", reviewType);

        return new WorkflowResult(true, result.getContent(), null, Map.of(
                "chapterNum", chapterNum,
                "reviewType", reviewType,
                "contentLength", content.length()
        ));
    }

    /**
     * 全量审计（逻辑 + 角色 + 文风 + 智商）
     */
    public WorkflowResult fullReview(String novelName, int chapterNum) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);
        String content = workspace.readChapter(chapterNum);

        if (content == null || content.isBlank()) {
            return new WorkflowResult(false, "第 " + chapterNum + " 章内容为空", null, null);
        }

        log.info("[审计工作流] 全量审计: {}, 第 {} 章", novelName, chapterNum);

        StringBuilder fullReport = new StringBuilder();
        String[] reviewTypes = {"logic_check", "character_consistency", "style_audit", "iq_gate"};
        String[] reviewNames = {"逻辑检查", "角色一致性", "文风统一", "智商门禁"};

        for (int i = 0; i < reviewTypes.length; i++) {
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("subTask", reviewTypes[i]);
            params.put("chapterContent", content);

            AgentContext ctx = new AgentContext(
                    workspace.getRoot(),
                    "请对第 " + chapterNum + " 章进行" + reviewNames[i] + "。",
                    null,
                    params
            );

            AgentResult result = reviewAgent.execute(ctx);
            if (result.isSuccess()) {
                fullReport.append("## ").append(reviewNames[i]).append("\n\n");
                fullReport.append(result.getContent()).append("\n\n---\n\n");
            }
        }

        return new WorkflowResult(true, fullReport.toString(), null, Map.of(
                "chapterNum", chapterNum,
                "reviewType", "full",
                "contentLength", content.length()
        ));
    }

    private String getReviewTypeName(String subTask) {
        return switch (subTask) {
            case "logic_check" -> "逻辑检查";
            case "character_consistency" -> "角色一致性检查";
            case "style_audit" -> "文风统一审计";
            case "iq_gate" -> "智商门禁检查";
            default -> "质量审计";
        };
    }

    public record WorkflowResult(
            boolean success,
            String content,
            String errorMessage,
            Map<String, Object> metadata
    ) {}
}
