package com.zwriter.controller;

import com.zwriter.common.ApiResponse;
import com.zwriter.workflow.AsyncWorkflowExecutor;
import com.zwriter.workflow.ContinueChapterWorkflow;
import com.zwriter.workflow.FixWriterBlockWorkflow;
import com.zwriter.workflow.WorkflowTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;

/**
 * 异步工作流 API（异步任务 + SSE 进度推送）
 * - POST /api/workflows/create-novel → 创建小说工作流（异步）
 * - POST /api/workflows/continue → 续写工作流（异步）
 * - POST /api/workflows/fix-block → 卡文修复工作流（同步）
 * - GET /api/workflows/{taskId}/status → 查询任务状态（SSE）
 */
@Slf4j
@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    @Autowired
    private AsyncWorkflowExecutor workflowExecutor;

    @Autowired
    private ContinueChapterWorkflow continueWorkflow;

    @Autowired
    private FixWriterBlockWorkflow fixWorkflow;

    /**
     * 创建小说工作流（异步）
     */
    @PostMapping("/create-novel")
    public ApiResponse<Map<String, Object>> createNovel(@RequestBody Map<String, Object> request) {
        WorkflowTask task = workflowExecutor.registerTask("create-novel");

        // 提交异步任务
        workflowExecutor.submit(task.getTaskId(), () -> {
            workflowExecutor.updateProgress(task.getTaskId(), "创建工作区", null);
            // TODO: Phase 3 填充实际工作流逻辑
            workflowExecutor.updateProgress(task.getTaskId(), "任务完成", null);
        });

        return ApiResponse.success(Map.of(
                "taskId", task.getTaskId(),
                "status", task.getStatus().name(),
                "message", "工作流已提交，使用 /api/workflows/{taskId}/status 查看进度"
        ));
    }

    /**
     * 续写章节工作流（异步）
     */
    @PostMapping("/continue")
    public ApiResponse<Map<String, Object>> continueChapter(@RequestBody Map<String, Object> request) {
        String novelName = (String) request.get("novelName");
        if (novelName == null) {
            return ApiResponse.failure("缺少参数: novelName");
        }

        WorkflowTask task = workflowExecutor.registerTask("continue-chapter");

        workflowExecutor.submit(task.getTaskId(), () -> {
            try {
                workflowExecutor.updateProgress(task.getTaskId(), "读取上下文", null);
                ContinueChapterWorkflow.WorkflowResult result = continueWorkflow.continueChapter(novelName);
                workflowExecutor.updateProgress(task.getTaskId(), "生成章节", null);

                if (result.success()) {
                    task.setResult(result.content());
                    task.getMetadata().put("chapterNum", result.metadata().get("chapterNum"));
                    task.getMetadata().put("wordCount", result.metadata().get("wordCount"));
                    task.updateStatus(WorkflowTask.TaskStatus.COMPLETED, "续写完成");
                } else {
                    task.setErrorMessage(result.errorMessage());
                    task.updateStatus(WorkflowTask.TaskStatus.FAILED, "续写失败");
                }
            } catch (Exception e) {
                task.setErrorMessage(e.getMessage());
                task.updateStatus(WorkflowTask.TaskStatus.FAILED, "异常: " + e.getMessage());
                log.error("[WorkflowController] 续写失败", e);
            }
        });

        return ApiResponse.success(Map.of(
                "taskId", task.getTaskId(),
                "status", task.getStatus().name()
        ));
    }

    /**
     * 卡文修复工作流（同步）
     */
    @PostMapping("/fix-block")
    public ApiResponse<Map<String, Object>> fixWriterBlock(@RequestBody Map<String, Object> request) {
        String novelName = (String) request.get("novelName");
        String problem = (String) request.get("problemDescription");

        if (novelName == null || problem == null) {
            return ApiResponse.failure("缺少参数: novelName, problemDescription");
        }

        try {
            FixWriterBlockWorkflow.WorkflowResult result = fixWorkflow.fixWriterBlock(novelName, problem);
            return result.success()
                    ? ApiResponse.success(Map.of("content", result.content()))
                    : ApiResponse.failure(result.errorMessage());
        } catch (IOException e) {
            return ApiResponse.failure("卡文修复失败: " + e.getMessage());
        }
    }

    /**
     * 查询任务状态（SSE 实时推送）
     */
    @GetMapping(value = "/{taskId}/status", produces = "text/event-stream")
    public SseEmitter getTaskStatus(@PathVariable String taskId) {
        return workflowExecutor.createSseEmitter(taskId);
    }

    /**
     * 查询任务状态（JSON 一次性返回）
     */
    @GetMapping("/{taskId}")
    public ApiResponse<WorkflowTask> getTask(@PathVariable String taskId) {
        WorkflowTask task = workflowExecutor.getTask(taskId);
        if (task == null) {
            return ApiResponse.failure("任务不存在: " + taskId);
        }
        return ApiResponse.success(task);
    }
}
