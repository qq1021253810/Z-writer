---
type: entity
title: LLM 服务层
created: 2026-06-26
updated: 2026-06-26
tags: [architecture, llm, adapter]
sources: [backend/src/main/java/com/zwriter/llm/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
key_files:
  - backend/src/main/java/com/zwriter/llm/LlmService.java
  - backend/src/main/java/com/zwriter/llm/LlmServiceRouter.java
  - backend/src/main/java/com/zwriter/llm/DashScopeLlmService.java
  - backend/src/main/java/com/zwriter/llm/OllamaLlmService.java
  - backend/src/main/java/com/zwriter/llm/MockLlmService.java
---

# LLM 服务层

## Purpose

Z-writer 的 LLM 服务层采用**百炼优先 + Ollama 降级**的双引擎路由策略，通过 `LlmServiceRouter` 统一对外暴露 `LlmService` 接口，对上层 Agent 和工作流透明。

## 架构设计

```
Agent / Workflow
    ↓
LlmServiceRouter (@Primary)  ← 百炼优先，失败降级 Ollama
    ├── DashScopeLlmService  → 百炼 DashScope API (qwen-plus)
    └── OllamaLlmService     → Ollama 本地 API (qwen3:8b)
```

**关键决策**：不再依赖 Spring AI 的自动配置和封装，改用 Spring AI 的 `ChatClient` 直接调用 HTTP API。百炼通过 OpenAI 兼容接口（`spring.ai.openai` 配置），Ollama 通过 `spring.ai.ollama` 配置。

## LlmService 接口

```java
public interface LlmService {
    String chat(String prompt, String systemPrompt);           // 同步调用
    Flux<String> chatStream(String prompt, String systemPrompt); // 流式调用（SSE）
    String chatWithContext(String prompt, String systemPrompt, String context); // 带上下文
    float[] embed(String text);                                // 文本向量化
}
```

## LlmServiceRouter（路由策略）

- 标注 `@Primary`，Spring 注入时优先使用
- 所有方法先调用 `DashScopeLlmService`，失败时自动降级到 `OllamaLlmService`
- 降级判断：`DashScopeLlmService` 返回以 `【错误】` 开头，或抛出异常
- 流式调用降级时，先检查第一个元素，失败则切换到 Ollama 流

## DashScopeLlmService（百炼实现）

- 模型：`qwen-plus`（百炼 DashScope）
- 接口：OpenAI 兼容模式（`https://dashscope.aliyuncs.com/compatible-mode`）
- 实现方式：Spring AI `ChatClient` + `ChatModel`（Qualifier: `openAiChatModel`）
- 配置路径：`application.yml` → `spring.ai.openai`
- 向量化：当前为 TODO 占位，返回空数组

## OllamaLlmService（本地降级实现）

- 模型：`qwen3:8b`（本地 Ollama）
- 接口：`http://localhost:11434`
- 实现方式：Spring AI `ChatClient` + `ChatModel`（Qualifier: `ollamaChatModel`）
- 配置路径：`application.yml` → `spring.ai.ollama`
- 向量化：当前为 TODO 占位，返回空数组

## MockLlmService（测试用）

- 实现 `LlmService` 接口
- 返回固定文本，不调用真实 LLM
- 用于单元测试和离线开发

## 配置示例

```yaml
spring:
  ai:
    openai:
      api-key: ${DASHSCOPE_API_KEY}
      base-url: https://dashscope.aliyuncs.com/compatible-mode
      chat:
        options:
          model: qwen-plus
          temperature: 0.8
          max-tokens: 4096
    ollama:
      base-url: http://localhost:11434
      chat:
        options:
          model: qwen3:8b
          temperature: 0.8
          max-tokens: 4096
```

## 相关页面

- [[multi-agent-architecture]] - 所有 Agent 通过 LlmServiceRouter 调用 LLM
- [[workflow-system]] - 工作流层使用 LlmService 执行创作任务
- [[infrastructure-stack]] - 基础设施配置