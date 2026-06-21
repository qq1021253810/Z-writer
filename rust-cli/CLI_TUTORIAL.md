# Z-Writer CLI 使用教程

## 快速开始

### 1. 安装

```bash
# 进入项目目录
cd rust-cli

# 编译（Release 模式）
cargo build --release

# 编译后的二进制文件
# Windows: target/release/zwriter-cli.exe
# Linux/Mac: target/release/zwriter-cli
```

**性能指标**：
- 启动时间：~16ms
- 内存占用：~2MB
- 磁盘占用：~2.18MB

### 2. 配置 LLM

Z-Writer CLI 支持两种 LLM 提供商：

#### 方案 A：百炼 DashScope（推荐）

创建 `config.local.toml` 文件（不提交到 Git）：

```toml
provider = "dashscope"

[dashscope]
api_key = "sk-your-api-key-here"
model = "qwen-plus"
```

**获取 API Key**：
1. 访问 [阿里云百炼](https://bailian.console.aliyun.com/)
2. 开通服务并创建 API Key
3. 将 API Key 填入 `config.local.toml`

**推荐模型**：
- `qwen-plus`：性价比高，适合日常创作
- `qwen-max`：质量最好，适合重要章节
- `qwen-turbo`：速度最快，适合快速迭代

#### 方案 B：Ollama 本地部署

```toml
provider = "ollama"
ollama_url = "http://localhost:11434"
chat_model = "qwen3:14b"
embed_model = "nomic-embed-text"
```

**安装 Ollama**：
```bash
# Windows
# 下载并安装：https://ollama.com/download

# 拉取模型
ollama pull qwen3:14b
ollama pull nomic-embed-text
```

### 3. 启动 CLI

```bash
# 直接运行
cargo run --release

# 或者运行编译好的二进制
./target/release/zwriter-cli
```

启动后会看到：

```
╔════════════════════════════════════════════════════════╗
║   Z-Writer CLI - 高智商网文小说创作助手              ║
║   LLM: 百炼 (qwen-plus)                              ║
║   输入 /help 查看可用命令                              ║
╚════════════════════════════════════════════════════════╝

>
```

---

## 核心命令

### 基础命令

| 命令 | 说明 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/list` | 列出所有小说 | `/list` |
| `/new <名称>` | 创建新小说 | `/new 商业帝国` |
| `/use <名称>` | 切换到指定小说 | `/use 商业帝国` |
| `/continue` | 续写下一章 | `/continue` |
| `/fix` | 卡文修复（开发中） | `/fix` |
| `/context` | 显示上下文统计 | `/context` |
| `/compress` | 手动压缩上下文 | `/compress` |
| `/wiki-health` | Wiki 健康检查 | `/wiki-health` |
| `/quit` | 退出程序 | `/quit` |

---

## 完整工作流程

### 场景 1：从零开始创作新小说

```bash
# 1. 创建新小说
> /new 商业帝国

✓ 已创建小说: 商业帝国

# 2. 编辑基础设定（用你喜欢的编辑器）
# 打开 workspaces/商业帝国/ 目录，编辑以下文件：

# novel.md - 小说基础信息
# 商业帝国

- 类型: 都市商战
- 状态: 创作中
- 简介: 陆远凭借超前技术眼光，从零开始构建科技商业帝国

# worldview.md - 世界观设定
# 世界观设定

## 规则体系
- 商业规则：公司法、证券法、知识产权法、反垄断法
- 科技规则：半导体产业链、芯片设计制造流程、技术专利布局
- 行业规则：供应链整合、资本运作、市场竞争

## 势力博弈网络
- 星辰科技：主角创立的初创公司，掌握第三代半导体核心技术
- 天虎集团：行业巨头，试图通过专利垄断封锁市场
- 国家基金：战略投资机构，扶持核心材料国产替代

## 科技树/产业链
- 上游：第三代半导体材料（碳化硅、氮化镓）
- 中游：芯片设计、晶圆制造、封装测试
- 下游：新能源汽车、5G 基站、消费电子

# outline.md - 大纲
# 大纲

## 第一卷：创业起步
- 第1章：陆远发现行业痛点，决定创业
- 第2章：攻克核心技术难题，申请专利
- 第3章：寻找天使投资，遭遇天虎集团打压
- 第4章：利用反垄断法规则，反手起诉天虎集团
- 第5章：获得天使投资，完成商业闭环

# 3. 创建角色卡
# 在 workspaces/商业帝国/characters/ 目录创建角色文件

# 陆远.md
# 陆远

- 身份: 星辰科技 CEO，麻省理工半导体博士
- 年龄: 32
- 决策风格: 谋略型，善于布局收网
- 核心优势: 超前技术眼光、全产业链思维、规则利用能力
- 目标: 实现第三代半导体材料国产替代，打破国外垄断
- 人脉关系: 陈博士（技术合伙人）、王建国（国家基金投资总监）

# 4. 切换小说并开始续写
> /use 商业帝国

✓ 已切换到小说: 商业帝国

> /continue

正在准备续写...
准备续写第 1 章
正在调用 LLM 生成内容...
第 1 章已保存
────────────────────────────────────────
（显示生成的内容预览）
────────────────────────────────────────
```

### 场景 2：续写已有小说

```bash
# 1. 查看所有小说
> /list

已有小说:
  - 商业帝国
  - 权力巅峰

# 2. 切换到目标小说
> /use 商业帝国

✓ 已切换到小说: 商业帝国

# 3. 查看当前状态
> /context

上下文统计:
  小说信息: 156 字
  世界观: 423 字
  大纲: 289 字
  角色数: 3
  当前章节: 第 5 章
  下一章: 第 6 章

# 4. 续写下一章
> /continue

正在准备续写...
准备续写第 6 章
正在调用 LLM 生成内容...
第 6 章已保存
```

### 场景 3：多小说管理

```bash
# 创建多部小说
> /new 商业帝国
> /new 权力巅峰
> /new 科技革命

# 查看所有小说
> /list

已有小说:
  - 商业帝国
  - 权力巅峰
  - 科技革命

# 在不同小说间切换
> /use 权力巅峰
✓ 已切换到小说: 权力巅峰

> /continue
# 续写《权力巅峰》的下一章

> /use 商业帝国
✓ 已切换到小说: 商业帝国

> /continue
# 续写《商业帝国》的下一章
```

---

## 工作区结构

每部小说的工作区结构如下：

```
workspaces/商业帝国/
├── novel.md              # 小说基础信息
├── worldview.md          # 世界观设定
├── outline.md            # 大纲
├── memory_tree.json      # L2 剧情记忆（自动生成）
├── characters/           # 角色卡目录
│   ├── 陆远.md
│   └── 赵天虎.md
├── chapters/             # 章节内容
│   ├── ch001.md
│   ├── ch002.md
│   └── ch003.md
├── vector_store/         # L3 向量存储
│   └── materials.json
└── wiki/                 # Wiki 知识库
    └── entities/         # 实体页面
```

### 文件说明

#### novel.md
```markdown
# 商业帝国

- 类型: 都市商战
- 状态: 创作中
- 简介: 陆远凭借超前技术眼光，从零开始构建科技商业帝国
- 目标字数: 100万
- 更新频率: 日更3000字
```

#### worldview.md
```markdown
# 世界观设定

## 规则体系
- 商业规则：公司法、证券法、知识产权法、反垄断法
- 科技规则：半导体产业链、芯片设计制造流程、技术专利布局
- 行业规则：供应链整合、资本运作、市场竞争

## 势力博弈网络
- 星辰科技：主角创立的初创公司
- 天虎集团：行业巨头，试图垄断市场
- 国家基金：战略投资机构

## 科技树/产业链
- 上游：第三代半导体材料
- 中游：芯片设计、晶圆制造
- 下游：新能源汽车、5G 基站
```

#### characters/角色名.md
```markdown
# 陆远

## 基础信息
- 身份: 星辰科技 CEO
- 年龄: 32
- 背景: 麻省理工半导体博士

## 性格特征
- 决策风格: 谋略型
- 情商水平: 高
- 价值观: 技术报国，产业自主

## 核心优势
- 决策能力: 超前技术眼光，全产业链思维
- 资源网络: 高校科研资源、产业链上下游人脉
- 专业技能: 半导体材料、芯片设计
- 人脉关系: 陈博士（技术合伙人）、王建国（投资人）

## 成长弧线
- 第1章: 发现行业痛点，决定创业
- 第2章: 攻克核心技术，申请专利
- 第3章: 获得天使投资，完成商业闭环
```

#### chapters/chXXX.md
```markdown
# 第 1 章：抉择

星辰科技，会议室。

空气凝重得能挤出水来。

"所以，我们只剩下三个月的现金流？"陆远看着财务总监递上来的报表，语气平静得可怕。

（章节内容）
```

---

## 高级功能

### 1. Git 版本管理

Z-Writer CLI 自动为每个小说工作区启用 Git 版本管理：

```bash
# 进入小说工作区
cd workspaces/商业帝国

# 查看提交历史
git log --oneline

# 查看某次修改的差异
git diff <commit-hash>

# 回滚到某个版本
git checkout <commit-hash> -- .
```

**自动提交时机**：
- 每次续写章节后
- 每次修改设定后

### 2. Wiki 知识库

Wiki 知识库位于 `wiki/` 目录，包含：

```
wiki/
├── genres/           # 类型知识
│   ├── shangzhan.md  # 都市商战
│   ├── quanmou.md    # 政治权谋
│   ├── keji.md       # 科技革命
│   └── kehuan.md     # 科幻
├── rules/            # 创作规则
│   ├── golden-three-chapters.md  # 黄金三章
│   ├── cool-points.md            # 爽点设计
│   └── poison-points.md          # 毒点规避
├── templates/        # 模板
│   ├── character.md  # 角色模板
│   └── world_outline.md  # 世界观模板
└── entities/         # 实体页面（自动生成）
```

**Wiki 健康检查**：
```bash
> /wiki-health

Wiki 健康报告:
- 总页面数: 12
- 断链数: 0
- 孤立页面: 2
- 健康度: 95%
```

### 3. 上下文管理

Z-Writer CLI 采用三层上下文管理：

- **Tier 1**：最近 3 章完整内容
- **Tier 2**：较早章节的摘要
- **Tier 3**：关键设定（永久保留）

**查看上下文统计**：
```bash
> /context

上下文统计:
  小说信息: 156 字
  世界观: 423 字
  大纲: 289 字
  角色数: 3
  当前章节: 第 5 章
  下一章: 第 6 章
```

**手动压缩上下文**：
```bash
> /compress

正在压缩上下文...
✓ 压缩完成，节省 40% Token
```

### 4. 混合 RAG 存储

Z-Writer CLI 采用三层 RAG 架构：

- **L1 Markdown**：小说设定、角色卡、章节内容（人类可读）
- **L2 JSON**：剧情记忆、伏笔追踪、时间线（结构化）
- **L3 向量库**：素材检索、语义搜索（轻量级）

**memory_tree.json 示例**：
```json
{
  "novel_id": "商业帝国",
  "volumes": [
    {
      "volume_num": 1,
      "title": "创业起步",
      "summary": "陆远发现行业痛点，决定创业，攻克核心技术...",
      "chapters": [
        {
          "chapter_num": 1,
          "title": "抉择",
          "summary": "陆远面临现金流危机，决定转型做全产业链...",
          "key_events": ["发现行业痛点", "决定创业", "制定全产业链战略"],
          "character_changes": ["陆远: 从技术专家→创业者"]
        }
      ]
    }
  ],
  "foreshadows": [
    {
      "id": "fs_001",
      "description": "天虎集团的专利垄断",
      "planted_chapter": 1,
      "expected_resolve": 50,
      "status": "active"
    }
  ]
}
```

---

## 最佳实践

### 1. 设定先行

在开始续写前，先完善以下设定：

- ✅ **novel.md**：小说类型、简介、目标
- ✅ **worldview.md**：规则体系、势力博弈网络、科技树/产业链
- ✅ **outline.md**：至少规划前 10 章大纲
- ✅ **characters/**：主要角色卡（至少 2-3 个）

### 2. 角色卡编写技巧

```markdown
# 角色名

## 基础信息
- 身份: 
- 年龄: 
- 背景: 

## 性格特征
- 决策风格（激进型、稳健型、谋略型）: 
- 情商水平: 
- 价值观: 

## 核心优势
- 决策能力: 
- 资源网络: 
- 专业技能: 
- 人脉关系: 

## 人物关系
- 合作伙伴: 
- 竞争对手: 
- 导师: 
- 下属: 

## 成长弧线
- 初始状态: 
- 关键转折: 
- 最终状态: 

## 对话风格（示例）
- "商场如战场，但战场的规则，写得很清楚。"
- "这不是威胁，这是规则。"
```

### 3. 大纲规划建议

```markdown
# 大纲

## 第一卷：卷名
- 第1章：开篇（展示主角核心优势、决策风格）
- 第2章：第一个危机（利用规则和资源解决）
- 第3章：第一个布局收网（展现高智商博弈）
- 第4章：新的挑战（更大的博弈出现）
- 第5章：格局升级（从局部博弈→全局博弈）

## 第二卷：卷名
- 第6章：...
```

**黄金三章原则**：
- 第 1 章：展示主角核心优势和决策风格
- 第 2 章：利用规则和资源解决第一个危机
- 第 3 章：第一个完整的布局收网

### 4. 续写技巧

**续写前**：
1. 检查设定是否完整
2. 确认大纲方向
3. 查看前 3 章内容

**续写后**：
1. 阅读生成的内容
2. 必要时手动修改
3. 更新角色卡（如有新角色/关系变化）
4. 更新大纲（如有偏离）

### 5. 多小说管理

如果你同时创作多部小说：

```bash
# 1. 创建多部小说
> /new 小说A
> /new 小说B

# 2. 轮流续写
> /use 小说A
> /continue

> /use 小说B
> /continue

# 3. 定期查看进度
> /list
```

---

## 常见问题

### Q1: 启动时提示 "config.local.toml not found"

**解决方案**：
```bash
# 创建配置文件
cd rust-cli
echo 'provider = "dashscope"

[dashscope]
api_key = "sk-your-api-key"' > config.local.toml
```

### Q2: 续写时提示 "LLM 调用失败"

**可能原因**：
1. API Key 错误或过期
2. 网络连接问题
3. Ollama 服务未启动

**解决方案**：
```bash
# 检查 API Key
cat config.local.toml

# 检查 Ollama 服务（如果使用本地模型）
ollama list
ollama serve
```

### Q3: 如何修改已生成的章节？

**方法 1**：直接用编辑器修改
```bash
# 编辑章节文件
vim workspaces/商业帝国/chapters/ch001.md
```

**方法 2**：删除后重新生成
```bash
# 删除章节
rm workspaces/商业帝国/chapters/ch001.md

# 重新续写
> /use 商业帝国
> /continue
```

### Q4: 如何查看创作历史？

```bash
# 进入小说工作区
cd workspaces/商业帝国

# 查看 Git 历史
git log --oneline

# 查看某次修改
git show <commit-hash>

# 回滚到某个版本
git checkout <commit-hash> -- .
```

### Q5: 如何备份小说数据？

```bash
# 方法 1：直接复制工作区
cp -r workspaces/商业帝国 ~/backup/

# 方法 2：使用 Git 推送到远程仓库
cd workspaces/商业帝国
git remote add origin https://github.com/yourname/商业帝国.git
git push -u origin main
```

### Q6: 如何切换 LLM 模型？

**修改 config.local.toml**：
```toml
[dashscope]
api_key = "sk-your-api-key"
model = "qwen-max"  # 改为 qwen-max
```

**重启 CLI**：
```bash
> /quit
cargo run --release
```

### Q7: 如何添加新角色？

```bash
# 在 characters/ 目录创建角色文件
echo '# 新角色

- 身份: 
- 决策风格: 
- 核心优势: ' > workspaces/商业帝国/characters/新角色.md
```

### Q8: 上下文太长怎么办？

```bash
# 手动压缩上下文
> /compress

# 或者重新开始（保留设定，清空章节）
> /use 商业帝国
> /context  # 查看当前状态
```

---

## 性能优化

### 1. 编译优化

```bash
# Release 模式编译（已优化）
cargo build --release

# 进一步优化（可选）
RUSTFLAGS="-C target-cpu=native" cargo build --release
```

### 2. 内存优化

Z-Writer CLI 默认内存占用 ~2MB，如需进一步优化：

```bash
# 使用更小的模型
# config.local.toml
[dashscope]
model = "qwen-turbo"  # 速度更快，资源占用更少
```

### 3. 磁盘优化

```bash
# 清理旧的 Git 历史（谨慎操作）
cd workspaces/商业帝国
git gc --aggressive
```

---

## 技术架构

### 多智能体系统

```
ControllerAgent (总控)
├─ WorldOutlineAgent (世界观&规则体系)
├─ CharacterAgent (角色塑造)
├─ PlotAgent (剧情策划)
├─ WritingAgent (正文写作)
├─ PolishAgent (润色优化)
└─ ComplianceAgent (合规检测)
```

### 混合 RAG 存储

- **L1 Markdown**：人类可读，易于编辑
- **L2 JSON**：结构化存储，支持复杂查询
- **L3 向量库**：语义检索，素材推荐

### 上下文管理

- **分层压缩**：最近 3 章完整 + 较早章节摘要 + 关键设定永久
- **Token 优化**：自动压缩，节省 API 调用成本
- **滚动摘要**：每 10 章自动深度压缩

---

## 开发相关

### 运行测试

```bash
# 运行所有测试
cargo test

# 运行特定测试
cargo test character_tracker_test

# 查看测试覆盖率
cargo tarpaulin
```

### 调试模式

```bash
# 启用调试日志
RUST_LOG=debug cargo run

# 启用追踪日志
RUST_LOG=trace cargo run
```

### 代码结构

```
src/
├── main.rs              # 入口
├── cli/mod.rs           # CLI 交互层
├── config.rs            # 配置管理
├── workspace.rs         # 工作区管理
├── agents/              # 7 个 Agent
│   ├── controller.rs    # 总控 Agent
│   ├── world_outline.rs # 世界观 Agent
│   ├── character.rs     # 角色 Agent
│   ├── plot.rs          # 剧情 Agent
│   ├── writing.rs       # 写作 Agent
│   ├── polish.rs        # 润色 Agent
│   └── compliance.rs    # 合规 Agent
├── context/             # 上下文管理
│   ├── context_manager.rs
│   ├── character_tracker.rs
│   ├── rolling_summary.rs
│   └── token_optimizer.rs
├── rag/                 # RAG 存储
│   ├── l1_markdown.rs
│   ├── l2_memory_tree.rs
│   └── l3_material_store.rs
├── llm/mod.rs           # LLM 集成
├── wiki/mod.rs          # Wiki 知识库
└── git.rs               # Git 版本管理
```

---

## 更新日志

### v0.2.0 (2026-06-21)

- ✅ 业务定位优化：从玄幻无脑文转向高智商、高情商、权谋博弈、规则利用的高格局都市文
- ✅ Agent 系统提示词重构：7 个 Agent 全部更新
- ✅ Wiki 类型指南重构：新增都市商战、政治权谋、科技革命
- ✅ 爽点设计升级：从境界碾压转向智商碾压、布局收网、规则利用
- ✅ 示例数据全面更新：所有示例替换为高智商都市文案例
- ✅ 131 个测试全部通过

### v0.1.0 (2026-06-19)

- ✅ 基础 CLI 框架
- ✅ 1+6 多智能体架构
- ✅ 混合 RAG 存储（L1 + L2 + L3）
- ✅ 智能上下文管理
- ✅ Git 版本管理
- ✅ 百炼 DashScope 集成
- ✅ Ollama 本地部署支持
- ✅ 94 个测试全部通过

---

## 反馈与支持

如有问题或建议，请：

1. 查看 [README.md](./README.md)
2. 查看 [AGENT_EVOLVE_LOG.md](./AGENT_EVOLVE_LOG.md)
3. 提交 Issue 或 PR

---

**Happy Writing!**
