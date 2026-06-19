---
type: entity
title: 上下文服务层
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, service]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 上下文服务层

上下文服务层是数据层与 AI Agent 层之间的桥梁，负责组装和压缩小说上下文信息，为 Agent 提供精准的创作上下文。

## 架构位置

```
数据层 (Repository)
    ↓
ContextService (上下文组装)
    ↓
ContextCompressionService (上下文压缩)
    ↓
Agent 层 (BaseAgent → 子 Agent)
```

## ContextService（上下文组装服务）

路径：`backend/src/main/java/com/zwriter/service/ContextService.java`

### 职责

1. **小说级上下文组装**：整合小说基本信息、金手指、大纲、角色信息
2. **章节级上下文组装**：获取前情提要 + 向量检索相关段落
3. **向量存储**：将章节内容存储到向量数据库

### 核心方法

| 方法 | 说明 |
|------|------|
| `getNovelContext(novelId)` | 获取完整小说上下文（信息+大纲+角色） |
| `getChapterContext(novelId, volumeNum, chapterNum)` | 获取章节上下文（前情+向量检索） |
| `storeChapterToVectorDB(chapter)` | 将章节内容存入向量库 |

### 依赖

- `NovelInfoRepository` - 获取小说信息
- `VolumeOutlineRepository` - 获取卷大纲
- `CharacterRepository` - 获取角色信息
- `VectorKnowledgeService` - 向量检索和存储
- `ContextCompressionService` - 上下文压缩

## ContextCompressionService（上下文压缩服务）

路径：`backend/src/main/java/com/zwriter/service/ContextCompressionService.java`

### 职责

将长篇小说内容压缩为 Agent 可消费的简洁上下文，避免超出 LLM 上下文窗口限制。

### 压缩维度

| 维度 | 说明 | 数据来源 |
|------|------|----------|
| 章节摘要 | 通过 LLM 生成每章摘要 | ChapterContent + LlmService |
| 角色状态 | 当前角色状态快照 | CharacterRepository |
| 时间线摘要 | 近期剧情时间线 | PlotTimelineRepository |
| 伏笔追踪 | 未回收伏笔列表 | ForeshadowRepository |
| 相关段落 | 向量检索的相关段落 | VectorKnowledgeService |

### 核心方法

| 方法 | 说明 |
|------|------|
| `compressContext(novelId, chapterContext)` | 压缩章节上下文 |
| `generateChapterSummary(chapter)` | 使用 LLM 生成章节摘要 |
| `getCharacterStatus(novelId)` | 获取角色当前状态 |
| `getTimelineSummary(novelId)` | 获取时间线摘要 |
| `getForeshadowTracking(novelId)` | 获取伏笔追踪信息 |

### 压缩策略

1. **摘要替代原文**：用 LLM 生成的摘要替代完整章节内容
2. **选择性检索**：只检索与当前创作相关的段落
3. **状态快照**：角色状态只保留最新快照，不保留历史

## 与 Agent 层的交互

`BaseAgent` 在调用 LLM 前，通过 `ContextService` 获取上下文：

```java
// BaseAgent 中的典型流程
String novelContext = contextService.getNovelContext(novelId);
String chapterContext = contextService.getChapterContext(novelId, volumeNum, chapterNum);
String compressedContext = contextCompressionService.compressContext(novelId, chapterContext);
String prompt = buildPrompt(novelContext, compressedContext, userInput);
```

## 相关页面

- [[data-model]] - 数据模型层，上下文服务读取的实体
- [[vector-knowledge-service]] - 向量知识服务，提供语义检索
- [[multi-agent-architecture]] - 多智能体架构，上下文服务的消费者
- [[workflow-system]] - 工作流系统，调用上下文服务
