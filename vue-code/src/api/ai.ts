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
  goodsID: string
  content: string
  createTime: string
}

// AI 对话 (SSE 流式)
// 后端 AIChatController 的 @RequestMapping 是 "/ai"（无 /api 前缀），
// 与其他控制器 @RequestMapping("/api/xxx") 不同，
// 所以不能用 request()（baseURL=/api），需用 fetch 直接请求
export function chatWithAI(data: ChatWithAIReq): Promise<Response> {
  return fetch('/ai/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
}

// 上传资料到 RAG 知识库
export function putNewDataToRAG(data: PutNewDataReq): Promise<Response> {
  return fetch('/ai/putNewData', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
}

// 查询 RAG 知识库资料
export function queryRAGData(data: { goodsId: string }): Promise<Response> {
  return fetch('/ai/queryRAGData', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify(data)
  })
}
