---
type: entity
title: CLI 命令参考
created: 2026-06-18
updated: 2026-06-18
tags: [cli, command, reference]
sources: [rust-cli/src/cli/mod.rs]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# CLI 命令参考

Z-writer Rust CLI 提供 14 个命令，分类为：基础操作、创作工作流、设定管理、上下文管理。

## 基础操作

| 命令 | 说明 | 示例 |
|------|------|------|
| `/help` | 显示帮助信息 | `/help` |
| `/quit` 或 `/exit` | 退出程序（自动保存上下文） | `/quit` |
| `/list` | 列出所有已创建的小说 | `/list` |
| `/use <名称>` | 切换到指定小说 | `/use 都市商战` |
| `/new <名称>` | 创建新小说（空作品） | `/new 科技帝国` |

## 创作工作流

| 命令 | 说明 | 调用 Agent | 执行流程 |
|------|------|------------|----------|
| `/create` | 交互式新建小说（多轮 Agent 对话引导） | WorldOutlineAgent + CharacterAgent + PlotAgent | 选择赛道 → 设计世界观 → 设计角色 → 生成大纲 |
| `/continue` | 自动续写下一章节 | WritingAgent | 读取上下文 → 分析前情 → 生成章节 → 保存 |
| `/fix` | 卡文修复：分析逻辑漏洞，提供修复方案 | PlotAgent | 分析卡文原因 → 生成 3 套方案 |
| `/polish` | 润色最新章节：优化语言和节奏 | PolishAgent | 读取最新章节 → 润色优化 → 输出结果 |

### `/create` 交互流程

1. 显示赛道选择提示（都市商战/政治权谋/科技革命/科幻）
2. 调用 WorldOutlineAgent 设计世界观
3. 调用 CharacterAgent 设计主角
4. 调用 PlotAgent 生成大纲
5. 创建工作区，保存所有设定文件

### `/continue` 自动续写

```
> /continue
正在准备续写...
准备续写第 3 章
[WritingAgent 生成内容]
第 3 章已保存
[内容预览...]
```

## 设定管理

| 命令 | 说明 | 调用 Agent | 保存位置 |
|------|------|------------|----------|
| `/world` | 世界观设计（多行输入，空行结束） | WorldOutlineAgent | `worldview.md` |
| `/character` | 角色设计（多行输入，空行结束） | CharacterAgent | `characters/角色_{timestamp}.md` |
| `/compliance` | 合规检测（检查最近 3 章） | ComplianceAgent | 仅控制台输出 |

## 上下文管理

| 命令 | 说明 | 适用场景 |
|------|------|----------|
| `/context` | 显示上下文统计（Token 用量、各 Tier 状态） | 了解当前上下文压力 |
| `/compress` | 手动触发上下文压缩（调用 LLM 压缩最近 10 章） | Token 接近预算上限时 |
| `/wiki-health` | Wiki 健康检查（实体页面数、素材数、剧情记忆树） | 检查知识库完整性 |
| `/stats` | 显示全局 Token 使用统计 | 了解 API 消耗 |

### `/context` 输出示例

```
上下文统计:
  小说信息: 2450 字
  世界观: 1800 字
  大纲: 3200 字
  角色数: 5
  当前章节: 第 3 章
  下一章: 第 4 章
  --- 上下文管理器 ---
  Tier 1 最近消息: 6 条
  Tier 2 压缩摘要: 1 条
  Tier 3 永久保留: 3 条
  上下文 Token 用量: 2150 / 4096 (52.5%)
```

### `/compress` 压缩流程

1. 读取最近 10 章内容
2. 调用 LLM 生成 1000 字摘要
3. 更新 ContextManager 的 Tier 2 压缩区
4. 输出压缩结果预览

## 命令别名

| 主要命令 | 别名 |
|----------|------|
| `/quit` | `/exit`, `quit`, `exit` |
| `/help` | `help` |

## 错误处理

所有 Agent 命令共用统一的错误处理机制：

```rust
async fn execute_agent_with_feedback<F>(
    success_msg: &str,
    operation: F,
) -> Result<()>
```

- 成功：绿色输出成功消息
- 失败：红色输出错误详情，记录 tracing error

## REPL 特性

- Tab 键自动补全命令
- ↑↓ 键浏览历史命令
- 多行输入以空行结束
- Ctrl+C 优雅关闭（自动保存上下文）
- Ctrl+D 退出
- 自动恢复上次会话的上下文（`.context_snapshot.json`）

## 相关页面

- [[rust-cli-architecture]] - Rust CLI 整体架构
- [[context-management]] - 上下文管理系统
- [[multi-agent-architecture]] - Agent 调用详情