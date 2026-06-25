package com.zwriter.llm;

import reactor.core.publisher.Flux;

/**
 * LLM 服务接口
 * 支持同步、流式对话和向量化
 */
public interface LlmService {

    /**
     * 同步调用 LLM
     */
    String chat(String prompt, String systemPrompt);

    /**
     * 流式调用 LLM（返回字符流）
     */
    Flux<String> chatStream(String prompt, String systemPrompt);

    /**
     * 带上下文的多轮对话
     */
    String chatWithContext(String prompt, String systemPrompt, String context);

    /**
     * 文本向量化（用于 RAG 检索）
     * @return 向量数组
     */
    float[] embed(String text);
}
