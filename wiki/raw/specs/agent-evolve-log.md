## 复盘记录：2026-06-18 20:30
- 本次任务：推进设计文档中尚未实现的核心功能
- 核心问题：
  1. 设计文档要求的功能与实际实现存在差距
  2. 缺少伏笔管理、人设一致性校验、查重工具、设定修改同步、思维导图导出等关键功能
- 执行短板：
  1. 前期开发过于聚焦核心架构，忽视了设计文档中的工具插件层和持久存储层的完整性
  2. 伏笔管理虽有数据库表但缺少服务层和 API
- 优化方向：
  1. 已完成伏笔管理模块（ForeshadowService + ForeshadowController，7 个 API）
  2. 已完成人设一致性校验器（CharacterConsistencyChecker + ConsistencyController）
  3. 已完成查重工具插件（PlagiarismDetector + PlagiarismController）
  4. 已完成设定修改同步机制（SettingSyncService + SettingSyncController）
  5. 已完成思维导图导出插件（MindMapExporter + MindMapController）
  6. 已完成前端集成（伏笔管理、人设校验、查重、思维导图面板）
  7. 前后端编译通过，API 联调测试通过
  8. 待推进：网文榜单爬虫插件、翻译工具

## 复盘记录：2026-06-18 23:00
- 本次任务：创建设定修改同步机制（SettingSyncService + SettingSyncController）
- 核心问题：无重大问题，编译一次通过
- 执行短板：向量库更新需先删除旧文档再重新添加，Spring AI VectorStore 无直接 update 方法
- 优化方向：
  1. 已完成 SettingSyncService（角色名/世界观/战力等级同步 + 受影响章节查询）
  2. 已完成 SettingSyncController（4 个 REST API 端点）
  3. 后续可考虑：批量同步时增加异步处理、同步前预览变更（dry-run 模式）

## 复盘记录：2026-06-18 22:30
- 本次任务：创建思维导图导出插件（MindMapExporter + MindMapController）
- 核心问题：无重大问题，编译一次通过
- 执行短板：PowerShell 中 `&&` 不是有效语句分隔符，需使用 `;` 代替
- 优化方向：记住 Windows PowerShell 环境下命令链接用分号而非双与号

## 复盘记录：2026-06-18 23:30
- 本次任务：将 NovelList 页面迁移至 shadcn/ui 组件
- 核心问题：无重大问题，编译一次通过
- 执行短板：无
- 优化方向：
  1. 已完成 Button、Card、Badge、Skeleton 组件替换
  2. 硬编码颜色（bg-primary-600、text-gray-900 等）已替换为 CSS 变量（bg-background、text-foreground 等）
  3. 加载状态从简单 spinner 升级为 Skeleton 骨架屏，体验更佳
  4. 后续可继续迁移其他页面（NovelEditor 等）至 shadcn/ui

## 复盘记录：2026-06-18 23:50
- 本次任务：将 CreateNovel 页面迁移至 shadcn/ui 组件
- 核心问题：无重大问题，TypeScript 编译一次通过
- 执行短板：无
- 优化方向：
  1. 已完成 Card/CardContent/CardHeader/CardTitle、Button、Textarea 组件替换
  2. 原生 input/select 的 className 已统一替换为 shadcn/ui Input 组件的样式模式
  3. 硬编码颜色（bg-gradient-to-br、bg-green-50、bg-red-50、text-gray-700 等）已替换为 CSS 变量（bg-background、text-foreground、text-muted-foreground、text-destructive 等）
  4. 结果展示区域从 div+硬编码背景色迁移为 Card+CardHeader+CardContent 结构
  5. 后续可继续迁移 NovelEditor 等页面至 shadcn/ui

## 复盘记录：2026-06-19 00:20
- 本次任务：将 NovelEditor 页面迁移至 shadcn/ui 组件（~1120行）
- 核心问题：无重大问题，TypeScript 编译一次通过
- 执行短板：无
- 优化方向：
  1. 已完成 Tabs/TabsList/TabsTrigger/TabsContent 替换自定义 tab 导航，activeTab 类型从联合类型改为 string
  2. 所有 button 替换为 Button 组件（default/outline/destructive/secondary/ghost 变体）
  3. 所有 textarea 替换为 Textarea 组件
  4. 卡片式 div 替换为 Card/CardHeader/CardTitle/CardDescription/CardContent
  5. 状态标签和违禁词/敏感词标签替换为 Badge 组件（destructive/secondary/default 变体）
  6. 加载 spinner 替换为 Skeleton 组件
  7. 硬编码颜色全部替换为 CSS 变量（bg-background、text-foreground、text-muted-foreground、text-primary 等）
  8. 背景渐变替换为 bg-background
  9. 原生 input/select 保留但 className 统一为 shadcn/ui Input 组件样式模式
  10. 所有功能（状态、事件处理、API 调用、mermaid 渲染、7个工具面板）完整保留
