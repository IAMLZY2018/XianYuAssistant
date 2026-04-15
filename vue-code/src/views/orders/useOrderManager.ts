import { ref, reactive, computed } from 'vue'
import { queryOrderList, confirmShipment } from '@/api/order'
import type { OrderVO, OrderQueryReq } from '@/api/order'
import { showSuccess, showError, showConfirm } from '@/utils'
import { formatTime } from '@/utils'
import { OrderStatus } from '@/constants/orderStatus'

// 扩展 OrderVO 添加运行时状态
export interface OrderItem extends OrderVO {
  confirming?: boolean
}

export function useOrderManager() {
  const loading = ref(false)
  const orderList = ref<OrderItem[]>([])
  const total = ref(0)

  const queryParams = reactive<OrderQueryReq>({
    pageNum: 1,
    pageSize: 20
  })

  const dialogs = reactive({
    confirmShipment: false,
    filter: false
  })

  const confirmTarget = ref<OrderItem | null>(null)

  // 计算属性
  const totalPages = computed(() => Math.ceil(total.value / queryParams.pageSize))

  // 状态相关
  const getStatusColor = (status: number | null) => {
    switch (status) {
      case OrderStatus.WAITING_PAYMENT: return '#ff9500'
      case OrderStatus.WAITING_DELIVERY: return '#007aff'
      case OrderStatus.DELIVERED: return '#34c759'
      case OrderStatus.COMPLETED: return '#34c759'
      case OrderStatus.CLOSED: return '#86868b'
      default: return '#86868b'
    }
  }

  const getStatusBg = (status: number | null) => {
    switch (status) {
      case OrderStatus.WAITING_PAYMENT: return 'rgba(255, 149, 0, 0.1)'
      case OrderStatus.WAITING_DELIVERY: return 'rgba(0, 122, 255, 0.1)'
      case OrderStatus.DELIVERED: return 'rgba(52, 199, 89, 0.1)'
      case OrderStatus.COMPLETED: return 'rgba(52, 199, 89, 0.1)'
      case OrderStatus.CLOSED: return 'rgba(134, 134, 139, 0.1)'
      default: return 'rgba(134, 134, 139, 0.1)'
    }
  }

  const getStatusText = (status: number | null) => {
    switch (status) {
      case OrderStatus.WAITING_PAYMENT: return '待付款'
      case OrderStatus.WAITING_DELIVERY: return '待发货'
      case OrderStatus.DELIVERED: return '已发货'
      case OrderStatus.COMPLETED: return '已完成'
      case OrderStatus.CLOSED: return '已关闭'
      default: return '未知'
    }
  }

  // 查询订单列表
  const loadOrders = async () => {
    loading.value = true
    try {
      const response = await queryOrderList(queryParams)
      orderList.value = (response.data?.records || []).map(item => ({
        ...item,
        confirming: false
      }))
      total.value = response.data?.total || 0
    } catch (error: any) {
      console.error('查询订单列表失败:', error)
      showError('查询订单列表失败: ' + (error.message || '未知错误'))
      orderList.value = []
    } finally {
      loading.value = false
    }
  }

  // 重置查询
  const handleReset = () => {
    queryParams.pageNum = 1
    queryParams.xyGoodsId = undefined
    queryParams.orderStatus = undefined
    queryParams.xianyuAccountId = undefined
    loadOrders()
  }

  // 分页
  const handlePageChange = (page: number) => {
    queryParams.pageNum = page
    loadOrders()
  }

  const handleSizeChange = (size: number) => {
    queryParams.pageSize = size
    queryParams.pageNum = 1
    loadOrders()
  }

  // 复制会话ID
  const copySId = (sid: string) => {
    navigator.clipboard.writeText(sid).then(() => {
      showSuccess('会话ID已复制')
    }).catch(() => {
      showError('复制失败')
    })
  }

  // 确认发货
  const handleConfirmShipment = async (row: OrderItem) => {
    try {
      await showConfirm(
        `确认订单 ${row.orderId} 已发货吗？`,
        '确认发货'
      )

      row.confirming = true
      await confirmShipment({
        xianyuAccountId: (row as any).xianyuAccountId,
        orderId: row.orderId
      })

      showSuccess('确认发货成功')
      row.orderStatus = OrderStatus.DELIVERED
      row.orderStatusText = '已发货'
      loadOrders()
    } catch (error: any) {
      if (error === 'cancel') return
      showError('确认发货失败: ' + (error.message || '未知错误'))
    } finally {
      row.confirming = false
    }
  }

  return {
    loading,
    orderList,
    total,
    queryParams,
    dialogs,
    confirmTarget,
    totalPages,
    loadOrders,
    handleReset,
    handlePageChange,
    handleSizeChange,
    copySId,
    handleConfirmShipment,
    getStatusColor,
    getStatusBg,
    getStatusText,
    formatTime,
    OrderStatus
  }
}
