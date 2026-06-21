//! LLM Wiki 知识库模块

use std::path::PathBuf;
use std::collections::{HashMap, HashSet};
use crate::error::Result;

/// Wiki 知识库
pub struct Wiki {
    pub root: PathBuf,
}

/// Wiki 页面元数据
#[derive(Debug, Clone)]
pub struct WikiPageMeta {
    pub name: String,
    pub category: String, // rules / genres / templates / examples / entities
    pub path: PathBuf,
    pub size: usize,
}

/// 健康检查结果
#[derive(Debug)]
pub struct HealthReport {
    pub total_pages: usize,
    pub missing_required: Vec<String>,
    pub empty_pages: Vec<String>,
    pub warnings: Vec<String>,
}

impl HealthReport {
    pub fn is_healthy(&self) -> bool {
        self.missing_required.is_empty() && self.empty_pages.is_empty()
    }

    pub fn summary(&self) -> String {
        let mut s = format!("Wiki 健康报告: 共 {} 个页面\n", self.total_pages);
        if !self.missing_required.is_empty() {
            s.push_str(&format!("  缺失必要文件: {:?}\n", self.missing_required));
        }
        if !self.empty_pages.is_empty() {
            s.push_str(&format!("  空文件: {:?}\n", self.empty_pages));
        }
        if !self.warnings.is_empty() {
            s.push_str(&format!("  警告: {:?}\n", self.warnings));
        }
        if self.is_healthy() {
            s.push_str("  状态: 健康");
        } else {
            s.push_str("  状态: 需要修复");
        }
        s
    }
}

/// 实体类型（用于自动页面生成）
#[derive(Debug, Clone, PartialEq, Eq, Hash)]
pub enum EntityType {
    Character,   // 角色
    Location,    // 地点
    Item,        // 物品/资源
    Skill,       // 技能/方案
    Organization,// 组织/公司
    Concept,     // 概念/设定
}

impl EntityType {
    pub fn as_str(&self) -> &str {
        match self {
            EntityType::Character => "character",
            EntityType::Location => "location",
            EntityType::Item => "item",
            EntityType::Skill => "skill",
            EntityType::Organization => "organization",
            EntityType::Concept => "concept",
        }
    }

    pub fn parse(s: &str) -> Option<Self> {
        match s.to_lowercase().as_str() {
            "character" | "角色" | "人物" => Some(EntityType::Character),
            "location" | "地点" | "地方" => Some(EntityType::Location),
            "item" | "物品" | "资源" | "资产" => Some(EntityType::Item),
            "skill" | "技能" | "方案" | "策略" => Some(EntityType::Skill),
            "organization" | "组织" | "公司" | "集团" | "机构" => Some(EntityType::Organization),
            "concept" | "概念" | "设定" => Some(EntityType::Concept),
            _ => None,
        }
    }
}

/// 提取的实体
#[derive(Debug, Clone)]
pub struct Entity {
    pub name: String,
    pub entity_type: EntityType,
    pub description: String,
    pub attributes: HashMap<String, String>,
}

impl Wiki {
    pub fn new(root: PathBuf) -> Self {
        Self { root }
    }

    /// 加载规则文件
    pub fn load_rules(&self) -> Result<String> {
        let rules_dir = self.root.join("rules");
        if !rules_dir.exists() {
            return Ok(String::new());
        }

        let mut content = String::new();
        for entry in std::fs::read_dir(rules_dir)? {
            let entry = entry?;
            let path = entry.path();
            if path.extension().and_then(|s| s.to_str()) == Some("md") {
                let text = std::fs::read_to_string(&path)?;
                content.push_str(&text);
                content.push_str("\n\n");
            }
        }
        Ok(content)
    }

    /// 加载赛道知识
    pub fn load_genre(&self, genre: &str) -> Result<String> {
        let path = self.root.join("genres").join(format!("{}.md", genre));
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }

    /// 加载模板
    pub fn load_template(&self, template_name: &str) -> Result<String> {
        let path = self.root.join("templates").join(format!("{}.md", template_name));
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }

    /// 加载相关页面（根据赛道）
    pub fn load_relevant_pages(&self, genre: &str) -> Result<Vec<String>> {
        let mut pages = Vec::new();

        // 加载通用规则
        let rules = self.load_rules()?;
        if !rules.is_empty() {
            pages.push(rules);
        }

        // 加载赛道知识
        let genre_content = self.load_genre(genre)?;
        if !genre_content.is_empty() {
            pages.push(genre_content);
        }

        // 加载示例
        let examples_dir = self.root.join("examples");
        if examples_dir.exists() {
            for entry in std::fs::read_dir(examples_dir)? {
                let entry = entry?;
                let path = entry.path();
                if path.extension().and_then(|s| s.to_str()) == Some("md") {
                    let text = std::fs::read_to_string(&path)?;
                    pages.push(text);
                }
            }
        }

        Ok(pages)
    }

    // ========== 增强功能 ==========

    /// 列出所有 Wiki 页面
    pub fn list_pages(&self) -> Result<Vec<WikiPageMeta>> {
        let mut pages = Vec::new();
        let categories = ["rules", "genres", "templates", "examples", "entities"];

        for category in &categories {
            let dir = self.root.join(category);
            if !dir.exists() {
                continue;
            }
            for entry in std::fs::read_dir(&dir)? {
                let entry = entry?;
                let path = entry.path();
                if path.extension().and_then(|s| s.to_str()) == Some("md") {
                    let name = path.file_stem()
                        .and_then(|s| s.to_str())
                        .unwrap_or("")
                        .to_string();
                    let size = std::fs::metadata(&path)?.len() as usize;
                    pages.push(WikiPageMeta {
                        name,
                        category: category.to_string(),
                        path,
                        size,
                    });
                }
            }
        }

        Ok(pages)
    }

    /// 健康检查
    pub fn health_check(&self) -> Result<HealthReport> {
        let pages = self.list_pages()?;
        let mut report = HealthReport {
            total_pages: pages.len(),
            missing_required: Vec::new(),
            empty_pages: Vec::new(),
            warnings: Vec::new(),
        };

        // 检查必要目录
        let required_dirs = ["rules", "genres", "templates"];
        for dir in &required_dirs {
            let dir_path = self.root.join(dir);
            if !dir_path.exists() {
                report.missing_required.push(format!("{}/ 目录不存在", dir));
            }
        }

        // 检查必要文件
        let required_files = [
            "rules/writing_rules.md",
            "templates/writing.md",
        ];
        for file in &required_files {
            let file_path = self.root.join(file);
            if !file_path.exists() {
                report.missing_required.push(file.to_string());
            }
        }

        // 检查空文件
        for page in &pages {
            if page.size == 0 {
                report.empty_pages.push(format!("{}/{}.md", page.category, page.name));
            }
        }

        // 检查赛道文件
        let genres_dir = self.root.join("genres");
        if genres_dir.exists() {
            let genre_count = std::fs::read_dir(&genres_dir)?
                .filter_map(|e| e.ok())
                .filter(|e| e.path().extension().and_then(|s| s.to_str()) == Some("md"))
                .count();
            if genre_count == 0 {
                report.warnings.push("genres/ 目录为空，没有赛道知识文件".to_string());
            }
        }

        // 检查实体目录
        let entities_dir = self.root.join("entities");
        if entities_dir.exists() {
            let entity_count = std::fs::read_dir(&entities_dir)?
                .filter_map(|e| e.ok())
                .filter(|e| e.path().extension().and_then(|s| s.to_str()) == Some("md"))
                .count();
            if entity_count == 0 {
                report.warnings.push("entities/ 目录为空，建议创建实体页面".to_string());
            }
        }

        Ok(report)
    }

    /// 从文本中提取实体（基于简单规则）
    pub fn extract_entities(&self, text: &str) -> Vec<Entity> {
        let mut entities = Vec::new();
        let mut seen_names = HashSet::new();

        // 基于关键词模式提取实体
        let patterns: Vec<(&str, EntityType)> = vec![
            ("角色:", EntityType::Character),
            ("人物:", EntityType::Character),
            ("地点:", EntityType::Location),
            ("地方:", EntityType::Location),
            ("方案:", EntityType::Skill),
            ("技能:", EntityType::Skill),
            ("资源:", EntityType::Item),
            ("公司:", EntityType::Organization),
            ("集团:", EntityType::Organization),
            ("组织:", EntityType::Organization),
        ];

        for line in text.lines() {
            let line = line.trim();
            for (prefix, entity_type) in &patterns {
                if let Some(rest) = line.strip_prefix(prefix) {
                    let rest = rest.trim();
                    // 提取名称（取冒号或逗号前的部分）
                    let name = rest.split(['，', ',', '：', ':'])
                        .next()
                        .unwrap_or("")
                        .trim()
                        .to_string();

                    if !name.is_empty() && name.len() <= 20 && !seen_names.contains(&name) {
                        seen_names.insert(name.clone());

                        // 提取描述（冒号后的部分）
                        let description = if let Some(pos) = rest.find(['：', ':']) {
                            rest[pos + 3..].trim().to_string() // 跳过 "： " 或 ": "
                        } else {
                            String::new()
                        };

                        entities.push(Entity {
                            name,
                            entity_type: entity_type.clone(),
                            description,
                            attributes: HashMap::new(),
                        });
                    }
                }
            }
        }

        entities
    }

    /// 为实体生成 Wiki 页面
    pub fn generate_entity_page(&self, entity: &Entity) -> Result<PathBuf> {
        let entities_dir = self.root.join("entities");
        std::fs::create_dir_all(&entities_dir)?;

        let filename = format!("{}.md", entity.name);
        let path = entities_dir.join(&filename);

        let mut content = String::new();
        content.push_str(&format!("# {}\n\n", entity.name));
        content.push_str(&format!("**类型**: {:?}\n\n", entity.entity_type));

        if !entity.description.is_empty() {
            content.push_str(&format!("## 描述\n\n{}\n\n", entity.description));
        }

        if !entity.attributes.is_empty() {
            content.push_str("## 属性\n\n");
            for (key, value) in &entity.attributes {
                content.push_str(&format!("- **{}**: {}\n", key, value));
            }
            content.push('\n');
        }

        content.push_str("## 相关剧情\n\n_待补充_\n");

        std::fs::write(&path, content)?;
        Ok(path)
    }

    /// 从对话文本自动生成 Wiki 页面
    pub fn auto_generate_pages(&self, dialogue: &str) -> Result<Vec<PathBuf>> {
        let entities = self.extract_entities(dialogue);
        let mut generated = Vec::new();

        for entity in &entities {
            // 检查是否已存在
            let entity_path = self.root.join("entities").join(format!("{}.md", entity.name));
            if !entity_path.exists() {
                let path = self.generate_entity_page(entity)?;
                generated.push(path);
            }
        }

        Ok(generated)
    }

    /// 智能加载：根据上下文关键词加载相关 Wiki 页面
    pub fn smart_load(&self, context: &str, genre: Option<&str>) -> Result<Vec<String>> {
        let mut pages = Vec::new();
        let context_lower = context.to_lowercase();

        // 1. 始终加载规则
        let rules = self.load_rules()?;
        if !rules.is_empty() {
            pages.push(rules);
        }

        // 2. 加载指定赛道
        if let Some(g) = genre {
            let genre_content = self.load_genre(g)?;
            if !genre_content.is_empty() {
                pages.push(genre_content);
            }
        }

        // 3. 根据上下文关键词加载实体页面
        let entities_dir = self.root.join("entities");
        if entities_dir.exists() {
            for entry in std::fs::read_dir(&entities_dir)? {
                let entry = entry?;
                let path = entry.path();
                if path.extension().and_then(|s| s.to_str()) == Some("md") {
                    let name = path.file_stem()
                        .and_then(|s| s.to_str())
                        .unwrap_or("")
                        .to_lowercase();

                    // 如果上下文包含实体名称，加载该页面
                    if !name.is_empty() && context_lower.contains(&name) {
                        if let Ok(content) = std::fs::read_to_string(&path) {
                            if !content.is_empty() {
                                pages.push(content);
                            }
                        }
                    }
                }
            }
        }

        // 4. 根据上下文关键词加载模板
        let keywords = ["写作", "润色", "大纲", "角色", "剧情", "世界观"];
        for keyword in &keywords {
            if context.contains(keyword) {
                let template_name = match *keyword {
                    "写作" => "writing",
                    "润色" => "polish",
                    "大纲" => "outline",
                    "角色" => "character",
                    "剧情" => "plot",
                    "世界观" => "worldview",
                    _ => continue,
                };
                let template = self.load_template(template_name)?;
                if !template.is_empty() && !pages.contains(&template) {
                    pages.push(template);
                }
            }
        }

        Ok(pages)
    }

    /// 读取指定实体页面
    pub fn read_entity(&self, name: &str) -> Result<Option<String>> {
        let path = self.root.join("entities").join(format!("{}.md", name));
        if path.exists() {
            Ok(Some(std::fs::read_to_string(path)?))
        } else {
            Ok(None)
        }
    }

    /// 更新实体页面
    pub fn update_entity(&self, entity: &Entity) -> Result<PathBuf> {
        let entities_dir = self.root.join("entities");
        std::fs::create_dir_all(&entities_dir)?;

        let filename = format!("{}.md", entity.name);
        let path = entities_dir.join(&filename);

        let mut content = String::new();
        content.push_str(&format!("# {}\n\n", entity.name));
        content.push_str(&format!("**类型**: {:?}\n\n", entity.entity_type));

        if !entity.description.is_empty() {
            content.push_str(&format!("## 描述\n\n{}\n\n", entity.description));
        }

        if !entity.attributes.is_empty() {
            content.push_str("## 属性\n\n");
            for (key, value) in &entity.attributes {
                content.push_str(&format!("- **{}**: {}\n", key, value));
            }
            content.push('\n');
        }

        content.push_str("## 相关剧情\n\n_待补充_\n");

        std::fs::write(&path, content)?;
        Ok(path)
    }
}
