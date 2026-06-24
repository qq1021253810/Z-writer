package com.zwriter.rag;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * L3 素材库（与 CLI l3_material_store.rs 对齐）
 * 存储写作素材、金句、情节模板，支持向量检索
 */
@Slf4j
public class MaterialStore {

    private final Path indexDir;
    private final Path filePath;
    private final ObjectMapper mapper = new ObjectMapper();

    public MaterialStore(Path workspacePath) {
        this.indexDir = workspacePath.resolve("vector_store");
        this.filePath = indexDir.resolve("materials.json");
    }

    /**
     * 加载素材列表
     */
    public List<Material> loadAll() throws IOException {
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }
        return mapper.readValue(filePath.toFile(), new TypeReference<>() {});
    }

    /**
     * 保存素材列表
     */
    public void saveAll(List<Material> materials) throws IOException {
        Files.createDirectories(indexDir);
        mapper.writerWithDefaultPrettyPrinter().writeValue(filePath.toFile(), materials);
    }

    /**
     * 添加素材
     */
    public void add(Material material) throws IOException {
        List<Material> materials = loadAll();
        materials.add(material);
        saveAll(materials);
        log.info("[MaterialStore] 添加素材: {}", material.getId());
    }

    /**
     * 删除素材
     */
    public boolean remove(String id) throws IOException {
        List<Material> materials = loadAll();
        boolean removed = materials.removeIf(m -> m.getId().equals(id));
        if (removed) {
            saveAll(materials);
            log.info("[MaterialStore] 删除素材: {}", id);
        }
        return removed;
    }

    /**
     * 向量检索（余弦相似度）
     */
    public List<Material> search(float[] queryEmbedding, int topK) throws IOException {
        List<Material> materials = loadAll();
        if (materials.isEmpty() || topK <= 0) {
            return List.of();
        }

        // 对标 Rust: 计算相似度，使用 select_nth_unstable_by 的思想
        List<ScoredMaterial> scored = materials.stream()
                .map(m -> new ScoredMaterial(m, cosineSimilarity(queryEmbedding, m.getEmbedding())))
                .filter(s -> s.score > 0) // 只保留有效相似度
                .sorted(Comparator.comparingDouble(s -> -s.score))
                .limit(topK)
                .toList();

        return scored.stream().map(s -> s.material).toList();
    }

    /**
     * 按分类搜索
     */
    public List<Material> searchByCategory(MaterialCategory category, int topK) throws IOException {
        List<Material> materials = loadAll();
        return materials.stream()
                .filter(m -> m.getCategory() == category)
                .limit(topK)
                .toList();
    }

    /**
     * 按关键词搜索
     */
    public List<Material> searchByKeywords(String[] keywords, int topK) throws IOException {
        List<Material> materials = loadAll();

        List<ScoredKeyword> scored = materials.stream()
                .map(m -> {
                    int score = 0;
                    for (String kw : keywords) {
                        if (m.getMetadata().getKeywords().stream().anyMatch(k -> k.contains(kw))) {
                            score++;
                        }
                    }
                    return new ScoredKeyword(m, score);
                })
                .filter(s -> s.score > 0)
                .sorted(Comparator.comparingInt(s -> -s.score))
                .limit(topK)
                .toList();

        return scored.stream().map(s -> s.material).toList();
    }

    /**
     * 获取素材总数
     */
    public int size() throws IOException {
        return loadAll().size();
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() throws IOException {
        return loadAll().isEmpty();
    }

    /**
     * 生成素材摘要（人类可读）
     */
    public String generateSummary() throws IOException {
        List<Material> materials = loadAll();
        StringBuilder summary = new StringBuilder();
        summary.append("# 素材库摘要\n\n");

        // 按分类统计
        Map<String, Long> counts = materials.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getCategory() != null ? m.getCategory().name() : "Unknown",
                        Collectors.counting()
                ));

        summary.append("## 分类统计\n\n");
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            summary.append(String.format("- **%s**: %d 条\n", entry.getKey(), entry.getValue()));
        }
        summary.append('\n');

        // 最近添加的素材
        summary.append("## 最近素材\n\n");
        List<Material> recent = materials.reversed().stream().limit(5).toList();
        for (Material m : recent) {
            String preview = m.getContent().length() > 50
                    ? m.getContent().substring(0, 50)
                    : m.getContent();
            summary.append(String.format("- [%s] %s\n",
                    m.getCategory() != null ? m.getCategory() : "未知", preview));
        }

        return summary.toString();
    }

    /**
     * 余弦相似度计算
     */
    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0f;
        }

        float dotProduct = 0f;
        float normA = 0f;
        float normB = 0f;

        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0 || normB == 0) return 0f;
        return dotProduct / (float) (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private record ScoredMaterial(Material material, double score) {}
    private record ScoredKeyword(Material material, int score) {}

    // ==================== 数据模型 ====================

    /**
     * 素材分类
     */
    public enum MaterialCategory {
        Scenery,      // 场景描写
        Combat,       // 战斗描写
        Character,    // 人物描写
        Technique,    // 技能描写
        Inspiration,  // 灵感碎片
        Historical    // 历史章节切片
    }

    /**
     * 素材元数据
     */
    @Data
    public static class MaterialMetadata {
        private List<String> keywords = new ArrayList<>();
        private List<String> characters = new ArrayList<>();
        private List<String> locations = new ArrayList<>();
        private List<String> tags = new ArrayList<>();
    }

    /**
     * 素材
     */
    @Data
    public static class Material {
        private String id;
        private String content;
        private MaterialCategory category;
        private String source;
        private float[] embedding = new float[0];
        private MaterialMetadata metadata = new MaterialMetadata();
        private Integer sourceChapter;

        public Material() {}

        public Material(String id, String content, MaterialCategory category, String source, float[] embedding) {
            this.id = id;
            this.content = content;
            this.category = category;
            this.source = source;
            this.embedding = embedding;
            this.metadata = new MaterialMetadata();
        }

        public Material withKeywords(List<String> keywords) {
            this.metadata.setKeywords(keywords);
            return this;
        }

        public Material withCharacters(List<String> characters) {
            this.metadata.setCharacters(characters);
            return this;
        }

        public Material withLocations(List<String> locations) {
            this.metadata.setLocations(locations);
            return this;
        }

        public Material withTags(List<String> tags) {
            this.metadata.setTags(tags);
            return this;
        }

        public Material withSourceChapter(int chapter) {
            this.sourceChapter = chapter;
            return this;
        }

        // Convenience setters that delegate to metadata
        public void setKeywords(List<String> keywords) {
            this.metadata.setKeywords(keywords);
        }

        public List<String> getKeywords() {
            return this.metadata.getKeywords();
        }

        public void setTags(List<String> tags) {
            this.metadata.setTags(tags);
        }

        public List<String> getTags() {
            return this.metadata.getTags();
        }
    }
}
