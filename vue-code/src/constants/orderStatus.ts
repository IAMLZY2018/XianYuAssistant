/**
 * 订单状态枚举
 */
export enum OrderStatus {
  /** 等待买家付款 */
  WAITING_PAYMENT = 1,
  /** 等待卖家发货 */
  WAITING_DELIVERY = 2,
  /** 已发货 */
  DELIVERED = 3,
  /** 交易成功 */
  COMPLETED = 4,
  /** 交易关闭 */
  CLOSED = 5
}

/**
 * 订单状态描述
 */
export const OrderStatusText: Record<OrderStatus, string> = {
  [OrderStatus.WAITING_PAYMENT]: '等待买家付款',
  [OrderStatus.WAITING_DELIVERY]: '等待卖家发货',
  [OrderStatus.DELIVERED]: '已发货',
  [OrderStatus.COMPLETED]: '交易成功',
  [OrderStatus.CLOSED]: '交易关闭'
}

/**
 * 获取订单状态描述
 */
export function getOrderStatusText(status: OrderStatus | null): string {
  if (status === null || status === undefined) {
    return '未知'
  }
  return OrderStatusText[status] || '未知'
}
