# Z-writer 项目记忆

## 项目概述
网文小说创作 Agent 智能体系统，采用 1 总控 + 6 子 Agent 多智能体协作架构。

## 技术栈
- 后端：Java 21 + Spring Boot 3.4.1 + Spring AI 1.1.7
- 前端：React 18 + TypeScript + Tailwind CSS + Vite
- 数据库：PostgreSQL 16 + Redis 7
- 向量库：Chroma（通过 Spring AI VectorStore 集成）
- LLM：Ollama 本地部署（qwen3:1.7b 聊天 + nomic-embed-text 嵌入）
- 容器：Docker Compose 管理依赖服务

## 项目结构
- 后端：Spring Boot + JPA + Lombok，位于 `backend/` 目录
- 实体层：`com.zwriter.entity` - JPA 实体类
- 仓库层：`com.zwriter.repository` - JpaRepository 接口
- 工具层：`com.zwriter.tool` - 业务工具类（@Component + @RequiredArgsConstructor）
- 控制器层：`com.zwriter.controller` - REST API（@RestController）
- 工作流层：`com.zwriter.workflow` - 复杂业务流程
- 服务层：`com.zwriter.service` - 业务服务

## 编码规范
- Tool 类使用 @Slf4j + @Component + @RequiredArgsConstructor
- Controller 使用 @Slf4j + @RestController + @RequiredArgsConstructor
- API 返回格式：`{"success": true/false, "data": ..., "message": ...}`
- 中文注释和日志
- 编译命令：`cd backend; mvn clean compile -q`（PowerShell 用分号）

## 已完成功能
1. **多智能体架构**：ControllerAgent + 6 个子 Agent（WorldOutline、Character、Plot、Writing、Polish、Compliance）
2. **三种核心工作流**：CreateNovelWorkflow、ContinueChapterWorkflow、FixWriterBlockWorkflow
3. **前端界面**：小说列表、新建小说、编辑器（续写/卡文修复/创作工具）
4. **向量数据库集成**：VectorKnowledgeService 实现章节段落存储与检索（带缓存机制）
5. **上下文压缩机制**：ContextCompressionService 实现前情提要、角色状态、时间线、伏笔追踪
6. **Spring AI 升级**：从 1.0.0-M5 升级至 1.1.7，适配 Chroma v2 API
7. **外部工具插件**：
   - WordCountTool：字数统计（总字数、章节字数、日均字数、字数规划）
   - BannedWordTool：违禁词检测（100+违禁词、40+敏感词、自定义词库、三级敏感度）
   - CharacterRelationTool：角色关系图生成（支持 Mermaid 格式输出）
   - PlagiarismDetector：查重检测（N-gram + Jaccard 相似度）
   - CharacterConsistencyChecker：人设一致性校验（对话/行为/战力三维度）
   - MindMapExporter：思维导图导出（Mermaid/Markdown/JSON）
8. **前端工具集成**：
   - 编辑器新增"创作工具"标签页
   - 字数统计面板（实时统计中文字数、英文字母、数字等，500ms 防抖）
   - 违禁词检测面板（风险等级评估、违禁词/敏感词高亮显示、一键替换、高级选项）
   - 角色关系图面板（Mermaid 渲染）
   - 伏笔管理面板（添加/列表/标记回收）
   - 人设一致性校验面板
   - 查重检测面板（相似度/风险等级/相似片段）
   - 大纲思维导图面板
9. **API 接口**：
   - POST /api/word-count/text：文本字数统计
   - GET /api/word-count/novel/{novelId}：小说总字数统计
   - GET /api/word-count/chapter/{chapterId}：章节字数统计
   - POST /api/banned-word/detect：违禁词检测（支持自定义选项）
   - POST /api/banned-word/replace：违禁词替换
   - GET /api/character-relation/graph/{novelId}：角色关系图数据
   - GET /api/character-relation/mermaid/{novelId}：Mermaid 格式关系图
   - POST /api/foreshadow：添加伏笔
   - GET /api/foreshadow/novel/{novelId}：获取小说所有伏笔
   - GET /api/foreshadow/novel/{novelId}/planted：获取未回收伏笔
   - GET /api/foreshadow/novel/{novelId}/resolved：获取已回收伏笔
   - PUT /api/foreshadow/{id}/resolve：回收伏笔
   - GET /api/foreshadow/novel/{novelId}/conflicts：检测伏笔冲突
   - GET /api/foreshadow/novel/{novelId}/overdue：获取超期伏笔
   - POST /api/consistency/check：人设一致性校验
   - GET /api/consistency/character/{characterId}/profile：角色人设档案
   - POST /api/plagiarism/detect：查重检测
   - POST /api/plagiarism/similarity：计算文本相似度
   - POST /api/setting-sync/character：同步角色修改
   - POST /api/setting-sync/worldview：同步世界观修改
   - POST /api/setting-sync/power-level：同步战力等级修改
   - GET /api/setting-sync/affected-chapters：获取受影响章节
   - GET /api/mindmap/mermaid/{novelId}：导出 Mermaid 格式思维导图
   - GET /api/mindmap/markdown/{novelId}：导出 Markdown 格式
   - GET /api/mindmap/json/{novelId}：导出 JSON 格式
10. **工作流工具集成**：
    - 续写章节工作流自动调用字数统计和违禁词检测
    - 新建小说工作流集成字数统计
    - 卡文修复工作流集成违禁词检测
11. **伏笔管理模块**：ForeshadowService + ForeshadowController
12. **人设一致性校验器**：CharacterConsistencyChecker + ConsistencyController
13. **查重工具插件**：PlagiarismDetector + PlagiarismController
14. **设定修改同步机制**：SettingSyncService + SettingSyncController
15. **思维导图导出插件**：MindMapExporter + MindMapController
16. **向量检索缓存机制**：ConcurrentHashMap + TTL 5分钟 + 自动清理
17. **对话引导生成 API**：DialogueGuideService + DialogueGuideController（POST /api/dialogue-guide/generate）
18. **前端组件库**：shadcn/ui（已完成 NovelList、CreateNovel、NovelEditor 页面迁移）

## 待优化功能
1. **网文榜单爬虫插件**：抓取起点/番茄/七猫爆款标签、热门题材
2. **翻译工具**：古言白话文互转、古风专有名词释义
3. **性能优化**：
   - 向量检索缓存可升级为 Caffeine/Redis
   - 前端代码分割优化（当前 chunk 超过 500KB）
4. **用户体验优化**：
   - 前端可增加更多实时反馈
   - 工作流进度可视化

## 项目约定
- 使用 Lombok 简化代码
- Agent 统一继承 BaseAgent
- 工作流返回 WorkflowResult 封装结果
- 向量存储使用 metadata 中的 collection 字段区分不同类型
- 配置文件：application.yml 统一管理

## 常见问题与解决方案
- Chroma v1 API deprecated → 升级 Spring AI 至 1.1.7 使用 v2 API
- Spring AI starter 名称变更 → 使用 spring-ai-starter-model-ollama 和 spring-ai-starter-vector-store-chroma
- Chroma 配置路径错误 → 使用 spring.ai.vectorstore.chroma 而非 spring.ai.chroma
- PowerShell 中文乱码 → 实际数据正确存储，仅终端显示问题
- Docker Desktop 未启动 → 后端无法连接 PostgreSQL，需先启动 Docker Desktop
