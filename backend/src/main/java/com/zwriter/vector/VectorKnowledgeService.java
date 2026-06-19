package com.zwriter.vector;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 向量知识库服务 - 管理小说相关的向量存储与检索
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VectorKnowledgeService {

    private final VectorStore vectorStore;

    // Collection 名称常量（作为 metadata 区分）
    private static final String COLLECTION_PARAGRAPHS = "novel_paragraphs";
    private static final String COLLECTION_EXAMPLES = "genre_examples";
    private static final String COLLECTION_HOOKS = "hook_templates";
    private static final String COLLECTION_USER_PREFS = "user_preferences";

    // 缓存机制（简单实现，生产环境建议使用 Redis 或 Caffeine）
    private final Map<String, List<QueryResult>> queryCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 1000;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟
    private final Map<String, Long> cacheTimestamps = new ConcurrentHashMap<>();

    /**
     * 存储章节段落向量
     */
    public void storeChapterParagraphs(Long novelId, Integer chapterNumber, List<String> paragraphs) {
        List<Document> documents = new ArrayList<>();

        for (int i = 0; i < paragraphs.size(); i++) {
            String id = "novel_" + novelId + "_ch" + chapterNumber + "_p" + i;
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("novel_id", novelId);
            metadata.put("chapter", chapterNumber);
            metadata.put("paragraph_index", i);
            metadata.put("collection", COLLECTION_PARAGRAPHS);
            
            Document doc = new Document(id, paragraphs.get(i), metadata);
            documents.add(doc);
        }

        vectorStore.add(documents);
        log.info("[VectorKnowledge] 存储 {} 个段落到 {}", documents.size(), COLLECTION_PARAGRAPHS);
    }

    /**
     * 检索相关段落（用于续写时获取前文参考）- 带缓存
     */
    public List<QueryResult> retrieveRelatedParagraphs(String query, int topK) {
        String cacheKey = "paragraphs:" + query + ":" + topK;
        
        // 检查缓存
        List<QueryResult> cached = getFromCache(cacheKey);
        if (cached != null) {
            log.debug("[VectorKnowledge] 命中缓存: {}", cacheKey);
            return cached;
        }
        
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        
        List<Document> docs = vectorStore.similaritySearch(request);
        
        List<QueryResult> results = docs.stream()
                .filter(doc -> COLLECTION_PARAGRAPHS.equals(doc.getMetadata().get("collection")))
                .map(doc -> new QueryResult(
                        doc.getId(),
                        doc.getText(),
                        0.0, // VectorStore 不直接返回距离
                        doc.getMetadata()
                ))
                .collect(Collectors.toList());
        
        // 存入缓存
        putToCache(cacheKey, results);
        return results;
    }

    /**
     * 检索同赛道范文 - 带缓存
     */
    public List<QueryResult> retrieveGenreExamples(String genre, String sceneType, int topK) {
        String cacheKey = "examples:" + genre + ":" + sceneType + ":" + topK;
        
        // 检查缓存
        List<QueryResult> cached = getFromCache(cacheKey);
        if (cached != null) {
            log.debug("[VectorKnowledge] 命中缓存: {}", cacheKey);
            return cached;
        }
        
        String query = genre + " " + sceneType;
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        
        List<Document> docs = vectorStore.similaritySearch(request);
        
        List<QueryResult> results = docs.stream()
                .filter(doc -> COLLECTION_EXAMPLES.equals(doc.getMetadata().get("collection")))
                .map(doc -> new QueryResult(
                        doc.getId(),
                        doc.getText(),
                        0.0,
                        doc.getMetadata()
                ))
                .collect(Collectors.toList());
        
        // 存入缓存
        putToCache(cacheKey, results);
        return results;
    }

    /**
     * 检索爽点/钩子模板 - 带缓存
     */
    public List<QueryResult> retrieveHookTemplates(String hookType, int topK) {
        String cacheKey = "hooks:" + hookType + ":" + topK;
        
        // 检查缓存
        List<QueryResult> cached = getFromCache(cacheKey);
        if (cached != null) {
            log.debug("[VectorKnowledge] 命中缓存: {}", cacheKey);
            return cached;
        }
        
        SearchRequest request = SearchRequest.builder()
                .query(hookType)
                .topK(topK)
                .build();
        
        List<Document> docs = vectorStore.similaritySearch(request);
        
        List<QueryResult> results = docs.stream()
                .filter(doc -> COLLECTION_HOOKS.equals(doc.getMetadata().get("collection")))
                .map(doc -> new QueryResult(
                        doc.getId(),
                        doc.getText(),
                        0.0,
                        doc.getMetadata()
                ))
                .collect(Collectors.toList());
        
        // 存入缓存
        putToCache(cacheKey, results);
        return results;
    }

    /**
     * 存储用户修改偏好
     */
    public void storeUserPreference(String userId, String preferenceType, String content) {
        String id = "user_" + userId + "_" + preferenceType + "_" + System.currentTimeMillis();
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("user_id", userId);
        metadata.put("type", preferenceType);
        metadata.put("collection", COLLECTION_USER_PREFS);
        
        Document doc = new Document(id, content, metadata);
        vectorStore.add(List.of(doc));
        log.info("[VectorKnowledge] 存储用户偏好: {}", id);
    }

    /**
     * 查询结果封装
     */
    public record QueryResult(String id, String document, double distance, Map<String, Object> metadata) {}

    /**
     * 从缓存获取
     */
    private List<QueryResult> getFromCache(String key) {
        Long timestamp = cacheTimestamps.get(key);
        if (timestamp != null && (System.currentTimeMillis() - timestamp) < CACHE_TTL_MS) {
            return queryCache.get(key);
        }
        // 过期则移除
        if (timestamp != null) {
            cacheTimestamps.remove(key);
            queryCache.remove(key);
        }
        return null;
    }

    /**
     * 存入缓存
     */
    private void putToCache(String key, List<QueryResult> results) {
        // 清理过期缓存
        if (cacheTimestamps.size() >= MAX_CACHE_SIZE) {
            cleanExpiredCache();
        }
        queryCache.put(key, results);
        cacheTimestamps.put(key, System.currentTimeMillis());
    }

    /**
     * 清理过期缓存
     */
    private void cleanExpiredCache() {
        long now = System.currentTimeMillis();
        List<String> expiredKeys = cacheTimestamps.entrySet().stream()
                .filter(entry -> (now - entry.getValue()) >= CACHE_TTL_MS)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        for (String key : expiredKeys) {
            cacheTimestamps.remove(key);
            queryCache.remove(key);
        }
        log.debug("[VectorKnowledge] 清理过期缓存 {} 条", expiredKeys.size());
    }

    /**
     * 清空缓存
     */
    public void clearCache() {
        queryCache.clear();
        cacheTimestamps.clear();
        log.info("[VectorKnowledge] 缓存已清空");
    }
}
