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
        let novel_info = workspace.novel_info()?;
        let worldview = workspace.worldview()?;
        let outline = workspace.outline()?;
        let rules = wiki.load_rules()?;

        let mut context = String::new();
        context.push_str("【小说信息】\n");
        context.push_str(&novel_info);
        context.push_str("\n\n");

        if !worldview.is_empty()
            && worldview != "# 世界观设定\n\n待补充\n"
        {
            context.push_str("【已有世界观】\n");
            context.push_str(&worldview);
            context.push_str("\n\n");
        }

        if !outline.is_empty() && outline != "# 大纲\n\n待补充\n" {
            context.push_str("【已有大纲】\n");
            context.push_str(&outline);
            context.push_str("\n\n");
        }

        if !rules.is_empty() {
            context.push_str("【写作规则】\n");
            context.push_str(&rules);
            context.push_str("\n\n");
        }

        Ok(context)
    }
}

#[async_trait]
impl Agent for WorldOutlineAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let workspace = Workspace::open(ctx.workspace_path.clone())?;
        let wiki = Wiki::new(workspace.root().join("wiki"));
        let base_context = self.build_context(&workspace, &wiki)?;

        let prompt = format!(
            "{base_context}\n\
             【用户请求】\n{input}\n\n\
             请根据小说信息和用户需求，设计完整的世界观和大纲。要求：\n\
             1. 修炼体系完整，等级划分清晰\n\
             2. 势力分布合理，各势力之间有明确的关系\n\
             3. 世界规则自洽，有内在逻辑\n\
             4. 有独特的世界观亮点\n\
             5. 为后续剧情留有发展空间\n\n\
             输出格式：\n\
             ## 修炼体系\n（等级划分、突破条件、战力对比）\n\
             ## 势力分布\n（主要势力、势力关系、地盘划分）\n\
             ## 世界规则\n（天地法则、特殊机制、禁忌事项）\n\
             ## 特色亮点\n（区别于同类小说的独特设定）",
            input = ctx.user_input
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
        r#"你是一位专业的网文小说世界观架构师，擅长构建宏大、自洽的世界观体系。

核心能力：
- 构建完整的修炼体系（等级划分、突破条件、战力对比）
- 设计合理的势力分布（主要势力、势力关系、地盘划分）
- 创造独特的世界规则（天地法则、特殊机制、禁忌事项）
- 保持设定前后一致，逻辑自洽

设计原则：
1. 体系完整：修炼等级、势力分布、世界规则相互关联
2. 逻辑自洽：设定之间不矛盾，有内在逻辑
3. 特色鲜明：有独特的世界观亮点，区别于同类小说
4. 可扩展性：为后续剧情留有发展空间

输出要求：以 Markdown 格式输出，结构清晰，层次分明。"#
            .to_string()
    }

    fn name(&self) -> &str {
        "WorldOutlineAgent"
    }
}
