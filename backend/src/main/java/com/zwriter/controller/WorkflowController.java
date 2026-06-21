package com.zwriter.controller;

import com.zwriter.workflow.CreateNovelWorkflow;
import com.zwriter.workflow.ContinueChapterWorkflow;
import com.zwriter.workflow.FixWriterBlockWorkflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流 API 控制器
 * 提供三种核心工作流的调用接口
 */
@Slf4j
@RestController
@RequestMapping("/api/workflow")
public class WorkflowController {
    
    @Autowired
    private CreateNovelWorkflow createNovelWorkflow;
    
    @Autowired
    private ContinueChapterWorkflow continueChapterWorkflow;
    
    @Autowired
    private FixWriterBlockWorkflow fixWriterBlockWorkflow;
    
    /**
     * 新建小说工作流
     */
    @PostMapping("/create-novel")
    public Map<String, Object> createNovel(@RequestBody CreateNovelWorkflow.CreateNovelRequest request) {
        log.info("收到新建小说请求: title={}, genre={}", request.getTitle(), request.getGenre());

        CreateNovelWorkflow.WorkflowResult result = createNovelWorkflow.execute(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("novelId", result.getNovelId());
        response.put("topic", result.getTopic());
        response.put("worldSetting", result.getWorldSetting());
        response.put("characterProfile", result.getCharacterProfile());
        response.put("outline", result.getOutline());
        response.put("golden3Design", result.getGolden3Design());
        response.put("errorMessage", result.getErrorMessage());
        response.put("durationMs", result.getDurationMs());

        return response;
    }

    /**
     * 续写章节工作流
     */
    @PostMapping("/continue-chapter")
    public Map<String, Object> continueChapter(@RequestBody ContinueChapterWorkflow.ContinueChapterRequest request) {
        log.info("收到续写章节请求: novelId={}, volume={}, chapter={}",
                request.getNovelId(), request.getVolumeNumber(), request.getChapterNumber());

        ContinueChapterWorkflow.WorkflowResult result = continueChapterWorkflow.execute(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("chapterId", result.getChapterId());
        response.put("content", result.getContent());
        response.put("contextUsed", result.getContextUsed());
        response.put("rhythmDesign", result.getRhythmDesign());
        response.put("storyboard", result.getStoryboard());
        response.put("complianceResult", result.getComplianceResult());
        response.put("wordCount", result.getWordCount());
        response.put("errorMessage", result.getErrorMessage());
        response.put("durationMs", result.getDurationMs());

        return response;
    }

    /**
     * 卡文修复工作流
     */
    @PostMapping("/fix-writer-block")
    public Map<String, Object> fixWriterBlock(@RequestBody FixWriterBlockWorkflow.FixWriterBlockRequest request) {
        log.info("收到卡文修复请求: novelId={}", request.getNovelId());

        FixWriterBlockWorkflow.WorkflowResult result = fixWriterBlockWorkflow.execute(request);

        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("analysis", result.getAnalysis());
        response.put("solutions", result.getSolutions());
        response.put("rewrittenContent", result.getRewrittenContent());
        response.put("polishedContent", result.getPolishedContent());
        response.put("errorMessage", result.getErrorMessage());
        response.put("durationMs", result.getDurationMs());

        return response;
    }
}
