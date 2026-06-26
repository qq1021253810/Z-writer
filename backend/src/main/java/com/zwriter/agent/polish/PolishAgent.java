package com.zwriter.agent.polish;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 润色&文风校准 Agent
 * 负责：文风统一校准、语言润色、章节衔接优化
 */
@Slf4j
@Component
public class PolishAgent extends BaseAgent {

    public PolishAgent() {
        registerSubTask("style", this::calibrateStyle);
        registerSubTask("polish", this::polishText);
        registerSubTask("transition", this::fixTransition);
    }

    @Override
    public String name() {
        return "润色&文风校准 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "style";
    }

    /**
     * 文风统一校准
     */
    private AgentResult calibrateStyle(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下章节进行文风统一校准（冷峻克制风格）：

                章节内容:
                %s

                校准维度：
                1. 风格一致性检查
                   - 是否有段落突然变成网文风格（过度渲染、煽情）？
                   - 是否有段落突然变成散文风格（过度抒情、抽象）？
                   - 全文是否保持冷峻克制的统一调性？

                2. 用词校准
                   - 替换过度渲染的形容词
                   - 删除廉价过渡词（"不禁""不由自主""仿佛"）
                   - 替换直白心理描写为行为暗示
                   - 删除感叹号

                3. 节奏校准
                   - 段落长度是否统一？
                   - 是否有突然变拖沓的段落？
                   - 信息密度是否一致？

                4. 对话校准
                   - 对话是否保持潜台词层次？
                   - 是否有废话/闲聊式对话？
                   - 对话风格是否统一？

                输出格式：
                1. 校准后的完整内容
                2. 修改说明（列出主要修改点和原因）

                请以 Markdown 格式输出。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "style", "content", response));
    }

    /**
     * 语言润色（提升文字质感）
     */
    private AgentResult polishText(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下章节进行语言润色（提升文字质感）：

                章节内容:
                %s

                润色原则：
                1. 保持原意，不过度修改
                2. 提升文字质感和精确度
                3. 删除冗余，增强信息密度
                4. 保持冷峻克制文风

                润色维度：
                1. 精确用词
                   - 用更精确的动词替换模糊动词
                   - 用具体细节替换抽象形容
                   - 删除冗余修饰

                2. 句式优化
                   - 长句拆短，增强节奏
                   - 删除被动表达
                   - 增强句式变化

                3. 段落优化
                   - 删除冗余段落
                   - 合并重复信息
                   - 增强段落间逻辑衔接

                4. 对话优化
                   - 增强对话潜台词
                   - 删除废话
                   - 增强对话节奏感

                请输出润色后的完整内容。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "polish", "content", response));
    }

    /**
     * 章节衔接优化
     */
    private AgentResult fixTransition(Map<String, Object> params, String userInput) {
        String previousChapter = getParam(params, "previousChapter", "");
        String currentChapter = getParam(params, "currentChapter", userInput);

        String prompt = String.format("""
                请检查并优化以下两章之间的衔接：

                上一章结尾（最后500字）:
                %s

                本章开头（前500字）:
                %s

                检查维度：
                1. 时间线连贯性
                   - 时间是否连续？是否有跳跃？
                   - 如需时间跳跃，是否有过渡？

                2. 空间连贯性
                   - 角色位置是否合理？
                   - 场景切换是否自然？

                3. 情节连贯性
                   - 上一章的悬念/伏笔是否在本章有回应？
                   - 情节是否有断裂感？
                   - 因果链是否连续？

                4. 情绪连贯性
                   - 情绪基调是否自然过渡？
                   - 是否有突然的情绪跳跃？

                5. 角色状态连贯性
                   - 角色的身体状态是否连续？
                   - 角色的心理状态是否连续？
                   - 角色掌握的信息是否连续？

                输出：
                1. 发现的问题（如有）
                2. 优化后的本章开头（约500字）
                3. 修改说明

                请以 Markdown 格式输出。
                """, previousChapter, currentChapter);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "transition", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是严肃文学级编辑，专精冷峻克制文风的统一校准和文字质感提升。

                你的核心能力：
                1. 文风校准：检测并修正风格偏差，确保全文保持冷峻克制的统一调性
                2. 语言润色：提升文字质感和精确度，删除冗余，增强信息密度
                3. 章节衔接：确保章节间时间线、空间、情节、情绪的连贯性

                你的编辑原则：
                - 保持原意，不过度修改
                - 注重节奏感和信息密度
                - 用精确的细节替换抽象的形容
                - 删除冗余，增强文字质感
                - 确保全文风格统一

                输出格式：Markdown
                """;
    }
}
