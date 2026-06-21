package com.zwriter.llm;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DashScopeLlmServiceTest {

    @Autowired
    private DashScopeLlmService dashScopeLlmService;

    @Test
    void testChat() {
        String prompt = "请用200字写一段玄幻小说的开篇，主角是一个在废墟中醒来的失忆少年。";
        String systemPrompt = "你是一个资深网络小说作家，擅长玄幻题材，文笔细腻大气。";

        String result = dashScopeLlmService.chat(prompt, systemPrompt);
        System.out.println("=== 百炼API响应 ===");
        System.out.println(result);
        System.out.println("===================");
    }
}
