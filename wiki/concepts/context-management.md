---
type: concept
title: 上下文管理
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, context, ai, optimization]
sources: [rust-cli/src/context/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 上下文管理

Rust CLI 实现了完整的上下文管理系统，解决 LLM 上下文窗口限制问题，确保长篇小说创作中 Agent 始终拥有足够的上下文信息。

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

```rust
pub struct ContextManager {
    pub token_budget: usize,               // Token 预算上限
    pub tier1_recent: VecDeque<Message>,   // 最近消息
    pub tier2_compressed: Vec<Message>,    // 压缩摘要
    pub tier3_permanent: Vec<Message>,     // 永久保留
    pub recent_limit: usize,               // Tier 1 保留条数
}
```

### 消息分类规则

| 条件 | 分类 | 存储位置 |
|------|------|----------|
| 包含关键词（设定/伏笔/关键/重要等） | Full | Tier 3 永久 |
| 长度 > 500 字 | Compressed | Tier 2 摘要 |
| 普通消息 | Placeholder | Tier 1 最近 |

## 角色状态追踪 (CharacterTracker)

实时追踪每个角色的多维状态，每章更新。

```rust
pub struct CharacterState {
    pub name: String,
    pub location: String,          // 当前位置
    pub emotional_state: String,   // 情感状态
    pub relationships: HashMap<String, String>, // 关系状态
    pub inventory: Vec<String>,    // 物品清单
    pub capability_tier: Option<String>, // 修为/能力层级
    pub dialogue_style: String,    // 对话风格
    pub growth_stage: String,      // 成长阶段
    pub last_updated_chapter: usize, // 最后更新章节
}
```

**变化追踪**：每次角色状态变更记录 `CharacterChange`（chapter, change_type, before, after），形成完整的角色成长弧线。

## 滚动摘要 (RollingSummary)

每章自动生成摘要，每 10 章触发一次深度压缩。

```rust
pub struct RollingSummary {
    pub chapter_summaries: Vec<RollingChapterSummary>,
    pub compressed_summaries: Vec<CompressedSummary>,
    pub style_anchor: String,               // 风格锚点（代表性文段）
    pub compression_threshold: usize,       // 深度压缩阈值（默认 10 章）
}
```

**摘要结构**：
- **章节摘要**：chapter_num, title, summary, key_events, style_passage（风格锚点）
- **深度压缩摘要**：volume_range, overall_summary, main_plot_progress, character_arcs

## Token 优化 (TokenOptimizer)

三级优化策略，逐级增强压缩力度。

| 级别 | 策略 | 说明 | 压缩比 |
|------|------|------|--------|
| 1 | 无损压缩 | 空白规范化，去除多余空白 | ~1.2x |
| 2 | 中文虚词过滤 | 去除"的/了/和/是"等 15 个虚词 | ~1.5x |
| 3 | 抽取式压缩 | 基于句子重要性评分，选择性保留 | ~2-3x |

### 句子重要性评分

```rust
let scored_sentences: Vec<(usize, f32, &str)> = sentences
    .iter()
    .enumerate()
    .map(|(i, s)| {
        let score = calculate_importance(s);  // 基于关键词 + 长度加权
        (i, score, *s)
    })
    .collect();
```

## Token 预算管理

- **CLI 启动时**：读取配置中的 `token_budget`（默认 4096）
- **每步操作前**：检查当前用量，超过 80% 时黄色警告，超过 100% 时强烈建议压缩
- **手动压缩**：`/compress` 命令调用 LLM 压缩最近 10 章，更新 Tier 2
- **上下文快照**：退出时自动保存 `.context_snapshot.json`，下次启动恢复

## Token 统计追踪 (TokenTracker)

全局统计所有 LLM 调用的 Token 消耗：

```rust
pub struct TokenTracker {
    total_prompt: AtomicU64,      // 总输入 tokens
    total_completion: AtomicU64,  // 总输出 tokens
    call_count: AtomicU64,        // 调用次数
}
```

通过 `/stats` 命令查看统计信息。

## 与 Java 后端的对比

| 维度 | Rust CLI | Java 后端 |
|------|----------|-----------|
| 上下文管理 | ContextManager（三层） | ContextService + ContextCompressionService |
| 角色追踪 | CharacterTracker（文件） | RDBMS CharacterRepository |
| 摘要机制 | RollingSummary（JSON） | 无独立摘要模块 |
| Token 优化 | TokenOptimizer（三级） | LlmService 直接调用 |
| Token 统计 | TokenTracker（全局） | 无 |

## 指标与限制

- Token 预算：4096（可配置）
- Tier 1 保留条数：10 条
- 深度压缩阈值：10 章
- 重试策略：3 次，指数退避（1s/2s/4s）

## 相关页面

- [[rust-cli-architecture]] - Rust CLI 架构
- [[hybrid-rag-storage]] - 混合 RAG 存储（上下文数据来源）
- [[multi-agent-architecture]] - Agent 使用上下文信息
- [[context-service-layer]] - Java 后端上下文服务（对比参考）