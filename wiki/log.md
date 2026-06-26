---
type: log
---

# CodeWiki Log

> Chronological record of accepted wiki actions.
> Format: `## [YYYY-MM-DD] action | subject`
> Actions: ingest, update, query, lint, create, archive, delete, absorb, breakdown
> Rotate when this file exceeds 500 entries.

## [2026-06-26] update | CodeWiki 精简优化

- 新增 3 个 wiki 页面：
  - entities/llm-service-layer — LLM 服务层（百炼优先 + Ollama 降级路由）
  - entities/workflow-expansion — 扩展工作流（Strategy/Review/Polish + AsyncExecutor）
  - entities/session-and-workspace — 会话与工作区管理（SessionManager + WorkspaceManager + WikiService）
- 修复 5 个过时页面：
  - entities/vector-knowledge-service — 模型版本 qwen3:1.7b→qwen3:8b，移除 Spring AI VectorStore 引用
  - concepts/infrastructure-stack — 模型版本修正，新增百炼 DashScope 配置
  - concepts/workflow-system — 补充 3 个扩展工作流 + 异步执行机制
  - concepts/api-endpoints — 新增 Strategy/Review/Polish/Session/Wiki 端点
  - entities/frontend-components — 新增 SSE 流式渲染基础设施
- 合并 2 个页面：
  - concepts/context-management → entities/context-service-layer（归档）
  - concepts/hybrid-rag-storage → entities/vector-knowledge-service（归档）
- 更新 2 个页面：
  - lessons/common-issues — 移除 Spring AI 过时内容，补充实际教训
  - issues/pending-optimizations — 标记已完成项
- 更新 index.md（总页数 17→18）、_backlinks.json、log.md

## [2026-06-25] delete | Rust CLI 项目清除

- 删除 `rust-cli/` 整个目录（Rust 源码、测试、wiki 副本、工作区数据、编译产物）
- 删除 CLI 规划文档：`cli-long-term-optimization-plan.md`、`plan_cli_business_positioning_optimization.md`
- 删除 CLI 专属 Wiki 页面：`rust-cli-architecture.md`、`cli-commands.md`
- 更新 `wiki/index.md`：移除 CLI 条目，页面计数 19→17
- 更新 `wiki/_backlinks.json`：移除所有 CLI 相关链接
- 更新 `wiki/concepts/workflow-system.md`：移除 Rust CLI 工作流章节
- 重写 `wiki/concepts/hybrid-rag-storage.md`：改为 Java 后端视角
- 重写 `wiki/concepts/context-management.md`：改为 Java 后端视角
- 更新 `wiki/entities/multi-agent-architecture.md`：移除 CLI 引用
- 更新 `.gitignore`：移除 rust-cli 相关条目

## [2026-06-23] absorb | API & Frontend & Database

- 新增 3 个 wiki 页面：
  - concepts/api-endpoints - API 接口完整文档（13 个控制器，50+ 端点）
  - concepts/database-schema - 数据库 Schema（6 张表，索引，PostgreSQL 特有类型）
  - entities/frontend-components - 前端组件架构（React 18 + shadcn/ui，3 个页面，4 个服务）

## [2026-06-18] absorb | Rust CLI

- 新增 4 个 wiki 页面：
  - entities/rust-cli-architecture - Rust CLI 架构（12 模块、性能指标、技术栈）
  - entities/cli-commands - CLI 命令参考（14 个命令）
  - concepts/hybrid-rag-storage - 混合 RAG 存储（L1/L2/L3 三层架构）
  - concepts/context-management - 上下文管理（分层压缩、角色追踪、Token 优化）
- 更新 multi-agent-architecture 和 workflow-system 页面，补充 Rust 实现

## [2026-06-18] absorb | codebase scan

- 扫描项目代码，新增 5 个 wiki 页面：
  - entities/data-model - 数据模型层（6 个 JPA 实体）
  - entities/context-service-layer - 上下文服务层
  - entities/setting-sync-service - 设定同步服务
  - entities/dialogue-guide-system - 对话引导系统
  - concepts/infrastructure-stack - 基础设施栈
- 更新 multi-agent-architecture 和 workflow-system 页面

## [2026-06-18] ingest | AGENT_MEMORY.md

- 从 AGENT_MEMORY.md 摄取知识，生成 7 个 wiki 页面

## [2026-06-18] create | CodeWiki initialized

- CodeWiki initialized. Future updates require human approval.