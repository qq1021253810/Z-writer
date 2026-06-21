//! 错误处理模块

use thiserror::Error;

#[derive(Error, Debug)]
pub enum AppError {
    #[error("IO 错误: {0}")]
    Io(#[from] std::io::Error),

    #[error("JSON 序列化错误: {0}")]
    Json(#[from] serde_json::Error),

    #[error("HTTP 请求错误: {0}")]
    Http(#[from] reqwest::Error),

    #[error("TOML 解析错误: {0}")]
    Toml(#[from] toml::de::Error),

    #[error("Readline 错误: {0}")]
    Readline(#[from] rustyline::error::ReadlineError),

    #[error("配置错误: {0}")]
    Config(String),

    #[error("LLM 调用错误: {0}")]
    Llm(String),

    #[error("工作区错误: {0}")]
    Workspace(String),

    #[error("Agent 执行错误: {0}")]
    Agent(String),

    #[error("RAG 存储错误: {0}")]
    Storage(String),

    #[error("Git 错误: {0}")]
    Git(String),

    #[error("上下文错误: {0}")]
    Context(String),
}

pub type Result<T> = std::result::Result<T, AppError>;
