import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

export interface AutoDeliveryConfig {
  id: number;
  xianyuAccountId: number;
  xianyuGoodsId: number;
  xyGoodsId: string;
  deliveryMode: number;
  skuId: string | null;
  skuName?: string;
  autoDeliveryContent: string;
  kamiConfigIds?: string;
  kamiDeliveryTemplate?: string;
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
  skuId?: string | null;
  skuName?: string;
  autoDeliveryContent: string;
  kamiConfigIds?: string;
  kamiDeliveryTemplate?: string;
  autoDeliveryImageUrl?: string;
  autoConfirmShipment?: number;
}

export interface GetAutoDeliveryConfigReq {
  xianyuAccountId: number;
  xyGoodsId?: string;
  skuId?: string | null;
}

export function saveOrUpdateAutoDeliveryConfig(data: SaveAutoDeliveryConfigReq) {
  return request<AutoDeliveryConfig>({
    url: '/auto-delivery-config/save',
    method: 'POST',
    data
  });
}

export function getAutoDeliveryConfig(data: GetAutoDeliveryConfigReq) {
  return request<AutoDeliveryConfig>({
    url: '/auto-delivery-config/get',
    method: 'POST',
    data
  });
}

export function getAutoDeliveryConfigsByGoodsId(data: { xianyuAccountId: number; xyGoodsId: string }) {
  return request<AutoDeliveryConfig[]>({
    url: '/auto-delivery-config/listByGoods',
    method: 'POST',
    data
  });
}

export function getAutoDeliveryConfigsByAccountId(xianyuAccountId: number) {
  return request<AutoDeliveryConfig[]>({
    url: '/auto-delivery-config/list',
    method: 'POST',
    params: { xianyuAccountId }
  });
}

export function deleteAutoDeliveryConfig(xianyuAccountId: number, xyGoodsId: string) {
  return request({
    url: '/auto-delivery-config/delete',
    method: 'POST',
    params: { xianyuAccountId, xyGoodsId }
  });
}

export interface GoodsSku {
  id: string;
  xyGoodsId: string;
  skuId: string | null;
  price: number;
  quantity: number;
  propertyText: string;
  propertyId: number;
  valueId: number;
  valueText: string;
  propertySortOrder: number;
  valueSortOrder: number;
  features: string;
  xianyuAccountId: number;
}

export interface GoodsSkuProperty {
  id: string;
  xyGoodsId: string;
  propertyId: number;
  propertyText: string;
  propertySortOrder: number;
  valueId: number;
  valueText: string;
  valueSortOrder: number;
  xianyuAccountId: number;
}

export interface GoodsSkuDetail {
  skuList: GoodsSku[];
  propertyList: GoodsSkuProperty[];
}

export function getGoodsSkuList(xyGoodsId: string) {
  return request<GoodsSku[]>({
    url: '/goods-sku/list',
    method: 'POST',
    params: { xyGoodsId }
  });
}

export function getGoodsSkuDetail(xyGoodsId: string) {
  return request<GoodsSkuDetail>({
    url: '/goods-sku/detail',
    method: 'POST',
    params: { xyGoodsId }
  });
}
