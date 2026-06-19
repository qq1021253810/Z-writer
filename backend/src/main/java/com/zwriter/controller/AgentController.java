package com.zwriter.controller;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.controller.ControllerAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Agent API 控制器
 * 提供统一的 Agent 调用入口
 */
@Slf4j
@RestController
@RequestMapping("/api/agent")
public class AgentController {
    
    @Autowired
    private ControllerAgent controllerAgent;
    
    /**
     * 统一 Agent 调用接口
     */
    @PostMapping("/execute")
    public Map<String, Object> execute(@RequestBody AgentRequest request) {
        log.info("收到 Agent 请求: taskType={}, novelId={}", request.getTaskType(), request.getNovelId());
        
        long startTime = System.currentTimeMillis();
        
        try {
            AgentInput input = AgentInput.builder()
                    .novelId(request.getNovelId())
                    .taskType(request.getTaskType())
                    .userInput(request.getUserInput())
                    .params(request.getParams())
                    .stream(false)
                    .build();
            
            AgentResult result = controllerAgent.handleRequest(input);
            
            long duration = System.currentTimeMillis() - startTime;
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isSuccess());
            response.put("content", result.getContent());
            response.put("errorMessage", result.getErrorMessage());
            response.put("data", result.getData());
            response.put("durationMs", duration);
            
            return response;
        } catch (Exception e) {
            log.error("Agent 执行失败", e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("errorMessage", e.getMessage());
            return response;
        }
    }
    
    /**
     * Agent 请求体
     */
    public static class AgentRequest {
        private Long novelId;
        private String taskType;
        private String userInput;
        private Map<String, Object> params;
        
        public Long getNovelId() { return novelId; }
        public void setNovelId(Long novelId) { this.novelId = novelId; }
        
        public String getTaskType() { return taskType; }
        public void setTaskType(String taskType) { this.taskType = taskType; }
        
        public String getUserInput() { return userInput; }
        public void setUserInput(String userInput) { this.userInput = userInput; }
        
        public Map<String, Object> getParams() { return params; }
        public void setParams(Map<String, Object> params) { this.params = params; }
    }
}
