# Tasks

- [x] Task 1: 安装 shadcn/ui 必要依赖
  - [x] SubTask 1.1: 安装运行时依赖：`tailwindcss-animate`、`class-variance-authority`、`clsx`、`tailwind-merge`、`lucide-react`
  - [x] SubTask 1.2: 验证依赖安装成功，项目可正常编译

- [x] Task 2: 配置 shadcn/ui 初始化
  - [x] SubTask 2.1: 创建 `components.json` 配置文件（style: default, baseColor: slate, cssVariables: true）
  - [x] SubTask 2.2: 创建 `src/lib/utils.ts`，实现 `cn()` 工具函数
  - [x] SubTask 2.3: 修改 `tailwind.config.js`，添加 `tailwindcss-animate` 插件和 shadcn/ui 主题扩展
  - [x] SubTask 2.4: 修改 `index.css`，替换为 shadcn/ui CSS 变量主题系统，整合现有 primary 色板
  - [x] SubTask 2.5: 验证项目编译通过，现有页面样式无异常

- [x] Task 3: 添加核心 shadcn/ui 组件
  - [x] SubTask 3.1: 添加 Button 组件（`npx shadcn@latest add button`）
  - [x] SubTask 3.2: 添加 Card 组件
  - [x] SubTask 3.3: 添加 Tabs 组件
  - [x] SubTask 3.4: 添加 Dialog 组件
  - [x] SubTask 3.5: 添加 Textarea 组件
  - [x] SubTask 3.6: 添加 Badge 组件
  - [x] SubTask 3.7: 添加 Tooltip 组件
  - [x] SubTask 3.8: 添加 ScrollArea 组件
  - [x] SubTask 3.9: 添加 Skeleton 组件
  - [x] SubTask 3.10: 添加 Toast/Sonner 组件
  - [x] SubTask 3.11: 验证所有组件可正常导入和使用

- [x] Task 4: 迁移 NovelList 页面
  - [x] SubTask 4.1: 将小说列表卡片替换为 shadcn/ui Card 组件
  - [x] SubTask 4.2: 将按钮替换为 shadcn/ui Button 组件
  - [x] SubTask 4.3: 将新建小说链接替换为 shadcn/ui Button（asLink variant）
  - [x] SubTask 4.4: 验证页面功能和样式正常

- [x] Task 5: 迁移 CreateNovel 页面
  - [x] SubTask 5.1: 将表单输入替换为 shadcn/ui Input/Textarea 组件
  - [x] SubTask 5.2: 将按钮替换为 shadcn/ui Button 组件
  - [x] SubTask 5.3: 将卡片容器替换为 shadcn/ui Card 组件
  - [x] SubTask 5.4: 验证页面功能和样式正常

- [x] Task 6: 迁移 NovelEditor 页面
  - [x] SubTask 6.1: 将 Tab 切换替换为 shadcn/ui Tabs 组件
  - [x] SubTask 6.2: 将按钮替换为 shadcn/ui Button 组件
  - [x] SubTask 6.3: 将卡片容器替换为 shadcn/ui Card 组件
  - [x] SubTask 6.4: 将 Badge/标签替换为 shadcn/ui Badge 组件
  - [x] SubTask 6.5: 将文本区域替换为 shadcn/ui Textarea 组件
  - [x] SubTask 6.6: 将 Tooltip 提示替换为 shadcn/ui Tooltip 组件
  - [x] SubTask 6.7: 将滚动区域替换为 shadcn/ui ScrollArea 组件
  - [x] SubTask 6.8: 验证页面功能和样式正常

- [x] Task 7: 端到端验证
  - [x] SubTask 7.1: 前端编译通过，无 TypeScript 错误
  - [x] SubTask 7.2: 所有页面可正常访问和交互
  - [x] SubTask 7.3: 暗色模式切换正常（如已实现）
  - [x] SubTask 7.4: 现有功能（续写、卡文修复、创作工具）全部正常

# Task Dependencies
- [Task 2] depends on [Task 1]
- [Task 3] depends on [Task 2]
- [Task 4] depends on [Task 3]
- [Task 5] depends on [Task 3]
- [Task 6] depends on [Task 3]
- [Task 7] depends on [Task 4, Task 5, Task 6]
- [Task 4, Task 5, Task 6] 可并行执行
