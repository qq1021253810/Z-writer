//! Git 版本管理模块
//!
//! 为小说工作区提供 Git 版本管理能力：
//! - 自动初始化 Git 仓库
//! - 每次续写/修改自动提交
//! - 查看历史版本、对比差异
//!
//! 实现策略：
//! - 优先使用系统 git 二进制（最可靠）
//! - 如果系统没有 git，提供友好的提示信息
//! - 未来可集成 gix 纯 Rust 实现作为 fallback

use std::path::{Path, PathBuf};
use std::process::Command;
use crate::error::{AppError, Result};

/// Git 仓库管理器
pub struct GitManager {
    repo_path: PathBuf,
    git_available: bool,
}

impl GitManager {
    /// 初始化新的 Git 仓库（如果已存在则打开）
    pub fn init(repo_path: &Path) -> Result<Self> {
        let git_available = Self::check_git_available();
        
        let git_dir = repo_path.join(".git");
        if !git_dir.exists() && git_available {
            Command::new("git")
                .args(["init"])
                .current_dir(repo_path)
                .output()
                .map_err(|e| AppError::Git(format!("初始化 Git 仓库失败: {}", e)))?;
            
            // 设置默认分支名
            Command::new("git")
                .args(["branch", "-M", "main"])
                .current_dir(repo_path)
                .output()
                .map_err(|e| AppError::Git(format!("设置分支名失败: {}", e)))?;
            
            // 配置用户信息
            Command::new("git")
                .args(["config", "user.name", "Z-Writer"])
                .current_dir(repo_path)
                .output()
                .ok();
            
            Command::new("git")
                .args(["config", "user.email", "zwriter@local"])
                .current_dir(repo_path)
                .output()
                .ok();
        }
        
        Ok(Self {
            repo_path: repo_path.to_path_buf(),
            git_available,
        })
    }

    /// 打开已存在的 Git 仓库
    pub fn open(repo_path: &Path) -> Result<Self> {
        let git_available = Self::check_git_available();
        
        if !repo_path.join(".git").exists() {
            return Err(AppError::Git("不是 Git 仓库".to_string()));
        }
        
        Ok(Self {
            repo_path: repo_path.to_path_buf(),
            git_available,
        })
    }

    /// 添加所有变更并提交
    pub fn add_and_commit(&self, message: &str) -> Result<String> {
        if !self.git_available {
            return Err(AppError::Git("Git 未安装，无法进行版本管理".to_string()));
        }
        
        // git add -A
        Command::new("git")
            .args(["add", "-A"])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("git add 失败: {}", e)))?;
        
        // git commit -m "message"
        let output = Command::new("git")
            .args(["commit", "-m", message, "--allow-empty"])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("git commit 失败: {}", e)))?;
        
        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            // 如果没有变更，不算错误
            if stderr.contains("nothing to commit") {
                return Ok("no changes".to_string());
            }
            return Err(AppError::Git(format!("git commit 失败: {}", stderr)));
        }
        
        // 获取最新 commit hash
        let output = Command::new("git")
            .args(["rev-parse", "--short", "HEAD"])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("获取 commit hash 失败: {}", e)))?;
        
        Ok(String::from_utf8_lossy(&output.stdout).trim().to_string())
    }

    /// 获取提交历史
    pub fn log(&self, max_count: usize) -> Result<Vec<CommitInfo>> {
        if !self.git_available {
            return Ok(Vec::new());
        }
        
        let output = Command::new("git")
            .args([
                "log",
                &format!("-{}", max_count),
                "--format=%H|%s|%an|%at",
            ])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("git log 失败: {}", e)))?;
        
        if !output.status.success() {
            return Ok(Vec::new()); // 可能还没有提交
        }
        
        let stdout = String::from_utf8_lossy(&output.stdout);
        let commits = stdout.lines()
            .filter(|line| !line.is_empty())
            .filter_map(|line| {
                let parts: Vec<&str> = line.splitn(4, '|').collect();
                if parts.len() >= 4 {
                    Some(CommitInfo {
                        id: parts[0].to_string(),
                        message: parts[1].to_string(),
                        author: parts[2].to_string(),
                        timestamp: parts[3].parse().unwrap_or(0),
                    })
                } else {
                    None
                }
            })
            .collect();
        
        Ok(commits)
    }

    /// 获取工作区状态
    pub fn status(&self) -> Result<RepoStatus> {
        if !self.git_available {
            return Ok(RepoStatus {
                modified: Vec::new(),
                added: Vec::new(),
                deleted: Vec::new(),
            });
        }
        
        let output = Command::new("git")
            .args(["status", "--porcelain"])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("git status 失败: {}", e)))?;
        
        let stdout = String::from_utf8_lossy(&output.stdout);
        let mut status = RepoStatus {
            modified: Vec::new(),
            added: Vec::new(),
            deleted: Vec::new(),
        };
        
        for line in stdout.lines() {
            if line.len() < 4 {
                continue;
            }
            let flag = &line[..2];
            let file = line[3..].to_string();
            
            match flag.trim() {
                "M" | "MM" => status.modified.push(file),
                "A" | "AM" => status.added.push(file),
                "D" => status.deleted.push(file),
                "??" => status.added.push(file),
                _ => {}
            }
        }
        
        Ok(status)
    }

    /// 查看某个提交的 diff
    pub fn diff(&self, commit_id: &str) -> Result<String> {
        if !self.git_available {
            return Err(AppError::Git("Git 未安装".to_string()));
        }
        
        let output = Command::new("git")
            .args(["diff", commit_id, "--stat"])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("git diff 失败: {}", e)))?;
        
        Ok(String::from_utf8_lossy(&output.stdout).to_string())
    }

    /// 恢复到某个版本
    pub fn checkout(&self, commit_id: &str) -> Result<()> {
        if !self.git_available {
            return Err(AppError::Git("Git 未安装".to_string()));
        }
        
        let output = Command::new("git")
            .args(["checkout", commit_id, "--", "."])
            .current_dir(&self.repo_path)
            .output()
            .map_err(|e| AppError::Git(format!("git checkout 失败: {}", e)))?;
        
        if !output.status.success() {
            let stderr = String::from_utf8_lossy(&output.stderr);
            return Err(AppError::Git(format!("git checkout 失败: {}", stderr)));
        }
        
        Ok(())
    }

    /// 检查系统是否安装了 git
    pub fn check_git_available() -> bool {
        Command::new("git")
            .args(["--version"])
            .output()
            .map(|o| o.status.success())
            .unwrap_or(false)
    }

    /// 检查是否是 Git 仓库
    pub fn is_git_repo(path: &Path) -> bool {
        path.join(".git").exists()
    }

    /// 获取仓库路径
    pub fn repo_path(&self) -> &Path {
        &self.repo_path
    }

    /// 检查 git 是否可用
    pub fn is_available(&self) -> bool {
        self.git_available
    }
}

/// 提交信息
#[derive(Debug, Clone)]
pub struct CommitInfo {
    pub id: String,
    pub message: String,
    pub author: String,
    pub timestamp: i64,
}

/// 仓库状态
#[derive(Debug, Clone)]
pub struct RepoStatus {
    pub modified: Vec<String>,
    pub added: Vec<String>,
    pub deleted: Vec<String>,
}
