package com.zwriter.workflow.base;

import com.zwriter.agent.base.AgentInput;
import com.zwriter.agent.base.AgentResult;
import com.zwriter.agent.controller.ControllerAgent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;

/**
 * 工作流抽象基类
 * 提供通用的Agent调用、错误处理、日志记录等功能
 * 使用模板方法模式统一工作流执行流程
 *
 * @param <REQ> 请求参数类型
 * @param <RES> 结果类型，必须继承BaseWorkflowResult
 */
@Slf4j
public abstract class BaseWorkflow<REQ, RES extends BaseWorkflowResult> {

    @Autowired
    protected ControllerAgent controllerAgent;

    /**
     * 执行工作流（模板方法，final防止子类覆盖）
     * 统一处理：计时、日志、异常捕获
     */
    public final RES execute(REQ request) {
        String name = getWorkflowName();
        long startTime = System.currentTimeMillis();

        log.info("[{}] 开始执行, {}", name, formatRequestInfo(request));

        try {
            RES result = doExecute(request);
            long duration = System.currentTimeMillis() - startTime;
            result.setDurationMs(duration);
            log.info("[{}] 执行完成，耗时：{}ms", name, duration);
            return result;

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("[{}] 执行失败，耗时：{}ms", name, duration, e);
            return createFailureResult(e.getMessage(), duration);
        }
    }

    /**
     * 具体工作流执行逻辑（由子类实现）
     */
    protected abstract RES doExecute(REQ request) throws Exception;

    /**
     * 工作流名称（用于日志标识）
     */
    protected abstract String getWorkflowName();

    /**
     * 格式化请求信息（用于日志）
     */
    protected abstract String formatRequestInfo(REQ request);

    /**
     * 创建失败结果（由子类实现，返回具体类型的结果对象）
     */
    protected abstract RES createFailureResult(String errorMessage, long duration);

    // ==================== Agent调用 ====================

    /**
     * 通用Agent调用方法
     *
     * @param novelId             小说ID
     * @param taskType            任务类型
     * @param params              参数Map
     * @param defaultFailureMessage 失败时的默认消息，null表示失败时返回null
     * @return Agent执行结果内容，失败时返回defaultFailureMessage
     */
    protected String callAgent(Long novelId, String taskType,
                               Map<String, Object> params, String defaultFailureMessage) {
        try {
            AgentInput input = AgentInput.builder()
                    .novelId(novelId)
                    .taskType(taskType)
                    .params(params)
                    .build();

            AgentResult result = controllerAgent.handleRequest(input);

            if (result.isSuccess()) {
                return result.getContent();
            } else {
                log.warn("[{}] Agent调用失败: taskType={}, error={}",
                        getWorkflowName(), taskType, result.getErrorMessage());
                return defaultFailureMessage;
            }
        } catch (Exception e) {
            log.error("[{}] Agent调用异常: taskType={}", getWorkflowName(), taskType, e);
            return defaultFailureMessage;
        }
    }

    /**
     * 便捷Agent调用（空参数）
     */
    protected String callAgent(Long novelId, String taskType, String defaultFailureMessage) {
        return callAgent(novelId, taskType, new HashMap<>(), defaultFailureMessage);
    }
}
