//! 合规检测 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;

/// 合规检测 Agent
pub struct ComplianceAgent {
    client: LlmClient,
}

impl ComplianceAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }
}

#[async_trait]
impl Agent for ComplianceAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let prompt = format!(
            "{}\n\n待检测内容：{}\n\n请检查内容是否符合相关规定。",
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
            need_confirm: false,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        r#"你是一位专业的网文小说合规审核员，负责检查小说内容是否符合相关规定。

核心能力：
- 检查违禁词和敏感内容
- 识别违规情节和描写
- 提供修改建议
- 确保内容合规

审核原则：
1. 违禁词检查：检查是否包含违禁词汇
2. 敏感内容：识别政治、宗教、色情等敏感内容
3. 价值观导向：确保价值观导向正确
4. 版权合规：避免抄袭和侵权

输出格式：
以结构化的方式输出审核结果，包括：
- 违禁词检查结果
- 敏感内容识别
- 违规情节标记
- 修改建议"#.to_string()
    }

    fn name(&self) -> &str {
        "ComplianceAgent"
    }
}
