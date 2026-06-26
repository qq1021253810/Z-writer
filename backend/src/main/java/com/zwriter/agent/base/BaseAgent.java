package com.zwriter.agent.base;

import com.zwriter.llm.LlmService;
import com.zwriter.service.ContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent 基类
 * 提供通用的 LLM 调用、上下文管理能力和统一的执行流程
 * 
 * 子类通过 registerSubTask() 注册子任务处理器，只需实现一份业务逻辑，
 * BaseAgent 自动适配 AgentInput 和 AgentContext 两种调用方式。
 */
@Slf4j
public abstract class BaseAgent {

    @Autowired
    protected LlmService llmService;

    @Autowired
    protected ContextService contextService;

    /**
     * 子任务处理器函数式接口
     */
    @FunctionalInterface
    protected interface SubTaskHandler {
        AgentResult handle(Map<String, Object> params, String userInput) throws Exception;
    }

    /**
     * 子任务处理器注册表
     */
    private final Map<String, SubTaskHandler> subTaskHandlers = new HashMap<>();

    // ==================== 执行入口 ====================

    /**
     * 执行入口（AgentContext）
     */
    public AgentResult execute(AgentContext ctx) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", name(), ctx.taskType());

        try {
            AgentResult result = dispatch(
                    ctx.params() != null ? ctx.params() : Map.of(),
                    ctx.userInput());
            result.setDurationMs(System.currentTimeMillis() - startTime);
            log.info("[{}] 执行完成，耗时: {}ms", name(), result.getDurationMs());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] 执行失败，耗时: {}ms", name(), duration, e);
            return AgentResult.failure(e.getMessage());
        }
    }

    /**
     * 执行入口（AgentInput，向后兼容）
     */
    public AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", name(), input.getTaskType());

        try {
            AgentResult result = dispatch(
                    input.getParams() != null ? input.getParams() : Map.of(),
                    input.getUserInput());
            result.setDurationMs(System.currentTimeMillis() - startTime);
            log.info("[{}] 执行完成，耗时: {}ms", name(), result.getDurationMs());
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] 执行失败，耗时: {}ms", name(), duration, e);
            return AgentResult.failure(e.getMessage());
        }
    }

    // ==================== 子类扩展点 ====================

    /**
     * 获取 Agent 名称
     */
    public abstract String name();

    /**
     * 获取 Agent 名称（兼容旧调用）
     */
    public String getName() {
        return name();
    }

    /**
     * 构建系统提示词
     */
    protected abstract String buildSystemPrompt();

    /**
     * 默认执行逻辑（当子任务未注册时调用）
     * 子类可重写以实现不基于子任务路由的逻辑
     */
    protected AgentResult doExecute(Map<String, Object> params, String userInput) throws Exception {
        return AgentResult.failure("未注册的子任务或无默认实现");
    }

    // ==================== 子任务注册与分发 ====================

    /**
     * 注册子任务处理器（子类在构造函数中调用）
     */
    protected void registerSubTask(String subTask, SubTaskHandler handler) {
        subTaskHandlers.put(subTask, handler);
    }

    /**
     * 分发到注册的子任务处理器
     */
    private AgentResult dispatch(Map<String, Object> params, String userInput) throws Exception {
        String subTask = getSubTask(params, defaultSubTask());
        SubTaskHandler handler = subTaskHandlers.get(subTask);

        if (handler != null) {
            return handler.handle(params, userInput);
        }
        return doExecute(params, userInput);
    }

    /**
     * 默认子任务名称（子类可重写）
     */
    protected String defaultSubTask() {
        return "profile";
    }

    // ==================== 参数辅助方法 ====================

    /**
     * 从 params Map 中获取子任务名称
     */
    protected String getSubTask(Map<String, Object> params, String defaultSubTask) {
        if (params == null) {
            return defaultSubTask;
        }
        Object subTask = params.get("subTask");
        return subTask != null ? subTask.toString() : defaultSubTask;
    }

    /**
     * 从 params Map 中获取参数，默认值为空字符串
     */
    protected String getParam(Map<String, Object> params, String key) {
        return getParam(params, key, "");
    }

    /**
     * 从 params Map 中获取参数
     */
    protected String getParam(Map<String, Object> params, String key, String defaultValue) {
        if (params == null) {
            return defaultValue;
        }
        Object value = params.get(key);
        return value != null ? value.toString() : defaultValue;
    }

    // ==================== LLM 调用 ====================

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

    // ==================== 流式执行 ====================

    /**
     * 流式执行 Agent 任务
     * 通过 SseEmitter 实时推送 LLM 响应
     */
    public void executeStream(AgentContext ctx, SseEmitter emitter) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始流式执行", name());

        StringBuilder fullContent = new StringBuilder();

        try {
            llmService.chatStream(ctx.userInput(), ctx.systemPrompt() != null ? ctx.systemPrompt() : buildSystemPrompt())
                    .doOnNext(chunk -> {
                        fullContent.append(chunk);
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("chunk")
                                    .data(chunk));
                        } catch (Exception e) {
                            log.warn("[{}] SSE 发送失败: {}", name(), e.getMessage());
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("complete")
                                    .data(Map.of(
                                            "content", fullContent.toString(),
                                            "durationMs", System.currentTimeMillis() - startTime
                                    )));
                            emitter.complete();
                            log.info("[{}] 流式执行完成，耗时: {}ms", name(), System.currentTimeMillis() - startTime);
                        } catch (Exception e) {
                            log.warn("[{}] SSE 完成发送失败: {}", name(), e.getMessage());
                        }
                    })
                    .doOnError(e -> {
                        try {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data(e.getMessage()));
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            log.warn("[{}] SSE 错误发送失败: {}", name(), ex.getMessage());
                        }
                        log.error("[{}] 流式执行失败", name(), e);
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("[{}] 流式执行初始化失败", name(), e);
            try {
                emitter.completeWithError(e);
            } catch (Exception ex) {
                // ignore
            }
        }
    }

    /**
     * 获取系统提示词
     */
    public String systemPrompt() {
        return buildSystemPrompt();
    }
}
