---
type: decision
id: ADR-001
title: Spring AI 从 1.0.0-M5 升级至 1.1.7
status: accepted
created: 2026-06-18
updated: 2026-06-18
date: 2026-06-18
deciders: []
tags: [decision, release]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# ADR-001: Spring AI 从 1.0.0-M5 升级至 1.1.7

## Context

Z-writer 项目最初使用 Spring AI 1.0.0-M5 版本，该版本存在以下问题：
- Chroma v1 API 已被标记为 deprecated
- Starter 名称与正式版不一致
- 配置路径与正式版不同

随着 Spring AI 正式版的发布，需要升级以获得更好的 API 兼容性和稳定性。

## Decision

将 Spring AI 从 1.0.0-M5 升级至 1.1.7，并适配 Chroma v2 API。

关键变更：
1. **Starter 名称变更**：使用 `spring-ai-starter-model-ollama` 和 `spring-ai-starter-vector-store-chroma`
2. **配置路径修正**：使用 `spring.ai.vectorstore.chroma` 而非 `spring.ai.chroma`
3. **Chroma API 升级**：从 v1 API 迁移至 v2 API

## Consequences

- 正面：解决了 Chroma v1 API deprecated 的问题，获得更好的长期维护支持
- 正面：Starter 命名更加规范，与 Spring AI 官方文档一致
- 风险：升级过程中需要验证所有向量存储相关功能的兼容性

## Alternatives Considered

1. **保持 1.0.0-M5**：不推荐，Chroma v1 API 已 deprecated，长期不可维护
2. **升级至其他中间版本**：不如直接升级至 1.1.7 稳定版

## Related Pages

- [[vector-knowledge-service]] - 向量知识服务
- [[common-issues]] - 常见问题与解决方案
