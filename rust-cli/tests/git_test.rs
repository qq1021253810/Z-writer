// Git 集成测试

use zwriter_cli::git::GitManager;
use zwriter_cli::workspace::Workspace;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicUsize, Ordering};

static COUNTER: AtomicUsize = AtomicUsize::new(0);

fn unique_test_path() -> PathBuf {
    let id = COUNTER.fetch_add(1, Ordering::SeqCst);
    let tid = std::thread::current().id();
    PathBuf::from(format!("./test_git_{:?}_{}", tid, id))
}

fn cleanup(path: &PathBuf) {
    let _ = std::fs::remove_dir_all(path);
}

/// 清理并创建测试工作区，避免并行测试残留目录冲突
fn fresh_workspace(base: &Path, name: &str) -> Workspace {
    let root = base.join(name);
    let _ = std::fs::remove_dir_all(&root);
    Workspace::create(base, name).unwrap()
}

#[test]
fn test_git_init() {
    let base = unique_test_path();
    let ws = fresh_workspace(&base, "测试小说");
    
    // 检查 git 是否可用
    if !GitManager::check_git_available() {
        println!("Git 未安装，跳过测试");
        cleanup(&base);
        return;
    }
    
    let git = GitManager::init(ws.root()).unwrap();
    assert!(git.is_available());
    assert!(ws.root().join(".git").exists());
    
    cleanup(&base);
}

#[test]
fn test_git_add_and_commit() {
    let base = unique_test_path();
    let ws = fresh_workspace(&base, "测试小说");
    
    if !GitManager::check_git_available() {
        println!("Git 未安装，跳过测试");
        cleanup(&base);
        return;
    }
    
    let git = GitManager::init(ws.root()).unwrap();
    
    // 初始提交
    let commit1 = git.add_and_commit("初始提交").unwrap();
    assert!(!commit1.is_empty());
    assert_ne!(commit1, "no changes");
    
    // 修改文件后提交
    ws.save_chapter(1, "第一章内容").unwrap();
    let commit2 = git.add_and_commit("添加第一章").unwrap();
    assert!(!commit2.is_empty());
    assert_ne!(commit2, commit1);
    
    cleanup(&base);
}

#[test]
fn test_git_log() {
    let base = unique_test_path();
    let ws = fresh_workspace(&base, "测试小说");
    
    if !GitManager::check_git_available() {
        println!("Git 未安装，跳过测试");
        cleanup(&base);
        return;
    }
    
    let git = GitManager::init(ws.root()).unwrap();
    
    // 创建多个提交
    git.add_and_commit("第一次提交").unwrap();
    ws.save_chapter(1, "第一章").unwrap();
    git.add_and_commit("添加第一章").unwrap();
    ws.save_chapter(2, "第二章").unwrap();
    git.add_and_commit("添加第二章").unwrap();
    
    // 查看历史
    let log = git.log(10).unwrap();
    assert_eq!(log.len(), 3);
    assert_eq!(log[0].message, "添加第二章");
    assert_eq!(log[1].message, "添加第一章");
    assert_eq!(log[2].message, "第一次提交");
    
    cleanup(&base);
}

#[test]
fn test_git_status() {
    let base = unique_test_path();
    let ws = fresh_workspace(&base, "测试小说");
    
    if !GitManager::check_git_available() {
        println!("Git 未安装，跳过测试");
        cleanup(&base);
        return;
    }
    
    let git = GitManager::init(ws.root()).unwrap();
    
    // 初始提交
    git.add_and_commit("初始提交").unwrap();
    
    // 添加新文件
    ws.save_chapter(1, "第一章").unwrap();
    
    // 检查状态
    let status = git.status().unwrap();
    assert!(!status.added.is_empty() || !status.modified.is_empty());
    
    cleanup(&base);
}

#[test]
fn test_git_diff() {
    let base = unique_test_path();
    let ws = fresh_workspace(&base, "测试小说");
    
    if !GitManager::check_git_available() {
        println!("Git 未安装，跳过测试");
        cleanup(&base);
        return;
    }
    
    let git = GitManager::init(ws.root()).unwrap();
    
    // 初始提交
    git.add_and_commit("初始提交").unwrap();
    
    // 添加文件
    ws.save_chapter(1, "第一章内容").unwrap();
    let _commit = git.add_and_commit("添加第一章").unwrap();
    
    // 查看 diff（对比初始提交和当前）
    let log = git.log(10).unwrap();
    let first_commit = &log[log.len() - 1].id;
    let diff = git.diff(first_commit).unwrap();
    assert!(!diff.is_empty());
    
    cleanup(&base);
}
