//! 世界观与大纲 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;
use crate::wiki::Wiki;
use crate::workspace::Workspace;

/// 世界观与大纲 Agent
pub struct WorldOutlineAgent {
    client: LlmClient,
}

impl WorldOutlineAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 构建世界观上下文
    fn build_context(&self, workspace: &Workspace, wiki: &Wiki) -> Result<String> {
        let mut context = workspace.build_full_context(0)?;
        
        let rules = wiki.load_rules()?;
        if !rules.is_empty() {
            context.push_str("【写作规则】\n");
            context.push_str(&rules);
            context.push_str("\n\n");
        }

        Ok(context)
    }

    /// 构建聊天消息
    fn build_messages(&self, ctx: &AgentContext) -> Result<Vec<crate::llm::ChatMessage>> {
        let workspace = Workspace::open(ctx.workspace_path.clone())?;
        let wiki = Wiki::new(workspace.root().join("wiki"));
        let base_context = self.build_context(&workspace, &wiki)?;

        let prompt = format!(
            "{base_context}\n\
             【用户请求】\n{input}\n\n\
             请根据小说信息和用户需求，设计完整的世界观和大纲。要求：\n\
             1. 规则体系完整，商业规则、法律规则、科技规则相互关联\n\
             2. 势力博弈网络清晰，各势力之间有明确的利益关系和竞争联盟\n\
             3. 科技树/产业链布局合理，支持商业闭环设计\n\
             4. 有独特的世界观亮点，区别于同类小说\n\
             5. 为后续剧情留有发展空间\n\n\
             输出格式：\n\
             ## 规则体系\n（商业规则、法律规则、科技规则、行业规则）\n\
             ## 势力博弈网络\n（主要势力、利益链、竞争关系、合作联盟）\n\
             ## 科技树/产业链\n（关键技术、产业链布局、商业闭环）\n\
             ## 特色亮点\n（区别于同类小说的独特设定）",
            input = ctx.user_input
        );

        Ok(vec![
            crate::llm::ChatMessage {
                role: "system".to_string(),
                content: self.system_prompt(),
            },
            crate::llm::ChatMessage {
                role: "user".to_string(),
                content: prompt,
            },
        ])
    }
}

#[async_trait]
impl Agent for WorldOutlineAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let messages = self.build_messages(ctx)?;
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
        let messages = self.build_messages(ctx)?;
        let result = self.client.chat_stream(messages, |chunk| on_chunk(chunk)).await?;
        Ok(AgentResult {
            content: result.content,
            need_confirm: true,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        r#"你是一位专业的网文小说世界观架构师，擅长构建高智商、高情商、权谋博弈、规则利用的高格局都市文世界观。

核心能力：
- 构建完整的规则体系（商业规则、法律规则、科技规则、行业规则）
- 设计复杂的势力博弈网络（利益链、竞争关系、合作联盟）
- 创造独特的科技树/产业链布局（关键技术、产业链、商业闭环）
- 保持设定前后一致，逻辑自洽

设计原则：
1. 体系完整：规则体系、势力网络、科技树相互关联
2. 逻辑自洽：设定之间不矛盾，符合现实逻辑
3. 特色鲜明：有独特的世界观亮点，区别于同类小说
4. 可扩展性：为后续剧情和商业布局留有发展空间

输出要求：以 Markdown 格式输出，结构清晰，层次分明。"#
            .to_string()
    }

    fn name(&self) -> &str {
        "WorldOutlineAgent"
    }
}
