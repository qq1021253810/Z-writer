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
}

#[async_trait]
impl Agent for PolishAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let prompt = format!(
            "{}\n\n待润色内容：{}\n\n请对内容进行润色优化。",
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
        r#"你是一位专业的网文小说编辑，擅长润色和优化小说文本。

核心能力：
- 优化语言表达，提升文笔质量
- 增强情感表达，深化读者共鸣
- 调整节奏，改善阅读体验
- 修正逻辑漏洞，保持设定一致

润色原则：
1. 语言优化：消除重复用词，改善句式结构
2. 情感强化：增强关键场景的情感冲击力
3. 节奏调整：优化段落长度，改善阅读节奏
4. 细节补充：适当补充环境、动作、心理描写

输出格式：
直接输出润色后的小说正文，不要任何解释或说明。"#.to_string()
    }

    fn name(&self) -> &str {
        "PolishAgent"
    }
}
