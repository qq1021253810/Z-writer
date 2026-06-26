package com.zwriter.agent.review;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 质量审计 Agent
 * 负责：剧情逻辑检查、角色行为一致性检查、文风统一性审计、智商在线门禁
 */
@Slf4j
@Component
public class ReviewAgent extends BaseAgent {

    public ReviewAgent() {
        registerSubTask("logic_check", this::checkLogic);
        registerSubTask("character_consistency", this::checkCharacterConsistency);
        registerSubTask("style_audit", this::auditStyle);
        registerSubTask("iq_gate", this::checkIQ);
    }

    @Override
    public String name() {
        return "质量审计 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "logic_check";
    }

    /**
     * 剧情逻辑检查（因果链完整性审计）
     */
    private AgentResult checkLogic(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下内容进行剧情逻辑检查（因果链完整性审计）：

                内容:
                %s

                检查维度：
                1. 因果链完整性
                   - 每个事件是否有明确的前置原因？
                   - 每个结果是否有合理的后续影响？
                   - 是否存在"无因之果"或"有因无果"？

                2. 逻辑漏洞检测
                   - 是否有前后矛盾的情节？
                   - 是否有违反已建立规则的情况？
                   - 是否有"机械降神"式解决（突然获得新能力/外部力量介入）？

                3. 信息逻辑
                   - 角色获取信息的方式是否合理？
                   - 是否有"上帝视角"泄露？
                   - 信息差是否被合理利用？

                4. 时间线逻辑
                   - 事件顺序是否合理？
                   - 时间跨度是否一致？
                   - 是否有时间悖论？

                输出格式：
                1. 逻辑问题列表（如有）
                2. 每个问题的严重程度（1-10）
                3. 每个问题的具体位置
                4. 修改建议

                请以 Markdown 格式输出。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "logic_check", "content", response));
    }

    /**
     * 角色行为一致性检查
     */
    private AgentResult checkCharacterConsistency(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);
        String characterProfiles = getParam(params, "characterProfiles", "");

        String prompt = String.format("""
                请对以下内容进行角色行为一致性检查：

                章节内容:
                %s

                角色档案:
                %s

                检查维度：
                1. 行为与性格一致性
                   - 角色决策是否符合其已建立的性格？
                   - 角色行为是否符合其核心信念？
                   - 是否有突然的性格转变（无铺垫）？

                2. 动机与行为一致性
                   - 角色行为是否符合其动机？
                   - 是否有"为反而反"的行为？
                   - 行为是否有充分的动机支撑？

                3. 信息掌握一致性
                   - 角色是否知道不该知道的？
                   - 角色是否忘记该知道的？
                   - 信息掌握是否符合其经历？

                4. 能力边界一致性
                   - 角色是否做了超出其能力的事？
                   - 角色能力是否突然提升（无铺垫）？
                   - 能力使用是否有代价？

                输出格式：
                1. 一致性问题列表（如有）
                2. 每个问题的严重程度（1-10）
                3. 每个问题的具体位置
                4. 修改建议

                请以 Markdown 格式输出。
                """, chapterContent, characterProfiles);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "character_consistency", "content", response));
    }

    /**
     * 文风统一性审计
     */
    private AgentResult auditStyle(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下内容进行文风统一性审计：

                内容:
                %s

                审计维度：
                1. 风格一致性
                   - 全文是否保持冷峻克制的统一调性？
                   - 是否有段落突然变成网文风格（过度渲染、煽情）？
                   - 是否有段落突然变成散文风格（过度抒情、抽象）？

                2. 用词一致性
                   - 是否有廉价过渡词（"不禁""不由自主""仿佛"）？
                   - 是否有直白心理描写（"他心中暗道"）？
                   - 是否有感叹号表达情绪？

                3. 节奏一致性
                   - 段落长度是否统一？
                   - 是否有突然变拖沓的段落？
                   - 信息密度是否一致？

                4. 对话一致性
                   - 对话风格是否统一？
                   - 是否有废话/闲聊式对话？
                   - 对话是否保持潜台词层次？

                输出格式：
                1. 文风偏差列表（如有）
                2. 每个偏差的严重程度（1-10）
                3. 每个偏差的具体位置
                4. 修改建议

                请以 Markdown 格式输出。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "style_audit", "content", response));
    }

    /**
     * 智商在线门禁（检查是否有角色突然降智）
     */
    private AgentResult checkIQ(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下内容进行智商在线门禁检查：

                内容:
                %s

                检查维度：
                1. 角色智商检查
                   - 角色决策是否体现其认知水平？
                   - 是否有突然降智的情况？
                   - 是否有"为了剧情需要而变蠢"？

                2. 信息利用检查
                   - 角色是否充分利用了已知信息？
                   - 是否有明显的信息遗漏（角色该想到但没想到）？
                   - 信息利用是否符合角色智商？

                3. 策略合理性检查
                   - 角色的策略是否合理？
                   - 是否有更好的解决方案被忽略？
                   - 策略是否符合角色的专业背景？

                4. 对手智商检查
                   - 对手是否有合理的策略？
                   - 对手是否突然变蠢（为了主角胜利）？
                   - 对手的失败是否有合理原因？

                输出格式：
                1. 智商问题列表（如有）
                2. 每个问题的严重程度（1-10）
                3. 每个问题的具体位置
                4. 修改建议

                请以 Markdown 格式输出。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "iq_gate", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是质量审计专家，负责对小说内容进行全方位的质量检查。

                你的核心能力：
                1. 剧情逻辑检查：审计因果链完整性，检测逻辑漏洞
                2. 角色行为一致性检查：确保角色行为符合已建立的性格和动机
                3. 文风统一性审计：确保全文保持冷峻克制的统一调性
                4. 智商在线门禁：检查是否有角色突然降智

                你的审计原则：
                - 严格检查因果链完整性
                - 确保角色行为一致性
                - 维护文风统一性
                - 保证角色智商在线

                输出格式：Markdown
                """;
    }
}
