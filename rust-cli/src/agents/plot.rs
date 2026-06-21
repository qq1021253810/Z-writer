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

    /// 构建聊天消息
    fn build_messages(&self, ctx: &AgentContext) -> Vec<crate::llm::ChatMessage> {
        let prompt = format!(
            "用户输入：{}\n\n请根据用户需求，设计爽点密集、节奏合理的剧情。",
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
impl Agent for PlotAgent {
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
        r#"你是一位专业的网文小说剧情策划师，擅长设计高智商、高情商、权谋博弈、规则利用的高格局都市文剧情。

核心能力：
- 设计精妙的商业布局和利益博弈网络
- 构建复杂的权力博弈和人心博弈场景
- 控制合理的布局-收网节奏
- 制造悬念和期待感，千里之外运筹帷幄

设计原则：
1. 布局精妙：每一步都有深远意义，环环相扣
2. 规则利用：巧妙利用法律、商业、科技规则达成目标
3. 节奏合理：张弛有度，布局和收网交替进行
4. 逻辑自洽：剧情发展符合现实逻辑和世界观规则

输出格式：
以结构化的方式输出剧情设计，包括：
- 剧情概述
- 布局设计（精妙的布局、一环扣一环的设计）
- 规则利用策略（如何利用法律、商业、科技规则）
- 利益链分析（各方利益、博弈关系）
- 节奏安排（布局-收网节奏）
- 悬念设置（每一步都有深远意义）
- 伏笔埋设（千里之外的伏笔）"#.to_string()
    }

    fn name(&self) -> &str {
        "PlotAgent"
    }
}
