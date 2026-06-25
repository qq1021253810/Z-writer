package com.zwriter.controller;

import com.zwriter.common.ApiResponse;
import com.zwriter.llm.DashScopeLlmService;
import com.zwriter.llm.LlmService;
import com.zwriter.llm.OllamaLlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * LLM 测试接口（用于验证百炼 API / Ollama 连接）
 */
@Slf4j
@RestController
@RequestMapping("/api/llm-test")
public class LlmTestController {

    @Autowired
    private LlmService llmService;

    @Autowired
    @Qualifier("dashScopeLlmService")
    private DashScopeLlmService dashScopeService;

    @Autowired
    @Qualifier("ollamaLlmService")
    private OllamaLlmService ollamaService;

    /**
     * 测试 LLM 调用（走 Router，百炼优先）
     */
    @PostMapping("/chat")
    public ApiResponse<Map<String, Object>> testChat(@RequestBody ChatRequest request) {
        try {
            String response = llmService.chat(request.getPrompt(), request.getSystemPrompt());
            return ApiResponse.success(Map.of(
                    "response", response,
                    "success", !response.startsWith("【错误】")
            ));
        } catch (Exception e) {
            log.error("[LlmTest] 调用失败", e);
            return ApiResponse.failure("调用失败: " + e.getMessage());
        }
    }

    /**
     * 直接测试百炼 API
     */
    @PostMapping("/dashscope")
    public ApiResponse<Map<String, Object>> testDashScope(@RequestBody ChatRequest request) {
        try {
            String response = dashScopeService.chat(request.getPrompt(), request.getSystemPrompt());
            return ApiResponse.success(Map.of(
                    "response", response,
                    "success", !response.startsWith("【错误】"),
                    "provider", "dashscope"
            ));
        } catch (Exception e) {
            log.error("[LlmTest] 百炼调用失败", e);
            return ApiResponse.failure("百炼调用失败: " + e.getMessage());
        }
    }

    /**
     * 直接测试 Ollama
     */
    @PostMapping("/ollama")
    public ApiResponse<Map<String, Object>> testOllama(@RequestBody ChatRequest request) {
        try {
            String response = ollamaService.chat(request.getPrompt(), request.getSystemPrompt());
            return ApiResponse.success(Map.of(
                    "response", response,
                    "success", !response.startsWith("【错误】"),
                    "provider", "ollama"
            ));
        } catch (Exception e) {
            log.error("[LlmTest] Ollama调用失败", e);
            return ApiResponse.failure("Ollama调用失败: " + e.getMessage());
        }
    }

    public static class ChatRequest {
        private String prompt;
        private String systemPrompt;

        public String getPrompt() { return prompt; }
        public void setPrompt(String prompt) { this.prompt = prompt; }
        public String getSystemPrompt() { return systemPrompt; }
        public void setSystemPrompt(String systemPrompt) { this.systemPrompt = systemPrompt; }
    }
}
