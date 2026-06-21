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

    /// 估算 Token 数量（委托给 llm 模块统一实现）
    pub fn estimate_tokens(&self, text: &str) -> usize {
        crate::llm::estimate_tokens(text)
    }

    /// 分类消息重要性
    pub fn classify_importance(&self, content: &str) -> FidelityLevel {
        let keywords = ["名字", "设定", "记住", "伏笔", "人设", "世界观", "核心优势", "关键", "重要"];
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

    /// 保存上下文到文件（用于优雅关闭）
    pub fn save_to_file(&self, path: &std::path::Path) -> crate::error::Result<()> {
        let snapshot = ContextSnapshot {
            token_budget: self.token_budget,
            recent_limit: self.recent_limit,
            tier1_recent: self.tier1_recent.iter().map(|m| MessageSnapshot {
                role: m.role.clone(),
                content: m.content.clone(),
                importance: match m.importance {
                    FidelityLevel::Full => "full".to_string(),
                    FidelityLevel::Compressed => "compressed".to_string(),
                    FidelityLevel::Placeholder => "placeholder".to_string(),
                },
            }).collect(),
            tier2_compressed: self.tier2_compressed.iter().map(|m| MessageSnapshot {
                role: m.role.clone(),
                content: m.content.clone(),
                importance: match m.importance {
                    FidelityLevel::Full => "full".to_string(),
                    FidelityLevel::Compressed => "compressed".to_string(),
                    FidelityLevel::Placeholder => "placeholder".to_string(),
                },
            }).collect(),
            tier3_permanent: self.tier3_permanent.iter().map(|m| MessageSnapshot {
                role: m.role.clone(),
                content: m.content.clone(),
                importance: match m.importance {
                    FidelityLevel::Full => "full".to_string(),
                    FidelityLevel::Compressed => "compressed".to_string(),
                    FidelityLevel::Placeholder => "placeholder".to_string(),
                },
            }).collect(),
        };
        
        let json = serde_json::to_string_pretty(&snapshot)
            .map_err(|e| crate::error::AppError::Context(format!("序列化上下文失败: {}", e)))?;
        std::fs::write(path, json)
            .map_err(|e| crate::error::AppError::Context(format!("保存上下文失败: {}", e)))?;
        
        tracing::info!("[ContextManager] 上下文已保存到: {}", path.display());
        Ok(())
    }

    /// 从文件加载上下文（用于恢复会话）
    pub fn load_from_file(&mut self, path: &std::path::Path) -> crate::error::Result<()> {
        if !path.exists() {
            return Ok(());
        }
        
        let json = std::fs::read_to_string(path)
            .map_err(|e| crate::error::AppError::Context(format!("读取上下文失败: {}", e)))?;
        let snapshot: ContextSnapshot = serde_json::from_str(&json)
            .map_err(|e| crate::error::AppError::Context(format!("解析上下文失败: {}", e)))?;
        
        self.token_budget = snapshot.token_budget;
        self.recent_limit = snapshot.recent_limit;
        
        self.tier1_recent = snapshot.tier1_recent.into_iter().map(|m| Message {
            role: m.role,
            content: m.content,
            importance: match m.importance.as_str() {
                "full" => FidelityLevel::Full,
                "compressed" => FidelityLevel::Compressed,
                _ => FidelityLevel::Placeholder,
            },
        }).collect();
        
        self.tier2_compressed = snapshot.tier2_compressed.into_iter().map(|m| Message {
            role: m.role,
            content: m.content,
            importance: match m.importance.as_str() {
                "full" => FidelityLevel::Full,
                "compressed" => FidelityLevel::Compressed,
                _ => FidelityLevel::Placeholder,
            },
        }).collect();
        
        self.tier3_permanent = snapshot.tier3_permanent.into_iter().map(|m| Message {
            role: m.role,
            content: m.content,
            importance: match m.importance.as_str() {
                "full" => FidelityLevel::Full,
                "compressed" => FidelityLevel::Compressed,
                _ => FidelityLevel::Placeholder,
            },
        }).collect();
        
        tracing::info!("[ContextManager] 上下文已从 {} 恢复", path.display());
        Ok(())
    }
}

/// 上下文快照（用于序列化）
#[derive(serde::Serialize, serde::Deserialize)]
struct ContextSnapshot {
    token_budget: usize,
    recent_limit: usize,
    tier1_recent: Vec<MessageSnapshot>,
    tier2_compressed: Vec<MessageSnapshot>,
    tier3_permanent: Vec<MessageSnapshot>,
}

/// 消息快照（用于序列化）
#[derive(serde::Serialize, serde::Deserialize)]
struct MessageSnapshot {
    role: String,
    content: String,
    importance: String,
}
