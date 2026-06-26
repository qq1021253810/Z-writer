package com.zwriter.config;

import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Agnes AI 配置类
 * 手动构建 OpenAI 兼容的 ChatModel
 */
@Configuration
public class AgnesAiConfig {

    @Value("${spring.ai.agnes-ai.api-key}")
    private String apiKey;

    @Value("${spring.ai.agnes-ai.base-url}")
    private String baseUrl;

    @Value("${spring.ai.agnes-ai.model}")
    private String model;

    @Value("${spring.ai.agnes-ai.temperature}")
    private Double temperature;

    @Value("${spring.ai.agnes-ai.max-tokens}")
    private Integer maxTokens;

    @Bean
    public OpenAiChatModel agnesAiChatModel() {
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();

        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }
}
