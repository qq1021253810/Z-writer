package com.zwriter.agent.base;

import com.zwriter.llm.LlmService;
import com.zwriter.service.ContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.function.Function;

/**
 * Agent 基类
 * 提供通用的 LLM 调用、上下文管理能力和统一的执行流程
 */
@Slf4j
public abstract class BaseAgent {

    @Autowired
    protected LlmService llmService;

    @Autowired
    protected ContextService contextService;

    /**
     * 获取 Agent 名称
     */
    public abstract String getName();

    /**
     * 模板方法：统一执行流程
     * 包含计时、日志、异常处理
     *
     * @param input 输入参数
     * @return 执行结果
     */
    public final AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", getName(), input.getTaskType());

        try {
            AgentResult result = doExecute(input);
            result.setDurationMs(System.currentTimeMillis() - startTime);
            log.info("[{}] 执行完成，耗时: {}ms", getName(), result.getDurationMs());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] 执行失败，耗时: {}ms", getName(), duration, e);
            return AgentResult.failure(e.getMessage());
        }
    }

    /**
     * 子类实现具体业务逻辑
     *
     * @param input 输入参数
     * @return 执行结果
     * @throws Exception 执行过程中可能抛出的异常
     */
    protected abstract AgentResult doExecute(AgentInput input) throws Exception;

    /**
     * 构建系统提示词
     */
    protected abstract String buildSystemPrompt();

    /**
     * 调用 LLM
     */
    protected String callLlm(String userPrompt, String systemPrompt) {
        log.info("[{}] 调用 LLM, 用户输入长度: {}", getName(), userPrompt.length());
        return llmService.chat(userPrompt, systemPrompt);
    }

    /**
     * 调用 LLM（使用默认系统提示词）
     */
    protected String callLlm(String userPrompt) {
        return callLlm(userPrompt, buildSystemPrompt());
    }

    /**
     * 子任务路由辅助方法
     * 使用 Map 策略模式替代 switch-case
     *
     * @param subTask        子任务类型
     * @param handlers       子任务处理器映射
     * @param defaultResult  默认结果（当子任务不存在时返回）
     * @return 处理结果
     */
    protected <T> T routeSubTask(String subTask, Map<String, Function<AgentInput, T>> handlers, T defaultResult) {
        return handlers.getOrDefault(subTask, i -> defaultResult).apply(null);
    }

    /**
     * 获取子任务参数，默认值为 "profile"
     */
    protected String getSubTask(AgentInput input) {
        return getSubTask(input, "profile");
    }

    /**
     * 获取子任务参数
     *
     * @param input       输入参数
     * @param defaultSubTask 默认子任务
     * @return 子任务名称
     */
    protected String getSubTask(AgentInput input, String defaultSubTask) {
        if (input.getParams() == null) {
            return defaultSubTask;
        }
        Object subTask = input.getParams().get("subTask");
        return subTask != null ? subTask.toString() : defaultSubTask;
    }
}