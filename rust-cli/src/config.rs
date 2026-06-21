//! 配置管理模块

use serde::{Deserialize, Serialize};
use std::path::PathBuf;
use crate::error::{AppError, Result};

/// LLM 提供商
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
#[serde(rename_all = "lowercase")]
pub enum LlmProvider {
    Dashscope,
    Ollama,
}

impl Default for LlmProvider {
    fn default() -> Self {
        LlmProvider::Dashscope
    }
}

/// 应用配置
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct AppConfig {
    /// LLM 提供商: "dashscope" 或 "ollama"
    #[serde(default)]
    pub provider: LlmProvider,

    /// Ollama 服务地址
    #[serde(default = "default_ollama_url")]
    pub ollama_url: String,

    /// 默认聊天模型
    #[serde(default = "default_chat_model")]
    pub chat_model: String,

    /// 默认嵌入模型
    #[serde(default = "default_embed_model")]
    pub embed_model: String,

    /// 默认工作区路径
    #[serde(default = "default_workspace_path")]
    pub workspace_path: PathBuf,

    /// Token 预算
    #[serde(default = "default_token_budget")]
    pub token_budget: usize,

    /// 百炼 DashScope 配置
    #[serde(default)]
    pub dashscope: DashScopeConfig,
}

/// 百炼 DashScope 配置
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct DashScopeConfig {
    /// API Key（敏感信息，不提交到仓库）
    #[serde(default)]
    pub api_key: String,

    /// API Base URL
    #[serde(default = "default_dashscope_base_url")]
    pub base_url: String,

    /// 模型名称
    #[serde(default = "default_dashscope_model")]
    pub model: String,
}

impl Default for DashScopeConfig {
    fn default() -> Self {
        Self {
            api_key: String::new(),
            base_url: default_dashscope_base_url(),
            model: default_dashscope_model(),
        }
    }
}

fn default_ollama_url() -> String {
    "http://localhost:11434".to_string()
}

fn default_chat_model() -> String {
    "qwen3:14b".to_string()
}

fn default_embed_model() -> String {
    "nomic-embed-text".to_string()
}

fn default_workspace_path() -> PathBuf {
    PathBuf::from("./workspaces")
}

fn default_token_budget() -> usize {
    100_000
}

fn default_dashscope_base_url() -> String {
    "https://dashscope.aliyuncs.com/compatible-mode".to_string()
}

fn default_dashscope_model() -> String {
    "qwen-plus".to_string()
}

impl Default for AppConfig {
    fn default() -> Self {
        Self {
            provider: LlmProvider::default(),
            ollama_url: default_ollama_url(),
            chat_model: default_chat_model(),
            embed_model: default_embed_model(),
            workspace_path: default_workspace_path(),
            token_budget: default_token_budget(),
            dashscope: DashScopeConfig::default(),
        }
    }
}

impl AppConfig {
    /// 从文件加载配置（支持 config.local.toml 覆盖）
    pub fn load(path: &PathBuf) -> Result<Self> {
        let mut config = if path.exists() {
            let content = std::fs::read_to_string(path)
                .map_err(|e| AppError::Config(format!("读取配置文件失败: {}", e)))?;
            toml::from_str(&content)?
        } else {
            Self::default()
        };

        // 尝试加载本地覆盖配置（含敏感信息，不提交到仓库）
        let local_path = path.with_file_name("config.local.toml");
        if local_path.exists() {
            let local_content = std::fs::read_to_string(&local_path)
                .map_err(|e| AppError::Config(format!("读取本地配置失败: {}", e)))?;
            let local_config: toml::Value = toml::from_str(&local_content)?;
            // 合并本地配置
            if let Some(dashscope) = local_config.get("dashscope") {
                if let Some(api_key) = dashscope.get("api_key").and_then(|v| v.as_str()) {
                    config.dashscope.api_key = api_key.to_string();
                }
                if let Some(base_url) = dashscope.get("base_url").and_then(|v| v.as_str()) {
                    config.dashscope.base_url = base_url.to_string();
                }
                if let Some(model) = dashscope.get("model").and_then(|v| v.as_str()) {
                    config.dashscope.model = model.to_string();
                }
            }
            if let Some(provider) = local_config.get("provider").and_then(|v| v.as_str()) {
                match provider {
                    "ollama" => config.provider = LlmProvider::Ollama,
                    _ => config.provider = LlmProvider::Dashscope,
                }
            }
        }

        Ok(config)
    }

    /// 保存配置到文件
    pub fn save(&self, path: &PathBuf) -> Result<()> {
        let content = toml::to_string_pretty(self)
            .map_err(|e| AppError::Config(format!("序列化配置失败: {}", e)))?;

        std::fs::write(path, content)?;
        Ok(())
    }
}
