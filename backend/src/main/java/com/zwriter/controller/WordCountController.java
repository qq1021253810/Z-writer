package com.zwriter.controller;

import com.zwriter.repository.ChapterContentRepository;
import com.zwriter.repository.NovelInfoRepository;
import com.zwriter.tool.WordCountTool;
import com.zwriter.tool.WordCountTool.WordCountResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 字数统计控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/word-count")
@RequiredArgsConstructor
public class WordCountController {
    
    private final WordCountTool wordCountTool;
    private final ChapterContentRepository chapterContentRepository;
    private final NovelInfoRepository novelInfoRepository;
    
    /**
     * 统计文本字数
     */
    @PostMapping("/text")
    public ResponseEntity<Map<String, Object>> countText(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        WordCountResult result = wordCountTool.countWords(text);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", result.toMap());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 统计小说总字数
     */
    @GetMapping("/novel/{novelId}")
    public ResponseEntity<Map<String, Object>> countNovelWords(@PathVariable Long novelId) {
        var chapters = chapterContentRepository.findByNovelIdOrderByVolumeNumberAscChapterNumberAsc(novelId);
        
        int totalWords = chapters.stream()
            .mapToInt(chapter -> wordCountTool.countWords(chapter.getContent()).totalWords())
            .sum();
        
        int chapterCount = chapters.size();
        int avgWordsPerChapter = chapterCount > 0 ? totalWords / chapterCount : 0;
        
        Map<String, Object> data = new HashMap<>();
        data.put("novelId", novelId);
        data.put("totalWords", totalWords);
        data.put("chapterCount", chapterCount);
        data.put("avgWordsPerChapter", avgWordsPerChapter);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 统计章节字数
     */
    @GetMapping("/chapter/{chapterId}")
    public ResponseEntity<Map<String, Object>> countChapterWords(@PathVariable Long chapterId) {
        var chapter = chapterContentRepository.findById(chapterId);
        
        if (chapter.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "章节不存在");
            return ResponseEntity.badRequest().body(response);
        }
        
        WordCountResult result = wordCountTool.countWords(chapter.get().getContent());
        
        Map<String, Object> data = new HashMap<>();
        data.put("chapterId", chapterId);
        data.put("wordCount", result.toMap());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 计算日均字数
     */
    @GetMapping("/daily-average/{novelId}")
    public ResponseEntity<Map<String, Object>> calculateDailyAverage(
            @PathVariable Long novelId,
            @RequestParam(defaultValue = "30") int days) {
        
        var chapters = chapterContentRepository.findByNovelIdOrderByVolumeNumberAscChapterNumberAsc(novelId);
        int totalWords = chapters.stream()
            .mapToInt(chapter -> wordCountTool.countWords(chapter.getContent()).totalWords())
            .sum();
        
        int dailyAverage = wordCountTool.calculateDailyAverage(totalWords, days);
        
        Map<String, Object> data = new HashMap<>();
        data.put("novelId", novelId);
        data.put("totalWords", totalWords);
        data.put("days", days);
        data.put("dailyAverage", dailyAverage);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 生成字数规划
     */
    @PostMapping("/plan")
    public ResponseEntity<Map<String, Object>> generatePlan(@RequestBody Map<String, Object> request) {
        int targetWords = (Integer) request.get("targetWords");
        int currentWords = (Integer) request.get("currentWords");
        int remainingDays = (Integer) request.get("remainingDays");
        
        int dailyPlan = wordCountTool.generateDailyPlan(targetWords, currentWords, remainingDays);
        
        Map<String, Object> data = new HashMap<>();
        data.put("targetWords", targetWords);
        data.put("currentWords", currentWords);
        data.put("remainingDays", remainingDays);
        data.put("remainingWords", targetWords - currentWords);
        data.put("dailyPlan", dailyPlan);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
}
