---
type: entity
title: 前端组件架构
created: 2026-06-18
updated: 2026-06-18
tags: [frontend, react, components]
sources: [frontend/src/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# 前端组件架构

Z-writer 前端基于 React 18 + Vite 构建，使用 shadcn/ui 组件库和 Tailwind CSS。

## 技术栈

| 组件 | 技术 | 版本 |
|------|------|------|
| 框架 | React | 18 |
| 构建工具 | Vite | 6.x |
| 路由 | react-router-dom | 6.x |
| 组件库 | shadcn/ui | latest |
| 样式 | Tailwind CSS | 3.x |
| 图标 | Lucide React | latest |
| HTTP | axios | latest |
| 图表 | Mermaid | latest |

## 项目结构

```
frontend/src/
├── main.tsx           # 入口文件
├── App.tsx            # 根组件（路由配置）
├── pages/             # 页面组件
│   ├── NovelList.tsx  # 小说列表页（首页）
│   ├── CreateNovel.tsx # 新建小说页
│   └── NovelEditor.tsx # 小说编辑器页
├── components/
│   ├── create-novel/  # 新建小说组件
│   │   ├── FormMode.tsx      # 表单模式
│   │   └── DialogueMode.tsx  # AI 对话引导模式
│   ├── ui/            # UI 基础组件（shadcn/ui）
│   │   ├── button.tsx
│   │   ├── card.tsx
│   │   ├── tabs.tsx
│   │   └── ...
│   ├── theme-provider.tsx    # 主题提供者
│   └── theme-toggle.tsx      # 主题切换按钮
├── hooks/             # 自定义 Hooks
│   └── useDialogueGuide.ts   # 对话引导状态管理
├── services/          # API 服务
│   └── api.ts         # 统一 API 封装
├── lib/               # 工具库
│   └── api.ts         # axios 实例配置
└── index.css          # 全局样式
```

## 页面组件

### 1. NovelList（小说列表页）

**路径**: `frontend/src/pages/NovelList.tsx`

**功能**:
- 展示所有小说卡片网格
- 支持创建新小说按钮
- 支持编辑/删除操作
- 调用 `novelService.getNovelList()` 获取数据

### 2. CreateNovel（新建小说页）

**路径**: `frontend/src/pages/CreateNovel.tsx`

**功能**:
- 提供两种创建模式切换：FormMode 和 DialogueMode
- FormMode：传统表单填写
- DialogueMode：AI 引导式创建（10 步引导）
- 调用 `workflowService.createNovel()` 或 `dialogueGuideService.generate()`

### 3. NovelEditor（小说编辑器页）

**路径**: `frontend/src/pages/NovelEditor.tsx`

**功能**: 最复杂的页面组件（~1120 行），包含三个标签页：

| 标签页 | 功能 | 调用服务 |
|--------|------|----------|
| Write | 续写章节 | workflowService.continueChapter() |
| Fix | 卡文修复 | workflowService.fixWriterBlock() |
| Tools | 创作工具面板 | 多个工具 API |

**工具面板功能**:
- 字数统计（WordCount）
- 违禁词检测（BannedWord）
- 角色关系图（CharacterRelation）
- 伏笔管理（Foreshadow）
- 人设一致性校验（Consistency）
- 查重（Plagiarism）
- 思维导图导出（MindMap）

## 核心组件

### FormMode（表单模式）

**路径**: `frontend/src/components/create-novel/FormMode.tsx`

**功能**: 表单填写创建小说，包含：
- 标题输入
- 类型选择（7 种选项）
- 梗概输入
- 金手指输入
- 总卷数输入
- 自动生成主题开关

### DialogueMode（对话引导模式）

**路径**: `frontend/src/components/create-novel/DialogueMode.tsx`

**功能**: AI 引导式小说创建，特点：
- 10 步引导流程
- 自动生成建议
- 步骤导航
- 确认视图

### useDialogueGuide Hook

**路径**: `frontend/src/hooks/useDialogueGuide.ts`

**功能**: 管理对话引导状态机：
- 10 个步骤状态管理
- 步骤导航（前进/后退）
- 建议选择处理
- 表单数据提取

**步骤顺序**:
1. intent - 确认创作意图
2. genre - 选择类型
3. synopsis - 生成梗概
4. title - 生成标题
5. synopsis_v2 - 优化梗概
6. power_system - 生成力量体系
7. world_background - 生成世界观背景
8. golden_finger - 生成金手指
9. total_volumes - 建议卷数
10. confirm - 确认所有设定

## API 服务封装

**路径**: `frontend/src/services/api.ts`

封装四个服务对象：

| 服务 | 方法 | 对应 API |
|------|------|----------|
| `workflowService` | createNovel | POST /workflow/create-novel |
| | continueChapter | POST /workflow/continue-chapter |
| | fixWriterBlock | POST /workflow/fix-writer-block |
| `novelService` | getNovelList | GET /novels |
| | getNovelDetail | GET /novels/{id} |
| | createNovel | POST /novels |
| | updateNovel | PUT /novels/{id} |
| | deleteNovel | DELETE /novels/{id} |
| `agentService` | execute | POST /agent/execute |
| `dialogueGuideService` | generate | POST /dialogue-guide/generate |

## UI 组件（shadcn/ui）

| 组件 | 用途 |
|------|------|
| Button | 按钮 |
| Card | 卡片容器 |
| Tabs | 标签页切换 |
| Textarea | 多行输入框 |
| Badge | 徽章标签 |
| Skeleton | 加载骨架屏 |
| ScrollArea | 滚动区域 |
| Tooltip | 工具提示 |
| Dialog | 弹窗 |
| Sonner | Toast 通知 |

## 主题系统

- 使用 `ThemeProvider` 提供主题上下文
- 支持深色/浅色模式切换
- 使用 Tailwind CSS 的 dark mode 类

## 路由配置

```typescript
<Routes>
  <Route path="/" element={<NovelList />} />        {/* 首页 - 小说列表 */}
  <Route path="/create" element={<CreateNovel />} /> {/* 新建小说 */}
  <Route path="/novel/:novelId" element={<NovelEditor />} /> {/* 小说编辑器 */}
</Routes>
```

## 相关页面

- [[api-endpoints]] - API 接口文档（前端调用的后端接口）
- [[workflow-system]] - 工作流系统（前端触发的工作流）
- [[external-tool-plugins]] - 外部工具插件（工具面板使用的工具）
