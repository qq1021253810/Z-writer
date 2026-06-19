package com.zwriter.controller;

import com.zwriter.service.DialogueGuideService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 对话引导控制器
 * 提供对话模式下的自动生成功能
 */
@Slf4j
@RestController
@RequestMapping("/api/dialogue-guide")
public class DialogueGuideController {

    @Autowired
    private DialogueGuideService dialogueGuideService;

    /**
     * 生成对话引导建议
     *
     * @param request 请求体，包含 stepType、userInput 和 existingInfo
     * @return 包含 success、suggestions 和 generatedContent 的响应
     */
    @PostMapping("/generate")
    public Map<String, Object> generate(@RequestBody GenerateRequest request) {
        log.info("收到对话引导生成请求: stepType={}", request.getStepType());

        long startTime = System.currentTimeMillis();

        try {
            List<String> suggestions = dialogueGuideService.generate(
                    request.getStepType(),
                    request.getUserInput(),
                    request.getExistingInfo()
            );

            long duration = System.currentTimeMillis() - startTime;

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("suggestions", suggestions);
            response.put("generatedContent", suggestions.isEmpty() ? "" : suggestions.get(0));
            response.put("durationMs", duration);

            return response;
        } catch (Exception e) {
            log.error("对话引导生成失败", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("suggestions", List.of());
            response.put("generatedContent", "");
            response.put("errorMessage", e.getMessage());

            return response;
        }
    }

    /**
     * 请求参数
     */
    public static class GenerateRequest {
        private String stepType;
        private String userInput;
        private Map<String, Object> existingInfo;

        public String getStepType() {
            return stepType;
        }

        public void setStepType(String stepType) {
            this.stepType = stepType;
        }

        public String getUserInput() {
            return userInput;
        }

        public void setUserInput(String userInput) {
            this.userInput = userInput;
        }

        public Map<String, Object> getExistingInfo() {
            return existingInfo != null ? existingInfo : new HashMap<>();
        }

        public void setExistingInfo(Map<String, Object> existingInfo) {
            this.existingInfo = existingInfo;
        }
    }
}
