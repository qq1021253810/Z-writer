package com.zwriter.service;

import com.zwriter.entity.ChapterContent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 向量数据库服务
 * 负责章节内容的向量化存储与语义检索
 */
@Slf4j
@Service
public class VectorDbService {

    @Autowired
    private VectorStore vectorStore;

    /**
     * 存储章节内容到向量数据库
     */
    public void storeChapter(Long novelId, Integer volumeNumber, Integer chapterNumber,
                             String content, String summary) {
        log.info("存储章节向量: novelId={}, volume={}, chapter={}",
                novelId, volumeNumber, chapterNumber);

        try {
            String docId = String.format("novel_%d_v%d_c%d", novelId, volumeNumber, chapterNumber);

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("novelId", novelId);
            metadata.put("volumeNumber", volumeNumber);
            metadata.put("chapterNumber", chapterNumber);
            metadata.put("type", "chapter_content");
            metadata.put("summary", summary != null ? summary : "");

            Document document = new Document(docId, content, metadata);
            vectorStore.add(List.of(document));

            log.info("成功存储章节向量: docId={}", docId);
        } catch (Exception e) {
            log.error("存储章节向量失败", e);
        }
    }

    /**
     * 语义检索相关章节
     */
    public List<Map<String, Object>> searchRelevantChapters(Long novelId, String query, int topK) {
        log.info("语义检索: novelId={}, query={}, topK={}", novelId, query, topK);

        try {
            List<Document> documents = vectorStore.similaritySearch(query);

            return documents.stream()
                    .limit(topK)
                    .map(doc -> {
                        Map<String, Object> result = new HashMap<>();
                        result.put("content", doc.getText());
                        result.put("metadata", doc.getMetadata());
                        result.put("score", doc.getScore());
                        return result;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("语义检索失败", e);
            return List.of();
        }
    }

    /**
     * 检索角色相关段落
     */
    public List<Map<String, Object>> searchCharacterMentions(Long novelId, String characterName, int topK) {
        log.info("检索角色出场: novelId={}, characterName={}, topK={}",
                novelId, characterName, topK);

        String query = characterName + " 出场 对话 行动";
        return searchRelevantChapters(novelId, query, topK);
    }

    /**
     * 检索伏笔相关段落
     */
    public List<Map<String, Object>> searchForeshadowing(Long novelId, String foreshadowKeyword, int topK) {
        log.info("检索伏笔: novelId={}, keyword={}, topK={}",
                novelId, foreshadowKeyword, topK);

        String query = foreshadowKeyword + " 伏笔 暗示 铺垫";
        return searchRelevantChapters(novelId, query, topK);
    }

    /**
     * 删除小说的所有向量数据
     */
    public void deleteNovelVectors(Long novelId) {
        log.info("删除小说向量数据: novelId={}", novelId);
        // VectorStore 暂不支持按条件删除，需要实现自定义逻辑
        log.warn("删除功能待实现，需要扩展 VectorStore");
    }
}
