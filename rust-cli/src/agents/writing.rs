//! 写作 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;
use crate::wiki::Wiki;
use crate::workspace::Workspace;

/// 写作 Agent
pub struct WritingAgent {
    client: LlmClient,
}

impl WritingAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 构建写作上下文
    async fn build_writing_context(&self, ctx: &AgentContext) -> Result<String> {
        let workspace = Workspace::open(ctx.workspace_path.clone())?;
        let wiki = Wiki::new(workspace.root().join("wiki"));
        
        // 加载小说基础信息
        let novel_info = workspace.novel_info()?;
        
        // 加载世界观
        let worldview = workspace.worldview()?;
        
        // 加载大纲
        let outline = workspace.outline()?;
        
        // 加载角色设定
        let characters = workspace.characters()?;
        
        // 加载最近章节
        let recent_chapters = workspace.recent_chapters(3)?;
        
        // 加载 Wiki 规则
        let rules = wiki.load_rules()?;
        
        // 构建上下文
        let mut context = String::new();
        context.push_str("【小说信息】\n");
        context.push_str(&novel_info);
        context.push_str("\n\n");
        
        if !worldview.is_empty() {
            context.push_str("【世界观】\n");
            context.push_str(&worldview);
            context.push_str("\n\n");
        }
        
        if !outline.is_empty() {
            context.push_str("【大纲】\n");
            context.push_str(&outline);
            context.push_str("\n\n");
        }
        
        if !characters.is_empty() {
            context.push_str("【角色设定】\n");
            for char_info in &characters {
                context.push_str(char_info);
                context.push_str("\n");
            }
            context.push_str("\n");
        }
        
        if !recent_chapters.is_empty() {
            context.push_str("【最近章节】\n");
            for (num, content) in &recent_chapters {
                context.push_str(&format!("第 {} 章:\n{}\n\n", num, content));
            }
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
impl Agent for WritingAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        // 构建写作上下文
        let writing_context = self.build_writing_context(ctx).await?;
        
        // 构建完整提示词
        let prompt = format!(
            "{}\n\n【用户请求】\n{}\n\n请根据以上设定和上下文，完成用户的写作请求。要求：\n\
             1. 保持与已有内容的连贯性\n\
             2. 符合角色性格和世界观设定\n\
             3. 遵循网文写作规则\n\
             4. 字数约 2000-3000 字",
            writing_context, ctx.user_input
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
        
        let result = self.client.chat(messages).await?;
        
        Ok(AgentResult {
            content: result.content,
            need_confirm: false,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        "你是专业的网文小说写作助手，擅长根据大纲和设定创作精彩的章节内容。\
         你的写作风格：\n\
         1. 节奏明快，情节紧凑\n\
         2. 人物对话生动，符合角色性格\n\
         3. 场景描写细腻，代入感强\n\
         4. 善用悬念和转折，吸引读者\n\
         5. 保持与已有内容的连贯性".to_string()
    }

    fn name(&self) -> &str {
        "WritingAgent"
    }
}
