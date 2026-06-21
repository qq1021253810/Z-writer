// LLM 集成测试 - 验证百炼云端模型调用

use zwriter_cli::config::{AppConfig, LlmProvider, DashScopeConfig};
use zwriter_cli::llm::{LlmClient, ChatMessage};

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
async fn test_dashscope_chat() {
    let config = create_dashscope_config();
    
    if config.dashscope.api_key.is_empty() {
        println!("⚠️  跳过测试：未配置百炼 API Key");
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
    println!("百炼响应: {}", preview);
}

fn safe_truncate(s: &str, max_bytes: usize) -> &str {
    if s.len() <= max_bytes {
        return s;
    }
    match s.char_indices().take_while(|&(i, _)| i < max_bytes).last() {
        Some((i, c)) => &s[..i + c.len_utf8()],
        None => "",
    }
}
