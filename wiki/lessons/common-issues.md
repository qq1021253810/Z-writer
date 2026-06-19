---
type: lesson
id: LESSON-001
title: 常见问题与解决方案
created: 2026-06-18
updated: 2026-06-18
tags: [lesson]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
linked_issues: [pending-optimizations]
---

# LESSON-001: 常见问题与解决方案

## Trigger

在 Z-writer 项目开发和部署过程中，反复遇到 Chroma API 兼容性、PowerShell 编码、Docker 依赖等问题，需要记录解决方案以避免重复踩坑。

## Verified Lesson

### 1. Chroma v1 API deprecated
- **问题**：Chroma v1 API 已被标记为废弃
- **解决方案**：升级 Spring AI 至 1.1.7，使用 v2 API
- **相关决策**：[[spring-ai-upgrade]]

### 2. Spring AI starter 名称变更
- **问题**：M5 版本的 starter 名称与正式版不同
- **解决方案**：使用 `spring-ai-starter-model-ollama` 和 `spring-ai-starter-vector-store-chroma`

### 3. Chroma 配置路径错误
- **问题**：配置路径使用错误导致连接失败
- **解决方案**：使用 `spring.ai.vectorstore.chroma` 而非 `spring.ai.chroma`

### 4. PowerShell 中文乱码
- **问题**：PowerShell 终端显示中文乱码
- **解决方案**：实际数据正确存储，仅终端显示问题，不影响功能

### 5. Docker Desktop 未启动
- **问题**：后端无法连接 PostgreSQL
- **解决方案**：需先启动 Docker Desktop，确保依赖服务运行

## Evidence

以上问题均来自项目实际开发经验，在 [[vector-knowledge-service|向量知识服务]] 开发和 [[spring-ai-upgrade|Spring AI 升级]] 过程中验证。

## Future Guidance

1. 新项目直接使用 Spring AI 1.1.7+ 版本，避免 M5 版本的兼容性问题
2. 开发环境启动前检查 Docker Desktop 状态
3. PowerShell 中文显示问题不影响数据，无需特殊处理
4. 配置 Spring AI 相关组件时，参考正式版文档确认配置路径
