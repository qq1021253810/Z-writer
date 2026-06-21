// Rolling summary tests

use zwriter_cli::context::rolling_summary::{RollingSummary, RollingChapterSummary};
use std::path::PathBuf;

#[test]
fn test_rolling_summary_new() {
    let rs = RollingSummary::new();
    assert!(rs.chapter_summaries.is_empty());
    assert!(rs.compressed_summaries.is_empty());
    assert!(rs.style_anchor.is_empty());
    assert_eq!(rs.compression_threshold, 10);
}

#[test]
fn test_add_chapter_summary() {
    let mut rs = RollingSummary::new();
    let summary = RollingChapterSummary::new(1, "test_title", "test_summary")
        .add_key_event("event1");
    rs.add_chapter_summary(summary);
    assert_eq!(rs.chapter_summaries.len(), 1);
    assert_eq!(rs.chapter_summaries[0].chapter_num, 1);
}

#[test]
fn test_update_style_anchor() {
    let mut rs = RollingSummary::new();
    rs.update_style_anchor("test_style_passage");
    assert_eq!(rs.style_anchor, "test_style_passage");
}

#[test]
fn test_needs_compression() {
    let mut rs = RollingSummary::new();
    rs.compression_threshold = 3;
    
    assert!(!rs.needs_compression());
    
    for i in 1..=3 {
        rs.add_chapter_summary(RollingChapterSummary::new(i, &format!("ch{}", i), "summary"));
    }
    
    assert!(rs.needs_compression());
}

#[test]
fn test_compress_chapters() {
    let mut rs = RollingSummary::new();
    
    for i in 1..=5 {
        rs.add_chapter_summary(RollingChapterSummary::new(i, &format!("ch{}", i), "summary"));
    }
    
    rs.compress_chapters("overall", "plot_progress", "character_arcs");
    
    assert!(rs.chapter_summaries.is_empty());
    assert_eq!(rs.compressed_summaries.len(), 1);
    assert!(rs.compressed_summaries[0].volume_range.contains("1"));
    assert!(rs.compressed_summaries[0].volume_range.contains("5"));
    assert_eq!(rs.compressed_summaries[0].overall_summary, "overall");
}

#[test]
fn test_compress_empty() {
    let mut rs = RollingSummary::new();
    rs.compress_chapters("overall", "plot", "chars");
    assert!(rs.compressed_summaries.is_empty());
}

#[test]
fn test_build_continuation_context() {
    let mut rs = RollingSummary::new();
    
    // Add some chapter summaries first (compress_chapters requires non-empty chapter_summaries)
    for i in 1..=5 {
        rs.add_chapter_summary(RollingChapterSummary::new(i, &format!("ch{}", i), "summary"));
    }
    
    // Compress them into overall summary
    rs.compress_chapters("previous_summary", "plot_progress", "char_growth");
    
    // Add recent chapter summaries
    rs.add_chapter_summary(RollingChapterSummary::new(11, "title11", "summary11")
        .add_key_event("event1"));
    rs.add_chapter_summary(RollingChapterSummary::new(12, "title12", "summary12"));
    
    rs.update_style_anchor("style_passage");
    
    let context = rs.build_continuation_context(13);
    
    assert!(context.contains("previous_summary"), "Should contain compressed summary");
    assert!(context.contains("title11"), "Should contain chapter 11");
    assert!(context.contains("title12"), "Should contain chapter 12");
    assert!(context.contains("style_passage"), "Should contain style anchor");
}

#[test]
fn test_build_context_filters_current_chapter() {
    let mut rs = RollingSummary::new();
    
    rs.add_chapter_summary(RollingChapterSummary::new(1, "ch1_title", "summary1"));
    rs.add_chapter_summary(RollingChapterSummary::new(2, "ch2_title", "summary2"));
    rs.add_chapter_summary(RollingChapterSummary::new(3, "ch3_title", "summary3"));
    
    // From chapter 2 perspective, should only see chapter 1
    let context = rs.build_continuation_context(2);
    assert!(context.contains("ch1_title"));
    assert!(!context.contains("ch2_title"));
    assert!(!context.contains("ch3_title"));
}

#[test]
fn test_save_load() {
    let mut rs = RollingSummary::new();
    
    rs.add_chapter_summary(RollingChapterSummary::new(1, "title1", "summary1")
        .add_key_event("event1"));
    rs.update_style_anchor("style_passage");
    
    let path = PathBuf::from("./test_rolling_summary.json");
    rs.save(&path).unwrap();
    
    let loaded = RollingSummary::load(&path).unwrap();
    assert_eq!(loaded.chapter_summaries.len(), 1);
    assert_eq!(loaded.style_anchor, "style_passage");
    
    std::fs::remove_file(path).ok();
}

#[test]
fn test_generate_report() {
    let mut rs = RollingSummary::new();
    
    // Add chapter summaries first
    for i in 1..=3 {
        rs.add_chapter_summary(RollingChapterSummary::new(i, &format!("ch{}", i), "summary"));
    }
    
    // Compress them
    rs.compress_chapters("compressed_content", "plot", "chars");
    
    // Add new chapter summaries
    rs.add_chapter_summary(RollingChapterSummary::new(11, "chapter_title", "chapter_summary")
        .add_key_event("key_event"));
    rs.update_style_anchor("style_content");
    
    let report = rs.generate_report();
    assert!(report.contains("compressed_content"));
    assert!(report.contains("chapter_title"));
    assert!(report.contains("style_content"));
}
