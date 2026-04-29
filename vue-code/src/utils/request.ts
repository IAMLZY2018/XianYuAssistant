import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import type { ApiResponse } from '@/types'

// Token存储key
const TOKEN_KEY = 'xianyu_auth_token'
const USERNAME_KEY = 'xianyu_auth_username'

/** 获取Token */
export function getAuthToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

/** 设置Token */
export function setAuthToken(token: string, username: string) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USERNAME_KEY, username)
}

/** 清除Token */
export function clearAuthToken() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USERNAME_KEY)
}

/** 获取用户名 */
export function getAuthUsername(): string | null {
  return localStorage.getItem(USERNAME_KEY)
}

/** 是否已登录 */
export function isLoggedIn(): boolean {
  return !!getAuthToken()
}

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 添加Token到请求头
    const token = getAuthToken()
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    console.log('发送请求:', config.url, config.data)
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse<any>>) => {
    console.log('收到响应:', response.config.url, response.data)
    const res = response.data

    // 401未登录 -> 跳转登录页
    if (res.code === 401) {
      clearAuthToken()
      // 避免在登录页重复跳转
      if (!window.location.pathname.includes('/login')) {
        ElMessage.error(res.msg || '登录已过期，请重新登录')
        window.location.href = '/login'
      }
      return Promise.reject(new Error(res.msg || '未登录'))
    }

    // 特殊处理：1001是滑块验证码，需要业务代码自己处理，不在这里拦截
    if (res.code === 1001) {
      return response // 直接返回，让业务代码处理
    }

    // 如果响应码不是 0 或 200，认为是错误
    if (res.code !== 0 && res.code !== 200) {
      const errorMsg = res.msg || res.message || '请求失败'
      ElMessage.error(errorMsg)
      const error = new Error(errorMsg)
      // 标记这个错误已经显示过消息，避免重复提示
      ;(error as any).messageShown = true
      return Promise.reject(error)
    }

    return response // 保持返回完整的 AxiosResponse
  },
  (error) => {
    console.error('响应错误:', error)
    // 只有在错误消息未显示过时才弹出提示
    if (!(error as any).messageShown) {
      ElMessage.error(error.message || '网络请求失败')
    }
    return Promise.reject(error)
  }
)

// 封装请求方法
export function request<T = any>(config: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.request<ApiResponse<T>>(config).then(response => response.data)
}

export default service
