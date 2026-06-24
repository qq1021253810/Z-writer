package com.zwriter.agent.worldoutline;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 世界观&大纲规划 Agent
 * 负责：赛道选题、世界观搭建、大纲生成、剧情分支推演
 */
@Slf4j
@Component
public class WorldOutlineAgent extends BaseAgent {

    @Override
    public String name() {
        return "世界观&大纲规划 Agent";
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    protected AgentResult doExecute(AgentInput input) throws Exception {
        String subTask = getSubTask(input, "topic");

        return switch (subTask) {
            case "topic" -> generateTopic(input);
            case "world" -> buildWorld(input);
            case "outline" -> generateOutline(input);
            case "branch" -> generateBranch(input);
            default -> AgentResult.failure("未知的子任务: " + subTask);
        };
    }
    
    /**
     * 赛道选题生成
     */
    private AgentResult generateTopic(AgentInput input) {
        String userPreference = input.getUserInput();
        String prompt = buildSystemPrompt() + "\n\n用户偏好: " + userPreference;
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "topic");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 世界观搭建
     */
    private AgentResult buildWorld(AgentInput input) {
        String topic = (String) input.getParams().get("topic");
        String prompt = String.format("""
                请为以下小说主题搭建完整的世界观：
                
                主题: %s
                
                需要包含：
                1. 力量体系（修炼等级、能力分类）
                2. 地域地图（主要区域、势力分布）
                3. 势力划分（宗门、家族、组织）
                4. 历史背景（重要事件、传说）
                5. 规则限制（世界法则、禁忌）
                
                请以 Markdown 格式输出。
                """, topic);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "world");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 分级大纲生成
     */
    private AgentResult generateOutline(AgentInput input) {
        String worldSetting = (String) input.getParams().get("worldSetting");
        String prompt = String.format("""
                请基于以下世界观生成小说大纲：
                
                世界观设定:
                %s
                
                需要生成：
                1. 全书总纲（核心冲突、主线走向）
                2. 分卷大纲（每卷核心目标）
                3. 每卷核心冲突（主要矛盾、转折点）
                4. 百章级长线伏笔（伏笔埋设点、揭示点）
                
                请以 Markdown 格式输出。
                """, worldSetting);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "outline");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 剧情分支推演
     */
    private AgentResult generateBranch(AgentInput input) {
        String outline = (String) input.getParams().get("outline");
        String prompt = String.format("""
                请基于以下大纲推演剧情分支：
                
                大纲:
                %s
                
                需要输出：
                1. 多条剧情走向（至少 3 条）
                2. 每条走向的爽度评分（1-10）
                3. 每条走向的受众适配度分析
                4. 推荐走向及理由
                
                请以 Markdown 格式输出。
                """, outline);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "branch");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 新版接口（AgentContext）
     */
    @Override
    protected AgentResult doExecute(AgentContext ctx) throws Exception {
        String subTask = getSubTask(ctx, "topic");
        
        return switch (subTask) {
            case "topic" -> generateTopic(ctx);
            case "world" -> buildWorld(ctx);
            case "outline" -> generateOutline(ctx);
            case "branch" -> generateBranch(ctx);
            default -> AgentResult.failure("未知的子任务: " + subTask);
        };
    }

    private AgentResult generateTopic(AgentContext ctx) {
        String prompt = buildSystemPrompt() + "\n\n用户偏好: " + ctx.userInput();
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "topic", "content", response));
    }
    
    private AgentResult buildWorld(AgentContext ctx) {
        String topic = getParam(ctx, "topic", "");
        String prompt = String.format("""
                请为以下小说主题搭建完整的世界观：
                
                主题: %s
                
                需要包含：
                1. 力量体系（修炼等级、能力分类）
                2. 地域地图（主要区域、势力分布）
                3. 势力划分（宗门、家族、组织）
                4. 历史背景（重要事件、传说）
                5. 规则限制（世界法则、禁忌）
                
                请以 Markdown 格式输出。
                """, topic);
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "world", "content", response));
    }
    
    private AgentResult generateOutline(AgentContext ctx) {
        String worldSetting = getParam(ctx, "worldSetting", "");
        String prompt = String.format("""
                请基于以下世界观生成小说大纲：
                
                世界观设定:
                %s
                
                需要生成：
                1. 全书总纲（核心冲突、主线走向）
                2. 分卷大纲（每卷核心目标）
                3. 每卷核心冲突（主要矛盾、转折点）
                4. 百章级长线伏笔（伏笔埋设点、揭示点）
                
                请以 Markdown 格式输出。
                """, worldSetting);
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "outline", "content", response));
    }
    
    private AgentResult generateBranch(AgentContext ctx) {
        String outline = getParam(ctx, "outline", "");
        String prompt = String.format("""
                请基于以下大纲推演剧情分支：
                
                大纲:
                %s
                
                需要输出：
                1. 多条剧情走向（至少 3 条）
                2. 每条走向的爽度评分（1-10）
                3. 每条走向的受众适配度分析
                4. 推荐走向及理由
                
                请以 Markdown 格式输出。
                """, outline);
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "branch", "content", response));
    }
    
    @Override
    protected String buildSystemPrompt() {
        return """
                你是一个专业的网文小说策划专家，精通各类网文赛道的选题和世界观搭建。
                
                你的职责：
                1. 根据用户偏好生成有商业价值的选题
                2. 搭建完整、自洽的世界观体系
                3. 设计吸引人的剧情大纲
                4. 提供多条剧情走向供选择
                
                输出要求：
                - 使用 Markdown 格式
                - 结构清晰，层次分明
                - 内容详实，具有可操作性
                - 符合网文市场规律
                """;
    }
}
