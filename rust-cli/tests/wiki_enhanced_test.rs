// Wiki 增强功能测试

use zwriter_cli::wiki::{Wiki, EntityType, Entity};
use std::path::PathBuf;
use std::collections::HashMap;
use std::fs;
use std::sync::atomic::{AtomicUsize, Ordering};

static TEST_COUNTER: AtomicUsize = AtomicUsize::new(0);

fn test_wiki_path() -> PathBuf {
    let id = TEST_COUNTER.fetch_add(1, Ordering::SeqCst);
    let thread_id = std::thread::current().id();
    PathBuf::from(format!("./test_wiki_enhanced_{:?}_{}", thread_id, id))
}

fn cleanup_test_wiki(path: &PathBuf) {
    if path.exists() {
        let _ = fs::remove_dir_all(path);
    }
}

fn setup_test_wiki() -> Wiki {
    let path = test_wiki_path();
    cleanup_test_wiki(&path);
    
    // 创建基础目录结构
    fs::create_dir_all(path.join("rules")).unwrap();
    fs::create_dir_all(path.join("genres")).unwrap();
    fs::create_dir_all(path.join("templates")).unwrap();
    fs::create_dir_all(path.join("examples")).unwrap();
    
    // 创建测试文件
    fs::write(
        path.join("rules/writing_rules.md"),
        "# 写作规则\n\n## 黄金三章\n\n第一章必须吸引读者。\n\n## 章节结构\n\n每章 2000-3000 字。"
    ).unwrap();
    
    fs::write(
        path.join("genres/shangzhan.md"),
        "# 商战赛道\n\n## 商业规则\n\n商业布局分为：市场调研、技术突破、产业链闭环、资本运作。"
    ).unwrap();
    
    fs::write(
        path.join("templates/writing.md"),
        "# 写作 Agent\n\n你是一位专业的网文写手。"
    ).unwrap();
    
    fs::write(
        path.join("templates/polish.md"),
        "# 润色 Agent\n\n你是一位专业的文本润色专家。"
    ).unwrap();
    
    Wiki::new(path)
}

#[test]
fn test_list_pages() {
    let wiki = setup_test_wiki();
    
    let pages = wiki.list_pages().unwrap();
    
    // 应该至少有 4 个页面（rules/writing_rules.md, genres/shangzhan.md, templates/writing.md, templates/polish.md）
    assert!(pages.len() >= 4, "应该至少有 4 个页面，实际有 {} 个", pages.len());
    
    // 验证页面包含正确的分类
    let categories: Vec<&str> = pages.iter().map(|p| p.category.as_str()).collect();
    assert!(categories.contains(&"rules"), "应该包含 rules 分类");
    assert!(categories.contains(&"genres"), "应该包含 genres 分类");
    assert!(categories.contains(&"templates"), "应该包含 templates 分类");
    
    println!("✅ 列出页面成功，共 {} 个页面", pages.len());
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_health_check_healthy() {
    let wiki = setup_test_wiki();
    
    let report = wiki.health_check().unwrap();
    
    // 健康检查应该通过
    assert!(report.is_healthy(), "Wiki 应该是健康的");
    assert!(report.missing_required.is_empty(), "不应该有缺失的必要文件");
    assert!(report.empty_pages.is_empty(), "不应该有空文件");
    assert!(report.total_pages >= 4, "应该至少有 4 个页面");
    
    println!("✅ 健康检查通过: {}", report.summary());
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_health_check_missing_files() {
    let path = test_wiki_path();
    cleanup_test_wiki(&path);
    
    // 只创建部分目录（缺少 rules 和 templates）
    fs::create_dir_all(path.join("genres")).unwrap();
    fs::write(
        path.join("genres/shangzhan.md"),
        "# 商战赛道"
    ).unwrap();
    
    let wiki = Wiki::new(path.clone());
    let report = wiki.health_check().unwrap();
    
    // 健康检查应该失败
    assert!(!report.is_healthy(), "Wiki 应该不健康");
    assert!(!report.missing_required.is_empty(), "应该有缺失的必要文件");
    
    // 应该缺少 rules 和 templates 目录
    let missing = report.missing_required.join(" ");
    assert!(missing.contains("rules"), "应该报告缺少 rules");
    assert!(missing.contains("templates"), "应该报告缺少 templates");
    
    println!("✅ 健康检查正确报告缺失文件: {}", report.summary());
    
    cleanup_test_wiki(&path);
}

#[test]
fn test_health_check_empty_pages() {
    let wiki = setup_test_wiki();
    
    // 创建一个空文件
    let empty_file = wiki.root.join("rules/empty.md");
    fs::write(&empty_file, "").unwrap();
    
    let report = wiki.health_check().unwrap();
    
    // 健康检查应该失败
    assert!(!report.is_healthy(), "Wiki 应该不健康（有空文件）");
    assert!(!report.empty_pages.is_empty(), "应该报告空文件");
    
    let empty_pages = report.empty_pages.join(" ");
    assert!(empty_pages.contains("empty"), "应该报告 empty.md 为空文件");
    
    println!("✅ 健康检查正确报告空文件: {}", report.summary());
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_extract_entities() {
    let wiki = setup_test_wiki();
    
    let text = r#"
角色: 张三，主角，科技创业家
人物: 李四，配角
地点: 星辰科技，创业圣地
方案: 全产业链闭环，核心战略
公司: 天虎集团，行业巨头
资源: 核心技术专利，核心资产
"#;
    
    let entities = wiki.extract_entities(text);
    
    // 应该提取出 6 个实体
    assert_eq!(entities.len(), 6, "应该提取出 6 个实体，实际提取出 {} 个", entities.len());
    
    // 验证实体类型
    let types: Vec<&EntityType> = entities.iter().map(|e| &e.entity_type).collect();
    assert!(types.contains(&&EntityType::Character), "应该包含角色类型");
    assert!(types.contains(&&EntityType::Location), "应该包含地点类型");
    assert!(types.contains(&&EntityType::Skill), "应该包含技能类型");
    assert!(types.contains(&&EntityType::Organization), "应该包含组织类型");
    assert!(types.contains(&&EntityType::Item), "应该包含物品类型");
    
    // 验证实体名称
    let names: Vec<&str> = entities.iter().map(|e| e.name.as_str()).collect();
    assert!(names.contains(&"张三"), "应该包含张三");
    assert!(names.contains(&"李四"), "应该包含李四");
    assert!(names.contains(&"星辰科技"), "应该包含星辰科技");
    assert!(names.contains(&"全产业链闭环"), "应该包含全产业链闭环");
    assert!(names.contains(&"天虎集团"), "应该包含天虎集团");
    assert!(names.contains(&"核心技术专利"), "应该包含核心技术专利");
    
    println!("✅ 实体提取成功，共提取 {} 个实体", entities.len());
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_generate_entity_page() {
    let wiki = setup_test_wiki();
    
    let mut attrs = HashMap::new();
    attrs.insert("职位".to_string(), "CEO".to_string());
    attrs.insert("年龄".to_string(), "25".to_string());
    
    let entity = Entity {
        name: "陆远".to_string(),
        entity_type: EntityType::Character,
        description: "科技创业公司CEO，战略眼光卓越".to_string(),
        attributes: attrs,
    };
    
    let path = wiki.generate_entity_page(&entity).unwrap();
    
    // 验证文件已创建
    assert!(path.exists(), "实体页面应该已创建");
    
    // 读取文件内容并验证
    let content = fs::read_to_string(&path).unwrap();
    assert!(content.contains("# 陆远"), "应该包含实体名称");
    assert!(content.contains("Character"), "应该包含实体类型");
    assert!(content.contains("科技创业公司CEO"), "应该包含描述");
    assert!(content.contains("职位"), "应该包含属性");
    assert!(content.contains("CEO"), "应该包含属性值");
    
    println!("✅ 实体页面生成成功: {:?}", path);
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_auto_generate_pages() {
    let wiki = setup_test_wiki();
    
    let dialogue = r#"
角色: 萧炎，主角，科技创业家
地点: 北京中关村，故事发生地
方案: 全产业链闭环，核心战略
公司: 云岚集团，强大势力
"#;
    
    let generated = wiki.auto_generate_pages(dialogue).unwrap();
    
    // 应该生成 4 个页面（角色、地点、方案、公司）
    assert_eq!(generated.len(), 4, "应该生成 4 个页面，实际生成 {} 个", generated.len());
    
    // 验证所有文件都已创建
    for path in &generated {
        assert!(path.exists(), "生成的页面应该存在: {:?}", path);
    }
    
    // 再次调用，不应该重复生成
    let generated_again = wiki.auto_generate_pages(dialogue).unwrap();
    assert_eq!(generated_again.len(), 0, "重复调用不应该生成新页面");
    
    println!("✅ 自动生成页面成功，共生成 {} 个页面", generated.len());
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_smart_load_basic() {
    let wiki = setup_test_wiki();
    
    // 测试基础加载（只加载规则）
    let pages = wiki.smart_load("普通文本", None).unwrap();
    
    assert!(!pages.is_empty(), "应该至少加载规则");
    assert!(pages[0].contains("写作规则"), "应该包含规则内容");
    
    println!("✅ 智能加载基础功能正常");
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_smart_load_with_genre() {
    let wiki = setup_test_wiki();
    
    // 测试加载指定赛道
    let pages = wiki.smart_load("普通文本", Some("shangzhan")).unwrap();
    
    assert!(pages.len() >= 2, "应该至少加载规则和赛道");
    
    let combined = pages.join("\n");
    assert!(combined.contains("写作规则"), "应该包含规则");
    assert!(combined.contains("商战赛道"), "应该包含赛道内容");
    assert!(combined.contains("商业规则"), "应该包含赛道详细内容");
    
    println!("✅ 智能加载赛道功能正常");
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_smart_load_with_entities() {
    let wiki = setup_test_wiki();
    
    // 先创建实体页面
    let entities_dir = wiki.root.join("entities");
    fs::create_dir_all(&entities_dir).unwrap();
    fs::write(
        entities_dir.join("张三.md"),
        "# 张三\n\n主角，科技创业家。"
    ).unwrap();
    fs::write(
        entities_dir.join("李四.md"),
        "# 李四\n\n配角，张三的合作伙伴。"
    ).unwrap();
    
    // 测试根据上下文加载实体
    let context = "张三和李四一起创业";
    let pages = wiki.smart_load(context, None).unwrap();
    
    let combined = pages.join("\n");
    assert!(combined.contains("张三"), "应该加载张三的页面");
    assert!(combined.contains("李四"), "应该加载李四的页面");
    
    println!("✅ 智能加载实体功能正常");
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_smart_load_with_keywords() {
    let wiki = setup_test_wiki();
    
    // 测试根据关键词加载模板
    let context = "我需要写作和润色帮助";
    let pages = wiki.smart_load(context, None).unwrap();
    
    let combined = pages.join("\n");
    assert!(combined.contains("写作 Agent"), "应该加载写作模板");
    assert!(combined.contains("润色 Agent"), "应该加载润色模板");
    
    println!("✅ 智能加载关键词功能正常");
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_read_entity() {
    let wiki = setup_test_wiki();
    
    // 先创建实体页面
    let entities_dir = wiki.root.join("entities");
    fs::create_dir_all(&entities_dir).unwrap();
    fs::write(
        entities_dir.join("王五.md"),
        "# 王五\n\n反派角色。"
    ).unwrap();
    
    // 测试读取存在的实体
    let content = wiki.read_entity("王五").unwrap();
    assert!(content.is_some(), "应该能读取存在的实体");
    let content = content.unwrap();
    assert!(content.contains("王五"), "内容应该包含实体名称");
    assert!(content.contains("反派角色"), "内容应该包含实体描述");
    
    // 测试读取不存在的实体
    let content = wiki.read_entity("赵六").unwrap();
    assert!(content.is_none(), "不存在的实体应该返回 None");
    
    println!("✅ 读取实体功能正常");
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_update_entity() {
    let wiki = setup_test_wiki();
    
    let mut attrs = HashMap::new();
    attrs.insert("职位".to_string(), "董事长".to_string());
    
    let entity = Entity {
        name: "张三".to_string(),
        entity_type: EntityType::Character,
        description: "科技创业家，公司已上市".to_string(),
        attributes: attrs,
    };
    
    // 更新实体（会创建新文件）
    let path = wiki.update_entity(&entity).unwrap();
    assert!(path.exists(), "实体页面应该已创建/更新");
    
    // 读取并验证内容
    let content = fs::read_to_string(&path).unwrap();
    assert!(content.contains("# 张三"), "应该包含实体名称");
    assert!(content.contains("董事长"), "应该包含更新的属性");
    assert!(content.contains("公司已上市"), "应该包含更新的描述");
    
    println!("✅ 更新实体功能正常");
    
    cleanup_test_wiki(&wiki.root);
}

#[test]
fn test_entity_type_conversion() {
    // 测试 EntityType 的字符串转换
    assert_eq!(EntityType::Character.as_str(), "character");
    assert_eq!(EntityType::Location.as_str(), "location");
    assert_eq!(EntityType::Item.as_str(), "item");
    assert_eq!(EntityType::Skill.as_str(), "skill");
    assert_eq!(EntityType::Organization.as_str(), "organization");
    assert_eq!(EntityType::Concept.as_str(), "concept");
    
    // 测试从字符串转换
    assert_eq!(EntityType::parse("character"), Some(EntityType::Character));
    assert_eq!(EntityType::parse("角色"), Some(EntityType::Character));
    assert_eq!(EntityType::parse("人物"), Some(EntityType::Character));
    assert_eq!(EntityType::parse("location"), Some(EntityType::Location));
    assert_eq!(EntityType::parse("地点"), Some(EntityType::Location));
    assert_eq!(EntityType::parse("技能"), Some(EntityType::Skill));
    assert_eq!(EntityType::parse("策略"), Some(EntityType::Skill));
    assert_eq!(EntityType::parse("方案"), Some(EntityType::Skill));
    assert_eq!(EntityType::parse("unknown"), None);
    
    println!("✅ EntityType 转换功能正常");
}
