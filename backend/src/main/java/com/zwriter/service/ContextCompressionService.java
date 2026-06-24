package com.zwriter.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 上下文压缩服务 (Stub - Phase 1.1 migration)
 * TODO: 将在 Phase 2-3 完全重写，使用文件系统存储替代数据库
 */
@Slf4j
@Service
public class ContextCompressionService {

    /**
     * 获取小说前情提要 (Stub)
     */
    public String getRecentSummary(Long novelId, int chapterCount) {
        log.warn("[ContextCompressionService] getRecentSummary 为 Stub 实现");
        return "暂无章节内容 (Stub)";
    }

    /**
     * 获取角色状态 (Stub)
     */
    public String getCharacterStatus(Long novelId) {
        log.warn("[ContextCompressionService] getCharacterStatus 为 Stub 实现");
        return "暂无角色信息 (Stub)";
    }

    /**
     * 获取时间线摘要 (Stub)
     */
    public String getTimelineSummary(Long novelId) {
        log.warn("[ContextCompressionService] getTimelineSummary 为 Stub 实现");
        return "暂无时间线信息 (Stub)";
    }

    /**
     * 获取伏笔追踪信息 (Stub)
     */
    public String getForeshadowTracking(Long novelId) {
        log.warn("[ContextCompressionService] getForeshadowTracking 为 Stub 实现");
        return "暂无伏笔信息 (Stub)";
    }

    /**
     * 检索相关段落 (Stub)
     */
    public String getRelatedParagraphs(String query, int topK) {
        log.warn("[ContextCompressionService] getRelatedParagraphs 为 Stub 实现");
        return "暂无相关段落 (Stub)";
    }

    /**
     * 构建完整上下文（压缩版）(Stub)
     */
    public String buildCompressedContext(Long novelId, int recentChapterCount) {
        log.warn("[ContextCompressionService] buildCompressedContext 为 Stub 实现");
        return getRecentSummary(novelId, recentChapterCount);
    }

    /**
     * 构建带向量检索的完整上下文 (Stub)
     */
    public String buildContextWithVectorSearch(Long novelId, int recentChapterCount, String query, int vectorTopK) {
        log.warn("[ContextCompressionService] buildContextWithVectorSearch 为 Stub 实现");
        return buildCompressedContext(novelId, recentChapterCount);
    }
}
