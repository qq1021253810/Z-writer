package com.zwriter.workflow;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.strategy.StrategyAgent;
import com.zwriter.workspace.Workspace;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

/**
 * 战略规划工作流
 * 调用 StrategyAgent 进行总体战略规划、多线叙事编织、主题深化、长线布局
 */
@Slf4j
@Component
public class StrategyWorkflow {

    @Autowired
    private StrategyAgent strategyAgent;

    @Autowired
    private WorkspaceManager workspaceManager;

    /**
     * 执行战略规划任务
     */
    public WorkflowResult executeStrategy(String novelName, String strategyType, Map<String, Object> extraParams) throws IOException {
        Workspace workspace = workspaceManager.openNovel(novelName);

        log.info("[战略工作流] 小说: {}, 类型: {}", novelName, strategyType);

        String subTask = switch (strategyType) {
            case "master_plan" -> "master_plan";
            case "thread_weave" -> "thread_weave";
            case "theme_deepen" -> "theme_deepen";
            case "long_game" -> "long_game";
            default -> "master_plan";
        };

        // 构建上下文
        String fullContext = workspace.buildFullContext(5);

        Map<String, Object> params = new java.util.HashMap<>();
        params.put("subTask", subTask);
        params.put("novelContext", fullContext);
        if (extraParams != null) {
            params.putAll(extraParams);
        }

        AgentContext ctx = new AgentContext(
                workspace.getRoot(),
                "请对小说进行" + getStrategyTypeName(subTask) + "。\n\n当前上下文:\n" + fullContext,
                null,
                params
        );

        AgentResult result = strategyAgent.execute(ctx);

        if (!result.isSuccess()) {
            return new WorkflowResult(false, "战略规划失败: " + result.getErrorMessage(), null, null);
        }

        log.info("[战略工作流] 完成，类型: {}", strategyType);

        return new WorkflowResult(true, result.getContent(), null, Map.of(
                "strategyType", strategyType,
                "novelName", novelName
        ));
    }

    private String getStrategyTypeName(String subTask) {
        return switch (subTask) {
            case "master_plan" -> "总体战略规划";
            case "thread_weave" -> "多线叙事编织";
            case "theme_deepen" -> "主题深化";
            case "long_game" -> "长线布局";
            default -> "战略规划";
        };
    }

    public record WorkflowResult(
            boolean success,
            String content,
            String errorMessage,
            Map<String, Object> metadata
    ) {}
}
