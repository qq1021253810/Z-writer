---
type: entity
title: Rust CLI 架构
created: 2026-06-18
updated: 2026-06-18
tags: [architecture, rust, cli]
sources: [rust-cli/README.md, rust-cli/src/]
confidence: high
contested: false
contradictions: []
verified_by: human
approved: true
---

# Rust CLI 架构

Z-writer Rust CLI 是基于 Rust 1.96.0 构建的高性能零依赖本地创作工具，与 Java 后端形成互补——CLI 负责本地离线创作，Java 后端负责 Web 端协作。

## 性能指标

| 指标 | 目标值 | 实际值 |
|------|--------|--------|
| 启动时间 | <50ms | ~16ms |
| 内存占用 | <50MB | 2.06MB |
| 磁盘占用 | <15MB | 2.18MB |

## 模块架构

```
src/
├── main.rs             # 入口：初始化日志、加载配置、启动 REPL
├── lib.rs              # 模块声明，11 个公共模块
├── cli/                # CLI 交互层（REPL 循环、命令解析）
│   └── mod.rs          # Command 枚举、run_repl 主循环
├── agents/             # 7 个 Agent 实现
│   ├── base.rs         # Agent trait + AgentContext
│   ├── writing.rs      # WritingAgent（续写）
│   ├── world_outline.rs# WorldOutlineAgent（世界观）
│   ├── character.rs    # CharacterAgent（角色）
│   ├── plot.rs         # PlotAgent（剧情）
│   ├── polish.rs       # PolishAgent（润色）
│   └── compliance.rs   # ComplianceAgent（合规）
├── workflows/          # 工作流
│   ├── create_novel.rs # 新建小说（多轮 Agent 对话）
│   └── fix_writer_block.rs # 卡文修复
├── context/            # 上下文管理
│   ├── context_manager.rs # 分层上下文管理器
│   ├── character_tracker.rs # 角色状态追踪
│   ├── rolling_summary.rs # 滚动摘要
│   └── token_optimizer.rs # Token 优化
├── rag/                # 混合 RAG 存储
│   ├── l2_memory_tree.rs # L2 JSON 剧情记忆树
│   └── l3_material_store.rs # L3 向量素材库
├── llm/                # LLM 集成
│   └── mod.rs          # LlmClient（统一适配器）
├── wiki/               # LLM Wiki 知识库
├── config/             # TOML 配置管理
├── workspace/          # 工作区管理
├── git/                # Git 版本管理
└── error/              # 错误处理
```

## 技术栈

| 组件 | 技术 | 用途 |
|------|------|------|
| 语言 | Rust 1.96.0 | 系统级性能，零运行时 |
| CLI 框架 | clap 4.5 + rustyline 14.0 | 命令解析 + REPL |
| 异步 | tokio 1.42 | 异步运行时 |
| HTTP | reqwest 0.12 | LLM API 调用 |
| 序列化 | serde + serde_json + toml | 配置/数据序列化 |
| Markdown | pulldown-cmark 0.12 | Wiki 解析 |
| 日志 | tracing | 结构化日志 |
| 错误处理 | anyhow + thiserror | 统一错误处理 |
| 彩色输出 | colored 2.1 | 终端彩色界面 |
| Git | git2 (系统 git 二进制) | 版本管理 |

## Release 优化

Cargo.toml 配置了极致的编译优化：

```toml
[profile.release]
lto = true
opt-level = "z"    # 优化二进制大小
strip = true       # 移除调试符号
codegen-units = 1  # 最大化优化机会
panic = "abort"    # 减小二进制体积
```

## 工作模式

CLI 采用 REPL（Read-Eval-Print Loop）模式，启动后进入交互式命令行界面：

1. **启动** → 加载配置、初始化 LLM 客户端、恢复上下文快照
2. **命令循环** → 解析用户输入 → 执行 Agent/工作流 → 输出结果
3. **退出** → 保存上下文快照 → 优雅关闭

## 与 Java 后端的对比

| 维度 | Rust CLI | Java 后端 |
|------|----------|-----------|
| 定位 | 本地离线创作 | Web 端在线协作 |
| 依赖 | 零外部依赖（无 DB/向量库） | PostgreSQL + Redis + ChromaDB + Ollama |
| 存储 | 文件系统（Markdown + JSON） | RDBMS + 向量数据库 |
| Agent | 7 个子 Agent，同架构 | 6 个子 Agent（不含 WritingAgent） |
| 工作流 | create_novel + fix_writer_block | 3 个工作流（含 ContinueChapter） |
| LLM | DashScope + Ollama（二选一） | Ollama 本地模型 |

## 相关页面

- [[cli-commands]] - CLI 命令参考手册
- [[hybrid-rag-storage]] - 混合 RAG 存储（L1/L2/L3）
- [[context-management]] - 上下文管理系统
- [[multi-agent-architecture]] - 多智能体架构
- [[workflow-system]] - 工作流系统