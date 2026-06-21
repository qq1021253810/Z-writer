//! 分层上下文管理器

use std::collections::VecDeque;

/// 消息
#[derive(Debug, Clone)]
pub struct Message {
    pub role: String,
    pub content: String,
    pub importance: FidelityLevel,
}

/// 保真度级别
#[derive(Debug, Clone, PartialEq)]
pub enum FidelityLevel {
    Full,       // 完整保留（Tier 3 永久）
    Compressed, // 压缩保留（Tier 2 摘要）
    Placeholder, // 占位符（Tier 1 最近）
}

/// 分层上下文管理器
pub struct ContextManager {
    pub token_budget: usize,
    pub tier1_recent: VecDeque<Message>,      // 最近 N 条消息（完整）
    pub tier2_compressed: Vec<Message>,       // 较早消息（摘要）
    pub tier3_permanent: Vec<Message>,        // 关键决策和约束（永久）
    pub recent_limit: usize,                  // Tier 1 保留条数
}

impl ContextManager {
    pub fn new(token_budget: usize) -> Self {
        Self {
            token_budget,
            tier1_recent: VecDeque::new(),
            tier2_compressed: Vec::new(),
            tier3_permanent: Vec::new(),
            recent_limit: 10,
        }
    }

    /// 估算 Token 数量（中文优化：1 字 ≈ 1.5 token）
    pub fn estimate_tokens(&self, text: &str) -> usize {
        let ascii_chars = text.chars().filter(|c| c.is_ascii()).count();
        let other_chars = text.chars().filter(|c| !c.is_ascii()).count();
        (ascii_chars as f32 * 0.75 + other_chars as f32 * 1.5) as usize
    }

    /// 分类消息重要性
    pub fn classify_importance(&self, content: &str) -> FidelityLevel {
        let keywords = ["名字", "设定", "记住", "伏笔", "人设", "世界观", "金手指", "关键", "重要"];
        if keywords.iter().any(|k| content.contains(k)) {
            FidelityLevel::Full
        } else if content.len() > 500 {
            FidelityLevel::Compressed
        } else {
            FidelityLevel::Placeholder
        }
    }

    /// 添加消息到上下文
    pub fn add_message(&mut self, role: &str, content: &str) {
        let importance = self.classify_importance(content);
        let message = Message {
            role: role.to_string(),
            content: content.to_string(),
            importance: importance.clone(),
        };

        // 根据重要性分类存储
        match importance {
            FidelityLevel::Full => {
                self.tier3_permanent.push(message);
            }
            FidelityLevel::Compressed => {
                self.tier2_compressed.push(message);
            }
            FidelityLevel::Placeholder => {
                self.tier1_recent.push_back(message);
                // 超出限制时移除最旧的
                while self.tier1_recent.len() > self.recent_limit {
                    if let Some(old_msg) = self.tier1_recent.pop_front() {
                        // 可以选择压缩后放入 tier2
                        self.tier2_compressed.push(old_msg);
                    }
                }
            }
        }
    }

    /// 构建续写上下文（按优先级组装）
    pub fn build_context(&self) -> String {
        let mut context = String::new();
        let mut total_tokens = 0;

        // 1. Tier 3 永久保留（最高优先级）
        if !self.tier3_permanent.is_empty() {
            context.push_str("【关键设定】\n");
            for msg in &self.tier3_permanent {
                let tokens = self.estimate_tokens(&msg.content);
                if total_tokens + tokens > self.token_budget {
                    break;
                }
                context.push_str(&format!("{}\n", msg.content));
                total_tokens += tokens;
            }
            context.push('\n');
        }

        // 2. Tier 2 压缩摘要
        if !self.tier2_compressed.is_empty() {
            context.push_str("【前情摘要】\n");
            for msg in &self.tier2_compressed {
                let tokens = self.estimate_tokens(&msg.content);
                if total_tokens + tokens > self.token_budget {
                    break;
                }
                context.push_str(&format!("{}\n", msg.content));
                total_tokens += tokens;
            }
            context.push('\n');
        }

        // 3. Tier 1 最近消息
        if !self.tier1_recent.is_empty() {
            context.push_str("【最近对话】\n");
            for msg in self.tier1_recent.iter().rev() {
                let tokens = self.estimate_tokens(&msg.content);
                if total_tokens + tokens > self.token_budget {
                    break;
                }
                context.push_str(&format!("[{}] {}\n", msg.role, msg.content));
                total_tokens += tokens;
            }
        }

        context
    }

    /// 压缩 Tier 2（合并多个摘要）
    pub fn compress_tier2(&mut self, summary: &str) {
        self.tier2_compressed.clear();
        self.tier2_compressed.push(Message {
            role: "system".to_string(),
            content: summary.to_string(),
            importance: FidelityLevel::Compressed,
        });
    }

    /// 获取当前 Token 使用量
    pub fn current_tokens(&self) -> usize {
        let mut total = 0;
        for msg in &self.tier3_permanent {
            total += self.estimate_tokens(&msg.content);
        }
        for msg in &self.tier2_compressed {
            total += self.estimate_tokens(&msg.content);
        }
        for msg in &self.tier1_recent {
            total += self.estimate_tokens(&msg.content);
        }
        total
    }

    /// 清空上下文
    pub fn clear(&mut self) {
        self.tier1_recent.clear();
        self.tier2_compressed.clear();
        self.tier3_permanent.clear();
    }
}
