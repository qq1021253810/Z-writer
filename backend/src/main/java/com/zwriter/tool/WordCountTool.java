package com.zwriter.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 字数统计工具
 * 支持：总字数、章节字数、日均字数、字数规划
 */
@Slf4j
@Component
public class WordCountTool {
    
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");
    private static final Pattern ENGLISH_PATTERN = Pattern.compile("[a-zA-Z]");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern PUNCTUATION_PATTERN = Pattern.compile("[\\p{Punct}\\p{IsPunctuation}]");
    
    /**
     * 统计文本字数
     * @param text 文本内容
     * @return 字数统计结果
     */
    public WordCountResult countWords(String text) {
        if (text == null || text.isEmpty()) {
            return new WordCountResult(0, 0, 0, 0, 0);
        }
        
        int chineseCount = countMatches(text, CHINESE_PATTERN);
        int englishCount = countMatches(text, ENGLISH_PATTERN);
        int numberCount = countMatches(text, NUMBER_PATTERN);
        int punctuationCount = countMatches(text, PUNCTUATION_PATTERN);
        
        // 网文标准：中文字数 + 英文单词数 + 数字串数
        int totalWords = chineseCount + (englishCount / 5) + (numberCount / 3);
        
        return new WordCountResult(totalWords, chineseCount, englishCount, numberCount, punctuationCount);
    }
    
    /**
     * 计算日均字数
     * @param totalWords 总字数
     * @param days 天数
     * @return 日均字数
     */
    public int calculateDailyAverage(int totalWords, int days) {
        if (days <= 0) return 0;
        return totalWords / days;
    }
    
    /**
     * 生成字数规划
     * @param targetWords 目标总字数
     * @param currentWords 当前已写字数
     * @param remainingDays 剩余天数
     * @return 每日建议字数
     */
    public int generateDailyPlan(int targetWords, int currentWords, int remainingDays) {
        int remainingWords = targetWords - currentWords;
        if (remainingDays <= 0 || remainingWords <= 0) return 0;
        return (int) Math.ceil((double) remainingWords / remainingDays);
    }
    
    private int countMatches(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    /**
     * 字数统计结果
     */
    public record WordCountResult(
        int totalWords,
        int chineseCount,
        int englishCount,
        int numberCount,
        int punctuationCount
    ) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("totalWords", totalWords);
            map.put("chineseCount", chineseCount);
            map.put("englishCount", englishCount);
            map.put("numberCount", numberCount);
            map.put("punctuationCount", punctuationCount);
            return map;
        }
    }
}
