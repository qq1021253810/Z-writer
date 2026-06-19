package com.zwriter.workflow;

import com.zwriter.entity.NovelInfo;
import com.zwriter.repository.NovelInfoRepository;
import com.zwriter.workflow.base.BaseWorkflow;
import com.zwriter.workflow.base.BaseWorkflowResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 新建小说工作流
 * 流程：选题→世界观→人设→大纲→正文→润色→合规
 */
@Slf4j
@Service
public class CreateNovelWorkflow extends BaseWorkflow<
        CreateNovelWorkflow.CreateNovelRequest,
        CreateNovelWorkflow.WorkflowResult> {

    @Autowired
    private NovelInfoRepository novelInfoRepository;

    @Override
    protected WorkflowResult doExecute(CreateNovelRequest request) throws Exception {
        // 1. 创建小说基础信息
        NovelInfo novel = createNovelInfo(request);
        Long novelId = novel.getId();

        // 2. 赛道选题（可选）
        String topicResult = null;
        if (request.isGenerateTopic()) {
            topicResult = generateTopic(novelId, request.getGenre());
        }

        // 3. 世界观搭建
        String worldResult = buildWorld(novelId, request.getGenre(), request.getGoldenFinger());

        // 4. 人物塑造（主角）
        String characterResult = createMainCharacter(novelId, worldResult);

        // 5. 大纲生成
        String outlineResult = generateOutline(novelId, worldResult, request.getTotalVolumes());

        // 6. 黄金三章设计
        String golden3Result = designGolden3(novelId, outlineResult, request.getGenre());

        // 7. 统计生成内容字数
        int totalGeneratedWords = calculateTotalWords(
                topicResult, worldResult, characterResult, outlineResult, golden3Result);

        // 构建结果
        WorkflowResultBuilder builder = new WorkflowResultBuilder();
        builder.success(true);
        builder.novelId(novelId)
               .topic(topicResult)
               .worldSetting(worldResult)
               .characterProfile(characterResult)
               .outline(outlineResult)
               .golden3Design(golden3Result)
               .totalGeneratedWords(totalGeneratedWords);

        return builder.build();
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
        WorkflowResultBuilder builder = new WorkflowResultBuilder();
        builder.success(false).errorMessage(errorMessage).durationMs(duration);
        return builder.build();
    }

    // ==================== 业务步骤 ====================

    private NovelInfo createNovelInfo(CreateNovelRequest request) {
        NovelInfo novel = new NovelInfo();
        novel.setTitle(request.getTitle());
        novel.setGenre(request.getGenre());
        novel.setSynopsis(request.getSynopsis());
        novel.setGoldenFinger(request.getGoldenFinger());
        novel.setTotalVolumes(request.getTotalVolumes());
        novel.setStatus("draft");
        return novelInfoRepository.save(novel);
    }

    private String generateTopic(Long novelId, String genre) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "topic");
        return callAgent(novelId, "world_outline", params, "选题生成失败");
    }

    private String buildWorld(Long novelId, String genre, String goldenFinger) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "world");
        params.put("topic", genre + "类型，金手指：" + goldenFinger);
        return callAgent(novelId, "world_outline", params, "世界观搭建失败");
    }

    private String createMainCharacter(Long novelId, String worldSetting) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "profile");
        params.put("worldSetting", worldSetting);
        params.put("roleType", "主角");
        return callAgent(novelId, "character", params, "角色塑造失败");
    }

    private String generateOutline(Long novelId, String worldSetting, Integer totalVolumes) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "outline");
        params.put("worldSetting", worldSetting);
        params.put("totalVolumes", totalVolumes);
        return callAgent(novelId, "world_outline", params, "大纲生成失败");
    }

    private String designGolden3(Long novelId, String outline, String genre) {
        Map<String, Object> params = new HashMap<>();
        params.put("subTask", "golden3");
        params.put("outline", outline);
        params.put("genre", genre);
        return callAgent(novelId, "plot", params, "黄金三章设计失败");
    }

    private int calculateTotalWords(String... contents) {
        int total = 0;
        for (String content : contents) {
            if (content != null) {
                total += wordCountTool.countWords(content).totalWords();
            }
        }
        log.info("[新建小说工作流] 生成内容总字数: {}", total);
        return total;
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

    // ==================== 扩展Builder ====================

    private static class WorkflowResultBuilder extends BaseWorkflowResult.Builder<WorkflowResultBuilder> {
        private final WorkflowResult result = new WorkflowResult();

        public WorkflowResultBuilder novelId(Long novelId) {
            result.setNovelId(novelId);
            return self();
        }

        public WorkflowResultBuilder topic(String topic) {
            result.setTopic(topic);
            return self();
        }

        public WorkflowResultBuilder worldSetting(String worldSetting) {
            result.setWorldSetting(worldSetting);
            return self();
        }

        public WorkflowResultBuilder characterProfile(String characterProfile) {
            result.setCharacterProfile(characterProfile);
            return self();
        }

        public WorkflowResultBuilder outline(String outline) {
            result.setOutline(outline);
            return self();
        }

        public WorkflowResultBuilder golden3Design(String golden3Design) {
            result.setGolden3Design(golden3Design);
            return self();
        }

        public WorkflowResultBuilder totalGeneratedWords(Integer totalGeneratedWords) {
            result.setTotalGeneratedWords(totalGeneratedWords);
            return self();
        }

        @Override
        public WorkflowResult build() {
            applyTo(result);
            return result;
        }
    }
}
