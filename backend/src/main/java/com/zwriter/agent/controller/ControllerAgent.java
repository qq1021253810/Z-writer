package com.zwriter.agent.controller;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.worldoutline.WorldOutlineAgent;
import com.zwriter.agent.character.CharacterAgent;
import com.zwriter.agent.plot.PlotAgent;
import com.zwriter.agent.writing.WritingAgent;
import com.zwriter.agent.polish.PolishAgent;
import com.zwriter.agent.compliance.ComplianceAgent;
import com.zwriter.llm.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.function.Function;

/**
 * 总控调度 Agent（与 CLI 的 Agent 路由对齐）
 * 支持两种调用方式：
 * 1. 旧版：AgentInput → execute (向后兼容)
 * 2. 新版：AgentContext → execute (推荐)
 */
@Slf4j
@Component
public class ControllerAgent {

    @Autowired
    private WorldOutlineAgent worldOutlineAgent;

    @Autowired
    private CharacterAgent characterAgent;

    @Autowired
    private PlotAgent plotAgent;

    @Autowired
    private WritingAgent writingAgent;

    @Autowired
    private PolishAgent polishAgent;

    @Autowired
    private ComplianceAgent complianceAgent;

    @Autowired
    private LlmService llmService;

    /**
     * Agent 路由映射表（旧版 AgentInput）
     */
    private final Map<String, Function<AgentInput, AgentResult>> agentRouter = Map.of(
            "world_outline", worldOutlineAgent::execute,
            "character", characterAgent::execute,
            "plot", plotAgent::execute,
            "writing", writingAgent::execute,
            "polish", polishAgent::execute,
            "compliance", complianceAgent::execute
    );

    /**
     * Agent 路由映射表（新版 AgentContext）
     */
    private final Map<String, Function<AgentContext, AgentResult>> agentRouterV2 = Map.of(
            "world_outline", worldOutlineAgent::execute,
            "character", characterAgent::execute,
            "plot", plotAgent::execute,
            "writing", writingAgent::execute,
            "polish", polishAgent::execute,
            "compliance", complianceAgent::execute
    );

    // ==================== 旧版接口（向后兼容） ====================

    /**
     * 处理用户请求（旧版 AgentInput）
     */
    public AgentResult handleRequest(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[总控 Agent] 收到请求: taskType={}", input.getTaskType());

        try {
            AgentResult result = routeToAgent(input.getTaskType(), input);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[总控 Agent] 请求完成，耗时: {}ms", duration);
            return result;
        } catch (Exception e) {
            log.error("[总控 Agent] 处理请求失败", e);
            return AgentResult.failure("处理失败: " + e.getMessage());
        }
    }

    // ==================== 新版接口（AgentContext） ====================

    /**
     * 处理用户请求（新版 AgentContext）
     */
    public AgentResult handleRequest(AgentContext ctx) {
        long startTime = System.currentTimeMillis();
        String taskType = inferTaskType(ctx);
        log.info("[总控 Agent] 收到请求: taskType={}", taskType);

        try {
            AgentResult result = routeToAgentV2(taskType, ctx);
            long duration = System.currentTimeMillis() - startTime;
            log.info("[总控 Agent] 请求完成，耗时: {}ms", duration);
            return result;
        } catch (Exception e) {
            log.error("[总控 Agent] 处理请求失败", e);
            return AgentResult.failure("处理失败: " + e.getMessage());
        }
    }

    /**
     * 流式处理（新版 AgentContext + SSE）
     */
    public void handleRequestStream(AgentContext ctx, SseEmitter emitter) {
        String taskType = inferTaskType(ctx);
        log.info("[总控 Agent] 流式请求: taskType={}", taskType);

        // 直接调用对应 Agent 的流式执行
        Function<AgentContext, AgentResult> agent = agentRouterV2.get(taskType);
        if (agent != null) {
            // 使用对应 Agent 的流式执行
            switch (taskType) {
                case "world_outline" -> worldOutlineAgent.executeStream(ctx, emitter);
                case "character" -> characterAgent.executeStream(ctx, emitter);
                case "plot" -> plotAgent.executeStream(ctx, emitter);
                case "writing" -> writingAgent.executeStream(ctx, emitter);
                case "polish" -> polishAgent.executeStream(ctx, emitter);
                case "compliance" -> complianceAgent.executeStream(ctx, emitter);
                default -> {
                    try {
                        emitter.send(SseEmitter.event().name("error").data("未知的任务类型: " + taskType));
                        emitter.complete();
                    } catch (Exception e) {
                        log.warn("[总控 Agent] SSE 发送失败", e);
                    }
                }
            }
        } else {
            // 默认：直接调用 LLM 流式响应
            llmService.chatStream(ctx.userInput(), ctx.systemPrompt())
                    .doOnNext(chunk -> {
                        try {
                            emitter.send(SseEmitter.event().name("chunk").data(chunk));
                        } catch (Exception e) {
                            log.warn("[总控 Agent] SSE 发送失败", e);
                        }
                    })
                    .doOnComplete(() -> {
                        try {
                            emitter.complete();
                        } catch (Exception e) {
                            log.warn("[总控 Agent] SSE 完成失败", e);
                        }
                    })
                    .doOnError(e -> {
                        try {
                            emitter.completeWithError(e);
                        } catch (Exception ex) {
                            // ignore
                        }
                    })
                    .subscribe();
        }
    }

    /**
     * 推断任务类型（从 params 或 userInput 中推断）
     */
    private String inferTaskType(AgentContext ctx) {
        Object taskType = ctx.params().get("taskType");
        if (taskType != null) {
            return taskType.toString();
        }
        // 默认：world_outline（通用创作）
        return "world_outline";
    }

    // ==================== 路由辅助 ====================

    private AgentResult routeToAgent(String taskType, AgentInput input) {
        return agentRouter.getOrDefault(taskType,
                i -> AgentResult.failure("未知的任务类型: " + taskType)).apply(input);
    }

    private AgentResult routeToAgentV2(String taskType, AgentContext ctx) {
        return agentRouterV2.getOrDefault(taskType,
                c -> AgentResult.failure("未知的任务类型: " + taskType)).apply(ctx);
    }
}
