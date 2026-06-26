package com.zwriter.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * LLM 服务路由器
 * 实现三级降级策略：百炼 API -> Agnes AI -> Ollama
 */
@Slf4j
@Service
@Primary
public class LlmServiceRouter implements LlmService {

    private final DashScopeLlmService dashScopeService;
    private final AgnesAiLlmService agnesAiService;
    private final OllamaLlmService ollamaService;

    @Autowired
    public LlmServiceRouter(
            @Qualifier("dashScopeLlmService") DashScopeLlmService dashScopeService,
            @Qualifier("agnesAiLlmService") AgnesAiLlmService agnesAiService,
            @Qualifier("ollamaLlmService") OllamaLlmService ollamaService) {
        this.dashScopeService = dashScopeService;
        this.agnesAiService = agnesAiService;
        this.ollamaService = ollamaService;
        log.info("[LlmRouter] 初始化完成，降级链：百炼 API -> Agnes AI -> Ollama");
    }

    @Override
    public String chat(String prompt, String systemPrompt) {
        // 第一优先级：百炼 API
        try {
            String result = dashScopeService.chat(prompt, systemPrompt);
            if (isSuccess(result)) {
                return result;
            }
            log.warn("[LlmRouter] 百炼 API 调用失败，降级到 Agnes AI");
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 异常: {}，降级到 Agnes AI", e.getMessage());
        }

        // 第二优先级：Agnes AI
        try {
            String result = agnesAiService.chat(prompt, systemPrompt);
            if (isSuccess(result)) {
                return result;
            }
            log.warn("[LlmRouter] Agnes AI 调用失败，降级到 Ollama");
        } catch (Exception e) {
            log.warn("[LlmRouter] Agnes AI 异常: {}，降级到 Ollama", e.getMessage());
        }

        // 第三优先级：Ollama
        return ollamaService.chat(prompt, systemPrompt);
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        // 第一优先级：百炼 API
        try {
            Flux<String> result = dashScopeService.chatStream(prompt, systemPrompt);
            return result
                    .take(1)
                    .flatMap(first -> {
                        if (isSuccess(first)) {
                            return Flux.concat(Flux.just(first),
                                    dashScopeService.chatStream(prompt, systemPrompt).skip(1));
                        }
                        log.warn("[LlmRouter] 百炼 API 流式调用失败，降级到 Agnes AI");
                        return tryAgnesAiStreamOrOllama(prompt, systemPrompt);
                    })
                    .switchIfEmpty(tryAgnesAiStreamOrOllama(prompt, systemPrompt));
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 流式异常: {}，降级到 Agnes AI", e.getMessage());
            return tryAgnesAiStreamOrOllama(prompt, systemPrompt);
        }
    }

    @Override
    public String chatWithContext(String prompt, String systemPrompt, String context) {
        // 第一优先级：百炼 API
        try {
            String result = dashScopeService.chatWithContext(prompt, systemPrompt, context);
            if (isSuccess(result)) {
                return result;
            }
            log.warn("[LlmRouter] 百炼 API 调用失败，降级到 Agnes AI");
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 异常: {}，降级到 Agnes AI", e.getMessage());
        }

        // 第二优先级：Agnes AI
        try {
            String result = agnesAiService.chatWithContext(prompt, systemPrompt, context);
            if (isSuccess(result)) {
                return result;
            }
            log.warn("[LlmRouter] Agnes AI 调用失败，降级到 Ollama");
        } catch (Exception e) {
            log.warn("[LlmRouter] Agnes AI 异常: {}，降级到 Ollama", e.getMessage());
        }

        // 第三优先级：Ollama
        return ollamaService.chatWithContext(prompt, systemPrompt, context);
    }

    @Override
    public float[] embed(String text) {
        // 第一优先级：百炼 API
        try {
            float[] result = dashScopeService.embed(text);
            if (result != null && result.length > 0) {
                return result;
            }
            log.warn("[LlmRouter] 百炼 API 向量化失败，降级到 Agnes AI");
        } catch (Exception e) {
            log.warn("[LlmRouter] 百炼 API 向量化异常: {}，降级到 Agnes AI", e.getMessage());
        }

        // 第二优先级：Agnes AI
        try {
            float[] result = agnesAiService.embed(text);
            if (result != null && result.length > 0) {
                return result;
            }
            log.warn("[LlmRouter] Agnes AI 向量化失败，降级到 Ollama");
        } catch (Exception e) {
            log.warn("[LlmRouter] Agnes AI 向量化异常: {}，降级到 Ollama", e.getMessage());
        }

        // 第三优先级：Ollama
        return ollamaService.embed(text);
    }

    /**
     * 尝试 Agnes AI 流式调用，失败则降级到 Ollama
     */
    private Flux<String> tryAgnesAiStreamOrOllama(String prompt, String systemPrompt) {
        try {
            Flux<String> result = agnesAiService.chatStream(prompt, systemPrompt);
            return result
                    .take(1)
                    .flatMap(first -> {
                        if (isSuccess(first)) {
                            return Flux.concat(Flux.just(first),
                                    agnesAiService.chatStream(prompt, systemPrompt).skip(1));
                        }
                        log.warn("[LlmRouter] Agnes AI 流式调用失败，降级到 Ollama");
                        return ollamaService.chatStream(prompt, systemPrompt);
                    })
                    .switchIfEmpty(ollamaService.chatStream(prompt, systemPrompt));
        } catch (Exception e) {
            log.warn("[LlmRouter] Agnes AI 流式异常: {}，降级到 Ollama", e.getMessage());
            return ollamaService.chatStream(prompt, systemPrompt);
        }
    }

    /**
     * 判断调用结果是否成功
     */
    private boolean isSuccess(String result) {
        return result != null && !result.startsWith("【错误】");
    }
}
