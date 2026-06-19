package com.zwriter.controller;

import com.zwriter.llm.LlmService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping("/api/llm")
@RequiredArgsConstructor
public class LlmController {

    private final LlmService llmService;

    @PostMapping("/chat")
    public Map<String, String> chat(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String systemPrompt = request.getOrDefault("systemPrompt", "你是一个专业的网文小说创作助手。");
        String response = llmService.chat(prompt, systemPrompt);
        return Map.of("response", response);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        String systemPrompt = request.getOrDefault("systemPrompt", "你是一个专业的网文小说创作助手。");
        return llmService.chatStream(prompt, systemPrompt);
    }
}
