import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 上传图片到闲鱼CDN
export function uploadImage(accountId: number, file: File): Promise<ApiResponse<string>> {
  const formData = new FormData();
  formData.append('accountId', String(accountId));
  formData.append('file', file);
  
  return request({
    url: '/image/upload',
    method: 'POST',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  });
}

// 通过URL上传图片
export function uploadImageFromUrl(accountId: number, imageUrl: string) {
  return request<string>({
    url: '/image/uploadFromUrl',
    method: 'POST',
    data: { accountId, imageUrl }
  });
}

// 发送图片消息
export function sendImageMessage(data: {
  xianyuAccountId: number;
  cid: string;
  toId: string;
  imageUrl: string;
  width?: number;
  height?: number;
  xyGoodsId?: string;
}) {
  return request<string>({
    url: '/websocket/sendImageMessage',
    method: 'POST',
    data
  });
}
