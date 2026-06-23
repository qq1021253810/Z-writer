---
type: concept
title: API 接口文档
created: 2026-06-18
updated: 2026-06-18
tags: [api, rest, endpoints]
sources: [backend/src/main/java/com/zwriter/controller/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# API 接口文档

Z-writer 后端提供 13 个 REST API 控制器，共 50+ 个接口端点。

## 基础信息

| 项 | 值 |
|----|----|
| 基础 URL | `http://localhost:8080/api` |
| Content-Type | `application/json` |
| 超时配置 | 工作流 60s，其他 10-30s |

## 接口分类

### 1. 工作流接口（WorkflowController）

| 方法 | 端点 | 说明 | 超时 |
|------|------|------|------|
| POST | `/workflow/create-novel` | 新建小说工作流 | 60s |
| POST | `/workflow/continue-chapter` | 续写章节工作流 | 60s |
| POST | `/workflow/fix-writer-block` | 卡文修复工作流 | 60s |

**POST /workflow/create-novel**

请求体：
```json
{
  "title": "小说标题",
  "genre": "玄幻",
  "synopsis": "故事梗概",
  "goldenFinger": "金手指设定",
  "totalVolumes": 1,
  "generateTopic": true
}
```

响应体：
```json
{
  "success": true,
  "novelId": 1,
  "topic": "...",
  "worldSetting": "...",
  "characterProfile": "...",
  "outline": "...",
  "golden3Design": "...",
  "durationMs": 15000
}
```

### 2. 小说管理接口（NovelController）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/novels` | 获取小说列表 |
| GET | `/novels/{id}` | 获取小说详情 |
| POST | `/novels` | 创建小说 |
| PUT | `/novels/{id}` | 更新小说 |
| DELETE | `/novels/{id}` | 删除小说 |

### 3. 伏笔管理接口（ForeshadowController）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/foreshadow` | 添加伏笔 |
| GET | `/foreshadow/novel/{novelId}` | 获取小说所有伏笔 |
| GET | `/foreshadow/novel/{novelId}/planted` | 获取未回收伏笔 |
| GET | `/foreshadow/novel/{novelId}/resolved` | 获取已回收伏笔 |
| PUT | `/foreshadow/{id}/resolve` | 回收伏笔 |
| GET | `/foreshadow/novel/{novelId}/conflicts` | 检测伏笔冲突 |
| GET | `/foreshadow/novel/{novelId}/overdue?currentChapter=N` | 获取超期伏笔 |

### 4. 字数统计接口（WordCountController）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/word-count/text` | 统计文本字数 |
| GET | `/word-count/novel/{novelId}` | 统计小说总字数 |
| GET | `/word-count/chapter/{chapterId}` | 统计章节字数 |
| GET | `/word-count/daily-average/{novelId}?days=30` | 计算日均字数 |
| POST | `/word-count/plan` | 生成字数规划 |

### 5. 违禁词检测接口（BannedWordController）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/banned-word/detect` | 检测文本违禁词 |
| POST | `/banned-word/replace` | 替换违禁词 |

**POST /banned-word/detect**

请求体：
```json
{
  "text": "要检测的文本",
  "checkBannedWords": true,
  "checkSensitiveWords": true,
  "sensitivityLevel": "standard",
  "customWords": ["自定义词1", "自定义词2"]
}
```

### 6. 角色关系接口（CharacterRelationController）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/character-relation/graph/{novelId}` | 获取角色关系图数据 |
| GET | `/character-relation/mermaid/{novelId}` | 获取 Mermaid 格式关系图 |

### 7. 人设一致性接口（ConsistencyController）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/consistency/check` | 检查人设一致性 |
| GET | `/consistency/character/{characterId}/profile` | 获取角色档案 |

### 8. 查重接口（PlagiarismController）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/plagiarism/detect` | 检测抄袭 |
| POST | `/plagiarism/similarity` | 计算相似度 |

### 9. 思维导图接口（MindMapController）

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/mindmap/mermaid/{novelId}` | 获取 Mermaid 思维导图 |
| GET | `/mindmap/markdown/{novelId}` | 获取 Markdown 思维导图 |
| GET | `/mindmap/json/{novelId}` | 获取 JSON 思维导图 |

### 10. 设定同步接口（SettingSyncController）

| 方法 | 端点 | 说明 |
|------|------|------|
| POST | `/setting-sync/character` | 同步角色名变更 |
| POST | `/setting-sync/worldview` | 同步世界观变更 |
| POST | `/setting-sync/power-level` | 同步战力等级变更 |
| GET | `/setting-sync/affected-chapters` | 获取受影响章节 |

### 11. Agent 接口（AgentController）

| 方法 | 端点 | 说明 | 超时 |
|------|------|------|------|
| POST | `/agent/execute` | 执行 Agent 任务 | 30s |

### 12. LLM 接口（LlmController）

| 方法 | 端点 | 说明 | 超时 |
|------|------|------|------|
| POST | `/llm/chat` | LLM 对话 | 30s |
| POST | `/llm/chat/stream` | LLM 流式对话（SSE） | 60s |

### 13. 对话引导接口（DialogueGuideController）

| 方法 | 端点 | 说明 | 超时 |
|------|------|------|------|
| POST | `/dialogue-guide/generate` | 生成引导建议 | 30s |

**POST /dialogue-guide/generate**

请求体：
```json
{
  "stepType": "genre",
  "userInput": "用户输入",
  "existingInfo": {...}
}
```

支持的 stepType：`intent`, `genre`, `synopsis`, `title`, `synopsis_v2`, `power_system`, `world_background`, `golden_finger`, `total_volumes`, `confirm`

## 错误响应格式

```json
{
  "success": false,
  "errorMessage": "错误描述",
  "durationMs": 1200
}
```

## 前端服务封装

前端在 `frontend/src/services/api.ts` 中封装了以下服务：

| 服务 | 封装的 API |
|------|-----------|
| `workflowService` | createNovel, continueChapter, fixWriterBlock |
| `novelService` | getNovelList, getNovelDetail, createNovel, updateNovel, deleteNovel |
| `agentService` | execute |
| `dialogueGuideService` | generate |

## 相关页面

- [[workflow-system]] - 工作流系统（API 调用的后端逻辑）
- [[data-model]] - 数据模型层（API 操作的实体）
- [[external-tool-plugins]] - 外部工具插件（部分 API 使用的工具）
