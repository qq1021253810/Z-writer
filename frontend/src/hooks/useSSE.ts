import { useState, useEffect, useRef, useCallback } from 'react';
import { createSSE, SSEController } from '../services/sse';

export interface UseSSEOptions {
  /** 是否立即连接（默认 false，需要手动调用 connect） */
  immediate?: boolean;
  /** 收到 Token 时的回调 */
  onToken?: (token: string) => void;
  /** 收到进度事件时的回调 */
  onProgress?: (progress: { step: string; message: string; percent?: number }) => void;
  /** 收到完成事件时的回调 */
  onComplete?: (data: any) => void;
  /** 收到错误时的回调 */
  onError?: (error: Error) => void;
}

export interface UseSSEReturn {
  /** 当前累积的内容（Token 流式拼接） */
  content: string;
  /** 当前进度信息 */
  progress: { step: string; message: string; percent?: number } | null;
  /** 是否正在连接/接收中 */
  loading: boolean;
  /** 错误信息 */
  error: Error | null;
  /** 是否已完成 */
  completed: boolean;
  /** 手动连接 */
  connect: (url: string, params?: Record<string, any>, method?: 'GET' | 'POST') => void;
  /** 关闭连接 */
  disconnect: () => void;
  /** 手动重连 */
  reconnect: () => void;
  /** 清空内容 */
  clear: () => void;
}

/**
 * SSE React Hook
 * 用于在组件中管理 SSE 连接和流式内容
 */
export function useSSE(options: UseSSEOptions = {}): UseSSEReturn {
  const { onToken, onProgress, onComplete, onError } = options;

  const [content, setContent] = useState('');
  const [progress, setProgress] = useState<{ step: string; message: string; percent?: number } | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);
  const [completed, setCompleted] = useState(false);

  const controllerRef = useRef<SSEController | null>(null);
  const contentRef = useRef('');

  // 清理函数
  const cleanup = useCallback(() => {
    if (controllerRef.current) {
      controllerRef.current.close();
      controllerRef.current = null;
    }
  }, []);

  // 连接函数
  const connect = useCallback((url: string, params?: Record<string, any>, method: 'GET' | 'POST' = 'POST') => {
    cleanup();

    setContent('');
    setProgress(null);
    setError(null);
    setCompleted(false);
    setLoading(true);
    contentRef.current = '';

    const controller = createSSE({
      url,
      params,
      method,
      onToken: (token) => {
        contentRef.current += token;
        setContent(contentRef.current);
        onToken?.(token);
      },
      onProgress: (prog) => {
        setProgress(prog);
        onProgress?.(prog);
      },
      onComplete: (data) => {
        setLoading(false);
        setCompleted(true);
        onComplete?.(data);
      },
      onError: (err) => {
        setLoading(false);
        setError(err);
        onError?.(err);
      },
    });

    controllerRef.current = controller;
  }, [cleanup, onToken, onProgress, onComplete, onError]);

  // 断开连接
  const disconnect = useCallback(() => {
    cleanup();
    setLoading(false);
  }, [cleanup]);

  // 重连
  const reconnect = useCallback(() => {
    if (controllerRef.current) {
      controllerRef.current.reconnect();
      setLoading(true);
      setError(null);
      setCompleted(false);
    }
  }, []);

  // 清空内容
  const clear = useCallback(() => {
    setContent('');
    contentRef.current = '';
    setProgress(null);
    setError(null);
    setCompleted(false);
  }, []);

  // 组件卸载时清理
  useEffect(() => {
    return cleanup;
  }, [cleanup]);

  return {
    content,
    progress,
    loading,
    error,
    completed,
    connect,
    disconnect,
    reconnect,
    clear,
  };
}
