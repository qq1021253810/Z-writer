//! 角色状态追踪系统

use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::path::Path;
use crate::error::{AppError, Result};

/// 角色状态
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CharacterState {
    pub name: String,
    pub location: String,
    pub emotional_state: String,
    pub relationships: HashMap<String, String>,
    pub inventory: Vec<String>,
    pub power_level: Option<String>,
    pub dialogue_style: String,
    pub growth_stage: String,
    pub last_updated_chapter: usize,
}

/// 角色状态变化记录
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct CharacterChange {
    pub chapter: usize,
    pub change_type: String,
    pub before: String,
    pub after: String,
}

/// 角色状态追踪器
pub struct CharacterTracker {
    pub states: HashMap<String, CharacterState>,
    pub history: HashMap<String, Vec<CharacterChange>>,
}

impl CharacterTracker {
    pub fn new() -> Self {
        Self {
            states: HashMap::new(),
            history: HashMap::new(),
        }
    }

    /// 添加或更新角色状态
    pub fn update_character(&mut self, state: CharacterState) {
        let name = state.name.clone();
        self.states.insert(name.clone(), state);
        if !self.history.contains_key(&name) {
            self.history.insert(name, Vec::new());
        }
    }

    /// 记录角色状态变化
    pub fn record_change(&mut self, name: &str, change: CharacterChange) {
        if let Some(history) = self.history.get_mut(name) {
            history.push(change);
        }
    }

    /// 获取角色状态
    pub fn get_character(&self, name: &str) -> Option<&CharacterState> {
        self.states.get(name)
    }

    /// 生成角色状态摘要（注入 Prompt）
    pub fn build_character_context(&self) -> String {
        let mut context = String::new();
        context.push_str("【当前出场角色状态】\n");

        for (name, state) in &self.states {
            context.push_str(&format!(
                "- {}：位置={}，情感={}，关系={}，修为={:?}，持有物品={}\n",
                name, 
                state.location, 
                state.emotional_state,
                state.relationships.len(), 
                state.power_level.as_deref().unwrap_or("无"),
                state.inventory.join("、")
            ));
            
            // 添加关键关系
            if !state.relationships.is_empty() {
                context.push_str("  关系：");
                for (other, relation) in &state.relationships {
                    context.push_str(&format!("{}={}, ", other, relation));
                }
                context.push('\n');
            }
        }

        context
    }

    /// 从 JSON 文件加载
    pub fn load(path: &Path) -> Result<Self> {
        if !path.exists() {
            return Ok(Self::new());
        }
        let content = std::fs::read_to_string(path)?;
        let data: TrackerData = serde_json::from_str(&content)
            .map_err(|e| AppError::Json(e))?;
        Ok(Self {
            states: data.states,
            history: data.history,
        })
    }

    /// 保存到 JSON 文件
    pub fn save(&self, path: &Path) -> Result<()> {
        let data = TrackerData {
            states: self.states.clone(),
            history: self.history.clone(),
        };
        let content = serde_json::to_string_pretty(&data)?;
        std::fs::write(path, content)?;
        Ok(())
    }

    /// 生成角色状态报告（人类可读）
    pub fn generate_report(&self) -> String {
        let mut report = String::new();
        report.push_str("# 角色状态报告\n\n");

        for (name, state) in &self.states {
            report.push_str(&format!("## {}\n\n", name));
            report.push_str(&format!("- **位置**: {}\n", state.location));
            report.push_str(&format!("- **情感状态**: {}\n", state.emotional_state));
            report.push_str(&format!("- **修为等级**: {}\n", 
                state.power_level.as_deref().unwrap_or("无")));
            report.push_str(&format!("- **成长阶段**: {}\n", state.growth_stage));
            report.push_str(&format!("- **对话风格**: {}\n", state.dialogue_style));
            
            if !state.inventory.is_empty() {
                report.push_str(&format!("- **持有物品**: {}\n", state.inventory.join("、")));
            }
            
            if !state.relationships.is_empty() {
                report.push_str("- **关系**:\n");
                for (other, relation) in &state.relationships {
                    report.push_str(&format!("  - {}: {}\n", other, relation));
                }
            }
            
            report.push_str(&format!("- **最后更新**: 第 {} 章\n\n", state.last_updated_chapter));
            
            // 添加变化历史
            if let Some(history) = self.history.get(name) {
                if !history.is_empty() {
                    report.push_str("### 变化历史\n\n");
                    for change in history.iter().rev().take(5) {
                        report.push_str(&format!(
                            "- 第 {} 章: {} 从 {} 变为 {}\n",
                            change.chapter, change.change_type, change.before, change.after
                        ));
                    }
                    report.push('\n');
                }
            }
        }

        report
    }
}

impl CharacterState {
    pub fn new(name: &str) -> Self {
        Self {
            name: name.to_string(),
            location: "未知".to_string(),
            emotional_state: "平静".to_string(),
            relationships: HashMap::new(),
            inventory: Vec::new(),
            power_level: None,
            dialogue_style: "普通".to_string(),
            growth_stage: "初始".to_string(),
            last_updated_chapter: 0,
        }
    }

    pub fn with_location(mut self, location: &str) -> Self {
        self.location = location.to_string();
        self
    }

    pub fn with_power_level(mut self, level: &str) -> Self {
        self.power_level = Some(level.to_string());
        self
    }

    pub fn add_relationship(mut self, other: &str, relation: &str) -> Self {
        self.relationships.insert(other.to_string(), relation.to_string());
        self
    }

    pub fn add_item(mut self, item: &str) -> Self {
        self.inventory.push(item.to_string());
        self
    }
}

impl CharacterChange {
    pub fn new(chapter: usize, change_type: &str, before: &str, after: &str) -> Self {
        Self {
            chapter,
            change_type: change_type.to_string(),
            before: before.to_string(),
            after: after.to_string(),
        }
    }
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct TrackerData {
    states: HashMap<String, CharacterState>,
    history: HashMap<String, Vec<CharacterChange>>,
}
