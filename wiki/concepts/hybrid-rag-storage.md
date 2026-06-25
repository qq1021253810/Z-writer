---
type: concept
title: 混合 RAG 存储
created: 2026-06-18
updated: 2026-06-25
tags: [architecture, storage, rag]
sources: [backend/src/main/java/com/zwriter/rag/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 混合 RAG 存储

Java 后端实现了三层混合 RAG 存储架构，从人类可读的 Markdown 到结构化 JSON 再到向量语义检索，覆盖不同粒度的知识需求。

## 三层架构

```
L1: Markdown 文件存储（人类可读）
    ├── novel_info.md        # 小说基本信息
    ├── worldview.md         # 世界观设定
    ├── outline.md           # 大纲规划
    ├── characters/          # 角色卡目录
    │   └── *.md
    └── chapters/            # 章节内容目录
        ├── chapter_001.md
        └── chapter_002.md

L2: JSON 剧情记忆树（结构化）
    ├── memory_tree.json     # 剧情记忆树
    │   ├── volumes[]        # 卷列表
    │   │   ├── volume_num   # 卷号
    │   │   ├── chapters[]   # 章节摘要
    │   │   │   ├── key_events[]       # 关键事件
    │   │   │   └── character_changes[] # 角色变化
    │   └── foreshadows[]    # 伏笔追踪
    │       ├── planted_chapter
    │       └── status (Active/Resolved/Abandoned)
    └── memory_timeline.json  # 剧情时间线

L3: 向量素材库（语义检索）
    ├── materials.json       # 素材库
    │   └── [Material]       # 素材条目
    │       ├── content      # 素材内容
    │       ├── category     # 分类（Scenery/Combat/Character/Technique/Inspiration/Historical）
    │       ├── embedding[]  # 嵌入向量
    │       └── metadata     # 元数据（关键词、角色、地点）
    └── (阶段三启用 usearch 加速)
```

## L1: Markdown 文件存储

**特点**：
- 零依赖，直接文件系统读写
- 人类可读，可直接编辑
- Markdown 格式，兼容版本控制

**用途**：全局设定、角色卡、章节正文等需要人类直接阅读和编辑的内容。

## L2: JSON 剧情记忆树

**数据结构**：

```java
public class MemoryTree {
    private String novelId;
    private List<VolumeSummary> volumes;
    private List<Foreshadow> foreshadows;
}

public class VolumeSummary {
    private int volumeNum;
    private String title;
    private String summary;
    private List<ChapterSummary> chapters;
}

public class ChapterSummary {
    private int chapterNum;
    private String title;
    private String summary;
    private List<String> keyEvents;
    private List<CharacterChange> characterChanges;
}
```

**伏笔追踪**：支持三种状态——Active（已埋下）、Resolved（已回收）、Abandoned（已放弃）。

**特点**：
- 比 Markdown 更易结构化查询
- 适合 Agent 快速检索剧情线索
- 支持伏笔追踪和角色变化记录

## L3: 向量素材库

**素材分类**：

| 分类 | 说明 | 示例 |
|------|------|------|
| Scenery | 场景描写 | 环境、建筑、自然景观 |
| Combat | 战斗描写 | 打斗、对决、战争 |
| Character | 人物描写 | 形象、神态、动作 |
| Technique | 技能描写 | 法术、异能、招式 |
| Inspiration | 灵感碎片 | 创意片段、对话灵感 |
| Historical | 历史章节切片 | 已写章节的关键段落 |

**特点**：
- 使用 LLM 生成嵌入向量（DashScope text-embedding-v3）
- 余弦相似度检索
- 支持按分类过滤查询

## 技术选型依据

采用纯文件系统存储，零外部依赖：

| 维度 | 实现方案 |
|------|----------|
| 运行依赖 | 零依赖，纯文件系统 |
| 启动时间 | 毫秒级 |
| L1 存储 | Markdown 文件 |
| L2 存储 | JSON 文件 |
| L3 存储 | 本地 JSON + 余弦相似度 |

## 相关页面

- [[context-management]] - 上下文管理（使用 RAG 数据）
- [[data-model]] - 数据模型
- [[vector-knowledge-service]] - 向量知识服务