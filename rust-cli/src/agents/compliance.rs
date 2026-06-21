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

    /// 构建聊天消息
    fn build_messages(&self, ctx: &AgentContext) -> Vec<crate::llm::ChatMessage> {
        let prompt = format!(
            "待检测内容：{}\n\n请检查内容是否符合相关规定。",
            ctx.user_input
        );

        vec![
            crate::llm::ChatMessage {
                role: "system".to_string(),
                content: self.system_prompt(),
            },
            crate::llm::ChatMessage {
                role: "user".to_string(),
                content: prompt,
            },
        ]
    }
}

#[async_trait]
impl Agent for ComplianceAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let messages = self.build_messages(ctx);
        let result = self.client.chat(messages).await?;
        Ok(AgentResult {
            content: result.content,
            need_confirm: false,
            metadata: std::collections::HashMap::new(),
        })
    }

    async fn execute_stream<F>(&self, ctx: &AgentContext, mut on_chunk: F) -> Result<AgentResult>
    where
        F: FnMut(&str) + Send,
    {
        let messages = self.build_messages(ctx);
        let result = self.client.chat_stream(messages, |chunk| on_chunk(chunk)).await?;
        Ok(AgentResult {
            content: result.content,
            need_confirm: false,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        r#"你是一位专业的网文小说合规审核员，负责检查小说内容是否符合相关规定，确保商业行为和法律场景的合规性。

核心能力：
- 检查违禁词和敏感内容
- 识别违规情节和描写
- 检查商业行为是否合规（公司法、证券法、反垄断法、知识产权法）
- 确保内容合规

审核原则：
1. 违禁词检查：检查是否包含违禁词汇
2. 敏感内容：识别政治、宗教、色情等敏感内容
3. 价值观导向：确保价值观导向正确，展现积极向上的商业精神
4. 商业合规：确保商业行为符合现实法律（公司法、证券法、反垄断法）
5. 版权合规：避免抄袭和侵权

输出格式：
以结构化的方式输出审核结果，包括：
- 违禁词检查结果
- 敏感内容识别
- 商业合规检查（法律适用性、合规性评估）
- 违规情节标记
- 修改建议"#.to_string()
    }

    fn name(&self) -> &str {
        "ComplianceAgent"
    }
}
