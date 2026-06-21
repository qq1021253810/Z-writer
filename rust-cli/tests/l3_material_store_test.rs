//! L3 细节素材层测试

use zwriter_cli::rag::l3_material_store::{L3MaterialStore, Material, MaterialCategory};
use std::path::PathBuf;

#[test]
fn test_l3_material_store_new() {
    let path = PathBuf::from("./test_l3_store");
    let store = L3MaterialStore::new(path);
    assert_eq!(store.len(), 0);
    assert!(store.is_empty());
}

#[test]
fn test_l3_add_material() {
    let path = PathBuf::from("./test_l3_store_add");
    let mut store = L3MaterialStore::new(path);
    
    let material = Material::new(
        "mat_001",
        "雨夜厮杀，刀光剑影",
        MaterialCategory::Combat,
        "用户输入",
        vec![0.1, 0.2, 0.3],
    );
    
    store.add_material(material);
    assert_eq!(store.len(), 1);
    assert!(!store.is_empty());
}

#[test]
fn test_l3_material_builder() {
    let material = Material::new(
        "mat_002",
        "青云山巅，云雾缭绕",
        MaterialCategory::Scenery,
        "用户输入",
        vec![0.4, 0.5, 0.6],
    )
    .with_source_chapter(3)
    .with_keywords(vec!["青云山".to_string(), "云雾".to_string()])
    .with_characters(vec!["林凡".to_string()])
    .with_locations(vec!["青云山".to_string()])
    .with_tags(vec!["场景描写".to_string()]);
    
    assert_eq!(material.id, "mat_002");
    assert_eq!(material.source_chapter, Some(3));
    assert_eq!(material.metadata.keywords.len(), 2);
    assert_eq!(material.metadata.characters.len(), 1);
    assert_eq!(material.metadata.locations.len(), 1);
    assert_eq!(material.metadata.tags.len(), 1);
}

#[test]
fn test_l3_search_by_embedding() {
    let path = PathBuf::from("./test_l3_search");
    let mut store = L3MaterialStore::new(path);
    
    // 添加多个素材
    let mat1 = Material::new(
        "mat_001",
        "雨夜厮杀",
        MaterialCategory::Combat,
        "用户输入",
        vec![0.9, 0.8, 0.7],
    );
    
    let mat2 = Material::new(
        "mat_002",
        "山巅云雾",
        MaterialCategory::Scenery,
        "用户输入",
        vec![0.1, 0.2, 0.3],
    );
    
    let mat3 = Material::new(
        "mat_003",
        "剑光如虹",
        MaterialCategory::Combat,
        "用户输入",
        vec![0.85, 0.75, 0.65],
    );
    
    store.add_material(mat1);
    store.add_material(mat2);
    store.add_material(mat3);
    
    // 搜索与 mat1 相似的素材
    let query = vec![0.88, 0.78, 0.68];
    let results = store.search(&query, 2);
    
    assert_eq!(results.len(), 2);
    // mat1 和 mat3 应该排在前面（与 query 更相似）
    assert!(results.iter().any(|m| m.id == "mat_001"));
    assert!(results.iter().any(|m| m.id == "mat_003"));
}

#[test]
fn test_l3_search_by_category() {
    let path = PathBuf::from("./test_l3_category");
    let mut store = L3MaterialStore::new(path);
    
    let mat1 = Material::new("mat_001", "战斗1", MaterialCategory::Combat, "用户输入", vec![0.1]);
    let mat2 = Material::new("mat_002", "场景1", MaterialCategory::Scenery, "用户输入", vec![0.2]);
    let mat3 = Material::new("mat_003", "战斗2", MaterialCategory::Combat, "用户输入", vec![0.3]);
    
    store.add_material(mat1);
    store.add_material(mat2);
    store.add_material(mat3);
    
    let combat_results = store.search_by_category(&MaterialCategory::Combat, 10);
    assert_eq!(combat_results.len(), 2);
    
    let scenery_results = store.search_by_category(&MaterialCategory::Scenery, 10);
    assert_eq!(scenery_results.len(), 1);
}

#[test]
fn test_l3_search_by_keywords() {
    let path = PathBuf::from("./test_l3_keywords");
    let mut store = L3MaterialStore::new(path);
    
    let mat1 = Material::new(
        "mat_001",
        "青云山战斗",
        MaterialCategory::Combat,
        "用户输入",
        vec![0.1],
    )
    .with_keywords(vec!["青云山".to_string(), "战斗".to_string()]);
    
    let mat2 = Material::new(
        "mat_002",
        "海底探险",
        MaterialCategory::Scenery,
        "用户输入",
        vec![0.2],
    )
    .with_keywords(vec!["海底".to_string(), "探险".to_string()]);
    
    let mat3 = Material::new(
        "mat_003",
        "青云山论剑",
        MaterialCategory::Combat,
        "用户输入",
        vec![0.3],
    )
    .with_keywords(vec!["青云山".to_string(), "论剑".to_string()]);
    
    store.add_material(mat1);
    store.add_material(mat2);
    store.add_material(mat3);
    
    // 搜索包含"青云山"的素材
    let results = store.search_by_keywords(&["青云山"], 10);
    assert_eq!(results.len(), 2);
    assert!(results.iter().any(|m| m.id == "mat_001"));
    assert!(results.iter().any(|m| m.id == "mat_003"));
    
    // 搜索包含"战斗"的素材
    let results = store.search_by_keywords(&["战斗"], 10);
    assert_eq!(results.len(), 1);
    assert_eq!(results[0].id, "mat_001");
}

#[test]
fn test_l3_save_and_load() {
    let path = PathBuf::from("./test_l3_persist");
    
    // 清理旧数据
    if path.exists() {
        std::fs::remove_dir_all(&path).ok();
    }
    
    // 创建并保存
    {
        let mut store = L3MaterialStore::new(path.clone());
        
        let mat = Material::new(
            "mat_001",
            "测试素材",
            MaterialCategory::Combat,
            "用户输入",
            vec![0.1, 0.2, 0.3],
        )
        .with_source_chapter(5)
        .with_keywords(vec!["测试".to_string()]);
        
        store.add_material(mat);
        store.save().unwrap();
    }
    
    // 加载并验证
    {
        let store = L3MaterialStore::load(&path).unwrap();
        assert_eq!(store.len(), 1);
        
        let mat = &store.materials()[0];
        assert_eq!(mat.id, "mat_001");
        assert_eq!(mat.content, "测试素材");
        assert_eq!(mat.source_chapter, Some(5));
        assert_eq!(mat.metadata.keywords, vec!["测试"]);
    }
    
    // 清理
    std::fs::remove_dir_all(&path).ok();
}

#[test]
fn test_l3_generate_summary() {
    let path = PathBuf::from("./test_l3_summary");
    let mut store = L3MaterialStore::new(path);
    
    let mat1 = Material::new("mat_001", "战斗素材一", MaterialCategory::Combat, "用户输入", vec![0.1]);
    let mat2 = Material::new("mat_002", "战斗素材二", MaterialCategory::Combat, "用户输入", vec![0.2]);
    let mat3 = Material::new("mat_003", "场景素材", MaterialCategory::Scenery, "用户输入", vec![0.3]);
    
    store.add_material(mat1);
    store.add_material(mat2);
    store.add_material(mat3);
    
    let summary = store.generate_summary();
    
    assert!(summary.contains("# 素材库摘要"));
    assert!(summary.contains("## 分类统计"));
    assert!(summary.contains("Combat"));
    assert!(summary.contains("Scenery"));
    assert!(summary.contains("## 最近素材"));
}

#[test]
fn test_cosine_similarity() {
    use zwriter_cli::rag::l3_material_store::cosine_similarity;
    
    // 相同向量，相似度为 1.0
    let v1 = vec![1.0, 0.0, 0.0];
    let v2 = vec![1.0, 0.0, 0.0];
    let sim = cosine_similarity(&v1, &v2);
    assert!((sim - 1.0).abs() < 0.001);
    
    // 正交向量，相似度为 0.0
    let v1 = vec![1.0, 0.0];
    let v2 = vec![0.0, 1.0];
    let sim = cosine_similarity(&v1, &v2);
    assert!(sim.abs() < 0.001);
    
    // 相反向量，相似度为 -1.0
    let v1 = vec![1.0, 0.0];
    let v2 = vec![-1.0, 0.0];
    let sim = cosine_similarity(&v1, &v2);
    assert!((sim + 1.0).abs() < 0.001);
    
    // 空向量，返回 0.0
    let v1: Vec<f32> = vec![];
    let v2: Vec<f32> = vec![];
    let sim = cosine_similarity(&v1, &v2);
    assert_eq!(sim, 0.0);
    
    // 长度不同，返回 0.0
    let v1 = vec![1.0, 0.0];
    let v2 = vec![1.0, 0.0, 0.0];
    let sim = cosine_similarity(&v1, &v2);
    assert_eq!(sim, 0.0);
}
