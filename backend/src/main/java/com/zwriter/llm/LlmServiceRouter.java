package com.zwriter.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * LLM 服务路由器
 * 实现百炼 API 优先、Ollama 降级的策略
 */
@Slf4j
@Service
@Primary
public class LlmServiceRouter implements LlmService {

    private final DashScopeLlmService dashScopeService;
    private final OllamaLlmService ollamaService;

    @Autowired
    public LlmServiceRouter(
            @Qualifier("dashScopeLlmService") DashScopeLlmService dashScopeService,
            @Qualifier("ollamaLlmService") OllamaLlmService ollamaService) {
        this.dashScopeService = dashScopeService;
        this.ollamaService = ollamaService;
        log.info("[LlmRouter] 初始化完成，优先使用百炼 API，降级到 Ollama");
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        try {
            String result = dashScopeService.chat(prompt, systemPrompt);
            if (isSuccess(result)) {
                return result;
            }
            log.warn("[LlmRouter] 百炼 API 调用失败，降级到 Ollama");
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 异常: {}，降级到 Ollama", e.getMessage());
        }

        return ollamaService.chat(prompt, systemPrompt);
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        try {
            Flux<String> result = dashScopeService.chatStream(prompt, systemPrompt);
            // 检查第一个元素是否成功
            return result
                    .take(1)
                    .flatMap(first -> {
                        if (isSuccess(first)) {
                            return Flux.concat(Flux.just(first),
                                    dashScopeService.chatStream(prompt, systemPrompt).skip(1));
                        }
                        log.warn("[LlmRouter] 百炼 API 流式调用失败，降级到 Ollama");
                        return ollamaService.chatStream(prompt, systemPrompt);
                    })
                    .switchIfEmpty(ollamaService.chatStream(prompt, systemPrompt));
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 流式异常: {}，降级到 Ollama", e.getMessage());
            return ollamaService.chatStream(prompt, systemPrompt);
        }
    }

    @Override
    public String chatWithContext(String prompt, String systemPrompt, String context) {
        try {
            String result = dashScopeService.chatWithContext(prompt, systemPrompt, context);
            if (isSuccess(result)) {
                return result;
            }
            log.warn("[LlmRouter] 百炼 API 调用失败，降级到 Ollama");
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 异常: {}，降级到 Ollama", e.getMessage());
        }

        return ollamaService.chatWithContext(prompt, systemPrompt, context);
    }

    @Override
    public float[] embed(String text) {
        try {
            float[] result = dashScopeService.embed(text);
            if (result != null && result.length > 0) {
                return result;
            }
            log.warn("[LlmRouter] 百炼 API 向量化失败，降级到 Ollama");
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 向量化异常: {}，降级到 Ollama", e.getMessage());
        }

        return ollamaService.embed(text);
    }

    /**
     * 判断调用结果是否成功
     */
    private boolean isSuccess(String result) {
        return result != null && !result.startsWith("【错误】");
    }
}
