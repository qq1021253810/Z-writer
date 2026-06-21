//! L1 全局设定层（Markdown 文件）

use std::path::PathBuf;
use crate::error::Result;

/// L1 Markdown 存储
pub struct L1MarkdownStore {
    workspace: PathBuf,
}

impl L1MarkdownStore {
    pub fn new(workspace: PathBuf) -> Self {
        Self { workspace }
    }

    /// 读取小说基础信息
    pub fn novel_info(&self) -> Result<String> {
        let path = self.workspace.join("novel.md");
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }

    /// 读取角色列表
    pub fn characters(&self) -> Result<Vec<String>> {
        let chars_dir = self.workspace.join("characters");
        if !chars_dir.exists() {
            return Ok(Vec::new());
        }

        let mut characters = Vec::new();
        for entry in std::fs::read_dir(chars_dir)? {
            let entry = entry?;
            let path = entry.path();
            if path.extension().and_then(|s| s.to_str()) == Some("md") {
                let content = std::fs::read_to_string(&path)?;
                characters.push(content);
            }
        }
        Ok(characters)
    }

    /// 保存章节
    pub fn save_chapter(&self, volume: usize, chapter: usize, content: &str) -> Result<()> {
        let chapters_dir = self.workspace.join("chapters");
        std::fs::create_dir_all(&chapters_dir)?;

        let filename = format!("vol{}-ch{:03}.md", volume, chapter);
        let path = chapters_dir.join(filename);
        std::fs::write(path, content)?;
        Ok(())
    }

    /// 读取章节
    pub fn read_chapter(&self, volume: usize, chapter: usize) -> Result<Option<String>> {
        let filename = format!("vol{}-ch{:03}.md", volume, chapter);
        let path = self.workspace.join("chapters").join(filename);
        
        if path.exists() {
            Ok(Some(std::fs::read_to_string(path)?))
        } else {
            Ok(None)
        }
    }

    /// 读取世界观设定
    pub fn worldview(&self) -> Result<String> {
        let path = self.workspace.join("worldview.md");
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }

    /// 读取大纲
    pub fn outline(&self) -> Result<String> {
        let path = self.workspace.join("outline.md");
        if path.exists() {
            Ok(std::fs::read_to_string(path)?)
        } else {
            Ok(String::new())
        }
    }
}
