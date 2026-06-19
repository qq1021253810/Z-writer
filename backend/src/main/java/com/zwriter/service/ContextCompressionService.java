package com.zwriter.service;

import com.zwriter.entity.ChapterContent;
import com.zwriter.entity.Character;
import com.zwriter.entity.Foreshadow;
import com.zwriter.entity.PlotTimeline;
import com.zwriter.llm.LlmService;
import com.zwriter.repository.ChapterContentRepository;
import com.zwriter.repository.CharacterRepository;
import com.zwriter.repository.ForeshadowRepository;
import com.zwriter.repository.PlotTimelineRepository;
import com.zwriter.vector.VectorKnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 上下文压缩服务
 * 负责将长篇小说内容压缩为精简摘要，供 Agent 上下文使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContextCompressionService {

    private final ChapterContentRepository chapterContentRepository;
    private final CharacterRepository characterRepository;
    private final PlotTimelineRepository plotTimelineRepository;
    private final ForeshadowRepository foreshadowRepository;
    private final LlmService llmService;
    private final VectorKnowledgeService vectorKnowledgeService;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 获取小说前情提要（使用LLM生成摘要）
     */
    public String getRecentSummary(Long novelId, int chapterCount) {
        List<ChapterContent> chapters = chapterContentRepository
                .findByNovelIdOrderByVolumeNumberAscChapterNumberAsc(novelId);

        if (chapters.isEmpty()) {
            return "暂无章节内容";
        }

        // 取最近 N 章
        int startIdx = Math.max(0, chapters.size() - chapterCount);
        List<ChapterContent> recentChapters = chapters.subList(startIdx, chapters.size());

        StringBuilder summary = new StringBuilder();
        summary.append("【前情提要】（最近 ").append(recentChapters.size()).append(" 章）\n\n");

        for (ChapterContent chapter : recentChapters) {
            summary.append(String.format("第%d卷 第%d章: %s\n",
                    chapter.getVolumeNumber(), chapter.getChapterNumber(), chapter.getTitle()));

            // 使用LLM生成章节摘要
            String chapterSummary = generateChapterSummary(chapter.getContent());
            summary.append(chapterSummary).append("\n\n");
        }

        return summary.toString();
    }

    /**
     * 使用LLM生成章节摘要
     */
    private String generateChapterSummary(String content) {
        if (content == null || content.isEmpty()) {
            return "无内容";
        }

        // 如果内容较短，直接返回
        if (content.length() <= 500) {
            return content;
        }

        try {
            String prompt = "请用200字以内概括以下小说章节的核心情节、关键冲突和重要转折：\n\n" + content;
            String systemPrompt = "你是一个专业的小说编辑，擅长提炼章节核心内容。请简洁、准确地概括章节要点。";
            return llmService.chat(prompt, systemPrompt);
        } catch (Exception e) {
            log.warn("[ContextCompression] LLM摘要生成失败，使用截断方式", e);
            // 降级处理：截取前200字
            return content.substring(0, Math.min(200, content.length())) + "...";
        }
    }

    /**
     * 获取角色状态（从数据库检索）
     */
    public String getCharacterStatus(Long novelId) {
        List<Character> characters = characterRepository.findByNovelId(novelId);

        if (characters.isEmpty()) {
            return "暂无角色信息";
        }

        StringBuilder status = new StringBuilder();
        status.append("【角色状态】\n\n");

        for (Character character : characters) {
            status.append("▶ ").append(character.getName())
                    .append("（").append(character.getRoleType()).append("）\n");

            // 基本信息
            if (character.getBasicInfo() != null) {
                Map<String, Object> basicInfo = character.getBasicInfo();
                if (basicInfo.containsKey("age")) {
                    status.append("  年龄: ").append(basicInfo.get("age")).append("\n");
                }
                if (basicInfo.containsKey("gender")) {
                    status.append("  性别: ").append(basicInfo.get("gender")).append("\n");
                }
                if (basicInfo.containsKey("identity")) {
                    status.append("  身份: ").append(basicInfo.get("identity")).append("\n");
                }
            }

            // 核心特征
            if (character.getCoreTraits() != null) {
                Map<String, Object> traits = character.getCoreTraits();
                if (traits.containsKey("personality")) {
                    status.append("  性格: ").append(traits.get("personality")).append("\n");
                }
                if (traits.containsKey("motivation")) {
                    status.append("  动机: ").append(traits.get("motivation")).append("\n");
                }
            }

            // 能力
            if (character.getAbilities() != null && character.getAbilities().containsKey("level")) {
                status.append("  实力等级: ").append(character.getAbilities().get("level")).append("\n");
            }

            // 关系
            if (character.getRelationships() != null && !character.getRelationships().isEmpty()) {
                status.append("  关系网: ").append(formatRelationships(character.getRelationships())).append("\n");
            }

            // 成长曲线
            if (character.getGrowthCurve() != null && character.getGrowthCurve().containsKey("current_stage")) {
                status.append("  当前阶段: ").append(character.getGrowthCurve().get("current_stage")).append("\n");
            }

            status.append("\n");
        }

        return status.toString();
    }

    /**
     * 格式化角色关系
     */
    private String formatRelationships(Map<String, Object> relationships) {
        return relationships.entrySet().stream()
                .map(entry -> entry.getKey() + ":" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    /**
     * 获取时间线摘要（从数据库检索）
     */
    public String getTimelineSummary(Long novelId) {
        List<PlotTimeline> timelines = plotTimelineRepository.findByNovelIdOrderByEventTime(novelId);

        if (timelines.isEmpty()) {
            return "暂无时间线信息";
        }

        StringBuilder timeline = new StringBuilder();
        timeline.append("【时间线】\n\n");

        // 只显示最近20个事件，避免上下文过长
        int limit = Math.min(timelines.size(), 20);
        for (int i = 0; i < limit; i++) {
            PlotTimeline event = timelines.get(i);
            timeline.append(String.format("▶ %s [%s]\n",
                    event.getEventTime().format(TIME_FORMATTER),
                    event.getEventType()));
            timeline.append("  ").append(event.getDescription()).append("\n");

            if (event.getPowerLevel() != null && !event.getPowerLevel().isEmpty()) {
                timeline.append("  战力等级: ").append(event.getPowerLevel()).append("\n");
            }
            timeline.append("\n");
        }

        if (timelines.size() > 20) {
            timeline.append("... 还有 ").append(timelines.size() - 20).append(" 个早期事件\n");
        }

        return timeline.toString();
    }

    /**
     * 获取伏笔追踪信息（从数据库检索）
     */
    public String getForeshadowTracking(Long novelId) {
        List<Foreshadow> foreshadows = foreshadowRepository.findByNovelId(novelId);

        if (foreshadows.isEmpty()) {
            return "暂无伏笔信息";
        }

        StringBuilder foreshadow = new StringBuilder();
        foreshadow.append("【伏笔追踪】\n\n");

        // 分类统计
        long plantedCount = foreshadows.stream()
                .filter(f -> "planted".equals(f.getStatus()))
                .count();
        long revealedCount = foreshadows.stream()
                .filter(f -> "revealed".equals(f.getStatus()))
                .count();

        foreshadow.append(String.format("已埋伏笔: %d 条 | 已揭示: %d 条\n\n", plantedCount, revealedCount));

        // 显示未揭示的伏笔（优先展示）
        List<Foreshadow> pendingForeshadows = foreshadows.stream()
                .filter(f -> !"revealed".equals(f.getStatus()))
                .limit(10)
                .toList();

        if (!pendingForeshadows.isEmpty()) {
            foreshadow.append("▶ 待揭示伏笔:\n");
            for (Foreshadow fs : pendingForeshadows) {
                foreshadow.append(String.format("  • 第%d章埋设: %s", fs.getSetupChapter(), fs.getClueDescription()));
                if (fs.getPayoffChapter() != null) {
                    foreshadow.append(String.format(" (预计第%d章揭示)", fs.getPayoffChapter()));
                }
                foreshadow.append("\n");
            }
            foreshadow.append("\n");
        }

        return foreshadow.toString();
    }

    /**
     * 检索相关段落（向量检索）
     */
    public String getRelatedParagraphs(String query, int topK) {
        List<VectorKnowledgeService.QueryResult> results = vectorKnowledgeService.retrieveRelatedParagraphs(query, topK);

        if (results.isEmpty()) {
            return "暂无相关段落";
        }

        StringBuilder related = new StringBuilder();
        related.append("【相关段落参考】\n\n");

        for (int i = 0; i < results.size(); i++) {
            VectorKnowledgeService.QueryResult result = results.get(i);
            related.append(String.format("▶ 段落 %d (相似度: %.2f)\n", i + 1, 1.0 - result.distance()));
            related.append("  ").append(result.document()).append("\n\n");
        }

        return related.toString();
    }

    /**
     * 构建完整上下文（压缩版）
     */
    public String buildCompressedContext(Long novelId, int recentChapterCount) {
        StringBuilder context = new StringBuilder();

        context.append(getRecentSummary(novelId, recentChapterCount));
        context.append("\n");
        context.append(getCharacterStatus(novelId));
        context.append("\n");
        context.append(getTimelineSummary(novelId));
        context.append("\n");
        context.append(getForeshadowTracking(novelId));

        return context.toString();
    }

    /**
     * 构建带向量检索的完整上下文
     */
    public String buildContextWithVectorSearch(Long novelId, int recentChapterCount, String query, int vectorTopK) {
        StringBuilder context = new StringBuilder();

        context.append(buildCompressedContext(novelId, recentChapterCount));
        context.append("\n");
        context.append(getRelatedParagraphs(query, vectorTopK));

        return context.toString();
    }
}
