package com.zwriter.agent.compliance;

import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.base.BaseAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 合规&质量审查 Agent
 * 负责：内容审查、元数据生成、梗概生成
 */
@Slf4j
@Component
public class ComplianceAgent extends BaseAgent {

    public ComplianceAgent() {
        registerSubTask("content_review", this::contentReview);
        registerSubTask("metadata", this::generateMetadata);
        registerSubTask("synopsis", this::generateSynopsis);
    }

    @Override
    public String name() {
        return "合规&质量审查 Agent";
    }

    @Override
    protected String defaultSubTask() {
        return "content_review";
    }

    /**
     * 内容审查（出版物级别）
     */
    private AgentResult contentReview(Map<String, Object> params, String userInput) {
        String chapterContent = getParam(params, "chapterContent", userInput);

        String prompt = String.format("""
                请对以下章节内容进行出版物级别审查：

                章节内容:
                %s

                审查维度：
                1. 内容合规性
                   - 政治敏感内容
                   - 色情/暴力描写
                   - 违禁词/敏感词
                   - 价值观导向
                   - 宗教/民族相关

                2. 质量门禁
                   - 角色智商是否在线？（是否有突然降智？）
                   - 逻辑是否严谨？（是否有逻辑漏洞？）
                   - 文风是否统一？（是否有风格偏差？）
                   - 信息密度是否足够？（是否有水字数？）

                3. 文学性评估
                   - 文字质感（是否达到严肃文学标准？）
                   - 对话质量（是否有潜台词层次？）
                   - 场景描写（是否功能性描写？）
                   - 主题深度（是否有思想性？）

                输出格式：
                1. 合规性评估（通过/不通过）
                2. 风险点列表（如有）
                3. 质量评分（1-10）
                   - 角色智商评分
                   - 逻辑严谨度评分
                   - 文风统一度评分
                   - 文学性评分
                4. 修改建议

                请以 Markdown 格式输出。
                """, chapterContent);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "content_review", "content", response));
    }

    /**
     * 元数据生成（专业小说元数据）
     */
    private AgentResult generateMetadata(Map<String, Object> params, String userInput) {
        String novelInfo = getParam(params, "novelInfo", userInput);

        String prompt = String.format("""
                请为以下小说生成专业元数据：

                小说信息:
                %s

                需要生成：
                1. 主标签（1-2 个，如：都市、商战、科幻）
                2. 副标签（3-5 个，如：权力博弈、战略谋划、科技革命）
                3. 搜索关键词（5-10 个）
                4. 目标读者画像
                5. 核心卖点提炼

                要求：
                - 标签要精准，避免网文套路标签
                - 关键词要有区分度
                - 读者画像要具体

                请以 Markdown 格式输出。
                """, novelInfo);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "metadata", "content", response));
    }

    /**
     * 梗概生成（高质量故事梗概）
     */
    private AgentResult generateSynopsis(Map<String, Object> params, String userInput) {
        String novelInfo = getParam(params, "novelInfo", userInput);

        String prompt = String.format("""
                请为以下小说生成高质量梗概：

                小说信息:
                %s

                需要生成 3 个版本：
                1. 短版（50 字以内，一句话概括核心冲突）
                2. 中版（150 字以内，包含核心冲突和主题）
                3. 长版（300 字以内，完整故事梗概）

                要求：
                - 突出核心冲突和主题深度
                - 避免网文套路化表达
                - 体现故事的独特性和思想性
                - 语言精炼，有文学质感

                请以 Markdown 格式输出。
                """, novelInfo);

        String response = callLlm(prompt);
        return AgentResult.success(response, Map.of("type", "synopsis", "content", response));
    }

    @Override
    protected String buildSystemPrompt() {
        return """
                你是出版物级别的内容审查专家和质量把关人。

                你的核心能力：
                1. 内容审查：检测合规风险，确保内容符合出版标准
                2. 质量门禁：评估角色智商、逻辑严谨度、文风统一度、文学性
                3. 元数据生成：生成专业的小说标签、关键词、读者画像
                4. 梗概生成：生成高质量、有文学质感的故事梗概

                你的审查原则：
                - 严格遵循出版规范
                - 准确识别质量风险
                - 提供可操作的修改建议
                - 追求文学性和思想性

                输出格式：Markdown
                """;
    }
}
