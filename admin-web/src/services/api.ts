import axios from 'axios';
import { useAuthStore } from '../stores/authStore';

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().token;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (response) => response.data,
  (error) => {
    if (error.response?.status === 401) {
      useAuthStore.getState().logout();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

// 认证
export const login = (data: { username: string; password: string }) =>
  api.post('/auth/login', data);

// 知识库
export const getKnowledgeBases = (params?: { page?: number; pageSize?: number }) =>
  api.get('/knowledge-bases', { params });

export const createKnowledgeBase = (data: { name: string; description?: string }) =>
  api.post('/knowledge-bases', data);

export const updateKnowledgeBase = (id: string, data: Partial<{ name: string; description: string; enabled: boolean }>) =>
  api.put(`/knowledge-bases/${id}`, data);

export const deleteKnowledgeBase = (id: string) =>
  api.delete(`/knowledge-bases/${id}`);

export const uploadDocument = (knowledgeBaseId: string, file: FormData) =>
  api.post(`/knowledge-bases/${knowledgeBaseId}/documents`, file, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });

export const getDocuments = (knowledgeBaseId: string) =>
  api.get(`/knowledge-bases/${knowledgeBaseId}/documents`);

export const searchKnowledge = (knowledgeBaseId: string, query: string) =>
  api.post(`/knowledge-bases/${knowledgeBaseId}/search`, { query });

// AI智能体
export const getAgents = (params?: { page?: number; pageSize?: number }) =>
  api.get('/agents', { params });

export const createAgent = (data: Record<string, unknown>) =>
  api.post('/agents', data);

export const updateAgent = (id: string, data: Record<string, unknown>) =>
  api.put(`/agents/${id}`, data);

export const deleteAgent = (id: string) =>
  api.delete(`/agents/${id}`);

export const testAgent = (id: string, message: string) =>
  api.post(`/agents/${id}/test`, { message });

// 用户管理
export const getUsers = (params?: { page?: number; pageSize?: number }) =>
  api.get('/users', { params });

export const updateUserRole = (id: string, role: string) =>
  api.put(`/users/${id}/role`, { role });

// 统计数据
export const getDashboardStats = () =>
  api.get('/dashboard/stats');

export const getUsageData = (days: number = 30) =>
  api.get('/dashboard/usage', { params: { days } });

export default api;
