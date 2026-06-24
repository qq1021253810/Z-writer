package com.zwriter.workflow.base;

import java.util.Map;

/**
 * 工作流执行结果基类
 * 包含所有工作流共有的字段：成功标志、错误信息、执行耗时
 */
public class BaseWorkflowResult {

    private boolean success;
    private String errorMessage;
    private long durationMs;
    private Map<String, Object> extensions;

    public static Builder<?> builder() {
        return new Builder<>();
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public long getDurationMs() { return durationMs; }
    public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

    public Map<String, Object> getExtensions() { return extensions; }
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }

    /**
     * 通用Builder，支持泛型自引用以兼容子类扩展
     */
    public static class Builder<T extends Builder<T>> {
        private boolean success;
        private String errorMessage;
        private long durationMs;
        private Map<String, Object> extensions;

        @SuppressWarnings("unchecked")
        protected T self() { return (T) this; }

        public T success(boolean success) { this.success = success; return self(); }
        public T errorMessage(String errorMessage) { this.errorMessage = errorMessage; return self(); }
        public T durationMs(long durationMs) { this.durationMs = durationMs; return self(); }
        public T extensions(Map<String, Object> extensions) { this.extensions = extensions; return self(); }

        public BaseWorkflowResult build() {
            BaseWorkflowResult result = new BaseWorkflowResult();
            result.success = this.success;
            result.errorMessage = this.errorMessage;
            result.durationMs = this.durationMs;
            result.extensions = this.extensions;
            return result;
        }

        /**
         * 将已设置的值应用到目标结果对象（供子类Builder使用）
         */
        protected void applyTo(BaseWorkflowResult target) {
            target.success = this.success;
            target.errorMessage = this.errorMessage;
            target.durationMs = this.durationMs;
            target.extensions = this.extensions;
        }
    }
}
