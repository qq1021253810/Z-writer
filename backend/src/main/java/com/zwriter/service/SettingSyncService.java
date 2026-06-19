package com.zwriter.service;

import com.zwriter.entity.ChapterContent;
import com.zwriter.entity.Foreshadow;
import com.zwriter.entity.PlotTimeline;
import com.zwriter.repository.ChapterContentRepository;
import com.zwriter.repository.ForeshadowRepository;
import com.zwriter.repository.PlotTimelineRepository;
import com.zwriter.vector.VectorKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 设定修改同步服务
 * 修改人设/世界观后自动同步更新全库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SettingSyncService {

    private final ForeshadowRepository foreshadowRepository;
    private final ChapterContentRepository chapterContentRepository;
    private final PlotTimelineRepository plotTimelineRepository;
    private final VectorKnowledgeService vectorKnowledgeService;
    private final VectorStore vectorStore;

    /**
     * 同步结果封装
     */
    public record SyncResult(
            int updatedForeshadows,
            int updatedChapters,
            int updatedVectorDocs,
            List<String> syncDetails
    ) {}

    /**
     * 角色名修改后同步更新所有相关数据
     *
     * @param novelId     小说ID
     * @param characterId 角色ID
     * @param oldName     旧角色名
     * @param newName     新角色名
     * @return 同步结果
     */
    @Transactional
    public SyncResult syncCharacterUpdate(Long novelId, Long characterId, String oldName, String newName) {
        log.info("[SettingSync] 开始同步角色名修改: novelId={}, characterId={}, {} -> {}", novelId, characterId, oldName, newName);

        List<String> details = new ArrayList<>();
        int updatedForeshadows = 0;
        int updatedChapters = 0;
        int updatedVectorDocs = 0;

        // 1. 更新伏笔库中包含该角色名的描述
        List<Foreshadow> foreshadows = foreshadowRepository.findByNovelId(novelId);
        for (Foreshadow foreshadow : foreshadows) {
            boolean changed = false;

            // 更新线索描述中的角色名引用
            if (foreshadow.getClueDescription() != null && foreshadow.getClueDescription().contains(oldName)) {
                foreshadow.setClueDescription(foreshadow.getClueDescription().replace(oldName, newName));
                changed = true;
            }

            // 确认 relatedCharacters 包含该角色ID（ID不变，但记录关联）
            if (foreshadow.getRelatedCharacters() != null
                    && foreshadow.getRelatedCharacters().contains(characterId)
                    && changed) {
                updatedForeshadows++;
                details.add(String.format("伏笔#%d: 更新角色名引用 '%s' -> '%s'", foreshadow.getId(), oldName, newName));
            }
        }
        foreshadowRepository.saveAll(foreshadows);

        // 2. 更新章节内容中的角色名引用
        List<ChapterContent> chapters = chapterContentRepository.findByNovelId(novelId);
        for (ChapterContent chapter : chapters) {
            if (chapter.getContent() != null && chapter.getContent().contains(oldName)) {
                String oldContent = chapter.getContent();
                String newContent = oldContent.replace(oldName, newName);
                chapter.setContent(newContent);
                updatedChapters++;
                details.add(String.format("第%d卷第%d章: 替换角色名 '%s' -> '%s'，共%d处",
                        chapter.getVolumeNumber(), chapter.getChapterNumber(),
                        oldName, newName, countOccurrences(oldContent, oldName)));
            }
        }
        chapterContentRepository.saveAll(chapters);

        // 3. 更新向量库中的角色信息
        updatedVectorDocs = updateVectorDocsForKeyword(novelId, oldName, newName);
        if (updatedVectorDocs > 0) {
            details.add(String.format("向量库: 更新 %d 个包含 '%s' 的文档", updatedVectorDocs, oldName));
        }

        log.info("[SettingSync] 角色名同步完成: 伏笔={}, 章节={}, 向量={}", updatedForeshadows, updatedChapters, updatedVectorDocs);
        return new SyncResult(updatedForeshadows, updatedChapters, updatedVectorDocs, details);
    }

    /**
     * 世界观修改后同步更新
     *
     * @param novelId  小说ID
     * @param field    修改的世界观字段名
     * @param oldValue 旧值
     * @param newValue 新值
     * @return 同步结果
     */
    @Transactional
    public SyncResult syncWorldviewUpdate(Long novelId, String field, String oldValue, String newValue) {
        log.info("[SettingSync] 开始同步世界观修改: novelId={}, field={}, '{}' -> '{}'", novelId, field, oldValue, newValue);

        List<String> details = new ArrayList<>();
        int updatedChapters = 0;
        int updatedVectorDocs = 0;

        // 1. 更新章节内容中的世界观相关描述
        List<ChapterContent> chapters = chapterContentRepository.findByNovelId(novelId);
        for (ChapterContent chapter : chapters) {
            if (chapter.getContent() != null && chapter.getContent().contains(oldValue)) {
                String oldContent = chapter.getContent();
                String newContent = oldContent.replace(oldValue, newValue);
                chapter.setContent(newContent);
                updatedChapters++;
                details.add(String.format("第%d卷第%d章: 替换世界观设定 '%s' -> '%s'，共%d处",
                        chapter.getVolumeNumber(), chapter.getChapterNumber(),
                        oldValue, newValue, countOccurrences(oldContent, oldValue)));
            }
        }
        chapterContentRepository.saveAll(chapters);

        // 2. 更新向量库中的世界观信息
        updatedVectorDocs = updateVectorDocsForKeyword(novelId, oldValue, newValue);
        if (updatedVectorDocs > 0) {
            details.add(String.format("向量库: 更新 %d 个包含 '%s' 的文档", updatedVectorDocs, oldValue));
        }

        log.info("[SettingSync] 世界观同步完成: 章节={}, 向量={}", updatedChapters, updatedVectorDocs);
        return new SyncResult(0, updatedChapters, updatedVectorDocs, details);
    }

    /**
     * 战力等级修改后同步更新
     *
     * @param novelId     小说ID
     * @param characterId 角色ID
     * @param oldLevel    旧战力等级
     * @param newLevel    新战力等级
     * @return 同步结果
     */
    @Transactional
    public SyncResult syncPowerLevelUpdate(Long novelId, Long characterId, String oldLevel, String newLevel) {
        log.info("[SettingSync] 开始同步战力等级修改: novelId={}, characterId={}, '{}' -> '{}'", novelId, characterId, oldLevel, newLevel);

        List<String> details = new ArrayList<>();
        int updatedTimelines = 0;
        int updatedChapters = 0;
        int updatedVectorDocs = 0;

        // 1. 更新时间线中的战力描述
        List<PlotTimeline> timelines = plotTimelineRepository.findByNovelIdOrderByEventTime(novelId);
        for (PlotTimeline timeline : timelines) {
            boolean changed = false;

            // 更新涉及该角色的时间线中的战力等级
            if (timeline.getCharactersInvolved() != null
                    && timeline.getCharactersInvolved().contains(characterId)
                    && oldLevel.equals(timeline.getPowerLevel())) {
                timeline.setPowerLevel(newLevel);
                updatedTimelines++;
                details.add(String.format("时间线#%d: 更新战力等级 '%s' -> '%s'", timeline.getId(), oldLevel, newLevel));
                changed = true;
            }

            // 更新描述中的战力等级文本
            if (timeline.getDescription() != null && timeline.getDescription().contains(oldLevel)) {
                timeline.setDescription(timeline.getDescription().replace(oldLevel, newLevel));
                if (!changed) {
                    updatedTimelines++;
                }
                details.add(String.format("时间线#%d: 更新描述中的战力等级", timeline.getId()));
            }
        }
        plotTimelineRepository.saveAll(timelines);

        // 2. 更新章节内容中的战力描述
        List<ChapterContent> chapters = chapterContentRepository.findByNovelId(novelId);
        for (ChapterContent chapter : chapters) {
            if (chapter.getContent() != null && chapter.getContent().contains(oldLevel)) {
                String oldContent = chapter.getContent();
                String newContent = oldContent.replace(oldLevel, newLevel);
                chapter.setContent(newContent);
                updatedChapters++;
                details.add(String.format("第%d卷第%d章: 替换战力等级 '%s' -> '%s'，共%d处",
                        chapter.getVolumeNumber(), chapter.getChapterNumber(),
                        oldLevel, newLevel, countOccurrences(oldContent, oldLevel)));
            }
        }
        chapterContentRepository.saveAll(chapters);

        // 3. 更新向量库中的战力信息
        updatedVectorDocs = updateVectorDocsForKeyword(novelId, oldLevel, newLevel);
        if (updatedVectorDocs > 0) {
            details.add(String.format("向量库: 更新 %d 个包含 '%s' 的文档", updatedVectorDocs, oldLevel));
        }

        log.info("[SettingSync] 战力等级同步完成: 时间线={}, 章节={}, 向量={}", updatedTimelines, updatedChapters, updatedVectorDocs);
        return new SyncResult(updatedTimelines, updatedChapters, updatedVectorDocs, details);
    }

    /**
     * 获取受影响的章节列表
     *
     * @param novelId 小说ID
     * @param keyword 关键词
     * @return 受影响的章节信息列表
     */
    public List<Map<String, Object>> getAffectedChapters(Long novelId, String keyword) {
        log.info("[SettingSync] 查询受影响章节: novelId={}, keyword='{}'", novelId, keyword);

        List<ChapterContent> chapters = chapterContentRepository.findByNovelIdOrderByVolumeNumberAscChapterNumberAsc(novelId);
        List<Map<String, Object>> affectedChapters = new ArrayList<>();

        for (ChapterContent chapter : chapters) {
            if (chapter.getContent() != null && chapter.getContent().contains(keyword)) {
                Map<String, Object> chapterInfo = new HashMap<>();
                chapterInfo.put("id", chapter.getId());
                chapterInfo.put("volumeNumber", chapter.getVolumeNumber());
                chapterInfo.put("chapterNumber", chapter.getChapterNumber());
                chapterInfo.put("title", chapter.getTitle());
                chapterInfo.put("occurrences", countOccurrences(chapter.getContent(), keyword));

                // 提取关键词出现的上下文片段
                chapterInfo.put("contexts", extractContexts(chapter.getContent(), keyword, 30));
                affectedChapters.add(chapterInfo);
            }
        }

        log.info("[SettingSync] 找到 {} 个受影响章节", affectedChapters.size());
        return affectedChapters;
    }

    /**
     * 更新向量库中包含指定关键词的文档
     * 通过语义检索找到相关文档，删除后重新添加更新内容
     */
    private int updateVectorDocsForKeyword(Long novelId, String oldKeyword, String newKeyword) {
        try {
            // 语义检索找到包含旧关键词的文档
            SearchRequest request = SearchRequest.builder()
                    .query(oldKeyword)
                    .topK(50)
                    .build();

            List<Document> docs = vectorStore.similaritySearch(request);

            // 过滤出属于该小说且包含旧关键词的文档
            List<Document> affectedDocs = docs.stream()
                    .filter(doc -> {
                        Object novelIdMeta = doc.getMetadata().get("novel_id");
                        return novelIdMeta != null
                                && novelId.equals(convertToLong(novelIdMeta))
                                && doc.getText() != null
                                && doc.getText().contains(oldKeyword);
                    })
                    .collect(Collectors.toList());

            if (affectedDocs.isEmpty()) {
                return 0;
            }

            // 删除旧文档
            List<String> idsToDelete = affectedDocs.stream()
                    .map(Document::getId)
                    .collect(Collectors.toList());
            vectorStore.delete(idsToDelete);

            // 创建更新后的文档并重新添加
            List<Document> updatedDocs = affectedDocs.stream()
                    .map(doc -> {
                        String updatedContent = doc.getText().replace(oldKeyword, newKeyword);
                        return new Document(doc.getId(), updatedContent, doc.getMetadata());
                    })
                    .collect(Collectors.toList());
            vectorStore.add(updatedDocs);

            // 清除向量检索缓存
            vectorKnowledgeService.clearCache();

            return affectedDocs.size();
        } catch (Exception e) {
            log.error("[SettingSync] 更新向量库失败: {}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 统计关键词在文本中出现的次数
     */
    private int countOccurrences(String text, String keyword) {
        if (text == null || keyword == null || keyword.isEmpty()) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            count++;
            index += keyword.length();
        }
        return count;
    }

    /**
     * 提取关键词出现的上下文片段
     */
    private List<String> extractContexts(String text, String keyword, int contextRadius) {
        List<String> contexts = new ArrayList<>();
        int index = 0;
        while ((index = text.indexOf(keyword, index)) != -1) {
            int start = Math.max(0, index - contextRadius);
            int end = Math.min(text.length(), index + keyword.length() + contextRadius);
            String context = text.substring(start, end);
            if (start > 0) {
                context = "..." + context;
            }
            if (end < text.length()) {
                context = context + "...";
            }
            contexts.add(context);
            index += keyword.length();
            // 最多提取5个上下文片段
            if (contexts.size() >= 5) {
                break;
            }
        }
        return contexts;
    }

    /**
     * 将 metadata 中的 novel_id 转换为 Long
     */
    private Long convertToLong(Object value) {
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else if (value instanceof String) {
            return Long.parseLong((String) value);
        }
        return null;
    }
}
