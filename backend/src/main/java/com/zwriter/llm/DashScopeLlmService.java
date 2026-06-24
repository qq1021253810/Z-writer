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
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 阿里云百炼 DashScope LLM 实现（与 CLI OllamaClient 对齐）
 * 通过 OpenAI 兼容接口调用，使用 WebClient 实现真正的流式响应
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "dashscope")
public class DashScopeLlmService implements LlmService {

    private final WebClient webClient;
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
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = new ObjectMapper();
        log.info("[DashScope] 初始化完成，baseUrl: {}", baseUrl);
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        log.debug("[DashScope] 收到请求 - prompt长度: {}, systemPrompt长度: {}",
                prompt != null ? prompt.length() : 0,
                systemPrompt != null ? systemPrompt.length() : 0);

        try {
            ObjectNode body = buildRequestBody(prompt, systemPrompt, false);
            String response = webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(120));

            JsonNode root = objectMapper.readTree(response);
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
            ObjectNode body = buildRequestBody(prompt, systemPrompt, true);

            return webClient.post()
                    .uri("/v1/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .flatMap(chunk -> {
                        StringBuilder content = new StringBuilder();
                        for (String line : chunk.split("\n")) {
                            if (line.startsWith("data: ")) {
                                String data = line.substring(6).trim();
                                if ("[DONE]".equals(data)) continue;
                                try {
                                    JsonNode node = objectMapper.readTree(data);
                                    String text = node.path("choices").path(0).path("delta").path("content").asText();
                                    if (!text.isEmpty()) {
                                        content.append(text);
                                    }
                                } catch (Exception e) {
                                    log.debug("[DashScope] 解析chunk失败: {}", data);
                                }
                            }
                        }
                        return content.length() > 0 ? Flux.just(content.toString()) : Flux.empty();
                    });
        } catch (Exception e) {
            log.error("[DashScope] 流式调用失败", e);
            return Flux.just("【错误】DashScope 流式调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithContext(String prompt, String systemPrompt, String context) {
        String fullPrompt = context != null && !context.isEmpty()
                ? context + "\n\n" + prompt
                : prompt;
        return chat(fullPrompt, systemPrompt);
    }

    @Override
    public float[] embed(String text) {
        log.debug("[DashScope] 向量化请求 - 文本长度: {}", text.length());

        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", "text-embedding-v3");
            body.put("input", text);

            String response = webClient.post()
                    .uri("/v1/services/embeddings/text-embedding/text-embedding")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(Duration.ofSeconds(30));

            JsonNode root = objectMapper.readTree(response);
            JsonNode embeddingNode = root.path("output").path("embeddings").path(0).path("embedding");

            float[] embedding = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                embedding[i] = (float) embeddingNode.get(i).asDouble();
            }

            log.debug("[DashScope] 向量化完成 - 维度: {}", embedding.length);
            return embedding;
        } catch (Exception e) {
            log.error("[DashScope] 向量化失败", e);
            return new float[0];
        }
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
