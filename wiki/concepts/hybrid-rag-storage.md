---
type: concept
title: 混合 RAG 存储
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, storage, rag]
sources: [rust-cli/src/rag/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 混合 RAG 存储

Rust CLI 实现了三层混合 RAG 存储架构，从人类可读的 Markdown 到结构化 JSON 再到向量语义检索，覆盖不同粒度的知识需求。

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

```rust
pub struct MemoryTree {
    pub novel_id: String,
    pub volumes: Vec<VolumeSummary>,
    pub foreshadows: Vec<Foreshadow>,
}

pub struct VolumeSummary {
    pub volume_num: usize,
    pub title: String,
    pub summary: String,
    pub chapters: Vec<ChapterSummary>,
}

pub struct ChapterSummary {
    pub chapter_num: usize,
    pub title: String,
    pub summary: String,
    pub key_events: Vec<String>,
    pub character_changes: Vec<CharacterChange>,
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
- 使用 LLM 生成嵌入向量（Ollama nomic-embed-text 或 DashScope text-embedding-v3）
- 余弦相似度检索
- 阶段三计划集成 usearch 向量库以获得更高性能

## 技术选型依据

与 Java 后端使用 ChromaDB 不同，Rust CLI 选择自建轻量级 RAG 存储：

| 维度 | Java 后端 (ChromaDB) | Rust CLI (自建) |
|------|---------------------|------------------|
| 运行依赖 | 需要 Docker 运行 ChromaDB | 零依赖，纯文件系统 |
| 启动时间 | 依赖 Docker 服务启动 | 毫秒级 |
| 适用场景 | Web 端多用户协作 | 本地离线创作 |
| L1 存储 | PostgreSQL | Markdown 文件 |
| L2 存储 | RDBMS 表 | JSON 文件 |
| L3 存储 | ChromaDB 向量库 | 本地 JSON + 余弦相似度 |

## 相关页面

- [[rust-cli-architecture]] - Rust CLI 整体架构
- [[context-management]] - 上下文管理（使用 RAG 数据）
- [[data-model]] - Java 后端数据模型（对比参考）
- [[vector-knowledge-service]] - Java 后端向量知识服务（对比参考）