package com.zwriter.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Agnes AI LLM 实现
 * 使用 OpenAI 兼容接口，作为第二优先级
 */
@Slf4j
@Service
public class AgnesAiLlmService implements LlmService {

    private final ChatClient chatClient;

    public AgnesAiLlmService(
            @Qualifier("agnesAiChatModel") ChatModel agnesAiChatModel) {
        this.chatClient = ChatClient.builder(agnesAiChatModel)
                .defaultSystem("你是 Z-Writer 小说创作助手，专注于帮助用户创作高质量的小说内容。")
                .build();
        log.info("[AgnesAI] 初始化完成，使用 OpenAI 兼容接口");
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        log.debug("[AgnesAI] 收到请求 - prompt长度: {}, systemPrompt长度: {}",
                prompt != null ? prompt.length() : 0,
                systemPrompt != null ? systemPrompt.length() : 0);

        try {
            String result = chatClient.prompt()
                    .user(prompt)
                    .system(systemPrompt != null ? systemPrompt : "你是 Z-Writer 小说创作助手。")
                    .call()
                    .content();

            log.debug("[AgnesAI] 返回响应 - 长度: {}", result.length());
            return result;
        } catch (Exception e) {
            log.error("[AgnesAI] 调用失败", e);
            return "【错误】AgnesAI 调用失败: " + e.getMessage();
        }
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        log.debug("[AgnesAI] 流式请求 - prompt长度: {}", prompt != null ? prompt.length() : 0);

        try {
            return chatClient.prompt()
                    .user(prompt)
                    .system(systemPrompt != null ? systemPrompt : "你是 Z-Writer 小说创作助手。")
                    .stream()
                    .content();
        } catch (Exception e) {
            log.error("[AgnesAI] 流式调用失败", e);
            return Flux.just("【错误】AgnesAI 流式调用失败: " + e.getMessage());
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
        log.debug("[AgnesAI] 向量化请求 - 文本长度: {}", text.length());
        return new float[0];
    }
}
