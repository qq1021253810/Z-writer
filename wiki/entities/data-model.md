---
type: entity
title: 数据模型层
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, database]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 数据模型层

Z-writer 系统使用 6 个 JPA 实体，存储在 PostgreSQL 16 中，通过 Flyway 管理迁移。

## 实体关系图

```
NovelInfo (1) ──┬── (N) Character
                ├── (N) VolumeOutline
                ├── (N) ChapterContent
                ├── (N) Foreshadow
                └── (N) PlotTimeline
```

所有子实体通过 `novel_id` 关联到 `NovelInfo`，外键均配置 `ON DELETE CASCADE`。

## 实体详情

### NovelInfo（小说信息）

表名：`novel_info`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| title | VARCHAR | 小说标题 |
| genre | VARCHAR | 类型（玄幻/都市/仙侠等） |
| tags | TEXT[] | PostgreSQL 数组类型，标签列表 |
| synopsis | TEXT | 故事梗概 |
| golden_finger | VARCHAR | 金手指设定 |
| total_volumes | INT | 总卷数，默认 1 |
| status | VARCHAR | 状态（draft/ongoing/completed），默认 draft |
| created_at | TIMESTAMP | 创建时间（自动） |
| updated_at | TIMESTAMP | 更新时间（自动） |

### Character（角色）

表名：`character_table`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| novel_id | BIGINT FK | 所属小说 |
| name | VARCHAR | 角色名 |
| role_type | VARCHAR | 角色类型（主角/配角/反派） |
| basic_info | JSONB | 基本信息（年龄、性别、外貌等） |
| core_traits | JSONB | 核心特质（性格、动机等） |
| abilities | JSONB | 能力设定 |
| relationships | JSONB | 关系网络（与其他角色的关系） |
| catchphrases | TEXT[] | 口头禅列表 |
| growth_curve | JSONB | 成长曲线 |

**JSONB 设计决策**：角色数据具有高度灵活性，不同角色类型字段差异大，使用 JSONB 存储半结构化数据。

### ChapterContent（章节内容）

表名：`chapter_content`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| novel_id | BIGINT FK | 所属小说 |
| volume_number | INT | 卷号 |
| chapter_number | INT | 章节号 |
| title | VARCHAR | 章节标题 |
| content | TEXT | 章节正文 |
| word_count | INT | 字数，默认 0 |
| hook_strength | INT | 钩子强度评分 |
| satisfaction_rating | INT | 满意度评分 |

**唯一约束**：`(novel_id, volume_number, chapter_number)` 联合唯一。

### VolumeOutline（卷大纲）

表名：`volume_outline`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| novel_id | BIGINT FK | 所属小说 |
| volume_number | INT | 卷号 |
| title | VARCHAR | 卷标题 |
| summary | TEXT | 卷摘要 |
| core_conflict | TEXT | 核心冲突 |
| chapter_count | INT | 章节数，默认 0 |

### Foreshadow（伏笔）

表名：`foreshadow_lib`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| novel_id | BIGINT FK | 所属小说 |
| setup_chapter | INT | 埋设章节 |
| payoff_chapter | INT | 回收章节（可空） |
| clue_description | TEXT | 线索描述 |
| status | VARCHAR | 状态（planted/resolved），默认 planted |
| related_characters | BIGINT[] | 关联角色 ID 数组 |

### PlotTimeline（剧情时间线）

表名：`plot_timeline`

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT PK | 自增主键 |
| novel_id | BIGINT FK | 所属小说 |
| event_time | TIMESTAMP | 事件时间 |
| event_type | VARCHAR | 事件类型 |
| description | TEXT | 事件描述 |
| characters_involved | BIGINT[] | 涉及角色 ID 数组 |
| power_level | VARCHAR | 战力等级 |

## PostgreSQL 特有类型

- `TEXT[]`：文本数组（tags、catchphrases）
- `BIGINT[]`：长整型数组（related_characters、characters_involved）
- `JSONB`：二进制 JSON（basic_info、core_traits、abilities、relationships、growth_curve）

## 数据库迁移

使用 Flyway 管理，迁移脚本位于 `backend/src/main/resources/db/migration/V1__init_schema.sql`。

## 相关页面

- [[multi-agent-architecture]] - 多智能体架构操作数据模型
- [[vector-knowledge-service]] - 向量知识服务存储章节内容
- [[workflow-system]] - 工作流系统读写这些实体
- [[infrastructure-stack]] - 基础设施栈包含 PostgreSQL 数据库
