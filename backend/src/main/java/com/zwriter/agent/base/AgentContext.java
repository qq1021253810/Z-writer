package com.zwriter.agent.base;

import java.nio.file.Path;
import java.util.Map;

/**
 AgentContext 是 Agent 执行的新上下文对象
 替代旧的 AgentInput，提供更简洁的 API
 */
public record AgentContext(
    String taskId,
    String taskType,
    String userInput,
    String systemPrompt,
    String workspacePath,
    Map<String, Object> params
) {
    public AgentContext(Path workspacePath, String userInput, String systemPrompt, Map<String, Object> params) {
        this(null, null, userInput, systemPrompt, workspacePath.toString(), params);
    }

    /**
     * 从 AgentInput 转换为 AgentContext
     */
    public static AgentContext fromAgentInput(AgentInput input) {
        return new AgentContext(
            input.getNovelId() != null ? input.getNovelId().toString() : null,
            input.getTaskType(),
            input.getUserInput(),
            null,
            input.getNovelId() != null ? input.getNovelId().toString() : null,
            input.getParams()
        );
    }
}
