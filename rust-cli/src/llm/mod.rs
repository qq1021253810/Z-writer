//! LLM 集成模块

use crate::config::{AppConfig, LlmProvider};
use crate::error::{AppError, Result};
use reqwest::Client;
use serde::{Deserialize, Serialize};
use std::sync::atomic::{AtomicU64, Ordering};
use std::sync::Arc;

/// Token 使用统计
#[derive(Debug, Clone, Default)]
pub struct TokenUsage {
    pub prompt_tokens: u64,
    pub completion_tokens: u64,
}

impl TokenUsage {
    pub fn total(&self) -> u64 {
        self.prompt_tokens + self.completion_tokens
    }
}

/// 全局 Token 统计追踪器
#[derive(Clone, Default)]
pub struct TokenTracker {
    inner: Arc<TokenTrackerInner>,
}

struct TokenTrackerInner {
    total_prompt: AtomicU64,
    total_completion: AtomicU64,
    call_count: AtomicU64,
}

impl Default for TokenTrackerInner {
    fn default() -> Self {
        Self {
            total_prompt: AtomicU64::new(0),
            total_completion: AtomicU64::new(0),
            call_count: AtomicU64::new(0),
        }
    }
}

impl TokenTracker {
    pub fn record(&self, usage: &TokenUsage) {
        self.inner.total_prompt.fetch_add(usage.prompt_tokens, Ordering::Relaxed);
        self.inner.total_completion.fetch_add(usage.completion_tokens, Ordering::Relaxed);
        self.inner.call_count.fetch_add(1, Ordering::Relaxed);
    }

    pub fn total_prompt(&self) -> u64 {
        self.inner.total_prompt.load(Ordering::Relaxed)
    }

    pub fn total_completion(&self) -> u64 {
        self.inner.total_completion.load(Ordering::Relaxed)
    }

    pub fn total_tokens(&self) -> u64 {
        self.total_prompt() + self.total_completion()
    }

    pub fn call_count(&self) -> u64 {
        self.inner.call_count.load(Ordering::Relaxed)
    }

    pub fn summary(&self) -> String {
        format!(
            "Token 统计: 输入 {} | 输出 {} | 总计 {} | 调用 {} 次",
            self.total_prompt(),
            self.total_completion(),
            self.total_tokens(),
            self.call_count()
        )
    }
}

/// Ollama 客户端
#[derive(Clone)]
pub struct OllamaClient {
    base_url: String,
    chat_model: String,
    embed_model: String,
    client: Client,
}

/// DashScope (百炼) 客户端 - 通过 OpenAI 兼容接口调用
#[derive(Clone)]
pub struct DashScopeClient {
    base_url: String,
    api_key: String,
    model: String,
    client: Client,
}

/// 统一 LLM 客户端 - 根据配置自动选择提供商
#[derive(Clone)]
pub struct LlmClient {
    inner: LlmClientInner,
    pub tracker: TokenTracker,
}

#[derive(Clone)]
enum LlmClientInner {
    DashScope(DashScopeClient),
    Ollama(OllamaClient),
}

/// 聊天消息
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChatMessage {
    pub role: String,
    pub content: String,
}

/// 聊天结果（包含内容和 token 统计）
#[derive(Debug)]
pub struct ChatResult {
    pub content: String,
    pub usage: TokenUsage,
}

/// 聊天请求 (OpenAI 兼容格式)
#[derive(Debug, Serialize)]
struct OpenAIChatRequest {
    model: String,
    messages: Vec<ChatMessage>,
    stream: bool,
}

/// 聊天响应 (OpenAI 兼容格式)
#[derive(Debug, Deserialize)]
struct OpenAIChatResponse {
    choices: Vec<OpenAIChoice>,
    #[serde(default)]
    usage: Option<OpenAIUsage>,
}

#[derive(Debug, Deserialize)]
struct OpenAIChoice {
    message: ChatMessage,
}

/// OpenAI 格式的 token 统计
#[derive(Debug, Deserialize)]
struct OpenAIUsage {
    #[serde(default)]
    prompt_tokens: u64,
    #[serde(default)]
    completion_tokens: u64,
}

/// Ollama 原生格式的聊天响应（用于获取 token 统计）
#[derive(Debug, Deserialize)]
struct OllamaNativeChatResponse {
    #[serde(default)]
    message: Option<ChatMessage>,
    #[serde(default)]
    prompt_eval_count: u64,
    #[serde(default)]
    eval_count: u64,
}

/// 嵌入请求 (Ollama 格式)
#[derive(Debug, Serialize)]
struct OllamaEmbedRequest {
    model: String,
    prompt: String,
}

/// 嵌入请求 (OpenAI/DashScope 格式)
#[derive(Debug, Serialize)]
#[allow(dead_code)]
struct OpenAIEmbedRequest {
    model: String,
    input: OpenAIEmbedInput,
}

#[derive(Debug, Serialize)]
#[allow(dead_code)]
struct OpenAIEmbedInput {
    texts: Vec<String>,
}

/// 嵌入响应 (Ollama 格式)
#[derive(Debug, Deserialize)]
struct OllamaEmbedResponse {
    embedding: Vec<f32>,
}

/// 嵌入响应 (OpenAI/DashScope 格式)
#[derive(Debug, Deserialize)]
struct OpenAIEmbedResponse {
    data: Vec<OpenAIEmbedData>,
}

#[derive(Debug, Deserialize)]
struct OpenAIEmbedData {
    embedding: Vec<f32>,
}

impl LlmClient {
    /// 根据配置创建 LLM 客户端
    pub fn new(config: &AppConfig) -> Self {
        let inner = match config.provider {
            LlmProvider::Dashscope => {
                tracing::info!("[LLM] 使用百炼 DashScope，模型: {}", config.dashscope.model);
                LlmClientInner::DashScope(DashScopeClient::new(config))
            }
            LlmProvider::Ollama => {
                tracing::info!("[LLM] 使用本地 Ollama，模型: {}", config.chat_model);
                LlmClientInner::Ollama(OllamaClient::new(config))
            }
        };
        Self {
            inner,
            tracker: TokenTracker::default(),
        }
    }

    /// 聊天调用（返回内容和 token 统计）
    pub async fn chat(&self, messages: Vec<ChatMessage>) -> Result<ChatResult> {
        let result = match &self.inner {
            LlmClientInner::DashScope(c) => c.chat(messages).await,
            LlmClientInner::Ollama(c) => c.chat(messages).await,
        }?;
        self.tracker.record(&result.usage);
        Ok(result)
    }

    pub async fn embed(&self, text: &str) -> Result<Vec<f32>> {
        match &self.inner {
            LlmClientInner::DashScope(c) => c.embed(text).await,
            LlmClientInner::Ollama(c) => c.embed(text).await,
        }
    }
}

impl OllamaClient {
    pub fn new(config: &AppConfig) -> Self {
        Self {
            base_url: config.ollama_url.clone(),
            chat_model: config.chat_model.clone(),
            embed_model: config.embed_model.clone(),
            client: Client::new(),
        }
    }

    /// 聊天调用
    pub async fn chat(&self, messages: Vec<ChatMessage>) -> Result<ChatResult> {
        let request = OpenAIChatRequest {
            model: self.chat_model.clone(),
            messages,
            stream: false,
        };

        let response = self.client
            .post(format!("{}/api/chat", self.base_url))
            .json(&request)
            .send()
            .await
            .map_err(|e| AppError::Llm(format!("HTTP 请求失败: {}", e)))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(AppError::Llm(format!("LLM 返回错误 {}: {}", status, body)));
        }

        let chat_response: OpenAIChatResponse = response.json().await
            .map_err(|e| AppError::Llm(format!("解析响应失败: {}", e)))?;

        let content = chat_response.choices[0].message.content.clone();
        let usage = chat_response.usage
            .map(|u| TokenUsage {
                prompt_tokens: u.prompt_tokens,
                completion_tokens: u.completion_tokens,
            })
            .unwrap_or_else(|| {
                // 如果 OpenAI 兼容接口没有返回 usage，估算 token 数
                estimate_tokens(&content)
            });

        Ok(ChatResult { content, usage })
    }

    /// 生成嵌入向量
    pub async fn embed(&self, text: &str) -> Result<Vec<f32>> {
        let request = OllamaEmbedRequest {
            model: self.embed_model.clone(),
            prompt: text.to_string(),
        };

        let response = self.client
            .post(format!("{}/api/embeddings", self.base_url))
            .json(&request)
            .send()
            .await
            .map_err(|e| AppError::Llm(format!("HTTP 请求失败: {}", e)))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(AppError::Llm(format!("LLM 返回错误 {}: {}", status, body)));
        }

        let embed_response: OllamaEmbedResponse = response.json().await
            .map_err(|e| AppError::Llm(format!("解析响应失败: {}", e)))?;

        Ok(embed_response.embedding)
    }
}

impl DashScopeClient {
    pub fn new(config: &AppConfig) -> Self {
        Self {
            base_url: config.dashscope.base_url.clone(),
            api_key: config.dashscope.api_key.clone(),
            model: config.dashscope.model.clone(),
            client: Client::new(),
        }
    }

    /// 聊天调用 - 通过 OpenAI 兼容接口
    pub async fn chat(&self, messages: Vec<ChatMessage>) -> Result<ChatResult> {
        if self.api_key.is_empty() {
            return Err(AppError::Llm("DashScope API Key 未配置，请检查 config.local.toml".to_string()));
        }

        let request = OpenAIChatRequest {
            model: self.model.clone(),
            messages,
            stream: false,
        };

        let response = self.client
            .post(format!("{}/v1/chat/completions", self.base_url))
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await
            .map_err(|e| AppError::Llm(format!("HTTP 请求失败: {}", e)))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(AppError::Llm(format!("DashScope 返回错误 {}: {}", status, body)));
        }

        let chat_response: OpenAIChatResponse = response.json().await
            .map_err(|e| AppError::Llm(format!("解析响应失败: {}", e)))?;

        if chat_response.choices.is_empty() {
            return Err(AppError::Llm("DashScope 返回空响应".to_string()));
        }

        let content = chat_response.choices[0].message.content.clone();
        let usage = chat_response.usage
            .map(|u| TokenUsage {
                prompt_tokens: u.prompt_tokens,
                completion_tokens: u.completion_tokens,
            })
            .unwrap_or_else(|| estimate_tokens(&content));

        Ok(ChatResult { content, usage })
    }

    /// 生成嵌入向量 - 百炼也支持 OpenAI 兼容的 embeddings 接口
    pub async fn embed(&self, text: &str) -> Result<Vec<f32>> {
        if self.api_key.is_empty() {
            return Err(AppError::Llm("DashScope API Key 未配置".to_string()));
        }

        // DashScope 使用简化的请求格式
        let request = serde_json::json!({
            "model": "text-embedding-v3",
            "input": text
        });

        let response = self.client
            .post(format!("{}/v1/embeddings", self.base_url))
            .header("Authorization", format!("Bearer {}", self.api_key))
            .header("Content-Type", "application/json")
            .json(&request)
            .send()
            .await
            .map_err(|e| AppError::Llm(format!("HTTP 请求失败: {}", e)))?;

        if !response.status().is_success() {
            let status = response.status();
            let body = response.text().await.unwrap_or_default();
            return Err(AppError::Llm(format!("DashScope 返回错误 {}: {}", status, body)));
        }

        let embed_response: OpenAIEmbedResponse = response.json().await
            .map_err(|e| AppError::Llm(format!("解析响应失败: {}", e)))?;

        embed_response.data
            .into_iter()
            .next()
            .map(|d| d.embedding)
            .ok_or_else(|| AppError::Llm("DashScope 返回空嵌入向量".to_string()))
    }
}

/// 估算 token 数量（中文约 1.5 字/token，英文约 4 字符/token）
fn estimate_tokens(text: &str) -> TokenUsage {
    let mut chinese_chars = 0u64;
    let mut other_chars = 0u64;
    
    for ch in text.chars() {
        if ch >= '\u{4e00}' && ch <= '\u{9fff}' {
            chinese_chars += 1;
        } else if ch.is_alphanumeric() || ch.is_whitespace() {
            other_chars += 1;
        }
    }
    
    // 中文：约 1.5 字/token；英文：约 4 字符/token
    let estimated = (chinese_chars as f64 / 1.5) + (other_chars as f64 / 4.0);
    
    TokenUsage {
        prompt_tokens: (estimated * 0.7) as u64, // 假设输入占 70%
        completion_tokens: (estimated * 0.3) as u64, // 输出占 30%
    }
}
