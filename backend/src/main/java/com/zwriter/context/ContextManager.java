package com.zwriter.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

/**
 * 上下文管理器（与 CLI context_manager.rs 对齐）
 * 管理对话历史，维护上下文窗口
 */
@Slf4j
public class ContextManager {

    private final Path historyFile;
    private final ObjectMapper mapper;
    private final int maxRounds; // 最大对话轮数，超过则触发压缩

    public ContextManager(Path workspacePath, int maxRounds) {
        this.historyFile = workspacePath.resolve(".context").resolve("conversation.jsonl");
        this.maxRounds = maxRounds;
        this.mapper = new ObjectMapper();
        this.mapper.findAndRegisterModules();
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    /**
     * 添加对话消息
     */
    public void addMessage(String role, String content) throws IOException {
        Files.createDirectories(historyFile.getParent());
        
        ConversationEntry entry = new ConversationEntry(
                role,
                content,
                String.valueOf(java.time.LocalDateTime.now()),
                estimateTokens(content)
        );
        
        String json = mapper.writeValueAsString(entry);
        Files.writeString(historyFile, json + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        
        log.debug("[ContextManager] 添加消息: role={}, tokens={}", role, entry.getTokens());
    }

    /**
     * 获取最近 N 轮对话
     */
    public List<ConversationEntry> getRecent(int count) throws IOException {
        if (!Files.exists(historyFile)) {
            return List.of();
        }
        
        List<String> lines = Files.readAllLines(historyFile);
        List<ConversationEntry> entries = new ArrayList<>();
        
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                try {
                    entries.add(mapper.readValue(line, ConversationEntry.class));
                } catch (Exception e) {
                    log.warn("[ContextManager] 解析失败: {}", e.getMessage());
                }
            }
        }
        
        int start = Math.max(0, entries.size() - count * 2); // 每轮 = user + assistant
        return entries.subList(start, entries.size());
    }

    /**
     * 检查是否需要压缩
     */
    public boolean needsCompression() throws IOException {
        if (!Files.exists(historyFile)) return false;
        long lineCount = Files.lines(historyFile).count();
        return lineCount > maxRounds * 2;
    }

    /**
     * 获取总 token 数
     */
    public int getTotalTokens() throws IOException {
        if (!Files.exists(historyFile)) return 0;
        
        int total = 0;
        for (String line : Files.readAllLines(historyFile)) {
            if (!line.trim().isEmpty()) {
                try {
                    ConversationEntry entry = mapper.readValue(line, ConversationEntry.class);
                    total += entry.getTokens();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
        return total;
    }

    /**
     * 清空历史
     */
    public void clear() throws IOException {
        if (Files.exists(historyFile)) {
            Files.delete(historyFile);
        }
    }

    /**
     * 估算 token 数（简化版：中文字符 / 2 + 英文单词数）
     */
    private int estimateTokens(String text) {
        if (text == null) return 0;
        int chineseChars = 0;
        int englishWords = 0;
        
        for (char c : text.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fff') {
                chineseChars++;
            } else if (Character.isLetter(c)) {
                englishWords++;
            }
        }
        
        return chineseChars / 2 + englishWords / 4 + 1;
    }

    @Data
    public static class ConversationEntry {
        private String role;
        private String content;
        private String timestamp;
        private int tokens;

        public ConversationEntry() {}
        public ConversationEntry(String role, String content, String timestamp, int tokens) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
            this.tokens = tokens;
        }
    }
}
