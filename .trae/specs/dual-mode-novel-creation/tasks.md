# Tasks - 双模式小说信息搜集

- [x] Task 1: 改造 CreateNovel 页面支持双模式切换
  - [x] SubTask 1.1: 添加模式切换 Tab（表单模式 / 对话模式）
  - [x] SubTask 1.2: 将现有表单内容提取为 `FormMode` 组件
  - [x] SubTask 1.3: 预留 `DialogueMode` 组件占位

- [x] Task 2: 实现对话引导状态管理
  - [x] SubTask 2.1: 创建 `useDialogueGuide` hook，管理 10 个步骤的状态
  - [x] SubTask 2.2: 实现步骤导航逻辑（前进/后退/跳转/跳过）
  - [x] SubTask 2.3: 实现数据同步（对话模式 ↔ 表单模式数据共享）

- [x] Task 3: 实现对话引导 UI 组件
  - [x] SubTask 3.1: 创建 `DialogueMode` 主组件（左右布局：对话区 + 预览区）
  - [x] SubTask 3.2: 实现步骤 1：欢迎 + 写作意图输入
  - [x] SubTask 3.3: 实现步骤 2：赛道选择（选项卡片 + AI 推荐）
  - [x] SubTask 3.4: 实现步骤 3：核心想法/故事梗概（输入 + AI 扩写）
  - [x] SubTask 3.5: 实现步骤 4：标题生成（显示候选列表 + AI 生成）
  - [x] SubTask 3.6: 实现步骤 5：简介生成（3 版本选择 + AI 生成）
  - [x] SubTask 3.7: 实现步骤 6：世界观 - 力量体系（输入 + AI 生成）
  - [x] SubTask 3.8: 实现步骤 7：世界观 - 世界背景（输入 + AI 生成）
  - [x] SubTask 3.9: 实现步骤 8：金手指设定（输入 + AI 生成）
  - [x] SubTask 3.10: 实现步骤 9：预计卷数（选择 + AI 建议）
  - [x] SubTask 3.11: 实现步骤 10：信息确认汇总页
  - [x] SubTask 3.12: 实现实时预览面板（步骤状态标记 + 点击跳转）

- [x] Task 4: 后端自动生成 API
  - [x] SubTask 4.1: 创建 `DialogueGuideController` 和 `DialogueGuideService`
  - [x] SubTask 4.2: 实现赛道推荐生成
  - [x] SubTask 4.3: 实现梗概扩写生成
  - [x] SubTask 4.4: 实现标题生成（3-5 个候选）
  - [x] SubTask 4.5: 实现简介生成（3 个版本）
  - [x] SubTask 4.6: 实现世界观生成（力量体系 + 世界背景）
  - [x] SubTask 4.7: 实现金手指生成
  - [x] SubTask 4.8: 实现卷数建议生成

- [x] Task 5: 端到端验证和优化
  - [x] SubTask 5.1: 测试表单模式功能正常
  - [x] SubTask 5.2: 测试对话模式完整流程（10 个步骤）
  - [x] SubTask 5.3: 测试每个步骤的"自动生成"功能
  - [x] SubTask 5.4: 测试模式切换数据同步
  - [x] SubTask 5.5: 测试步骤跳转和修改
  - [x] SubTask 5.6: 测试提交创建小说流程
  - [x] SubTask 5.7: UI 样式统一和响应式适配

# Task Dependencies
- Task 2 depends on Task 1（状态管理需要在改造后的页面中集成）
- Task 3 depends on Task 2（UI 组件依赖状态管理）
- Task 4 can be parallel with Task 2, Task 3（后端 API 可并行开发，前端先用 mock 数据）
- Task 5 depends on Task 3, Task 4