import { request } from '@/utils/request'

export interface SysSetting {
  settingKey: string
  settingValue: string
  settingDesc: string
}

/** 获取配置 */
export function getSetting(data: { settingKey: string }) {
  return request<SysSetting>({
    url: '/setting/get',
    method: 'post',
    data
  })
}

/** 获取所有配置 */
export function getAllSettings() {
  return request<SysSetting[]>({
    url: '/setting/list',
    method: 'post',
    data: {}
  })
}

/** 保存配置 */
export function saveSetting(data: { settingKey: string; settingValue: string; settingDesc?: string }) {
  return request<null>({
    url: '/setting/save',
    method: 'post',
    data
  })
}

/** 删除配置 */
export function deleteSetting(data: { settingKey: string }) {
  return request<null>({
    url: '/setting/delete',
    method: 'post',
    data
  })
}

/** 测试邮箱配置 */
export function testEmail() {
  return request<string>({
    url: '/setting/testEmail',
    method: 'post',
    data: {}
  })
}
