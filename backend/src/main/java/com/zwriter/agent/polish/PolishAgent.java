package com.zwriter.agent.polish;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 润色&文风校准 Agent
 * 负责：文风统一、语言润色、节奏调整、前后文衔接
 */
@Slf4j
@Component
public class PolishAgent extends BaseAgent {
    
    @Override
    public String getName() {
        return "润色&文风校准 Agent";
    }
    
    @Override
    public AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", getName(), input.getTaskType());
        
        try {
            String subTask = (String) input.getParams().getOrDefault("subTask", "style");
            
            return switch (subTask) {
                case "style" -> calibrateStyle(input);
                case "polish" -> polishText(input);
                case "transition" -> fixTransition(input);
                default -> AgentResult.failure("未知的子任务: " + subTask);
            };
        } catch (Exception e) {
            log.error("[{}] 执行失败", getName(), e);
            return AgentResult.failure(e.getMessage());
        }
    }
    
    /**
     * 文风校准
     */
    private AgentResult calibrateStyle(AgentInput input) {
        String chapterContent = (String) input.getParams().get("chapterContent");
        String targetStyle = (String) input.getParams().get("targetStyle");
        
        String prompt = String.format("""
                请对以下章节进行文风校准：
                
                目标文风: %s
                
                章节内容:
                %s
                
                校准要求：
                1. 统一全文语言风格
                2. 修正不符合目标文风的用词
                3. 保持叙事节奏一致
                4. 标注修改点
                
                请输出校准后的内容，并在末尾附上修改说明。
                """, targetStyle, chapterContent);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "style");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 语言润色
     */
    private AgentResult polishText(AgentInput input) {
        String chapterContent = (String) input.getParams().get("chapterContent");
        
        String prompt = String.format("""
                请对以下章节进行语言润色：
                
                章节内容:
                %s
                
                润色要求：
                1. 修正错别字、病句
                2. 优化表达，增强画面感
                3. 删除冗余段落
                4. 增强对话表现力
                5. 保持原意不变
                
                请输出润色后的内容。
                """, chapterContent);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "polish");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 前后文衔接
     */
    private AgentResult fixTransition(AgentInput input) {
        String previousChapter = (String) input.getParams().get("previousChapter");
        String currentChapter = (String) input.getParams().get("currentChapter");
        
        String prompt = String.format("""
                请检查并修复以下两章之间的衔接问题：
                
                上一章结尾:
                %s
                
                本章开头:
                %s
                
                需要检查：
                1. 时间线是否连贯
                2. 角色位置是否合理
                3. 情节是否有断裂
                4. 情绪是否自然过渡
                
                请输出修复后的本章开头（约 500 字）。
                """, previousChapter, currentChapter);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "transition");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    @Override
    protected String buildSystemPrompt() {
        return """
                你是一个专业的网文小说编辑，精通文风校准和语言润色。
                
                你的职责：
                1. 统一全文语言风格
                2. 润色提升文字质量
                3. 确保前后文衔接自然
                4. 保持作者个人特色
                
                编辑原则：
                - 保持原意，不过度修改
                - 注重节奏感和可读性
                - 尊重作者风格
                """;
    }
}
