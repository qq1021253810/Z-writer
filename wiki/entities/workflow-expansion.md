---
type: entity
title: 扩展工作流
created: 2026-06-26
updated: 2026-06-26
tags: [architecture, workflow]
sources: [backend/src/main/java/com/zwriter/workflow/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
key_files:
  - backend/src/main/java/com/zwriter/workflow/StrategyWorkflow.java
  - backend/src/main/java/com/zwriter/workflow/ReviewWorkflow.java
  - backend/src/main/java/com/zwriter/workflow/PolishWorkflow.java
  - backend/src/main/java/com/zwriter/workflow/AsyncWorkflowExecutor.java
  - backend/src/main/java/com/zwriter/workflow/WorkflowTask.java
---

# 扩展工作流

## Purpose

在 [[workflow-system|核心工作流]]（CreateNovel/ContinueChapter/FixWriterBlock）之外，Z-writer 新增了三个扩展工作流，分别负责战略规划、质量审计和文本润色。所有工作流通过 `AsyncWorkflowExecutor` 统一调度，支持异步执行和 SSE 进度推送。

## StrategyWorkflow（战略规划工作流）

- **Agent**：`StrategyAgent`
- **用途**：总体战略规划、多线叙事编织、主题深化、长线布局
- **策略类型**：
  - `master_plan` — 总体战略规划
  - `thread_weave` — 多线叙事编织
  - `theme_deepen` — 主题深化
- **流程**：openNovel → executeStrategy → 返回 WorkflowResult
- **依赖**：WorkspaceManager（读取小说内容）、StrategyAgent（LLM 分析）

## ReviewWorkflow（质量审计工作流）

- **Agent**：`ReviewAgent`
- **用途**：章节逻辑检查、角色一致性校验、文风统一审查、智商门禁
- **审计类型**：
  - `logic` — 逻辑检查
  - `character` — 角色一致性
  - `style` — 文风统一
  - `iq_gate` — 智商门禁（确保角色行为符合高智商设定）
- **流程**：openNovel → readChapter → reviewChapter → 返回 WorkflowResult
- **依赖**：WorkspaceManager（读取章节）、ReviewAgent（LLM 审计）

## PolishWorkflow（润色工作流）

- **Agent**：`PolishAgent`
- **用途**：文风校准、语言润色、章节衔接优化
- **润色类型**：
  - `style` — 文风校准
  - `language` — 语言润色
  - `transition` — 章节衔接
- **流程**：openNovel → readChapter → polishChapter → 返回 WorkflowResult
- **依赖**：WorkspaceManager（读取章节）、PolishAgent（LLM 润色）

## AsyncWorkflowExecutor（异步工作流执行器）

统一管理所有工作流的异步执行和进度推送：

- **任务注册**：`registerTask(workflowName)` → 返回 `WorkflowTask`
- **任务提交**：`submit(taskId, runnable)` → 虚拟线程执行
- **进度推送**：`updateProgress(taskId, step, data)` → SSE 推送
- **状态查询**：`getStatus(taskId)` → 返回 `WorkflowTask`
- **SSE 订阅**：`subscribe(taskId, emitter)` → 注册 SseEmitter

## WorkflowTask（任务定义）

```java
public class WorkflowTask {
    String taskId;           // UUID
    String workflowName;     // 工作流名称
    TaskStatus status;       // PENDING / RUNNING / COMPLETED / FAILED
    List<ProgressEvent> progress; // 进度事件列表
    Object result;           // 最终结果
    String error;            // 错误信息
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `POST /api/workflows/create-novel` | 异步 | 创建小说工作流 |
| `POST /api/workflows/continue` | 异步 | 续写章节工作流 |
| `POST /api/workflows/fix-block` | 同步 | 卡文修复工作流 |
| `POST /api/workflows/polish` | 同步 | 润色章节 |
| `POST /api/workflows/review` | 同步 | 审计章节 |
| `POST /api/workflows/strategy` | 同步 | 战略规划 |
| `GET /api/workflows/{taskId}/status` | SSE | 订阅任务进度 |

## 相关页面

- [[workflow-system]] - 核心工作流（CreateNovel/ContinueChapter/FixWriterBlock）
- [[llm-service-layer]] - LLM 服务层（工作流调用 LLM 的入口）
- [[multi-agent-architecture]] - 各工作流对应的 Agent（StrategyAgent/ReviewAgent/PolishAgent）
- [[session-and-workspace]] - 工作区管理（WorkspaceManager 读写章节内容）