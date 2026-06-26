import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

// 创建工作流 API 实例
const workflowApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 60000, // 工作流可能耗时较长
});

// 小说管理 API
const novelApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

// Agent API
const agentApi = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
});

export interface CreateNovelRequest {
  title: string;
  genre: string;
  synopsis?: string;
  goldenFinger?: string;
  totalVolumes?: number;
  generateTopic?: boolean;
}

export interface ContinueChapterRequest {
  novelId: number;
  volumeNumber: number;
  chapterNumber: number;
  chapterTitle?: string;
  chapterOutline?: string;
  wordCount?: number;
}

export interface FixWriterBlockRequest {
  novelId: number;
  blockDescription: string;
  previousContent: string;
  expectedDirection?: string;
  blockType?: string;
  selectedSolution?: string;
}

export interface WorkflowResponse<T> {
  success: boolean;
  errorMessage?: string;
  durationMs: number;
  data?: T;
}

// 工作流 API
export const workflowService = {
  // 新建小说
  createNovel: async (request: CreateNovelRequest) => {
    const response = await workflowApi.post('/workflow/create-novel', request);
    return response.data;
  },

  // 续写章节
  continueChapter: async (request: ContinueChapterRequest) => {
    const response = await workflowApi.post('/workflow/continue-chapter', request);
    return response.data;
  },

  // 卡文修复
  fixWriterBlock: async (request: FixWriterBlockRequest) => {
    const response = await workflowApi.post('/workflow/fix-writer-block', request);
    return response.data;
  },
};

// 小说管理 API
export const novelService = {
  // 获取小说列表（后端返回 ApiResponse<List<String>>，需要转换为 Novel[]）
  getNovelList: async () => {
    const response = await novelApi.get('/novels');
    const apiResp = response.data;
    if (apiResp?.success && Array.isArray(apiResp.data)) {
      return apiResp.data.map((name: string, index: number) => ({
        id: index + 1,
        title: name,
        genre: '',
        synopsis: '',
        status: '进行中',
        createdAt: '',
      }));
    }
    return [];
  },

  // 获取小说详情（后端返回 ApiResponse<Map>，需要提取 data）
  getNovelDetail: async (novelId: number) => {
    const response = await novelApi.get(`/novels/${novelId}`);
    const apiResp = response.data;
    return apiResp?.data || apiResp;
  },

  // 创建小说
  createNovel: async (data: any) => {
    const response = await novelApi.post('/novels', data);
    const apiResp = response.data;
    return apiResp?.data || apiResp;
  },

  // 更新小说
  updateNovel: async (novelId: number, data: any) => {
    const response = await novelApi.put(`/novels/${novelId}`, data);
    return response.data;
  },

  // 删除小说（后端使用 name 而非 id）
  deleteNovel: async (novelId: number) => {
    // 先获取列表找到对应的 name
    const novels = await novelService.getNovelList();
    const novel = novels.find((n: any) => n.id === novelId);
    if (!novel) throw new Error('小说不存在');
    const response = await novelApi.delete(`/novels/${novel.title}`);
    return response.data;
  },
};

// Agent API
export const agentService = {
  // 执行 Agent 任务
  execute: async (request: any) => {
    const response = await agentApi.post('/agent/execute', request);
    return response.data;
  },
};

// 对话引导 API
export const dialogueGuideService = {
  generate: async (request: { stepType: string; userInput?: string; existingInfo: Record<string, any> }) => {
    const response = await workflowApi.post('/dialogue-guide/generate', request);
    return response.data;
  },
};

export default {
  workflowService,
  novelService,
  agentService,
  dialogueGuideService,
};
