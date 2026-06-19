package com.zwriter.service;

import com.zwriter.entity.NovelInfo;
import com.zwriter.entity.VolumeOutline;
import com.zwriter.entity.Character;
import com.zwriter.repository.NovelInfoRepository;
import com.zwriter.repository.VolumeOutlineRepository;
import com.zwriter.repository.CharacterRepository;
import com.zwriter.vector.VectorKnowledgeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 上下文服务
 * 负责整合小说相关的上下文信息，供 Agent 使用
 */
@Slf4j
@Service
public class ContextService {

    @Autowired
    private NovelInfoRepository novelInfoRepository;

    @Autowired
    private VolumeOutlineRepository volumeOutlineRepository;

    @Autowired
    private CharacterRepository characterRepository;

    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;

    @Autowired
    private ContextCompressionService contextCompressionService;
    
    /**
     * 获取小说完整上下文
     */
    public String getNovelContext(Long novelId) {
        NovelInfo novel = novelInfoRepository.findById(novelId)
                .orElseThrow(() -> new RuntimeException("小说不存在: " + novelId));
        
        List<VolumeOutline> volumes = volumeOutlineRepository.findByNovelIdOrderByVolumeNumber(novelId);
        List<Character> characters = characterRepository.findByNovelId(novelId);
        
        StringBuilder context = new StringBuilder();
        
        // 小说基本信息
        context.append("【小说信息】\n");
        context.append(String.format("标题: %s\n", novel.getTitle()));
        context.append(String.format("类型: %s\n", novel.getGenre()));
        context.append(String.format("标签: %s\n", novel.getTags()));
        context.append(String.format("简介: %s\n\n", novel.getSynopsis()));
        
        // 金手指设定
        if (novel.getGoldenFinger() != null && !novel.getGoldenFinger().isEmpty()) {
            context.append("【金手指设定】\n");
            context.append(novel.getGoldenFinger()).append("\n\n");
        }
        
        // 大纲信息
        if (!volumes.isEmpty()) {
            context.append("【大纲信息】\n");
            for (VolumeOutline volume : volumes) {
                context.append(String.format("第%d卷: %s\n", volume.getVolumeNumber(), volume.getTitle()));
                context.append(String.format("  核心冲突: %s\n", volume.getCoreConflict()));
                context.append(String.format("  摘要: %s\n", volume.getSummary()));
            }
            context.append("\n");
        }
        
        // 角色信息
        if (!characters.isEmpty()) {
            context.append("【角色信息】\n");
            for (Character character : characters) {
                context.append(String.format("%s (%s):\n", character.getName(), character.getRoleType()));
                if (character.getCoreTraits() != null) {
                    context.append(String.format("  核心特质: %s\n", character.getCoreTraits()));
                }
                if (character.getBasicInfo() != null) {
                    context.append(String.format("  基本信息: %s\n", character.getBasicInfo()));
                }
                if (character.getGrowthCurve() != null) {
                    context.append(String.format("  成长曲线: %s\n", character.getGrowthCurve()));
                }
            }
            context.append("\n");
        }
        
        return context.toString();
    }
    
    /**
     * 获取章节上下文（包含前文摘要和向量检索）
     */
    public String getChapterContext(Long novelId, Integer volumeNumber, Integer chapterNumber) {
        StringBuilder context = new StringBuilder();

        // 基础小说上下文
        context.append(getNovelContext(novelId));

        // 使用上下文压缩服务获取完整上下文
        String compressedContext = contextCompressionService.buildCompressedContext(novelId, 3);
        context.append(compressedContext);

        return context.toString();
    }

    /**
     * 获取章节上下文（带向量检索）
     */
    public String getChapterContextWithVectorSearch(Long novelId, Integer volumeNumber,
                                                     Integer chapterNumber, String query) {
        StringBuilder context = new StringBuilder();

        // 基础小说上下文
        context.append(getNovelContext(novelId));

        // 使用上下文压缩服务获取完整上下文（带向量检索）
        String compressedContext = contextCompressionService.buildContextWithVectorSearch(
                novelId, 3, query, 5);
        context.append(compressedContext);

        return context.toString();
    }

    /**
     * 存储章节内容到向量库
     */
    public void storeChapterToVector(Long novelId, Integer chapterNumber, String content) {
        // 简单分段：按段落分割
        String[] paragraphs = content.split("\n\n+");
        List<String> paragraphList = List.of(paragraphs);

        vectorKnowledgeService.storeChapterParagraphs(novelId, chapterNumber, paragraphList);
        log.info("[ContextService] 存储章节到向量库: novelId={}, chapter={}, paragraphs={}",
                novelId, chapterNumber, paragraphList.size());
    }
}
