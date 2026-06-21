package com.zwriter.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Random;

/**
 * LLM Mock 实现 - 用于开发阶段，不依赖真实模型
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmService implements LlmService {

    private static final List<String> SAMPLE_RESPONSES = List.of(
            "【Mock 响应】这是一个关于玄幻世界的开篇。少年站在山巅，望着远方连绵的云海，心中涌起一股莫名的激动。他知道，从今天起，自己的命运将彻底改变...",
            "【Mock 响应】'你以为你能赢？'反派冷笑着，眼中闪过一丝不屑。主角紧握双拳，体内灵力疯狂涌动，他知道这一战避无可避...",
            "【Mock 响应】【世界观设定】\n力量体系：炼气→筑基→金丹→元婴→化神→大乘→渡劫\n主要势力：天剑宗、万魔殿、散修联盟\n金手指：神秘小瓶，可催熟灵药",
            "【Mock 响应】【角色档案】\n姓名：林逸\n身份：天剑宗外门弟子\n性格：隐忍坚毅，心思缜密\n口头禅：'事出反常必有妖'\n目标：查明父母失踪真相",
            "【Mock 响应】【大纲生成】\n第一卷：初入宗门（1-30章）\n- 第1章：穿越重生，获得金手指\n- 第2-5章：宗门考核，崭露头角\n- 第6-10章：结识挚友，树立初敌\n- 第11-20章：秘境探险，实力暴涨\n- 第21-30章：宗门大比，一战成名"
    );

    private final Random random = new Random();

    @Override
    public String chat(String prompt, String systemPrompt) {
        log.debug("[MockLLM] 收到请求 - prompt长度: {}, systemPrompt长度: {}",
                prompt != null ? prompt.length() : 0,
                systemPrompt != null ? systemPrompt.length() : 0);

        // 模拟延迟
        try {
            Thread.sleep(500 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        String response = SAMPLE_RESPONSES.get(random.nextInt(SAMPLE_RESPONSES.size()));
        log.debug("[MockLLM] 返回响应 - 长度: {}", response.length());
        return response;
    }

    @Override
    public Flux<String> chatStream(String prompt, String systemPrompt) {
        log.debug("[MockLLM] 流式请求 - prompt长度: {}", prompt != null ? prompt.length() : 0);

        String fullResponse = chat(prompt, systemPrompt);
        // 按字符流式输出
        return Flux.fromArray(fullResponse.split(""))
                .delayElements(Duration.ofMillis(50));
    }

    @Override
    public String chatWithContext(String prompt, String systemPrompt, String context) {
        log.debug("[MockLLM] 带上下文请求 - context长度: {}", context != null ? context.length() : 0);
        return chat(prompt, systemPrompt);
    }
}
