package com.zwriter.workflow.base;

import com.zwriter.tool.BannedWordTool;
import com.zwriter.tool.WordCountTool;

import java.util.Map;

/**
 * 工作流执行结果基类
 * 包含所有工作流共有的字段：成功标志、错误信息、执行耗时、内容检测结果
 */
public class BaseWorkflowResult {

    private boolean success;
    private String errorMessage;
    private long durationMs;
    private WordCountTool.WordCountResult wordCountResult;
    private BannedWordTool.BannedWordResult bannedWordResult;
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

    public WordCountTool.WordCountResult getWordCountResult() { return wordCountResult; }
    public void setWordCountResult(WordCountTool.WordCountResult wordCountResult) { this.wordCountResult = wordCountResult; }

    public BannedWordTool.BannedWordResult getBannedWordResult() { return bannedWordResult; }
    public void setBannedWordResult(BannedWordTool.BannedWordResult bannedWordResult) { this.bannedWordResult = bannedWordResult; }

    public Map<String, Object> getExtensions() { return extensions; }
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }

    /**
     * 通用Builder，支持泛型自引用以兼容子类扩展
     */
    public static class Builder<T extends Builder<T>> {
        private boolean success;
        private String errorMessage;
        private long durationMs;
        private WordCountTool.WordCountResult wordCountResult;
        private BannedWordTool.BannedWordResult bannedWordResult;
        private Map<String, Object> extensions;

        @SuppressWarnings("unchecked")
        protected T self() { return (T) this; }

        public T success(boolean success) { this.success = success; return self(); }
        public T errorMessage(String errorMessage) { this.errorMessage = errorMessage; return self(); }
        public T durationMs(long durationMs) { this.durationMs = durationMs; return self(); }
        public T wordCountResult(WordCountTool.WordCountResult wordCountResult) { this.wordCountResult = wordCountResult; return self(); }
        public T bannedWordResult(BannedWordTool.BannedWordResult bannedWordResult) { this.bannedWordResult = bannedWordResult; return self(); }
        public T extensions(Map<String, Object> extensions) { this.extensions = extensions; return self(); }

        public BaseWorkflowResult build() {
            BaseWorkflowResult result = new BaseWorkflowResult();
            result.success = this.success;
            result.errorMessage = this.errorMessage;
            result.durationMs = this.durationMs;
            result.wordCountResult = this.wordCountResult;
            result.bannedWordResult = this.bannedWordResult;
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
            target.wordCountResult = this.wordCountResult;
            target.bannedWordResult = this.bannedWordResult;
            target.extensions = this.extensions;
        }
    }
}
