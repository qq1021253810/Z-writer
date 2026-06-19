---
type: entity
title: 设定同步服务
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

# 设定同步服务

SettingSyncService 负责在用户修改小说设定（角色名、世界观术语、战力等级）时，级联更新所有相关数据存储，确保 RDBMS 和向量数据库之间的一致性。

## 设计动机

小说创作过程中，用户可能随时修改核心设定：
- 角色改名
- 世界观术语变更
- 战力等级调整

这些修改需要级联传播到：
1. 伏笔描述（Foreshadow.clueDescription）
2. 章节内容（ChapterContent.content）
3. 时间线条目（PlotTimeline.description）
4. 向量数据库中的文档

## 核心方法

### 角色同步

```java
public void syncCharacterChanges(Long novelId, String oldName, String newName)
```

**级联更新范围**：
- `Foreshadow.clueDescription` - 替换角色名
- `ChapterContent.content` - 替换章节正文中的角色名
- `PlotTimeline.description` - 替换时间线描述中的角色名
- 向量数据库文档 - 更新相关向量文档

### 世界观同步

```java
public void syncWorldviewChanges(Long novelId, String oldTerm, String newTerm)
```

**级联更新范围**：
- `ChapterContent.content` - 替换章节正文中的世界观术语
- 向量数据库文档 - 更新相关向量文档

### 战力等级同步

```java
public void syncPowerLevelChanges(Long novelId, String oldLevel, String newLevel)
```

**级联更新范围**：
- `PlotTimeline.powerLevel` - 更新时间线战力等级
- `ChapterContent.content` - 替换章节正文中的战力等级
- 向量数据库文档 - 更新相关向量文档

## 依赖组件

| 组件 | 用途 |
|------|------|
| `ForeshadowRepository` | 更新伏笔描述 |
| `ChapterContentRepository` | 更新章节内容 |
| `PlotTimelineRepository` | 更新时间线条目 |
| `VectorKnowledgeService` | 更新向量库文档 |
| `VectorStore` | 直接操作向量存储 |

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `POST /api/setting-sync/character` | syncCharacterChanges | 同步角色修改 |
| `POST /api/setting-sync/worldview` | syncWorldviewChanges | 同步世界观修改 |
| `POST /api/setting-sync/power-level` | syncPowerLevelChanges | 同步战力等级修改 |
| `GET /api/setting-sync/affected-chapters` | getAffectedChapters | 获取受影响章节列表 |

## 技术要点

1. **事务一致性**：RDBMS 更新和向量库更新需要保证最终一致性
2. **批量更新**：章节内容替换使用批量操作，避免逐条更新
3. **向量库同步**：更新 RDBMS 后，需要重新生成向量文档

## 相关页面

- [[data-model]] - 数据模型层，同步服务修改的实体
- [[vector-knowledge-service]] - 向量知识服务，同步目标之一
- [[external-tool-plugins]] - 外部工具插件，与设定数据交互
