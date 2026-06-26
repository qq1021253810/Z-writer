package com.zwriter.agent.strategy;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 战略架构师 Agent
 * 负责：总体战略规划、多线叙事编织、主题深化、长线布局
 */
@Slf4j
@Component
public class StrategyAgent extends BaseAgent {

    public StrategyAgent() {
        registerSubTask("master_plan", this::createMasterPlan);
        registerSubTask("thread_weave", this::weaveThreads);
        registerSubTask("theme_deepen", this::deepenTheme);
        registerSubTask("long_game", this::planLongGame);
    }

    @Override
    public String name() {
        return "战略架构师 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "master_plan";
    }

    /**
     * 总体战略规划
     */
    private AgentResult createMasterPlan(Map<String, Object> params, String userInput) {
        String novelInfo = getParam(params, "novelInfo", userInput);

        String prompt = String.format("""
                请为以下小说制定总体战略规划：

                小说信息:
                %s

                需要输出：
                1. 故事核心定位
                   - 核心冲突（贯穿全书的主线矛盾）
                   - 主题表达（通过故事探讨什么？）
                   - 目标读者（谁会喜欢这个故事？）
                   - 差异化优势（与同类作品的区别）

                2. 整体结构设计
                   - 卷数规划（预计多少卷？）
                   - 每卷核心目标（每卷解决什么问题？）
                   - 卷间递进关系（如何层层深入？）
                   - 高潮节点分布（大高潮在哪几卷？）

                3. 主线剧情走向
                   - 开局设定（如何引入世界和角色？）
                   - 发展阶段（如何逐步展开冲突？）
                   - 转折节点（关键转折点在哪？）
                   - 结局走向（如何收束？）

                4. 商业策略
                   - 读者粘性设计（如何保持追读？）
                   - 节奏控制（张弛有度的节奏规划）
                   - 卖点提炼（核心卖点是什么？）

                要求：
                - 战略必须有全局视角
                - 各卷之间必须有递进关系
                - 主线必须清晰明确
                - 商业策略必须可执行

                请以 Markdown 格式输出。
                """, novelInfo);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "master_plan", "content", response));
    }

    /**
     * 多线叙事编织
     */
    private AgentResult weaveThreads(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline", userInput);

        String prompt = String.format("""
                请为以下大纲设计多线叙事编织方案：

                大纲:
                %s

                需要输出：
                1. 叙事线识别
                   - 主线（核心冲突的推进）
                   - 角色线（主要角色的成长弧线）
                   - 关系线（重要关系的发展）
                   - 主题线（主题的深化过程）
                   - 悬念线（长线悬念的展开）

                2. 叙事线编织
                   - 各线的独立发展轨迹
                   - 各线的交汇节点（多线共振）
                   - 交汇时的冲突设计（如何产生化学反应？）
                   - 交汇后的分离方式（如何继续独立发展？）

                3. 节奏协调
                   - 各线的节奏控制（快慢交替）
                   - 多线并行的信息密度控制
                   - 读者注意力的引导策略
                   - 避免信息过载的方法

                4. 伏笔管理
                   - 各线的伏笔埋设点
                   - 跨线伏笔的呼应设计
                   - 伏笔回收的时机选择
                   - 伏笔之间的关联设计

                要求：
                - 多线必须有明确的独立性
                - 交汇必须有意义（非强行凑在一起）
                - 节奏必须协调（不能一条线太快，一条线太慢）
                - 伏笔必须有系统性

                请以 Markdown 格式输出。
                """, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "thread_weave", "content", response));
    }

    /**
     * 主题深化
     */
    private AgentResult deepenTheme(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline", userInput);

        String prompt = String.format("""
                请为以下大纲设计主题深化方案：

                大纲:
                %s

                需要输出：
                1. 主题层次设计
                   - 表面主题（故事直接讲述的）
                   - 深层主题（通过故事探讨的）
                   - 哲学主题（故事背后的思想）
                   - 各层次之间的关系

                2. 主题递进设计
                   - 开局阶段：主题如何初步呈现？
                   - 发展阶段：主题如何逐步深化？
                   - 高潮阶段：主题如何集中爆发？
                   - 结局阶段：主题如何升华？

                3. 角色与主题
                   - 主角代表什么主题思想？
                   - 对手代表什么主题思想？
                   - 配角如何丰富主题层次？
                   - 角色成长如何体现主题深化？

                4. 冲突与主题
                   - 核心冲突如何体现主题？
                   - 次要冲突如何丰富主题？
                   - 冲突解决如何深化主题？
                   - 主题是否有多角度探讨？

                5. 象征与隐喻
                   - 核心意象（贯穿故事的象征）
                   - 场景隐喻（用环境暗示主题）
                   - 角色象征（角色代表的思想）
                   - 意象的递进变化

                要求：
                - 主题必须通过故事自然呈现
                - 主题深化必须有层次递进
                - 主题探讨必须有多角度
                - 象征隐喻必须自然融入

                请以 Markdown 格式输出。
                """, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "theme_deepen", "content", response));
    }

    /**
     * 长线布局（跨卷伏笔管理）
     */
    private AgentResult planLongGame(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline", userInput);

        String prompt = String.format("""
                请为以下大纲设计长线布局方案（跨卷伏笔管理）：

                大纲:
                %s

                需要输出：
                1. 长线伏笔规划
                   - 开局伏笔（第1卷埋设，哪卷回收？）
                   - 发展伏笔（中期埋设，哪卷回收？）
                   - 高潮伏笔（后期埋设，哪卷回收？）
                   - 伏笔之间的关联设计

                2. 伏笔管理矩阵
                   - 伏笔编号
                   - 埋设位置（卷/章）
                   - 伏笔内容
                   - 回收位置（卷/章）
                   - 伏笔类型（角色/剧情/主题/世界观）
                   - 重要程度（核心/重要/次要）

                3. 伏笔呼应设计
                   - 短线呼应（1-3章内）
                   - 中线呼应（1卷内）
                   - 长线呼应（跨卷）
                   - 伏笔叠加（多个伏笔指向同一结论）

                4. 伏笔回收策略
                   - 回收时机选择（何时回收效果最好？）
                   - 回收方式设计（如何回收才震撼？）
                   - 回收后的影响（回收后带来什么变化？）
                   - 未回收伏笔的处理（是否需要回收？）

                要求：
                - 伏笔必须有系统性规划
                - 伏笔之间必须有关联
                - 回收必须有震撼效果
                - 伏笔管理必须清晰可追踪

                请以 Markdown 表格形式输出伏笔管理矩阵。
                """, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "long_game", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是战略架构师，负责小说的整体战略规划和长期布局。

                你的核心能力：
                1. 总体战略规划：制定故事核心定位、整体结构设计、主线走向、商业策略
                2. 多线叙事编织：管理多条叙事线的独立发展、交汇节点、节奏协调、伏笔管理
                3. 主题深化：设计主题层次、递进方式、角色与主题的关系、象征隐喻
                4. 长线布局：规划跨卷伏笔、管理伏笔矩阵、设计伏笔呼应、制定回收策略

                你的设计原则：
                - 战略必须有全局视角
                - 多线必须有独立性和协调性
                - 主题必须有层次和递进
                - 伏笔必须有系统和关联

                输出格式：Markdown
                """;
    }
}
