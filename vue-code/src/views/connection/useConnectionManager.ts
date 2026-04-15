import { ref, onMounted, onUnmounted } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getAccountList } from '@/api/account'
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket'
import { showSuccess, showError, showInfo } from '@/utils'
import type { Account } from '@/types'

interface ConnectionStatus {
  xianyuAccountId: number
  connected: boolean
  status: string
  cookieStatus?: number
  cookieText?: string
  websocketToken?: string
  tokenExpireTime?: number
}

interface LogEntry {
  time: string
  message: string
  isError?: boolean
}

export function useConnectionManager() {
  const loading = ref(false)
  const accounts = ref<Account[]>([])
  const selectedAccountId = ref<number | null>(null)
  const connectionStatus = ref<ConnectionStatus | null>(null)
  const statusLoading = ref(false)
  const logs = ref<LogEntry[]>([])
  let statusInterval: number | null = null

  // 所有账号的连接状态（用于列表显示）
  const allConnectionStatuses = ref<Map<number, ConnectionStatus>>(new Map())

  const dialogs = ref({
    detail: false,
    manualCookie: false,
    manualToken: false,
    qrUpdate: false
  })

  // Load account list
  const loadAccounts = async () => {
    loading.value = true
    try {
      const response = await getAccountList()
      if (response.code === 0 || response.code === 200) {
        accounts.value = response.data?.accounts || []
        // 加载账号列表后，自动查询所有账号的连接状态
        loadAllConnectionStatuses()
      } else {
        throw new Error(response.msg || '获取账号列表失败')
      }
    } catch (error: any) {
      showError('加载账号列表失败: ' + error.message)
      accounts.value = []
    } finally {
      loading.value = false
    }
  }

  // 加载所有账号的连接状态（用于列表显示）
  const loadAllConnectionStatuses = async () => {
    const newMap = new Map<number, ConnectionStatus>()
    for (const account of accounts.value) {
      try {
        const response = await getConnectionStatus(account.id)
        if (response.code === 0 || response.code === 200) {
          const status = response.data as ConnectionStatus
          newMap.set(account.id, status)
        }
      } catch {
        // 单个账号查询失败不影响其他账号
      }
    }
    allConnectionStatuses.value = newMap
  }

  // Load connection status
  const loadConnectionStatus = async (accountId: number, silent = false) => {
    if (!silent) {
      statusLoading.value = true
    }
    try {
      const response = await getConnectionStatus(accountId)
      if (response.code === 0 || response.code === 200) {
        connectionStatus.value = response.data as ConnectionStatus
        // 同步更新 allConnectionStatuses
        allConnectionStatuses.value.set(accountId, connectionStatus.value)
        // 触发响应式更新
        allConnectionStatuses.value = new Map(allConnectionStatuses.value)
        if (!silent) {
          addLog('状态已更新')
        }
      } else {
        throw new Error(response.msg || '获取连接状态失败')
      }
    } catch (error: any) {
      if (!silent) {
        addLog('加载状态失败: ' + error.message, true)
      }
    } finally {
      statusLoading.value = false
    }
  }

  // Select account
  const selectAccount = (accountId: number) => {
    selectedAccountId.value = accountId
    loadConnectionStatus(accountId)
    startAutoRefresh()
  }

  // Start auto refresh
  const startAutoRefresh = () => {
    stopAutoRefresh()
    statusInterval = window.setInterval(() => {
      if (selectedAccountId.value) {
        loadConnectionStatus(selectedAccountId.value, true)
      }
    }, 5000)
  }

  // Stop auto refresh
  const stopAutoRefresh = () => {
    if (statusInterval) {
      clearInterval(statusInterval)
      statusInterval = null
    }
  }

  // Start connection
  const handleStartConnection = async () => {
    if (!selectedAccountId.value) return
    statusLoading.value = true
    addLog('正在启动连接...')
    try {
      const response = await startConnection(selectedAccountId.value)
      if (response.code === 0 || response.code === 200) {
        showSuccess('连接启动成功')
        addLog('连接启动成功')
        await loadConnectionStatus(selectedAccountId.value)
      } else if (response.code === 1001 && response.data?.needCaptcha) {
        addLog('检测到需要滑块验证', true)
        await ElMessageBox.confirm(
          `检测到账号需要完成滑块验证才能启动连接。\n\n` +
          `操作步骤：\n\n` +
          `1. 点击"访问闲鱼IM"按钮，打开闲鱼消息页面\n\n` +
          `2. 在闲鱼页面完成滑块验证\n\n` +
          `3. 使用帮助按钮获取 Cookie 和 Token\n\n` +
          `4. 手动更新后重新启动连接`,
          '需要滑块验证',
          {
            confirmButtonText: '访问闲鱼IM',
            cancelButtonText: '取消',
            type: 'warning',
            distinguishCancelAndClose: true,
            customClass: 'captcha-guide-dialog'
          }
        )
        window.open('https://www.goofish.com/im', '_blank')
        addLog('已打开闲鱼IM页面')
        showInfo('请完成验证后使用帮助按钮获取凭证')
      } else {
        throw new Error(response.msg || '启动连接失败')
      }
    } catch (error: any) {
      if (error !== 'cancel' && error !== 'close') {
        addLog('启动连接失败: ' + error.message, true)
      }
    } finally {
      statusLoading.value = false
    }
  }

  // Stop connection
  const handleStopConnection = async () => {
    if (!selectedAccountId.value) return
    try {
      await ElMessageBox.confirm(
        '断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？',
        '确认断开连接',
        {
          confirmButtonText: '确定断开',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
    } catch {
      return
    }
    statusLoading.value = true
    addLog('正在断开连接...')
    try {
      const response = await stopConnection(selectedAccountId.value)
      if (response.code === 0 || response.code === 200) {
        showSuccess('连接已断开')
        addLog('连接已断开')
        await loadConnectionStatus(selectedAccountId.value)
      } else {
        throw new Error(response.msg || '断开连接失败')
      }
    } catch (error: any) {
      addLog('断开连接失败: ' + error.message, true)
    } finally {
      statusLoading.value = false
    }
  }

  // Refresh
  const handleRefresh = () => {
    if (selectedAccountId.value) {
      loadConnectionStatus(selectedAccountId.value)
      showInfo('状态已刷新')
    }
  }

  // Add log
  const addLog = (message: string, isError = false) => {
    const time = new Date().toLocaleTimeString()
    logs.value.push({ time, message, isError })
    if (logs.value.length > 50) {
      logs.value.shift()
    }
  }

  // Helpers
  const getAccountName = (account: Account) => {
    return account.accountNote || account.unb || '未命名账号'
  }

  const getAccountAvatar = (account: Account) => {
    return getAccountName(account).charAt(0)
  }

  const getCookieStatusText = (status?: number) => {
    if (status === undefined || status === null) return '未知'
    const map: Record<number, string> = { 1: '有效', 2: '过期', 3: '失效' }
    return map[status] || '未知'
  }

  const getCookieStatusType = (status?: number) => {
    if (status === undefined || status === null) return 'neutral'
    const map: Record<number, string> = { 1: 'success', 2: 'warning', 3: 'danger' }
    return map[status] || 'neutral'
  }

  const formatTimestamp = (timestamp?: number) => {
    if (!timestamp) return '未设置'
    return new Date(timestamp).toLocaleString('zh-CN', {
      year: 'numeric', month: '2-digit', day: '2-digit',
      hour: '2-digit', minute: '2-digit', second: '2-digit'
    })
  }

  const isTokenExpired = (timestamp?: number) => {
    if (!timestamp) return false
    return Date.now() > timestamp
  }

  const getTokenStatusText = (timestamp?: number) => {
    if (!timestamp) return '未设置'
    return isTokenExpired(timestamp) ? '已过期' : '有效'
  }

  const getTokenStatusType = (timestamp?: number) => {
    if (!timestamp) return 'neutral'
    return isTokenExpired(timestamp) ? 'danger' : 'success'
  }

  onMounted(async () => {
    await loadAccounts()
  })

  onUnmounted(() => {
    stopAutoRefresh()
  })

  return {
    loading,
    accounts,
    selectedAccountId,
    connectionStatus,
    statusLoading,
    logs,
    dialogs,
    allConnectionStatuses,
    loadAccounts,
    selectAccount,
    handleStartConnection,
    handleStopConnection,
    handleRefresh,
    getAccountName,
    getAccountAvatar,
    getCookieStatusText,
    getCookieStatusType,
    formatTimestamp,
    getTokenStatusText,
    getTokenStatusType,
    addLog,
    loadConnectionStatus
  }
}
