package com.zwriter.service;

import com.zwriter.llm.LlmService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 对话引导生成服务
 * 根据步骤类型生成对应的引导建议内容
 */
@Slf4j
@Service
public class DialogueGuideService {

    @Autowired
    private LlmService llmService;

    private static final List<String> GENRE_LIST = Arrays.asList(
            "玄幻", "仙侠", "都市", "科幻", "历史", "游戏", "悬疑"
    );

    /**
     * 根据步骤类型生成引导建议
     *
     * @param stepType     步骤类型
     * @param userInput    用户输入/意图
     * @param existingInfo 已收集的信息
     * @return 生成结果列表
     */
    public List<String> generate(String stepType, String userInput, Map<String, Object> existingInfo) {
        log.info("[对话引导] 开始生成, stepType={}, userInput={}", stepType, userInput);

        try {
            return switch (stepType) {
                case "recommendGenre" -> recommendGenre(userInput);
                case "expandSynopsis" -> expandSynopsis(userInput, existingInfo);
                case "generateTitles" -> generateTitles(userInput, existingInfo);
                case "generateSynopses" -> generateSynopses(userInput, existingInfo);
                case "generatePowerSystem" -> generatePowerSystem(existingInfo);
                case "generateWorldBackground" -> generateWorldBackground(existingInfo);
                case "generateGoldenFinger" -> generateGoldenFinger(userInput, existingInfo);
                case "suggestVolumeCount" -> suggestVolumeCount(existingInfo);
                default -> fallbackGenerate(stepType, userInput, existingInfo);
            };
        } catch (Exception e) {
            log.error("[对话引导] 生成失败, stepType={}", stepType, e);
            return getDefaultSuggestions(stepType, existingInfo);
        }
    }

    /**
     * 推荐题材
     */
    private List<String> recommendGenre(String userInput) {
        String prompt = String.format("""
                你是一个专业的网文小说编辑。根据用户的写作意图，从以下题材中推荐最合适的 3 个，并说明理由。
                
                可选题材：%s
                
                用户意图：%s
                
                请只返回 3 个推荐题材，每个一行，格式为：题材名称 - 推荐理由（简短）
                """, String.join("、", GENRE_LIST), userInput != null ? userInput : "未提供");

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文编辑，擅长根据用户意图推荐合适的小说题材。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认推荐", e);
            return getDefaultGenreRecommendations(userInput);
        }

        if (response == null || response.isBlank()) {
            return getDefaultGenreRecommendations(userInput);
        }

        return Arrays.stream(response.split("\n"))
                .filter(line -> !line.trim().isEmpty())
                .map(String::trim)
                .collect(Collectors.toList());
    }

    /**
     * 扩写梗概
     */
    private List<String> expandSynopsis(String userInput, Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "");
        String prompt = String.format("""
                请将以下小说核心创意扩写为一段完整的梗概（200-300字）。
                
                题材：%s
                核心创意：%s
                
                要求：
                1. 包含主角、核心冲突、故事走向
                2. 符合该题材的常见套路和读者期待
                3. 有吸引力，能激发读者阅读兴趣
                4. 只返回梗概正文，不要其他说明
                """, genre, userInput != null ? userInput : "无");

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文小说策划，擅长将简短创意扩写为吸引人的故事梗概。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认扩写", e);
            return Collections.singletonList(fallbackSynopsis(userInput, genre));
        }

        if (response == null || response.isBlank()) {
            return Collections.singletonList(fallbackSynopsis(userInput, genre));
        }

        return Collections.singletonList(response.trim());
    }

    /**
     * 生成标题建议
     */
    private List<String> generateTitles(String userInput, Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "");
        String synopsis = (String) existingInfo.getOrDefault("synopsis", userInput);

        String prompt = String.format("""
                请为以下小说生成 5 个标题建议。
                
                题材：%s
                梗概：%s
                
                要求：
                1. 标题要有吸引力，符合网文风格
                2. 每个标题 4-10 个字
                3. 只返回标题，每行一个，不要编号和说明
                """, genre, synopsis != null ? synopsis : "无");

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文编辑，擅长起吸引人的小说标题。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认标题", e);
            return getDefaultTitles(genre);
        }

        if (response == null || response.isBlank()) {
            return getDefaultTitles(genre);
        }

        List<String> titles = Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .map(line -> line.replaceAll("^\\d+[.、\\s]*", ""))
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        return titles.isEmpty() ? getDefaultTitles(genre) : titles;
    }

    /**
     * 生成多个梗概版本
     */
    private List<String> generateSynopses(String userInput, Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "");

        String prompt = String.format("""
                请基于以下信息生成 3 个不同风格的小说梗概版本：
                
                题材：%s
                核心创意：%s
                
                要求生成 3 个版本：
                1. 【精简版】50字以内，一句话概括核心
                2. 【标准版】150-200字，包含主角、冲突、走向
                3. 【详细版】300-400字，包含世界观、人物关系、主线剧情
                
                请按以下格式返回：
                【精简版】
                xxx
                
                【标准版】
                xxx
                
                【详细版】
                xxx
                """, genre, userInput != null ? userInput : "无");

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文小说策划，擅长撰写不同长度的故事梗概。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认梗概", e);
            return getDefaultSynopses(userInput, genre);
        }

        if (response == null || response.isBlank()) {
            return getDefaultSynopses(userInput, genre);
        }

        // 尝试按版本分割
        List<String> versions = new ArrayList<>();
        String[] parts = response.split("【.*?版】");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                versions.add(trimmed);
            }
        }

        return versions.isEmpty() ? Collections.singletonList(response.trim()) : versions;
    }

    /**
     * 生成力量体系
     */
    private List<String> generatePowerSystem(Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "玄幻");

        String prompt = String.format("""
                请为以下题材的小说设计完整的力量/修炼体系：
                
                题材：%s
                
                需要包含：
                1. 修炼等级划分（至少 6 个等级）
                2. 每个等级的名称和特征描述
                3. 突破条件或瓶颈
                4. 力量体系的独特之处
                
                请以清晰的层级格式返回。
                """, genre);

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文小说世界观设计师，擅长设计完整自洽的力量体系。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认力量体系", e);
            return Collections.singletonList(getDefaultPowerSystem(genre));
        }

        if (response == null || response.isBlank()) {
            return Collections.singletonList(getDefaultPowerSystem(genre));
        }

        return Collections.singletonList(response.trim());
    }

    /**
     * 生成世界背景
     */
    private List<String> generateWorldBackground(Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "玄幻");

        String prompt = String.format("""
                请为以下题材的小说设计完整的世界背景：
                
                题材：%s
                
                需要包含：
                1. 世界的基本构造（大陆、星球、位面等）
                2. 主要势力/宗门/家族分布
                3. 历史背景和重要事件
                4. 世界规则或法则
                5. 地理环境和特色区域
                
                请以清晰的层级格式返回。
                """, genre);

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文小说世界观设计师，擅长构建宏大自洽的世界背景。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认世界背景", e);
            return Collections.singletonList(getDefaultWorldBackground(genre));
        }

        if (response == null || response.isBlank()) {
            return Collections.singletonList(getDefaultWorldBackground(genre));
        }

        return Collections.singletonList(response.trim());
    }

    /**
     * 生成金手指（主角特殊能力）
     */
    private List<String> generateGoldenFinger(String userInput, Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "");

        String prompt = String.format("""
                请为以下题材的小说主角设计 5 个金手指（特殊能力/外挂）选项：
                
                题材：%s
                用户偏好：%s
                
                要求：
                1. 每个金手指要有独特性和爽点
                2. 符合该题材的读者期待
                3. 包含金手指名称和简短描述
                4. 只返回选项，每行一个，格式：名称 - 描述
                
                注意不要设计过于无敌的外挂，要有成长空间和限制。
                """, genre, userInput != null ? userInput : "无特殊偏好");

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文小说策划，擅长设计有吸引力且有成长空间的主角金手指。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认金手指", e);
            return getDefaultGoldenFingers(genre);
        }

        if (response == null || response.isBlank()) {
            return getDefaultGoldenFingers(genre);
        }

        List<String> goldenFingers = Arrays.stream(response.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .collect(Collectors.toList());

        return goldenFingers.isEmpty() ? getDefaultGoldenFingers(genre) : goldenFingers;
    }

    /**
     * 建议卷数
     */
    private List<String> suggestVolumeCount(Map<String, Object> existingInfo) {
        String genre = (String) existingInfo.getOrDefault("genre", "");
        String synopsis = (String) existingInfo.getOrDefault("synopsis", "");
        String powerSystem = (String) existingInfo.getOrDefault("powerSystem", "");

        String prompt = String.format("""
                请根据以下信息为小说建议合适的分卷数量，并说明每卷的核心内容：
                
                题材：%s
                梗概：%s
                力量体系：%s
                
                要求：
                1. 建议 3-8 卷
                2. 每卷要有明确的核心目标和主题
                3. 返回格式：建议 X 卷，然后简要说明每卷核心内容
                """, genre,
                synopsis != null ? synopsis : "无",
                powerSystem != null ? powerSystem : "无");

        String response;
        try {
            response = llmService.chat(prompt, "你是专业的网文小说编辑，擅长规划小说的整体结构和分卷。");
        } catch (Exception e) {
            log.warn("[对话引导] LLM 调用失败，使用默认卷数建议", e);
            return Collections.singletonList(getDefaultVolumeSuggestion(genre));
        }

        if (response == null || response.isBlank()) {
            return Collections.singletonList(getDefaultVolumeSuggestion(genre));
        }

        return Collections.singletonList(response.trim());
    }

    /**
     * 获取步骤的默认建议（异常回退）
     */
    private List<String> getDefaultSuggestions(String stepType, Map<String, Object> existingInfo) {
        return switch (stepType) {
            case "recommendGenre" -> getDefaultGenreRecommendations(null);
            case "expandSynopsis" -> Collections.singletonList("请手动输入您的故事梗概。");
            case "generateTitles" -> getDefaultTitles((String) existingInfo.getOrDefault("genre", ""));
            case "generateSynopses" -> getDefaultSynopses(null, (String) existingInfo.getOrDefault("genre", ""));
            case "generatePowerSystem" -> Collections.singletonList(getDefaultPowerSystem((String) existingInfo.getOrDefault("genre", "")));
            case "generateWorldBackground" -> Collections.singletonList(getDefaultWorldBackground((String) existingInfo.getOrDefault("genre", "")));
            case "generateGoldenFinger" -> getDefaultGoldenFingers((String) existingInfo.getOrDefault("genre", ""));
            case "suggestVolumeCount" -> Collections.singletonList(getDefaultVolumeSuggestion((String) existingInfo.getOrDefault("genre", "")));
            default -> Collections.singletonList("暂不支持该步骤的自动生成功能，请手动输入。");
        };
    }

    /**
     * 通用 fallback 生成
     */
    private List<String> fallbackGenerate(String stepType, String userInput, Map<String, Object> existingInfo) {
        log.warn("[对话引导] 未知步骤类型: {}，尝试通用生成", stepType);
        return Collections.singletonList("暂不支持该步骤的自动生成功能，请手动输入。");
    }

    // ==================== 默认/回退值 ====================

    private List<String> getDefaultGenreRecommendations(String userInput) {
        List<String> defaults = new ArrayList<>();
        defaults.add("玄幻 - 受众广泛，适合展开宏大世界观");
        defaults.add("都市 - 贴近生活，容易引发读者共鸣");
        defaults.add("仙侠 - 文化底蕴深厚，适合修炼体系设计");
        return defaults;
    }

    private String fallbackSynopsis(String userInput, String genre) {
        return String.format("这是一个%s题材的故事，讲述了%s的冒险历程。主角在成长过程中不断遭遇挑战与机遇，最终实现自我突破。",
                genre != null ? genre : "未知",
                userInput != null ? userInput : "主角");
    }

    private List<String> getDefaultTitles(String genre) {
        return Arrays.asList(
                genre + "之巅峰",
                genre + "传说",
                "绝世" + genre,
                genre + "至尊",
                "不朽" + genre
        );
    }

    private List<String> getDefaultSynopses(String userInput, String genre) {
        String core = userInput != null ? userInput : "一段精彩的故事";
        return Arrays.asList(
                String.format("一个关于%s的故事。", core),
                String.format("在%s的世界里，主角踏上了充满未知与挑战的旅程，经历成长与蜕变。", genre != null ? genre : "未知"),
                String.format("这是一个发生在%s背景下的故事。主角从微末起步，凭借毅力和机遇，一路披荆斩棘，最终站上巅峰。故事融合了热血、友情与成长元素。", genre != null ? genre : "未知")
        );
    }

    private String getDefaultPowerSystem(String genre) {
        return switch (genre) {
            case "玄幻" -> """
                    修炼等级：
                    1. 炼体境 - 淬炼肉身，打基础
                    2. 凝气境 - 凝聚灵气，初窥门径
                    3. 筑基境 - 奠定道基，脱胎换骨
                    4. 金丹境 - 凝结金丹，实力飞跃
                    5. 元婴境 - 元婴出窍，寿元大增
                    6. 化神境 - 神游太虚，半步飞升
                    7. 渡劫境 - 历经天劫，羽化登仙
                    """;
            case "仙侠" -> """
                    修炼等级：
                    1. 练气期 - 引气入体
                    2. 筑基期 - 奠定仙基
                    3. 结丹期 - 凝结金丹
                    4. 元婴期 - 孕育元婴
                    5. 化神期 - 神识大成
                    6. 炼虚期 - 虚实转换
                    7. 合体期 - 肉身元神合一
                    8. 大乘期 - 功德圆满
                    9. 渡劫期 - 飞升仙界
                    """;
            case "都市" -> """
                    实力等级：
                    1. 普通人 - 毫无特殊能力
                    2. 觉醒者 - 初步觉醒异能
                    3. 精英级 - 能力成熟，可独当一面
                    4. 大师级 - 领域级强者
                    5. 宗师级 - 开宗立派的存在
                    6. 传说级 - 都市传说般的存在
                    """;
            default -> """
                    修炼等级：
                    1. 入门境 - 初窥门径
                    2. 小成境 - 略有小成
                    3. 大成境 - 融会贯通
                    4. 圆满境 - 登峰造极
                    5. 突破境 - 超越极限
                    6. 至尊境 - 至高无上
                    """;
        };
    }

    private String getDefaultWorldBackground(String genre) {
        return switch (genre) {
            case "玄幻" -> """
                    世界背景：
                    - 大陆名称：天元大陆
                    - 基本构造：一界九域，中央为天元圣城，四周分布九大域
                    - 主要势力：三大圣宗、八大古族、无数中小宗门
                    - 历史背景：万年前天道崩碎，大能者重开天地
                    - 世界规则：以武为尊，强者为尊
                    """;
            case "仙侠" -> """
                    世界背景：
                    - 世界结构：凡间界、仙界、魔界三界并存
                    - 主要势力：道门三大祖庭、佛门四大名山、魔道六宗
                    - 历史背景：上古封神之战后，三界分立
                    - 世界规则：因果循环，天道轮回
                    """;
            case "科幻" -> """
                    世界背景：
                    - 时代设定：星际纪元 3000 年
                    - 基本构造：银河联邦统治下的数千个殖民星球
                    - 主要势力：联邦政府、星际商会、反抗军联盟
                    - 历史背景：地球资源枯竭后，人类迈向星空
                    - 世界规则：科技即力量，数据即财富
                    """;
            default -> """
                    世界背景：
                    - 世界结构：一个充满机遇与挑战的世界
                    - 主要势力：多方势力并存，相互制衡
                    - 历史背景：经历过大变革后形成的新格局
                    - 世界规则：适者生存，强者为尊
                    """;
        };
    }

    private List<String> getDefaultGoldenFingers(String genre) {
        return Arrays.asList(
                "神秘古玉 - 内含远古传承，可辅助修炼",
                "穿越者系统 - 完成任务获取奖励",
                "时光沙漏 - 可短暂回溯时间，改变关键节点",
                "万物图鉴 - 可查看万物信息，识破弱点",
                "气运珠 - 提升运气，更容易遇到机缘"
        );
    }

    private String getDefaultVolumeSuggestion(String genre) {
        return "建议 4-6 卷：\n" +
                "第一卷：初入世界，奠定基础（约50章）\n" +
                "第二卷：崭露头角，崭露锋芒（约80章）\n" +
                "第三卷：风云变幻，势力崛起（约100章）\n" +
                "第四卷：巅峰对决，问鼎天下（约80章）\n" +
                "第五卷（可选）：新篇章，更高舞台（约100章）\n" +
                "第六卷（可选）：终极决战，圆满结局（约50章）";
    }
}
