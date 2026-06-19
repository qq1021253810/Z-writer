package com.zwriter.llm;

import reactor.core.publisher.Flux;

/**
 * LLM 服务接口 - 支持 Mock 和 Ollama 实现切换
 */
public interface LlmService {

    /**
     * 同步调用 LLM
     */
    String chat(String prompt, String systemPrompt);

    /**
     * 流式调用 LLM
     */
    Flux<String> chatStream(String prompt, String systemPrompt);

    /**
     * 带上下文的多轮对话
     */
    String chatWithContext(String prompt, String systemPrompt, String context);
}
