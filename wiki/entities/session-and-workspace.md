---
type: entity
title: 会话与工作区管理
created: 2026-06-26
updated: 2026-06-26
tags: [architecture, session, workspace]
sources:
  - backend/src/main/java/com/zwriter/session/
  - backend/src/main/java/com/zwriter/workspace/
  - backend/src/main/java/com/zwriter/wiki/
  - backend/src/main/java/com/zwriter/agent/base/AgentContext.java
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
key_files:
  - backend/src/main/java/com/zwriter/session/SessionManager.java
  - backend/src/main/java/com/zwriter/workspace/WorkspaceManager.java
  - backend/src/main/java/com/zwriter/workspace/Workspace.java
  - backend/src/main/java/com/zwriter/wiki/WikiService.java
  - backend/src/main/java/com/zwriter/agent/base/AgentContext.java
---

# 会话与工作区管理

## Purpose

Z-writer 的会话与工作区管理提供了多轮对话的会话生命周期管理、文件系统工作区读写、以及内建 Wiki 知识库加载能力，是 Agent 和 Workflow 执行的底层基础设施。

## 架构概览

```
Controller
    ↓
SessionManager ← 会话生命周期（创建/查询/历史/关闭）
    ↓
WorkspaceManager ← 工作区读写（创建/打开/列表/删除）
    ↓
Workspace ← 单个小说工作区（目录结构 + 章节读写）
    + WikiService ← 类型规则加载（内建 Wiki）
    + AgentContext ← 上下文传递（record 类型）
```

---

## SessionManager（会话管理器）

管理多轮对话模式的会话，基于 `ConcurrentHashMap` 存储。

### 会话生命周期

| 方法 | 说明 |
|------|------|
| `createSession(novelName, mode)` | 创建会话，返回 sessionId（UUID） |
| `getSession(sessionId)` | 获取会话 |
| `addMessage(sessionId, role, content)` | 添加消息到对话历史 |
| `getHistory(sessionId)` | 获取对话历史 |
| `updateContext(sessionId, key, value)` | 更新上下文数据 |
| `closeSession(sessionId)` | 关闭会话，清理资源 |

### Session 结构

```java
class Session {
    String id;                    // UUID
    String novelName;             // 关联小说
    String mode;                  // "create" | "continue" | "chat"
    SessionState state;           // ACTIVE | PAUSED | CLOSED
    List<Message> history;        // 对话历史
    Map<String, Object> contextData; // 上下文数据
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
}
```

### API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `POST /api/sessions` | 创建会话 | body: { novelName, mode } |
| `GET /api/sessions/{id}` | 获取会话 | 返回会话详情 |
| `POST /api/sessions/{id}/messages` | 发送消息 | body: { content } |
| `DELETE /api/sessions/{id}` | 关闭会话 | |

---

## WorkspaceManager（工作区管理器）

Spring 组件，管理文件系统上的小说工作区目录。

### 配置

```yaml
zwriter:
  workspace:
    base-path: ./workspaces  # 工作区根目录
```

### 核心方法

| 方法 | 说明 |
|------|------|
| `createNovel(name)` | 创建小说工作区目录 |
| `openNovel(name)` | 打开已有小说工作区 |
| `listNovels()` | 列出所有小说名称 |
| `deleteNovel(name)` | 删除小说工作区 |

### Workspace（工作区）

每个小说的目录结构：

```
workspaces/{novel-name}/
├── novel_info.md
├── worldview.md
├── outline.md
├── characters/
│   └── *.md
└── chapters/
    ├── chapter_001.md
    └── chapter_002.md
```

核心方法：
- `readChapter(chapterNum)` — 读取章节内容
- `writeChapter(chapterNum, content)` — 写入章节
- `listChapters()` — 列出所有章节文件
- `getChapterCount()` — 获取章节数

---

## WikiService（Wiki 知识库服务）

加载内建的类型创作规则，为 Agent 提供类型特定的创作指导。

### 配置

```yaml
zwriter:
  wiki:
    builtin-path: ./wiki  # 内建 Wiki 目录
```

### 核心方法

| 方法 | 说明 |
|------|------|
| `getGenreRule(genre)` | 加载类型规则（`wiki/genres/{genre}.md`） |
| `listGenres()` | 列出所有可用类型 |
| `getBuiltinPath()` | 获取 Wiki 目录路径 |

### API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `GET /api/wiki/genres` | 列出所有类型 | |
| `GET /api/wiki/genres/{genre}` | 获取类型规则 | |

---

## AgentContext（Agent 上下文）

Java 17 `record` 类型，替代旧的 `AgentInput`，作为 Agent 执行的统一上下文对象。

```java
public record AgentContext(
    String taskId,          // 任务 ID
    String taskType,        // 任务类型
    String userInput,       // 用户输入
    String systemPrompt,    // 系统提示词
    String workspacePath,   // 工作区路径
    Map<String, Object> params  // 额外参数
) {}
```

---

## 相关页面

- [[workflow-system]] - 工作流层使用 WorkspaceManager 读写章节
- [[multi-agent-architecture]] - Agent 通过 AgentContext 接收上下文
- [[llm-service-layer]] - LLM 服务层，会话对话调用 LLM
- [[workflow-expansion]] - 扩展工作流，通过 WorkspaceManager 操作文件