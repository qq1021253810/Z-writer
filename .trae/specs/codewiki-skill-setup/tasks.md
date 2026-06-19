# Tasks

- [x] Task 1: 安装 CodeWiki CLI 并在项目中初始化
  - [x] SubTask 1.1: 全局安装 @giuice/codewiki npm 包
  - [x] SubTask 1.2: 在 Z-writer 项目根目录执行 `npx @giuice/codewiki init --name "Z-writer"`
  - [x] SubTask 1.3: 验证生成的目录结构（wiki/、.codewiki/）

- [x] Task 2: 创建 CodeWiki 全局技能（Trae IDE 格式）
  - [x] SubTask 2.1: 创建 codewiki-ingest 技能（知识摄取）
  - [x] SubTask 2.2: 创建 codewiki-query 技能（知识查询）
  - [x] SubTask 2.3: 创建 codewiki-lint 技能（知识审计）
  - [x] SubTask 2.4: 创建 codewiki-absorb 技能（知识吸收）
  - [x] SubTask 2.5: 创建 codewiki-breakdown 技能（知识分解）
  - [x] SubTask 2.6: 创建 codewiki-prd 技能（PRD 生成）
  - [x] SubTask 2.7: 创建 codewiki-tasks 技能（任务生成）
  - [x] SubTask 2.8: 创建 codewiki-process 技能（任务执行）
  - [x] SubTask 2.9: 创建 codewiki-flow 技能（自动工作流）
  - [x] SubTask 2.10: 创建 codewiki-obsidian 技能（Obsidian 导出）

- [x] Task 3: 将现有项目文档导入知识库
  - [x] SubTask 3.1: 将 AGENT_MEMORY.md 复制到 wiki/raw/ 目录
  - [x] SubTask 3.2: 执行 codewiki-ingest 生成初始 wiki 页面
  - [x] SubTask 3.3: 验证生成的 wiki 页面内容

- [x] Task 4: 验证全局技能可用性
  - [x] SubTask 4.1: 确认全局技能目录中所有 SKILL.md 文件格式正确
  - [x] SubTask 4.2: 测试技能在对话中的触发和执行

# Task Dependencies
- [Task 2] depends on [Task 1]（需要先了解 CodeWiki 生成的技能结构）
- [Task 3] depends on [Task 1]（需要先初始化 CodeWiki）
- [Task 4] depends on [Task 2, Task 3]
