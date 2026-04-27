import { request } from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface DeliveryRecordQueryReq {
  xianyuAccountId?: number
  xyGoodsId?: string
  pageNum?: number
  pageSize?: number
}

export interface DeliveryRecordVO {
  id: number
  xyGoodsId: string
  goodsTitle?: string
  buyerUserName?: string
  content?: string
  state: number
  confirmState: number
  orderId?: string
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
