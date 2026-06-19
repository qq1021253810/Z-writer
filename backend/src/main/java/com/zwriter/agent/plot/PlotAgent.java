package com.zwriter.agent.plot;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 剧情爽点节奏 Agent
 * 负责：黄金三章设计、爽点节奏规划、毒点规避、情绪曲线控制
 */
@Slf4j
@Component
public class PlotAgent extends BaseAgent {
    
    @Override
    public String getName() {
        return "剧情爽点节奏 Agent";
    }
    
    @Override
    public AgentResult execute(AgentInput input) {
        long startTime = System.currentTimeMillis();
        log.info("[{}] 开始执行任务: {}", getName(), input.getTaskType());
        
        try {
            String subTask = (String) input.getParams().getOrDefault("subTask", "golden3");
            
            return switch (subTask) {
                case "golden3" -> designGolden3(input);
                case "rhythm" -> planRhythm(input);
                case "poison" -> avoidPoison(input);
                case "emotion" -> controlEmotion(input);
                default -> AgentResult.failure("未知的子任务: " + subTask);
            };
        } catch (Exception e) {
            log.error("[{}] 执行失败", getName(), e);
            return AgentResult.failure(e.getMessage());
        }
    }
    
    /**
     * 黄金三章设计
     */
    private AgentResult designGolden3(AgentInput input) {
        String outline = (String) input.getParams().get("outline");
        String genre = (String) input.getParams().get("genre");
        
        String prompt = String.format("""
                请为以下小说设计黄金三章：
                
                小说类型: %s
                大纲:
                %s
                
                黄金三章要求：
                1. 第一章：快速引入主角，展示核心冲突，设置悬念
                2. 第二章：展现主角特殊性，制造第一个爽点
                3. 第三章：扩大冲突，展示世界观，留下钩子
                
                需要输出：
                - 每章核心事件
                - 爽点设计
                - 悬念设置
                - 字数建议（每章 2000-3000 字）
                
                请以 Markdown 格式输出。
                """, genre, outline);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "golden3");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 爽点节奏规划
     */
    private AgentResult planRhythm(AgentInput input) {
        String outline = (String) input.getParams().get("outline");
        
        String prompt = String.format("""
                请为以下小说规划爽点节奏：
                
                大纲:
                %s
                
                需要输出：
                1. 爽点类型分布（打脸、升级、获宝、复仇等）
                2. 爽点间隔（每 3-5 章一个小爽点，每 15-20 章一个大爽点）
                3. 爽点强度曲线（逐步升级）
                4. 高潮节点设计
                
                请以 Markdown 表格形式输出。
                """, outline);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "rhythm");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 毒点规避
     */
    private AgentResult avoidPoison(AgentInput input) {
        String chapterContent = (String) input.getParams().get("chapterContent");
        
        String prompt = String.format("""
                请检查以下章节内容是否存在毒点：
                
                章节内容:
                %s
                
                常见毒点：
                1. 主角过于圣母/懦弱
                2. 女角色过度依赖男主
                3. 逻辑硬伤
                4. 节奏拖沓
                5. 强行降智
                6. 过度说教
                
                需要输出：
                - 发现的毒点（如有）
                - 毒点严重程度（1-10）
                - 修改建议
                
                请以 Markdown 格式输出。
                """, chapterContent);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "poison");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    /**
     * 情绪曲线控制
     */
    private AgentResult controlEmotion(AgentInput input) {
        String outline = (String) input.getParams().get("outline");
        
        String prompt = String.format("""
                请为以下小说设计情绪曲线：
                
                大纲:
                %s
                
                需要输出：
                1. 整体情绪走势（起伏节奏）
                2. 关键情绪节点（燃、虐、甜、爽）
                3. 情绪转换过渡
                4. 读者情绪引导策略
                
                请以 Markdown 格式输出，可包含简单图示。
                """, outline);
        
        String response = callLlm(prompt);
        
        Map<String, Object> data = new HashMap<>();
        data.put("type", "emotion");
        data.put("content", response);
        
        return AgentResult.success(response, data);
    }
    
    @Override
    protected String buildSystemPrompt() {
        return """
                你是一个专业的网文小说剧情策划专家，精通爽点设计和节奏控制。

                【核心职责】
                1. 设计吸引人的黄金三章
                2. 规划合理的爽点节奏
                3. 规避常见毒点
                4. 控制读者情绪曲线

                【黄金三章规则】
                第一章：
                - 前500字必须出现核心冲突或悬念
                - 快速引入主角，展示特殊性（金手指/身份/能力）
                - 结尾必须留钩子，让读者想看第二章

                第二章：
                - 展现主角第一次小爽点（打脸/获宝/升级）
                - 扩大世界观，引入关键配角
                - 制造新的冲突或危机

                第三章：
                - 第一个小高潮（解决危机/击败敌人）
                - 展示主角成长潜力
                - 留下长线悬念，引导读者追读

                【爽点设计模板】
                - 打脸爽：被轻视→展示实力→震惊全场
                - 升级爽：遇到瓶颈→获得机缘→突破境界
                - 复仇爽：受辱→修炼→回来碾压
                - 获宝爽：偶然发现→艰难获取→实力暴涨
                - 情感爽：单恋→误会→表白→在一起

                【毒点规避库】
                严禁出现：
                - 主角过于圣母/懦弱（该杀不杀）
                - 女角色过度依赖男主（花瓶化）
                - 逻辑硬伤（前后矛盾）
                - 节奏拖沓（水字数）
                - 强行降智（配角突然变蠢）
                - 过度说教（破坏代入感）
                - 憋屈无反击（被虐太久）
                - 战力崩坏（升级太快/太慢）

                【节奏控制】
                - 每3-5章一个小爽点
                - 每15-20章一个大高潮
                - 每50章一个大转折
                - 情绪曲线：压抑→爆发→短暂平静→新冲突

                【输出要求】
                - 使用 Markdown 格式
                - 符合网文市场规律
                - 注重读者体验
                - 数据化、可视化呈现
                - 给出具体章节建议和字数规划
                """;
    }
}
