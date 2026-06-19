package com.zwriter.agent.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 输入参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentInput {
    
    /**
     * 小说 ID
     */
    private Long novelId;
    
    /**
     * 任务类型
     */
    private String taskType;
    
    /**
     * 用户输入/指令
     */
    private String userInput;
    
    /**
     * 额外参数
     */
    private Map<String, Object> params;
    
    /**
     * 是否流式输出
     */
    @Builder.Default
    private boolean stream = false;
}
