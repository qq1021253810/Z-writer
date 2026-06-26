/**
 * SSE (Server-Sent Events) 客户端封装
 * 支持自动重连、Token 流式渲染、进度事件
 */
import { useState, useRef, useCallback } from 'react';

export interface SSEOptions {
  /** 连接 URL */
  url: string;
  /** 请求参数（会转为 query string 或 request body） */
  params?: Record<string, any>;
  /** HTTP 方法（GET 使用 EventSource，POST 使用 fetch + ReadableStream） */
  method?: 'GET' | 'POST';
  /** 收到 Token 时的回调 */
  onToken?: (token: string) => void;
  /** 收到进度事件时的回调 */
  onProgress?: (progress: { step: string; message: string; percent?: number }) => void;
  /** 收到完成事件时的回调 */
  onComplete?: (data: any) => void;
  /** 收到错误时的回调 */
  onError?: (error: Error) => void;
  /** 是否自动重连（默认 true） */
  autoReconnect?: boolean;
  /** 重连间隔（毫秒，默认 3000） */
  reconnectInterval?: number;
  /** 最大重连次数（默认 3） */
  maxReconnectAttempts?: number;
}

export interface SSEController {
  /** 关闭连接 */
  close: () => void;
  /** 手动重连 */
  reconnect: () => void;
}

/**
 * 创建 SSE 连接
 * - GET 请求使用原生 EventSource
 * - POST 请求使用 fetch + ReadableStream 模拟 SSE
 */
export function createSSE(options: SSEOptions): SSEController {
  const {
    url,
    params = {},
    method = 'GET',
    onToken,
    onProgress,
    onComplete,
    onError,
    autoReconnect = true,
    reconnectInterval = 3000,
    maxReconnectAttempts = 3,
  } = options;

  let abortController: AbortController | null = null;
  let reconnectAttempts = 0;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let isClosed = false;

  const connect = () => {
    if (isClosed) return;

    if (method === 'GET') {
      connectGET();
    } else {
      connectPOST();
    }
  };

  const connectGET = () => {
    // 构建 query string
    const queryString = new URLSearchParams(params).toString();
    const fullUrl = queryString ? `${url}?${queryString}` : url;

    const eventSource = new EventSource(fullUrl);

    eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        handleSSEEvent(data);
      } catch (e) {
        // 非 JSON 数据，直接作为 token 处理
        onToken?.(event.data);
      }
    };

    eventSource.onerror = (_event) => {
      const error = new Error('SSE connection error');
      onError?.(error);

      if (autoReconnect && reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        reconnectTimer = setTimeout(() => {
          eventSource.close();
          connect();
        }, reconnectInterval);
      } else {
        eventSource.close();
      }
    };

    abortController = {
      abort: () => eventSource.close(),
    } as AbortController;
  };

  const connectPOST = async () => {
    abortController = new AbortController();

    try {
      const response = await fetch(url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Accept': 'text/event-stream',
        },
        body: JSON.stringify(params),
        signal: abortController.signal,
      });

      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const reader = response.body?.getReader();
      if (!reader) {
        throw new Error('Response body is not readable');
      }

      const decoder = new TextDecoder();
      let buffer = '';

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });

        // 按行解析 SSE 数据
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // 保留最后一个不完整的行

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const dataStr = line.slice(5).trim();
            if (dataStr === '[DONE]') {
              onComplete?.({ success: true });
              return;
            }
            try {
              const data = JSON.parse(dataStr);
              handleSSEEvent(data);
            } catch {
              // 非 JSON，作为 token 处理
              onToken?.(dataStr);
            }
          }
        }
      }
    } catch (error: any) {
      if (error.name === 'AbortError') {
        return; // 主动关闭，不报错
      }
      onError?.(error);

      if (autoReconnect && reconnectAttempts < maxReconnectAttempts) {
        reconnectAttempts++;
        reconnectTimer = setTimeout(connect, reconnectInterval);
      }
    }
  };

  const handleSSEEvent = (data: any) => {
    if (!data || typeof data !== 'object') return;

    // 根据事件类型分发
    if (data.type === 'token' && data.content) {
      onToken?.(data.content);
    } else if (data.type === 'progress') {
      onProgress?.({
        step: data.step || '',
        message: data.message || '',
        percent: data.percent,
      });
    } else if (data.type === 'complete' || data.type === 'done') {
      onComplete?.(data.data || data);
    } else if (data.type === 'error') {
      onError?.(new Error(data.message || 'Unknown error'));
    }
  };

  const close = () => {
    isClosed = true;
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
    abortController?.abort();
  };

  const reconnect = () => {
    close();
    isClosed = false;
    reconnectAttempts = 0;
    connect();
  };

  // 启动连接
  connect();

  return { close, reconnect };
}

/**
 * 简化版：仅用于 Token 流式输出
 */
export function streamTokens(
  url: string,
  params: Record<string, any>,
  onToken: (token: string) => void,
  onComplete?: (data: any) => void,
  onError?: (error: Error) => void,
): SSEController {
  return createSSE({
    url,
    params,
    method: 'POST',
    onToken,
    onComplete,
    onError,
    autoReconnect: false,
  });
}

/**
 * 工作流 SSE Hook
 * 用于管理工作流的 SSE 连接和状态
 */

export interface ProgressLog {
  currentStep: string;
  data?: any;
}

export interface UseWorkflowSSEReturn {
  /** 最终结果 */
  result: any;
  /** 进度日志 */
  progressLog: ProgressLog[];
  /** 当前状态 */
  status: 'idle' | 'running' | 'completed' | 'error';
  /** 错误信息 */
  error: string | null;
  /** 是否正在运行 */
  isRunning: boolean;
  /** 当前步骤 */
  currentStep: string | null;
  /** 提交工作流请求 */
  submit: (url: string, params: Record<string, any>) => Promise<void>;
  /** 重置状态 */
  reset: () => void;
}

export function useWorkflowSSE(): UseWorkflowSSEReturn {
  const [result, setResult] = useState<any>(null);
  const [progressLog, setProgressLog] = useState<ProgressLog[]>([]);
  const [status, setStatus] = useState<'idle' | 'running' | 'completed' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);
  const [currentStep, setCurrentStep] = useState<string | null>(null);

  const controllerRef = useRef<SSEController | null>(null);

  const submit = useCallback(async (url: string, params: Record<string, any>) => {
    // 清理之前的连接
    if (controllerRef.current) {
      controllerRef.current.close();
    }

    // 重置状态
    setResult(null);
    setProgressLog([]);
    setError(null);
    setCurrentStep(null);
    setStatus('running');

    return new Promise<void>((resolve, reject) => {
      const controller = createSSE({
        url,
        params,
        method: 'POST',
        onProgress: (progress) => {
          setCurrentStep(progress.step || progress.message);
          setProgressLog(prev => [...prev, {
            currentStep: progress.step || progress.message,
            data: progress,
          }]);
        },
        onToken: (token) => {
          // Token 也记录到进度日志
          setProgressLog(prev => [...prev, {
            currentStep: 'token',
            data: { token },
          }]);
        },
        onComplete: (data) => {
          setResult(data);
          setStatus('completed');
          setCurrentStep(null);
          resolve();
        },
        onError: (err) => {
          setError(err.message);
          setStatus('error');
          setCurrentStep(null);
          reject(err);
        },
        autoReconnect: false,
      });

      controllerRef.current = controller;
    });
  }, []);

  const reset = useCallback(() => {
    if (controllerRef.current) {
      controllerRef.current.close();
      controllerRef.current = null;
    }
    setResult(null);
    setProgressLog([]);
    setStatus('idle');
    setError(null);
    setCurrentStep(null);
  }, []);

  return {
    result,
    progressLog,
    status,
    error,
    isRunning: status === 'running',
    currentStep,
    submit,
    reset,
  };
}
