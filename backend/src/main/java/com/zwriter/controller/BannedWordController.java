package com.zwriter.controller;

import com.zwriter.tool.BannedWordTool;
import com.zwriter.tool.BannedWordTool.BannedWordResult;
import com.zwriter.tool.BannedWordTool.DetectOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 违禁词检测控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/banned-word")
@RequiredArgsConstructor
public class BannedWordController {
    
    private final BannedWordTool bannedWordTool;
    
    /**
     * 检测文本违禁词（支持自定义选项）
     */
    @PostMapping("/detect")
    public ResponseEntity<Map<String, Object>> detect(@RequestBody Map<String, Object> request) {
        String text = (String) request.get("text");
        
        // 解析自定义选项
        DetectOptions options = DetectOptions.defaultOptions();
        if (request.containsKey("checkBannedWords")) {
            options.setCheckBannedWords((Boolean) request.get("checkBannedWords"));
        }
        if (request.containsKey("checkSensitiveWords")) {
            options.setCheckSensitiveWords((Boolean) request.get("checkSensitiveWords"));
        }
        if (request.containsKey("customWords")) {
            @SuppressWarnings("unchecked")
            List<String> customWordsList = (List<String>) request.get("customWords");
            options.setCustomWords(new HashSet<>(customWordsList));
        }
        if (request.containsKey("sensitivityLevel")) {
            options.setSensitivityLevel((String) request.get("sensitivityLevel"));
        }
        
        BannedWordResult result = bannedWordTool.detect(text, options);
        
        Map<String, Object> data = new HashMap<>();
        data.put("isCompliant", result.isCompliant());
        data.put("bannedWords", result.bannedWords());
        data.put("sensitiveWords", result.sensitiveWords());
        data.put("riskLevel", result.riskLevel());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 替换违禁词
     */
    @PostMapping("/replace")
    public ResponseEntity<Map<String, Object>> replace(@RequestBody Map<String, String> request) {
        String text = request.get("text");
        
        String replacedText = bannedWordTool.replaceBannedWords(text);
        
        Map<String, Object> data = new HashMap<>();
        data.put("originalText", text);
        data.put("replacedText", replacedText);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", data);
        
        return ResponseEntity.ok(response);
    }
}
