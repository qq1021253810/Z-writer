package com.zwriter.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 角色追踪器（与 CLI character_tracker.rs 对齐）
 * 追踪角色状态变化，检测设定矛盾
 */
@Slf4j
public class CharacterTracker {

    private final Path trackerFile;
    private final ObjectMapper mapper = new ObjectMapper();

    public CharacterTracker(Path workspacePath) {
        this.trackerFile = workspacePath.resolve(".context").resolve("character_states.json");
    }

    /**
     * 加载角色状态
     */
    public Map<String, CharacterState> load() throws IOException {
        if (!Files.exists(trackerFile)) {
            return new HashMap<>();
        }
        return mapper.readValue(trackerFile.toFile(), 
                mapper.getTypeFactory().constructMapType(Map.class, String.class, CharacterState.class));
    }

    /**
     * 保存角色状态
     */
    public void save(Map<String, CharacterState> states) throws IOException {
        Files.createDirectories(trackerFile.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(trackerFile.toFile(), states);
    }

    /**
     * 记录角色状态变更
     */
    public void recordChange(String characterName, String change, String chapterNum) throws IOException {
        Map<String, CharacterState> states = load();
        CharacterState state = states.computeIfAbsent(characterName, CharacterState::new);
        
        state.getChanges().add(new StateChange(change, chapterNum, new Date().toString()));
        save(states);
        
        log.info("[CharacterTracker] 记录变更: {} - {}", characterName, change);
    }

    /**
     * 检测矛盾
     */
    public List<String> detectConflicts() throws IOException {
        Map<String, CharacterState> states = load();
        List<String> conflicts = new ArrayList<>();

        for (Map.Entry<String, CharacterState> entry : states.entrySet()) {
            String name = entry.getKey();
            CharacterState state = entry.getValue();
            
            // 检测属性矛盾
            for (int i = 0; i < state.getAttributes().size(); i++) {
                for (int j = i + 1; j < state.getAttributes().size(); j++) {
                    Attribute a1 = state.getAttributes().get(i);
                    Attribute a2 = state.getAttributes().get(j);
                    if (a1.key().equals(a2.key()) && !a1.value().equals(a2.value())) {
                        conflicts.add("角色 '%s' 属性 '%s' 矛盾: '%s' vs '%s'"
                                .formatted(name, a1.key(), a1.value(), a2.value()));
                    }
                }
            }
        }

        return conflicts;
    }

    /**
     * 获取角色摘要（用于 LLM prompt）
     */
    public String buildCharacterContext() throws IOException {
        Map<String, CharacterState> states = load();
        if (states.isEmpty()) return "";

        StringBuilder ctx = new StringBuilder("【角色状态追踪】\n");
        for (Map.Entry<String, CharacterState> entry : states.entrySet()) {
            ctx.append("角色: ").append(entry.getKey()).append("\n");
            if (!entry.getValue().getAttributes().isEmpty()) {
                ctx.append("  属性: ");
                ctx.append(entry.getValue().getAttributes().stream()
                        .map(a -> a.key() + "=" + a.value())
                        .collect(Collectors.joining(", ")));
                ctx.append("\n");
            }
            if (!entry.getValue().getChanges().isEmpty()) {
                List<StateChange> recent = entry.getValue().getChanges().stream()
                        .skip(Math.max(0, entry.getValue().getChanges().size() - 5))
                        .toList();
                ctx.append("  最近变更:\n");
                for (StateChange c : recent) {
                    ctx.append("    - ").append(c.change()).append(" (第 ").append(c.chapterNum()).append(" 章)\n");
                }
            }
            ctx.append("\n");
        }

        return ctx.toString();
    }

    // ==================== 数据模型 ====================

    @Data
    public static class CharacterState {
        private String name;
        private List<Attribute> attributes = new ArrayList<>();
        private List<StateChange> changes = new ArrayList<>();

        public CharacterState() {}
        public CharacterState(String name) {
            this.name = name;
        }

        public void setAttribute(String key, String value) {
            attributes.removeIf(a -> a.key().equals(key));
            attributes.add(new Attribute(key, value));
        }
    }

    public record Attribute(String key, String value) {}
    public record StateChange(String change, String chapterNum, String timestamp) {}
}
