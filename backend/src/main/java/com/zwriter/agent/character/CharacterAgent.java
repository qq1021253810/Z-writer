package com.zwriter.agent.character;

import com.zwriter.agent.base.AgentContext;
import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 人物塑造 Agent (Stub - Phase 1.1 migration)
 * TODO: 将在 Phase 3 完全重写，使用文件系统存储替代数据库
 */
@Slf4j
@Component
public class CharacterAgent extends BaseAgent {

    @Override
    public String name() {
        return "人物塑造 Agent";
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    protected AgentResult doExecute(AgentInput input) throws Exception {
        String subTask = getSubTask(input);

        return switch (subTask) {
            case "profile" -> generateProfile(input);
            case "relation" -> generateRelation(input);
            case "growth" -> generateGrowth(input);
            case "dialogue" -> generateDialogue(input);
            default -> AgentResult.failure("未知的子任务: " + subTask);
        };
    }
    
    /**
     * 角色档案生成
     */
    private AgentResult generateProfile(AgentInput input) {
        String worldSetting = (String) input.getParams().get("worldSetting");
        String roleType = (String) input.getParams().getOrDefault("roleType", "主角");
        
        String prompt = String.format("""
                请基于以下世界观生成角色档案：
                
                世界观:
                %s
                
                角色类型: %s
                
                需要包含：
                1. 基本信息（姓名、年龄、身份、外貌）
                2. 性格特点（核心性格、优缺点）
                3. 背景故事（出身、经历、动机）
                4. 能力设定（天赋、技能、成长潜力）
                5. 人际关系（重要联系人）
                6. 目标追求（短期目标、长期目标）
                
                请以 Markdown 格式输出。
                """, worldSetting, roleType);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "profile");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 角色关系网络 (Stub - 数据库依赖已移除)
     */
    private AgentResult generateRelation(AgentInput input) {
        log.warn("[CharacterAgent] generateRelation 为 Stub 实现 - 数据库依赖已移除");
        String prompt = """
                请生成角色关系网络。
                需要输出：
                1. 角色间的关系类型（敌对、友好、暧昧、师徒等）
                2. 关系强度（1-10）
                3. 关系发展走向
                4. 关键冲突点
                
                请以 Markdown 格式输出。
                """;
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "relation");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 角色成长弧线
     */
    private AgentResult generateGrowth(AgentInput input) {
        String characterProfile = (String) input.getParams().get("characterProfile");
        
        String prompt = String.format("""
                请为以下角色设计成长弧线：
                
                角色档案:
                %s
                
                需要包含：
                1. 初始状态（能力、心态、认知）
                2. 关键转折点（至少 3 个）
                3. 每个转折点的触发事件
                4. 成长后的状态变化
                5. 最终成就
                
                请以 Markdown 格式输出。
                """, characterProfile);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "growth");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 角色对话生成
     */
    private AgentResult generateDialogue(AgentInput input) {
        String characterA = (String) input.getParams().get("characterA");
        String characterB = (String) input.getParams().get("characterB");
        String scene = (String) input.getParams().get("scene");
        
        String prompt = String.format("""
                请生成以下场景的角色对话：
                
                角色 A: %s
                角色 B: %s
                场景: %s
                
                要求：
                1. 对话符合角色性格
                2. 体现角色关系
                3. 推动剧情发展
                4. 语言风格统一
                
                请以剧本格式输出。
                """, characterA, characterB, scene);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "dialogue");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 新版接口（AgentContext）
     */
    @Override
    protected AgentResult doExecute(AgentContext ctx) throws Exception {
        String subTask = getSubTask(ctx, "profile");
        
        return switch (subTask) {
            case "profile" -> generateProfile(ctx);
            case "relation" -> generateRelation(ctx);
            case "growth" -> generateGrowth(ctx);
            case "dialogue" -> generateDialogue(ctx);
            default -> AgentResult.failure("未知的子任务: " + subTask);
        };
    }

    private AgentResult generateProfile(AgentContext ctx) {
        String worldSetting = getParam(ctx, "worldSetting", "");
        String roleType = getParam(ctx, "roleType", "主角");
        
        String prompt = String.format("""
                请基于以下世界观生成角色档案：
                
                世界观:
                %s
                
                角色类型: %s
                
                需要包含：
                1. 基本信息（姓名、年龄、身份、外貌）
                2. 性格特点（核心性格、优缺点）
                3. 背景故事（出身、经历、动机）
                4. 能力设定（天赋、技能、成长潜力）
                5. 人际关系（重要联系人）
                6. 目标追求（短期目标、长期目标）
                
                请以 Markdown 格式输出。
                """, worldSetting, roleType);
        
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "profile", "content", response));
    }
    
    private AgentResult generateRelation(AgentContext ctx) {
        String prompt = """
                请生成角色关系网络。
                需要输出：
                1. 角色间的关系类型（敌对、友好、暧昧、师徒等）
                2. 关系强度（1-10）
                3. 关系发展走向
                4. 关键冲突点
                
                请以 Markdown 格式输出。
                """;
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "relation", "content", response));
    }
    
    private AgentResult generateGrowth(AgentContext ctx) {
        String characterProfile = getParam(ctx, "characterProfile", "");
        String prompt = String.format("""
                请为以下角色设计成长弧线：
                
                角色档案:
                %s
                
                需要包含：
                1. 初始状态（能力、心态、认知）
                2. 关键转折点（至少 3 个）
                3. 每个转折点的触发事件
                4. 成长后的状态变化
                5. 最终成就
                
                请以 Markdown 格式输出。
                """, characterProfile);
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "growth", "content", response));
    }
    
    private AgentResult generateDialogue(AgentContext ctx) {
        String characterA = getParam(ctx, "characterA", "");
        String characterB = getParam(ctx, "characterB", "");
        String scene = getParam(ctx, "scene", "");
        String prompt = String.format("""
                请生成以下场景的角色对话：
                
                角色 A: %s
                角色 B: %s
                场景: %s
                
                要求：
                1. 对话符合角色性格
                2. 体现角色关系
                3. 推动剧情发展
                4. 语言风格统一
                
                请以剧本格式输出。
                """, characterA, characterB, scene);
        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "dialogue", "content", response));
    }
    
    @Override
    protected String buildSystemPrompt() {
        return """
                你是一个专业的网文小说人物塑造专家，精通角色设计和人物关系构建。
                
                你的职责：
                1. 创造立体、有血有肉的角色形象
                2. 设计合理的角色关系网络
                3. 规划角色成长弧线
                4. 生成符合角色性格的对话
                
                输出要求：
                - 使用 Markdown 格式
                - 角色特点鲜明，避免脸谱化
                - 关系设计合理，有戏剧张力
                - 对话生动，符合人物设定
                """;
    }
}
