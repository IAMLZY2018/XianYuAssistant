import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 自动发货记录 (简化版)
export interface AutoDeliveryRecord {
  id: number;
  xyGoodsId: string;
  goodsTitle?: string;
  buyerUserName?: string;
  content?: string;
  state: number; // 1-成功，0-失败
  orderId?: string;
  createTime: string;
}

// 查询记录请求
export interface AutoDeliveryRecordReq {
  xianyuAccountId: number;
  xyGoodsId?: string;
  pageNum?: number;
  pageSize?: number;
}

// 查询记录响应
export interface AutoDeliveryRecordResp {
  records: AutoDeliveryRecord[];
  total: number;
  pageNum: number;
  pageSize: number;
}

// 获取自动发货记录
export function getAutoDeliveryRecords(data: AutoDeliveryRecordReq) {
  return request<AutoDeliveryRecordResp>({
    url: '/items/autoDeliveryRecords',
    method: 'POST',
    data
  });
}

// 确认收货请求
export interface ConfirmShipmentReq {
  xianyuAccountId: number;
  orderId: string;
}

// 确认收货
export function confirmShipment(data: ConfirmShipmentReq) {
  return request<string>({
    url: '/order/confirmShipment',
    method: 'POST',
    data
  });
}
