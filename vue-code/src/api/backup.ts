import { request } from '@/utils/request'

export interface BackupModule {
  moduleKey: string
  moduleName: string
}

export interface BackupExportResult {
  jsonData: string
}

export interface BackupImportResult {
  totalCount: number
  successCount: number
  failedModules: string[]
}

export function getBackupModules() {
  return request<BackupModule[]>({
    url: '/backup/modules',
    method: 'post',
    data: {}
  })
}

export function exportBackup(data: { modules: string[] }) {
  return request<BackupExportResult>({
    url: '/backup/export',
    method: 'post',
    data
  })
}

export function importBackup(data: { jsonData: string; modules: string[] }) {
  return request<BackupImportResult>({
    url: '/backup/import',
    method: 'post',
    data
  })
}

export function getLogDates() {
  return request<string[]>({
    url: '/backup/log-dates',
    method: 'get'
  })
}

export function downloadLog(date: string): Promise<Blob> {
  const token = localStorage.getItem('xianyu_auth_token')
  return fetch(`/api/backup/log-download?date=${encodeURIComponent(date)}`, {
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  }).then(res => {
    if (!res.ok) throw new Error('下载失败')
    return res.blob()
  })
}
