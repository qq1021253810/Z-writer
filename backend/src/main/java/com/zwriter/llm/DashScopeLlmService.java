package com.zwriter.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 阿里云百炼 DashScope LLM 实现
 * 通过 OpenAI 兼容接口调用，使用 RestTemplate 避免 Spring AI 自动配置冲突
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope")
public class DashScopeLlmService implements LlmService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${llm.dashscope.api-key}")
    private String apiKey;

    @Value("${llm.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode}")
    private String baseUrl;

    @Value("${llm.dashscope.model:qwen-plus}")
    private String model;

    @Value("${llm.max-tokens:4096}")
    private int maxTokens;

    @Value("${llm.temperature:0.8}")
    private double temperature;

    public DashScopeLlmService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        log.info("[DashScope] 初始化完成");
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        log.debug("[DashScope] 收到请求 - prompt长度: {}, systemPrompt长度: {}",
                prompt != null ? prompt.length() : 0,
                systemPrompt != null ? systemPrompt.length() : 0);

        try {
            String url = baseUrl + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ObjectNode body = buildRequestBody(prompt, systemPrompt, false);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());
            String result = root.path("choices").path(0).path("message").path("content").asText();

            log.debug("[DashScope] 返回响应 - 长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("[DashScope] 调用失败", e);
            return "【错误】DashScope 调用失败: " + e.getMessage();
        }
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        log.debug("[DashScope] 流式请求 - prompt长度: {}", prompt != null ? prompt.length() : 0);

        try {
            String url = baseUrl + "/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            ObjectNode body = buildRequestBody(prompt, systemPrompt, true);
            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            String responseBody = response.getBody();

            StringBuilder fullContent = new StringBuilder();
            for (String line : responseBody.split("\n")) {
                if (line.startsWith("data: ")) {
                    String data = line.substring(6).trim();
                    if ("[DONE]".equals(data)) break;
                    try {
                        JsonNode chunk = objectMapper.readTree(data);
                        String content = chunk.path("choices").path(0).path("delta").path("content").asText();
                        if (!content.isEmpty()) {
                            fullContent.append(content);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }

            String result = fullContent.toString();
            if (result.isEmpty()) {
                result = "【错误】DashScope 流式响应为空";
            }

            return Flux.fromArray(result.split("(?<=.)"))
                    .delayElements(Duration.ofMillis(30));
        } catch (Exception e) {
            log.error("[DashScope] 流式调用失败", e);
            return Flux.just("【错误】DashScope 流式调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithContext(String prompt, String systemPrompt, String context) {
        log.debug("[DashScope] 带上下文请求 - context长度: {}", context != null ? context.length() : 0);

        String fullPrompt = context != null && !context.isEmpty()
                ? context + "\n\n" + prompt
                : prompt;

        return chat(fullPrompt, systemPrompt);
    }

    private ObjectNode buildRequestBody(String prompt, String systemPrompt, boolean stream) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("temperature", temperature);
        body.put("stream", stream);

        ArrayNode messages = body.putArray("messages");

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            ObjectNode sysMsg = objectMapper.createObjectNode();
            sysMsg.put("role", "system");
            sysMsg.put("content", systemPrompt);
            messages.add(sysMsg);
        }

        ObjectNode userMsg = objectMapper.createObjectNode();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);

        return body;
    }
}
