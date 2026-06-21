// 角色状态追踪测试

use zwriter_cli::context::character_tracker::{CharacterTracker, CharacterState, CharacterChange};
use std::path::PathBuf;

#[test]
fn test_character_state_new() {
    let state = CharacterState::new("陆远");
    assert_eq!(state.name, "陆远");
    assert_eq!(state.location, "未知");
    assert_eq!(state.emotional_state, "平静");
    assert!(state.capability_tier.is_none());
}

#[test]
fn test_character_state_builder() {
    let state = CharacterState::new("陆远")
        .with_location("星辰科技")
        .with_capability_tier("CEO")
        .add_relationship("赵天虎", "敌对")
        .add_item("核心技术专利");
    
    assert_eq!(state.location, "星辰科技");
    assert_eq!(state.capability_tier, Some("CEO".to_string()));
    assert_eq!(state.relationships.get("赵天虎"), Some(&"敌对".to_string()));
    assert_eq!(state.inventory, vec!["核心技术专利"]);
}

#[test]
fn test_tracker_update_character() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("陆远")
        .with_location("星辰科技")
        .with_capability_tier("CEO");
    
    tracker.update_character(state);
    
    let retrieved = tracker.get_character("陆远");
    assert!(retrieved.is_some());
    assert_eq!(retrieved.unwrap().location, "星辰科技");
}

#[test]
fn test_tracker_record_change() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("陆远");
    tracker.update_character(state);
    
    let change = CharacterChange::new(1, "地位", "初创者", "行业领袖");
    tracker.record_change("陆远", change);
    
    assert_eq!(tracker.history.get("陆远").unwrap().len(), 1);
    assert_eq!(tracker.history.get("陆远").unwrap()[0].change_type, "地位");
}

#[test]
fn test_build_character_context() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("陆远")
        .with_location("星辰科技")
        .with_capability_tier("CEO")
        .add_relationship("赵天虎", "敌对")
        .add_item("核心技术专利");
    
    tracker.update_character(state);
    
    let context = tracker.build_character_context();
    assert!(context.contains("【当前出场角色状态】"));
    assert!(context.contains("陆远"));
    assert!(context.contains("星辰科技"));
    assert!(context.contains("CEO"));
    assert!(context.contains("核心技术专利"));
}

#[test]
fn test_tracker_save_load() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("陆远")
        .with_location("星辰科技")
        .with_capability_tier("CEO");
    tracker.update_character(state);
    
    let change = CharacterChange::new(1, "地位", "初创者", "行业领袖");
    tracker.record_change("陆远", change);
    
    let path = PathBuf::from("./test_character_tracker.json");
    tracker.save(&path).unwrap();
    
    let loaded = CharacterTracker::load(&path).unwrap();
    assert!(loaded.get_character("陆远").is_some());
    assert_eq!(loaded.history.get("陆远").unwrap().len(), 1);
    
    std::fs::remove_file(path).ok();
}

#[test]
fn test_generate_report() {
    let mut tracker = CharacterTracker::new();
    let state = CharacterState::new("陆远")
        .with_location("星辰科技")
        .with_capability_tier("CEO")
        .add_relationship("赵天虎", "敌对")
        .add_item("核心技术专利");
    
    tracker.update_character(state);
    
    let change = CharacterChange::new(1, "地位", "初创者", "行业领袖");
    tracker.record_change("陆远", change);
    
    let report = tracker.generate_report();
    assert!(report.contains("# 角色状态报告"));
    assert!(report.contains("## 陆远"));
    assert!(report.contains("**位置**: 星辰科技"));
    assert!(report.contains("**能力评级**: CEO"));
    assert!(report.contains("核心技术专利"));
    assert!(report.contains("### 变化历史"));
}
