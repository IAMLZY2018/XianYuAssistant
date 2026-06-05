import { request } from '@/utils/request'

/** 获取当前用户信息 */
export function getCurrentUser() {
  return request<{ username: string; lastLoginTime: string }>({
    url: '/system/currentUser',
    method: 'post'
  })
}

/** 修改密码 */
export function changePassword(data: { oldPassword: string; newPassword: string; confirmPassword: string }) {
  return request<null>({
    url: '/system/changePassword',
    method: 'post',
    data
  })
}

/** 获取当前版本号 */
export function getVersion() {
  return request<string>({
    url: '/system/version',
    method: 'get'
  })
}

/** 检查更新 */
export function checkUpdate() {
  return request<{
    currentVersion: string
    latestVersion: string
    hasUpdate: boolean
    updateContent: string
    publishedAt: string
    downloadUrl: string
  }>({
    url: '/system/checkUpdate',
    method: 'get'
  })
}
