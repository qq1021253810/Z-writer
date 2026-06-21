//! 工作区管理模块

use std::path::{Path, PathBuf};
use crate::error::{AppError, Result};

/// 空世界观占位符
pub const EMPTY_WORLDVIEW: &str = "# 世界观设定\n\n待补充\n";
/// 空大纲占位符
pub const EMPTY_OUTLINE: &str = "# 大纲\n\n待补充\n";

/// 小说工作区
pub struct Workspace {
    root: PathBuf,
    name: String,
}

impl Workspace {
    /// 打开已有工作区
    pub fn open(root: PathBuf) -> Result<Self> {
        let novel_path = root.join("novel.md");
        if !novel_path.exists() {
            return Err(AppError::Workspace(format!(
                "工作区不存在: {}", root.display()
            )));
        }
        let name = root.file_name()
            .and_then(|n| n.to_str())
            .unwrap_or("unknown")
            .to_string();
        Ok(Self { root, name })
    }

    /// 创建新工作区
    pub fn create(base_path: &Path, name: &str) -> Result<Self> {
        let root = base_path.join(name);
        if root.exists() {
            return Err(AppError::Workspace(format!("小说 '{}' 已存在", name)));
        }

        // 创建目录结构
        std::fs::create_dir_all(root.join("characters"))?;
        std::fs::create_dir_all(root.join("chapters"))?;
        std::fs::create_dir_all(root.join("wiki"))?;

        // 创建 novel.md
        let novel_content = format!(
            "# {}\n\n- 类型: ?\n- 状态: 初始化\n- 简介: ?\n",
            name
        );
        std::fs::write(root.join("novel.md"), &novel_content)?;

        // 创建 worldview.md
        std::fs::write(root.join("worldview.md"), EMPTY_WORLDVIEW)?;

        // 创建 outline.md
        std::fs::write(root.join("outline.md"), EMPTY_OUTLINE)?;

        // 创建 memory_tree.json
        let memory_tree = serde_json::json!({
            "novel_id": name,
            "volumes": [],
            "foreshadows": []
        });
        std::fs::write(
            root.join("memory_tree.json"),
            serde_json::to_string_pretty(&memory_tree).expect("memory_tree.json 序列化失败"),
        )?;

        // 创建 vector_store 目录
        std::fs::create_dir_all(root.join("vector_store"))?;
        std::fs::write(root.join("vector_store").join("materials.json"), "[]")?;

        let name = name.to_string();
        Ok(Self { root, name })
    }

    pub fn root(&self) -> &Path {
        &self.root
    }

    pub fn name(&self) -> &str {
        &self.name
    }

    /// 读取小说基础信息
    pub fn novel_info(&self) -> Result<String> {
        let path = self.root.join("novel.md");
        Ok(std::fs::read_to_string(path)?)
    }

    /// 读取角色列表
    pub fn characters(&self) -> Result<Vec<String>> {
        let chars_dir = self.root.join("characters");
        if !chars_dir.exists() {
            return Ok(Vec::new());
        }
        let mut characters = Vec::new();
        for entry in std::fs::read_dir(chars_dir)? {
            let entry = entry?;
            let path = entry.path();
            if path.extension().and_then(|s| s.to_str()) == Some("md") {
                characters.push(std::fs::read_to_string(&path)?);
            }
        }
        Ok(characters)
    }

    /// 读取世界观
    pub fn worldview(&self) -> Result<String> {
        let path = self.root.join("worldview.md");
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }

    /// 读取大纲
    pub fn outline(&self) -> Result<String> {
        let path = self.root.join("outline.md");
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }

    /// 保存章节
    pub fn save_chapter(&self, chapter: usize, content: &str) -> Result<()> {
        let chapters_dir = self.root.join("chapters");
        std::fs::create_dir_all(&chapters_dir)?;
        let filename = format!("ch{:03}.md", chapter);
        std::fs::write(chapters_dir.join(filename), content)?;
        Ok(())
    }

    /// 读取章节
    pub fn read_chapter(&self, chapter: usize) -> Result<Option<String>> {
        let filename = format!("ch{:03}.md", chapter);
        let path = self.root.join("chapters").join(filename);
        if path.exists() {
            Ok(Some(std::fs::read_to_string(path)?))
        } else {
            Ok(None)
        }
    }

    /// 获取下一章编号
    pub fn next_chapter_num(&self) -> Result<usize> {
        let chapters_dir = self.root.join("chapters");
        if !chapters_dir.exists() {
            return Ok(1);
        }
        let mut max_num = 0usize;
        for entry in std::fs::read_dir(chapters_dir)? {
            let entry = entry?;
            let name = entry.file_name();
            let name = name.to_string_lossy();
            if let Some(num_str) = name.strip_prefix("ch").and_then(|s| s.strip_suffix(".md")) {
                if let Ok(num) = num_str.parse::<usize>() {
                    max_num = max_num.max(num);
                }
            }
        }
        Ok(max_num + 1)
    }

    /// 读取最近 N 章内容
    pub fn recent_chapters(&self, count: usize) -> Result<Vec<(usize, String)>> {
        let next = self.next_chapter_num()?;
        if next <= 1 {
            return Ok(Vec::new());
        }
        let start = next.saturating_sub(count);
        let mut chapters = Vec::new();
        for i in start..next {
            if let Some(content) = self.read_chapter(i)? {
                chapters.push((i, content));
            }
        }
        Ok(chapters)
    }

    /// 构建完整小说上下文（供 Agent 和 CLI 使用）
    ///
    /// 收集：小说信息 → 世界观 → 大纲 → 角色 → 最近章节
    /// 返回格式化的上下文字符串
    pub fn build_full_context(&self, recent_chapters_count: usize) -> Result<String> {
        let mut context = String::new();

        // 小说信息
        let novel_info = self.novel_info()?;
        context.push_str("【小说信息】\n");
        context.push_str(&novel_info);
        context.push_str("\n\n");

        // 世界观
        let worldview = self.worldview()?;
        if !worldview.is_empty() && worldview != EMPTY_WORLDVIEW {
            context.push_str("【世界观】\n");
            context.push_str(&worldview);
            context.push_str("\n\n");
        }

        // 大纲
        let outline = self.outline()?;
        if !outline.is_empty() && outline != EMPTY_OUTLINE {
            context.push_str("【大纲】\n");
            context.push_str(&outline);
            context.push_str("\n\n");
        }

        // 角色设定
        let characters = self.characters()?;
        if !characters.is_empty() {
            context.push_str("【角色设定】\n");
            for char_info in &characters {
                context.push_str(char_info);
                context.push('\n');
            }
            context.push('\n');
        }

        // 最近章节
        let recent = self.recent_chapters(recent_chapters_count)?;
        if !recent.is_empty() {
            context.push_str("【最近章节】\n");
            for (num, content) in &recent {
                context.push_str(&format!("第 {} 章:\n{}\n\n", num, content));
            }
        }

        Ok(context)
    }

    /// 列出所有工作区
    pub fn list_all(base_path: &Path) -> Result<Vec<String>> {
        if !base_path.exists() {
            return Ok(Vec::new());
        }
        let mut names = Vec::new();
        for entry in std::fs::read_dir(base_path)? {
            let entry = entry?;
            if entry.file_type()?.is_dir() {
                let name = entry.file_name();
                let name = name.to_string_lossy().to_string();
                if entry.path().join("novel.md").exists() {
                    names.push(name);
                }
            }
        }
        Ok(names)
    }
}
