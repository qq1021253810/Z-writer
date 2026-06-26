package com.zwriter.agent.worldoutline;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 世界观&大纲规划 Agent
 * 负责：赛道选题、世界观搭建、大纲生成、剧情分支推演
 */
@Slf4j
@Component
public class WorldOutlineAgent extends BaseAgent {

    public WorldOutlineAgent() {
        registerSubTask("topic", this::generateTopic);
        registerSubTask("world", this::buildWorld);
        registerSubTask("outline", this::generateOutline);
        registerSubTask("branch", this::generateBranch);
    }

    @Override
    public String name() {
        return "世界观&大纲规划 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "topic";
    }

    /**
     * 赛道选题生成（都市/商战/科幻）
     */
    private AgentResult generateTopic(Map<String, Object> params, String userInput) {
        String prompt = String.format("""
                请基于用户偏好，生成都市/商战/科幻赛道的小说选题方案：

                用户偏好: %s

                需要输出：
                1. 3-5 个差异化选题（每个选题包含核心概念、目标读者、市场定位）
                2. 每个选题的商业逻辑闭环分析（如何吸引读者→如何维持粘性→如何变现）
                3. 每个选题的差异化优势（与同类作品的区别）
                4. 每个选题的可行性评估（创作难度、知识储备要求）
                5. 推荐选题及详细理由

                请以 Markdown 格式输出。
                """, userInput);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "topic", "content", response));
    }

    /**
     * 世界观搭建（商业逻辑/权力结构/科技体系）
     */
    private AgentResult buildWorld(Map<String, Object> params, String userInput) {
        String topic = getParam(params, "topic");

        String prompt = String.format("""
                请为以下小说主题搭建完整的世界观（聚焦都市/商战/科幻）：

                主题: %s

                需要包含：
                1. 商业逻辑体系（核心商业模式、产业链结构、盈利逻辑、资本运作方式）
                2. 权力结构图谱（权力层级、决策机制、利益分配、制衡关系）
                3. 科技/技术体系（如为科幻：技术等级、核心科技、技术限制；如为都市/商战：行业技术壁垒、创新方向）
                4. 社会规则（法律框架、行业规范、潜规则、灰色地带）
                5. 历史背景（关键事件、行业演变、技术发展历程）
                6. 核心矛盾（资源争夺、利益冲突、价值观对立）

                要求：
                - 所有设定必须有内在逻辑自洽性
                - 商业逻辑必须可验证、可推演
                - 权力结构必须有明确的制衡机制
                - 避免"开挂"式设定，所有能力/资源都有代价和限制

                请以 Markdown 格式输出。
                """, topic);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "world", "content", response));
    }

    /**
     * 分级大纲生成（因果链驱动）
     */
    private AgentResult generateOutline(Map<String, Object> params, String userInput) {
        String worldSetting = getParam(params, "worldSetting");

        String prompt = String.format("""
                请基于以下世界观生成小说大纲（因果链驱动）：

                世界观设定:
                %s

                需要生成：
                1. 全书总纲
                   - 核心冲突（必须根植于世界观的内在矛盾）
                   - 主线走向（清晰的因果链条：A 导致 B，B 导致 C）
                   - 主题表达（通过故事想传达什么）

                2. 分卷大纲
                   - 每卷核心目标（必须推动主线发展）
                   - 每卷核心冲突（必须源于角色动机与世界观规则的碰撞）
                   - 关键转折点（必须有前置铺垫，禁止巧合推动）

                3. 伏笔规划
                   - 短线伏笔（1-3 章内回收）
                   - 中线伏笔（1 卷内回收）
                   - 长线伏笔（跨卷回收，需标注埋设点和揭示点）

                要求：
                - 每个剧情转折必须有前置原因
                - 禁止"机械降神"式解决（突然获得新能力/外部力量介入）
                - 所有冲突解决必须基于已建立的规则和角色能力

                请以 Markdown 格式输出。
                """, worldSetting);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "outline", "content", response));
    }

    /**
     * 剧情分支推演（多路线逻辑自洽）
     */
    private AgentResult generateBranch(Map<String, Object> params, String userInput) {
        String outline = getParam(params, "outline");

        String prompt = String.format("""
                请基于以下大纲推演剧情分支（多路线逻辑自洽）：

                大纲:
                %s

                需要输出：
                1. 3-5 条剧情走向
                   - 每条走向的完整因果链（起点→关键节点→结局）
                   - 每条走向的核心冲突差异
                   - 每条走向的主题表达差异

                2. 路线评估
                   - 逻辑自洽性评分（1-10）
                   - 角色动机一致性评分（1-10）
                   - 主题深度评分（1-10）
                   - 读者情感冲击评分（1-10）

                3. 推荐走向及详细理由
                   - 为什么这条路线最优
                   - 可能的风险点
                   - 如何规避风险

                要求：
                - 每条路线必须有独立的逻辑自洽性
                - 禁止"为了反转而反转"的路线设计
                - 所有路线必须基于已建立的角色动机和世界观规则

                请以 Markdown 格式输出。
                """, outline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "branch", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是顶级小说策划大师，专精都市/商战/科幻题材。

                你的核心能力：
                1. 选题策划：生成有商业价值且差异化的选题，每个选题必须有完整的商业逻辑闭环
                2. 世界观构建：搭建逻辑自洽的世界体系，包含完整的商业逻辑链条、权力结构图谱、科技/技术体系
                3. 大纲设计：采用因果链驱动的大纲设计，每个转折必须有前置铺垫，禁止巧合推动剧情
                4. 分支推演：提供多条逻辑自洽的剧情走向，每条路线必须有独立的完整性

                你的设计原则：
                - 逻辑至上：所有设定必须有内在逻辑自洽性，禁止"开挂"式设定
                - 因果严谨：每个剧情转折必须有前置原因，禁止"机械降神"
                - 商业可行：选题和大纲必须有明确的商业价值和读者粘性
                - 主题深刻：通过故事传达深刻的主题思考，而非简单的爽感刺激

                输出要求：
                - 使用 Markdown 格式
                - 结构清晰，层次分明
                - 内容详实，具有可操作性
                - 所有商业逻辑必须可验证、可推演
                """;
    }
}
