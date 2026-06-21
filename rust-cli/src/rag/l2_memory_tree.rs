//! L2 剧情记忆层 - JSON 树状结构

use serde::{Deserialize, Serialize};
use std::path::Path;
use crate::error::{AppError, Result};

/// 剧情记忆树
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct MemoryTree {
    pub novel_id: String,
    pub volumes: Vec<VolumeSummary>,
    pub foreshadows: Vec<Foreshadow>,
}

/// 卷摘要
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct VolumeSummary {
    pub volume_num: usize,
    pub title: String,
    pub summary: String,
    pub chapters: Vec<ChapterSummary>,
}

/// 章节摘要
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ChapterSummary {
    pub chapter_num: usize,
    pub title: String,
    pub summary: String,
    pub key_events: Vec<String>,
    pub character_changes: Vec<CharacterChange>,
}

/// 角色变化
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CharacterChange {
    pub character_name: String,
    pub change_type: String, // 位置、情感、关系、物品、修为等
    pub before: String,
    pub after: String,
}

/// 伏笔
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Foreshadow {
    pub id: String,
    pub description: String,
    pub planted_chapter: usize,
    pub expected_resolve: Option<usize>,
    pub actual_resolve: Option<usize>,
    pub status: ForeshadowStatus,
}

/// 伏笔状态
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum ForeshadowStatus {
    Active,      // 活跃（已埋下未回收）
    Resolved,    // 已回收
    Abandoned,   // 已放弃
}

impl MemoryTree {
    /// 创建新的记忆树
    pub fn new(novel_id: &str) -> Self {
        Self {
            novel_id: novel_id.to_string(),
            volumes: Vec::new(),
            foreshadows: Vec::new(),
        }
    }

    /// 从 JSON 文件加载
    pub fn load(path: &Path) -> Result<Self> {
        if !path.exists() {
            return Ok(Self::new(""));
        }
        let content = std::fs::read_to_string(path)?;
        let tree: MemoryTree = serde_json::from_str(&content)
            .map_err(|e| AppError::Json(e))?;
        Ok(tree)
    }

    /// 保存到 JSON 文件
    pub fn save(&self, path: &Path) -> Result<()> {
        let content = serde_json::to_string_pretty(self)?;
        std::fs::write(path, content)?;
        Ok(())
    }

    /// 添加卷
    pub fn add_volume(&mut self, volume: VolumeSummary) {
        self.volumes.push(volume);
    }

    /// 获取卷摘要
    pub fn get_volume(&self, volume_num: usize) -> Option<&VolumeSummary> {
        self.volumes.iter().find(|v| v.volume_num == volume_num)
    }

    /// 获取章节摘要
    pub fn get_chapter(&self, volume_num: usize, chapter_num: usize) -> Option<&ChapterSummary> {
        self.volumes
            .iter()
            .find(|v| v.volume_num == volume_num)
            .and_then(|v| v.chapters.iter().find(|c| c.chapter_num == chapter_num))
    }

    /// 按需召回：先检索卷摘要，再向下钻取章摘要
    pub fn recall_plot(&self, current_chapter: usize, lookback: usize) -> String {
        let mut result = String::new();
        
        // 找到当前章节所在的卷
        let current_volume = self.volumes
            .iter()
            .find(|v| {
                v.chapters.iter().any(|c| c.chapter_num == current_chapter)
            });

        if let Some(volume) = current_volume {
            result.push_str(&format!("【第 {} 卷：{}】\n", volume.volume_num, volume.title));
            result.push_str(&format!("{}\n\n", volume.summary));

            // 召回最近 N 章的摘要
            let start = current_chapter.saturating_sub(lookback);
            let chapters: Vec<&ChapterSummary> = volume.chapters
                .iter()
                .filter(|c| c.chapter_num >= start && c.chapter_num < current_chapter)
                .collect();

            if !chapters.is_empty() {
                result.push_str("【最近章节】\n");
                for chapter in chapters {
                    result.push_str(&format!("第 {} 章：{}\n", chapter.chapter_num, chapter.title));
                    result.push_str(&format!("{}\n", chapter.summary));
                    
                    if !chapter.key_events.is_empty() {
                        result.push_str("关键事件：\n");
                        for event in &chapter.key_events {
                            result.push_str(&format!("  - {}\n", event));
                        }
                    }
                    result.push('\n');
                }
            }
        }

        // 召回活跃伏笔
        let active_foreshadows: Vec<&Foreshadow> = self.foreshadows
            .iter()
            .filter(|f| f.status == ForeshadowStatus::Active)
            .collect();

        if !active_foreshadows.is_empty() {
            result.push_str("【活跃伏笔】\n");
            for foreshadow in active_foreshadows {
                result.push_str(&format!("- {}（第 {} 章埋下）\n", 
                    foreshadow.description, foreshadow.planted_chapter));
            }
        }

        result
    }

    /// 添加伏笔
    pub fn add_foreshadow(&mut self, foreshadow: Foreshadow) {
        self.foreshadows.push(foreshadow);
    }

    /// 更新伏笔状态
    pub fn update_foreshadow(&mut self, id: &str, status: ForeshadowStatus, resolve_chapter: Option<usize>) -> bool {
        if let Some(foreshadow) = self.foreshadows.iter_mut().find(|f| f.id == id) {
            foreshadow.status = status;
            if let Some(chapter) = resolve_chapter {
                foreshadow.actual_resolve = Some(chapter);
            }
            true
        } else {
            false
        }
    }

    /// 获取活跃伏笔
    pub fn get_active_foreshadows(&self) -> Vec<&Foreshadow> {
        self.foreshadows
            .iter()
            .filter(|f| f.status == ForeshadowStatus::Active)
            .collect()
    }

    /// 生成伏笔追踪表（人类可读）
    pub fn generate_foreshadow_report(&self) -> String {
        let mut report = String::new();
        report.push_str("# 伏笔追踪表\n\n");

        let active: Vec<&Foreshadow> = self.foreshadows
            .iter()
            .filter(|f| f.status == ForeshadowStatus::Active)
            .collect();
        
        let resolved: Vec<&Foreshadow> = self.foreshadows
            .iter()
            .filter(|f| f.status == ForeshadowStatus::Resolved)
            .collect();

        if !active.is_empty() {
            report.push_str("## 活跃伏笔\n\n");
            for f in active {
                report.push_str(&format!("- **{}**（第 {} 章埋下）\n", f.description, f.planted_chapter));
                if let Some(expected) = f.expected_resolve {
                    report.push_str(&format!("  - 预计回收：第 {} 章\n", expected));
                }
            }
            report.push('\n');
        }

        if !resolved.is_empty() {
            report.push_str("## 已回收伏笔\n\n");
            for f in resolved {
                report.push_str(&format!("- ~~{}~~（第 {} 章埋下，第 {} 章回收）\n", 
                    f.description, f.planted_chapter, 
                    f.actual_resolve.unwrap_or(0)));
            }
        }

        report
    }

    /// 生成时间线（人类可读）
    pub fn generate_timeline(&self) -> String {
        let mut timeline = String::new();
        timeline.push_str("# 剧情时间线\n\n");

        for volume in &self.volumes {
            timeline.push_str(&format!("## 第 {} 卷：{}\n\n", volume.volume_num, volume.title));
            timeline.push_str(&format!("{}\n\n", volume.summary));

            for chapter in &volume.chapters {
                timeline.push_str(&format!("### 第 {} 章：{}\n\n", chapter.chapter_num, chapter.title));
                timeline.push_str(&format!("{}\n\n", chapter.summary));

                if !chapter.key_events.is_empty() {
                    timeline.push_str("**关键事件：**\n");
                    for event in &chapter.key_events {
                        timeline.push_str(&format!("- {}\n", event));
                    }
                    timeline.push('\n');
                }
            }
        }

        timeline
    }
}

impl VolumeSummary {
    pub fn new(volume_num: usize, title: &str, summary: &str) -> Self {
        Self {
            volume_num,
            title: title.to_string(),
            summary: summary.to_string(),
            chapters: Vec::new(),
        }
    }

    pub fn add_chapter(&mut self, chapter: ChapterSummary) {
        self.chapters.push(chapter);
    }
}

impl ChapterSummary {
    pub fn new(chapter_num: usize, title: &str, summary: &str) -> Self {
        Self {
            chapter_num,
            title: title.to_string(),
            summary: summary.to_string(),
            key_events: Vec::new(),
            character_changes: Vec::new(),
        }
    }

    pub fn add_key_event(&mut self, event: &str) {
        self.key_events.push(event.to_string());
    }

    pub fn add_character_change(&mut self, change: CharacterChange) {
        self.character_changes.push(change);
    }
}

impl CharacterChange {
    pub fn new(character_name: &str, change_type: &str, before: &str, after: &str) -> Self {
        Self {
            character_name: character_name.to_string(),
            change_type: change_type.to_string(),
            before: before.to_string(),
            after: after.to_string(),
        }
    }
}

impl Foreshadow {
    pub fn new(id: &str, description: &str, planted_chapter: usize) -> Self {
        Self {
            id: id.to_string(),
            description: description.to_string(),
            planted_chapter,
            expected_resolve: None,
            actual_resolve: None,
            status: ForeshadowStatus::Active,
        }
    }

    pub fn with_expected_resolve(mut self, chapter: usize) -> Self {
        self.expected_resolve = Some(chapter);
        self
    }
}
