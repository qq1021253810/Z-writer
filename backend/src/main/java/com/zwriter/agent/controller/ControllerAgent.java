package com.zwriter.agent.controller;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.worldoutline.WorldOutlineAgent;
import com.zwriter.agent.character.CharacterAgent;
import com.zwriter.agent.plot.PlotAgent;
import com.zwriter.agent.writing.WritingAgent;
import com.zwriter.agent.polish.PolishAgent;
import com.zwriter.agent.compliance.ComplianceAgent;
import com.zwriter.service.ContextService;
import com.zwriter.service.ContextCompressionService;
import com.zwriter.llm.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 总控调度 Agent
 * 负责接收用户请求，分析意图，分发到对应的子 Agent
 * 增强功能：上下文统筹、冲突校验、多Agent协作
 */
@Slf4j
@Component
public class ControllerAgent {

    @Autowired
    private WorldOutlineAgent worldOutlineAgent;

    @Autowired
    private CharacterAgent characterAgent;

    @Autowired
    private PlotAgent plotAgent;

    @Autowired
    private WritingAgent writingAgent;

    @Autowired
    private PolishAgent polishAgent;

    @Autowired
    private ComplianceAgent complianceAgent;

    @Autowired
    private ContextService contextService;

    @Autowired
    private ContextCompressionService contextCompressionService;

    @Autowired
    private LlmService llmService;

    /**
     * Agent 路由映射表（策略模式）
     */
    private final Map<String, Function<AgentInput, AgentResult>> agentRouter = Map.of(
            "world_outline", worldOutlineAgent::execute,
            "character", characterAgent::execute,
            "plot", plotAgent::execute,
            "writing", writingAgent::execute,
            "polish", polishAgent::execute,
            "compliance", complianceAgent::execute
    );
    
    /**
     * 处理用户请求（增强版：带上下文统筹和冲突校验）
     */
    public AgentResult handleRequest(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[总控 Agent] 收到请求: taskType={}, userInput={}", 
                input.getTaskType(), truncate(input.getUserInput(), 100));
        
        try {
            // 1. 上下文统筹：为子Agent准备完整上下文
            if (input.getNovelId() != null && input.getParams() != null) {
                Map<String, Object> params = input.getParams();
                int volumeNumber = params.containsKey("volumeNumber")
                        ? ((Number) params.get("volumeNumber")).intValue() : 1;
                int chapterNumber = params.containsKey("chapterNumber")
                        ? ((Number) params.get("chapterNumber")).intValue() : 1;
                String context = contextService.getChapterContext(
                    input.getNovelId(), volumeNumber, chapterNumber
                );
                // 将上下文注入params供子Agent使用
                Map<String, Object> newParams = new HashMap<>(params);
                newParams.put("globalContext", context);
                input.setParams(newParams);
            }
            
            // 2. 路由到对应Agent执行
            String taskType = input.getTaskType();
            AgentResult result = routeToAgent(taskType, input);
            
            // 3. 冲突校验：检查生成内容是否与已有设定冲突
            if (result.isSuccess() && input.getNovelId() != null && 
                (taskType.equals("writing") || taskType.equals("character"))) {
                String conflictCheck = checkConflicts(input.getNovelId(), result.getContent());
                if (conflictCheck != null && !conflictCheck.isEmpty()) {
                    log.warn("[总控 Agent] 检测到潜在冲突: {}", conflictCheck);
                    // 将冲突信息附加到结果中
                    Map<String, Object> data = result.getData() != null ? 
                        new HashMap<>(result.getData()) : new HashMap<>();
                    data.put("conflictWarning", conflictCheck);
                    result = AgentResult.success(result.getContent(), data);
                }
            }
            
            long duration = System.currentTimeMillis() - startTime;
            log.info("[总控 Agent] 请求完成，耗时: {}ms", duration);
            
            return result;
        } catch (Exception e) {
            log.error("[总控 Agent] 处理请求失败", e);
            return AgentResult.failure("处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 冲突校验：检查生成内容是否与已有设定冲突
     */
    private String checkConflicts(Long novelId, String generatedContent) {
        try {
            // 获取压缩后的上下文（角色、时间线、伏笔）
            String compressedContext = contextCompressionService.buildCompressedContext(novelId, 3);
            
            String prompt = String.format("""
                你是一个专业的小说设定校验专家。请检查以下生成内容是否与已有设定存在冲突。
                
                【已有设定】
                %s
                
                【待检查内容】
                %s
                
                请检查以下方面：
                1. 角色人设是否崩塌（性格、能力、关系）
                2. 时间线是否矛盾
                3. 伏笔是否冲突
                4. 世界观设定是否一致
                
                如果发现冲突，请明确指出冲突点和原因。如果没有冲突，回复"无冲突"。
                """, compressedContext, truncate(generatedContent, 2000));
            
            String response = llmService.chat(prompt, "你是一个严谨的设定校验专家，只关注事实冲突，不评价文笔。");
            
            if (response != null && !response.contains("无冲突") && response.length() > 10) {
                return response;
            }
        } catch (Exception e) {
            log.warn("[总控 Agent] 冲突校验失败: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * 路由到对应的子 Agent（使用策略模式）
     */
    private AgentResult routeToAgent(String taskType, AgentInput input) {
        return agentRouter.getOrDefault(taskType,
                i -> AgentResult.failure("未知的任务类型: " + taskType)).apply(input);
    }
    
    /**
     * 截断字符串（用于日志）
     */
    private String truncate(String str, int maxLen) {
        if (str == null) return "";
        return str.length() > maxLen ? str.substring(0, maxLen) + "..." : str;
    }
}
