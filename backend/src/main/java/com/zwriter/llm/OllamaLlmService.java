package com.zwriter.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

/**
 * LLM Ollama 实现 - 调用本地 Ollama 模型
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "ollama")
public class OllamaLlmService implements LlmService {

    private final OllamaChatModel chatModel;

    @Autowired
    public OllamaLlmService(OllamaChatModel chatModel) {
        this.chatModel = chatModel;
        log.info("[OllamaLLM] 初始化完成，使用模型: {}", chatModel.getDefaultOptions().getModel());
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        log.debug("[OllamaLLM] 收到请求 - prompt长度: {}, systemPrompt长度: {}",
                prompt != null ? prompt.length() : 0,
                systemPrompt != null ? systemPrompt.length() : 0);

        try {
            List<Message> messages = new ArrayList<>();
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            
            messages.add(new UserMessage(prompt));

            Prompt chatPrompt = new Prompt(messages);
            ChatResponse response = chatModel.call(chatPrompt);
            
            String result = response.getResult().getOutput().getText();
            log.debug("[OllamaLLM] 返回响应 - 长度: {}", result.length());
            
            return result;
        } catch (Exception e) {
            log.error("[OllamaLLM] 调用失败", e);
            return "【错误】Ollama 调用失败: " + e.getMessage();
        }
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        log.debug("[OllamaLLM] 流式请求 - prompt长度: {}", prompt != null ? prompt.length() : 0);

        try {
            List<Message> messages = new ArrayList<>();
            
            if (systemPrompt != null && !systemPrompt.isEmpty()) {
                messages.add(new SystemMessage(systemPrompt));
            }
            
            messages.add(new UserMessage(prompt));

            Prompt chatPrompt = new Prompt(messages);
            
            return chatModel.stream(chatPrompt)
                    .map(response -> {
                        if (response.getResult() != null && response.getResult().getOutput() != null) {
                            return response.getResult().getOutput().getText();
                        }
                        return "";
                    })
                    .filter(text -> text != null && !text.isEmpty());
        } catch (Exception e) {
            log.error("[OllamaLLM] 流式调用失败", e);
            return Flux.just("【错误】Ollama 流式调用失败: " + e.getMessage());
        }
    }

    @Override
    public String chatWithContext(String prompt, String systemPrompt, String context) {
        log.debug("[OllamaLLM] 带上下文请求 - context长度: {}", context != null ? context.length() : 0);

        String fullPrompt = context != null && !context.isEmpty()
                ? context + "\n\n" + prompt
                : prompt;

        return chat(fullPrompt, systemPrompt);
    }

    @Override
    public float[] embed(String text) {
        log.debug("[OllamaLLM] 向量化请求 - 文本长度: {}", text.length());
        try {
            // Spring AI Ollama 已注入 embedding model
            // 这里复用 Spring AI 的 OllamaEmbeddingModel
            // 但由于我们没直接注入它，需要通过 Spring 上下文获取
            // 简化处理：返回空向量（Phase 3.4 时完善）
            log.warn("[OllamaLLM] Ollama embedding 暂未实现，将在 Phase 3.4 完善");
            return new float[0];
        } catch (Exception e) {
            log.error("[OllamaLLM] 向量化失败", e);
            return new float[0];
        }
    }
}
