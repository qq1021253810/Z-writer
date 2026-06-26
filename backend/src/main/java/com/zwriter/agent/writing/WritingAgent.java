package com.zwriter.agent.writing;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 正文写作 Agent
 * 负责：章节分镜（信息密度规划）、正文生成（冷峻克制文风）、场景描写（功能性描写）
 */
@Slf4j
@Component
public class WritingAgent extends BaseAgent {

    public WritingAgent() {
        registerSubTask("storyboard", this::generateStoryboard);
        registerSubTask("chapter", this::generateChapter);
        registerSubTask("scene", this::generateScene);
    }

    @Override
    public String name() {
        return "正文写作 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "storyboard";
    }

    /**
     * 章节分镜（信息密度规划）
     */
    private AgentResult generateStoryboard(Map<String, Object> params, String userInput) {
        String chapterOutline = getParam(params, "chapterOutline", userInput);

        String prompt = String.format("""
                请为以下章节大纲生成分镜脚本（信息密度规划）：

                章节大纲:
                %s

                分镜要求：
                1. 场景划分
                   - 每个场景的起止位置
                   - 场景核心信息（该场景传达什么？）
                   - 场景功能（推动剧情/揭示角色/埋设伏笔/深化主题）

                2. 信息密度控制
                   - 每个场景揭示什么信息
                   - 每个场景隐藏什么信息
                   - 信息揭示方式（直接展示/暗示/对话透露/行动体现）

                3. 视角与节奏
                   - 视角选择（谁的视角？为什么？）
                   - 节奏标注（紧凑/舒缓/张弛交替）
                   - 段落长度建议

                4. 伏笔管理
                   - 本章埋设的伏笔
                   - 本章呼应的伏笔
                   - 伏笔呈现方式（细节/对话/场景暗示）

                5. 情绪基调
                   - 场景情绪（克制中的张力/暗流涌动/表面平静）
                   - 情绪转换节点

                请以 Markdown 格式输出。
                """, chapterOutline);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "storyboard", "content", response));
    }

    /**
     * 正文生成（冷峻克制文风）
     */
    private AgentResult generateChapter(Map<String, Object> params, String userInput) {
        String storyboard = getParam(params, "storyboard", userInput);
        String wordCount = getParam(params, "wordCount", "3000");

        String prompt = String.format("""
                请基于以下分镜脚本生成正文（冷峻克制文风）：

                分镜脚本:
                %s

                文风要求：
                - 目标字数: %s 字
                - 冷峻克制：不煽情，不渲染，用事实和细节说话
                - 信息密度高：每段都有信息量，无废话
                - 暗示大于直述：用行动和细节暗示心理和关系

                写作规则：
                1. 禁止过度渲染
                   - 不用"他心中暗道"等直白心理描写
                   - 不用"不约而同""不由自主"等廉价过渡
                   - 不用感叹号表达情绪
                   - 情绪通过行为和细节体现

                2. 对话设计
                   - 每句对话有潜台词
                   - 对话中有信息差和博弈
                   - 对话推动剧情，非闲聊
                   - 言外之意 > 字面意思

                3. 场景描写
                   - 功能性描写（暗示心理/烘托氛围/埋设伏笔）
                   - 用具体细节而非抽象形容
                   - 场景与剧情/角色状态呼应

                4. 节奏控制
                   - 紧张场景：短句、动词密集、留白
                   - 对话场景：潜台词层次、信息博弈
                   - 过渡场景：简洁、信息承载

                5. 禁止事项
                   - 禁止"他不禁""他忍不住"等被动表达
                   - 禁止"仿佛""好像"等模糊比喻
                   - 禁止大段心理独白
                   - 禁止强行说教
                   - 禁止水字数

                请直接输出正文内容。
                """, storyboard, wordCount);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "chapter", "content", response, "wordCount", response.length()));
    }

    /**
     * 场景描写（功能性描写）
     */
    private AgentResult generateScene(Map<String, Object> params, String userInput) {
        String sceneType = getParam(params, "sceneType", "");
        String sceneDesc = getParam(params, "sceneDesc", userInput);
        String characterState = getParam(params, "characterState", "");

        String prompt = String.format("""
                请生成以下场景描写（功能性描写）：

                场景类型: %s
                场景描述: %s
                角色状态: %s

                描写原则：
                1. 功能性：每处描写都有功能（暗示心理/烘托氛围/埋设伏笔/呼应主题）
                2. 具体细节：用具体、可感知的细节，非抽象形容
                3. 克制表达：不渲染，不煽情，让细节自己说话
                4. 与角色呼应：场景与角色当前状态/心理形成呼应或反差

                描写维度：
                - 视觉细节（具体物件、光线、色彩）
                - 听觉细节（环境音、沉默、节奏）
                - 触觉/温度（暗示心理状态）
                - 空间感（压迫/开阔/封闭）

                禁止：
                - 堆砌辞藻
                - 为描写而描写
                - 抽象抒情
                - 与剧情/角色无关的描写

                请直接输出场景描写内容。
                """, sceneType, sceneDesc, characterState);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "scene", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是严肃文学级写手，专精冷峻克制、信息密度高的文风。

                你的文风定位：
                - 冷峻克制：不煽情，不渲染，用事实和细节说话
                - 信息密度高：每段都有信息量，无废话
                - 暗示大于直述：用行动和细节暗示心理和关系
                - 严肃文学质感：追求文学性，非网文快餐

                你的写作原则：
                1. 禁止过度渲染
                   - 不用直白心理描写（"他心中暗道"）
                   - 不用廉价过渡词（"不约而同""不由自主"）
                   - 不用感叹号表达情绪
                   - 情绪通过行为和细节体现

                2. 对话设计
                   - 每句对话有潜台词和目的
                   - 对话中有信息差和博弈
                   - 言外之意 > 字面意思
                   - 对话推动剧情，非闲聊

                3. 场景描写
                   - 功能性描写（暗示心理/烘托氛围/埋设伏笔）
                   - 用具体细节而非抽象形容
                   - 场景与剧情/角色状态呼应
                   - 不为描写而描写

                4. 节奏控制
                   - 紧张场景：短句、动词密集、留白
                   - 对话场景：潜台词层次、信息博弈
                   - 过渡场景：简洁、信息承载

                5. 禁止事项
                   - 禁止"他不禁""他忍不住"等被动表达
                   - 禁止"仿佛""好像"等模糊比喻
                   - 禁止大段心理独白
                   - 禁止强行说教
                   - 禁止水字数

                你的核心能力：
                1. 分镜脚本：规划信息密度、伏笔管理、节奏控制
                2. 正文生成：冷峻克制文风，高信息密度
                3. 场景描写：功能性描写，用细节暗示心理

                输出要求：
                - 严格遵循分镜脚本
                - 保持文风统一
                - 信息密度高，无废话
                - 用细节和行动说话
                """;
    }
}
