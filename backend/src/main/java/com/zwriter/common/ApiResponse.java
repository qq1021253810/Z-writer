package com.zwriter.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一 API 响应包装类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行耗时（毫秒）
     */
    private long durationMs;

    /**
     * 创建成功响应
     */
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .build();
    }

    /**
     * 创建成功响应（带耗时）
     */
    public static <T> ApiResponse<T> success(T data, long durationMs) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .durationMs(durationMs)
                .build();
    }

    /**
     * 创建失败响应
     */
    public static <T> ApiResponse<T> failure(String errorMessage) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }

    /**
     * 创建失败响应（带耗时）
     */
    public static <T> ApiResponse<T> failure(String errorMessage, long durationMs) {
        return ApiResponse.<T>builder()
                .success(false)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }
}