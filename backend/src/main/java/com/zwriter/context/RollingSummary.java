package com.zwriter.context;

import com.zwriter.llm.LlmService;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 滚动摘要
 * 当对话过长时，自动压缩历史对话为摘要
 */
@Slf4j
public class RollingSummary {

    private final Path summaryFile;
    private final LlmService llmService;
    private final int compressThreshold; // 触发压缩的 token 阈值

    public RollingSummary(Path workspacePath, LlmService llmService, int compressThreshold) {
        this.summaryFile = workspacePath.resolve(".context").resolve("rolling_summary.md");
        this.llmService = llmService;
        this.compressThreshold = compressThreshold;
    }

    /**
     * 检查并执行压缩
     */
    public boolean compressIfNeeded(ContextManager contextManager) throws IOException {
        if (!contextManager.needsCompression()) {
            return false;
        }

        int totalTokens = contextManager.getTotalTokens();
        if (totalTokens < compressThreshold) {
            return false;
        }

        log.info("[RollingSummary] 开始压缩，当前 token: {}", totalTokens);

        // 获取最近的对话
        List<ContextManager.ConversationEntry> recent = contextManager.getRecent(10);
        String recentHistory = recent.stream()
                .map(e -> e.getRole() + ": " + e.getContent())
                .collect(Collectors.joining("\n"));

        // 调用 LLM 生成摘要
        String summary = generateSummary(recentHistory);

        // 保存摘要
        Files.createDirectories(summaryFile.getParent());
        Files.writeString(summaryFile, summary + "\n\n---\n\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);

        // 清空旧历史（保留最近 4 轮 -> 2 轮，每轮=2条消息）
        List<ContextManager.ConversationEntry> keep = contextManager.getRecent(2);
        contextManager.clear();
        for (ContextManager.ConversationEntry entry : keep) {
            contextManager.addMessage(entry.getRole(), entry.getContent());
        }

        log.info("[RollingSummary] 压缩完成，摘要已保存");
        return true;
    }

    /**
     * 生成摘要
     */
    private String generateSummary(String recentHistory) {
        String prompt = """
                请对以下对话历史进行摘要：
                
                %s
                
                要求：
                1. 提取关键信息和决策
                2. 保留重要设定和约定
                3. 删除冗余和重复内容
                4. 输出简洁的 Markdown 格式
                
                摘要内容：
                """.formatted(recentHistory);

        return llmService.chat(prompt, "你是一个高效的对话摘要助手。请提取关键信息，生成简洁的摘要。");
    }

    /**
     * 获取已有摘要
     */
    public String getSummary() throws IOException {
        if (!Files.exists(summaryFile)) return "";
        return Files.readString(summaryFile);
    }

    /**
     * 构建完整上下文（摘要 + 最近对话）
     */
    public String buildContext(ContextManager contextManager) throws IOException {
        StringBuilder ctx = new StringBuilder();

        // 添加摘要
        String summary = getSummary();
        if (!summary.isEmpty()) {
            ctx.append("【历史摘要】\n").append(summary).append("\n\n");
        }

        // 添加最近对话
        List<ContextManager.ConversationEntry> recent = contextManager.getRecent(5);
        if (!recent.isEmpty()) {
            ctx.append("【最近对话】\n");
            for (ContextManager.ConversationEntry entry : recent) {
                String content = entry.getContent();
                if (content.length() > 500) {
                    content = content.substring(0, 500) + "...";
                }
                ctx.append(entry.getRole()).append(": ").append(content).append("\n");
            }
        }

        return ctx.toString();
    }
}
