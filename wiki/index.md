---
type: index
project: "Z-writer"
---

# CodeWiki Index

This index is the first file read by `codewiki query` before matched pages.

> Last updated: 2026-06-26 | Total pages: 18

## Entities

- [[entities/multi-agent-architecture]] - 多智能体架构（ControllerAgent + 6 子 Agent）
- [[entities/llm-service-layer]] - LLM 服务层（百炼优先 + Ollama 降级路由）
- [[entities/external-tool-plugins]] - 外部工具插件（6 个创作辅助工具）
- [[entities/vector-knowledge-service]] - 向量知识服务（Chroma + VectorKnowledgeService + 混合RAG存储）
- [[entities/data-model]] - 数据模型层（6 个 JPA 实体及关系）
- [[entities/context-service-layer]] - 上下文服务层（ContextService + 上下文管理实现）
- [[entities/setting-sync-service]] - 设定同步服务（跨数据存储一致性机制）
- [[entities/dialogue-guide-system]] - 对话引导系统（AI 引导式创作流程）
- [[entities/frontend-components]] - 前端组件架构（React 18 + shadcn/ui + SSE流式）
- [[entities/workflow-expansion]] - 扩展工作流（Strategy/Review/Polish + AsyncExecutor）
- [[entities/session-and-workspace]] - 会话与工作区管理（SessionManager + WorkspaceManager + WikiService）

## Decisions

- [[decisions/spring-ai-upgrade]] - Spring AI 从 1.0.0-M5 升级至 1.1.7

## Concepts

- [[concepts/workflow-system]] - 工作流系统（6 种核心+扩展工作流）
- [[concepts/infrastructure-stack]] - 基础设施栈（Docker Compose + PostgreSQL + Redis + ChromaDB）
- [[concepts/api-endpoints]] - API 接口文档（14 个控制器，50+ 端点）
- [[concepts/database-schema]] - 数据库 Schema（6 张表，PostgreSQL 特有类型）

## Comparisons

## Lessons

- [[lessons/common-issues]] - 常见问题与解决方案

## Issues

- [[issues/pending-optimizations]] - 待优化功能

## Sources

## Queries