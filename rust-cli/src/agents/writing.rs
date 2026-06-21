//! 写作 Agent

use async_trait::async_trait;
use crate::agents::base::{Agent, AgentContext, AgentResult};
use crate::error::Result;
use crate::llm::LlmClient;
use crate::wiki::Wiki;
use crate::workspace::Workspace;
use crate::rag::l2_memory_tree::MemoryTree;
use crate::rag::l3_material_store::L3MaterialStore;

/// 写作 Agent
pub struct WritingAgent {
    client: LlmClient,
}

impl WritingAgent {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 构建写作上下文（含 L2 记忆树 + L3 素材库）
    async fn build_writing_context(&self, ctx: &AgentContext) -> Result<String> {
        let workspace = Workspace::open(ctx.workspace_path.clone())?;
        let wiki = Wiki::new(workspace.root().join("wiki"));
        
        // 使用公共方法构建基础上下文
        let mut context = workspace.build_full_context(3)?;
        
        // 加载 Wiki 规则
        let rules = wiki.load_rules()?;
        if !rules.is_empty() {
            context.push_str("【写作规则】\n");
            context.push_str(&rules);
            context.push_str("\n\n");
        }

        // L2 记忆树：召回剧情上下文
        let memory_tree_path = workspace.root().join("memory_tree.json");
        if memory_tree_path.exists() {
            if let Ok(memory_tree) = MemoryTree::load(&memory_tree_path) {
                let current_chapter = workspace.next_chapter_num().unwrap_or(1);
                if current_chapter > 1 {
                    let plot_context = memory_tree.recall_plot(current_chapter, 3);
                    if !plot_context.is_empty() {
                        context.push_str("【剧情记忆树】\n");
                        context.push_str(&plot_context);
                        context.push('\n');
                    }
                }
                
                // 注入活跃伏笔
                let active_foreshadows = memory_tree.get_active_foreshadows();
                if !active_foreshadows.is_empty() {
                    context.push_str("【需回收伏笔】\n");
                    for fs in &active_foreshadows {
                        context.push_str(&format!("- {}（第 {} 章埋下）\n", 
                            fs.description, fs.planted_chapter));
                    }
                    context.push('\n');
                }
            }
        }

        // L3 素材库：搜索相关素材
        let vector_store_path = workspace.root().join("vector_store");
        if vector_store_path.exists() {
            if let Ok(store) = L3MaterialStore::load(&vector_store_path) {
                if !store.is_empty() {
                    // 获取最近章节的关键词用于搜索
                    let keywords = extract_keywords_from_context(&context);
                    let relevant = store.search_by_keywords(&keywords, 3);
                    if !relevant.is_empty() {
                        context.push_str("【相关素材参考】\n");
                        for mat in &relevant {
                            let preview = if mat.content.len() > 100 {
                                &mat.content[..100]
                            } else {
                                &mat.content
                            };
                            context.push_str(&format!("- [{:?}] {}\n", mat.category, preview));
                        }
                        context.push('\n');
                    }
                }
            }
        }
        
        Ok(context)
    }

    /// 构建聊天消息（供 execute 和 execute_stream 共用）
    async fn build_messages(&self, ctx: &AgentContext) -> Result<Vec<crate::llm::ChatMessage>> {
        let writing_context = self.build_writing_context(ctx).await?;
        let prompt = format!(
            "{}\n\n【用户请求】\n{}\n\n请根据以上设定和上下文，完成用户的写作请求。要求：\n\
             1. 保持与已有内容的连贯性\n\
             2. 符合角色性格和世界观设定\n\
             3. 遵循网文写作规则\n\
             4. 如有活跃伏笔，在合适时机回收\n\
             5. 字数约 2000-3000 字",
            writing_context, ctx.user_input
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

/// 从上下文中提取关键词用于素材搜索
fn extract_keywords_from_context(context: &str) -> Vec<&str> {
    let candidates = [
        "商业", "科技", "法律", "博弈", "谈判", "收购", "上市", "融资",
        "董事会", "股东", "竞争", "合作", "创新", "专利", "市场", "战略",
        "布局", "收网", "利益", "权力", "政治", "情报", "谈判", "协议",
    ];
    let mut keywords = Vec::new();
    for kw in &candidates {
        if context.contains(kw) {
            keywords.push(*kw);
        }
    }
    if keywords.is_empty() {
        keywords.push("商业"); // 默认关键词
    }
    keywords
}

#[async_trait]
impl Agent for WritingAgent {
    async fn execute(&self, ctx: &AgentContext) -> Result<AgentResult> {
        let messages = self.build_messages(ctx).await?;
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
        let messages = self.build_messages(ctx).await?;
        let result = self.client.chat_stream(messages, |chunk| on_chunk(chunk)).await?;
        Ok(AgentResult {
            content: result.content,
            need_confirm: false,
            metadata: std::collections::HashMap::new(),
        })
    }

    fn system_prompt(&self) -> String {
        "你是专业的网文小说写作助手，擅长创作高智商、高情商、权谋博弈、规则利用的高格局都市文。\
         你的写作风格：\n\
         1. 逻辑严密，布局精妙，每一步都有深远意义\n\
         2. 人物对话展现高智商和高情商，善用潜台词和话术博弈\n\
         3. 商业场景描写专业，善于设计精妙的商业布局和利益博弈\n\
         4. 善用悬念和转折，布局收网环环相扣\n\
         5. 符合现实规则（法律、商业、科技），展现极致的规则利用\n\
         6. 保持与已有内容的连贯性，世界观自洽".to_string()
    }

    fn name(&self) -> &str {
        "WritingAgent"
    }
}