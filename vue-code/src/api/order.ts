import request from '@/utils/request'
import type { ApiResponse } from '@/types'

export interface OrderQueryReq {
  xianyuAccountId?: number
  xyGoodsId?: string
  orderStatus?: number
  pageNum: number
  pageSize: number
}

export interface OrderVO {
  id: number
  accountRemark: string
  orderId: string
  goodsTitle: string
  sid: string  // 改为小写，匹配后端返回
  createTime: number
  autoDeliverySuccess: boolean
  orderStatus: number
  orderStatusText: string
  buyerUserName: string
  xyGoodsId: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export function queryOrderList(data: OrderQueryReq) {
  return request<PageResult<OrderVO>>({
    url: '/order/list',
    method: 'POST',
    data
  }) as Promise<ApiResponse<PageResult<OrderVO>>>
}

export function confirmShipment(data: { xianyuAccountId: number; orderId: string }) {
  return request<string>({
    url: '/order/confirmShipment',
    method: 'POST',
    data
  })
}
