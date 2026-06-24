package com.zwriter.rag;

import com.zwriter.llm.LlmService;
import com.zwriter.workspace.WorkspaceManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/**
 * RAG 服务（L2 + L3 的统一入口）
 */
@Slf4j
@Service
public class RagService {

    @Autowired
    private WorkspaceManager workspaceManager;

    @Autowired
    private LlmService llmService;

    /**
     * 获取记忆树
     */
    public MemoryTree getMemoryTree(String novelName) throws IOException {
        Path workspacePath = workspaceManager.openNovel(novelName).getRoot();
        return new MemoryTree(workspacePath);
    }

    /**
     * 获取素材库
     */
    public MaterialStore getMaterialStore(String novelName) throws IOException {
        Path workspacePath = workspaceManager.openNovel(novelName).getRoot();
        return new MaterialStore(workspacePath);
    }

    /**
     * 构建完整的 RAG 上下文（记忆树 + 相关素材）
     */
    public String buildRagContext(String novelName, String query) throws IOException {
        StringBuilder context = new StringBuilder();

        // L2: 记忆树上下文
        try {
            MemoryTree memoryTree = getMemoryTree(novelName);
            String memoryContext = memoryTree.buildContext();
            if (!memoryContext.isEmpty()) {
                context.append(memoryContext);
            }
        } catch (Exception e) {
            log.warn("[RagService] 加载记忆树失败: {}", e.getMessage());
        }

        // L3: 向量检索相关素材
        try {
            float[] queryEmbedding = llmService.embed(query);
            if (queryEmbedding.length > 0) {
                MaterialStore materialStore = getMaterialStore(novelName);
                List<MaterialStore.Material> materials = materialStore.search(queryEmbedding, 5);
                if (!materials.isEmpty()) {
                    context.append("【相关素材】\n");
                    for (MaterialStore.Material m : materials) {
                        String preview = m.getContent().length() > 200
                                ? m.getContent().substring(0, 200)
                                : m.getContent();
                        context.append("- [").append(m.getCategory()).append("] ")
                               .append(m.getContent().substring(0, Math.min(200, m.getContent().length())))
                               .append("\n");
                    }
                    context.append("\n");
                }
            }
        } catch (Exception e) {
            log.warn("[RagService] 素材检索失败，回退到关键词搜索: {}", e.getMessage());
            // 回退到关键词搜索
            try {
                MaterialStore materialStore = getMaterialStore(novelName);
                List<MaterialStore.Material> materials = materialStore.searchByKeywords(
                        new String[]{query}, 5);
                if (!materials.isEmpty()) {
                    context.append("【相关素材（关键词）】\n");
                    for (MaterialStore.Material m : materials) {
                        context.append("- [").append(m.getCategory()).append("] ")
                               .append(m.getContent().substring(0, Math.min(200, m.getContent().length())))
                               .append("\n");
                    }
                    context.append("\n");
                }
            } catch (Exception ex) {
                log.warn("[RagService] 关键词搜索也失败: {}", ex.getMessage());
            }
        }

        return context.toString();
    }

    /**
     * 添加素材（自动向量化）
     */
    public void addMaterial(String novelName, String content, MaterialStore.MaterialCategory category,
                           List<String> keywords, List<String> tags) throws IOException {
        MaterialStore materialStore = getMaterialStore(novelName);

        // 向量化
        float[] embedding = llmService.embed(content);

        MaterialStore.Material material = new MaterialStore.Material(
                UUID.randomUUID().toString(),
                content,
                category,
                novelName,
                embedding
        );

        if (keywords != null) {
            material.setKeywords(keywords);
        }
        if (tags != null) {
            material.setTags(tags);
        }

        materialStore.add(material);
    }

    /**
     * 召回剧情上下文（记忆树）
     */
    public String recallPlot(String novelName, int currentChapter, int lookback) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        return memoryTree.recallPlot(currentChapter, lookback);
    }

    /**
     * 添加卷摘要
     */
    public void addVolume(String novelName, int volumeNum, String title, String summary) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        MemoryTree.VolumeSummary volume = new MemoryTree.VolumeSummary(volumeNum, title, summary);
        memoryTree.addVolume(volume);
    }

    /**
     * 添加章节摘要
     */
    public void addChapterSummary(String novelName, int volumeNum, int chapterNum,
                                  String title, String summary, List<String> keyEvents) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        MemoryTree.MemoryTreeData data = memoryTree.load();

        // 找到对应卷
        MemoryTree.VolumeSummary volume = data.getVolumes().stream()
                .filter(v -> v.getVolumeNum() == volumeNum)
                .findFirst()
                .orElseThrow(() -> new IOException("未找到卷: " + volumeNum));

        MemoryTree.ChapterSummary chapter = new MemoryTree.ChapterSummary(chapterNum, title, summary);
        if (keyEvents != null) {
            for (String event : keyEvents) {
                chapter.addKeyEvent(event);
            }
        }
        volume.addChapter(chapter);
        memoryTree.save(data);
    }

    /**
     * 添加伏笔
     */
    public void addForeshadow(String novelName, String id, String description, int plantedChapter) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        MemoryTree.Foreshadow foreshadow = new MemoryTree.Foreshadow(id, description, plantedChapter);
        memoryTree.addForeshadow(foreshadow);
    }

    /**
     * 更新伏笔状态
     */
    public boolean updateForeshadow(String novelName, String id, MemoryTree.ForeshadowStatus status,
                                    Integer resolveChapter) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        return memoryTree.updateForeshadow(id, status, resolveChapter);
    }

    /**
     * 生成伏笔追踪报告
     */
    public String generateForeshadowReport(String novelName) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        return memoryTree.generateForeshadowReport();
    }

    /**
     * 生成剧情时间线
     */
    public String generateTimeline(String novelName) throws IOException {
        MemoryTree memoryTree = getMemoryTree(novelName);
        return memoryTree.generateTimeline();
    }

    /**
     * 搜索素材（向量）
     */
    public List<MaterialStore.Material> searchMaterials(String novelName, String query, int topK) throws IOException {
        MaterialStore materialStore = getMaterialStore(novelName);

        // 尝试向量检索
        try {
            float[] embedding = llmService.embed(query);
            if (embedding.length > 0) {
                List<MaterialStore.Material> results = materialStore.search(embedding, topK);
                if (!results.isEmpty()) {
                    return results;
                }
            }
        } catch (Exception e) {
            log.warn("[RagService] 向量检索失败: {}", e.getMessage());
        }

        // 回退到关键词搜索
        return materialStore.searchByKeywords(new String[]{query}, topK);
    }

    /**
     * 删除素材
     */
    public boolean removeMaterial(String novelName, String materialId) throws IOException {
        MaterialStore materialStore = getMaterialStore(novelName);
        return materialStore.remove(materialId);
    }

    /**
     * 获取素材库摘要
     */
    public String getMaterialStoreSummary(String novelName) throws IOException {
        MaterialStore materialStore = getMaterialStore(novelName);
        return materialStore.generateSummary();
    }
}
