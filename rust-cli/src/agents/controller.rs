//! 总控 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;

/// 总控 Agent
pub struct ControllerAgent {
    client: LlmClient,
}

impl ControllerAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 意图识别：根据用户输入判断应该路由到哪个子 Agent
    pub async fn route_task(&self, ctx: &AgentContext) -> Result<String> {
        let prompt = format!(
            "根据用户输入，判断应该由哪个子 Agent 处理。可选 Agent：\n\
             - WritingAgent: 正文写作、续写章节\n\
             - WorldOutlineAgent: 世界观设定、大纲规划\n\
             - CharacterAgent: 角色塑造、人物设定\n\
             - PlotAgent: 剧情策划、节奏控制\n\
             - ComplianceAgent: 合规检测、内容审查\n\
             - PolishAgent: 润色优化、文笔提升\n\n\
             用户输入：{}\n\n\
             请直接返回 Agent 名称（如 WritingAgent），不要其他内容。",
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

        let response = self.client.chat(messages).await?;
        Ok(response.trim().to_string())
    }
}

#[async_trait]
impl Agent for ControllerAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let target_agent = self.route_task(ctx).await?;
        
        let metadata = std::collections::HashMap::new();
        let mut result_metadata = metadata;
        result_metadata.insert("routed_to".to_string(), target_agent.clone());

        Ok(AgentResult {
            content: format!("已路由到: {}", target_agent),
            need_confirm: false,
            metadata: result_metadata,
        })
    }

    fn system_prompt(&self) -> String {
        "你是 Z-Writer 总控 Agent，负责协调各个子 Agent 完成小说创作任务。\
         你的职责是：\n\
         1. 理解用户意图\n\
         2. 将任务路由到合适的子 Agent\n\
         3. 协调多个 Agent 的协作\n\
         4. 确保任务完成的连贯性".to_string()
    }

    fn name(&self) -> &str {
        "ControllerAgent"
    }
}
