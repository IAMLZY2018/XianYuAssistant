import { request } from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface DeliveryRecordQueryReq {
  xianyuAccountId?: number
  xyGoodsId?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}

export interface DeliveryRecordVO {
  id: number
  xianyuAccountId?: number
  xyGoodsId: string
  goodsTitle?: string
  buyerUserName?: string
  sid?: string
  content?: string
  state: number
  failReason?: string
  confirmState: number
  orderId?: string
  skuName?: string
  orderCreateTime?: string
  paySuccessTime?: string
  consignTime?: string
  totalPrice?: string
  buyNum?: number
  createTime: string
}

export interface DeliveryRecordPageResult {
  records: DeliveryRecordVO[]
  total: number
  pageNum: number
  pageSize: number
}

export function queryDeliveryRecordList(data: DeliveryRecordQueryReq) {
  return request<DeliveryRecordPageResult>({
    url: '/items/autoDeliveryRecords',
    method: 'POST',
    data
  })
}

export function confirmShipment(data: { xianyuAccountId: number; orderId: string }) {
  return request<string>({
    url: '/order/confirmShipment',
    method: 'POST',
    data
  })
}

export function getOrderDetail(data: { xianyuAccountId: number; orderId: string; fromServer?: boolean }) {
  return request<string>({
    url: '/order/detail',
    method: 'POST',
    data
  })
}

export function manualDelivery(data: { xianyuAccountId: number; orderId: string; content: string }) {
  return request<string>({
    url: '/autoDelivery/manual',
    method: 'POST',
    data
  })
}

export function getPendingOrders(xianyuAccountId: number) {
  return request<any[]>({
    url: '/order/pendingOrders',
    method: 'POST',
    data: { xianyuAccountId }
  })
}

export function deliverPendingOrders(xianyuAccountId: number) {
  return request<number>({
    url: '/order/deliverPendingOrders',
    method: 'POST',
    data: { xianyuAccountId }
  })
}

export function consignDummyDelivery(data: { xianyuAccountId: number; xyGoodsId: string; orderId: string }) {
  return request<string>({
    url: '/order/consignDummyDelivery',
    method: 'POST',
    data
  })
}
