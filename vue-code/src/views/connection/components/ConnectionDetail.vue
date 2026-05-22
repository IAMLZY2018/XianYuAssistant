<script setup lang="ts">
import { ref, watch, computed, onBeforeUnmount } from 'vue'
import { showConfirm } from '@/utils/confirm'
import { toast } from '@/utils/toast'
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket'
import { queryOperationLogs, type OperationLog } from '@/api/operation-log'
import { showSuccess, showError, showInfo } from '@/utils'
import CredentialModal from './CredentialModal.vue'
import ManualUpdateCookieModal from './ManualUpdateCookieModal.vue'
import QRUpdateDialog from './QRUpdateDialog.vue'
import CaptchaGuideDialog from './CaptchaGuideDialog.vue'

import IconKey from '@/components/icons/IconKey.vue'
import IconPlay from '@/components/icons/IconPlay.vue'
import IconStop from '@/components/icons/IconStop.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconLog from '@/components/icons/IconLog.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconAlert from '@/components/icons/IconAlert.vue'
import IconLink from '@/components/icons/IconLink.vue'

interface ConnectionStatus {
  xianyuAccountId: number
  connected: boolean
  status: string
  cookieStatus?: number
  cookieText?: string
  mH5Tk?: string
  mh5Tk?: string
  websocketToken?: string
  tokenExpireTime?: number
}

interface Props {
  accountId: number | null
}

const props = defineProps<Props>()

const connectionStatus = ref<ConnectionStatus | null>(null)
const statusLoading = ref(false)
const operationLogs = ref<OperationLog[]>([])
let statusInterval: number | null = null

const showManualUpdateCookieDialog = ref(false)
const showQRUpdateDialog = ref(false)
const showCaptchaGuideDialog = ref(false)
const showCredentialDialog = ref(false)

const loadConnectionStatus = async (silent = false) => {
  if (!props.accountId) return
  if (!silent) statusLoading.value = true
  try {
    const response = await getConnectionStatus(props.accountId)
    if (response.code === 0 || response.code === 200) {
      connectionStatus.value = response.data as ConnectionStatus
    } else {
      throw new Error(response.msg || '获取连接状态失败')
    }
  } catch (error: any) {
    console.error('加载状态失败:', error.message)
  } finally {
    statusLoading.value = false
  }
}

const loadOperationLogs = async () => {
  if (!props.accountId) return
  try {
    const response = await queryOperationLogs({
      accountId: props.accountId,
      page: 1,
      pageSize: 20
    })
    if (response.code === 0 || response.code === 200) {
      const data = response.data
      operationLogs.value = (data?.logs || []).filter(
        (log: OperationLog) => log.operationModule === 'COOKIE' || log.operationModule === 'TOKEN'
      )
    }
  } catch (error: any) {
    console.error('加载操作日志失败:', error.message)
  }
}

const handleStartConnection = async () => {
  if (!props.accountId) return
  statusLoading.value = true
  try {
    const response = await startConnection(props.accountId)
    if (response.code === 0 || response.code === 200) {
      await loadConnectionStatus()
      toast.info('1、请勿使用闲鱼网页版进行消息回复，不然容易触发风控；2、刚开始使用会掉线且无法自动刷新的情况，属于正常，多挂几天就好了；3、有问题可以在"系统设置"->"关于"加入交流群；')
    } else if (response.code === 1001 && response.data?.needCaptcha) {
      showCaptchaGuideDialog.value = true
    } else {
      throw new Error(response.msg || '启动连接失败')
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      showError('启动连接失败: ' + error.message)
    }
  } finally {
    statusLoading.value = false
  }
}

const handleStopConnection = async () => {
  if (!props.accountId) return
  try {
    await showConfirm(
      '断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？',
      '确认断开连接'
    )
  } catch { return }

  statusLoading.value = true
  try {
    const response = await stopConnection(props.accountId)
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接已断开')
      await loadConnectionStatus()
    } else {
      throw new Error(response.msg || '断开连接失败')
    }
  } catch (error: any) {
    showError('断开连接失败: ' + error.message)
  } finally {
    statusLoading.value = false
  }
}

const handleRefresh = async () => {
  await Promise.all([loadConnectionStatus(), loadOperationLogs()])
  showInfo('状态已刷新')
}

const handleManualUpdateCookieSuccess = async () => {
  await loadConnectionStatus()
}

const handleQRUpdateSuccess = async () => {
  await loadConnectionStatus()
}

const handleCaptchaConfirm = () => {
  window.open('https://www.goofish.com/im', '_blank')
  showInfo('请完成验证后使用帮助按钮获取凭证')
}

const getCookieStatusText = (status?: number) => {
  if (status === undefined || status === null) return '未知'
  const map: Record<number, string> = { 1: '有效', 2: '过期', 3: '失效' }
  return map[status] || '未知'
}

const getCookieStatusColor = (status?: number) => {
  if (status === 1) return '#30D158'
  if (status === 2) return '#FF9F0A'
  if (status === 3) return '#FF453A'
  return 'rgba(28,28,30,.55)'
}

const formatTimestamp = (timestamp?: number | string) => {
  if (!timestamp) return '未设置'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  }).replace(/\//g, '-')
}

const isTokenExpired = (timestamp?: number) => {
  if (!timestamp) return false
  return Date.now() > timestamp
}

const getTokenStatusText = (timestamp?: number) => {
  if (!timestamp) return '未设置'
  return isTokenExpired(timestamp) ? '已过期' : '有效'
}

const getTokenStatusColor = (timestamp?: number) => {
  if (!timestamp) return 'rgba(28,28,30,.55)'
  return isTokenExpired(timestamp) ? '#FF453A' : '#30D158'
}

const getMH5TkStatusText = (mH5Tk?: string) => {
  if (!mH5Tk) return '未设置'
  return '有效'
}

const getMH5TkStatusColor = (mH5Tk?: string) => {
  if (!mH5Tk) return 'rgba(28,28,30,.55)'
  return '#30D158'
}

const h5Token = computed(() => connectionStatus.value?.mH5Tk || connectionStatus.value?.mh5Tk)

const getOperationStatusText = (status: number) => {
  const map: Record<number, string> = { 1: '成功', 2: '失败', 3: '部分成功' }
  return map[status] || '未知'
}

const getOperationStatusColor = (status: number) => {
  if (status === 1) return '#30D158'
  if (status === 2) return '#FF453A'
  if (status === 3) return '#FF9F0A'
  return 'rgba(28,28,30,.55)'
}

const canSyncGoods = computed(() => connectionStatus.value?.cookieStatus === 1)
const canAutoReply = computed(() => connectionStatus.value?.connected === true)

watch(() => props.accountId, (newId) => {
  if (newId) {
    loadConnectionStatus()
    loadOperationLogs()
    if (statusInterval) clearInterval(statusInterval)
    statusInterval = window.setInterval(() => {
      if (props.accountId) {
        loadConnectionStatus(true)
        loadOperationLogs()
      }
    }, 10000)
  } else {
    connectionStatus.value = null
    operationLogs.value = []
    if (statusInterval) {
      clearInterval(statusInterval)
      statusInterval = null
    }
  }
}, { immediate: true })

onBeforeUnmount(() => {
  if (statusInterval) clearInterval(statusInterval)
})
</script>

<template>
  <div class="detail-panel">
    <div v-if="!accountId" class="detail-empty">
      <div class="detail-empty__icon"><IconLink /></div>
      <p class="detail-empty__text">请选择一个账号查看连接状态</p>
    </div>

    <div v-else class="detail-scroll" :class="{ 'detail-scroll--loading': statusLoading }">
      <div v-if="connectionStatus" class="detail-body">
        <div class="status-cards">
          <div class="status-card" :class="canSyncGoods ? 'status-card--success' : 'status-card--danger'">
            <div class="status-card__icon">
              <component :is="canSyncGoods ? IconCheck : IconAlert" />
            </div>
            <div class="status-card__content">
              <span class="status-card__title">同步商品信息</span>
              <span class="status-card__desc">{{ canSyncGoods ? '可正常同步商品信息' : 'Cookie无效，无法同步商品信息' }}</span>
            </div>
          </div>

          <div class="status-card" :class="canAutoReply ? 'status-card--success' : 'status-card--danger'">
            <div class="status-card__icon">
              <component :is="canAutoReply ? IconCheck : IconAlert" />
            </div>
            <div class="status-card__content">
              <span class="status-card__title">自动发货与回复</span>
              <span class="status-card__desc">{{ canAutoReply ? '可正常自动发货与回复' : '未连接，无法自动发货与回复' }}</span>
            </div>
          </div>
        </div>

        <div class="action-bar">
          <button
            v-if="connectionStatus.connected === true"
            class="btn btn--stop"
            @click="handleStopConnection"
          >
            <IconStop /><span>断开连接</span>
          </button>
          <button
            v-else
            class="btn btn--start"
            @click="handleStartConnection"
          >
            <IconPlay /><span>开始连接</span>
          </button>
          <button class="btn btn--ghost btn--small" @click="showCredentialDialog = true">
            <IconKey /><span>凭证详情</span>
          </button>
          <button class="btn btn--ghost btn--small" @click="handleRefresh" :disabled="statusLoading">
            <IconRefresh /><span>刷新状态</span>
          </button>
        </div>

        <div class="log-section">
          <div class="log-section__header">
            <div class="log-section__title">
              <IconLog />
              <span>操作日志</span>
            </div>
          </div>
          <div class="log-container">
            <div v-for="log in operationLogs" :key="log.id" class="log-entry">
              <span class="log-entry__time">{{ formatTimestamp(log.createTime) }}</span>
              <span class="log-entry__module">{{ log.operationModule }}</span>
              <span class="log-entry__desc">{{ log.operationDesc }}</span>
              <span class="log-entry__status" :style="{ color: getOperationStatusColor(log.operationStatus) }">
                {{ getOperationStatusText(log.operationStatus) }}
              </span>
            </div>
            <div v-if="operationLogs.length === 0" class="log-empty">暂无Cookie/Token相关日志</div>
          </div>
        </div>
      </div>
    </div>

    <CredentialModal
      v-model="showCredentialDialog"
      :connection-status="connectionStatus"
      @qr-update="showQRUpdateDialog = true"
      @manual-update="showManualUpdateCookieDialog = true"
    />

    <ManualUpdateCookieModal
      v-if="connectionStatus"
      v-model="showManualUpdateCookieDialog"
      :account-id="accountId || 0"
      :current-cookie="connectionStatus.cookieText || ''"
      @success="handleManualUpdateCookieSuccess"
    />
    <QRUpdateDialog
      v-model="showQRUpdateDialog"
      :account-id="accountId || 0"
      @success="handleQRUpdateSuccess"
    />
    <CaptchaGuideDialog
      v-model="showCaptchaGuideDialog"
      @confirm="handleCaptchaConfirm"
    />
  </div>
</template>

<style scoped>
.detail-panel {
  --c-bg: transparent;
  --c-surface: #ffffff;
  --c-border: rgba(0, 0, 0, 0.06);
  --c-text-1: #1c1c1e;
  --c-text-2: rgba(28,28,30,.55);
  --c-text-3: rgba(28,28,30,.55);
  --c-accent: #0A84FF;
  --c-danger: #FF453A;
  --c-success: #30D158;
  --c-warning: #FF9F0A;
  --c-r-sm: 8px;
  --c-r-md: 12px;
  --c-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.detail-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
}

.detail-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: var(--c-text-3);
}

.detail-empty__icon {
  width: 48px;
  height: 48px;
  opacity: 0.3;
}

.detail-empty__icon svg {
  width: 36px;
  height: 36px;
}

.detail-empty__text {
  font-size: 14px;
  margin: 0;
}

.detail-scroll {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  scrollbar-width: none;
}

.detail-scroll::-webkit-scrollbar {
  display: none;
}

.detail-scroll--loading {
  opacity: 0.5;
  pointer-events: none;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding: 20px 16px;
}

.status-cards {
  display: flex;
  gap: 12px;
  width: 100%;
}

.status-card {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: 16px;
  border: 1px solid;
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  flex: 1;
}

.status-card--success {
  border-color: rgba(52, 199, 89, 0.3);
  background: rgba(52, 199, 89, 0.08);
}

.status-card--success:hover {
  border-color: rgba(52, 199, 89, 0.4);
  background: rgba(52, 199, 89, 0.12);
}

.status-card--danger {
  border-color: rgba(255, 59, 48, 0.3);
  background: rgba(255, 59, 48, 0.08);
}

.status-card--danger:hover {
  border-color: rgba(255, 59, 48, 0.4);
  background: rgba(255, 59, 48, 0.12);
}

.status-card__icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-card--success .status-card__icon {
  background: rgba(52, 199, 89, 0.2);
  color: var(--c-success);
}

.status-card--danger .status-card__icon {
  background: rgba(255, 59, 48, 0.2);
  color: var(--c-danger);
}

.status-card__icon svg { width: 20px; height: 20px; }

.status-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  width: 100%;
  min-width: 0;
}

.status-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
  letter-spacing: -0.01em;
}

.status-card__desc {
  font-size: 11px;
  color: var(--c-text-3);
  line-height: 1.4;
}

.status-card--success .status-card__title { color: var(--c-success); }
.status-card--danger .status-card__title { color: var(--c-danger); }

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 16px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  -webkit-tap-highlight-color: transparent;
  letter-spacing: -0.01em;
}

.btn svg { width: 16px; height: 16px; }

.btn--small {
  padding: 8px 12px;
  font-size: 13px;
  font-weight: 500;
}

.btn--small svg { width: 14px; height: 14px; }

.btn--start {
  background: var(--c-success);
  color: white;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

.btn--start:hover {
  box-shadow: 0 6px 16px rgba(52, 199, 89, 0.4);
  transform: translateY(-1px);
}

.btn--start:active { transform: scale(0.97); }

.btn--stop {
  background: var(--c-danger);
  color: white;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

.btn--stop:hover {
  box-shadow: 0 6px 16px rgba(255, 59, 48, 0.4);
  transform: translateY(-1px);
}

.btn--stop:active { transform: scale(0.97); }

.btn--ghost {
  background: rgba(0, 122, 255, 0.08);
  color: var(--c-accent);
  border-color: rgba(0, 122, 255, 0.2);
}

.btn--ghost:hover {
  background: rgba(0, 122, 255, 0.15);
}

.btn--ghost:active { transform: scale(0.97); }

.btn--ghost:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.log-section {
  margin-top: 20px;
  padding-bottom: 8px;
}

.log-section__header {
  margin-bottom: 10px;
}

.log-section__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
}

.log-section__title svg { width: 16px; height: 16px; }

.log-container {
  background: #2c3e50;
  color: #ecf0f1;
  border-radius: 8px;
  padding: 12px;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  max-height: 180px;
  overflow-y: auto;
}

.log-entry {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  line-height: 1.5;
}

.log-entry:last-child { margin-bottom: 0; }

.log-entry__time {
  color: #95a5a6;
  font-size: 11px;
  flex-shrink: 0;
}

.log-entry__module {
  color: #3498db;
  flex-shrink: 0;
  font-weight: 500;
}

.log-entry__desc {
  color: #ecf0f1;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.log-entry__status {
  flex-shrink: 0;
  font-weight: 500;
}

.log-empty {
  text-align: center;
  color: #95a5a6;
  padding: 16px;
  font-size: 12px;
}
</style>
