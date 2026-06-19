package com.zwriter.agent.character;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import com.zwriter.entity.Character;
import com.zwriter.repository.CharacterRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 人物塑造 Agent
 * 负责：角色档案生成、角色关系网络、角色成长弧线、角色对话生成
 */
@Slf4j
@Component
public class CharacterAgent extends BaseAgent {
    
    @Autowired
    private CharacterRepository characterRepository;
    
    @Override
    public String getName() {
        return "人物塑造 Agent";
    }
    
    @Override
    public AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", getName(), input.getTaskType());
        
        try {
            String subTask = (String) input.getParams().getOrDefault("subTask", "profile");
            
            return switch (subTask) {
                case "profile" -> generateProfile(input);
                case "relation" -> generateRelation(input);
                case "growth" -> generateGrowth(input);
                case "dialogue" -> generateDialogue(input);
                default -> AgentResult.failure("未知的子任务: " + subTask);
            };
        } catch (Exception e) {
            log.error("[{}] 执行失败", getName(), e);
            return AgentResult.failure(e.getMessage());
        }
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
     * 角色关系网络
     */
    private AgentResult generateRelation(AgentInput input) {
        Long novelId = input.getNovelId();
        List<Character> characters = characterRepository.findByNovelId(novelId);
        
        StringBuilder charInfo = new StringBuilder();
        for (Character c : characters) {
            charInfo.append(String.format("- %s (%s): %s\n", 
                    c.getName(), c.getRoleType(), c.getCoreTraits()));
        }
        
        String prompt = String.format("""
                请为以下角色生成关系网络：
                
                角色列表:
                %s
                
                需要输出：
                1. 角色间的关系类型（敌对、友好、暧昧、师徒等）
                2. 关系强度（1-10）
                3. 关系发展走向
                4. 关键冲突点
                
                请以 Markdown 格式输出。
                """, charInfo.toString());
        
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
