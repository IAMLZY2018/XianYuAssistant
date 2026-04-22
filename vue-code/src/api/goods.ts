import { request } from '@/utils/request';
import type { ApiResponse } from '@/types';

// 商品信息
export interface GoodsItem {
  id: number;
  xyGoodId: string;
  xianyuAccountId: number;
  title: string;
  coverPic: string;
  infoPic: string;
  detailInfo: string;
  detailUrl: string;
  soldPrice: string;
  status: number;
  createdTime: string;
  updatedTime: string;
}

// 带配置的商品信息
export interface GoodsItemWithConfig {
  item: GoodsItem;
  xianyuAutoDeliveryOn: number;
  xianyuAutoReplyOn: number;
}

// 商品列表响应
export interface GoodsListResponse {
  itemsWithConfig: GoodsItemWithConfig[];
  totalCount: number;
  totalPage: number;
  pageNum: number;
  pageSize: number;
}

// 商品详情响应
export interface GoodsDetailResponse {
  itemWithConfig: GoodsItemWithConfig;
}

// 刷新商品响应
export interface RefreshItemsResponse {
  success: boolean;
  totalCount: number;
  successCount: number;
  updatedItemIds: string[];
  message: string;
}

// 获取商品列表
export function getGoodsList(data: {
  xianyuAccountId: number;
  status?: number;
  pageNum?: number;
  pageSize?: number;
}) {
  return request<GoodsListResponse>({
    url: '/items/list',
    method: 'POST',
    data
  });
}

// 刷新商品数据
export function refreshGoods(xianyuAccountId: number) {
  return request<RefreshItemsResponse>({
    url: '/items/refresh',
    method: 'POST',
    data: { xianyuAccountId }
  });
}

// 获取商品详情
export function getGoodsDetail(xyGoodId: string) {
  return request<GoodsDetailResponse>({
    url: '/items/detail',
    method: 'POST',
    data: { xyGoodId }
  });
}

// 更新自动发货状态
export function updateAutoDeliveryStatus(data: {
  xianyuAccountId: number;
  xyGoodsId: string;
  xianyuAutoDeliveryOn: number;
}) {
  return request({
    url: '/items/updateAutoDeliveryStatus',
    method: 'POST',
    data
  });
}

// 更新自动回复状态
export function updateAutoReplyStatus(data: {
  xianyuAccountId: number;
  xyGoodsId: string;
  xianyuAutoReplyOn: number;
}) {
  return request({
    url: '/items/updateAutoReplyStatus',
    method: 'POST',
    data
  });
}

// 删除商品
export function deleteItem(data: {
  xianyuAccountId: number;
  xyGoodsId: string;
}) {
  return request({
    url: '/items/delete',
    method: 'POST',
    data
  });
}

// RAG自动回复配置响应
export interface RagAutoReplyConfigResponse {
  ragDelaySeconds: number;
}

// 获取RAG自动回复配置
export function getRagAutoReplyConfig(data: {
  xianyuAccountId: number;
  xyGoodsId: string;
}) {
  return request<RagAutoReplyConfigResponse>({
    url: '/items/getRagAutoReplyConfig',
    method: 'POST',
    data
  });
}

// 更新RAG自动回复配置
export function updateRagAutoReplyConfig(data: {
  xianyuAccountId: number;
  xyGoodsId: string;
  ragDelaySeconds: number;
}) {
  return request({
    url: '/items/updateRagAutoReplyConfig',
    method: 'POST',
    data
  });
}
