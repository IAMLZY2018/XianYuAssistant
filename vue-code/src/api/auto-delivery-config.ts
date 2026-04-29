import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 自动发货配置
export interface AutoDeliveryConfig {
  id: number;
  xianyuAccountId: number;
  xianyuGoodsId: number;
  xyGoodsId: string;
  deliveryMode: number; // 1-自动发货，2-卡密发货，3-自定义发货
  autoDeliveryContent: string;
  kamiConfigIds?: string; // 卡密发货绑定的配置ID列表（逗号分隔）
  kamiDeliveryTemplate?: string; // 卡密发货文案模板，使用{kmKey}占位符
  autoDeliveryImageUrl?: string;
  autoConfirmShipment?: number;
  createTime: string;
  updateTime: string;
}

export interface SaveAutoDeliveryConfigReq {
  xianyuAccountId: number;
  xianyuGoodsId?: number;
  xyGoodsId: string;
  deliveryMode: number;
  autoDeliveryContent: string;
  kamiConfigIds?: string;
  kamiDeliveryTemplate?: string;
  autoDeliveryImageUrl?: string;
  autoConfirmShipment?: number;
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