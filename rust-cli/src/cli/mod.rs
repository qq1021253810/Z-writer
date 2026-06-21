//! CLI 交互层

use rustyline::error::ReadlineError;
use rustyline::DefaultEditor;
use colored::*;
use crate::config::AppConfig;
use crate::error::Result;
use crate::workspace::Workspace;
use crate::llm::LlmClient;

/// 命令解析
pub enum Command {
    Help,
    Quit,
    List,
    Use(String),
    New(String),
    Continue,
    Fix,
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

    loop {
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
                        if let Some(ref ws) = current_workspace {
                            match continue_chapter(ws, &llm_client).await {
                                Ok(_) => println!("{}", "续写完成".green()),
                                Err(e) => println!("{}", format!("错误: {}", e).red()),
                            }
                        } else {
                            println!("{}", "请先使用 /use <小说名> 切换到小说".yellow());
                        }
                    }
                    Command::Fix => {
                        if let Some(ref ws) = current_workspace {
                            match fix_writer_block(ws, &llm_client).await {
                                Ok(_) => println!("{}", "卡文修复完成".green()),
                                Err(e) => println!("{}", format!("错误: {}", e).red()),
                            }
                        } else {
                            println!("{}", "请先使用 /use <小说名> 切换到小说".yellow());
                        }
                    }
                    Command::Context => {
                        if let Some(ref ws) = current_workspace {
                            show_context(ws);
                        } else {
                            println!("{}", "请先使用 /use <小说名> 切换到小说".yellow());
                        }
                    }
                    Command::Compress => {
                        if let Some(ref ws) = current_workspace {
                            match compress_context(ws, &llm_client).await {
                                Ok(_) => println!("{}", "上下文压缩完成".green()),
                                Err(e) => println!("{}", format!("错误: {}", e).red()),
                            }
                        } else {
                            println!("{}", "请先使用 /use <小说名> 切换到小说".yellow());
                        }
                    }
                    Command::WikiHealth => {
                        if let Some(ref ws) = current_workspace {
                            check_wiki_health(ws);
                        } else {
                            println!("{}", "请先使用 /use <小说名> 切换到小说".yellow());
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

/// 续写章节
async fn continue_chapter(workspace: &Workspace, client: &LlmClient) -> Result<()> {
    println!("{}", "正在准备续写...".cyan());
    
    let chapter_num = workspace.next_chapter_num()?;
    println!("{}", format!("准备续写第 {} 章", chapter_num).cyan());
    
    // 收集上下文
    let novel_info = workspace.novel_info()?;
    let worldview = workspace.worldview()?;
    let outline = workspace.outline()?;
    let characters = workspace.characters()?;
    let recent = workspace.recent_chapters(3)?;
    
    // 构建 prompt
    let mut prompt = String::new();
    prompt.push_str("【小说信息】\n");
    prompt.push_str(&novel_info);
    prompt.push_str("\n");
    
    if !worldview.is_empty() && worldview != "# 世界观设定\n\n待补充\n" {
        prompt.push_str("【世界观】\n");
        prompt.push_str(&worldview);
        prompt.push_str("\n");
    }
    
    if !outline.is_empty() && outline != "# 大纲\n\n待补充\n" {
        prompt.push_str("【大纲】\n");
        prompt.push_str(&outline);
        prompt.push_str("\n");
    }
    
    if !characters.is_empty() {
        prompt.push_str("【角色设定】\n");
        for char_info in &characters {
            prompt.push_str(char_info);
            prompt.push_str("\n");
        }
    }
    
    if !recent.is_empty() {
        prompt.push_str("【前情提要】\n");
        for (num, content) in &recent {
            prompt.push_str(&format!("第 {} 章:\n{}\n\n", num, content));
        }
    }
    
    prompt.push_str(&format!("\n请续写第 {} 章，保持与前文连贯，字数约 2000 字。\n", chapter_num));
    
    println!("{}", "正在调用 LLM 生成内容...".cyan());
    
    // 调用 LLM
    let messages = vec![
        crate::llm::ChatMessage {
            role: "system".to_string(),
            content: "你是一位专业的网文小说作家，擅长创作精彩的章节内容。请根据提供的设定和前文，续写下一章。".to_string(),
        },
        crate::llm::ChatMessage {
            role: "user".to_string(),
            content: prompt,
        },
    ];
    
    let result = client.chat(messages).await?;
    
    // 显示 token 统计
    println!("{}", format!("本次调用 - 输入: {} tokens, 输出: {} tokens, 总计: {} tokens",
        result.usage.prompt_tokens,
        result.usage.completion_tokens,
        result.usage.total()
    ).dimmed());
    
    // 保存章节
    workspace.save_chapter(chapter_num, &result.content)?;
    
    println!("{}", format!("第 {} 章已保存", chapter_num).green());
    println!("{}", "─".repeat(40).cyan());
    println!("{}", &result.content[..result.content.len().min(500)]);
    if result.content.len() > 500 {
        println!("... (已截断，完整内容已保存到文件)");
    }
    println!("{}", "─".repeat(40).cyan());
    
    Ok(())
}

/// 显示上下文统计
fn show_context(workspace: &Workspace) {
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
    
    println!();
}

/// 卡文修复
async fn fix_writer_block(workspace: &Workspace, client: &LlmClient) -> Result<()> {
    println!("{}", "正在分析卡文问题...".cyan());
    
    let next_chapter = workspace.next_chapter_num()?;
    if next_chapter <= 1 {
        println!("{}", "还没有章节可以修复".yellow());
        return Ok(());
    }
    
    let last_chapter_num = next_chapter - 1;
    let last_chapter = workspace.read_chapter(last_chapter_num)?;
    
    if last_chapter.is_none() {
        println!("{}", "找不到最新章节".yellow());
        return Ok(());
    }
    
    let last_chapter = last_chapter.unwrap();
    
    // 收集上下文
    let novel_info = workspace.novel_info()?;
    let worldview = workspace.worldview()?;
    let outline = workspace.outline()?;
    
    let mut prompt = String::new();
    prompt.push_str("【小说信息】\n");
    prompt.push_str(&novel_info);
    prompt.push_str("\n");
    
    if !worldview.is_empty() && worldview != "# 世界观设定\n\n待补充\n" {
        prompt.push_str("【世界观】\n");
        prompt.push_str(&worldview);
        prompt.push_str("\n");
    }
    
    if !outline.is_empty() && outline != "# 大纲\n\n待补充\n" {
        prompt.push_str("【大纲】\n");
        prompt.push_str(&outline);
        prompt.push_str("\n");
    }
    
    prompt.push_str("【最新章节】\n");
    prompt.push_str(&last_chapter);
    prompt.push_str("\n");
    
    prompt.push_str(&format!("\n第 {} 章出现了卡文问题。请提供 3 个不同的续写方案，每个方案约 500 字，帮助作者突破创作瓶颈。\n", last_chapter_num));
    
    println!("{}", "正在生成修复方案...".cyan());
    
    let messages = vec![
        crate::llm::ChatMessage {
            role: "system".to_string(),
            content: "你是一位专业的网文小说创作顾问，擅长帮助作者突破卡文困境。请提供多个不同方向的续写方案。".to_string(),
        },
        crate::llm::ChatMessage {
            role: "user".to_string(),
            content: prompt,
        },
    ];
    
    let result = client.chat(messages).await?;
    
    // 显示 token 统计
    println!("{}", format!("本次调用 - 输入: {} tokens, 输出: {} tokens, 总计: {} tokens",
        result.usage.prompt_tokens,
        result.usage.completion_tokens,
        result.usage.total()
    ).dimmed());
    
    println!("{}", "─".repeat(40).cyan());
    println!("{}", result.content);
    println!("{}", "─".repeat(40).cyan());
    
    Ok(())
}

/// 上下文压缩
async fn compress_context(workspace: &Workspace, client: &LlmClient) -> Result<()> {
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
    
    // 显示 token 统计
    println!("{}", format!("本次调用 - 输入: {} tokens, 输出: {} tokens, 总计: {} tokens",
        result.usage.prompt_tokens,
        result.usage.completion_tokens,
        result.usage.total()
    ).dimmed());
    
    println!("{}", "─".repeat(40).cyan());
    println!("{}", result.content);
    println!("{}", "─".repeat(40).cyan());
    println!("{}", format!("压缩完成: {} 章 → {} 字", recent.len(), result.content.len()).green());
    
    Ok(())
}

/// 显示 Token 统计信息
fn show_token_stats(client: &LlmClient) {
    println!("{}", "Token 使用统计:".green().bold());
    println!("  总输入 tokens: {}", client.tracker.total_prompt);
    println!("  总输出 tokens: {}", client.tracker.total_completion);
    println!("  总 tokens: {}", client.tracker.total_tokens());
    println!("  调用次数: {}", client.tracker.call_count);
    println!();
}

/// Wiki 健康检查
fn check_wiki_health(workspace: &Workspace) {
    use crate::wiki::Wiki;
    
    let wiki_path = workspace.root().join("wiki");
    let _wiki = Wiki::new(wiki_path);
    
    println!("{}", "Wiki 健康检查:".green().bold());
    
    // 检查实体页面
    let entities_dir = workspace.root().join("wiki/entities");
    if entities_dir.exists() {
        let entities: Vec<_> = std::fs::read_dir(&entities_dir)
            .unwrap()
            .filter_map(|e| e.ok())
            .filter(|e| e.path().extension().and_then(|s| s.to_str()) == Some("md"))
            .collect();
        println!("  实体页面: {} 个", entities.len());
    } else {
        println!("  实体页面: 0 个");
    }
    
    // 检查素材库
    let materials_path = workspace.root().join("vector_store/materials.json");
    if materials_path.exists() {
        if let Ok(content) = std::fs::read_to_string(&materials_path) {
            if let Ok(materials) = serde_json::from_str::<Vec<serde_json::Value>>(&content) {
                println!("  素材数量: {} 条", materials.len());
            }
        }
    }
    
    // 检查记忆树
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
    println!("  /help          - 显示帮助信息");
    println!("  /quit          - 退出程序");
    println!("  /list          - 列出所有小说");
    println!("  /use <name>    - 切换到指定小说");
    println!("  /new <name>    - 创建新小说");
    println!("  /continue      - 续写章节");
    println!("  /fix           - 卡文修复");
    println!("  /context       - 显示上下文统计");
    println!("  /compress      - 手动触发压缩");
    println!("  /wiki-health   - Wiki 健康检查");
    println!("  /stats         - Token 使用统计");
    println!();
}
