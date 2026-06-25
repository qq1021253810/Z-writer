package com.zwriter.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Ollama LLM 实现
 * 使用 Spring AI 的 Ollama 接口
 */
@Slf4j
@Service
public class OllamaLlmService implements LlmService {

    private final ChatClient chatClient;

    public OllamaLlmService(
            @Qualifier("ollamaChatModel") ChatModel ollamaChatModel) {
        this.chatClient = ChatClient.builder(ollamaChatModel)
                .defaultSystem("你是 Z-Writer 小说创作助手，专注于帮助用户创作高质量的小说内容。")
                .build();
        log.info("[Ollama] 初始化完成，使用 Spring AI Ollama 接口");
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        log.debug("[Ollama] 收到请求 - prompt长度: {}, systemPrompt长度: {}",
                prompt != null ? prompt.length() : 0,
                systemPrompt != null ? systemPrompt.length() : 0);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .system(systemPrompt != null ? systemPrompt : "你是 Z-Writer 小说创作助手。")
                    .call()
                    .content();

            log.debug("[Ollama] 返回响应 - 长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("[Ollama] 调用失败", e);
            return "【错误】Ollama 调用失败: " + e.getMessage();
        }
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        log.debug("[Ollama] 流式请求 - prompt长度: {}", prompt != null ? prompt.length() : 0);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .system(systemPrompt != null ? systemPrompt : "你是 Z-Writer 小说创作助手。")
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("[Ollama] 流式调用失败", e);
            return Flux.just("【错误】Ollama 流式调用失败: " + e.getMessage());
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
        log.debug("[Ollama] 向量化请求 - 文本长度: {}", text.length());
        // TODO: 实现向量化（需要单独的 embedding 模型配置）
        return new float[0];
    }
}
