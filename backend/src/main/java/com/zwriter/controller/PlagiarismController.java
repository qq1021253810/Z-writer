package com.zwriter.controller;

import com.zwriter.tool.PlagiarismDetector;
import com.zwriter.tool.PlagiarismDetector.PlagiarismResult;
import com.zwriter.tool.PlagiarismDetector.SimilarSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 查重检测控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/plagiarism")
@RequiredArgsConstructor
public class PlagiarismController {

    private final PlagiarismDetector plagiarismDetector;

    /**
     * 查重检测
     * 请求体：{"text": "...", "corpus": ["...", "..."]}
     */
    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detect(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");

        @SuppressWarnings("unchecked")
        List<String> corpus = (List<String>) request.get("corpus");
        if (corpus == null) {
            corpus = Collections.emptyList();
        }

        PlagiarismResult result = plagiarismDetector.detect(text, corpus);

        // 构建相似片段列表
        List<Map<String, Object>> segmentList = new ArrayList<>();
        for (SimilarSegment segment : result.similarSegments()) {
            Map<String, Object> segMap = new HashMap<>();
            segMap.put("sourceText", segment.sourceText());
            segMap.put("matchedText", segment.matchedText());
            segMap.put("similarity", segment.similarity());
            segMap.put("sourceIndex", segment.sourceIndex());
            segMap.put("matchedIndex", segment.matchedIndex());
            segmentList.add(segMap);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("overallSimilarity", result.overallSimilarity());
        data.put("similarSegments", segmentList);
        data.put("riskLevel", result.riskLevel());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }

    /**
     * 计算两段文本相似度
     * 请求体：{"text1": "...", "text2": "..."}
     */
    @PostMapping("/similarity")
    public ResponseEntity<Map<String, Object>> similarity(@RequestBody Map<String, String> request) {
        String text1 = request.get("text1");
        String text2 = request.get("text2");

        double similarity = plagiarismDetector.calculateSimilarity(text1, text2);

        Map<String, Object> data = new HashMap<>();
        data.put("similarity", similarity);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);

        return ResponseEntity.ok(response);
    }
}
