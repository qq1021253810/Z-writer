//! L3 细节素材层（轻量向量库）
//! 注意：usearch 集成将在阶段三启用

use serde::{Deserialize, Serialize};
use std::path::Path;
use crate::error::Result;

/// 素材分类
#[derive(Debug, Clone, Serialize, Deserialize, PartialEq)]
pub enum MaterialCategory {
    Scenery,      // 场景描写
    Combat,       // 战斗描写
    Character,    // 人物描写
    Technique,    // 技能描写
    Inspiration,  // 灵感碎片
    Historical,   // 历史章节切片
}

/// 素材元数据
#[derive(Debug, Clone, Serialize, Deserialize, Default)]
pub struct MaterialMetadata {
    pub keywords: Vec<String>,
    pub characters: Vec<String>,
    pub locations: Vec<String>,
    pub tags: Vec<String>,
}

/// 素材
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct Material {
    pub id: String,
    pub content: String,
    pub category: MaterialCategory,
    pub source: String,
    pub embedding: Vec<f32>,
    pub metadata: MaterialMetadata,
    pub source_chapter: Option<usize>,
}

/// L3 素材存储（阶段三将集成 usearch）
pub struct L3MaterialStore {
    materials: Vec<Material>,
    index_path: std::path::PathBuf,
}

impl L3MaterialStore {
    pub fn new(index_path: std::path::PathBuf) -> Self {
        Self {
            materials: Vec::new(),
            index_path,
        }
    }

    /// 从文件加载
    pub fn load(path: &Path) -> Result<Self> {
        let materials_path = path.join("materials.json");
        
        if !materials_path.exists() {
            return Ok(Self::new(path.to_path_buf()));
        }

        let content = std::fs::read_to_string(&materials_path)?;
        let materials: Vec<Material> = serde_json::from_str(&content)?;
        
        Ok(Self {
            materials,
            index_path: path.to_path_buf(),
        })
    }

    /// 保存到文件
    pub fn save(&self) -> Result<()> {
        std::fs::create_dir_all(&self.index_path)?;
        
        let materials_path = self.index_path.join("materials.json");
        let content = serde_json::to_string_pretty(&self.materials)?;
        std::fs::write(materials_path, content)?;
        
        Ok(())
    }

    /// 添加素材
    pub fn add_material(&mut self, material: Material) {
        self.materials.push(material);
    }

    /// 搜索素材（简单余弦相似度 - 阶段三将替换为 usearch）
    pub fn search(&self, query_embedding: &[f32], top_k: usize) -> Vec<&Material> {
        let mut scored: Vec<(f32, &Material)> = self.materials.iter()
            .map(|m| {
                let score = cosine_similarity(query_embedding, &m.embedding);
                (score, m)
            })
            .collect();

        scored.sort_by(|a, b| b.0.partial_cmp(&a.0).expect("浮点数比较失败"));
        scored.into_iter().take(top_k).map(|(_, m)| m).collect()
    }

    /// 按分类搜索
    pub fn search_by_category(&self, category: &MaterialCategory, top_k: usize) -> Vec<&Material> {
        self.materials
            .iter()
            .filter(|m| &m.category == category)
            .take(top_k)
            .collect()
    }

    /// 按关键词搜索
    pub fn search_by_keywords(&self, keywords: &[&str], top_k: usize) -> Vec<&Material> {
        let mut scored: Vec<(usize, &Material)> = self.materials
            .iter()
            .map(|m| {
                let score = keywords.iter()
                    .filter(|k| m.metadata.keywords.iter().any(|mk| mk.contains(**k)))
                    .count();
                (score, m)
            })
            .filter(|(score, _)| *score > 0)
            .collect();

        scored.sort_by_key(|s| std::cmp::Reverse(s.0));
        scored.into_iter().take(top_k).map(|(_, m)| m).collect()
    }

    /// 获取所有素材
    pub fn materials(&self) -> &[Material] {
        &self.materials
    }

    /// 获取素材总数
    pub fn len(&self) -> usize {
        self.materials.len()
    }

    /// 是否为空
    pub fn is_empty(&self) -> bool {
        self.materials.is_empty()
    }

    /// 生成素材摘要（人类可读）
    pub fn generate_summary(&self) -> String {
        let mut summary = String::new();
        summary.push_str("# 素材库摘要\n\n");

        // 按分类统计
        let mut counts = std::collections::HashMap::new();
        for m in &self.materials {
            *counts.entry(format!("{:?}", m.category)).or_insert(0) += 1;
        }

        summary.push_str("## 分类统计\n\n");
        for (category, count) in counts.iter() {
            summary.push_str(&format!("- **{}**: {} 条\n", category, count));
        }
        summary.push('\n');

        // 最近添加的素材
        summary.push_str("## 最近素材\n\n");
        for m in self.materials.iter().rev().take(5) {
            let preview = if m.content.len() > 50 { 
                &m.content[..50] 
            } else { 
                &m.content 
            };
            summary.push_str(&format!("- [{:?}] {}\n", m.category, preview));
        }

        summary
    }
}

impl Material {
    /// 创建新素材
    pub fn new(id: &str, content: &str, category: MaterialCategory, source: &str, embedding: Vec<f32>) -> Self {
        Self {
            id: id.to_string(),
            content: content.to_string(),
            category,
            source: source.to_string(),
            embedding,
            metadata: MaterialMetadata::default(),
            source_chapter: None,
        }
    }

    /// 设置来源章节
    pub fn with_source_chapter(mut self, chapter: usize) -> Self {
        self.source_chapter = Some(chapter);
        self
    }

    /// 添加关键词
    pub fn with_keywords(mut self, keywords: Vec<String>) -> Self {
        self.metadata.keywords = keywords;
        self
    }

    /// 添加角色
    pub fn with_characters(mut self, characters: Vec<String>) -> Self {
        self.metadata.characters = characters;
        self
    }

    /// 添加地点
    pub fn with_locations(mut self, locations: Vec<String>) -> Self {
        self.metadata.locations = locations;
        self
    }

    /// 添加标签
    pub fn with_tags(mut self, tags: Vec<String>) -> Self {
        self.metadata.tags = tags;
        self
    }
}

/// 计算余弦相似度
pub fn cosine_similarity(a: &[f32], b: &[f32]) -> f32 {
    if a.len() != b.len() || a.is_empty() {
        return 0.0;
    }

    let dot_product: f32 = a.iter().zip(b.iter()).map(|(x, y)| x * y).sum();
    let norm_a: f32 = a.iter().map(|x| x * x).sum::<f32>().sqrt();
    let norm_b: f32 = b.iter().map(|x| x * x).sum::<f32>().sqrt();

    if norm_a == 0.0 || norm_b == 0.0 {
        return 0.0;
    }

    dot_product / (norm_a * norm_b)
}
