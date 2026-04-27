// 通用响应类型
export interface ApiResponse<T = any> {
  code: number
  msg: string
  message?: string  // 兼容字段
  data?: T
}

// 账号类型
export interface Account {
  id: number
  accountNote: string
  unb: string
  status: number
  createdTime: string
  updatedTime: string
}

// 商品类型
export interface Goods {
  id: number
  xyGoodId: string
  xianyuAccountId: number
  title: string
  coverPic: string
  infoPic: string
  detailInfo: string
  detailUrl: string
  soldPrice: string
  status: number
  createdTime: string
  updatedTime: string
  // 配置信息
  xianyuAutoDeliveryOn?: number
  xianyuAutoReplyOn?: number
}

// 商品列表请求
export interface GoodsListRequest {
  xianyuAccountId?: number
  page?: number
  pageSize?: number
}

// WebSocket 状态
export interface WebSocketStatus {
  connected: boolean
  accountId?: number
  message?: string
}

// 消息类型
export interface Message {
  id: number
  xianyuAccountId: number
  lwp: string
  pnmId: string
  sId: string
  contentType: number
  msgContent: string
  senderUserName: string
  senderUserId: string
  senderAppV: string
  senderOsType: string
  reminderUrl: string
  xyGoodsId: string
  completeMsg: string
  messageTime: number
  createTime: string
}

// 二维码登录
export interface QRLoginSession {
  sessionId: string
  qrCodeUrl: string
  status: 'pending' | 'scanned' | 'confirmed' | 'expired'
}

// 自动发货配置
export interface AutoDeliveryConfig {
  id?: number
  xianyuAccountId: number
  xyGoodsId: string
  type: number
  autoDeliveryContent: string
  autoConfirmShipment?: number
  cardSecretCount?: number
}

// 自动回复配置
export interface AutoReplyConfig {
  id?: number
  xianyuAccountId: number
  xyGoodsId: string
  keyword: string
  replyContent: string
  matchType: number
}

// 操作记录
export interface OperationRecord {
  id: number
  xianyuAccountId: number
  xyGoodsId: string
  buyerMessage?: string
  replyContent?: string
  deliveryContent?: string
  state: number
  createTime: string
}
