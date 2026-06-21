//! Token 输入优化系统

/// Token 优化器
pub struct TokenOptimizer {
    pub budget: usize,
}

impl TokenOptimizer {
    pub fn new(budget: usize) -> Self {
        Self { budget }
    }

    /// 无损压缩（空白规范化）
    pub fn lossless_compression(&self, text: &str) -> String {
        // 去除多余空白，保留单个空格
        text.split_whitespace().collect::<Vec<_>>().join(" ")
    }

    /// 中文虚词过滤
    pub fn chinese_stopword_filter(&self, text: &str) -> String {
        let stop_words = [
            "的", "了", "和", "是", "就", "都", "而", "及", "与",
            "着", "或", "一个", "没有", "我们", "你们", "他们",
        ];

        let mut result = text.to_string();
        for word in &stop_words {
            result = result.replace(word, "");
        }
        result
    }

    /// 抽取式压缩（基于句子重要性）
    pub fn extractive_compression(&self, text: &str, target_ratio: f32) -> String {
        if text.is_empty() || target_ratio >= 1.0 {
            return text.to_string();
        }

        // 按句子分割（简单实现：按句号、问号、感叹号分割）
        let sentences: Vec<&str> = text
            .split(|c| c == '。' || c == '？' || c == '！' || c == '.' || c == '?' || c == '!')
            .filter(|s| !s.trim().is_empty())
            .collect();

        if sentences.is_empty() {
            return text.to_string();
        }

        // 计算每个句子的重要性（基于关键词和长度）
        let mut scored_sentences: Vec<(usize, f32, &str)> = sentences
            .iter()
            .enumerate()
            .map(|(idx, sentence)| {
                let score = self.calculate_sentence_importance(sentence);
                (idx, score, *sentence)
            })
            .collect();

        // 按重要性排序
        scored_sentences.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap());

        // 选择最重要的句子，直到达到目标比例
        let target_count = (sentences.len() as f32 * target_ratio).ceil() as usize;
        let mut selected: Vec<(usize, &str)> = scored_sentences
            .iter()
            .take(target_count)
            .map(|(idx, _, sentence)| (*idx, *sentence))
            .collect();

        // 按原始顺序排序
        selected.sort_by_key(|(idx, _)| *idx);

        // 重新组合
        selected
            .iter()
            .map(|(_, sentence)| *sentence)
            .collect::<Vec<_>>()
            .join("。") + "。"
    }

    /// 计算句子重要性
    fn calculate_sentence_importance(&self, sentence: &str) -> f32 {
        let mut score = 0.0;

        // 关键词加分
        let keywords = ["关键", "重要", "必须", "注意", "记住", "设定", "伏笔"];
        for keyword in &keywords {
            if sentence.contains(keyword) {
                score += 2.0;
            }
        }

        // 对话加分
        if sentence.contains('"') || sentence.contains('"') || sentence.contains('"') {
            score += 1.5;
        }

        // 数字加分（可能包含重要信息）
        if sentence.chars().any(|c| c.is_numeric()) {
            score += 1.0;
        }

        // 长度适中加分（太短或太长都不太好）
        let len = sentence.len();
        if len >= 20 && len <= 100 {
            score += 1.0;
        }

        score
    }

    /// 智能压缩（组合多种策略）
    pub fn smart_compress(&self, text: &str, current_tokens: usize) -> String {
        if current_tokens <= self.budget {
            return text.to_string();
        }

        let mut result = text.to_string();

        // 1. 先做无损压缩
        result = self.lossless_compression(&result);

        // 2. 如果还是超预算，做虚词过滤
        if self.estimate_tokens(&result) > self.budget {
            result = self.chinese_stopword_filter(&result);
        }

        // 3. 如果还是超预算，做抽取式压缩
        if self.estimate_tokens(&result) > self.budget {
            let ratio = self.budget as f32 / self.estimate_tokens(&result) as f32;
            result = self.extractive_compression(&result, ratio * 0.8); // 留一些余量
        }

        result
    }

    /// 估算 Token 数量（中文优化）
    pub fn estimate_tokens(&self, text: &str) -> usize {
        // 中文大约 1 字 = 1.5 token，英文大约 1 词 = 1.3 token
        let chinese_chars = text.chars().filter(|c| *c >= '\u{4e00}' && *c <= '\u{9fff}').count();
        let other_chars = text.chars().count() - chinese_chars;
        
        ((chinese_chars as f32 * 1.5) + (other_chars as f32 * 0.75)) as usize
    }
}
