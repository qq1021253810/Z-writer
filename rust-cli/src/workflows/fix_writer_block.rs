//! 卡文修复工作流

use crate::error::Result;
use crate::workspace::Workspace;
use crate::llm::LlmClient;
use crate::agents::plot::PlotAgent;
use crate::agents::base::{Agent, AgentContext};
use std::path::Path;

/// 卡文修复工作流
pub struct FixWriterBlockWorkflow {
    client: LlmClient,
}

impl FixWriterBlockWorkflow {
    pub fn new(client: LlmClient) -> Self {
        Self { client }
    }

    /// 执行卡文修复工作流
    pub async fn execute(&self, workspace_path: &Path, chapter_num: usize) -> Result<()> {
        tracing::info!("[Workflow] 开始卡文修复: 第 {} 章", chapter_num);
        println!("🔧 开始卡文修复工作流...\n");

        let ws = Workspace::open(workspace_path.to_path_buf())?;
        println!("✅ 打开工作区: {}", ws.root().display());
        tracing::info!("[Workflow] 打开工作区: {}", ws.root().display());

        // 1. 读取当前章节
        let chapter_content = ws.read_chapter(chapter_num)?;
        let chapter_content = chapter_content.ok_or_else(|| {
            crate::error::AppError::Workspace(format!("第 {} 章不存在或为空", chapter_num))
        })?;
        println!("📖 读取第 {} 章，共 {} 字", chapter_num, chapter_content.len());
        tracing::info!("[Workflow] 读取第 {} 章，字数: {}", chapter_num, chapter_content.len());

        // 2. 使用 PlotAgent 分析卡文原因
        println!("\n🔍 正在分析卡文原因...");
        let plot_agent = PlotAgent::new(self.client.clone());
        let ctx = AgentContext {
            workspace_path: workspace_path.to_path_buf(),
            user_input: format!(
                "请分析以下章节内容，找出可能导致卡文的原因，并提供 3 套解决方案：\n\n{}",
                chapter_content
            ),
            system_prompt: plot_agent.system_prompt(),
        };
        let analysis_result = plot_agent.execute(&ctx).await?;
        tracing::info!("[Workflow] 卡文分析完成");
        
        // 保存分析结果
        std::fs::write(
            ws.root().join(format!("chapter_{}_analysis.md", chapter_num)),
            format!("# 第 {} 章卡文分析\n\n{}", chapter_num, analysis_result.content),
        )?;
        println!("✅ 分析结果已保存");
        tracing::info!("[CLI] 保存文件: chapter_{}_analysis.md", chapter_num);

        // 3. 提供解决方案给用户选择
        println!("\n💡 已生成 3 套解决方案，请查看: chapter_{}_analysis.md", chapter_num);
        println!("   选择方案后，可使用 /continue 命令继续创作");
        tracing::info!("[Workflow] 卡文修复工作流完成");

        Ok(())
    }
}
