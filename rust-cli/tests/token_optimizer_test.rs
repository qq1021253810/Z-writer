// Token 优化器测试

use zwriter_cli::context::token_optimizer::TokenOptimizer;

#[test]
fn test_lossless_compression() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "这是  一段   测试    文本";
    let compressed = optimizer.lossless_compression(text);
    
    assert_eq!(compressed, "这是 一段 测试 文本");
}

#[test]
fn test_chinese_stopword_filter() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "这是一个测试，他们的对话很精彩";
    let filtered = optimizer.chinese_stopword_filter(text);
    
    assert!(!filtered.contains("一个"));
    assert!(!filtered.contains("他们"));
}

#[test]
fn test_extractive_compression() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "这是第一句。这是第二句包含关键词重要。这是第三句。这是第四句包含伏笔。";
    let compressed = optimizer.extractive_compression(text, 0.5);
    
    // 应该保留包含关键词的句子
    assert!(compressed.contains("重要") || compressed.contains("伏笔"));
}

#[test]
fn test_extractive_compression_empty() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "";
    let compressed = optimizer.extractive_compression(text, 0.5);
    
    assert_eq!(compressed, "");
}

#[test]
fn test_extractive_compression_full_ratio() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "这是第一句。这是第二句。这是第三句。";
    let compressed = optimizer.extractive_compression(text, 1.0);
    
    // ratio >= 1.0 应该返回原文
    assert_eq!(compressed, text);
}

#[test]
fn test_estimate_tokens_chinese() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "这是中文测试";
    let tokens = optimizer.estimate_tokens(text);
    
    // 6 个中文字符，约 4 个 token
    assert_eq!(tokens, 4);
}

#[test]
fn test_estimate_tokens_english() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "hello world";
    let tokens = optimizer.estimate_tokens(text);
    
    // 11 个英文字符，约 2 个 token
    assert_eq!(tokens, 2);
}

#[test]
fn test_estimate_tokens_mixed() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "中文English混合";
    let tokens = optimizer.estimate_tokens(text);
    
    // 3 中文 + 7 英文 = 4.5 + 5.25 ≈ 9
    assert!(tokens > 0);
}

#[test]
fn test_smart_compress_within_budget() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "这是短文本";
    let compressed = optimizer.smart_compress(text, 10);
    
    // 在预算内应该返回原文
    assert_eq!(compressed, text);
}

#[test]
fn test_smart_compress_over_budget() {
    let optimizer = TokenOptimizer::new(5);
    
    let text = "这是  一段   很长   的   文本   内容";
    let compressed = optimizer.smart_compress(text, 100);
    
    // 应该进行压缩
    assert!(compressed.len() < text.len());
}

#[test]
fn test_extractive_compression_preserves_keywords() {
    let optimizer = TokenOptimizer::new(1000);
    
    // 测试包含关键词的句子会被优先保留
    let text = "普通句子一。这是重要的设定。普通句子二。这里有伏笔。普通句子三。";
    let compressed = optimizer.extractive_compression(text, 0.4);
    
    // 应该保留包含"重要"和"伏笔"的句子
    assert!(compressed.contains("重要") || compressed.contains("伏笔"));
}

#[test]
fn test_extractive_compression_preserves_dialogue() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "描述性文字。他说：\"你好世界\"。更多描述。";
    let compressed = optimizer.extractive_compression(text, 0.5);
    
    // 应该保留包含对话的句子
    assert!(compressed.contains("你好世界") || compressed.contains("他说"));
}

#[test]
fn test_extractive_compression_preserves_numbers() {
    let optimizer = TokenOptimizer::new(1000);
    
    let text = "普通描述。他有100个金币。更多描述。";
    let compressed = optimizer.extractive_compression(text, 0.5);
    
    // 应该保留包含数字的句子
    assert!(compressed.contains("100"));
}
