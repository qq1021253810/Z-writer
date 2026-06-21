// L2 剧情记忆层测试

use zwriter_cli::rag::l2_memory_tree::*;
use std::path::PathBuf;

#[test]
fn test_memory_tree_create() {
    let tree = MemoryTree::new("测试小说");
    assert_eq!(tree.novel_id, "测试小说");
    assert!(tree.volumes.is_empty());
    assert!(tree.foreshadows.is_empty());
}

#[test]
fn test_memory_tree_add_volume() {
    let mut tree = MemoryTree::new("测试小说");
    
    let mut volume = VolumeSummary::new(1, "初入仙途", "少年林凡偶得神秘铁片，踏上修仙之路");
    volume.add_chapter(ChapterSummary::new(1, "神秘铁片", "林凡在青云门修炼，偶得神秘铁片"));
    volume.add_chapter(ChapterSummary::new(2, "系统激活", "神秘铁片激活无敌系统"));
    
    tree.add_volume(volume);
    
    assert_eq!(tree.volumes.len(), 1);
    assert_eq!(tree.volumes[0].chapters.len(), 2);
}

#[test]
fn test_memory_tree_get_chapter() {
    let mut tree = MemoryTree::new("测试小说");
    
    let mut volume = VolumeSummary::new(1, "初入仙途", "少年林凡的修仙之路");
    volume.add_chapter(ChapterSummary::new(1, "神秘铁片", "林凡偶得神秘铁片"));
    tree.add_volume(volume);
    
    let chapter = tree.get_chapter(1, 1);
    assert!(chapter.is_some());
    assert_eq!(chapter.unwrap().title, "神秘铁片");
    
    let missing = tree.get_chapter(1, 99);
    assert!(missing.is_none());
}

#[test]
fn test_memory_tree_foreshadow() {
    let mut tree = MemoryTree::new("测试小说");
    
    let foreshadow = Foreshadow::new("fs_001", "神秘铁片的来历", 1)
        .with_expected_resolve(10);
    tree.add_foreshadow(foreshadow);
    
    assert_eq!(tree.foreshadows.len(), 1);
    assert_eq!(tree.foreshadows[0].status, ForeshadowStatus::Active);
    
    // 更新伏笔状态
    let updated = tree.update_foreshadow("fs_001", ForeshadowStatus::Resolved, Some(10));
    assert!(updated);
    assert_eq!(tree.foreshadows[0].status, ForeshadowStatus::Resolved);
    assert_eq!(tree.foreshadows[0].actual_resolve, Some(10));
}

#[test]
fn test_memory_tree_recall_plot() {
    let mut tree = MemoryTree::new("测试小说");
    
    let mut volume = VolumeSummary::new(1, "初入仙途", "少年林凡的修仙之路");
    
    let mut ch1 = ChapterSummary::new(1, "神秘铁片", "林凡偶得神秘铁片");
    ch1.add_key_event("获得金手指");
    volume.add_chapter(ch1);
    
    let mut ch2 = ChapterSummary::new(2, "系统激活", "系统激活获得新手大礼包");
    ch2.add_key_event("实力恢复");
    volume.add_chapter(ch2);
    
    // 添加第3章，以便从第3章视角召回前两章
    let ch3 = ChapterSummary::new(3, "初次历练", "林凡开始第一次历练");
    volume.add_chapter(ch3);
    
    tree.add_volume(volume);
    
    let foreshadow = Foreshadow::new("fs_001", "神秘铁片的来历", 1);
    tree.add_foreshadow(foreshadow);
    
    // 从第3章视角召回，应该能召回第1、2章
    let recall = tree.recall_plot(3, 5);
    assert!(recall.contains("初入仙途"));
    assert!(recall.contains("神秘铁片"));
    assert!(recall.contains("系统激活"));
    assert!(recall.contains("神秘铁片的来历"));
}

#[test]
fn test_memory_tree_save_load() {
    let mut tree = MemoryTree::new("测试小说");
    
    let mut volume = VolumeSummary::new(1, "初入仙途", "少年林凡的修仙之路");
    volume.add_chapter(ChapterSummary::new(1, "神秘铁片", "林凡偶得神秘铁片"));
    tree.add_volume(volume);
    
    let foreshadow = Foreshadow::new("fs_001", "神秘铁片的来历", 1);
    tree.add_foreshadow(foreshadow);
    
    let path = PathBuf::from("./test_memory_tree.json");
    tree.save(&path).unwrap();
    
    let loaded = MemoryTree::load(&path).unwrap();
    assert_eq!(loaded.novel_id, "测试小说");
    assert_eq!(loaded.volumes.len(), 1);
    assert_eq!(loaded.foreshadows.len(), 1);
    
    std::fs::remove_file(path).ok();
}

#[test]
fn test_memory_tree_generate_reports() {
    let mut tree = MemoryTree::new("测试小说");
    
    let mut volume = VolumeSummary::new(1, "初入仙途", "少年林凡的修仙之路");
    let mut ch1 = ChapterSummary::new(1, "神秘铁片", "林凡偶得神秘铁片");
    ch1.add_key_event("获得金手指");
    volume.add_chapter(ch1);
    tree.add_volume(volume);
    
    let foreshadow = Foreshadow::new("fs_001", "神秘铁片的来历", 1);
    tree.add_foreshadow(foreshadow);
    
    let foreshadow_report = tree.generate_foreshadow_report();
    assert!(foreshadow_report.contains("伏笔追踪表"));
    assert!(foreshadow_report.contains("神秘铁片的来历"));
    
    let timeline = tree.generate_timeline();
    assert!(timeline.contains("剧情时间线"));
    assert!(timeline.contains("初入仙途"));
    assert!(timeline.contains("神秘铁片"));
}
