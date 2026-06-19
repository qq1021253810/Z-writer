---
type: entity
title: 对话引导系统
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, frontend, service]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 对话引导系统

对话引导系统提供 AI 驱动的小说创建流程，通过 10 步引导帮助用户逐步构建小说设定。

## 架构概览

```
前端 (DialogueMode 组件)
    ↓ useDialogueGuide Hook
后端 (DialogueGuideService)
    ↓ LlmService
LLM (Ollama)
```

## 10 步引导流程

| 步骤 | 类型 | 说明 |
|------|------|------|
| 1 | intent | 确认创作意图 |
| 2 | genre | 选择小说类型（7 种选项） |
| 3 | synopsis | 生成故事梗概 |
| 4 | title | 生成小说标题 |
| 5 | synopsis_v2 | 优化故事梗概 |
| 6 | power_system | 生成力量体系 |
| 7 | world_background | 生成世界观背景 |
| 8 | golden_finger | 生成金手指设定 |
| 9 | total_volumes | 建议总卷数 |
| 10 | confirm | 确认所有设定 |

## 后端：DialogueGuideService

路径：`backend/src/main/java/com/zwriter/service/DialogueGuideService.java`

### 支持的步骤类型

| 步骤类型 | 方法 | 说明 |
|----------|------|------|
| recommendGenre | generateGenreSuggestions | 推荐小说类型 |
| expandSynopsis | generateSynopsisExpansion | 扩展故事梗概 |
| generateTitles | generateTitleSuggestions | 生成标题建议 |
| generateSynopses | generateSynopsisSuggestions | 生成梗概建议 |
| generatePowerSystem | generatePowerSystem | 生成力量体系 |
| generateWorldBackground | generateWorldBackground | 生成世界观背景 |
| generateGoldenFinger | generateGoldenFinger | 生成金手指设定 |
| suggestVolumeCount | suggestVolumeCount | 建议卷数 |

### 容错机制

LLM 调用失败时，使用默认值回退，确保引导流程不中断。

## 前端：useDialogueGuide Hook

路径：`frontend/src/hooks/useDialogueGuide.ts`

### 状态管理

```typescript
interface DialogueGuideState {
  currentStep: number;        // 当前步骤 (1-10)
  suggestions: string[];      // 当前步骤的建议列表
  formData: NovelFormData;    // 已收集的表单数据
  isLoading: boolean;         // 加载状态
}
```

### 核心功能

1. **步骤导航**：`nextStep()`, `prevStep()`, `goToStep(n)`
2. **建议选择**：`selectSuggestion(suggestion)` - 选择建议并推进到下一步
3. **自动生成**：`autoGenerate()` - 调用后端获取当前步骤建议
4. **表单提取**：`extractFormData()` - 从引导结果提取表单数据

## 前端：DialogueMode 组件

路径：`frontend/src/components/create-novel/DialogueMode.tsx`

### 功能

- AI 引导式小说创建
- 步骤进度指示器
- 建议卡片展示
- 自动生成功能
- 确认视图（步骤 10）

### 与 FormMode 的关系

`CreateNovel` 页面提供两种创建模式：
- **FormMode** - 传统表单填写
- **DialogueMode** - AI 引导式创建

两者最终都调用 `workflowService.createNovel()` 创建小说。

## API 端点

| 端点 | 方法 | 说明 |
|------|------|------|
| `POST /api/dialogue-guide/generate` | generate | 生成当前步骤建议 |

请求体：
```json
{
  "step": "genre",
  "novelId": null,
  "context": { ... }
}
```

## 相关页面

- [[workflow-system]] - 工作流系统，对话引导最终调用创建工作流
- [[multi-agent-architecture]] - 多智能体架构，对话引导使用 LLM 服务
- [[context-service-layer]] - 上下文服务层，为对话引导提供创作上下文
