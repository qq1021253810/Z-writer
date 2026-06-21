// Wiki 知识库测试

use zwriter_cli::wiki::Wiki;
use std::path::PathBuf;

fn wiki_path() -> PathBuf {
    PathBuf::from("./wiki")
}

#[test]
fn test_wiki_load_rules() {
    let wiki = Wiki::new(wiki_path());
    let rules = wiki.load_rules().unwrap();
    assert!(!rules.is_empty(), "规则文件为空");
    assert!(rules.contains("黄金三章"), "未包含黄金三章规则");
    assert!(rules.contains("章节结构"), "未包含章节结构规则");
    println!("✅ Wiki 规则加载成功，共 {} 字节", rules.len());
}

#[test]
fn test_wiki_load_genre() {
    let wiki = Wiki::new(wiki_path());
    
    // 测试科幻赛道
    let kehuan = wiki.load_genre("kehuan").unwrap();
    assert!(!kehuan.is_empty(), "科幻赛道为空");
    assert!(kehuan.contains("科技"), "未包含科技元素");
    
    // 测试科技赛道
    let keji = wiki.load_genre("keji").unwrap();
    assert!(!keji.is_empty(), "科技赛道为空");
    
    // 测试不存在的赛道
    let unknown = wiki.load_genre("unknown").unwrap();
    assert!(unknown.is_empty(), "不存在的赛道应返回空字符串");
    
    println!("✅ Wiki 赛道加载成功");
}

#[test]
fn test_wiki_load_template() {
    let wiki = Wiki::new(wiki_path());
    
    // 测试写作模板
    let writing = wiki.load_template("writing").unwrap();
    assert!(!writing.is_empty(), "写作模板为空");
    assert!(writing.contains("写作 Agent"), "未包含写作 Agent 定义");
    
    // 测试润色模板
    let polish = wiki.load_template("polish").unwrap();
    assert!(!polish.is_empty(), "润色模板为空");
    assert!(polish.contains("润色 Agent"), "未包含润色 Agent 定义");
    
    // 测试不存在的模板
    let unknown = wiki.load_template("unknown").unwrap();
    assert!(unknown.is_empty(), "不存在的模板应返回空字符串");
    
    println!("✅ Wiki 模板加载成功");
}

#[test]
fn test_wiki_load_relevant_pages() {
    let wiki = Wiki::new(wiki_path());
    
    // 测试加载商战相关页面
    let pages = wiki.load_relevant_pages("shangzhan").unwrap();
    assert!(!pages.is_empty(), "商战相关页面为空");
    
    // 验证包含规则、赛道、模板
    let combined = pages.join("\n");
    assert!(combined.contains("黄金三章") || combined.contains("商业规则"), 
            "未包含相关内容");
    
    println!("✅ Wiki 相关页面加载成功，共 {} 个页面", pages.len());
}
