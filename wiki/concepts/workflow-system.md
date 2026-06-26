---
type: concept
id: CONCEPT-001
title: 工作流系统
created: 2026-06-18
updated: 2026-06-26
tags: [concept, architecture]
sources: [wiki/raw/specs/agent-memory.md]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 工作流系统

## Definition

Z-writer 的工作流系统定义了 6 种创作工作流（3 种核心 + 3 种扩展），每种工作流编排 [[multi-agent-architecture|多智能体架构]] 中的子 Agent 按特定顺序协作完成创作任务。工作流层位于 `com.zwriter.workflow` 包，返回 WorkflowResult 封装结果。

## Current Understanding

### 核心工作流

#### CreateNovelWorkflow（新建小说工作流）
- 用途：从零开始创建一部新小说
- **7 步流程**：
  1. createNovelInfo - 创建小说基本信息
  2. generateTopic - 生成小说主题
  3. buildWorld - 构建世界观
  4. createMainCharacter - 创建主角设定
  5. generateOutline - 生成卷大纲
  6. designGolden3 - 设计黄金三章
  7. calculateTotalWords - 计算总字数规划
- 集成工具：字数统计

### ContinueChapterWorkflow（续写章节工作流）
- 用途：基于已有内容续写下一章节
- **9 步流程**：
  1. retrieveContext - 检索上下文（前情提要、相关段落）
  2. getNovelInfo - 获取小说基本信息
  3. designRhythm - 设计章节节奏
  4. generateStoryboard - 生成分镜大纲
  5. generateChapter - 生成章节正文
  6. polishChapter - 润色章节
  7. checkCompliance - 合规性检查
  8. saveChapter - 保存章节到数据库
  9. storeToVectorDB - 存储到向量数据库
- 集成工具：字数统计、违禁词检测
- 依赖：[[vector-knowledge-service|向量知识服务]] 提供上下文检索

### FixWriterBlockWorkflow（卡文修复工作流）
- 用途：帮助作者突破写作瓶颈
- **4 步流程**：
  1. analyzeBlockPoint - 分析卡文原因（情节断裂、角色动机不足、世界观冲突等）
  2. generateSolutions - 生成多种续写方案（3-5 个备选）
  3. rewriteContent - 重写卡文段落
  4. polishContent - 润色重写内容
- 集成工具：违禁词检测

### 扩展工作流

#### StrategyWorkflow（战略规划工作流）
- 用途：总体战略规划、多线叙事编织、主题深化、长线布局
- **3 种策略类型**：master_plan（总体规划）、thread_weave（多线编织）、theme_deepen（主题深化）
- 依赖：[[llm-service-layer|LLM 服务层]]、[[session-and-workspace|WorkspaceManager]]

#### ReviewWorkflow（质量审计工作流）
- 用途：章节逻辑检查、角色一致性、文风统一、智商门禁
- **4 种审计类型**：logic（逻辑检查）、character（角色一致性）、style（文风统一）、iq_gate（智商门禁）
- 依赖：[[llm-service-layer|LLM 服务层]]、[[session-and-workspace|WorkspaceManager]]

#### PolishWorkflow（润色工作流）
- 用途：文风校准、语言润色、章节衔接优化
- **3 种润色类型**：style（文风校准）、language（语言润色）、transition（章节衔接）
- 依赖：[[llm-service-layer|LLM 服务层]]、[[session-and-workspace|WorkspaceManager]]

## 异步执行

所有工作流通过 `AsyncWorkflowExecutor` 支持异步执行 + SSE 进度推送：
- 任务注册 → 虚拟线程执行 → 进度事件推送 → 结果返回
- 详见 [[workflow-expansion|扩展工作流]]

## 工作流与工具集成

| 工作流 | 集成的 [[external-tool-plugins|工具]] |
|--------|--------------------------------------|
| CreateNovelWorkflow | WordCountTool |
| ContinueChapterWorkflow | WordCountTool, BannedWordTool |
| FixWriterBlockWorkflow | BannedWordTool |

## 工作流基类设计

所有工作流继承自 `BaseWorkflow<REQ, RES>`，使用模板方法模式：

```java
public abstract class BaseWorkflow<REQ, RES extends BaseWorkflowResult> {
    public final RES execute(REQ request) {
        // 1. 执行核心业务逻辑
        RES result = doExecute(request);
        // 2. 自动执行字数统计
        performWordCount(result);
        // 3. 自动执行违禁词检测
        performBannedWordCheck(result);
        // 4. 自动执行内容检查
        performContentChecks(result);
        return result;
    }
    
    protected abstract RES doExecute(REQ request);
}
```

**BaseWorkflowResult**：包含 success、errorMessage、durationMs、wordCountResult、bannedWordResult、extensions

## Related Pages

- [[multi-agent-architecture]] - 多智能体架构
- [[external-tool-plugins]] - 外部工具插件
- [[vector-knowledge-service]] - 向量知识服务
- [[context-service-layer]] - 上下文服务层
- [[data-model]] - 数据模型层

## Open Questions

- 无
