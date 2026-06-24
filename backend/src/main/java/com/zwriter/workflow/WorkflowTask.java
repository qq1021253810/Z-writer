package com.zwriter.workflow;

import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 异步工作流任务（内存存储，Phase 4 后可升级为数据库）
 */
@Data
public class WorkflowTask {
    private String taskId;
    private String workflowName;
    private TaskStatus status;
    private String result;
    private String errorMessage;
    private String currentStep;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> metadata;

    public enum TaskStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }

    public static WorkflowTask create(String workflowName) {
        WorkflowTask task = new WorkflowTask();
        task.setTaskId(UUID.randomUUID().toString());
        task.setWorkflowName(workflowName);
        task.setStatus(TaskStatus.PENDING);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        task.setMetadata(new ConcurrentHashMap<>());
        return task;
    }

    public void updateStatus(TaskStatus status, String currentStep) {
        this.status = status;
        this.currentStep = currentStep;
        this.updatedAt = LocalDateTime.now();
    }
}
