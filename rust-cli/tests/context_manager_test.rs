// 分层上下文管理测试

use zwriter_cli::context::context_manager::{ContextManager, FidelityLevel};

#[test]
fn test_context_manager_new() {
    let cm = ContextManager::new(10000);
    assert_eq!(cm.token_budget, 10000);
    assert_eq!(cm.recent_limit, 10);
    assert!(cm.tier1_recent.is_empty());
    assert!(cm.tier2_compressed.is_empty());
    assert!(cm.tier3_permanent.is_empty());
}

#[test]
fn test_estimate_tokens() {
    let cm = ContextManager::new(10000);
    
    // 纯中文
    let chinese = "这是一段中文文本";
    let tokens = cm.estimate_tokens(chinese);
    assert!(tokens > 0);
    
    // 纯英文
    let english = "This is English text";
    let tokens_en = cm.estimate_tokens(english);
    assert!(tokens_en > 0);
    
    // 混合
    let mixed = "这是English混合文本";
    let tokens_mixed = cm.estimate_tokens(mixed);
    assert!(tokens_mixed > 0);
}

#[test]
fn test_classify_importance() {
    let cm = ContextManager::new(10000);
    
    // 包含关键词 -> Full
    let with_keyword = "主角的名字是林凡，他有一个金手指";
    assert_eq!(cm.classify_importance(with_keyword), FidelityLevel::Full);
    
    // 长文本 -> Compressed
    let long_text = "这是一段很长的文本".repeat(50);
    assert_eq!(cm.classify_importance(&long_text), FidelityLevel::Compressed);
    
    // 短文本 -> Placeholder
    let short_text = "普通对话";
    assert_eq!(cm.classify_importance(short_text), FidelityLevel::Placeholder);
}

#[test]
fn test_add_message_tier3() {
    let mut cm = ContextManager::new(10000);
    
    // 添加包含关键词的消息 -> Tier 3
    cm.add_message("user", "主角的名字是林凡");
    assert_eq!(cm.tier3_permanent.len(), 1);
    assert_eq!(cm.tier3_permanent[0].content, "主角的名字是林凡");
}

#[test]
fn test_add_message_tier2() {
    let mut cm = ContextManager::new(10000);
    
    // 添加长文本 -> Tier 2
    let long_text = "这是一段很长的文本".repeat(50);
    cm.add_message("user", &long_text);
    assert_eq!(cm.tier2_compressed.len(), 1);
}

#[test]
fn test_add_message_tier1() {
    let mut cm = ContextManager::new(10000);
    
    // 添加短文本 -> Tier 1
    cm.add_message("user", "普通对话");
    assert_eq!(cm.tier1_recent.len(), 1);
}

#[test]
fn test_tier1_overflow() {
    let mut cm = ContextManager::new(10000);
    cm.recent_limit = 3;
    
    // 添加超过限制的消息
    for i in 0..5 {
        cm.add_message("user", &format!("消息{}", i));
    }
    
    // Tier 1 应该只保留最近的 3 条
    assert_eq!(cm.tier1_recent.len(), 3);
    // 溢出的 2 条应该进入 Tier 2
    assert_eq!(cm.tier2_compressed.len(), 2);
}

#[test]
fn test_build_context() {
    let mut cm = ContextManager::new(10000);
    
    // 添加不同层级的消息
    cm.add_message("system", "世界观设定：这是一个修仙世界");
    cm.add_message("user", "继续写第5章");
    cm.add_message("assistant", "好的，我来继续写");
    
    let context = cm.build_context();
    
    // 应该包含所有层级
    assert!(context.contains("【关键设定】"));
    assert!(context.contains("【最近对话】"));
    assert!(context.contains("世界观设定"));
}

#[test]
fn test_compress_tier2() {
    let mut cm = ContextManager::new(10000);
    
    // 添加多条 Tier 2 消息
    let long1 = "长文本1".repeat(100);
    let long2 = "长文本2".repeat(100);
    cm.add_message("user", &long1);
    cm.add_message("user", &long2);
    assert_eq!(cm.tier2_compressed.len(), 2);
    
    // 压缩 Tier 2
    cm.compress_tier2("这是压缩后的摘要");
    assert_eq!(cm.tier2_compressed.len(), 1);
    assert_eq!(cm.tier2_compressed[0].content, "这是压缩后的摘要");
}

#[test]
fn test_current_tokens() {
    let mut cm = ContextManager::new(10000);
    
    cm.add_message("user", "短消息");
    cm.add_message("user", "主角的名字是林凡");
    
    let tokens = cm.current_tokens();
    assert!(tokens > 0);
}

#[test]
fn test_clear() {
    let mut cm = ContextManager::new(10000);
    
    cm.add_message("user", "消息1");
    cm.add_message("user", "主角的名字是林凡");
    
    assert!(!cm.tier1_recent.is_empty() || !cm.tier3_permanent.is_empty());
    
    cm.clear();
    
    assert!(cm.tier1_recent.is_empty());
    assert!(cm.tier2_compressed.is_empty());
    assert!(cm.tier3_permanent.is_empty());
}
