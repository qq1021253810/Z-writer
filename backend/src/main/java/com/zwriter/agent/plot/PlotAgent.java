package com.zwriter.agent.plot;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 剧情架构师 Agent
 * 负责：开篇钩子设计、张力管理、逻辑门禁、深度设计、卡文分析
 */
@Slf4j
@Component
public class PlotAgent extends BaseAgent {

    public PlotAgent() {
        registerSubTask("hook", this::designHook);
        registerSubTask("tension", this::manageTension);
        registerSubTask("logic_gate", this::checkLogic);
        registerSubTask("depth", this::designDepth);
        registerSubTask("analysis", this::analyzeWriterBlock);
    }

    @Override
    public String name() {
        return "剧情架构师 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "hook";
    }

    /**
     * 开篇钩子设计（高信息密度开篇）
     */
    private AgentResult designHook(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline");
        String genre = getParam(params, "genre");

        String prompt = String.format("""
                请为以下小说设计高信息密度开篇：

                小说类型: %s
                大纲:
                %s

                开篇设计原则（非黄金三章套路）：
                1. 第一章：矛盾切入
                   - 直接切入核心冲突的某个切面
                   - 通过具体场景暗示更大的世界观
                   - 用细节而非说明展示角色特质
                   - 结尾留下信息缺口，驱动阅读

                2. 第二章：信息展开
                   - 通过角色行动自然展开世界观
                   - 引入关键配角，建立关系张力
                   - 展示角色的决策模式和价值观
                   - 埋设第一个伏笔

                3. 第三章：冲突升级
                   - 核心矛盾的第一次正面碰撞
                   - 角色面临真正的选择（有代价的选择）
                   - 展示故事的独特视角/主题
                   - 建立长线悬念

                需要输出：
                - 每章核心事件和叙事目的
                - 信息密度控制（每章揭示什么、隐藏什么）
                - 伏笔埋设点
                - 悬念设计（信息缺口而非廉价钩子）

                要求：
                - 禁止"被轻视→展示实力"的套路
                - 禁止突然出现的"金手指"
                - 所有信息通过场景和行动自然展开
                - 开篇即建立故事的独特调性

                请以 Markdown 格式输出。
                """, genre, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "hook", "content", response));
    }

    /**
     * 张力管理（多线并行的张力曲线）
     */
    private AgentResult manageTension(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline");

        String prompt = String.format("""
                请为以下小说设计张力管理方案（多线并行）：

                大纲:
                %s

                需要输出：
                1. 张力线识别
                   - 主线张力（核心冲突的推进）
                   - 角色张力（内在冲突的拉扯）
                   - 关系张力（人际关系的博弈）
                   - 主题张力（主题思想的碰撞）

                2. 张力曲线设计
                   - 各张力线的起伏节奏
                   - 张力线的交汇点（多线共振）
                   - 张力释放节点（冲突解决/转化）
                   - 新张力生成点（解决旧问题带来新问题）

                3. 节奏控制
                   - 高张力段落（冲突集中爆发）
                   - 低张力段落（角色反思/关系深化/信息铺垫）
                   - 张弛有度的交替规律
                   - 读者注意力的引导策略

                4. 悬念管理
                   - 短线悬念（1-3章解决）
                   - 中线悬念（1卷内解决）
                   - 长线悬念（跨卷解决）
                   - 悬念的揭示节奏

                要求：
                - 张力来自角色动机冲突，非外部强加
                - 禁止"为虐而虐"的廉价张力
                - 张力释放必须有代价和后果
                - 多线并行时注意信息密度控制

                请以 Markdown 格式输出。
                """, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "tension", "content", response));
    }

    /**
     * 逻辑门禁（逻辑漏洞检测）
     */
    private AgentResult checkLogic(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下内容进行逻辑门禁检查：

                内容:
                %s

                检查维度：
                1. 因果链完整性
                   - 每个事件是否有前置原因？
                   - 每个结果是否有合理铺垫？
                   - 是否存在"无因之果"或"有因无果"？

                2. 角色行为一致性
                   - 角色决策是否符合其已建立的性格？
                   - 角色是否突然降智或升智？
                   - 角色信息掌握是否合理（是否知道不该知道的）？

                3. 世界观规则一致性
                   - 是否违反已建立的世界观规则？
                   - 能力/资源使用是否有代价？
                   - 是否存在"双标"（同样情况不同处理）？

                4. 信息逻辑
                   - 角色获取信息的方式是否合理？
                   - 是否存在"上帝视角"泄露？
                   - 信息差是否被合理利用？

                5. 时间线逻辑
                   - 事件顺序是否合理？
                   - 时间跨度是否一致？
                   - 是否有时间悖论？

                需要输出：
                - 发现的逻辑问题（如有）
                - 问题严重程度（1-10）
                - 问题类型（因果链/角色/世界观/信息/时间）
                - 修改建议

                请以 Markdown 格式输出。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "logic_gate", "content", response));
    }

    /**
     * 深度设计（主题深度和情感共鸣）
     */
    private AgentResult designDepth(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline");

        String prompt = String.format("""
                请为以下小说设计主题深度和情感共鸣方案：

                大纲:
                %s

                需要输出：
                1. 主题层次设计
                   - 表面主题（故事直接讲述的）
                   - 深层主题（通过故事探讨的）
                   - 哲学主题（故事背后的思想）
                   - 主题在各章节的递进方式

                2. 情感共鸣设计
                   - 读者与角色的共情点（哪些经历能引发共鸣？）
                   - 情感高潮节点（何时触动读者？）
                   - 情感转化设计（从什么情感转化为什么情感？）
                   - 留白与回味（哪些情感不说透？）

                3. 思想碰撞设计
                   - 不同角色的价值观碰撞
                   - 没有标准答案的道德困境
                   - 读者会思考的问题
                   - 思想的递进和深化

                4. 象征与隐喻
                   - 核心意象（贯穿故事的象征）
                   - 场景隐喻（用环境暗示主题）
                   - 角色象征（角色代表的思想）
                   - 意象的递进变化

                要求：
                - 主题通过故事自然呈现，非说教
                - 情感共鸣基于真实人性，非刻意煽情
                - 思想碰撞有多角度，非单一立场
                - 象征隐喻自然融入，非生硬植入

                请以 Markdown 格式输出。
                """, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "depth", "content", response));
    }

    /**
     * 卡文分析（基于因果链的分析）
     */
    private AgentResult analyzeWriterBlock(Map<String, Object> params, String userInput) {
        String prompt = String.format("""
                请分析以下卡文问题（基于因果链分析）：

                问题描述:
                %s

                分析维度：
                1. 因果链断裂检查
                   - 当前剧情是否缺少前置铺垫？
                   - 是否缺少合理的后续发展？
                   - 是否需要补充中间环节？

                2. 角色动机检查
                   - 角色在当前情境下的动机是否清晰？
                   - 角色是否有合理的行动选择？
                   - 是否需要强化角色动机？

                3. 冲突张力检查
                   - 当前冲突是否还有挖掘空间？
                   - 是否需要引入新的冲突维度？
                   - 冲突解决是否有代价和后果？

                4. 信息节奏检查
                   - 是否信息揭示过快/过慢？
                   - 是否需要调整信息密度？
                   - 是否有未利用的信息差？

                请提供：
                - 卡文原因分析
                - 3种可能的突破方向
                - 每种方向的优缺点
                - 推荐方案

                请以 Markdown 格式输出。
                """, userInput);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "analysis", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是剧情架构师，专精因果链严谨、战略博弈深度的小说剧情设计。

                你的核心理念：
                1. 因果链驱动：每个剧情转折必须有前置铺垫，禁止巧合推动
                2. 战略博弈：角色决策基于信息差和利益考量，非情绪冲动
                3. 张力管理：多线并行的张力曲线，张力来自角色动机冲突
                4. 主题深度：通过故事探讨深刻主题，非简单爽感刺激

                你的设计原则：
                - 禁止"黄金三章"套路：开篇是高信息密度切入，非"被轻视→展示实力"
                - 禁止"爽点模板"：冲突解决有代价和后果，非廉价爽感
                - 禁止"毒点规避"思维：主动检测逻辑漏洞，非被动规避
                - 禁止"情绪曲线"操控：张力来自角色动机冲突，非外部强加

                你的核心能力：
                1. 开篇钩子设计：高信息密度开篇，用悬念和矛盾吸引读者
                2. 张力管理：多线并行的张力曲线，张弛有度的节奏控制
                3. 逻辑门禁：因果链完整性、角色行为一致性、世界观规则一致性检测
                4. 深度设计：主题层次、情感共鸣、思想碰撞、象征隐喻

                输出要求：
                - 使用 Markdown 格式
                - 所有设计必须有因果链支撑
                - 张力来自角色动机冲突
                - 主题通过故事自然呈现
                """;
    }
}
