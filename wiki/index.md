---
type: index
project: "Z-writer"
---

# CodeWiki Index

This index is the first file read by `codewiki query` before matched pages.

> Last updated: 2026-06-23 | Total pages: 19

## Entities

- [[entities/multi-agent-architecture]] - 多智能体架构（ControllerAgent + 6 子 Agent）
- [[entities/external-tool-plugins]] - 外部工具插件（6 个创作辅助工具）
- [[entities/vector-knowledge-service]] - 向量知识服务（Chroma + VectorKnowledgeService + 缓存）
- [[entities/data-model]] - 数据模型层（6 个 JPA 实体及关系）
- [[entities/context-service-layer]] - 上下文服务层（ContextService + ContextCompressionService）
- [[entities/setting-sync-service]] - 设定同步服务（跨数据存储一致性机制）
- [[entities/dialogue-guide-system]] - 对话引导系统（AI 引导式创作流程）
- [[entities/rust-cli-architecture]] - Rust CLI 架构（性能 16ms/2MB，12 个模块）
- [[entities/cli-commands]] - CLI 命令参考（14 个命令详解）
- [[entities/frontend-components]] - 前端组件架构（React 18 + shadcn/ui）

## Decisions

- [[decisions/spring-ai-upgrade]] - Spring AI 从 1.0.0-M5 升级至 1.1.7

## Concepts

- [[concepts/workflow-system]] - 工作流系统（Java 3 种 + Rust 2 种）
- [[concepts/infrastructure-stack]] - 基础设施栈（Docker Compose + PostgreSQL + Redis + ChromaDB）
- [[concepts/hybrid-rag-storage]] - 混合 RAG 存储（L1 Markdown + L2 JSON + L3 向量库）
- [[concepts/context-management]] - 上下文管理（Tier1/2/3 分层 + 角色追踪 + Token 优化）
- [[concepts/api-endpoints]] - API 接口文档（13 个控制器，50+ 端点）
- [[concepts/database-schema]] - 数据库 Schema（6 张表，PostgreSQL 特有类型）

## Comparisons

## Lessons

- [[lessons/common-issues]] - 常见问题与解决方案

## Issues

- [[issues/pending-optimizations]] - 待优化功能

## Sources

## Queries