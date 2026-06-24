package com.zwriter.context;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * Token 优化器（与 CLI token_optimizer.rs 对齐）
 * 预估 token 数量，裁剪上下文防止超出 LLM 限制
 */
@Slf4j
public class TokenOptimizer {

    private final int maxTokens; // LLM 最大上下文
    private final int reservedTokens; // 预留给回复的 token

    public TokenOptimizer(int maxTokens, int reservedTokens) {
        this.maxTokens = maxTokens;
        this.reservedTokens = reservedTokens;
    }

    /**
     * 计算可用 token 预算
     */
    public int getAvailableBudget(int systemPromptTokens, int userInputTokens) {
        return maxTokens - reservedTokens - systemPromptTokens - userInputTokens;
    }

    /**
     * 裁剪上下文到可用 token 数
     */
    public String trimContext(String context, int maxContextTokens) {
        if (context == null || context.isEmpty()) return "";
        
        int estimatedTokens = estimateTokens(context);
        if (estimatedTokens <= maxContextTokens) {
            return context;
        }

        log.info("[TokenOptimizer] 裁剪上下文: {} -> {} tokens", estimatedTokens, maxContextTokens);
        
        // 按比例裁剪
        double ratio = (double) maxContextTokens / estimatedTokens;
        int targetLength = (int) (context.length() * ratio);
        
        // 保留开头（重要信息）和结尾（最新信息）
        int headLength = targetLength / 3;
        int tailLength = targetLength - headLength;
        
        String head = context.substring(0, Math.min(headLength, context.length()));
        String tail = context.substring(Math.max(headLength, context.length() - tailLength));
        
        return head + "\n... [中间内容已裁剪以节省 token] ...\n" + tail;
    }

    /**
     * 按优先级裁剪多个上下文片段
     */
    public String trimMultipleContexts(List<ContextSegment> segments, int maxTotalTokens) {
        // 按优先级排序（高优先级保留更多）
        segments.sort((a, b) -> Integer.compare(b.priority(), a.priority()));
        
        StringBuilder result = new StringBuilder();
        int usedTokens = 0;
        
        for (ContextSegment segment : segments) {
            int availableTokens = maxTotalTokens - usedTokens;
            if (availableTokens <= 0) break;
            
            String trimmed = trimContext(segment.content(), availableTokens);
            result.append(trimmed).append("\n\n");
            usedTokens += estimateTokens(trimmed);
        }
        
        return result.toString();
    }

    /**
     * 估算 token 数
     */
    public int estimateTokens(String text) {
        if (text == null) return 0;
        int chineseChars = 0;
        int englishWords = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseChars++;
            } else if (Character.isLetter(c) || Character.isDigit(c)) {
                englishWords++;
            }
        }
        
        return chineseChars / 2 + englishWords / 4 + 1;
    }

    public record ContextSegment(String title, String content, int priority) {}
}
