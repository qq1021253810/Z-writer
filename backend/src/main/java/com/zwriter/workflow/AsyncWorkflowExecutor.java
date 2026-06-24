package com.zwriter.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步工作流执行器
 * 支持后台执行 + SSE 进度推送
 */
@Slf4j
@Component
public class AsyncWorkflowExecutor {

    private final Map<String, WorkflowTask> taskStore = new ConcurrentHashMap<>();
    private final Map<String, List<SseEmitter>> emitterStore = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 注册任务
     */
    public WorkflowTask registerTask(String workflowName) {
        WorkflowTask task = WorkflowTask.create(workflowName);
        taskStore.put(task.getTaskId(), task);
        emitterStore.put(task.getTaskId(), new CopyOnWriteArrayList<>());
        log.info("[AsyncWorkflow] 注册任务: {} ({})", workflowName, task.getTaskId());
        return task;
    }

    /**
     * 提交异步任务
     */
    public String submit(String taskId, Runnable workflow) {
        WorkflowTask task = taskStore.get(taskId);
        if (task == null) {
            log.error("[AsyncWorkflow] 任务不存在: {}", taskId);
            return null;
        }

        task.updateStatus(WorkflowTask.TaskStatus.RUNNING, "任务开始");
        pushEvent(taskId, "status", Map.of(
                "taskId", taskId,
                "status", "RUNNING",
                "currentStep", "任务开始"
        ));

        executor.submit(() -> {
            try {
                workflow.run();
                task.updateStatus(WorkflowTask.TaskStatus.COMPLETED, "任务完成");
                pushEvent(taskId, "status", Map.of(
                        "taskId", taskId,
                        "status", "COMPLETED",
                        "currentStep", "任务完成"
                ));
                log.info("[AsyncWorkflow] 任务完成: {}", taskId);
            } catch (Exception e) {
                log.error("[AsyncWorkflow] 任务失败: {}", taskId, e);
                task.setStatus(WorkflowTask.TaskStatus.FAILED);
                task.setErrorMessage(e.getMessage());
                task.setUpdatedAt(java.time.LocalDateTime.now());
                pushEvent(taskId, "error", Map.of(
                        "taskId", taskId,
                        "status", "FAILED",
                        "errorMessage", e.getMessage()
                ));
            }
        });

        return taskId;
    }

    /**
     * 查询任务状态
     */
    public WorkflowTask getTask(String taskId) {
        return taskStore.get(taskId);
    }

    /**
     * 清理已完成的任务（避免内存泄漏）
     */
    public void cleanupCompletedTasks() {
        taskStore.entrySet().removeIf(entry -> {
            WorkflowTask task = entry.getValue();
            if (task.getStatus() == WorkflowTask.TaskStatus.COMPLETED ||
                task.getStatus() == WorkflowTask.TaskStatus.FAILED) {
                emitterStore.remove(entry.getKey());
                return true;
            }
            return false;
        });
    }

    /**
     * 更新任务进度
     */
    public void updateProgress(String taskId, String step, Map<String, Object> metadata) {
        WorkflowTask task = taskStore.get(taskId);
        if (task != null) {
            task.setCurrentStep(step);
            if (metadata != null) {
                task.getMetadata().putAll(metadata);
            }
            task.setUpdatedAt(java.time.LocalDateTime.now());
        }
    }

    /**
     * 创建 SSE Emitter 并推送任务进度
     */
    public SseEmitter createSseEmitter(String taskId) {
        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        List<SseEmitter> emitters = emitterStore.computeIfAbsent(taskId, k -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        // 发送初始状态
        WorkflowTask task = taskStore.get(taskId);
        if (task != null) {
            try {
                emitter.send(SseEmitter.event()
                        .name("status")
                        .data(Map.of(
                                "taskId", task.getTaskId(),
                                "status", task.getStatus().name(),
                                "currentStep", task.getCurrentStep()
                        )));
            } catch (Exception e) {
                log.warn("[AsyncWorkflow] SSE 初始发送失败: {}", e.getMessage());
                emitters.remove(emitter);
            }
        }

        // 设置回调
        emitter.onTimeout(() -> {
            log.info("[AsyncWorkflow] SSE 超时: {}", taskId);
            emitters.remove(emitter);
        });
        emitter.onCompletion(() -> {
            log.info("[AsyncWorkflow] SSE 完成: {}", taskId);
            emitters.remove(emitter);
        });
        emitter.onError((ex) -> {
            log.error("[AsyncWorkflow] SSE 错误: {}", taskId, ex);
            emitters.remove(emitter);
        });

        return emitter;
    }

    /**
     * 通过 SSE 推送进度更新到所有连接的客户端
     */
    public void pushProgress(String taskId, String step, Object data) {
        updateProgress(taskId, step, data instanceof Map ? (Map<String, Object>) data : Map.of("detail", data.toString()));
        pushEvent(taskId, "progress", Map.of(
                "taskId", taskId,
                "currentStep", step,
                "data", data
        ));
    }

    /**
     * 推送 SSE 事件到所有连接的客户端
     */
    private void pushEvent(String taskId, String eventName, Object data) {
        List<SseEmitter> emitters = emitterStore.get(taskId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        List<SseEmitter> deadEmitters = new java.util.ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.warn("[AsyncWorkflow] SSE 推送失败: {}", e.getMessage());
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }
}
