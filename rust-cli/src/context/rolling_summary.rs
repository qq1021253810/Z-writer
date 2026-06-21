//! 滚动摘要机制

use serde::{Deserialize, Serialize};
use std::path::Path;
use crate::error::{AppError, Result};

/// 章节摘要
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct RollingChapterSummary {
    pub chapter_num: usize,
    pub title: String,
    pub summary: String,
    pub key_events: Vec<String>,
    pub style_passage: Option<String>,  // 代表性文段（风格锚点）
}

/// 整体压缩摘要（每 10 章深度压缩一次）
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CompressedSummary {
    pub volume_range: String,         // 如 "第 1-10 章"
    pub overall_summary: String,
    pub main_plot_progress: String,   // 主线剧情进展
    pub character_arcs: String,       // 角色成长弧线
}

/// 滚动摘要管理器
pub struct RollingSummary {
    pub chapter_summaries: Vec<RollingChapterSummary>,
    pub compressed_summaries: Vec<CompressedSummary>,  // 历史压缩摘要
    pub style_anchor: String,
    pub compression_threshold: usize,  // 每 N 章触发深度压缩
}

impl Default for RollingSummary {
    fn default() -> Self {
        Self::new()
    }
}

impl RollingSummary {
    pub fn new() -> Self {
        Self {
            chapter_summaries: Vec::new(),
            compressed_summaries: Vec::new(),
            style_anchor: String::new(),
            compression_threshold: 10,
        }
    }

    /// 添加章节摘要
    pub fn add_chapter_summary(&mut self, summary: RollingChapterSummary) {
        self.chapter_summaries.push(summary);
    }

    /// 更新风格锚点（从最新章节提取代表性文段）
    pub fn update_style_anchor(&mut self, passage: &str) {
        self.style_anchor = passage.to_string();
    }

    /// 检查是否需要深度压缩
    pub fn needs_compression(&self) -> bool {
        self.chapter_summaries.len() >= self.compression_threshold
    }

    /// 执行深度压缩（将最近 N 章压缩为整体摘要）
    /// 注意：实际压缩需要调用 LLM，这里只负责数据结构管理
    pub fn compress_chapters(&mut self, overall_summary: &str, plot_progress: &str, character_arcs: &str) {
        if self.chapter_summaries.is_empty() {
            return;
        }

        let first_chapter = self.chapter_summaries.first().expect("章节摘要列表为空").chapter_num;
        let last_chapter = self.chapter_summaries.last().expect("章节摘要列表为空").chapter_num;

        let compressed = CompressedSummary {
            volume_range: format!("第 {}-{} 章", first_chapter, last_chapter),
            overall_summary: overall_summary.to_string(),
            main_plot_progress: plot_progress.to_string(),
            character_arcs: character_arcs.to_string(),
        };

        self.compressed_summaries.push(compressed);
        self.chapter_summaries.clear();
    }

    /// 构建续写上下文
    pub fn build_continuation_context(&self, current_chapter: usize) -> String {
        let mut context = String::new();

        // 1. 历史压缩摘要（整体前情）
        if !self.compressed_summaries.is_empty() {
            context.push_str("【前情提要】\n");
            for compressed in &self.compressed_summaries {
                context.push_str(&format!("{}：{}\n", compressed.volume_range, compressed.overall_summary));
                if !compressed.main_plot_progress.is_empty() {
                    context.push_str(&format!("  主线进展：{}\n", compressed.main_plot_progress));
                }
                if !compressed.character_arcs.is_empty() {
                    context.push_str(&format!("  角色成长：{}\n", compressed.character_arcs));
                }
            }
            context.push('\n');
        }

        // 2. 最近 3 章摘要（详细）
        let recent: Vec<&RollingChapterSummary> = self.chapter_summaries
            .iter()
            .filter(|s| s.chapter_num < current_chapter)
            .rev()
            .take(3)
            .collect();

        if !recent.is_empty() {
            context.push_str("【最近章节】\n");
            for summary in recent.iter().rev() {
                context.push_str(&format!("第 {} 章：{}\n", summary.chapter_num, summary.title));
                context.push_str(&format!("{}\n", summary.summary));
                if !summary.key_events.is_empty() {
                    context.push_str("关键事件：");
                    for event in &summary.key_events {
                        context.push_str(&format!("{}、", event));
                    }
                    context.push('\n');
                }
                context.push('\n');
            }
        }

        // 3. 风格锚点
        if !self.style_anchor.is_empty() {
            context.push_str(&format!("【文风参考】\n{}\n\n", self.style_anchor));
        }

        context
    }

    /// 从 JSON 文件加载
    pub fn load(path: &Path) -> Result<Self> {
        if !path.exists() {
            return Ok(Self::new());
        }
        let content = std::fs::read_to_string(path)?;
        let data: SummaryData = serde_json::from_str(&content)
            .map_err(AppError::Json)?;
        Ok(Self {
            chapter_summaries: data.chapter_summaries,
            compressed_summaries: data.compressed_summaries,
            style_anchor: data.style_anchor,
            compression_threshold: data.compression_threshold,
        })
    }

    /// 保存到 JSON 文件
    pub fn save(&self, path: &Path) -> Result<()> {
        let data = SummaryData {
            chapter_summaries: self.chapter_summaries.clone(),
            compressed_summaries: self.compressed_summaries.clone(),
            style_anchor: self.style_anchor.clone(),
            compression_threshold: self.compression_threshold,
        };
        let content = serde_json::to_string_pretty(&data)?;
        std::fs::write(path, content)?;
        Ok(())
    }

    /// 生成摘要报告（人类可读）
    pub fn generate_report(&self) -> String {
        let mut report = String::new();
        report.push_str("# 剧情摘要报告\n\n");

        if !self.compressed_summaries.is_empty() {
            report.push_str("## 历史压缩摘要\n\n");
            for compressed in &self.compressed_summaries {
                report.push_str(&format!("### {}\n\n", compressed.volume_range));
                report.push_str(&format!("{}\n\n", compressed.overall_summary));
                if !compressed.main_plot_progress.is_empty() {
                    report.push_str(&format!("**主线进展**: {}\n\n", compressed.main_plot_progress));
                }
                if !compressed.character_arcs.is_empty() {
                    report.push_str(&format!("**角色成长**: {}\n\n", compressed.character_arcs));
                }
            }
        }

        if !self.chapter_summaries.is_empty() {
            report.push_str("## 章节摘要\n\n");
            for summary in &self.chapter_summaries {
                report.push_str(&format!("### 第 {} 章：{}\n\n", summary.chapter_num, summary.title));
                report.push_str(&format!("{}\n\n", summary.summary));
                if !summary.key_events.is_empty() {
                    report.push_str("**关键事件**：\n");
                    for event in &summary.key_events {
                        report.push_str(&format!("- {}\n", event));
                    }
                    report.push('\n');
                }
            }
        }

        if !self.style_anchor.is_empty() {
            report.push_str("## 文风锚点\n\n");
            report.push_str(&format!("{}\n", self.style_anchor));
        }

        report
    }
}

impl RollingChapterSummary {
    pub fn new(chapter_num: usize, title: &str, summary: &str) -> Self {
        Self {
            chapter_num,
            title: title.to_string(),
            summary: summary.to_string(),
            key_events: Vec::new(),
            style_passage: None,
        }
    }

    pub fn add_key_event(mut self, event: &str) -> Self {
        self.key_events.push(event.to_string());
        self
    }

    pub fn with_style_passage(mut self, passage: &str) -> Self {
        self.style_passage = Some(passage.to_string());
        self
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct SummaryData {
    chapter_summaries: Vec<RollingChapterSummary>,
    compressed_summaries: Vec<CompressedSummary>,
    style_anchor: String,
    compression_threshold: usize,
}
