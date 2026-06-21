// 集成测试 - 验证基本功能

use zwriter_cli::config::AppConfig;
use zwriter_cli::workspace::Workspace;
use std::path::PathBuf;
use std::sync::atomic::{AtomicUsize, Ordering};

static COUNTER: AtomicUsize = AtomicUsize::new(0);

fn unique_test_path() -> PathBuf {
    let id = COUNTER.fetch_add(1, Ordering::SeqCst);
    let tid = std::thread::current().id();
    PathBuf::from(format!("./test_ws_{:?}_{}", tid, id))
}

fn cleanup(path: &PathBuf) {
    let _ = std::fs::remove_dir_all(path);
}

#[test]
fn test_create_workspace() {
    let base = unique_test_path();
    let ws = Workspace::create(&base, "测试小说").unwrap();
    assert_eq!(ws.name(), "测试小说");
    assert!(ws.root().join("novel.md").exists());
    assert!(ws.root().join("worldview.md").exists());
    assert!(ws.root().join("outline.md").exists());
    assert!(ws.root().join("memory_tree.json").exists());
    assert!(ws.root().join("characters").exists());
    assert!(ws.root().join("chapters").exists());
    cleanup(&base);
}

#[test]
fn test_open_workspace() {
    let base = unique_test_path();
    Workspace::create(&base, "测试小说").unwrap();
    let ws = Workspace::open(base.join("测试小说")).unwrap();
    assert_eq!(ws.name(), "测试小说");
    cleanup(&base);
}

#[test]
fn test_list_workspaces() {
    let base = unique_test_path();
    Workspace::create(&base, "小说A").unwrap();
    Workspace::create(&base, "小说B").unwrap();
    let names = Workspace::list_all(&base).unwrap();
    assert_eq!(names.len(), 2);
    assert!(names.contains(&"小说A".to_string()));
    assert!(names.contains(&"小说B".to_string()));
    cleanup(&base);
}

#[test]
fn test_save_and_read_chapter() {
    let base = unique_test_path();
    let ws = Workspace::create(&base, "测试小说").unwrap();
    
    ws.save_chapter(1, "第一章内容").unwrap();
    ws.save_chapter(2, "第二章内容").unwrap();
    
    let ch1 = ws.read_chapter(1).unwrap();
    assert_eq!(ch1, Some("第一章内容".to_string()));
    
    let ch3 = ws.read_chapter(3).unwrap();
    assert_eq!(ch3, None);
    
    cleanup(&base);
}

#[test]
fn test_next_chapter_num() {
    let base = unique_test_path();
    let ws = Workspace::create(&base, "测试小说").unwrap();
    
    assert_eq!(ws.next_chapter_num().unwrap(), 1);
    
    ws.save_chapter(1, "第一章").unwrap();
    assert_eq!(ws.next_chapter_num().unwrap(), 2);
    
    ws.save_chapter(2, "第二章").unwrap();
    assert_eq!(ws.next_chapter_num().unwrap(), 3);
    
    cleanup(&base);
}

#[test]
fn test_recent_chapters() {
    let base = unique_test_path();
    let ws = Workspace::create(&base, "测试小说").unwrap();
    
    ws.save_chapter(1, "第一章").unwrap();
    ws.save_chapter(2, "第二章").unwrap();
    ws.save_chapter(3, "第三章").unwrap();
    
    let recent = ws.recent_chapters(2).unwrap();
    assert_eq!(recent.len(), 2);
    assert_eq!(recent[0].0, 2);
    assert_eq!(recent[1].0, 3);
    
    cleanup(&base);
}

#[test]
fn test_novel_info() {
    let base = unique_test_path();
    let ws = Workspace::create(&base, "测试小说").unwrap();
    let info = ws.novel_info().unwrap();
    assert!(info.contains("测试小说"));
    cleanup(&base);
}

#[test]
fn test_config_default() {
    let config = AppConfig::default();
    assert_eq!(config.ollama_url, "http://localhost:11434");
    assert_eq!(config.chat_model, "qwen3:14b");
    assert_eq!(config.token_budget, 100_000);
}

#[test]
fn test_config_save_and_load() {
    let config = AppConfig::default();
    let path = PathBuf::from("./test_config_unique.toml");
    config.save(&path).unwrap();
    
    let loaded = AppConfig::load(&path).unwrap();
    assert_eq!(loaded.ollama_url, config.ollama_url);
    assert_eq!(loaded.chat_model, config.chat_model);
    
    let _ = std::fs::remove_file(&path);
}
