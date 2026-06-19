---
type: entity
id: ENTITY-003
title: 向量知识服务
name: 向量知识服务
created: 2026-06-18
updated: 2026-06-18
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

- 向量数据库：Chroma（通过 Spring AI VectorStore 集成）
- 嵌入模型：nomic-embed-text（Ollama 本地部署）
- LLM 部署：Ollama 本地部署（qwen3:1.7b 聊天 + nomic-embed-text 嵌入）

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

- 配置路径：`spring.ai.vectorstore.chroma`（注意：非 `spring.ai.chroma`）
- 配置文件：application.yml 统一管理

## Current Behavior

向量知识服务已完整实现，支持章节段落的存储、语义检索和缓存。缓存机制使用 ConcurrentHashMap + TTL 5 分钟 + 自动清理，后续可升级为 Caffeine/Redis。

## Related Pages

- [[multi-agent-architecture]] - 多智能体架构
- [[spring-ai-upgrade]] - Spring AI 升级决策
- [[common-issues]] - 常见问题与解决方案
- [[pending-optimizations]] - 待优化功能

## Open Questions

- 向量检索缓存可升级为 Caffeine/Redis 以提升性能
