---
type: issue
id: ISSUE-001
title: 待优化功能
status: open
created: 2026-06-18
updated: 2026-06-18
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
- **向量检索缓存**：当前使用 ConcurrentHashMap，可升级为 Caffeine/Redis 以提升性能和可扩展性
- **前端代码分割**：当前 chunk 超过 500KB，需要优化
- **相关页面**：[[vector-knowledge-service]]

### 4. 用户体验优化
- **前端实时反馈**：可增加更多实时反馈机制
- **工作流进度可视化**：当前工作流执行过程缺乏可视化展示
- **相关页面**：[[workflow-system]]

## Resolution

Set status: resolved and resolved_by: LESSON-XXX after a human-approved lesson captures the fix.

## Related Pages

- [[vector-knowledge-service]] - 向量知识服务
- [[workflow-system]] - 工作流系统
- [[common-issues]] - 常见问题与解决方案
