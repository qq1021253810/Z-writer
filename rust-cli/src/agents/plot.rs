//! 剧情策划 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;

/// 剧情策划 Agent
pub struct PlotAgent {
    client: LlmClient,
}

impl PlotAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }
}

#[async_trait]
impl Agent for PlotAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let prompt = format!(
            "{}\n\n用户输入：{}\n\n请根据用户需求，设计爽点密集、节奏合理的剧情。",
            self.system_prompt(),
            ctx.user_input
        );

        let messages = vec![
            crate::llm::ChatMessage {
                role: "system".to_string(),
                content: self.system_prompt(),
            },
            crate::llm::ChatMessage {
                role: "user".to_string(),
                content: prompt,
            },
        ];

        let content = self.client.chat(messages).await?;

        Ok(AgentResult {
            content,
            need_confirm: true,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        r#"你是一位专业的网文小说剧情策划师，擅长设计爽点密集、节奏合理的剧情。

核心能力：
- 设计爽点密集的剧情
- 控制合理的剧情节奏
- 构建完整的剧情弧线
- 制造悬念和期待感

设计原则：
1. 爽点密集：每章至少一个爽点或情感点
2. 节奏合理：张弛有度，避免平铺直叙
3. 悬念设置：每章结尾留有悬念
4. 逻辑自洽：剧情发展符合逻辑

输出格式：
以结构化的方式输出剧情设计，包括：
- 剧情概述
- 爽点设计
- 节奏安排
- 悬念设置
- 伏笔埋设"#.to_string()
    }

    fn name(&self) -> &str {
        "PlotAgent"
    }
}
