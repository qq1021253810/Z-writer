package com.zwriter.controller;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.controller.ControllerAgent;
import com.zwriter.common.ApiResponse;
import com.zwriter.session.SessionManager;
import com.zwriter.session.SessionManager.Session;
import com.zwriter.workflow.CreateNovelWorkflow;
import com.zwriter.workflow.DialogueStep;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 会话对话 API（多轮对话模式 + SSE 流式推送）
 * - POST /api/sessions → 创建会话
 * - POST /api/sessions/{id}/message → 发送消息（SSE 流式）
 * - GET /api/sessions/{id}/history → 获取对话历史
 * - DELETE /api/sessions/{id} → 删除会话
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    @Autowired
    private SessionManager sessionManager;

    @Autowired
    private ControllerAgent controllerAgent;

    @Autowired
    private CreateNovelWorkflow createNovelWorkflow;

    /**
     * 创建会话
     */
    @PostMapping
    public ApiResponse<Map<String, Object>> createSession(@RequestBody CreateSessionRequest request) {
        try {
            String mode = request.getMode() != null ? request.getMode() : "create";
            Session session = sessionManager.createSession(
                    request.getNovelName(),
                    mode
            );

            Map<String, Object> response = Map.of(
                    "sessionId", session.getId(),
                    "mode", session.getMode(),
                    "novelName", session.getNovelName()
            );

            // 对于 create 模式，获取第一步对话引导
            if ("create".equals(mode)) {
                Map<String, Object> context = session.getContextData();
                context.put("progress", 1);
                DialogueStep step = createNovelWorkflow.getNextStep(session.getId(), context);
                sessionManager.updateContext(session.getId(), "progress", 1);

                // 返回第一步引导问题
                return ApiResponse.success(Map.of(
                        "sessionId", session.getId(),
                        "mode", mode,
                        "novelName", session.getNovelName(),
                        "dialogueStep", Map.of(
                                "stepNumber", step.stepNumber(),
                                "stepName", step.stepName(),
                                "prompt", step.prompt(),
                                "isFinal", step.isFinal()
                        )
                ));
            }

            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("[SessionController] 创建会话失败", e);
            return ApiResponse.failure("创建会话失败: " + e.getMessage());
        }
    }

    /**
     * 发送消息（SSE 流式返回）
     */
    @PostMapping("/{sessionId}/message")
    public SseEmitter sendMessage(@PathVariable String sessionId,
                                  @RequestBody MessageRequest request) {
        Session session = sessionManager.getSession(sessionId);
        if (session == null) {
            // 返回一个立即关闭的 emitter
            SseEmitter emitter = new SseEmitter();
            try {
                emitter.send(SseEmitter.event().name("error").data("会话不存在"));
                emitter.complete();
            } catch (Exception e) {
                // ignore
            }
            return emitter;
        }

        // 保存用户消息
        sessionManager.addMessage(sessionId, "user", request.getMessage());

        // 对于 create 模式，使用对话模式工作流处理
        if ("create".equals(session.getMode())) {
            return handleCreateModeMessage(sessionId, session, request);
        }

        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        // 构建 AgentContext
        AgentContext ctx = new AgentContext(
                Path.of("./workspaces/" + session.getNovelName()),
                request.getMessage(),
                buildSystemPrompt(session),
                request.getParams()
        );

        // 根据会话模式选择 Agent 执行
        if (request.isStream()) {
            // 流式执行
            controllerAgent.handleRequestStream(ctx, emitter);
        } else {
            // 同步执行
            try {
                AgentResult result = controllerAgent.handleRequest(ctx);
                String content = result.getContent();

                // 保存 AI 回复
                sessionManager.addMessage(sessionId, "assistant", content);

                // 一次性返回结果
                emitter.send(SseEmitter.event()
                        .name("message")
                        .data(Map.of(
                                "content", content,
                                "success", result.isSuccess(),
                                "errorMessage", result.getErrorMessage()
                        )));
                emitter.complete();
            } catch (Exception e) {
                log.error("[SessionController] 消息处理失败", e);
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.completeWithError(e);
                } catch (Exception ex) {
                    // ignore
                }
            }
        }

        return emitter;
    }

    /**
     * 处理 create 模式的消息（对话模式）
     */
    private SseEmitter handleCreateModeMessage(String sessionId, Session session, MessageRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);

        try {
            Map<String, Object> context = session.getContextData();
            String userReply = request.getMessage();

            // 处理用户回复，推进到下一步
            DialogueStep nextStep = createNovelWorkflow.processUserReply(sessionId, userReply, context);

            // 保存 workflow 的 context 到 session
            sessionManager.updateContext(sessionId, "progress", context.get("progress"));
            // 保存其他上下文数据
            for (Map.Entry<String, Object> entry : context.entrySet()) {
                sessionManager.updateContext(sessionId, entry.getKey(), entry.getValue());
            }

            // 如果已完成所有步骤，触发实际生成
            if (nextStep.isFinal()) {
                CreateNovelWorkflow.WorkflowResult result = createNovelWorkflow.executeGeneration(context);

                sessionManager.addMessage(sessionId, "assistant", "小说创建完成！");
                emitter.send(SseEmitter.event()
                        .name("complete")
                        .data(Map.of(
                                "content", "所有文件已生成并保存。",
                                "success", result.isSuccess(),
                                "errorMessage", result.getErrorMessage(),
                                "workflowResult", Map.of(
                                        "topic", result.getTopic(),
                                        "worldSetting", result.getWorldSetting(),
                                        "characterProfile", result.getCharacterProfile()
                                )
                        )));
                emitter.complete();
                return emitter;
            }

            // 保存 AI 回复（下一步的引导问题）
            sessionManager.addMessage(sessionId, "assistant", nextStep.prompt());

            // 返回下一步引导问题
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(Map.of(
                            "content", nextStep.prompt(),
                            "dialogueStep", Map.of(
                                    "stepNumber", nextStep.stepNumber(),
                                    "stepName", nextStep.stepName(),
                                    "prompt", nextStep.prompt(),
                                    "isFinal", nextStep.isFinal()
                            )
                    )));
            emitter.complete();

        } catch (Exception e) {
            log.error("[SessionController] create 模式消息处理失败", e);
            try {
                emitter.send(SseEmitter.event().name("error").data("处理失败: " + e.getMessage()));
                emitter.completeWithError(e);
            } catch (Exception ex) {
                // ignore
            }
        }

        return emitter;
    }

    /**
     * 获取对话历史
     */
    @GetMapping("/{sessionId}/history")
    public ApiResponse<List<Session.Message>> getHistory(@PathVariable String sessionId) {
        List<Session.Message> history = sessionManager.getHistory(sessionId);
        return ApiResponse.success(history);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public ApiResponse<Void> deleteSession(@PathVariable String sessionId) {
        sessionManager.closeSession(sessionId);
        return ApiResponse.success(null);
    }

    /**
     * 根据会话模式构建系统提示词
     */
    private String buildSystemPrompt(Session session) {
        return switch (session.getMode()) {
            case "create" -> """
                    你是一个专业的网文小说创作助手，正在帮助用户创建新小说。
                    引导用户完成：选题→世界观→角色→大纲→黄金三章设计。
                    每次只问一个问题，等待用户回答后再进入下一步。
                    语言简洁，问题具体，避免过于抽象。
                    """;
            case "continue" -> """
                    你是一个专业的网文小说续写助手。
                    基于已有内容生成新的章节，保持文风一致、角色设定不变。
                    """;
            case "chat" -> """
                    你是一个网文小说创作顾问，可以回答用户关于小说创作的任何问题。
                    提供写作建议、剧情分析、角色调整建议等。
                    """;
            default -> "你是一个网文小说创作助手。";
        };
    }

    public static class CreateSessionRequest {
        private String novelName;
        private String mode;

        public String getNovelName() { return novelName; }
        public void setNovelName(String novelName) { this.novelName = novelName; }
        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
    }

    public static class MessageRequest {
        private String message;
        private Map<String, Object> params;
        private boolean stream = true;

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public Map<String, Object> getParams() { return params; }
        public void setParams(Map<String, Object> params) { this.params = params; }
        public boolean isStream() { return stream; }
        public void setStream(boolean stream) { this.stream = stream; }
    }
}
