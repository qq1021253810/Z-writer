package com.zwriter.workflow;

import com.zwriter.workflow.base.BaseWorkflow;
import com.zwriter.workflow.base.BaseWorkflowResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 新建小说工作流（对话模式）
 *
 * 通过多轮对话引导用户完成小说创建：
 * Step 1: 题材选择
 * Step 2: 世界观设定
 * Step 3: 角色设定
 * Step 4: 大纲生成
 * Step 5: 黄金三章设计
 * Step 6: 完成并保存所有文件
 */
@Slf4j
@Service
public class CreateNovelWorkflow extends BaseWorkflow<
        CreateNovelWorkflow.CreateNovelRequest,
        CreateNovelWorkflow.WorkflowResult> {

    private static final int TOTAL_STEPS = 6;
    private static final int FINAL_STEP = TOTAL_STEPS;

    // ==================== 对话模式核心方法 ====================

    /**
     * 获取当前进度步骤
     */
    private int getProgressStep(Map<String, Object> context) {
        Object progress = context.get("progress");
        if (progress instanceof Integer p) {
            return p;
        }
        return 1; // 默认从第一步开始
    }

    /**
     * 根据题材生成世界观草稿
     */
    private String generateWorldviewDraft(Map<String, Object> context) {
        String topic = (String) context.get("topic");
        if (topic == null || topic.isBlank()) {
            return "（请先提供题材信息）";
        }

        return switch (topic.toLowerCase()) {
            case "商战" -> """
                **世界观草稿：商战题材**

                - 时代背景：现代都市，金融中心
                - 核心冲突：商业竞争、利益博弈、人性考验
                - 关键场景：写字楼、谈判桌、交易所
                - 力量体系：资本、人脉、信息差
                - 行业特色：金融、科技、实业
                """;
            case "科幻" -> """
                **世界观草稿：科幻题材**

                - 时代背景：近未来/远未来
                - 核心冲突：科技与人性的碰撞
                - 关键场景：太空站、虚拟世界、未来都市
                - 力量体系：科技水平、AI能力、基因改造
                - 核心设定：物理法则、社会形态
                """;
            case "权谋" -> """
                **世界观草稿：权谋题材**

                - 时代背景：古代朝堂/架空王朝
                - 核心冲突：权力斗争、派系博弈
                - 关键场景：朝堂、后宫、军营
                - 力量体系：官阶、军权、情报网
                - 核心设定：权力结构、政治制度
                """;
            case "都市" -> """
                **世界观草稿：都市题材**

                - 时代背景：现代都市生活
                - 核心冲突：事业与情感、理想与现实
                - 关键场景：职场、家庭、社交圈
                - 力量体系：财富、地位、人脉
                - 核心设定：社会阶层、人际关系
                """;
            default -> """
                **世界观草稿**

                - 时代背景：待定
                - 核心冲突：待定
                - 关键场景：待定
                - 力量体系：待定
                - 核心设定：待定
                """;
        };
    }

    /**
     * 获取下一步引导问题
     */
    public DialogueStep getNextStep(String sessionId, Map<String, Object> context) {
        int step = getProgressStep(context);

        return switch (step) {
            case 1 -> new DialogueStep(1, "题材选择",
                "你想写什么类型的小说？（商战、科幻、权谋、都市等）",
                "topic", false);

            case 2 -> {
                String draft = generateWorldviewDraft(context);
                yield new DialogueStep(2, "世界观设定",
                    "基于你的题材，以下是世界观草稿：\n" + draft +
                    "\n\n需要修改吗？还是直接使用？",
                    "worldview", false);
            }

            case 3 -> {
                String topic = (String) context.get("topic");
                String worldview = (String) context.get("worldview");
                yield new DialogueStep(3, "角色设定",
                    "接下来我们定义主要角色。" +
                    "请描述你的主角（姓名、性格、背景、目标等）。" +
                    "也可以一并描述其他重要角色。",
                    "characters", false);
            }

            case 4 -> {
                String topic = (String) context.get("topic");
                yield new DialogueStep(4, "大纲生成",
                    "基于世界观和角色设定，我现在为你生成小说大纲，" +
                    "包括主线剧情、卷次划分和关键转折点。\n\n" +
                    "有什么特别想要包含的剧情元素或转折点吗？" +
                    "（如果没有特殊要求，输入直接使用）",
                    "outline_requirements", false);
            }

            case 5 -> new DialogueStep(5, "黄金三章设计",
                "现在来设计前三章（黄金三章）。" +
                "请告诉我：\n" +
                "1. 开篇想用什么场景吸引读者？\n" +
                "2. 第一章的核心冲突是什么？\n" +
                "3. 前三章要埋下哪些伏笔？\n\n" +
                "（如果不确定，输入由你设计）",
                "golden3_requirements", false);

            case FINAL_STEP -> new DialogueStep(FINAL_STEP, "完成",
                "所有信息已收集完毕！现在开始生成并保存所有文件。\n\n" +
                "请确认以下信息：\n" +
                "题材：" + context.get("topic") + "\n" +
                "世界观：已设定\n" +
                "角色：已设定\n" +
                "大纲：待生成\n" +
                "黄金三章：待设计\n\n" +
                "输入确认开始生成。",
                "final_confirm", true);

            default -> new DialogueStep(TOTAL_STEPS + 1, "已完成",
                "小说创建工作已完成！如需继续，请开始新的会话。",
                "done", true);
        };
    }

    /**
     * 处理用户回复
     */
    public DialogueStep processUserReply(String sessionId, String userReply, Map<String, Object> context) {
        int step = getProgressStep(context);

        // 保存用户回复到 context
        String contextKey = switch (step) {
            case 1 -> "topic";
            case 2 -> "worldview";
            case 3 -> "characters";
            case 4 -> "outline_requirements";
            case 5 -> "golden3_requirements";
            case FINAL_STEP -> "final_confirm";
            default -> "step_" + step + "_reply";
        };
        context.put(contextKey, userReply);

        // 第二步特殊处理：如果用户回复"直接使用"，保存当前草稿
        if (step == 2 && ("直接使用".equals(userReply) || "确认".equals(userReply) || "不用修改".equals(userReply))) {
            context.put("worldview", generateWorldviewDraft(context));
        }

        // 前进到下一步
        context.put("progress", step + 1);

        log.info("[新建小说工作流] 用户完成步骤 {}，进入下一步", step);

        return getNextStep(sessionId, context);
    }

    /**
     * 获取当前步骤数
     */
    public int getCurrentStep(Map<String, Object> context) {
        return getProgressStep(context);
    }

    /**
     * 判断是否已完成所有对话步骤
     */
    public boolean isComplete(Map<String, Object> context) {
        return getProgressStep(context) > FINAL_STEP;
    }

    /**
     * 获取总步骤数
     */
    public int getTotalSteps() {
        return TOTAL_STEPS;
    }

    /**
     * 对话完成后执行实际 Agent 生成和文件保存
     * 该方法由 SessionController 在对话完成后调用
     */
    public WorkflowResult executeGeneration(Map<String, Object> context) {
        log.info("[新建小说工作流] 开始执行 Agent 生成阶段");

        WorkflowResult result = new WorkflowResult();
        try {
            String topic = (String) context.get("topic");
            String worldview = (String) context.get("worldview");
            String characters = (String) context.get("characters");

            result.setTopic(topic);
            result.setWorldSetting(worldview);
            result.setCharacterProfile(characters);

            // TODO: 后续接入实际 Agent 调用
            // 1. WorldOutlineAgent -> worldview.md
            // 2. CharacterAgent -> characters/
            // 3. PlotAgent -> outline.md
            // 4. PlotAgent -> plot_design.md
            // 5. 保存 novel.md

            result.setSuccess(true);
            log.info("[新建小说工作流] Agent 生成阶段完成");
        } catch (Exception e) {
            log.error("[新建小说工作流] Agent 生成阶段失败", e);
            result.setSuccess(false);
            result.setErrorMessage(e.getMessage());
        }

        return result;
    }

    // ==================== 以下为 BaseWorkflow 抽象方法实现（保持兼容性） ====================

    @Override
    protected WorkflowResult doExecute(CreateNovelRequest request) throws Exception {
        log.info("[新建小说工作流] 批量模式执行（待实现）- 标题: {}", request.getTitle());
        WorkflowResult result = new WorkflowResult();
        result.setSuccess(true);
        return result;
    }

    @Override
    protected String getWorkflowName() {
        return "新建小说工作流";
    }

    @Override
    protected String formatRequestInfo(CreateNovelRequest request) {
        return String.format("标题: %s", request.getTitle());
    }

    @Override
    protected WorkflowResult createFailureResult(String errorMessage, long duration) {
        WorkflowResult result = new WorkflowResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        result.setDurationMs(duration);
        return result;
    }

    // ==================== 请求参数 ====================

    public static class CreateNovelRequest {
        private String title;
        private String genre;
        private String synopsis;
        private String goldenFinger;
        private Integer totalVolumes = 1;
        private boolean generateTopic = true;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        public String getSynopsis() { return synopsis; }
        public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

        public String getGoldenFinger() { return goldenFinger; }
        public void setGoldenFinger(String goldenFinger) { this.goldenFinger = goldenFinger; }

        public Integer getTotalVolumes() { return totalVolumes; }
        public void setTotalVolumes(Integer totalVolumes) { this.totalVolumes = totalVolumes; }

        public boolean isGenerateTopic() { return generateTopic; }
        public void setGenerateTopic(boolean generateTopic) { this.generateTopic = generateTopic; }
    }

    // ==================== 工作流结果 ====================

    public static class WorkflowResult extends BaseWorkflowResult {
        private Long novelId;
        private String topic;
        private String worldSetting;
        private String characterProfile;
        private String outline;
        private String golden3Design;
        private Integer totalGeneratedWords;

        public Long getNovelId() { return novelId; }
        public void setNovelId(Long novelId) { this.novelId = novelId; }

        public String getTopic() { return topic; }
        public void setTopic(String topic) { this.topic = topic; }

        public String getWorldSetting() { return worldSetting; }
        public void setWorldSetting(String worldSetting) { this.worldSetting = worldSetting; }

        public String getCharacterProfile() { return characterProfile; }
        public void setCharacterProfile(String characterProfile) { this.characterProfile = characterProfile; }

        public String getOutline() { return outline; }
        public void setOutline(String outline) { this.outline = outline; }

        public String getGolden3Design() { return golden3Design; }
        public void setGolden3Design(String golden3Design) { this.golden3Design = golden3Design; }

        public Integer getTotalGeneratedWords() { return totalGeneratedWords; }
        public void setTotalGeneratedWords(Integer totalGeneratedWords) { this.totalGeneratedWords = totalGeneratedWords; }
    }
}
