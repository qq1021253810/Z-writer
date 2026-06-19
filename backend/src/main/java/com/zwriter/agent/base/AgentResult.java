package com.zwriter.agent.base;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Agent 执行结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 结果内容
     */
    private String content;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 额外数据
     */
    private Map<String, Object> data;
    
    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;
    
    /**
     * 创建成功结果
     */
    public static AgentResult success(String content) {
        return AgentResult.builder()
                .success(true)
                .content(content)
                .build();
    }
    
    /**
     * 创建成功结果（带数据）
     */
    public static AgentResult success(String content, Map<String, Object> data) {
        return AgentResult.builder()
                .success(true)
                .content(content)
                .data(data)
                .build();
    }
    
    /**
     * 创建失败结果
     */
    public static AgentResult failure(String errorMessage) {
        return AgentResult.builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
