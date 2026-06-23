---
type: concept
title: 数据库 Schema
created: 2026-06-18
updated: 2026-06-18
tags: [database, schema, postgresql]
sources: [backend/src/main/resources/db/migration/V1__init_schema.sql]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 数据库 Schema

Z-writer 使用 PostgreSQL 16 作为数据库，通过 Flyway 管理迁移。

## 数据库连接信息

| 项 | 值 |
|----|----|
| 主机 | localhost |
| 端口 | 5432 |
| 数据库名 | zwriter |
| 用户名 | zwriter |
| 密码 | zwriter123 |

## 表结构

### 1. novel_info（小说基础信息表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| title | VARCHAR(255) | NOT NULL | 小说标题 |
| genre | VARCHAR(100) | NOT NULL | 类型（玄幻/都市/仙侠等） |
| tags | TEXT[] | | 标签列表（PostgreSQL 数组） |
| synopsis | TEXT | | 故事梗概 |
| golden_finger | VARCHAR(500) | | 金手指设定 |
| total_volumes | INTEGER | DEFAULT 1 | 总卷数 |
| status | VARCHAR(50) | DEFAULT 'draft' | 状态（draft/ongoing/completed） |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 2. character_table（角色档案表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| novel_id | BIGINT | NOT NULL, FK → novel_info(id) ON DELETE CASCADE | 所属小说 |
| name | VARCHAR(100) | NOT NULL | 角色名 |
| role_type | VARCHAR(50) | NOT NULL | 角色类型（主角/配角/反派） |
| basic_info | JSONB | | 基本信息（年龄、性别、外貌等） |
| core_traits | JSONB | | 核心特质（性格、动机等） |
| abilities | JSONB | | 能力设定 |
| relationships | JSONB | | 关系网络 |
| catchphrases | TEXT[] | | 口头禅列表 |
| growth_curve | JSONB | | 成长曲线 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 3. volume_outline（分卷大纲表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| novel_id | BIGINT | NOT NULL, FK → novel_info(id) ON DELETE CASCADE | 所属小说 |
| volume_number | INTEGER | NOT NULL | 卷号 |
| title | VARCHAR(255) | | 卷标题 |
| summary | TEXT | | 卷摘要 |
| core_conflict | TEXT | | 核心冲突 |
| chapter_count | INTEGER | DEFAULT 0 | 章节数 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 4. plot_timeline（时间线事件表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| novel_id | BIGINT | NOT NULL, FK → novel_info(id) ON DELETE CASCADE | 所属小说 |
| event_time | TIMESTAMP | NOT NULL | 事件时间 |
| event_type | VARCHAR(50) | NOT NULL | 事件类型 |
| description | TEXT | NOT NULL | 事件描述 |
| characters_involved | BIGINT[] | | 涉及角色 ID 数组 |
| power_level | VARCHAR(100) | | 战力等级 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |

### 5. foreshadow_lib（伏笔库表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| novel_id | BIGINT | NOT NULL, FK → novel_info(id) ON DELETE CASCADE | 所属小说 |
| setup_chapter | INTEGER | NOT NULL | 埋设章节 |
| payoff_chapter | INTEGER | | 回收章节（可空） |
| clue_description | TEXT | NOT NULL | 线索描述 |
| status | VARCHAR(50) | DEFAULT 'planted' | 状态（planted/resolved） |
| related_characters | BIGINT[] | | 关联角色 ID 数组 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

### 6. chapter_content（章节内容表）

| 字段 | 类型 | 约束 | 说明 |
|------|------|------|------|
| id | BIGSERIAL | PRIMARY KEY | 自增主键 |
| novel_id | BIGINT | NOT NULL, FK → novel_info(id) ON DELETE CASCADE | 所属小说 |
| volume_number | INTEGER | NOT NULL | 卷号 |
| chapter_number | INTEGER | NOT NULL | 章节号 |
| title | VARCHAR(255) | | 章节标题 |
| content | TEXT | NOT NULL | 章节正文 |
| word_count | INTEGER | DEFAULT 0 | 字数 |
| hook_strength | INTEGER | | 钩子强度评分 |
| satisfaction_rating | INTEGER | | 满意度评分 |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 创建时间 |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | 更新时间 |

**唯一约束**：`(novel_id, volume_number, chapter_number)` 联合唯一

## 索引

| 索引名 | 表 | 字段 | 说明 |
|--------|----|------|------|
| idx_character_novel | character_table | novel_id | 角色表小说索引 |
| idx_volume_novel | volume_outline | novel_id | 卷大纲小说索引 |
| idx_timeline_novel | plot_timeline | novel_id | 时间线小说索引 |
| idx_foreshadow_novel | foreshadow_lib | novel_id | 伏笔库小说索引 |
| idx_chapter_novel | chapter_content | novel_id | 章节小说索引 |
| idx_chapter_number | chapter_content | novel_id, volume_number, chapter_number | 章节编号复合索引 |

## PostgreSQL 特有类型

| 类型 | 用途 | 示例 |
|------|------|------|
| TEXT[] | 文本数组 | tags, catchphrases |
| BIGINT[] | 长整型数组 | characters_involved, related_characters |
| JSONB | 二进制 JSON | basic_info, core_traits, abilities, relationships, growth_curve |

## 表关系图

```
novel_info (1)
    ├── character_table (N)     ON DELETE CASCADE
    ├── volume_outline (N)      ON DELETE CASCADE
    ├── chapter_content (N)     ON DELETE CASCADE
    ├── foreshadow_lib (N)      ON DELETE CASCADE
    └── plot_timeline (N)       ON DELETE CASCADE
```

## 迁移管理

使用 Flyway 管理数据库迁移，脚本位于：
- `backend/src/main/resources/db/migration/V1__init_schema.sql`

启动时自动执行迁移（`spring.flyway.enabled=true`）。

## 相关页面

- [[data-model]] - 数据模型层（JPA 实体映射）
- [[infrastructure-stack]] - 基础设施栈（Docker Compose 配置）
- [[setting-sync-service]] - 设定同步服务（跨存储一致性）
