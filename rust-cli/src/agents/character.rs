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
}

#[async_trait]
impl Agent for CharacterAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let prompt = format!(
            "{}\n\n用户输入：{}\n\n请根据用户需求，设计鲜活、立体的角色形象。",
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
        r#"你是一位专业的网文小说人物塑造师，擅长创造鲜活、立体的角色形象。

核心能力：
- 设计鲜明的角色性格
- 构建合理的角色关系
- 规划角色成长弧线
- 创作生动的角色对话

设计原则：
1. 性格鲜明：每个角色有独特的性格特征
2. 动机明确：角色行为有合理的动机
3. 成长弧线：角色有清晰的发展轨迹
4. 关系网络：角色之间形成复杂的关系网

输出格式：
以结构化的方式输出角色设定，包括：
- 基本信息（姓名、年龄、身份）
- 性格特征（优点、缺点、特点）
- 能力设定（实力、技能、金手指）
- 人物关系（与其他角色的关系）
- 成长弧线（从开始到结束的变化）"#.to_string()
    }

    fn name(&self) -> &str {
        "CharacterAgent"
    }
}
