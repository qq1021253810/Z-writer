// 端到端集成测试 - 创建小说 → 续写章节 → 验证（使用百炼云端模型）

use zwriter_cli::config::{AppConfig, LlmProvider, DashScopeConfig};
use zwriter_cli::workspace::Workspace;
use zwriter_cli::llm::{LlmClient, ChatMessage};
use std::path::PathBuf;

fn e2e_test_path() -> PathBuf {
    PathBuf::from("./e2e_test_ws")
}

fn cleanup_e2e() {
    let _ = std::fs::remove_dir_all(e2e_test_path());
}

fn create_dashscope_config() -> AppConfig {
    let api_key = std::env::var("DASHSCOPE_API_KEY")
        .unwrap_or_else(|_| {
            std::fs::read_to_string("./.dashscope_key")
                .unwrap_or_default()
                .trim()
                .to_string()
        });
    
    AppConfig {
        provider: LlmProvider::Dashscope,
        ollama_url: "http://localhost:11434".to_string(),
        chat_model: "qwen3:14b".to_string(),
        embed_model: "nomic-embed-text".to_string(),
        workspace_path: "./test_workspaces".into(),
        token_budget: 100_000,
        dashscope: DashScopeConfig {
            api_key,
            base_url: "https://dashscope.aliyuncs.com/compatible-mode".to_string(),
            model: "qwen-plus".to_string(),
        },
    }
}

#[tokio::test]
async fn test_e2e_create_and_continue() {
    cleanup_e2e();
    let base = e2e_test_path();
    let config = create_dashscope_config();
    
    // 检查 API Key 是否配置
    if config.dashscope.api_key.is_empty() {
        println!("⚠️  跳过 e2e 测试：未配置百炼 API Key");
        println!("   请设置环境变量 DASHSCOPE_API_KEY 或创建 ./.dashscope_key 文件");
        cleanup_e2e();
        return;
    }
    
    // 1. 创建小说工作区
    let ws = Workspace::create(&base, "商战测试").unwrap();
    assert!(ws.root().join("novel.md").exists());
    assert!(ws.root().join("characters").exists());
    assert!(ws.root().join("chapters").exists());
    
    // 2. 写入小说设定
    std::fs::write(
        ws.root().join("novel.md"),
        "# 商战测试\n\n- 类型: 都市商战\n- 状态: 创作中\n- 简介: 陆远凭借超前技术眼光，从零开始构建科技商业帝国\n",
    ).unwrap();
    
    // 3. 写入世界观
    std::fs::write(
        ws.root().join("worldview.md"),
        "# 世界观设定\n\n规则体系：公司法、证券法、反垄断法、知识产权法\n主要势力：星辰科技、天虎集团、国家基金\n",
    ).unwrap();
    
    // 4. 写入角色
    std::fs::write(
        ws.root().join("characters").join("陆远.md"),
        "# 陆远\n\n- 身份: 星辰科技CEO\n- 年龄: 32\n- 决策风格: 谋略型\n- 核心优势: 超前技术眼光、全产业链思维\n",
    ).unwrap();
    
    // 5. 写入大纲
    std::fs::write(
        ws.root().join("outline.md"),
        "# 大纲\n\n第一卷：创业起步\n- 第1章：陆远发现行业痛点，决定创业\n",
    ).unwrap();
    
    // 6. 验证工作区状态
    assert_eq!(ws.next_chapter_num().unwrap(), 1);
    let info = ws.novel_info().unwrap();
    assert!(info.contains("商战测试"));
    let chars = ws.characters().unwrap();
    assert_eq!(chars.len(), 1);
    
    // 7. 调用百炼 LLM 续写第 1 章
    let client = LlmClient::new(&config);
    let mut prompt = String::new();
    prompt.push_str("【小说信息】\n");
    prompt.push_str(&ws.novel_info().unwrap());
    prompt.push_str("\n【世界观】\n");
    prompt.push_str(&ws.worldview().unwrap());
    prompt.push_str("\n【大纲】\n");
    prompt.push_str(&ws.outline().unwrap());
    prompt.push_str("\n【角色设定】\n");
    for c in &ws.characters().unwrap() {
        prompt.push_str(c);
        prompt.push_str("\n");
    }
    prompt.push_str("\n请续写第 1 章，保持与前文连贯，字数约 500 字。\n");
    
    let messages = vec![
        ChatMessage {
            role: "system".to_string(),
            content: "你是一位专业的网文小说作家。请根据设定续写章节，控制在500字以内。".to_string(),
        },
        ChatMessage {
            role: "user".to_string(),
            content: prompt,
        },
    ];
    
    let response = client.chat(messages).await.unwrap();
    assert!(!response.content.is_empty());
    
    // 8. 保存章节
    ws.save_chapter(1, &response.content).unwrap();
    assert_eq!(ws.next_chapter_num().unwrap(), 2);
    
    // 9. 验证章节已保存
    let saved = ws.read_chapter(1).unwrap();
    assert!(saved.is_some());
    assert!(!saved.unwrap().is_empty());
    
    // 10. 验证最近章节
    let recent = ws.recent_chapters(3).unwrap();
    assert_eq!(recent.len(), 1);
    assert_eq!(recent[0].0, 1);
    
    println!("✅ 端到端测试通过！");
    println!("   - 小说创建: OK");
    println!("   - 设定写入: OK");
    println!("   - 百炼 LLM 续写: OK ({} 字)", response.content.len());
    println!("   - 章节保存: OK");
    println!("   - 章节读取: OK");
    
    cleanup_e2e();
}
