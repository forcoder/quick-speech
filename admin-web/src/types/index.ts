// 通用类型
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  pageSize: number;
}

// 用户相关
export interface User {
  id: string;
  username: string;
  email: string;
  phone?: string;
  role: 'admin' | 'user';
  enterpriseId?: string;
  createdAt: string;
  updatedAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  user: User;
}

// 知识库相关
export interface KnowledgeBase {
  id: string;
  name: string;
  description?: string;
  enabled: boolean;
  documentCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeDocument {
  id: string;
  knowledgeBaseId: string;
  name: string;
  type: 'pdf' | 'word' | 'excel' | 'txt' | 'markdown' | 'web';
  status: 'processing' | 'ready' | 'error';
  chunkCount: number;
  createdAt: string;
}

export interface DocumentChunk {
  id: string;
  documentId: string;
  content: string;
  index: number;
}

// AI智能体相关
export interface AiAgent {
  id: string;
  name: string;
  description?: string;
  promptTemplate: string;
  model: string;
  temperature: number;
  maxTokens: number;
  sceneBindings: SceneBinding[];
  enabled: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface SceneBinding {
  appType: 'email' | 'im' | 'document' | 'other';
  agentId: string;
}

// 统计数据
export interface DashboardStats {
  totalUsers: number;
  activeUsers: number;
  totalKnowledgeBases: number;
  totalDocuments: number;
  totalAgents: number;
  dailyRequests: number;
  adoptionRate: number;
}

export interface UsageData {
  date: string;
  requests: number;
  adopted: number;
}
