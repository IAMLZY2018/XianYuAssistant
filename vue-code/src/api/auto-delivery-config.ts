import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 自动发货配置
export interface AutoDeliveryConfig {
  id: number;
  xianyuAccountId: number;
  xianyuGoodsId: number;
  xyGoodsId: string;
  type: number; // 1-文本，2-自定义
  autoDeliveryContent: string;
  autoConfirmShipment?: number; // 自动确认发货开关：0-关闭，1-开启
  cardSecretCount?: number; // 卡密库存
  createTime: string;
  updateTime: string;
}

// 导入卡密请求
export interface ImportCardSecretReq {
  xianyuAccountId: number;
  xyGoodsId: string;
  content: string;
}

// 保存配置请求
export interface SaveAutoDeliveryConfigReq {
  xianyuAccountId: number;
  xianyuGoodsId?: number;
  xyGoodsId: string;
  type: number;
  autoDeliveryContent: string;
  autoConfirmShipment?: number; // 自动确认发货开关：0-关闭，1-开启
}

// 查询配置请求
export interface GetAutoDeliveryConfigReq {
  xianyuAccountId: number;
  xyGoodsId?: string;
}

// 保存或更新自动发货配置
export function saveOrUpdateAutoDeliveryConfig(data: SaveAutoDeliveryConfigReq) {
  return request<AutoDeliveryConfig>({
    url: '/auto-delivery-config/save',
    method: 'POST',
    data
  });
}

// 查询自动发货配置
export function getAutoDeliveryConfig(data: GetAutoDeliveryConfigReq) {
  return request<AutoDeliveryConfig>({
    url: '/auto-delivery-config/get',
    method: 'POST',
    data
  });
}

// 根据账号ID查询所有配置
export function getAutoDeliveryConfigsByAccountId(xianyuAccountId: number) {
  return request<AutoDeliveryConfig[]>({
    url: '/auto-delivery-config/list',
    method: 'POST',
    params: { xianyuAccountId }
  });
}

// 删除自动发货配置
export function deleteAutoDeliveryConfig(xianyuAccountId: number, xyGoodsId: string) {
  return request({
    url: '/auto-delivery-config/delete',
    method: 'POST',
    params: { xianyuAccountId, xyGoodsId }
  });
}

// 导入卡密
export function importCardSecret(data: ImportCardSecretReq) {
  return request<number>({
    url: '/auto-delivery-config/import-card-secret',
    method: 'POST',
    data
  });
}
