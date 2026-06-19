package com.zwriter.agent.compliance;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 合规&网文运营 Agent
 * 负责：违禁词检测、内容合规审查、标签优化、简介生成
 */
@Slf4j
@Component
public class ComplianceAgent extends BaseAgent {
    
    @Override
    public String getName() {
        return "合规&网文运营 Agent";
    }
    
    @Override
    public AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", getName(), input.getTaskType());
        
        try {
            String subTask = (String) input.getParams().getOrDefault("subTask", "check");
            
            return switch (subTask) {
                case "check" -> complianceCheck(input);
                case "tags" -> optimizeTags(input);
                case "summary" -> generateSummary(input);
                default -> AgentResult.failure("未知的子任务: " + subTask);
            };
        } catch (Exception e) {
            log.error("[{}] 执行失败", getName(), e);
            return AgentResult.failure(e.getMessage());
        }
    }
    
    /**
     * 内容合规审查
     */
    private AgentResult complianceCheck(AgentInput input) {
        String chapterContent = (String) input.getParams().get("chapterContent");
        
        String prompt = String.format("""
                请对以下章节内容进行合规审查：
                
                章节内容:
                %s
                
                审查维度：
                1. 政治敏感内容
                2. 色情/暴力描写
                3. 违禁词/敏感词
                4. 价值观导向
                5. 宗教/民族相关
                6. 其他违规风险
                
                需要输出：
                - 是否合规（通过/不通过）
                - 风险点列表（如有）
                - 风险等级（高/中/低）
                - 修改建议
                
                请以 Markdown 格式输出。
                """, chapterContent);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "check");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 标签优化
     */
    private AgentResult optimizeTags(AgentInput input) {
        String novelInfo = (String) input.getParams().get("novelInfo");
        
        String prompt = String.format("""
                请为以下小说优化标签：
                
                小说信息:
                %s
                
                需要输出：
                1. 主标签（1-2 个，如：玄幻、都市）
                2. 副标签（3-5 个，如：升级、热血、系统）
                3. 搜索关键词（5-10 个）
                4. 标签优化建议
                
                请以 Markdown 格式输出。
                """, novelInfo);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "tags");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 简介生成
     */
    private AgentResult generateSummary(AgentInput input) {
        String novelInfo = (String) input.getParams().get("novelInfo");
        
        String prompt = String.format("""
                请为以下小说生成简介：
                
                小说信息:
                %s
                
                需要生成 3 个版本：
                1. 短版（50 字以内，适合列表展示）
                2. 中版（150 字以内，适合详情页）
                3. 长版（300 字以内，适合推荐位）
                
                要求：
                - 突出核心卖点
                - 设置悬念吸引点击
                - 符合网文简介风格
                
                请以 Markdown 格式输出。
                """, novelInfo);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "summary");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    @Override
    protected String buildSystemPrompt() {
        return """
                你是一个专业的网文合规审核和运营专家。
                
                你的职责：
                1. 检测内容合规风险
                2. 优化小说标签提升曝光
                3. 生成吸引人的小说简介
                4. 提供运营建议
                
                审核原则：
                - 严格遵循平台规范
                - 准确识别风险等级
                - 提供可操作的修改建议
                """;
    }
}
