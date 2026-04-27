import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 消息信息
export interface ChatMessage {
  id: number;
  xianyuAccountId: number;
  lwp: string;
  pnmId: string;
  sid: string;  // 注意：后端返回的是全小写的 sid
  contentType: number;
  msgContent: string;
  senderUserName: string;
  senderUserId: string;
  senderAppV: string;
  senderOsType: string;
  reminderUrl: string;
  xyGoodsId: string;
  completeMsg: string;
  messageTime: number;
  createTime: string;
}

// 消息列表响应
export interface MessageListResponse {
  list: ChatMessage[];
  totalCount: number;
  totalPage: number;
  pageNum: number;
  pageSize: number;
}

// 获取消息列表
export function getMessageList(data: {
  xianyuAccountId: number;
  xyGoodsId?: string;
  pageNum?: number;
  pageSize?: number;
  filterCurrentAccount?: boolean; // 过滤当前账号消息
}) {
  return request<MessageListResponse>({
    url: '/msg/list',
    method: 'POST',
    data
  });
}

// 根据会话ID获取上下文消息
export function getContextMessages(data: {
  sid: string;
  limit?: number;
  offset?: number;
}) {
  return request<ChatMessage[]>({
    url: '/msg/context',
    method: 'POST',
    data: { sid: data.sid, limit: data.limit || 20, offset: data.offset || 0 }
  });
}

// 发送消息
export function sendMessage(data: {
  xianyuAccountId: number;
  cid: string;
  toId: string;
  text: string;
  xyGoodsId?: string;
}) {
  return request<string>({
    url: '/sendMessage',
    method: 'POST',
    data
  });
}
