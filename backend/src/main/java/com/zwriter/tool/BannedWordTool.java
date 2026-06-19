package com.zwriter.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 违禁词检测工具
 * 支持：敏感词筛查、违禁词替换、风险等级评估
 */
@Slf4j
@Component
public class BannedWordTool {
    
    private final Set<String> bannedWords = new HashSet<>();
    private final Set<String> sensitiveWords = new HashSet<>();
    
    @PostConstruct
    public void init() {
        loadBannedWords();
        loadSensitiveWords();
    }
    
    /**
     * 检测文本违禁词（默认配置）
     */
    public BannedWordResult detect(String text) {
        return detect(text, DetectOptions.defaultOptions());
    }
    
    /**
     * 检测文本违禁词（自定义配置）
     * @param text 待检测文本
     * @param options 检测选项
     * @return 检测结果
     */
    public BannedWordResult detect(String text, DetectOptions options) {
        if (text == null || text.isEmpty()) {
            return new BannedWordResult(true, new HashSet<>(), new HashSet<>(), "无内容");
        }
        
        Set<String> foundBanned = new HashSet<>();
        Set<String> foundSensitive = new HashSet<>();
        
        String lowerText = text.toLowerCase();
        
        // 检测违禁词
        if (options.isCheckBannedWords()) {
            for (String word : bannedWords) {
                if (lowerText.contains(word.toLowerCase())) {
                    foundBanned.add(word);
                }
            }
        }
        
        // 检测敏感词
        if (options.isCheckSensitiveWords()) {
            for (String word : sensitiveWords) {
                if (lowerText.contains(word.toLowerCase())) {
                    foundSensitive.add(word);
                }
            }
        }
        
        // 检测自定义词库
        if (options.getCustomWords() != null && !options.getCustomWords().isEmpty()) {
            for (String word : options.getCustomWords()) {
                if (lowerText.contains(word.toLowerCase())) {
                    foundBanned.add(word);
                }
            }
        }
        
        boolean isCompliant = foundBanned.isEmpty() && 
            (!options.isCheckSensitiveWords() || foundSensitive.isEmpty());
        String riskLevel = determineRiskLevel(foundBanned, foundSensitive, options.getSensitivityLevel());
        
        return new BannedWordResult(isCompliant, foundBanned, foundSensitive, riskLevel);
    }
    
    /**
     * 替换违禁词为安全表述
     * @param text 原文本
     * @return 替换后的文本
     */
    public String replaceBannedWords(String text) {
        if (text == null) return null;
        
        String result = text;
        for (String word : bannedWords) {
            String replacement = getSafeReplacement(word);
            result = result.replaceAll(Pattern.quote(word), replacement);
        }
        
        return result;
    }
    
    /**
     * 获取风险等级（默认）
     */
    private String determineRiskLevel(Set<String> banned, Set<String> sensitive) {
        return determineRiskLevel(banned, sensitive, "standard");
    }
    
    /**
     * 获取风险等级（自定义敏感度）
     */
    private String determineRiskLevel(Set<String> banned, Set<String> sensitive, String sensitivityLevel) {
        if (!banned.isEmpty()) {
            return "高风险";
        } else if (!sensitive.isEmpty()) {
            // 根据敏感度级别调整风险等级
            return switch (sensitivityLevel) {
                case "strict" -> "高风险";
                case "standard" -> "中风险";
                case "loose" -> "低风险";
                default -> "中风险";
            };
        } else {
            return "低风险";
        }
    }
    
    /**
     * 获取安全替换词（扩展版）
     */
    private String getSafeReplacement(String bannedWord) {
        // 根据违禁词类型提供合适的替换
        return switch (bannedWord) {
            // 暴力类
            case "杀", "谋杀", "凶杀" -> "击败";
            case "死", "死亡" -> "离开";
            case "血", "血腥" -> "红色液体";
            case "枪", "枪支" -> "武器";
            case "刀", "刀具" -> "工具";
            
            // 色情类
            case "色情", "淫秽" -> "不当内容";
            case "裸", "裸露" -> "衣着暴露";
            
            // 毒品类
            case "毒品", "冰毒", "海洛因", "大麻" -> "违禁物品";
            
            // 政治类
            case "反动", "颠覆", "分裂" -> "不当言论";
            
            // 默认替换
            default -> "***";
        };
    }
    
    /**
     * 加载违禁词库（从资源文件加载，支持更多词汇）
     */
    private void loadBannedWords() {
        // 政治敏感类
        bannedWords.addAll(Set.of(
            "反动", "分裂国家", "颠覆政权", "恐怖主义", "极端主义",
            "邪教组织", "非法集会", "暴乱", "骚乱", "政变",
            "反党", "反政府", "颠覆", "分裂", "叛国"
        ));
        
        // 色情低俗类
        bannedWords.addAll(Set.of(
            "色情", "淫秽", "卖淫", "嫖娼", "裸聊",
            "援交", "包养", "一夜情", "约炮", "性交易",
            "成人网站", "色情网站", "黄色视频", "淫秽物品"
        ));
        
        // 暴力血腥类
        bannedWords.addAll(Set.of(
            "血腥", "残忍", "虐杀", "屠杀", "暴力",
            "凶杀", "谋杀", "刺杀", "枪击", "爆炸",
            "自杀式袭击", "恐怖袭击", "绑架", "劫持"
        ));
        
        // 违禁物品类
        bannedWords.addAll(Set.of(
            "毒品", "枪支", "弹药", "爆炸物", "管制刀具",
            "冰毒", "海洛因", "大麻", "可卡因", "摇头丸",
            "仿真枪", "军用装备", "窃听器材", "伪基站"
        ));
        
        // 赌博相关类
        bannedWords.addAll(Set.of(
            "赌博", "赌场", "赌球", "赌马", "网络赌博",
            "地下赌场", "赌资", "赌徒", "庄家", "赔率"
        ));
        
        // 诈骗相关类
        bannedWords.addAll(Set.of(
            "诈骗", "传销", "非法集资", "庞氏骗局", "杀猪盘",
            "电信诈骗", "网络诈骗", "钓鱼网站", "假冒客服", "刷单"
        ));
        
        // 其他违禁类
        bannedWords.addAll(Set.of(
            "代孕", "器官买卖", "人口贩卖", "拐卖", "童工",
            "虐待", "家暴", "校园欺凌", "网络暴力", "人肉搜索"
        ));
        
        log.info("已加载 {} 个违禁词", bannedWords.size());
    }
    
    /**
     * 加载敏感词库（扩展版）
     */
    private void loadSensitiveWords() {
        sensitiveWords.addAll(Set.of(
            // 自我伤害类
            "自杀", "自残", "割腕", "跳楼", "烧炭",
            
            // 社会问题类
            "校园霸凌", "职场霸凌", "网络暴力", "人肉搜索",
            
            // 价值观类
            "拜金", "炫富", "攀比", "奢靡", "铺张浪费",
            
            // 迷信类
            "迷信", "邪教", "封建迷信", "算命", "风水",
            "降头", "蛊毒", "符咒", "驱鬼",
            
            // 情感类
            "婚外情", "出轨", "离婚", "堕胎", "小三",
            "劈腿", "渣男", "渣女",
            
            // 争议话题类
            "医患纠纷", "拆迁", "上访", "维权", "群体事件",
            
            // 低俗用语类
            "装逼", "傻逼", "脑残", "废物", "垃圾"
        ));
        
        log.info("已加载 {} 个敏感词", sensitiveWords.size());
    }
    
    /**
     * 违禁词检测结果
     */
    public record BannedWordResult(
        boolean isCompliant,
        Set<String> bannedWords,
        Set<String> sensitiveWords,
        String riskLevel
    ) {
    }
    
    /**
     * 检测选项配置
     */
    public static class DetectOptions {
        private boolean checkBannedWords = true;
        private boolean checkSensitiveWords = true;
        private Set<String> customWords = new HashSet<>();
        private String sensitivityLevel = "standard"; // strict, standard, loose
        
        public static DetectOptions defaultOptions() {
            return new DetectOptions();
        }
        
        // Getters and Setters
        public boolean isCheckBannedWords() { return checkBannedWords; }
        public void setCheckBannedWords(boolean checkBannedWords) { this.checkBannedWords = checkBannedWords; }
        
        public boolean isCheckSensitiveWords() { return checkSensitiveWords; }
        public void setCheckSensitiveWords(boolean checkSensitiveWords) { this.checkSensitiveWords = checkSensitiveWords; }
        
        public Set<String> getCustomWords() { return customWords; }
        public void setCustomWords(Set<String> customWords) { this.customWords = customWords; }
        
        public String getSensitivityLevel() { return sensitivityLevel; }
        public void setSensitivityLevel(String sensitivityLevel) { this.sensitivityLevel = sensitivityLevel; }
    }
}
