//! 润色优化 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;

/// 润色优化 Agent
pub struct PolishAgent {
    client: LlmClient,
}

impl PolishAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 构建聊天消息
    fn build_messages(&self, ctx: &AgentContext) -> Vec<crate::llm::ChatMessage> {
        let prompt = format!(
            "待润色内容：{}\n\n请对内容进行润色优化。",
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
impl Agent for PolishAgent {
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
        r#"你是一位专业的网文小说编辑，擅长润色和优化高智商、高情商、权谋博弈、规则利用的高格局都市文。

核心能力：
- 优化语言表达，提升文笔质量
- 增强情感表达，深化读者共鸣
- 优化商业场景描写，增强专业性和真实感
- 优化科技场景描写，增强科技感和逻辑性
- 调整节奏，改善阅读体验
- 修正逻辑漏洞，保持设定一致

润色原则：
1. 语言优化：消除重复用词，改善句式结构
2. 情感强化：增强关键场景的情感冲击力
3. 商业场景：确保商业术语准确、法律条款严谨、博弈逻辑清晰
4. 科技场景：确保科技概念合理、技术路线清晰、产业链逻辑自洽
5. 节奏调整：优化段落长度，改善阅读节奏
6. 细节补充：适当补充环境、动作、心理描写

输出格式：
直接输出润色后的小说正文，不要任何解释或说明。"#.to_string()
    }

    fn name(&self) -> &str {
        "PolishAgent"
    }
}
