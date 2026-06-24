package com.zwriter.agent.base;

import com.zwriter.llm.LlmService;
import com.zwriter.service.ContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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
     * 新版执行入口（AgentContext）
     * 子类应重写 doExecute(AgentContext ctx) 来实现新接口
     *
     * @param ctx 执行上下文
     * @return 执行结果
     */
    public AgentResult execute(AgentContext ctx) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务 (AgentContext): {}", name(), ctx.taskType());

        try {
            AgentResult result = doExecute(ctx);
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
     * 旧版执行入口（兼容旧代码）
     *
     * @param input 输入参数
     * @return 执行结果
     */
    public AgentResult execute(AgentInput input) {
        AgentContext ctx = AgentContext.fromAgentInput(input);
        return execute(ctx);
    }

    /**
     * 新版：子类实现具体业务逻辑（AgentContext 版本）
     * 默认实现：转换为 AgentInput 调用旧方法，子类应重写此方法
     *
     * @param ctx 执行上下文
     * @return 执行结果
     * @throws Exception 执行过程中可能抛出的异常
     */
    protected AgentResult doExecute(AgentContext ctx) throws Exception {
        // 默认实现：将 AgentContext 转换为 AgentInput 调用旧方法
        AgentInput input = new AgentInput();
        input.setTaskType(ctx.taskType());
        input.setUserInput(ctx.userInput());
        input.setParams(ctx.params());
        return doExecute(input);
    }

    /**
     * 旧版：子类实现具体业务逻辑（AgentInput 版本）
     * 保留用于向后兼容
     *
     * @param input 输入参数
     * @return 执行结果
     * @throws Exception 执行过程中可能抛出的异常
     */
    protected abstract AgentResult doExecute(AgentInput input) throws Exception;

    /**
     * 获取 Agent 名称（新版方法）
     */
    public abstract String name();

    /**
     * 获取 Agent 名称（旧版兼容）
     */
    public String getName() {
        return name();
    }

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

    /**
     * 获取子任务参数（AgentContext 版本），默认值为 "profile"
     */
    protected String getSubTask(AgentContext ctx) {
        return getSubTask(ctx, "profile");
    }

    /**
     * 获取子任务参数（AgentContext 版本）
     *
     * @param ctx            执行上下文
     * @param defaultSubTask 默认子任务
     * @return 子任务名称
     */
    protected String getSubTask(AgentContext ctx, String defaultSubTask) {
        if (ctx.params() == null) {
            return defaultSubTask;
        }
        Object subTask = ctx.params().get("subTask");
        return subTask != null ? subTask.toString() : defaultSubTask;
    }

    /**
     * 获取参数（AgentContext 版本），默认值为空字符串
     *
     * @param ctx 执行上下文
     * @param key 参数名
     * @return 参数值
     */
    protected String getParam(AgentContext ctx, String key) {
        return getParam(ctx, key, "");
    }

    /**
     * 获取参数（AgentContext 版本）
     *
     * @param ctx          执行上下文
     * @param key          参数名
     * @param defaultValue 默认值
     * @return 参数值
     */
    protected String getParam(AgentContext ctx, String key, String defaultValue) {
        if (ctx.params() == null) {
            return defaultValue;
        }
        Object value = ctx.params().get(key);
        return value != null ? value.toString() : defaultValue;
    }

    // ==================== 流式执行（与 CLI execute_stream 对齐） ====================

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
     * 获取系统提示词（与 CLI system_prompt() 对齐）
     */
    public String systemPrompt() {
        return buildSystemPrompt();
    }
}