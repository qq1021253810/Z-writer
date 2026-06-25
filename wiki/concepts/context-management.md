---
type: concept
title: 上下文管理
created: 2026-06-18
updated: 2026-06-25
tags: [architecture, context, ai, optimization]
sources: [backend/src/main/java/com/zwriter/context/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 上下文管理

Java 后端实现了完整的上下文管理系统，解决 LLM 上下文窗口限制问题，确保长篇小说创作中 Agent 始终拥有足够的上下文信息。

## 分层上下文架构 (ContextManager)

```
Tier 1: 最近消息（完整保留）
        最近 N 条消息（默认 10 条）
        → 保真度: Placeholder（普通消息）/ Compressed（>500 字）
        → 超出时移出到 Tier 2

Tier 2: 压缩摘要（摘要保留）
        较早消息的摘要版本
        → 保真度: Compressed
        → 定期深度压缩

Tier 3: 永久保留（关键决策）
        含关键词的消息（"名字"、"设定"、"伏笔"、"人设"等）
        → 保真度: Full
        → 永久保留，永不压缩
```

### 核心实现

ContextManager 管理对话历史和上下文窗口，使用 JSONL 文件持久化存储。

**关键功能**：
- 添加消息并估算 token 数
- 获取最近 N 条消息
- 检查是否需要压缩
- 清空历史记录

## 角色状态追踪 (CharacterTracker)

实时追踪每个角色的多维状态，每章更新。

**CharacterState 结构**：
- name: 角色名称
- location: 当前位置
- emotional_state: 情感状态
- relationships: 关系状态 Map
- inventory: 物品清单
- capability_tier: 能力层级
- dialogue_style: 对话风格
- growth_stage: 成长阶段
- last_updated_chapter: 最后更新章节

**变化追踪**：每次角色状态变更记录 CharacterChange（chapter, change_type, before, after），形成完整的角色成长弧线。

**冲突检测**：自动检测角色属性矛盾（如同一角色的同一属性出现不同值）。

## 滚动摘要 (RollingSummary)

每章自动生成摘要，超过阈值时触发深度压缩。

**核心机制**：
- 检查当前 token 数是否超过压缩阈值
- 获取最近对话历史
- 调用 LLM 生成摘要
- 保存摘要到文件
- 清空旧历史，保留最近几轮对话

**摘要结构**：
- 章节摘要：chapter_num, title, summary, key_events, style_passage
- 深度压缩摘要：volume_range, overall_summary, main_plot_progress, character_arcs

## Token 优化 (TokenOptimizer)

多级优化策略，管理 token 预算和上下文窗口。

**核心功能**：
- 估算文本 token 数
- 裁剪上下文到可用 token 数
- 保留开头（重要信息）和结尾（最新信息）
- 中间内容按比例裁剪并添加裁剪标记

## Token 预算管理

- **启动时**：读取配置中的 token_budget
- **每步操作前**：检查当前用量，超过阈值时触发压缩
- **手动压缩**：调用 LLM 压缩最近对话，更新 Tier 2
- **上下文快照**：自动保存到 JSONL 文件

## 指标与限制

- Token 预算：可配置（默认 4096）
- Tier 1 保留条数：10 条
- 深度压缩阈值：可配置
- 重试策略：3 次，指数退避

## 相关页面

- [[hybrid-rag-storage]] - 混合 RAG 存储（上下文数据来源）
- [[multi-agent-architecture]] - Agent 使用上下文信息
- [[context-service-layer]] - 上下文服务层