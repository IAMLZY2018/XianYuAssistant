import { toast } from './toast'
import { showConfirm } from './confirm'

export const showSuccess = toast.success
export const showError = toast.error
export const showWarning = toast.warning
export const showInfo = toast.info
export { showConfirm }

export function formatTime(timestamp: number | string | Date): string {
  if (!timestamp) return '-'
  if (typeof timestamp === 'string') {
    if (/^\d{4}-\d{2}-\d{2}/.test(timestamp)) {
      return timestamp.replace('T', ' ').substring(0, 19)
    }
    const num = Number(timestamp)
    if (!isNaN(num)) {
      const date = new Date(num)
      if (!isNaN(date.getTime())) {
        return date.toLocaleString('zh-CN', {
          year: 'numeric', month: '2-digit', day: '2-digit',
          hour: '2-digit', minute: '2-digit', second: '2-digit'
        })
      }
    }
    return '-'
  }
  const date = new Date(timestamp)
  if (isNaN(date.getTime())) return '-'
  return date.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  })
}

export function formatPrice(price: string | number): string {
  if (!price) return '¥0.00'
  const num = typeof price === 'string' ? parseFloat(price) : price
  return `¥${num.toFixed(2)}`
}

export function getGoodsStatusText(status: number): { text: string; type: string } {
  const statusMap: Record<number, { text: string; type: string }> = {
    0: { text: '在售', type: 'success' },
    1: { text: '已下架', type: 'info' },
    2: { text: '已售出', type: 'warning' }
  }
  return statusMap[status] || { text: '未知', type: 'info' }
}

export function getAccountStatusText(status: number): { text: string; type: string } {
  const statusMap: Record<number, { text: string; type: string }> = {
    1: { text: '正常', type: 'success' },
    '-1': { text: '需要验证', type: 'warning' }
  }
  return statusMap[status] || { text: '未知', type: 'info' }
}

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
