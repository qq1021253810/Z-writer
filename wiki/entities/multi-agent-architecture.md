---
type: entity
id: ENTITY-001
title: 多智能体架构
name: 多智能体架构
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

# 多智能体架构

## Purpose

Z-writer 采用 1 总控 + 6 子 Agent 的多智能体协作架构，实现网文小说创作流程的分工协作。ControllerAgent 作为总控调度各子 Agent，每个子 Agent 负责创作流程中的特定环节。

## 核心组件

### ControllerAgent（总控 Agent）
- 负责整体创作流程的调度与协调
- 接收用户请求，分配任务给各子 Agent
- 汇总各子 Agent 的输出，形成最终结果

### 6 个子 Agent

| 子 Agent | 职责 | 子任务 |
|----------|------|--------|
| WorldOutline Agent | 世界观与大纲构建 | topic（主题生成）、world（世界构建）、outline（大纲生成）、branch（情节分支） |
| Character Agent | 角色设定与管理 | profile（角色档案）、relation（关系网络）、growth（成长弧线）、dialogue（对话生成） |
| Plot Agent | 情节规划与推进 | golden3（黄金三章）、rhythm（节奏规划）、poison（毒点规避）、emotion（情绪曲线） |
| Writing Agent | 正文撰写 | storyboard（分镜生成）、chapter（章节生成）、scene（场景描写） |
| Polish Agent | 文本润色与优化 | style（风格校准）、polish（文本润色）、transition（章节过渡） |
| Compliance Agent | 合规性检查 | check（内容合规）、tags（标签优化）、summary（摘要生成） |

### Agent 基类设计

所有 Agent 继承自 `BaseAgent`，提供统一的接口：

```java
public abstract class BaseAgent {
    protected abstract String buildPrompt(AgentInput input);
    protected abstract AgentResult processResponse(String response);
    
    public AgentResult execute(AgentInput input) {
        String prompt = buildPrompt(input);
        String response = llmService.chat(prompt);
        return processResponse(response);
    }
}
```

**AgentInput**：包含 novelId、taskType、userInput、params、stream 标志
**AgentResult**：包含 success、content、errorMessage、data Map、durationMs

## 协作流程

1. ControllerAgent 接收创作请求
2. 根据请求类型选择对应 [[workflow-system|工作流]]
3. 工作流按序调用各子 Agent
4. 各子 Agent 可调用 [[external-tool-plugins|外部工具插件]] 辅助创作
5. ControllerAgent 汇总结果返回

## 项目约定

- Agent 统一继承 BaseAgent
- 工作流返回 WorkflowResult 封装结果
- 使用 Lombok 简化代码
- 中文注释和日志

## Current Behavior

多智能体架构已完整实现，支持三种核心工作流（新建小说、续写章节、卡文修复），各子 Agent 可独立运行也可协作完成复杂创作任务。

## Related Pages

- [[workflow-system]] - 工作流系统
- [[external-tool-plugins]] - 外部工具插件
- [[vector-knowledge-service]] - 向量知识服务

## Open Questions

- 无
