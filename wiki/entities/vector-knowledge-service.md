---
type: entity
id: ENTITY-003
title: 向量知识服务
name: 向量知识服务
created: 2026-06-18
updated: 2026-06-26
tags: [architecture]
sources: [wiki/raw/specs/agent-memory.md]
status: active
key_files: []
file_hashes: {}
linked_issues: []
linked_lessons: []
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 向量知识服务

## Purpose

VectorKnowledgeService 实现章节段落的向量存储与检索，为创作过程提供上下文知识支撑。通过 Chroma 向量数据库和 Spring AI VectorStore 集成，支持语义检索和缓存机制。

## 技术栈

- 向量数据库：Chroma（通过 Chroma 原生 API 调用）
- 嵌入模型：nomic-embed-text（Ollama 本地部署）
- LLM 部署：百炼 DashScope qwen-plus（主） + Ollama qwen3:8b（降级）

## 核心功能

### VectorKnowledgeService
- 章节段落存储与检索
- 语义相似度搜索
- 向量存储使用 metadata 中的 collection 字段区分不同类型

### 缓存机制
- 使用 ConcurrentHashMap 实现
- TTL 5 分钟
- 自动清理过期缓存

### ContextCompressionService（上下文压缩）
- 前情提要生成
- 角色状态追踪
- 时间线管理
- 伏笔追踪

## 配置

- 配置路径：`application.yml` → `spring.ai.vectorstore.chroma`
- LLM 调用：通过 `LlmServiceRouter`（百炼优先 + Ollama 降级）

## Current Behavior

向量知识服务已完整实现，支持章节段落的存储、语义检索和缓存。缓存机制使用 ConcurrentHashMap + TTL 5 分钟 + 自动清理，后续可升级为 Caffeine/Redis。

## 相关页面

- [[multi-agent-architecture]] - 多智能体架构
- [[spring-ai-upgrade]] - Spring AI 升级决策
- [[common-issues]] - 常见问题与解决方案
- [[pending-optimizations]] - 待优化功能
- [[llm-service-layer]] - LLM 服务层（嵌入向量生成）

## 混合 RAG 存储架构

向量知识服务采用三层混合 RAG 存储，从 Markdown 到 JSON 到向量检索，覆盖不同粒度的知识需求。

### L1: Markdown 文件存储（人类可读）

```
workspaces/{novel-name}/
├── novel_info.md        # 小说基本信息
├── worldview.md         # 世界观设定
├── outline.md           # 大纲规划
├── characters/          # 角色卡目录
└── chapters/            # 章节内容目录
```

零依赖，直接文件系统读写，兼容版本控制。

### L2: JSON 剧情记忆树（结构化）

由 `RagService` + `MemoryTree` 管理：

```java
public class MemoryTree {
    String novelId;
    List<VolumeSummary> volumes;   // 卷摘要列表
    List<Foreshadow> foreshadows;  // 伏笔追踪（Active/Resolved/Abandoned）
}
```

- 比 Markdown 更易结构化查询
- 适合 Agent 快速检索剧情线索
- 伏笔支持三种状态：Active（已埋下）、Resolved（已回收）、Abandoned（已放弃）

### L3: 向量素材库（语义检索）

由 `RagService` + `MaterialStore` 管理，使用 LLM 生成嵌入向量（DashScope text-embedding-v3），余弦相似度检索。

| 分类 | 说明 |
|------|------|
| Scenery | 场景描写 |
| Combat | 战斗描写 |
| Character | 人物描写 |
| Technique | 技能描写 |
| Inspiration | 灵感碎片 |
| Historical | 历史章节切片 |

### 技术选型

| 维度 | 实现方案 |
|------|----------|
| 运行依赖 | 零依赖，纯文件系统 |
| L1 存储 | Markdown 文件 |
| L2 存储 | JSON 文件 |
| L3 存储 | 本地 JSON + 余弦相似度 |

## Open Questions

- 向量检索缓存可升级为 Caffeine/Redis 以提升性能
