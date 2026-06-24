package com.zwriter.error;

/**
 * 工作区操作异常
 */
public class WorkspaceException extends RuntimeException {
    public WorkspaceException(String message) {
        super(message);
    }
    public WorkspaceException(String message, Throwable cause) {
        super(message, cause);
    }
}
