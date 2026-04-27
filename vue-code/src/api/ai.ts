import { getAuthToken } from '@/utils/request'

// AI 对话请求
export interface ChatWithAIReq {
  msg: string
  goodsId: string
}

// 上传资料到 RAG 请求
export interface PutNewDataReq {
  content: string
  goodsId: string
}

// 查询 RAG 资料响应
export interface RAGDataItem {
  documentId: string
  goodsID: string
  content: string
  createTime: string
}

/** 构建带Token的headers */
function authHeaders(): Record<string, string> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json'
  }
  const token = getAuthToken()
  if (token) {
    headers['Authorization'] = `Bearer ${token}`
  }
  return headers
}

// AI 对话 (SSE 流式)
// 后端 AIChatController 的 @RequestMapping 是 "/ai"（无 /api 前缀），
// 与其他控制器 @RequestMapping("/api/xxx") 不同，
// 所以不能用 request()（baseURL=/api），需用 fetch 直接请求
export function chatWithAI(data: ChatWithAIReq): Promise<Response> {
  return fetch('/ai/chat', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data)
  })
}

// 上传资料到 RAG 知识库
export function putNewDataToRAG(data: PutNewDataReq): Promise<Response> {
  return fetch('/ai/putNewData', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data)
  })
}

// 查询 RAG 知识库资料
export function queryRAGData(data: { goodsId: string }): Promise<Response> {
  return fetch('/ai/queryRAGData', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data)
  })
}

// 删除 RAG 知识库资料
export function deleteRAGData(data: { documentId: string }): Promise<Response> {
  return fetch('/ai/deleteRAGData', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify(data)
  })
}

// AI 状态信息
export interface AIStatus {
  enabled: boolean
  available: boolean
  apiKeyConfigured: boolean
  message: string
  baseUrl: string
  model: string
}

// 获取 AI 状态
export function getAIStatus(): Promise<Response> {
  return fetch('/ai/status', {
    method: 'POST',
    headers: authHeaders(),
    body: JSON.stringify({})
  })
}
