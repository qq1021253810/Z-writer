//! 角色塑造 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;

/// 角色塑造 Agent
pub struct CharacterAgent {
    client: LlmClient,
}

impl CharacterAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 构建聊天消息
    fn build_messages(&self, ctx: &AgentContext) -> Vec<crate::llm::ChatMessage> {
        let prompt = format!(
            "用户输入：{}\n\n请根据用户需求，设计鲜活、立体的角色形象。",
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
impl Agent for CharacterAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let messages = self.build_messages(ctx);
        let result = self.client.chat(messages).await?;
        Ok(AgentResult {
            content: result.content,
            need_confirm: true,
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
            need_confirm: true,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        r#"你是一位专业的网文小说人物塑造师，擅长创造高智商、高情商、权谋博弈、规则利用的高格局都市文角色。

核心能力：
- 设计鲜明的角色性格和决策风格
- 构建合理的角色关系网络（合作伙伴、竞争对手、导师、下属）
- 规划角色成长弧线（从初始状态到最终状态的变化）
- 创作展现高智商和高情商的人物对话

设计原则：
1. 性格鲜明：每个角色有独特的决策风格（激进型、稳健型、谋略型）
2. 动机明确：角色行为有合理的动机和利益驱动
3. 成长弧线：角色有清晰的成长轨迹，不只是能力提升，更是眼界和格局的拓展
4. 关系网络：角色之间形成复杂的利益关系网（合作、竞争、联盟、背叛）

输出格式：
以结构化的方式输出角色设定，包括：
- 基本信息（姓名、年龄、身份、背景）
- 性格特征（决策风格、情商水平、价值观）
- 核心优势（决策能力、资源网络、专业技能、人脉关系）
- 人物关系（合作伙伴、竞争对手、导师、下属、家人）
- 成长弧线（从初始状态到最终状态的变化）"#.to_string()
    }

    fn name(&self) -> &str {
        "CharacterAgent"
    }
}
