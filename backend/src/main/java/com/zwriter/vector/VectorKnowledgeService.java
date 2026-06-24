package com.zwriter.vector;

import com.zwriter.rag.RagService;
import com.zwriter.rag.MaterialStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 向量知识库服务（已废弃，委托给新的 RagService）
 *
 * @deprecated 使用 {@link RagService} 代替
 */
@Slf4j
@Service
@Deprecated
public class VectorKnowledgeService {

    @Autowired(required = false)
    private RagService ragService;

    /**
     * 存储章节段落向量
     */
    public void storeChapterParagraphs(Long novelId, Integer chapterNumber, List<String> paragraphs) {
        log.warn("[VectorKnowledgeService] 此方法已废弃，请使用 RagService.addMaterial");
    }

    /**
     * 检索相关段落
     */
    public List<QueryResult> retrieveRelatedParagraphs(String query, int topK) {
        log.warn("[VectorKnowledgeService] 此方法已废弃，请使用 RagService.searchMaterials");
        return List.of();
    }

    /**
     * 检索同赛道范文
     */
    public List<QueryResult> retrieveGenreExamples(String genre, String sceneType, int topK) {
        log.warn("[VectorKnowledgeService] 此方法已废弃，请使用 RagService.searchMaterials");
        return List.of();
    }

    /**
     * 检索爽点/钩子模板
     */
    public List<QueryResult> retrieveHookTemplates(String hookType, int topK) {
        log.warn("[VectorKnowledgeService] 此方法已废弃，请使用 RagService.searchMaterials");
        return List.of();
    }

    /**
     * 存储用户偏好
     */
    public void storeUserPreference(String userId, String preferenceType, String content) {
        log.warn("[VectorKnowledgeService] 此方法已废弃");
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        log.info("[VectorKnowledgeService] clearCache 为空操作");
    }

    // ==================== 委托给 RagService 的便捷方法 ====================

    /**
     * 添加素材（委托给 RagService）
     */
    public void addMaterial(String novelName, String content, MaterialStore.MaterialCategory category,
                           List<String> keywords, List<String> tags) throws IOException {
        if (ragService != null) {
            ragService.addMaterial(novelName, content, category, keywords, tags);
        } else {
            log.error("[VectorKnowledgeService] RagService 未注入");
        }
    }

    /**
     * 搜索素材（委托给 RagService）
     */
    public List<MaterialStore.Material> searchMaterials(String novelName, String query, int topK) throws IOException {
        if (ragService != null) {
            return ragService.searchMaterials(novelName, query, topK);
        }
        log.error("[VectorKnowledgeService] RagService 未注入");
        return List.of();
    }

    /**
     * 构建 RAG 上下文（委托给 RagService）
     */
    public String buildRagContext(String novelName, String query) throws IOException {
        if (ragService != null) {
            return ragService.buildRagContext(novelName, query);
        }
        log.error("[VectorKnowledgeService] RagService 未注入");
        return "";
    }

    /**
     * 查询结果封装
     */
    public record QueryResult(String id, String document, double distance, Map<String, Object> metadata) {}
}
