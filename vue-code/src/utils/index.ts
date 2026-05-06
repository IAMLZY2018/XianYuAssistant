import { ElMessage, ElMessageBox } from 'element-plus'

// 显示成功消息
export function showSuccess(message: string) {
  ElMessage.success(message)
}

// 显示错误消息
export function showError(message: string) {
  ElMessage.error(message)
}

// 显示警告消息
export function showWarning(message: string) {
  ElMessage.warning(message)
}

// 显示信息消息
export function showInfo(message: string) {
  ElMessage.info(message)
}

// 显示确认对话框
export function showConfirm(message: string, title = '确认') {
  return ElMessageBox.confirm(message, title, {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  })
}

// 格式化时间
export function formatTime(timestamp: number | string | Date): string {
  if (!timestamp) return '-'
  const ts = typeof timestamp === 'string' || typeof timestamp === 'number' ? Number(timestamp) : timestamp
  if (typeof ts === 'number' && isNaN(ts)) return '-'
  const date = new Date(ts)
  if (isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

// 格式化价格
export function formatPrice(price: string | number): string {
  if (!price) return '¥0.00'
  const num = typeof price === 'string' ? parseFloat(price) : price
  return `¥${num.toFixed(2)}`
}

// 获取商品状态文本
export function getGoodsStatusText(status: number): { text: string; type: string } {
  const statusMap: Record<number, { text: string; type: string }> = {
    0: { text: '在售', type: 'success' },
    1: { text: '已下架', type: 'info' },
    2: { text: '已售出', type: 'warning' }
  }
  return statusMap[status] || { text: '未知', type: 'info' }
}

// 获取账号状态文本
export function getAccountStatusText(status: number): { text: string; type: string } {
  const statusMap: Record<number, { text: string; type: string }> = {
    1: { text: '正常', type: 'success' },
    '-1': { text: '需要验证', type: 'warning' }
  }
  return statusMap[status] || { text: '未知', type: 'info' }
}

// 防抖函数
export function debounce<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null
  return function (this: any, ...args: Parameters<T>) {
    if (timeout) clearTimeout(timeout)
    timeout = setTimeout(() => {
      func.apply(this, args)
    }, wait)
  }
}

// 节流函数
export function throttle<T extends (...args: any[]) => any>(
  func: T,
  wait: number
): (...args: Parameters<T>) => void {
  let timeout: ReturnType<typeof setTimeout> | null = null
  return function (this: any, ...args: Parameters<T>) {
    if (!timeout) {
      timeout = setTimeout(() => {
        timeout = null
        func.apply(this, args)
      }, wait)
    }
  }
}
