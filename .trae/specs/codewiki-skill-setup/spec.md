# CodeWiki 知识库技能构建 Spec

## Why
当前项目缺乏系统化的代码知识库管理机制，AI 助手每次会话都需要重新探索代码库，无法复用历史知识。需要引入 CodeWiki 框架，将代码库转化为持久化、LLM 维护的知识系统，并封装为 Trae IDE 全局技能，使所有项目都能受益。

## What Changes
- 安装 @giuice/codewiki (v0.2.15) CLI 工具
- 在项目中初始化 CodeWiki，生成 wiki 目录结构和配置
- 将 CodeWiki 的 10 个核心技能适配为 Trae IDE 技能格式（SKILL.md）
- 将适配后的技能注册为全局技能（c:\Users\Administrator\.trae-cn\builtin\global\skills\）
- 在项目中执行首次知识摄取（codewiki-ingest）

## Impact
- Affected specs: 无现有 spec 受影响
- Affected code: 新增 wiki/ 目录、.codewiki/ 配置目录、全局技能文件
- 新增依赖: @giuice/codewiki npm 包

## ADDED Requirements

### Requirement: CodeWiki 全局技能
系统 SHALL 提供一个名为 `codewiki` 的全局技能，使 Trae IDE 在任何项目中都能使用 CodeWiki 知识库功能。

#### Scenario: 首次初始化知识库
- **WHEN** 用户在项目中请求初始化知识库
- **THEN** 系统执行 `npx @giuice/codewiki init`，生成 wiki/ 目录、.codewiki/ 配置、技能文件

#### Scenario: 知识摄取
- **WHEN** 用户请求将现有文档导入知识库
- **THEN** 系统调用 codewiki-ingest 技能，将 wiki/raw/ 中的文档消化为结构化 wiki 页面

#### Scenario: 知识查询
- **WHEN** 用户询问项目相关问题
- **THEN** 系统调用 codewiki-query 技能，从 wiki 知识库中检索答案

#### Scenario: 知识维护
- **WHEN** 用户请求审计知识库质量
- **THEN** 系统调用 codewiki-lint 技能，检测矛盾、孤立页面、过时声明

#### Scenario: 知识吸收
- **WHEN** 完成一个开发阶段后
- **THEN** 系统调用 codewiki-absorb 技能，从代码差异中提取持久知识

#### Scenario: 功能规划
- **WHEN** 用户请求规划新功能
- **THEN** 系统依次调用 codewiki-prd 和 codewiki-tasks 技能，生成 PRD 和任务列表

#### Scenario: 自动工作流
- **WHEN** 用户不确定该执行哪个 CodeWiki 操作
- **THEN** 系统调用 codewiki-flow 技能，根据仓库状态自动选择下一步操作

### Requirement: 技能适配 Trae IDE 格式
系统 SHALL 将 CodeWiki 的 10 个技能适配为 Trae IDE 的 SKILL.md 格式，放置在全局技能目录中。

#### Scenario: 技能文件格式
- **WHEN** 技能被创建
- **THEN** 每个技能目录包含 SKILL.md 文件，具有正确的 YAML frontmatter（name + description）和 Markdown body

#### Scenario: 技能触发
- **WHEN** 用户在对话中提及知识库、wiki、代码文档、项目知识等相关话题
- **THEN** Trae IDE 自动识别并加载对应的 codewiki 技能

### Requirement: 项目级知识库初始化
系统 SHALL 在 Z-writer 项目中初始化 CodeWiki 知识库，将现有项目文档导入。

#### Scenario: 项目初始化
- **WHEN** 执行 codewiki init
- **THEN** 生成 wiki/ 目录（含 raw/、entities/、decisions/、lessons/、issues/、sources/ 子目录）和 .codewiki/ 配置目录

#### Scenario: 现有文档导入
- **WHEN** 将 AGENT_MEMORY.md 等现有文档放入 wiki/raw/ 并执行 ingest
- **THEN** 系统生成结构化的 wiki 页面，包含实体、决策、经验教训等
