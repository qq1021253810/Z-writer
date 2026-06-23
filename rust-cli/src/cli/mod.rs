//! CLI 交互层

use rustyline::error::ReadlineError;
use rustyline::DefaultEditor;
use colored::*;
use crate::config::AppConfig;
use crate::error::Result;
use crate::workspace::Workspace;
use crate::llm::LlmClient;
use crate::agents::base::{Agent, AgentContext};
use crate::agents::writing::WritingAgent;
use crate::agents::plot::PlotAgent;
use crate::agents::polish::PolishAgent;
use crate::agents::world_outline::WorldOutlineAgent;
use crate::agents::character::CharacterAgent;
use crate::agents::compliance::ComplianceAgent;
use crate::context::context_manager::ContextManager;

/// 常用提示信息
const MSG_NO_NOVEL_SELECTED: &str = "请先使用 /use <小说名> 切换到小说";

/// 执行 Agent 命令并处理结果（统一错误处理）
async fn execute_agent_with_feedback<F>(
    success_msg: &str,
    operation: F,
) -> Result<()>
where
    F: std::future::Future<Output = Result<()>>,
{
    match operation.await {
        Ok(_) => println!("{}", success_msg.green()),
        Err(e) => {
            tracing::error!("[CLI] 操作失败: {}", e);
            println!("{}", format!("错误: {}", e).red());
        }
    }
    Ok(())
}

/// 命令解析
pub enum Command {
    Help,
    Quit,
    List,
    Use(String),
    New(String),
    Continue,
    Fix,
    Polish,
    World,
    Character,
    Compliance,
    Create,
    Context,
    Compress,
    WikiHealth,
    Stats,
    Unknown(String),
}

impl Command {
    pub fn parse(input: &str) -> Self {
        let input = input.trim();
        
        if input.is_empty() {
            return Command::Unknown(String::new());
        }

        let parts: Vec<&str> = input.split_whitespace().collect();
        
        match parts[0] {
            "/help" | "help" => Command::Help,
            "/quit" | "/exit" | "quit" | "exit" => Command::Quit,
            "/list" => Command::List,
            "/use" => {
                if parts.len() > 1 {
                    Command::Use(parts[1].to_string())
                } else {
                    Command::Unknown("缺少小说名称".to_string())
                }
            }
            "/new" => {
                if parts.len() > 1 {
                    Command::New(parts[1].to_string())
                } else {
                    Command::Unknown("缺少小说名称".to_string())
                }
            }
            "/continue" => Command::Continue,
            "/fix" => Command::Fix,
            "/polish" => Command::Polish,
            "/world" => Command::World,
            "/character" => Command::Character,
            "/compliance" => Command::Compliance,
            "/create" => Command::Create,
            "/context" => Command::Context,
            "/compress" => Command::Compress,
            "/wiki-health" => Command::WikiHealth,
            "/stats" => Command::Stats,
            _ => Command::Unknown(input.to_string()),
        }
    }
}

/// REPL 主循环
pub async fn run_repl(config: AppConfig) -> Result<()> {
    let mut rl = DefaultEditor::new()?;
    let mut current_workspace: Option<Workspace> = None;
    let llm_client = LlmClient::new(&config);
    let mut ctx_manager = ContextManager::new(config.token_budget);
    
    // 尝试恢复上次会话的上下文
    let context_file = config.workspace_path.join(".context_snapshot.json");
    if context_file.exists() {
        if let Err(e) = ctx_manager.load_from_file(&context_file) {
            tracing::warn!("[CLI] 恢复上下文失败: {}", e);
        } else {
            println!("{}", "已恢复上次会话的上下文".green());
        }
    }
    
    let provider_name = match config.provider {
        crate::config::LlmProvider::Dashscope => format!("百炼 ({})", config.dashscope.model),
        crate::config::LlmProvider::Ollama => format!("Ollama ({})", config.chat_model),
    };
    
    println!("{}", "╔════════════════════════════════════════╗".cyan());
    println!("{}", "║   Z-Writer CLI - 网文小说创作助手     ║".cyan());
    println!("{}", format!("║   LLM: {:<30}║", provider_name).cyan());
    println!("{}", "║   输入 /help 查看可用命令              ║".cyan());
    println!("{}", "╚════════════════════════════════════════╝".cyan());
    println!();

    // 设置 Ctrl+C 信号处理器，实现优雅关闭
    let running = std::sync::Arc::new(std::sync::atomic::AtomicBool::new(true));
    let r = running.clone();
    tokio::spawn(async move {
        if let Ok(()) = tokio::signal::ctrl_c().await {
            r.store(false, std::sync::atomic::Ordering::SeqCst);
        }
    });

    while running.load(std::sync::atomic::Ordering::SeqCst) {
        let prompt = if let Some(ref ws) = current_workspace {
            format!("[{}] > ", ws.name()).cyan().to_string()
        } else {
            "> ".to_string()
        };
        
        let readline = rl.readline(&prompt);
        
        match readline {
            Ok(line) => {
                let _ = rl.add_history_entry(&line);
                let cmd = Command::parse(&line);
                
                match cmd {
                    Command::Help => show_help(),
                    Command::Quit => {
                        // 优雅关闭：保存上下文快照
                        if let Err(e) = ctx_manager.save_to_file(&context_file) {
                            tracing::error!("[CLI] 保存上下文失败: {}", e);
                            println!("{}", format!("警告: 保存上下文失败: {}", e).yellow());
                        } else {
                            tracing::info!("[CLI] 上下文已保存");
                        }
                        println!("{}", "再见！".green());
                        break;
                    }
                    Command::List => {
                        match Workspace::list_all(&config.workspace_path) {
                            Ok(names) => {
                                if names.is_empty() {
                                    println!("{}", "暂无小说".yellow());
                                } else {
                                    println!("{}", "已有小说:".green());
                                    for name in names {
                                        println!("  - {}", name);
                                    }
                                }
                            }
                            Err(e) => println!("{}", format!("错误: {}", e).red()),
                        }
                    }
                    Command::Use(name) => {
                        let ws_path = config.workspace_path.join(&name);
                        match Workspace::open(ws_path) {
                            Ok(ws) => {
                                println!("{}", format!("已切换到小说: {}", name).green());
                                current_workspace = Some(ws);
                            }
                            Err(e) => println!("{}", format!("错误: {}", e).red()),
                        }
                    }
                    Command::New(name) => {
                        match Workspace::create(&config.workspace_path, &name) {
                            Ok(ws) => {
                                println!("{}", format!("已创建小说: {}", name).green());
                                current_workspace = Some(ws);
                            }
                            Err(e) => println!("{}", format!("错误: {}", e).red()),
                        }
                    }
                    Command::Continue => {
                        tracing::info!("[CLI] 执行 continue 命令");
                        if let Some(ref ws) = current_workspace {
                            execute_agent_with_feedback("续写完成", run_agent_command(ws, &llm_client, &mut ctx_manager, "continue", "")).await?;
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Fix => {
                        tracing::info!("[CLI] 执行 fix 命令");
                        if let Some(ref ws) = current_workspace {
                            execute_agent_with_feedback("卡文修复完成", run_agent_command(ws, &llm_client, &mut ctx_manager, "fix", "")).await?;
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Polish => {
                        tracing::info!("[CLI] 执行 polish 命令");
                        if let Some(ref ws) = current_workspace {
                            let user_input = read_user_input_for_polish(ws);
                            execute_agent_with_feedback("润色完成", run_agent_command(ws, &llm_client, &mut ctx_manager, "polish", &user_input)).await?;
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::World => {
                        tracing::info!("[CLI] 执行 world 命令");
                        if let Some(ref ws) = current_workspace {
                            println!("{}", "请输入世界观设计需求（如：设计一个近未来科技商业帝国的规则体系）：".cyan());
                            let user_input = read_multiline(&mut rl)?;
                            execute_agent_with_feedback("世界观设计完成", run_agent_command(ws, &llm_client, &mut ctx_manager, "world", &user_input)).await?;
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Character => {
                        tracing::info!("[CLI] 执行 character 命令");
                        if let Some(ref ws) = current_workspace {
                            println!("{}", "请输入角色设计需求（如：设计一个白手起家的科技创业者角色）：".cyan());
                            let user_input = read_multiline(&mut rl)?;
                            execute_agent_with_feedback("角色设计完成", run_agent_command(ws, &llm_client, &mut ctx_manager, "character", &user_input)).await?;
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Compliance => {
                        tracing::info!("[CLI] 执行 compliance 命令");
                        if let Some(ref ws) = current_workspace {
                            let user_input = read_user_input_for_compliance(ws);
                            execute_agent_with_feedback("合规检测完成", run_agent_command(ws, &llm_client, &mut ctx_manager, "compliance", &user_input)).await?;
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Create => {
                        tracing::info!("[CLI] 执行 create 命令");
                        println!("{}", "请选择赛道（都市商战/政治权谋/科技革命/科幻）：".cyan());
                        let genre = match rl.readline("  赛道 > ") {
                            Ok(line) => {
                                let line = line.trim().to_string();
                                if line.is_empty() { "都市商战".to_string() } else { line }
                            }
                            Err(_) => "都市商战".to_string(),
                        };
                        tracing::info!("[CLI] 用户选择赛道: {}", genre);
                        println!("{}", format!("正在启动新建小说工作流（赛道：{}）...", genre).cyan());
                        match run_create_workflow(&config.workspace_path, &llm_client, &genre).await {
                            Ok(ws) => {
                                println!("{}", "新小说创建完成".green());
                                current_workspace = Some(ws);
                            }
                            Err(e) => {
                                tracing::error!("[CLI] 操作失败: {}", e);
                                println!("{}", format!("错误: {}", e).red());
                            }
                        }
                    }
                    Command::Context => {
                        if let Some(ref ws) = current_workspace {
                            show_context(ws, &ctx_manager);
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Compress => {
                        tracing::info!("[CLI] 执行 compress 命令");
                        if let Some(ref ws) = current_workspace {
                            match compress_context(ws, &llm_client, &mut ctx_manager).await {
                                Ok(_) => println!("{}", "上下文压缩完成".green()),
                                Err(e) => {
                                    tracing::error!("[CLI] 操作失败: {}", e);
                                    println!("{}", format!("错误: {}", e).red());
                                }
                            }
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::WikiHealth => {
                        if let Some(ref ws) = current_workspace {
                            check_wiki_health(ws);
                        } else {
                            println!("{}", MSG_NO_NOVEL_SELECTED.yellow());
                        }
                    }
                    Command::Stats => {
                        show_token_stats(&llm_client);
                    }
                    Command::Unknown(input) => {
                        if !input.is_empty() {
                            println!("{}", format!("未知命令: {}", input).red());
                            println!("{}", "输入 /help 查看可用命令".yellow());
                        }
                    }
                }
            }
            Err(ReadlineError::Interrupted) => {
                println!("{}", "按 Ctrl+D 退出".yellow());
                continue;
            }
            Err(ReadlineError::Eof) => {
                println!("{}", "再见！".green());
                break;
            }
            Err(err) => {
                println!("{}", format!("错误: {}", err).red());
                break;
            }
        }
    }

    Ok(())
}

/// 统一 Agent 命令路由
async fn run_agent_command(workspace: &Workspace, client: &LlmClient, ctx_manager: &mut ContextManager, cmd: &str, user_input: &str) -> Result<()> {
    tracing::info!("[CLI] 执行 {} 命令", cmd);
    
    // Token 预算检查
    check_token_budget(ctx_manager);
    
    match cmd {
        "continue" => handle_continue(workspace, client, ctx_manager).await,
        "fix" => handle_fix(workspace, client).await,
        "polish" => handle_polish(workspace, client, user_input).await,
        "world" => handle_world(workspace, client, user_input).await,
        "character" => handle_character(workspace, client, user_input).await,
        "compliance" => handle_compliance(workspace, client, user_input).await,
        _ => Err(crate::error::AppError::Agent(format!("未知 Agent 命令: {}", cmd))),
    }
}

/// Token 预算检查
fn check_token_budget(ctx_manager: &ContextManager) {
    let current_tokens = ctx_manager.current_tokens();
    let budget = ctx_manager.token_budget;
    tracing::debug!("[CLI] 上下文 Token 使用: {}/{}", current_tokens, budget);
    
    if current_tokens > budget {
        println!("{}", format!("⚠ 上下文 Token 用量 ({}) 已超过预算 ({})，建议执行 /compress 压缩", current_tokens, budget).yellow());
    } else if current_tokens > budget * 8 / 10 {
        println!("{}", format!("⚠ 上下文 Token 用量 ({}/{}) 已达 {:.0}%，建议执行 /compress 压缩", current_tokens, budget, current_tokens as f64 / budget as f64 * 100.0).yellow());
    }
}

/// 处理续写命令
async fn handle_continue(workspace: &Workspace, client: &LlmClient, ctx_manager: &mut ContextManager) -> Result<()> {
    println!("{}", "正在准备续写...".cyan());
    let chapter_num = workspace.next_chapter_num()?;
    println!("{}", format!("准备续写第 {} 章", chapter_num).cyan());

    let agent = WritingAgent::new(client.clone());
    let ctx = AgentContext {
        workspace_path: workspace.root().to_path_buf(),
        user_input: format!("续写第 {} 章，保持与前文连贯，字数约 2000 字", chapter_num),
        system_prompt: agent.system_prompt(),
    };
    let result = agent.execute(&ctx).await?;
    
    // 记录到上下文管理器
    ctx_manager.add_message("user", &format!("续写第 {} 章", chapter_num));
    ctx_manager.add_message("assistant", &result.content[..result.content.len().min(200)]);
    
    // 保存章节
    workspace.save_chapter(chapter_num, &result.content)?;
    tracing::info!("[CLI] 保存文件: chapter_{}.md", chapter_num);
    println!("{}", format!("第 {} 章已保存", chapter_num).green());
    
    print_result_preview(&result.content, 500);
    Ok(())
}

/// 处理卡文修复命令
async fn handle_fix(workspace: &Workspace, client: &LlmClient) -> Result<()> {
    println!("{}", "正在分析卡文问题...".cyan());
    let next_chapter = workspace.next_chapter_num()?;
    if next_chapter <= 1 {
        println!("{}", "还没有章节可以修复".yellow());
        return Ok(());
    }
    let last_chapter_num = next_chapter - 1;
    
    let agent = PlotAgent::new(client.clone());
    let ctx = AgentContext {
        workspace_path: workspace.root().to_path_buf(),
        user_input: format!("第 {} 章出现了卡文问题。请提供 3 个不同的续写方案，每个方案约 500 字，帮助作者突破创作瓶颈。", last_chapter_num),
        system_prompt: agent.system_prompt(),
    };
    let result = agent.execute(&ctx).await?;
    
    print_result_preview(&result.content, usize::MAX);
    Ok(())
}

/// 处理润色命令
async fn handle_polish(workspace: &Workspace, client: &LlmClient, user_input: &str) -> Result<()> {
    println!("{}", "正在润色内容...".cyan());
    let agent = PolishAgent::new(client.clone());
    let ctx = AgentContext {
        workspace_path: workspace.root().to_path_buf(),
        user_input: user_input.to_string(),
        system_prompt: agent.system_prompt(),
    };
    let result = agent.execute(&ctx).await?;
    
    print_result_preview(&result.content, usize::MAX);
    Ok(())
}

/// 处理世界观设计命令
async fn handle_world(workspace: &Workspace, client: &LlmClient, user_input: &str) -> Result<()> {
    println!("{}", "正在设计世界观...".cyan());
    let agent = WorldOutlineAgent::new(client.clone());
    let ctx = AgentContext {
        workspace_path: workspace.root().to_path_buf(),
        user_input: user_input.to_string(),
        system_prompt: agent.system_prompt(),
    };
    let result = agent.execute(&ctx).await?;
    
    // 保存世界观
    let worldview_content = format!("# 世界观设定\n\n{}", result.content);
    std::fs::write(workspace.root().join("worldview.md"), &worldview_content)?;
    tracing::info!("[CLI] 保存文件: worldview.md");
    println!("{}", "世界观已保存到 worldview.md".green());
    
    print_result_preview(&result.content, 800);
    Ok(())
}

/// 处理角色设计命令
async fn handle_character(workspace: &Workspace, client: &LlmClient, user_input: &str) -> Result<()> {
    println!("{}", "正在设计角色...".cyan());
    let agent = CharacterAgent::new(client.clone());
    let ctx = AgentContext {
        workspace_path: workspace.root().to_path_buf(),
        user_input: user_input.to_string(),
        system_prompt: agent.system_prompt(),
    };
    let result = agent.execute(&ctx).await?;
    
    // 保存角色
    let char_dir = workspace.root().join("characters");
    std::fs::create_dir_all(&char_dir)?;
    let timestamp = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .expect("系统时间获取失败")
        .as_secs();
    let char_name = format!("角色_{}.md", timestamp);
    std::fs::write(char_dir.join(&char_name), &result.content)?;
    tracing::info!("[CLI] 保存文件: characters/{}", char_name);
    println!("{}", format!("角色已保存到 characters/{}", char_name).green());
    
    print_result_preview(&result.content, 800);
    Ok(())
}

/// 处理合规检测命令
async fn handle_compliance(workspace: &Workspace, client: &LlmClient, user_input: &str) -> Result<()> {
    println!("{}", "正在进行合规检测...".cyan());
    let agent = ComplianceAgent::new(client.clone());
    let ctx = AgentContext {
        workspace_path: workspace.root().to_path_buf(),
        user_input: user_input.to_string(),
        system_prompt: agent.system_prompt(),
    };
    let result = agent.execute(&ctx).await?;
    
    print_result_preview(&result.content, usize::MAX);
    Ok(())
}

/// 打印结果预览
fn print_result_preview(content: &str, max_len: usize) {
    println!("{}", "─".repeat(40).cyan());
    if content.len() > max_len {
        println!("{}", &content[..max_len]);
        println!("... (已截断，完整内容已保存到文件)");
    } else {
        println!("{}", content);
    }
    println!("{}", "─".repeat(40).cyan());
}

/// 读取润色输入（最新章节内容）
fn read_user_input_for_polish(workspace: &Workspace) -> String {
    if let Ok(next) = workspace.next_chapter_num() {
        if next > 1 {
            if let Ok(Some(content)) = workspace.read_chapter(next - 1) {
                return content;
            }
        }
    }
    String::new()
}

/// 读取合规检测输入（最新章节内容）
fn read_user_input_for_compliance(workspace: &Workspace) -> String {
    // 收集最近3章内容
    let mut content = String::new();
    if let Ok(recent) = workspace.recent_chapters(3) {
        for (num, text) in &recent {
            content.push_str(&format!("第 {} 章:\n{}\n\n", num, text));
        }
    }
    if content.is_empty() {
        content = "暂无内容可检测".to_string();
    }
    content
}

/// 读取多行输入（以空行结束）
fn read_multiline(rl: &mut DefaultEditor) -> Result<String> {
    let mut lines = Vec::new();
    loop {
        let line = rl.readline("  > ")?;
        if line.trim().is_empty() {
            break;
        }
        lines.push(line);
    }
    if lines.is_empty() {
        return Err(crate::error::AppError::Agent("输入为空".to_string()));
    }
    Ok(lines.join("\n"))
}

/// 运行新建小说工作流
async fn run_create_workflow(base_path: &std::path::Path, client: &LlmClient, genre: &str) -> Result<Workspace> {
    use crate::workflows::create_novel::CreateNovelWorkflow;
    
    let workflow = CreateNovelWorkflow::new(client.clone());
    workflow.execute(base_path, genre).await?;
    
    // 返回创建的工作区
    let ws_path = base_path.join(genre);
    Workspace::open(ws_path)
}

/// 显示上下文统计
fn show_context(workspace: &Workspace, ctx_manager: &ContextManager) {
    println!("{}", "上下文统计:".green().bold());
    
    if let Ok(info) = workspace.novel_info() {
        println!("  小说信息: {} 字", info.len());
    }
    
    if let Ok(worldview) = workspace.worldview() {
        println!("  世界观: {} 字", worldview.len());
    }
    
    if let Ok(outline) = workspace.outline() {
        println!("  大纲: {} 字", outline.len());
    }
    
    if let Ok(chars) = workspace.characters() {
        println!("  角色数: {}", chars.len());
    }
    
    if let Ok(next) = workspace.next_chapter_num() {
        println!("  当前章节: 第 {} 章", next - 1);
        println!("  下一章: 第 {} 章", next);
    }
    
    // ContextManager 状态
    let ctx_tokens = ctx_manager.current_tokens();
    println!("  --- 上下文管理器 ---");
    println!("  Tier 1 最近消息: {} 条", ctx_manager.tier1_recent.len());
    println!("  Tier 2 压缩摘要: {} 条", ctx_manager.tier2_compressed.len());
    println!("  Tier 3 永久保留: {} 条", ctx_manager.tier3_permanent.len());
    println!("  上下文 Token 用量: {} / {} ({:.1}%)",
        ctx_tokens,
        ctx_manager.token_budget,
        if ctx_manager.token_budget > 0 { ctx_tokens as f64 / ctx_manager.token_budget as f64 * 100.0 } else { 0.0 }
    );
    
    println!();
}

/// 上下文压缩
async fn compress_context(workspace: &Workspace, client: &LlmClient, ctx_manager: &mut ContextManager) -> Result<()> {
    println!("{}", "正在压缩上下文...".cyan());
    
    let recent = workspace.recent_chapters(10)?;
    
    if recent.is_empty() {
        println!("{}", "没有章节可以压缩".yellow());
        return Ok(());
    }
    
    let mut prompt = String::new();
    prompt.push_str("【章节内容】\n");
    for (num, content) in &recent {
        prompt.push_str(&format!("第 {} 章:\n{}\n\n", num, content));
    }
    
    prompt.push_str("\n请将以上章节内容压缩为 1000 字的摘要，保留关键情节、人物发展和重要细节。\n");
    
    println!("{}", "正在调用 LLM 压缩...".cyan());
    
    let messages = vec![
        crate::llm::ChatMessage {
            role: "system".to_string(),
            content: "你是一位专业的文本摘要专家，擅长将长文本压缩为简洁的摘要，同时保留关键信息。".to_string(),
        },
        crate::llm::ChatMessage {
            role: "user".to_string(),
            content: prompt,
        },
    ];
    
    let result = client.chat(messages).await?;
    
    // 使用 ContextManager 进行压缩
    ctx_manager.compress_tier2(&result.content);
    
    println!("{}", format!("本次调用 - 输入: {} tokens, 输出: {} tokens, 总计: {} tokens",
        result.usage.prompt_tokens,
        result.usage.completion_tokens,
        result.usage.total()
    ).dimmed());
    
    println!("{}", "─".repeat(40).cyan());
    println!("{}", result.content);
    println!("{}", "─".repeat(40).cyan());
    println!("{}", format!("压缩完成: {} 章 → {} 字", recent.len(), result.content.len()).green());
    println!("{}", "上下文管理器已更新: Tier2 已压缩为单条摘要".to_string().dimmed());
    
    Ok(())
}

/// 显示 Token 统计信息
fn show_token_stats(client: &LlmClient) {
    println!("{}", "Token 使用统计:".green().bold());
    println!("  总输入 tokens: {}", client.tracker.total_prompt());
    println!("  总输出 tokens: {}", client.tracker.total_completion());
    println!("  总 tokens: {}", client.tracker.total_tokens());
    println!("  调用次数: {}", client.tracker.call_count());
    println!();
}

/// Wiki 健康检查
fn check_wiki_health(workspace: &Workspace) {
    use crate::wiki::Wiki;
    
    let wiki_path = workspace.root().join("wiki");
    let _wiki = Wiki::new(wiki_path);
    
    println!("{}", "Wiki 健康检查:".green().bold());
    
    let entities_dir = workspace.root().join("wiki/entities");
    if entities_dir.exists() {
        let entities: Vec<_> = std::fs::read_dir(&entities_dir)
            .map(|entries| entries.filter_map(|e| e.ok()).filter(|e| e.path().extension().and_then(|s| s.to_str()) == Some("md")).collect())
            .unwrap_or_default();
        println!("  实体页面: {} 个", entities.len());
    } else {
        println!("  实体页面: 0 个");
    }
    
    let materials_path = workspace.root().join("vector_store/materials.json");
    if materials_path.exists() {
        if let Ok(content) = std::fs::read_to_string(&materials_path) {
            if let Ok(materials) = serde_json::from_str::<Vec<serde_json::Value>>(&content) {
                println!("  素材数量: {} 条", materials.len());
            }
        }
    }
    
    let memory_tree_path = workspace.root().join("memory_tree.json");
    if memory_tree_path.exists() {
        if let Ok(content) = std::fs::read_to_string(&memory_tree_path) {
            if let Ok(tree) = serde_json::from_str::<serde_json::Value>(&content) {
                let volumes = tree.get("volumes").and_then(|v| v.as_array()).map(|a| a.len()).unwrap_or(0);
                let foreshadows = tree.get("foreshadows").and_then(|v| v.as_array()).map(|a| a.len()).unwrap_or(0);
                println!("  卷数: {}", volumes);
                println!("  伏笔: {} 条", foreshadows);
            }
        }
    }
    
    println!();
}

fn show_help() {
    println!("{}", "可用命令:".green().bold());
    println!();
    println!("{}", "【基础操作】".cyan().bold());
    println!("  /help          - 显示帮助信息");
    println!("  /quit          - 退出程序");
    println!("  /list          - 列出所有小说");
    println!("  /use <name>    - 切换到指定小说");
    println!("                 示例: /use 都市商战");
    println!("  /new <name>    - 创建新小说");
    println!("                 示例: /new 科技帝国");
    println!();
    println!("{}", "【创作工作流】".cyan().bold());
    println!("  /create        - 交互式新建小说（Agent 工作流）");
    println!("                 流程: 选择赛道 → 生成世界观 → 设计角色 → 生成大纲");
    println!("  /continue      - 续写章节（WritingAgent）");
    println!("                 自动读取上下文、世界观、角色设定，生成下一章");
    println!("  /fix           - 卡文修复（PlotAgent）");
    println!("                 分析当前章节逻辑漏洞，提供修复方案");
    println!("  /polish        - 润色最新章节（PolishAgent）");
    println!("                 优化语言表达、情感描写、节奏控制");
    println!();
    println!("{}", "【设定管理】".cyan().bold());
    println!("  /world         - 世界观设计（WorldOutlineAgent）");
    println!("                 示例: 设计一个近未来科技商业帝国的规则体系");
    println!("  /character     - 角色设计（CharacterAgent）");
    println!("                 示例: 设计一个白手起家的科技创业者角色");
    println!("  /compliance    - 合规检测（ComplianceAgent）");
    println!("                 检查内容是否符合平台规范");
    println!();
    println!("{}", "【上下文管理】".cyan().bold());
    println!("  /context       - 显示上下文统计");
    println!("                 显示 Token 使用量、章节摘要、角色状态");
    println!("  /compress      - 手动触发压缩");
    println!("                 当上下文接近预算时，压缩历史内容");
    println!("  /wiki-health   - Wiki 健康检查");
    println!("                 检查知识库完整性、缺失页面");
    println!("  /stats         - Token 使用统计");
    println!("                 显示累计 Token 消耗、调用次数");
    println!();
    println!("{}", "【提示】".yellow().bold());
    println!("  - 使用 Tab 键自动补全命令");
    println!("  - 使用 ↑↓ 键浏览历史命令");
    println!("  - 多行输入: 输入空行结束");
    println!();
}