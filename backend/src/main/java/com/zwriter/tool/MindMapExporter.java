package com.zwriter.tool;

import com.zwriter.entity.NovelInfo;
import com.zwriter.entity.VolumeOutline;
import com.zwriter.repository.NovelInfoRepository;
import com.zwriter.repository.VolumeOutlineRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 思维导图导出工具
 * 支持：Mermaid mindmap 格式、Markdown 层级格式、JSON 树形结构
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MindMapExporter {

    private final NovelInfoRepository novelInfoRepository;
    private final VolumeOutlineRepository volumeOutlineRepository;

    /**
     * 导出为 Mermaid mindmap 格式
     * @param novelId 小说ID
     * @return Mermaid 代码字符串
     */
    public String exportToMermaid(Long novelId) {
        NovelInfo novel = novelInfoRepository.findById(novelId).orElse(null);
        if (novel == null) {
            log.warn("小说不存在，novelId={}", novelId);
            return "";
        }

        List<VolumeOutline> volumes = volumeOutlineRepository.findByNovelIdOrderByVolumeNumber(novelId);

        StringBuilder sb = new StringBuilder();
        sb.append("mindmap\n");
        sb.append("  root((").append(escapeMermaid(novel.getTitle())).append("))\n");

        for (VolumeOutline volume : volumes) {
            String volumeLabel = "第" + volume.getVolumeNumber() + "卷：" + escapeMermaid(volume.getTitle());
            sb.append("    ").append(volumeLabel).append("\n");

            // 核心冲突
            if (volume.getCoreConflict() != null && !volume.getCoreConflict().isBlank()) {
                sb.append("      ").append(escapeMermaid(volume.getCoreConflict())).append("\n");
            }

            // 章节范围
            if (volume.getChapterCount() != null && volume.getChapterCount() > 0) {
                int startChapter = calculateStartChapter(volumes, volume);
                int endChapter = startChapter + volume.getChapterCount() - 1;
                sb.append("      第").append(startChapter).append("-").append(endChapter).append("章\n");
            }

            // 摘要作为补充节点
            if (volume.getSummary() != null && !volume.getSummary().isBlank()) {
                String summary = volume.getSummary().length() > 30
                        ? volume.getSummary().substring(0, 30) + "…"
                        : volume.getSummary();
                sb.append("      ").append(escapeMermaid(summary)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 导出为 Markdown 层级格式
     * @param novelId 小说ID
     * @return Markdown 字符串
     */
    public String exportToMarkdown(Long novelId) {
        NovelInfo novel = novelInfoRepository.findById(novelId).orElse(null);
        if (novel == null) {
            log.warn("小说不存在，novelId={}", novelId);
            return "";
        }

        List<VolumeOutline> volumes = volumeOutlineRepository.findByNovelIdOrderByVolumeNumber(novelId);

        StringBuilder sb = new StringBuilder();
        sb.append("# ").append(novel.getTitle()).append("\n\n");

        // 基本信息
        sb.append("- **类型**：").append(novel.getGenre()).append("\n");
        if (novel.getTags() != null && !novel.getTags().isEmpty()) {
            sb.append("- **标签**：").append(String.join("、", novel.getTags())).append("\n");
        }
        if (novel.getGoldenFinger() != null && !novel.getGoldenFinger().isBlank()) {
            sb.append("- **金手指**：").append(novel.getGoldenFinger()).append("\n");
        }
        if (novel.getSynopsis() != null && !novel.getSynopsis().isBlank()) {
            sb.append("- **简介**：").append(novel.getSynopsis()).append("\n");
        }
        sb.append("\n");

        // 卷级大纲
        for (VolumeOutline volume : volumes) {
            sb.append("## 第").append(volume.getVolumeNumber()).append("卷：").append(volume.getTitle()).append("\n\n");

            if (volume.getCoreConflict() != null && !volume.getCoreConflict().isBlank()) {
                sb.append("- **核心冲突**：").append(volume.getCoreConflict()).append("\n");
            }
            if (volume.getChapterCount() != null && volume.getChapterCount() > 0) {
                int startChapter = calculateStartChapter(volumes, volume);
                int endChapter = startChapter + volume.getChapterCount() - 1;
                sb.append("- **章节范围**：第").append(startChapter).append("-").append(endChapter).append("章\n");
            }
            if (volume.getSummary() != null && !volume.getSummary().isBlank()) {
                sb.append("- **概要**：").append(volume.getSummary()).append("\n");
            }
            sb.append("\n");
        }

        return sb.toString();
    }

    /**
     * 导出为 JSON 树形结构
     * @param novelId 小说ID
     * @return 树形 Map 结构
     */
    public Map<String, Object> exportToJSON(Long novelId) {
        NovelInfo novel = novelInfoRepository.findById(novelId).orElse(null);
        if (novel == null) {
            log.warn("小说不存在，novelId={}", novelId);
            return Collections.emptyMap();
        }

        List<VolumeOutline> volumes = volumeOutlineRepository.findByNovelIdOrderByVolumeNumber(novelId);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("id", novel.getId());
        root.put("title", novel.getTitle());
        root.put("genre", novel.getGenre());
        root.put("tags", novel.getTags());
        root.put("goldenFinger", novel.getGoldenFinger());
        root.put("synopsis", novel.getSynopsis());

        List<Map<String, Object>> volumeList = new ArrayList<>();
        for (VolumeOutline volume : volumes) {
            Map<String, Object> volumeNode = new LinkedHashMap<>();
            volumeNode.put("id", volume.getId());
            volumeNode.put("volumeNumber", volume.getVolumeNumber());
            volumeNode.put("title", volume.getTitle());
            volumeNode.put("coreConflict", volume.getCoreConflict());
            volumeNode.put("chapterCount", volume.getChapterCount());
            volumeNode.put("summary", volume.getSummary());

            if (volume.getChapterCount() != null && volume.getChapterCount() > 0) {
                int startChapter = calculateStartChapter(volumes, volume);
                int endChapter = startChapter + volume.getChapterCount() - 1;
                volumeNode.put("chapterRange", "第" + startChapter + "-" + endChapter + "章");
            }

            volumeList.add(volumeNode);
        }

        root.put("volumes", volumeList);
        return root;
    }

    /**
     * 计算某卷的起始章节号
     */
    private int calculateStartChapter(List<VolumeOutline> volumes, VolumeOutline current) {
        int start = 1;
        for (VolumeOutline v : volumes) {
            if (v.getVolumeNumber() >= current.getVolumeNumber()) {
                break;
            }
            if (v.getChapterCount() != null) {
                start += v.getChapterCount();
            }
        }
        return start;
    }

    /**
     * 转义 Mermaid 特殊字符
     */
    private String escapeMermaid(String text) {
        if (text == null) return "";
        return text.replace("(", "（")
                   .replace(")", "）")
                   .replace("[", "［")
                   .replace("]", "］")
                   .replace("{", "｛")
                   .replace("}", "｝")
                   .replace("\"", "'")
                   .replace("\n", " ");
    }
}
