//! 新建小说工作流

use crate::error::Result;
use crate::workspace::Workspace;
use crate::llm::LlmClient;
use crate::agents::world_outline::WorldOutlineAgent;
use crate::agents::character::CharacterAgent;
use crate::agents::plot::PlotAgent;
use crate::agents::base::{Agent, AgentContext};
use std::path::Path;

/// 新建小说工作流
pub struct CreateNovelWorkflow {
    client: LlmClient,
}

impl CreateNovelWorkflow {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 执行 Agent 并保存结果
    async fn execute_agent_and_save<A: Agent>(
        &self,
        agent: A,
        workspace: &Workspace,
        user_input: &str,
        output_file: &str,
        title: &str,
    ) -> Result<()> {
        let ctx = AgentContext {
            workspace_path: workspace.root().to_path_buf(),
            user_input: user_input.to_string(),
            system_prompt: agent.system_prompt(),
        };
        let result = agent.execute(&ctx).await?;
        
        std::fs::write(
            workspace.root().join(output_file),
            format!("# {}\n\n{}", title, result.content),
        )?;
        tracing::info!("[CLI] 保存文件: {}", output_file);
        
        Ok(())
    }

    /// 执行新建小说工作流（多轮对话引导）
    pub async fn execute(&self, workspace_path: &Path, genre: &str) -> Result<()> {
        tracing::info!("[Workflow] 开始新建小说: {}", genre);
        println!("🎬 开始新建小说工作流...\n");

        // 1. 创建小说工作区（以赛道命名）
        let ws = Workspace::create(workspace_path, genre)?;
        println!("✅ 创建工作区: {}", ws.root().display());
        tracing::info!("[Workflow] 创建工作区: {}", ws.root().display());

        // 2. 使用用户选择的赛道
        println!("\n📚 赛道: {}", genre);

        // 3. 使用 WorldOutlineAgent 生成世界观
        println!("\n🌍 正在生成世界观...");
        let world_agent = WorldOutlineAgent::new(self.client.clone());
        self.execute_agent_and_save(
            world_agent,
            &ws,
            &format!("请为{}类型小说设计世界观的规则体系", genre),
            "worldview.md",
            "世界观设定",
        ).await?;
        println!("✅ 世界观已保存");
        tracing::info!("[Workflow] 完成步骤: 世界观生成");

        // 4. 使用 PlotAgent 生成大纲
        println!("\n📋 正在生成大纲...");
        let plot_agent = PlotAgent::new(self.client.clone());
        self.execute_agent_and_save(
            plot_agent,
            &ws,
            &format!("请为{}类型小说设计整体大纲，包括主线剧情、卷次划分和关键转折点", genre),
            "outline.md",
            "大纲",
        ).await?;
        println!("✅ 大纲已保存");
        tracing::info!("[Workflow] 完成步骤: 大纲生成");

        // 5. 使用 CharacterAgent 生成角色
        println!("\n👤 正在生成角色设定...");
        let char_agent = CharacterAgent::new(self.client.clone());
        self.execute_agent_and_save(
            char_agent,
            &ws,
            "请设计主角和 3 个重要配角",
            "characters/主角.md",
            "角色设定",
        ).await?;
        println!("✅ 角色设定已保存");
        tracing::info!("[Workflow] 完成步骤: 角色生成");

        // 6. 使用 PlotAgent 生成前三章剧情
        println!("\n📖 正在生成前三章剧情...");
        let plot_agent = PlotAgent::new(self.client.clone());
        self.execute_agent_and_save(
            plot_agent,
            &ws,
            "请设计黄金三章的剧情",
            "plot_design.md",
            "剧情设计",
        ).await?;
        println!("✅ 剧情设计已保存");
        tracing::info!("[Workflow] 完成步骤: 剧情生成");

        // 7. 更新小说信息
        std::fs::write(
            ws.root().join("novel.md"),
            format!(
                "# {}\n\n- 类型: {}\n- 状态: 初始化\n- 简介: 待补充\n",
                genre, genre
            ),
        )?;
        tracing::info!("[CLI] 保存文件: novel.md");

        println!("\n🎉 新建小说工作流完成！");
        println!("   工作区: {}", ws.root().display());
        println!("   已创建: novel.md, worldview.md, outline.md, characters/, plot_design.md");
        tracing::info!("[Workflow] 新建小说工作流完成");

        Ok(())
    }
}
