package com.zwriter.agent.writing;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 正文写作分镜 Agent
 * 负责：章节分镜、正文生成、场景描写、对话生成
 */
@Slf4j
@Component
public class WritingAgent extends BaseAgent {
    
    @Override
    public String getName() {
        return "正文写作分镜 Agent";
    }
    
    @Override
    public AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", getName(), input.getTaskType());
        
        try {
            String subTask = (String) input.getParams().getOrDefault("subTask", "storyboard");
            
            return switch (subTask) {
                case "storyboard" -> generateStoryboard(input);
                case "chapter" -> generateChapter(input);
                case "scene" -> generateScene(input);
                default -> AgentResult.failure("未知的子任务: " + subTask);
            };
        } catch (Exception e) {
            log.error("[{}] 执行失败", getName(), e);
            return AgentResult.failure(e.getMessage());
        }
    }
    
    /**
     * 章节分镜
     */
    private AgentResult generateStoryboard(AgentInput input) {
        String chapterOutline = (String) input.getParams().get("chapterOutline");
        String worldContext = contextService.getNovelContext(input.getNovelId());
        
        String prompt = String.format("""
                请为以下章节大纲生成分镜脚本：
                
                上下文:
                %s
                
                章节大纲:
                %s
                
                分镜要求：
                1. 每个场景的起止位置
                2. 场景类型（对话、动作、心理、环境）
                3. 视角切换
                4. 节奏快慢标注
                5. 情绪基调
                
                请以 Markdown 格式输出。
                """, worldContext, chapterOutline);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "storyboard");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 正文生成
     */
    private AgentResult generateChapter(AgentInput input) {
        String storyboard = (String) input.getParams().get("storyboard");
        String writingStyle = (String) input.getParams().getOrDefault("writingStyle", "热血爽文");
        String wordCount = (String) input.getParams().getOrDefault("wordCount", "3000");
        String worldContext = contextService.getNovelContext(input.getNovelId());
        
        String prompt = String.format("""
                请基于以下分镜脚本生成正文：
                
                上下文（必须遵循）:
                %s
                
                分镜脚本:
                %s
                
                写作要求：
                - 笔风: %s
                - 目标字数: %s 字
                - 严格遵循上下文设定
                - 场景转换自然
                - 对话生动，动作描写有画面感
                - 段落节奏感强
                
                请直接输出正文内容。
                """, worldContext, storyboard, writingStyle, wordCount);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "chapter");
        data.put("content", response);
        data.put("wordCount", response.length());
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 场景描写
     */
    private AgentResult generateScene(AgentInput input) {
        String sceneType = (String) input.getParams().get("sceneType");
        String sceneDesc = (String) input.getParams().get("sceneDesc");
        String worldContext = contextService.getNovelContext(input.getNovelId());
        
        String prompt = String.format("""
                请生成以下场景描写：
                
                上下文:
                %s
                
                场景类型: %s
                场景描述: %s
                
                要求：
                - 五感描写（视觉、听觉、嗅觉、触觉、味觉）
                - 氛围渲染
                - 与上下文衔接自然
                
                请直接输出场景描写内容。
                """, worldContext, sceneType, sceneDesc);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "scene");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    @Override
    protected String buildSystemPrompt() {
        return """
                你是一个专业的网文小说写手，精通各类风格的正文写作。

                【核心职责】
                1. 根据分镜脚本生成高质量正文
                2. 场景描写有画面感
                3. 对话生动自然
                4. 严格遵循世界观和角色设定

                【网文写作规则】
                1. 开篇即冲突：第一章前500字必须出现核心冲突或悬念
                2. 短句为主：避免长句，多用短句增强节奏感
                3. 对话驱动：对话要推动剧情，避免废话
                4. 画面感强：动作描写要有镜头感，让读者"看到"场景
                5. 情绪代入：心理描写要让读者感同身受
                6. 钩子结尾：每章结尾必须留悬念或钩子

                【分段规则】
                - 每段不超过3-4行（适配手机阅读）
                - 对话单独成段
                - 重要转折单独成段
                - 场景切换空一行

                【对话写作】
                - 符合角色性格（参考人设）
                - 口语化，避免书面语
                - 每句对话不超过2行
                - 动作+对话结合，避免纯对话

                【场景描写】
                - 五感描写：视觉、听觉、嗅觉、触觉、味觉
                - 氛围渲染：用环境烘托情绪
                - 简洁有力：不堆砌辞藻
                - 与剧情相关：不为描写而描写

                【节奏控制】
                - 紧张场景：短句、快节奏
                - 情感场景：长句、慢节奏
                - 战斗场景：动词密集、画面感强
                - 日常场景：轻松幽默、生活化

                【禁止事项】
                - 不要过度文绉绉（网文要通俗易懂）
                - 不要大段心理独白（用行动表现）
                - 不要强行说教（让读者自己感悟）
                - 不要水字数（每段都要有信息量）

                【写作原则】
                - 严格遵循上下文设定，不出现矛盾
                - 文笔流畅，节奏感强
                - 注重画面感和代入感
                - 对话推动剧情，避免废话
                - 符合目标赛道的文风（热血/甜宠/悬疑等）
                """;
    }
}
