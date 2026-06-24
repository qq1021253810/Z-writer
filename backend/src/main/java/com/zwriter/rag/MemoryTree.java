package com.zwriter.rag;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * L2 记忆树（与 CLI l2_memory_tree.rs 对齐）
 * 分层存储小说的全局设定、世界观、角色关系等
 */
@Slf4j
public class MemoryTree {

    private final Path filePath;
    private final ObjectMapper mapper = new ObjectMapper();

    public MemoryTree(Path workspacePath) {
        this.filePath = workspacePath.resolve("memory_tree.json");
    }

    /**
     * 加载记忆树
     */
    public MemoryTreeData load() throws IOException {
        if (!Files.exists(filePath)) {
            MemoryTreeData empty = new MemoryTreeData();
            empty.setNovelId("");
            return empty;
        }
        return mapper.readValue(filePath.toFile(), MemoryTreeData.class);
    }

    /**
     * 保存记忆树
     */
    public void save(MemoryTreeData data) throws IOException {
        Files.createDirectories(filePath.getParent());
        mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), data);
    }

    /**
     * 添加卷
     */
    public void addVolume(VolumeSummary volume) throws IOException {
        MemoryTreeData data = load();
        data.getVolumes().add(volume);
        save(data);
    }

    /**
     * 获取卷摘要
     */
    public Optional<VolumeSummary> getVolume(int volumeNum) throws IOException {
        MemoryTreeData data = load();
        return data.getVolumes().stream()
                .filter(v -> v.getVolumeNum() == volumeNum)
                .findFirst();
    }

    /**
     * 获取章节摘要
     */
    public Optional<ChapterSummary> getChapter(int volumeNum, int chapterNum) throws IOException {
        MemoryTreeData data = load();
        return data.getVolumes().stream()
                .filter(v -> v.getVolumeNum() == volumeNum)
                .flatMap(v -> v.getChapters().stream())
                .filter(c -> c.getChapterNum() == chapterNum)
                .findFirst();
    }

    /**
     * 召回剧情上下文（对标 Rust recall_plot）
     */
    public String recallPlot(int currentChapter, int lookback) throws IOException {
        MemoryTreeData data = load();
        StringBuilder result = new StringBuilder();

        // 找到当前章节所在的卷
        Optional<VolumeSummary> currentVolume = data.getVolumes().stream()
                .filter(v -> v.getChapters().stream().anyMatch(c -> c.getChapterNum() == currentChapter))
                .findFirst();

        if (currentVolume.isPresent()) {
            VolumeSummary vol = currentVolume.get();
            result.append(String.format("【第 %d 卷：%s】\n", vol.getVolumeNum(), vol.getTitle()));
            result.append(vol.getSummary()).append("\n\n");

            // 召回最近 N 章的摘要
            int start = Math.max(1, currentChapter - lookback);
            List<ChapterSummary> chapters = vol.getChapters().stream()
                    .filter(c -> c.getChapterNum() >= start && c.getChapterNum() < currentChapter)
                    .toList();

            if (!chapters.isEmpty()) {
                result.append("【最近章节】\n");
                for (ChapterSummary chapter : chapters) {
                    result.append(String.format("第 %d 章：%s\n", chapter.getChapterNum(), chapter.getTitle()));
                    result.append(chapter.getSummary()).append("\n");

                    if (!chapter.getKeyEvents().isEmpty()) {
                        result.append("关键事件：\n");
                        for (String event : chapter.getKeyEvents()) {
                            result.append("  - ").append(event).append("\n");
                        }
                    }
                    result.append('\n');
                }
            }
        }

        // 召回活跃伏笔
        List<Foreshadow> activeForeshadows = data.getForeshadows().stream()
                .filter(f -> f.getStatus() == ForeshadowStatus.Active)
                .toList();

        if (!activeForeshadows.isEmpty()) {
            result.append("【活跃伏笔】\n");
            for (Foreshadow f : activeForeshadows) {
                result.append(String.format("- %s（第 %d 章埋下）\n",
                        f.getDescription(), f.getPlantedChapter()));
            }
        }

        return result.toString();
    }

    /**
     * 添加伏笔
     */
    public void addForeshadow(Foreshadow foreshadow) throws IOException {
        MemoryTreeData data = load();
        data.getForeshadows().add(foreshadow);
        save(data);
    }

    /**
     * 更新伏笔状态
     */
    public boolean updateForeshadow(String id, ForeshadowStatus status, Integer resolveChapter) throws IOException {
        MemoryTreeData data = load();
        Optional<Foreshadow> opt = data.getForeshadows().stream()
                .filter(f -> f.getId().equals(id))
                .findFirst();
        if (opt.isPresent()) {
            Foreshadow f = opt.get();
            f.setStatus(status);
            if (resolveChapter != null) {
                f.setActualResolve(resolveChapter);
            }
            save(data);
            return true;
        }
        return false;
    }

    /**
     * 获取活跃伏笔
     */
    public List<Foreshadow> getActiveForeshadows() throws IOException {
        MemoryTreeData data = load();
        return data.getForeshadows().stream()
                .filter(f -> f.getStatus() == ForeshadowStatus.Active)
                .toList();
    }

    /**
     * 构建记忆上下文（用于 LLM prompt）
     */
    public String buildContext() throws IOException {
        MemoryTreeData data = load();
        StringBuilder ctx = new StringBuilder();

        if (!data.getVolumes().isEmpty()) {
            ctx.append("【卷记忆】\n");
            for (VolumeSummary vol : data.getVolumes()) {
                ctx.append("- ").append(vol.getTitle()).append(": ").append(vol.getSummary()).append("\n");
            }
            ctx.append("\n");
        }

        List<Foreshadow> active = getActiveForeshadows();
        if (!active.isEmpty()) {
            ctx.append("【活跃伏笔】\n");
            for (Foreshadow f : active) {
                ctx.append("- ").append(f.getDescription()).append(" (埋设: ").append(f.getPlantedChapter()).append("章)\n");
            }
            ctx.append("\n");
        }

        return ctx.toString();
    }

    /**
     * 生成伏笔追踪表（人类可读）
     */
    public String generateForeshadowReport() throws IOException {
        MemoryTreeData data = load();
        StringBuilder report = new StringBuilder();
        report.append("# 伏笔追踪表\n\n");

        List<Foreshadow> active = data.getForeshadows().stream()
                .filter(f -> f.getStatus() == ForeshadowStatus.Active)
                .toList();
        List<Foreshadow> resolved = data.getForeshadows().stream()
                .filter(f -> f.getStatus() == ForeshadowStatus.Resolved)
                .toList();

        if (!active.isEmpty()) {
            report.append("## 活跃伏笔\n\n");
            for (Foreshadow f : active) {
                report.append(String.format("- **%s**（第 %d 章埋下）\n", f.getDescription(), f.getPlantedChapter()));
                if (f.getExpectedResolve() != null) {
                    report.append(String.format("  - 预计回收：第 %d 章\n", f.getExpectedResolve()));
                }
            }
            report.append('\n');
        }

        if (!resolved.isEmpty()) {
            report.append("## 已回收伏笔\n\n");
            for (Foreshadow f : resolved) {
                report.append(String.format("- ~~%s~~（第 %d 章埋下，第 %d 章回收）\n",
                        f.getDescription(), f.getPlantedChapter(),
                        f.getActualResolve() != null ? f.getActualResolve() : 0));
            }
        }

        return report.toString();
    }

    /**
     * 生成时间线（人类可读）
     */
    public String generateTimeline() throws IOException {
        MemoryTreeData data = load();
        StringBuilder timeline = new StringBuilder();
        timeline.append("# 剧情时间线\n\n");

        for (VolumeSummary volume : data.getVolumes()) {
            timeline.append(String.format("## 第 %d 卷：%s\n\n", volume.getVolumeNum(), volume.getTitle()));
            timeline.append(volume.getSummary()).append("\n\n");

            for (ChapterSummary chapter : volume.getChapters()) {
                timeline.append(String.format("### 第 %d 章：%s\n\n", chapter.getChapterNum(), chapter.getTitle()));
                timeline.append(chapter.getSummary()).append("\n\n");

                if (!chapter.getKeyEvents().isEmpty()) {
                    timeline.append("**关键事件：**\n");
                    for (String event : chapter.getKeyEvents()) {
                        timeline.append("- ").append(event).append("\n");
                    }
                    timeline.append('\n');
                }
            }
        }

        return timeline.toString();
    }

    // ==================== 数据模型 ====================

    @Data
    public static class MemoryTreeData {
        private String novelId = "";
        private List<VolumeSummary> volumes = new ArrayList<>();
        private List<Foreshadow> foreshadows = new ArrayList<>();
    }

    @Data
    public static class VolumeSummary {
        private int volumeNum;
        private String title;
        private String summary;
        private List<ChapterSummary> chapters = new ArrayList<>();

        public VolumeSummary() {}

        public VolumeSummary(int volumeNum, String title, String summary) {
            this.volumeNum = volumeNum;
            this.title = title;
            this.summary = summary;
        }

        public void addChapter(ChapterSummary chapter) {
            this.chapters.add(chapter);
        }
    }

    @Data
    public static class ChapterSummary {
        private int chapterNum;
        private String title;
        private String summary;
        private List<String> keyEvents = new ArrayList<>();
        private List<CharacterChange> characterChanges = new ArrayList<>();

        public ChapterSummary() {}

        public ChapterSummary(int chapterNum, String title, String summary) {
            this.chapterNum = chapterNum;
            this.title = title;
            this.summary = summary;
        }

        public void addKeyEvent(String event) {
            this.keyEvents.add(event);
        }

        public void addCharacterChange(CharacterChange change) {
            this.characterChanges.add(change);
        }
    }

    @Data
    public static class CharacterChange {
        private String characterName;
        private String changeType; // 位置、情感、关系、物品、能力层级等
        private String before;
        private String after;

        public CharacterChange() {}

        public CharacterChange(String characterName, String changeType, String before, String after) {
            this.characterName = characterName;
            this.changeType = changeType;
            this.before = before;
            this.after = after;
        }
    }

    @Data
    public static class Foreshadow {
        private String id;
        private String description;
        private int plantedChapter;
        private Integer expectedResolve;
        private Integer actualResolve;
        private ForeshadowStatus status = ForeshadowStatus.Active;

        public Foreshadow() {}

        public Foreshadow(String id, String description, int plantedChapter) {
            this.id = id;
            this.description = description;
            this.plantedChapter = plantedChapter;
            this.status = ForeshadowStatus.Active;
        }
    }

    public enum ForeshadowStatus {
        Active,     // 活跃（已埋下未回收）
        Resolved,   // 已回收
        Abandoned   // 已放弃
    }
}
