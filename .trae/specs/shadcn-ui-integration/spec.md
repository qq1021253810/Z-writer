# shadcn/ui 接入方案 Spec

## Why
当前前端项目所有 UI 组件均为手写 Tailwind 类名，没有组件抽象（无 `components/` 目录），核心页面 `NovelEditor.tsx` 已膨胀至约 800 行。接入 shadcn/ui 可以提供高质量的标准化组件、暗色模式支持、可访问性保障，同时与现有 Tailwind + Vite + React 18 + TypeScript 技术栈完全兼容。

## What Changes
- 初始化 shadcn/ui CLI 配置（`components.json`、`lib/utils.ts`）
- 修改 `tailwind.config.js`，添加 `tailwindcss-animate` 插件和 shadcn/ui 主题扩展
- 修改 `index.css`，引入 shadcn/ui CSS 变量主题系统，整合现有 `primary` 色板
- 安装必要依赖：`tailwindcss-animate`、`class-variance-authority`、`clsx`、`tailwind-merge`、`lucide-react`
- 按需添加 shadcn/ui 组件到 `src/components/ui/` 目录
- 逐步将现有页面中的手写 UI 替换为 shadcn/ui 组件

## Impact
- Affected code: `tailwind.config.js`、`index.css`、`vite.config.ts`（无需改动，已兼容）、所有页面组件
- 新增文件：`components.json`、`src/lib/utils.ts`、`src/components/ui/` 目录下的组件文件
- **BREAKING**: `index.css` 中的全局样式和 CSS 变量将被替换为 shadcn/ui 主题系统，现有硬编码颜色类名需逐步迁移

## 兼容性分析

| 技术 | 当前版本 | shadcn/ui 要求 | 兼容性 |
|------|---------|---------------|--------|
| React | ^18.3.1 | 18+ | 完全兼容 |
| Vite | ^5.3.4 | 5+ | 完全兼容 |
| TypeScript | ^5.5.3 | 5+ | 完全兼容 |
| Tailwind CSS | ^3.4.7 | 3.0+ | 完全兼容 |
| PostCSS | ^8.4.40 | 需要 | 已配置 |
| 路径别名 @ | 已配置 | 需要 | 已配置 |

**结论：完全兼容，无需任何版本调整。**

## 优缺点评估

### 优点
1. **完全控制**：组件源码在项目中，可随意修改
2. **零运行时开销**：无额外 JS 运行时，包体积极小
3. **Tailwind 原生**：与现有 Tailwind 技术栈无缝集成
4. **可访问性**：基于 Radix UI，WCAG 合规
5. **暗色模式**：CSS 变量方案天然支持，对写作工具很重要
6. **TypeScript 优先**：完整类型定义

### 缺点
1. **升级需手动**：组件代码在项目中，shadcn/ui 更新后需手动合并
2. **无内置高级组件**：Data Table、Form 等需额外配置第三方库
3. **中文文档缺失**：官方文档只有英文

### 与替代方案对比

| 维度 | shadcn/ui | Ant Design | MUI |
|------|-----------|------------|-----|
| 安装方式 | 源码复制 | npm 依赖 | npm 依赖 |
| 可定制性 | 极高 | 中等 | 中等 |
| 包体积 | 极小 | 较大 | 较大 |
| Tailwind 集成 | 原生 | 不支持 | 不支持 |
| 暗色模式 | CSS 变量，极简 | 需配置 | 需配置 |
| 学习曲线 | 低 | 中 | 中 |

**选择 shadcn/ui 的核心理由**：项目已深度使用 Tailwind CSS，shadcn/ui 是唯一原生支持 Tailwind 的组件库，接入成本最低。

## ADDED Requirements

### Requirement: shadcn/ui 初始化配置
系统 SHALL 通过 `npx shadcn@latest init` 完成初始化，生成 `components.json`、`src/lib/utils.ts`，并修改 `tailwind.config.js` 和 `index.css`。

#### Scenario: 初始化成功
- **WHEN** 运行 `npx shadcn@latest init` 并选择 Default 风格 + Slate 基色 + CSS 变量
- **THEN** 生成 `components.json` 配置文件、`src/lib/utils.ts` 工具函数，`tailwind.config.js` 包含 animate 插件和主题扩展，`index.css` 包含完整的 CSS 变量主题系统

### Requirement: 现有 primary 色板迁移
系统 SHALL 将现有 `tailwind.config.js` 中的自定义 `primary` 色板迁移到 shadcn/ui 的 CSS 变量主题系统中，保持视觉一致性。

#### Scenario: 色板迁移
- **WHEN** 初始化 shadcn/ui 后
- **THEN** 现有 `bg-primary-600` 等类名仍然生效，颜色值与迁移前一致

### Requirement: 按需添加组件
系统 SHALL 支持通过 `npx shadcn@latest add <component>` 按需添加组件到 `src/components/ui/` 目录。

#### Scenario: 添加 Button 组件
- **WHEN** 运行 `npx shadcn@latest add button`
- **THEN** 在 `src/components/ui/button.tsx` 生成 Button 组件源码，可在页面中通过 `import { Button } from "@/components/ui/button"` 使用

### Requirement: 页面组件逐步迁移
系统 SHALL 逐步将现有页面中的手写 UI 元素替换为 shadcn/ui 组件，优先迁移以下组件：
- Button（按钮）
- Card（卡片容器）
- Tabs（标签页切换）
- Dialog（弹窗/模态框）
- Textarea（文本输入区）
- Badge（标签/徽章）
- Tooltip（提示信息）
- ScrollArea（滚动区域）
- Skeleton（加载骨架屏）
- Toast/Sonner（消息通知）

#### Scenario: NovelEditor 页面迁移
- **WHEN** 将 NovelEditor 中的手写 Tab 切换替换为 shadcn/ui Tabs 组件
- **THEN** Tab 切换功能不变，样式更统一，代码更简洁

## MODIFIED Requirements

### Requirement: 全局样式管理
原有 `index.css` 中的硬编码 CSS 变量和全局样式 SHALL 替换为 shadcn/ui 的 CSS 变量主题系统，统一管理亮色/暗色模式下的颜色、圆角、间距等设计 token。

## REMOVED Requirements

无移除需求。现有功能全部保留，仅替换底层实现方式。
