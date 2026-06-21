// 百炼 DashScope LLM 集成测试

use zwriter_cli::config::{AppConfig, LlmProvider, DashScopeConfig};
use zwriter_cli::llm::{LlmClient, ChatMessage};

/// 创建百炼测试配置
fn create_dashscope_config() -> AppConfig {
    // 从环境变量读取 API Key（CI/CD 环境）
    let api_key = std::env::var("DASHSCOPE_API_KEY")
        .unwrap_or_else(|_| {
            // 尝试从本地配置文件读取
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
async fn test_dashscope_chat() {
    let config = create_dashscope_config();
    
    if config.dashscope.api_key.is_empty() {
        println!("⚠️  跳过百炼测试：未配置 API Key");
        println!("   请设置环境变量 DASHSCOPE_API_KEY 或创建 ./.dashscope_key 文件");
        return;
    }
    
    let client = LlmClient::new(&config);
    
    let messages = vec![
        ChatMessage {
            role: "user".to_string(),
            content: "请用一句话介绍自己".to_string(),
        },
    ];
    
    let result = client.chat(messages).await;
    assert!(result.is_ok(), "百炼调用失败: {:?}", result.err());
    let response = result.unwrap();
    assert!(!response.content.is_empty(), "百炼返回空内容");
    let preview = safe_truncate(&response.content, 200);
    println!("✅ 百炼响应: {}", preview);
}

#[tokio::test]
async fn test_dashscope_embed() {
    let config = create_dashscope_config();
    
    if config.dashscope.api_key.is_empty() {
        println!("⚠️  跳过百炼嵌入测试：未配置 API Key");
        return;
    }
    
    let client = LlmClient::new(&config);
    
    let result = client.embed("测试文本").await;
    assert!(result.is_ok(), "百炼嵌入失败: {:?}", result.err());
    let embedding = result.unwrap();
    assert!(!embedding.is_empty(), "百炼嵌入返回空向量");
    println!("✅ 百炼嵌入维度: {}", embedding.len());
}

#[tokio::test]
async fn test_dashscope_novel_generation() {
    let config = create_dashscope_config();
    
    if config.dashscope.api_key.is_empty() {
        println!("⚠️  跳过百炼小说生成测试：未配置 API Key");
        return;
    }
    
    let client = LlmClient::new(&config);
    
    let messages = vec![
        ChatMessage {
            role: "system".to_string(),
            content: "你是一位专业的网文小说作家。".to_string(),
        },
        ChatMessage {
            role: "user".to_string(),
            content: "请写一段 100 字左右的都市商战小说开头，主角名叫陆远。".to_string(),
        },
    ];
    
    let result = client.chat(messages).await;
    assert!(result.is_ok(), "百炼小说生成失败: {:?}", result.err());
    let response = result.unwrap();
    assert!(response.content.len() >= 50, "生成内容过短");
    let preview = safe_truncate(&response.content, 300);
    println!("✅ 百炼小说生成:\n{}", preview);
}

/// 安全截断 UTF-8 字符串，不破坏字符边界
fn safe_truncate(s: &str, max_bytes: usize) -> &str {
    if s.len() <= max_bytes {
        return s;
    }
    match s.char_indices().nth(max_bytes / 4).map(|(i, _)| i) {
        Some(i) if i <= max_bytes => &s[..i],
        _ => &s[..max_bytes],
    }
}
