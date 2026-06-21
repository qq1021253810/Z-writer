//! Agent 基础定义

use async_trait::async_trait;
use crate::error::Result;

/// Agent 上下文
pub struct AgentContext {
    /// 当前小说工作区路径
    pub workspace_path: std::path::PathBuf,
    
    /// 用户输入
    pub user_input: String,
    
    /// 系统提示词
    pub system_prompt: String,
}

/// Agent 执行结果
pub struct AgentResult {
    /// 生成的内容
    pub content: String,
    
    /// 是否需要用户确认
    pub need_confirm: bool,
    
    /// 附加信息
    pub metadata: std::collections::HashMap<String, String>,
}

/// Agent trait
#[async_trait]
pub trait Agent: Send + Sync {
    /// 执行 Agent 任务
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult>;
    
    /// 流式执行 Agent 任务（实时输出）
    async fn execute_stream<F>(&self, ctx: &AgentContext, on_chunk: F) -> Result<AgentResult>
    where
        F: FnMut(&str) + Send;
    
    /// 获取系统提示词
    fn system_prompt(&self) -> String;
    
    /// 获取 Agent 名称
    fn name(&self) -> &str;
}
