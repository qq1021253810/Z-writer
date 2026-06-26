---
type: issue
id: ISSUE-001
title: 待优化功能
status: open
created: 2026-06-18
updated: 2026-06-26
tags: [issue]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
resolved_by: ""
verified_by: human
approved: true
---

# ISSUE-001: 待优化功能

## Symptom

项目存在多个待优化方向，包括功能缺失、性能瓶颈和用户体验不足。

## Investigation

### 1. 网文榜单爬虫插件
- **需求**：抓取起点/番茄/七猫等平台的爆款标签、热门题材
- **状态**：未实现
- **优先级**：中

### 2. 翻译工具
- **需求**：古言白话文互转、古风专有名词释义
- **状态**：未实现
- **优先级**：中

### 3. 性能优化
- **向量检索缓存**：当前使用 ConcurrentHashMap，可升级为 Caffeine/Redis
- **前端代码分割**：已优化（SSE 流式渲染已实现）
- **相关页面**：[[vector-knowledge-service]]

### 4. 已完成项
- **SSE 流式渲染基础设施**：已实现（sse.ts + useWorkflowSSE）
- **前端实时反馈**：已通过 SSE 进度推送实现
- **工作流进度可视化**：已通过 AsyncWorkflowExecutor + SSE 实现
- **LLM 降级机制**：已实现（百炼优先 + Ollama 降级）

## Resolution

Set status: resolved and resolved_by: LESSON-XXX after a human-approved lesson captures the fix.

## Related Pages

- [[vector-knowledge-service]] - 向量知识服务
- [[workflow-system]] - 工作流系统
- [[common-issues]] - 常见问题与解决方案
