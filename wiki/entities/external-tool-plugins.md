---
type: entity
id: ENTITY-002
title: 外部工具插件
name: 外部工具插件
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, adapter]
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

# 外部工具插件

## Purpose

Z-writer 提供 6 个外部工具插件，为创作流程提供字数统计、违禁词检测、角色关系分析、查重、人设一致性校验和思维导图导出等辅助能力。工具类位于 `com.zwriter.tool` 包，使用 `@Component + @RequiredArgsConstructor` 注解。

## 工具清单

### WordCountTool（字数统计）
- 功能：总字数、章节字数、日均字数、字数规划
- API：
  - `POST /api/word-count/text`：文本字数统计
  - `GET /api/word-count/novel/{novelId}`：小说总字数统计
  - `GET /api/word-count/chapter/{chapterId}`：章节字数统计
- 前端集成：字数统计面板（实时统计中文字数、英文字母、数字等，500ms 防抖）

### BannedWordTool（违禁词检测）
- 功能：100+ 违禁词、40+ 敏感词、自定义词库、三级敏感度
- API：
  - `POST /api/banned-word/detect`：违禁词检测（支持自定义选项）
  - `POST /api/banned-word/replace`：违禁词替换
- 前端集成：违禁词检测面板（风险等级评估、违禁词/敏感词高亮显示、一键替换、高级选项）

### CharacterRelationTool（角色关系图生成）
- 功能：支持 Mermaid 格式输出
- API：
  - `GET /api/character-relation/graph/{novelId}`：角色关系图数据
  - `GET /api/character-relation/mermaid/{novelId}`：Mermaid 格式关系图
- 前端集成：角色关系图面板（Mermaid 渲染）

### PlagiarismDetector（查重检测）
- 功能：N-gram + Jaccard 相似度算法
- API：
  - `POST /api/plagiarism/detect`：查重检测
  - `POST /api/plagiarism/similarity`：计算文本相似度
- 前端集成：查重检测面板（相似度/风险等级/相似片段）

### CharacterConsistencyChecker（人设一致性校验）
- 功能：对话/行为/战力三维度校验
- API：
  - `POST /api/consistency/check`：人设一致性校验
  - `GET /api/consistency/character/{characterId}/profile`：角色人设档案
- 前端集成：人设一致性校验面板

### MindMapExporter（思维导图导出）
- 功能：Mermaid/Markdown/JSON 三种格式导出
- API：
  - `GET /api/mindmap/mermaid/{novelId}`：导出 Mermaid 格式
  - `GET /api/mindmap/markdown/{novelId}`：导出 Markdown 格式
  - `GET /api/mindmap/json/{novelId}`：导出 JSON 格式
- 前端集成：大纲思维导图面板

## 编码规范

- Tool 类使用 `@Slf4j + @Component + @RequiredArgsConstructor`
- Controller 使用 `@Slf4j + @RestController + @RequiredArgsConstructor`
- API 返回格式：`{"success": true/false, "data": ..., "message": ...}`

## 工作流集成

- 续写章节工作流自动调用字数统计和违禁词检测
- 新建小说工作流集成字数统计
- 卡文修复工作流集成违禁词检测

## Current Behavior

6 个工具插件已全部实现并集成到前端编辑器的"创作工具"标签页中，工作流也已集成相关工具调用。

## Related Pages

- [[multi-agent-architecture]] - 多智能体架构
- [[workflow-system]] - 工作流系统

## Open Questions

- 无
