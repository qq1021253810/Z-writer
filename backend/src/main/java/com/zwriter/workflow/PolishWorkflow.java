package com.zwriter.workflow;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.polish.PolishAgent;
import com.zwriter.workspace.Workspace;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 润色工作流
 * 调用 PolishAgent 对章节进行文风校准、语言润色、章节衔接优化
 */
@Slf4j
@Component
public class PolishWorkflow {

    @Autowired
    private PolishAgent polishAgent;

    @Autowired
    private WorkspaceManager workspaceManager;

    /**
     * 润色指定章节
     */
    public WorkflowResult polishChapter(String novelName, int chapterNum, String polishType) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);
        String content = workspace.readChapter(chapterNum);

        if (content == null || content.isBlank()) {
            return new WorkflowResult(false, "第 " + chapterNum + " 章内容为空", null, null);
        }

        log.info("[润色工作流] 小说: {}, 第 {} 章, 类型: {}", novelName, chapterNum, polishType);

        String subTask = switch (polishType) {
            case "style" -> "style";
            case "transition" -> "transition";
            default -> "polish";
        };

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("subTask", subTask);
        params.put("chapterContent", content);

        // 衔接模式需要上一章内容
        if ("transition".equals(subTask) && chapterNum > 1) {
            String prevContent = workspace.readChapter(chapterNum - 1);
            if (prevContent != null) {
                params.put("previousChapter", prevContent.length() > 500
                        ? prevContent.substring(prevContent.length() - 500) : prevContent);
            }
        }

        AgentContext ctx = new AgentContext(
                workspace.getRoot(),
                "请对以下章节进行" + getPolishTypeName(subTask) + "。",
                null,
                params
        );

        AgentResult result = polishAgent.execute(ctx);

        if (!result.isSuccess()) {
            return new WorkflowResult(false, "润色失败: " + result.getErrorMessage(), null, null);
        }

        log.info("[润色工作流] 完成，字数: {} -> {}", content.length(), result.getContent().length());

        return new WorkflowResult(true, result.getContent(), null, Map.of(
                "chapterNum", chapterNum,
                "polishType", polishType,
                "originalLength", content.length(),
                "polishedLength", result.getContent().length()
        ));
    }

    private String getPolishTypeName(String subTask) {
        return switch (subTask) {
            case "style" -> "文风校准";
            case "transition" -> "章节衔接优化";
            default -> "语言润色";
        };
    }

    public record WorkflowResult(
            boolean success,
            String content,
            String errorMessage,
            Map<String, Object> metadata
    ) {}
}
