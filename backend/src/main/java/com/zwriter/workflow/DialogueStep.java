package com.zwriter.workflow;

/**
 * 对话引导步骤
 * 用于对话模式下创建小说的流程控制，每一步包含引导问题和元信息
 */
public record DialogueStep(
    int stepNumber,
    String stepName,
    String prompt,        // AI 向用户展示的问题
    String contextKey,    // 存储到 session context 的 key
    boolean isFinal       // 是否为最后一步
) {
    public DialogueStep(int stepNumber, String stepName, String prompt, String contextKey) {
        this(stepNumber, stepName, prompt, contextKey, false);
    }

    /**
     * 判断是否还有下一步
     */
    public boolean hasNext() {
        return !isFinal;
    }
}
