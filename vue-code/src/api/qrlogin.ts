import { request } from '@/utils/request'
import type { ApiResponse, QRLoginSession } from '@/types'

// 生成二维码
export function generateQRCode() {
  return request<QRLoginSession>({
    url: '/qrlogin/generate',
    method: 'POST'
  })
}

// 查询二维码状态
export function getQRCodeStatus(sessionId: string) {
  return request<QRLoginSession>({
    url: `/qrlogin/status/${sessionId}`,
    method: 'POST'
  })
}

// 获取 Cookie
export function getQRCodeCookies(sessionId: string) {
  return request<{ cookies: string; unb: string }>({
    url: `/qrlogin/cookies/${sessionId}`,
    method: 'POST'
  })
}

// 清理过期会话
export function cleanupQRSessions() {
  return request({
    url: '/qrlogin/cleanup',
    method: 'POST'
  })
}
