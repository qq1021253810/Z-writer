# Z-writer CLI

网文小说创作 Agent 系统 - Rust CLI 版本

## 特性

- **极致性能**: 启动时间 ~16ms，内存占用 2MB，磁盘占用 2.18MB
- **零外部依赖**: 纯 Rust 实现，无需安装数据库/向量库
- **1+6 多智能体架构**: 总控 Agent + 6 个专业子 Agent
- **混合 RAG 存储**: L1 Markdown + L2 JSON + L3 向量库
- **智能上下文管理**: 分层压缩、角色追踪、滚动摘要
- **Git 版本管理**: 内置 Git，自动提交创作历史
- **云端 LLM 支持**: 百炼 DashScope API（OpenAI 兼容接口）

## 安装

### 从源码编译

```bash
cd rust-cli
cargo build --release
```

编译后的二进制文件位于 `target/release/zwriter-cli.exe`

### 性能指标

| 指标 | 目标 | 实际 |
|------|------|------|
| 启动时间 | <50ms | ~16ms |
| 内存占用 | <50MB | 2.06MB |
| 磁盘占用 | <15MB | 2.18MB |

## 配置

### 百炼 DashScope API

创建 `config.local.toml` 文件（不提交到 Git）：

```toml
[llm]
provider = "dashscope"
api_key = "sk-your-api-key"
model = "qwen-max"
```

### Ollama 本地模型（可选）

```toml
[llm]
provider = "ollama"
base_url = "http://localhost:11434"
chat_model = "qwen3:8b"
embed_model = "nomic-embed-text"
```

## 使用

### 基础命令

```bash
# 查看帮助
zwriter-cli --help

# 新建小说
/new

# 续写章节
/continue

# 卡文修复
/fix

# 列出所有小说
/list

# 切换小说
/use <小说名>

# 查看上下文
/context

# 压缩上下文
/compress

# Wiki 健康检查
/wiki-health
```

### 工作流示例

#### 1. 新建小说

```
> /new
> 赛道: 玄幻
> 书名: 仙途
> 金手指: 神秘系统
> 世界观: 修仙世界，境界分为炼气、筑基、金丹、元婴
> 主角: 林凡，天赋异禀
> 大纲: 第一章偶得系统，第二章开始修炼...
```

#### 2. 续写章节

```
> /use 仙途
> /continue
正在续写第 3 章...
[LLM 生成内容]
已保存到 chapters/chapter_003.md
```

#### 3. 卡文修复

```
> /fix
当前章节: 第 5 章
问题: 剧情卡壳
[PlotAgent 生成 3 套解决方案]
选择方案 2...
已改写段落
```

## 架构

### 多智能体系统

```
ControllerAgent (总控)
├─ WritingAgent (写作)
├─ WorldOutlineAgent (世界观&大纲)
├─ CharacterAgent (角色塑造)
├─ PlotAgent (剧情策划)
├─ ComplianceAgent (合规检测)
└─ PolishAgent (润色优化)
```

### 混合 RAG 存储

- **L1 Markdown**: 全局设定、角色卡、章节内容（人类可读）
- **L2 JSON**: 剧情记忆、伏笔追踪、时间线（结构化）
- **L3 向量库**: 素材检索、语义搜索（轻量级实现）

### 高级能力

- **分层上下文管理**: Tier 1（最近消息）/ Tier 2（压缩摘要）/ Tier 3（永久保留）
- **角色状态追踪**: 位置、情感、关系、物品、修为变化
- **滚动摘要**: 章节摘要 + 深度压缩 + 风格锚点
- **Token 优化**: 无损压缩 → 虚词过滤 → 抽取式压缩
- **LLM Wiki**: 自动页面生成、健康检查、智能加载
- **Git 集成**: 自动提交、版本历史、差异对比

## 测试

运行所有测试：

```bash
cargo test
```

当前测试统计：**94/94 测试全部通过**

测试覆盖：
- character_tracker_test: 7 个测试
- context_manager_test: 11 个测试
- dashscope_test: 3 个测试
- e2e_test: 1 个测试
- git_test: 5 个测试
- integration_test: 9 个测试
- l2_memory_tree_test: 7 个测试
- l3_material_store_test: 9 个测试
- llm_test: 1 个测试
- rolling_summary_test: 10 个测试
- token_optimizer_test: 13 个测试
- wiki_enhanced_test: 14 个测试
- wiki_test: 4 个测试

## 项目结构

```
rust-cli/
├─ src/
│  ├─ agents/          # 7 个 Agent 实现
│  ├─ cli/             # CLI 交互层
│  ├─ context/         # 上下文管理系统
│  ├─ llm/             # LLM 集成（Ollama + DashScope）
│  ├─ rag/             # RAG 存储层（L2 + L3）
│  ├─ storage/         # L1 文件存储
│  ├─ wiki/            # Wiki 知识库
│  └─ workflows/       # 工作流（新建/续写/卡文修复）
├─ tests/              # 12 个测试文件，94 个测试用例
├─ wiki/               # Wiki 知识库文件（genres/, rules/, templates/）
├─ Cargo.toml          # 依赖管理 + Release 优化
└─ AGENT_EVOLVE_LOG.md # 项目演进日志
```

## 技术栈

- **Rust 1.96.0** (GNU toolchain)
- **clap**: 命令行参数解析
- **tokio**: 异步运行时
- **reqwest**: HTTP 客户端
- **serde/serde_json**: 序列化
- **git2**: Git 版本管理（纯 Rust 实现）
- **pulldown-cmark**: Markdown 解析

## 开发日志

详见 [AGENT_EVOLVE_LOG.md](./AGENT_EVOLVE_LOG.md)

## 许可证

MIT
