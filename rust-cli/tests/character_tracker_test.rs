// 角色状态追踪测试

use zwriter_cli::context::character_tracker::{CharacterTracker, CharacterState, CharacterChange};
use std::path::PathBuf;

#[test]
fn test_character_state_new() {
    let state = CharacterState::new("林凡");
    assert_eq!(state.name, "林凡");
    assert_eq!(state.location, "未知");
    assert_eq!(state.emotional_state, "平静");
    assert!(state.power_level.is_none());
}

#[test]
fn test_character_state_builder() {
    let state = CharacterState::new("林凡")
        .with_location("青云门")
        .with_power_level("炼气三层")
        .add_relationship("赵天虎", "敌对")
        .add_item("神秘铁片");
    
    assert_eq!(state.location, "青云门");
    assert_eq!(state.power_level, Some("炼气三层".to_string()));
    assert_eq!(state.relationships.get("赵天虎"), Some(&"敌对".to_string()));
    assert_eq!(state.inventory, vec!["神秘铁片"]);
}

#[test]
fn test_tracker_update_character() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("林凡")
        .with_location("青云门")
        .with_power_level("炼气三层");
    
    tracker.update_character(state);
    
    let retrieved = tracker.get_character("林凡");
    assert!(retrieved.is_some());
    assert_eq!(retrieved.unwrap().location, "青云门");
}

#[test]
fn test_tracker_record_change() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("林凡");
    tracker.update_character(state);
    
    let change = CharacterChange::new(1, "修为", "炼气二层", "炼气三层");
    tracker.record_change("林凡", change);
    
    assert_eq!(tracker.history.get("林凡").unwrap().len(), 1);
    assert_eq!(tracker.history.get("林凡").unwrap()[0].change_type, "修为");
}

#[test]
fn test_build_character_context() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("林凡")
        .with_location("青云门")
        .with_power_level("炼气三层")
        .add_relationship("赵天虎", "敌对")
        .add_item("神秘铁片");
    
    tracker.update_character(state);
    
    let context = tracker.build_character_context();
    assert!(context.contains("【当前出场角色状态】"));
    assert!(context.contains("林凡"));
    assert!(context.contains("青云门"));
    assert!(context.contains("炼气三层"));
    assert!(context.contains("神秘铁片"));
}

#[test]
fn test_tracker_save_load() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("林凡")
        .with_location("青云门")
        .with_power_level("炼气三层");
    tracker.update_character(state);
    
    let change = CharacterChange::new(1, "修为", "炼气二层", "炼气三层");
    tracker.record_change("林凡", change);
    
    let path = PathBuf::from("./test_character_tracker.json");
    tracker.save(&path).unwrap();
    
    let loaded = CharacterTracker::load(&path).unwrap();
    assert!(loaded.get_character("林凡").is_some());
    assert_eq!(loaded.history.get("林凡").unwrap().len(), 1);
    
    std::fs::remove_file(path).ok();
}

#[test]
fn test_generate_report() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("林凡")
        .with_location("青云门")
        .with_power_level("炼气三层")
        .add_relationship("赵天虎", "敌对")
        .add_item("神秘铁片");
    
    tracker.update_character(state);
    
    let change = CharacterChange::new(1, "修为", "炼气二层", "炼气三层");
    tracker.record_change("林凡", change);
    
    let report = tracker.generate_report();
    assert!(report.contains("# 角色状态报告"));
    assert!(report.contains("## 林凡"));
    assert!(report.contains("**位置**: 青云门"));
    assert!(report.contains("**修为等级**: 炼气三层"));
    assert!(report.contains("神秘铁片"));
    assert!(report.contains("### 变化历史"));
}
