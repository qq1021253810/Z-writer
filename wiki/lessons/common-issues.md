---
type: lesson
id: LESSON-001
title: 常见问题与解决方案
created: 2026-06-18
updated: 2026-06-26
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

### 1. 百炼优先 + Ollama 降级策略
- **问题**：单点 LLM 依赖不可靠，API 可能超时或限流
- **解决方案**：通过 `LlmServiceRouter` 实现自动降级，百炼 API 失败时切换 Ollama
- **相关**：[[llm-service-layer]]

### 2. 前后端 ApiResponse 解包一致性
- **问题**：后端返回 `ApiResponse<List<String>>`，前端期望对象数组，`novels.map is not a function`
- **解决方案**：前端 `api.ts` 统一处理 `ApiResponse` 解包，提取 `.data` 字段
- **相关**：[[api-endpoints]]

### 3. 工作区路径问题
- **问题**：后端运行时 workspaces 目录在 `backend/workspaces`，而非项目根目录
- **解决方案**：使用 `WorkspaceManager` 统一管理路径，通过 `@PostConstruct` 初始化
- **相关**：[[session-and-workspace]]

### 4. 章节文件命名格式
- **问题**：后端期望 `chapter-001.md`（连字符），手动创建时用 `chapter_001.md`（下划线）
- **解决方案**：统一使用 `chapter_{number}.md` 格式

### 5. Docker Desktop 未启动
- **问题**：后端无法连接 PostgreSQL
- **解决方案**：需先启动 Docker Desktop，确保依赖服务运行

## Evidence

以上问题均来自项目实际开发经验，在 LLM 降级机制实现、前后端连通性验证、工作流测试等过程中验证。

## Future Guidance

1. 新项目直接使用 Spring AI 1.1.7+ 版本，避免 M5 版本的兼容性问题
2. 开发环境启动前检查 Docker Desktop 状态
3. PowerShell 中文显示问题不影响数据，无需特殊处理
4. 配置 Spring AI 相关组件时，参考正式版文档确认配置路径
