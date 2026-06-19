package com.zwriter.agent.base;

import com.zwriter.llm.LlmService;
import com.zwriter.service.ContextService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Agent 基类
 * 提供通用的 LLM 调用和上下文管理能力
 */
@Slf4j
public abstract class BaseAgent {
    
    @Autowired
    protected LlmService llmService;
    
    @Autowired
    protected ContextService contextService;
    
    /**
     * 获取 Agent 名称
     */
    public abstract String getName();
    
    /**
     * 执行 Agent 任务
     * @param input 输入参数
     * @return 执行结果
     */
    public abstract AgentResult execute(AgentInput input);
    
    /**
     * 构建系统提示词
     */
    protected abstract String buildSystemPrompt();
    
    /**
     * 调用 LLM
     */
    protected String callLlm(String userPrompt, String systemPrompt) {
        log.info("[{}] 调用 LLM, 用户输入长度: {}", getName(), userPrompt.length());
        return llmService.chat(userPrompt, systemPrompt);
    }
    
    /**
     * 调用 LLM（使用默认系统提示词）
     */
    protected String callLlm(String userPrompt) {
        return callLlm(userPrompt, buildSystemPrompt());
    }
}
