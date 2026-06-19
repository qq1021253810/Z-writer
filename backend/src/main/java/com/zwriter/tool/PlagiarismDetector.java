package com.zwriter.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 查重检测工具
 * 支持：文本与语料库重复度检测、两段文本相似度计算、相似片段查找
 * 使用 N-gram 相似度算法（3-gram）和 Jaccard 相似度
 */
@Slf4j
@Component
public class PlagiarismDetector {

    /** 默认 N-gram 长度 */
    private static final int DEFAULT_NGRAM_SIZE = 3;

    /** 默认相似片段阈值 */
    private static final double DEFAULT_THRESHOLD = 0.3;

    /**
     * 检测文本与语料库的重复度
     * @param text 待检测文本
     * @param corpus 语料库文本列表
     * @return 查重结果
     */
    public PlagiarismResult detect(String text, List<String> corpus) {
        if (text == null || text.isEmpty()) {
            return new PlagiarismResult(0.0, Collections.emptyList(), "低风险");
        }
        if (corpus == null || corpus.isEmpty()) {
            return new PlagiarismResult(0.0, Collections.emptyList(), "低风险");
        }

        // 查找相似片段
        List<SimilarSegment> segments = findSimilarSegments(text, corpus, DEFAULT_THRESHOLD);

        // 计算总体相似度：取所有语料文本的最大相似度
        double maxSimilarity = 0.0;
        for (String corpusText : corpus) {
            double similarity = calculateSimilarity(text, corpusText);
            maxSimilarity = Math.max(maxSimilarity, similarity);
        }

        // 如果有相似片段，综合计算总体相似度
        if (!segments.isEmpty()) {
            double avgSimilarity = segments.stream()
                    .mapToDouble(SimilarSegment::similarity)
                    .average()
                    .orElse(0.0);
            // 综合最大相似度和平均相似度
            maxSimilarity = Math.max(maxSimilarity, avgSimilarity * 0.7 + maxSimilarity * 0.3);
        }

        // 确定风险等级
        String riskLevel = determineRiskLevel(maxSimilarity);

        log.info("查重检测完成：总体相似度={}, 相似片段数={}, 风险等级={}",
                String.format("%.2f", maxSimilarity), segments.size(), riskLevel);

        return new PlagiarismResult(maxSimilarity, segments, riskLevel);
    }

    /**
     * 计算两段文本的相似度
     * @param text1 第一段文本
     * @param text2 第二段文本
     * @return 相似度（0-1）
     */
    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isEmpty() || text2.isEmpty()) {
            return 0.0;
        }

        Set<String> ngrams1 = extractNgrams(text1, DEFAULT_NGRAM_SIZE);
        Set<String> ngrams2 = extractNgrams(text2, DEFAULT_NGRAM_SIZE);

        return calculateJaccardSimilarity(ngrams1, ngrams2);
    }

    /**
     * 查找相似片段
     * @param text 待检测文本
     * @param corpus 语料库文本列表
     * @param threshold 相似度阈值
     * @return 相似片段列表
     */
    public List<SimilarSegment> findSimilarSegments(String text, List<String> corpus, double threshold) {
        List<SimilarSegment> segments = new ArrayList<>();

        if (text == null || text.isEmpty() || corpus == null || corpus.isEmpty()) {
            return segments;
        }

        // 将待检测文本按句子拆分
        String[] sourceSentences = splitIntoSentences(text);

        for (int i = 0; i < sourceSentences.length; i++) {
            String sourceSentence = sourceSentences[i];
            if (sourceSentence.trim().isEmpty()) {
                continue;
            }

            Set<String> sourceNgrams = extractNgrams(sourceSentence, DEFAULT_NGRAM_SIZE);
            if (sourceNgrams.isEmpty()) {
                continue;
            }

            // 在语料库中查找相似片段
            for (String corpusText : corpus) {
                String[] corpusSentences = splitIntoSentences(corpusText);

                for (int j = 0; j < corpusSentences.length; j++) {
                    String corpusSentence = corpusSentences[j];
                    if (corpusSentence.trim().isEmpty()) {
                        continue;
                    }

                    Set<String> corpusNgrams = extractNgrams(corpusSentence, DEFAULT_NGRAM_SIZE);
                    if (corpusNgrams.isEmpty()) {
                        continue;
                    }

                    double similarity = calculateJaccardSimilarity(sourceNgrams, corpusNgrams);

                    if (similarity >= threshold) {
                        segments.add(new SimilarSegment(
                                sourceSentence,
                                corpusSentence,
                                similarity,
                                findCharIndex(text, sourceSentence),
                                findCharIndex(corpusText, corpusSentence)
                        ));
                    }
                }
            }
        }

        // 按相似度降序排序
        segments.sort((a, b) -> Double.compare(b.similarity(), a.similarity()));

        return segments;
    }

    /**
     * 提取 N-gram
     * @param text 文本
     * @param n N-gram 长度
     * @return N-gram 集合
     */
    public Set<String> extractNgrams(String text, int n) {
        Set<String> ngrams = new HashSet<>();

        if (text == null || text.isEmpty() || n <= 0) {
            return ngrams;
        }

        // 去除空白字符，统一为小写
        String normalized = text.replaceAll("\\s+", "").toLowerCase();

        if (normalized.length() < n) {
            ngrams.add(normalized);
            return ngrams;
        }

        for (int i = 0; i <= normalized.length() - n; i++) {
            ngrams.add(normalized.substring(i, i + n));
        }

        return ngrams;
    }

    /**
     * 计算 Jaccard 相似度
     * @param set1 集合1
     * @param set2 集合2
     * @return Jaccard 相似度（0-1）
     */
    public double calculateJaccardSimilarity(Set<String> set1, Set<String> set2) {
        if (set1 == null || set2 == null || set1.isEmpty() || set2.isEmpty()) {
            return 0.0;
        }

        // 计算交集大小
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        // 计算并集大小
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return (double) intersection.size() / union.size();
    }

    /**
     * 确定风险等级
     * @param similarity 相似度
     * @return 风险等级（低风险/中风险/高风险）
     */
    private String determineRiskLevel(double similarity) {
        if (similarity >= 0.6) {
            return "高风险";
        } else if (similarity >= 0.3) {
            return "中风险";
        } else {
            return "低风险";
        }
    }

    /**
     * 将文本拆分为句子
     */
    private String[] splitIntoSentences(String text) {
        // 按中英文句号、感叹号、问号、换行符拆分
        return text.split("[。！？!?.\n]+");
    }

    /**
     * 查找子串在原文中的字符位置
     */
    private int findCharIndex(String text, String substring) {
        if (text == null || substring == null) {
            return -1;
        }
        int index = text.indexOf(substring);
        return index >= 0 ? index : -1;
    }

    /**
     * 查重检测结果
     */
    public record PlagiarismResult(
            double overallSimilarity,
            List<SimilarSegment> similarSegments,
            String riskLevel
    ) {
    }

    /**
     * 相似片段
     */
    public record SimilarSegment(
            String sourceText,
            String matchedText,
            double similarity,
            int sourceIndex,
            int matchedIndex
    ) {
    }
}
