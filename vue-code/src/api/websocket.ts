import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// WebSocket连接状态
export interface WebSocketStatus {
  xianyuAccountId: number;
  connected: boolean;
  status: string;
  cookieStatus?: number;      // Cookie状态 1:有效 2:过期 3:失效
  cookieText?: string;        // Cookie值
  websocketToken?: string;    // WebSocket Token
  tokenExpireTime?: number;   // Token过期时间戳（毫秒）
}

// 获取连接状态
export function getConnectionStatus(accountId: number) {
  return request<WebSocketStatus>({
    url: '/websocket/status',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 启动连接
export function startConnection(accountId: number) {
  return request({
    url: '/websocket/start',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 停止连接
export function stopConnection(accountId: number) {
  return request({
    url: '/websocket/stop',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 更新Cookie
export function updateCookie(data: { xianyuAccountId: number; cookieText: string }) {
  return request({
    url: '/websocket/updateCookie',
    method: 'POST',
    data
  });
}

// 发送消息请求参数
export interface SendMessageRequest {
  xianyuAccountId: number;
  cid: string;  // 会话ID (即 sid)
  toId: string;  // 接收方ID (即 senderUserId)
  text: string;  // 消息内容（注意：后端接收的参数名是 text，不是 content）
  xyGoodsId?: string;  // 闲鱼商品ID
}

// 发送消息
export function sendMessage(data: SendMessageRequest) {
  return request<ApiResponse<any>>({
    url: '/websocket/sendMessage',
    method: 'POST',
    data
  });
}

// 清除验证等待状态
export function clearCaptchaWait(accountId: number) {
  return request({
    url: '/websocket/clearCaptchaWait',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 刷新Token响应
export interface RefreshTokenResponse {
  mh5tkRefreshed: boolean;    // _m_h5_tk是否刷新成功
  wsTokenRefreshed: boolean;  // websocket_token是否刷新成功
  message: string;            // 提示信息
}

// 手动刷新Token
export function refreshToken(accountId: number) {
  return request<RefreshTokenResponse>({
    url: '/websocket/refreshToken',
    method: 'POST',
    data: { xianyuAccountId: accountId }
  });
}

// 手动更新Token
export function updateToken(data: { xianyuAccountId: number; websocketToken: string }) {
  return request<string>({
    url: '/websocket/updateToken',
    method: 'POST',
    data
  });
}
