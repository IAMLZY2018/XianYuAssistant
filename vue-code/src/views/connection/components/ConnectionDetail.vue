<script setup lang="ts">
import { ref, watch, computed, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket'
import { queryOperationLogs, type OperationLog } from '@/api/operation-log'
import { showSuccess, showError, showInfo } from '@/utils'
import ManualUpdateCookieDialog from './ManualUpdateCookieDialog.vue'
import QRUpdateDialog from './QRUpdateDialog.vue'
import CaptchaGuideDialog from './CaptchaGuideDialog.vue'

import IconWifi from '@/components/icons/IconWifi.vue'
import IconWifiOff from '@/components/icons/IconWifiOff.vue'
import IconCookie from '@/components/icons/IconCookie.vue'
import IconKey from '@/components/icons/IconKey.vue'
import IconPlay from '@/components/icons/IconPlay.vue'
import IconStop from '@/components/icons/IconStop.vue'
import IconQrCode from '@/components/icons/IconQrCode.vue'
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
  isMobile?: boolean
}

const props = defineProps<Props>()

const connectionStatus = ref<ConnectionStatus | null>(null)
const statusLoading = ref(false)
const operationLogs = ref<OperationLog[]>([])
let statusInterval: number | null = null

const showManualUpdateCookieDialog = ref(false)
const showQRUpdateDialog = ref(false)
const showCaptchaGuideDialog = ref(false)
const showMobileDetail = ref(false)
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
      showSuccess('连接启动成功')
      await loadConnectionStatus()
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
    await ElMessageBox.confirm(
      '断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？',
      '确认断开连接',
      { confirmButtonText: '确定断开', cancelButtonText: '取消', type: 'warning' }
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
  if (status === 1) return '#34c759'
  if (status === 2) return '#ff9500'
  if (status === 3) return '#ff3b30'
  return '#86868b'
}

const formatTimestamp = (timestamp?: number) => {
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
  if (!timestamp) return '#86868b'
  return isTokenExpired(timestamp) ? '#ff3b30' : '#34c759'
}

const getMH5TkStatusText = (mH5Tk?: string) => {
  if (!mH5Tk) return '未设置'
  return '有效'
}

const getMH5TkStatusColor = (mH5Tk?: string) => {
  if (!mH5Tk) return '#86868b'
  return '#34c759'
}

const h5Token = computed(() => connectionStatus.value?.mH5Tk || connectionStatus.value?.mh5Tk)

const getOperationStatusText = (status: number) => {
  const map: Record<number, string> = { 1: '成功', 2: '失败', 3: '部分成功' }
  return map[status] || '未知'
}

const getOperationStatusColor = (status: number) => {
  if (status === 1) return '#34c759'
  if (status === 2) return '#ff3b30'
  if (status === 3) return '#ff9500'
  return '#86868b'
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
    if (props.isMobile) showMobileDetail.value = true
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
  <div v-if="!isMobile" class="detail-panel">
    <div v-if="!accountId" class="detail-empty">
      <div class="detail-empty__icon"><IconLink /></div>
      <p class="detail-empty__text">请选择一个账号查看连接状态</p>
    </div>

    <div v-else class="detail-scroll" :class="{ 'detail-scroll--loading': statusLoading }">
      <div v-if="connectionStatus" class="detail-body">
        <div class="status-header">
          <div class="status-header__left">
            <div class="status-icon" :class="connectionStatus.connected ? 'status-icon--on' : 'status-icon--off'">
              <component :is="connectionStatus.connected ? IconWifi : IconWifiOff" />
            </div>
            <div class="status-header__info">
              <span class="status-header__title">连接状态</span>
              <span v-if="connectionStatus.cookieStatus !== 1" class="status-header__sub status-header__sub--warning">
                Cookie已过期，请点击<span class="status-header__link">凭证详情</span>按钮更新Cookie
              </span>
              <span v-else class="status-header__sub">账号 ID: {{ connectionStatus.xianyuAccountId }}</span>
            </div>
          </div>
          <span class="status-badge" :class="connectionStatus.connected ? 'status-badge--on' : 'status-badge--off'">
            <component :is="connectionStatus.connected ? IconCheck : IconAlert" />
            {{ connectionStatus.connected ? '已连接' : '未连接' }}
          </span>
        </div>

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

    <el-dialog
      v-model="showCredentialDialog"
      title="凭证详情"
      width="500px"
      class="credential-dialog"
    >
      <div v-if="connectionStatus" class="credential-dialog__content">
        <div class="credential-dialog__actions">
          <button class="btn btn--qr" @click="showQRUpdateDialog = true">
            <IconQrCode /><span>扫码更新</span>
          </button>
          <button class="btn btn--secondary" @click="showManualUpdateCookieDialog = true">
            <IconCookie /><span>手动更新Cookie</span>
          </button>
        </div>

        <div class="credential-list">
          <div class="credential-item">
            <div class="credential-item__header">
              <div class="credential-item__left">
                <div class="credential-item__icon credential-item__icon--cookie"><IconCookie /></div>
                <span class="credential-item__name">Cookie 凭证</span>
              </div>
              <span class="credential-item__status" :style="{ color: getCookieStatusColor(connectionStatus.cookieStatus) }">
                {{ getCookieStatusText(connectionStatus.cookieStatus) }}
              </span>
            </div>
            <div class="credential-item__value" v-if="connectionStatus.cookieText">
              {{ connectionStatus.cookieText.substring(0, 60) }}...
              <span class="credential-item__meta">{{ connectionStatus.cookieText.length }} 字符</span>
            </div>
            <div class="credential-item__value credential-item__value--empty" v-else>未设置</div>
          </div>

          <div class="credential-item">
            <div class="credential-item__header">
              <div class="credential-item__left">
                <div class="credential-item__icon credential-item__icon--token"><IconKey /></div>
                <span class="credential-item__name">WebSocket Token</span>
              </div>
              <span class="credential-item__status" :style="{ color: getTokenStatusColor(connectionStatus.tokenExpireTime) }">
                {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
              </span>
            </div>
            <div class="credential-item__value" v-if="connectionStatus.websocketToken">
              {{ connectionStatus.websocketToken.substring(0, 40) }}...
              <span class="credential-item__meta">{{ connectionStatus.websocketToken.length }} 字符</span>
            </div>
            <div class="credential-item__value credential-item__value--empty" v-else>未设置</div>
            <div class="credential-item__expire" v-if="connectionStatus.tokenExpireTime">
              过期时间: {{ formatTimestamp(connectionStatus.tokenExpireTime) }}
            </div>
          </div>

          <div class="credential-item">
            <div class="credential-item__header">
              <div class="credential-item__left">
                <div class="credential-item__icon credential-item__icon--h5"><IconKey /></div>
                <span class="credential-item__name">H5 Token (_m_h5_tk)</span>
              </div>
              <span class="credential-item__status" :style="{ color: getMH5TkStatusColor(h5Token) }">
                {{ getMH5TkStatusText(h5Token) }}
              </span>
            </div>
            <div class="credential-item__value" v-if="h5Token">
              {{ h5Token.substring(0, 40) }}...
              <span class="credential-item__meta">{{ h5Token.length }} 字符</span>
            </div>
            <div class="credential-item__value credential-item__value--empty" v-else>未设置</div>
          </div>
        </div>
      </div>
    </el-dialog>

    <ManualUpdateCookieDialog
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

  <template v-else>
    <Transition name="slide-up">
      <div v-if="showMobileDetail && accountId" class="mobile-overlay">
        <div class="mobile-overlay__header">
          <button class="mobile-overlay__back" @click="showMobileDetail = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/>
            </svg>
            <span>返回</span>
          </button>
          <span class="mobile-overlay__title">连接详情</span>
          <button class="mobile-overlay__refresh" @click="handleRefresh" :disabled="statusLoading">
            <IconRefresh />
          </button>
        </div>

        <div class="mobile-overlay__scroll" :class="{ 'detail-scroll--loading': statusLoading }">
          <div v-if="connectionStatus" class="detail-body">
            <div class="status-header">
              <div class="status-header__left">
                <div class="status-icon" :class="connectionStatus.connected ? 'status-icon--on' : 'status-icon--off'">
                  <component :is="connectionStatus.connected ? IconWifi : IconWifiOff" />
                </div>
                <div class="status-header__info">
                  <span class="status-header__title">连接状态</span>
                  <span v-if="connectionStatus.cookieStatus !== 1" class="status-header__sub status-header__sub--warning">
                    Cookie已过期，请更新
                  </span>
                </div>
              </div>
              <span class="status-badge" :class="connectionStatus.connected ? 'status-badge--on' : 'status-badge--off'">
                {{ connectionStatus.connected ? '已连接' : '未连接' }}
              </span>
            </div>

            <div class="status-cards">
              <div class="status-card" :class="canSyncGoods ? 'status-card--success' : 'status-card--danger'">
                <div class="status-card__icon">
                  <component :is="canSyncGoods ? IconCheck : IconAlert" />
                </div>
                <div class="status-card__content">
                  <span class="status-card__title">同步商品信息</span>
                  <span class="status-card__desc">{{ canSyncGoods ? '可正常同步' : '无法同步' }}</span>
                </div>
              </div>

              <div class="status-card" :class="canAutoReply ? 'status-card--success' : 'status-card--danger'">
                <div class="status-card__icon">
                  <component :is="canAutoReply ? IconCheck : IconAlert" />
                </div>
                <div class="status-card__content">
                  <span class="status-card__title">自动发货与回复</span>
                  <span class="status-card__desc">{{ canAutoReply ? '可正常工作' : '无法工作' }}</span>
                </div>
              </div>
            </div>

            <div class="action-bar action-bar--mobile">
              <button
                v-if="connectionStatus.connected === true"
                class="btn btn--stop btn--block"
                @click="handleStopConnection"
              >
                <IconStop /><span>断开连接</span>
              </button>
              <button
                v-else
                class="btn btn--start btn--block"
                @click="handleStartConnection"
              >
                <IconPlay /><span>开始连接</span>
              </button>
              <div class="action-bar__row">
                <button class="btn btn--ghost btn--small btn--flex" @click="showCredentialDialog = true">
                  <IconKey /><span>凭证详情</span>
                </button>
                <button class="btn btn--ghost btn--small btn--flex" @click="handleRefresh" :disabled="statusLoading">
                  <IconRefresh /><span>刷新状态</span>
                </button>
              </div>
            </div>

            <div class="log-section">
              <div class="log-section__header">
                <div class="log-section__title"><IconLog /><span>操作日志</span></div>
              </div>
              <div class="log-container">
                <div v-for="log in operationLogs" :key="log.id" class="log-entry">
                  <span class="log-entry__time">{{ formatTimestamp(log.createTime) }}</span>
                  <span class="log-entry__desc">{{ log.operationDesc }}</span>
                </div>
                <div v-if="operationLogs.length === 0" class="log-empty">暂无日志</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <el-dialog
      v-model="showCredentialDialog"
      title="凭证详情"
      width="90%"
      class="credential-dialog"
    >
      <div v-if="connectionStatus" class="credential-dialog__content">
        <div class="credential-dialog__actions">
          <button class="btn btn--qr btn--block" @click="showQRUpdateDialog = true">
            <IconQrCode /><span>扫码更新</span>
          </button>
          <button class="btn btn--secondary btn--block" @click="showManualUpdateCookieDialog = true">
            <IconCookie /><span>手动更新Cookie</span>
          </button>
        </div>

        <div class="credential-list">
          <div class="credential-item">
            <div class="credential-item__header">
              <span class="credential-item__name">Cookie 凭证</span>
              <span class="credential-item__status" :style="{ color: getCookieStatusColor(connectionStatus.cookieStatus) }">
                {{ getCookieStatusText(connectionStatus.cookieStatus) }}
              </span>
            </div>
            <div class="credential-item__value" v-if="connectionStatus.cookieText">
              {{ connectionStatus.cookieText.substring(0, 50) }}...
            </div>
            <div class="credential-item__value credential-item__value--empty" v-else>未设置</div>
          </div>

          <div class="credential-item">
            <div class="credential-item__header">
              <span class="credential-item__name">WebSocket Token</span>
              <span class="credential-item__status" :style="{ color: getTokenStatusColor(connectionStatus.tokenExpireTime) }">
                {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
              </span>
            </div>
            <div class="credential-item__value" v-if="connectionStatus.websocketToken">
              {{ connectionStatus.websocketToken.substring(0, 30) }}...
            </div>
            <div class="credential-item__value credential-item__value--empty" v-else>未设置</div>
          </div>

          <div class="credential-item">
            <div class="credential-item__header">
              <span class="credential-item__name">H5 Token</span>
              <span class="credential-item__status" :style="{ color: getMH5TkStatusColor(h5Token) }">
                {{ getMH5TkStatusText(h5Token) }}
              </span>
            </div>
            <div class="credential-item__value" v-if="h5Token">
              {{ h5Token.substring(0, 30) }}...
            </div>
            <div class="credential-item__value credential-item__value--empty" v-else>未设置</div>
          </div>
        </div>
      </div>
    </el-dialog>

    <ManualUpdateCookieDialog
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
  </template>
</template>

<style scoped>
.detail-panel {
  --c-bg: transparent;
  --c-surface: #ffffff;
  --c-border: rgba(0, 0, 0, 0.06);
  --c-text-1: #1d1d1f;
  --c-text-2: #6e6e73;
  --c-text-3: #86868b;
  --c-accent: #007aff;
  --c-danger: #ff3b30;
  --c-success: #34c759;
  --c-warning: #ff9500;
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
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.12) transparent;
}

.detail-scroll--loading {
  opacity: 0.5;
  pointer-events: none;
}

.detail-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
}

.status-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.status-header__left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--c-r-sm);
  display: flex;
  align-items: center;
  justify-content: center;
}

.status-icon svg {
  width: 20px;
  height: 20px;
}

.status-icon--on {
  background: rgba(52, 199, 89, 0.12);
  color: var(--c-success);
}

.status-icon--off {
  background: rgba(255, 59, 48, 0.12);
  color: var(--c-danger);
}

.status-header__info {
  display: flex;
  flex-direction: column;
}

.status-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text-1);
}

.status-header__sub {
  font-size: 12px;
  color: var(--c-text-3);
}

.status-header__sub--warning {
  color: var(--c-warning);
  font-weight: 500;
}

.status-header__link {
  color: var(--c-danger);
  font-weight: 600;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 20px;
}

.status-badge svg {
  width: 12px;
  height: 12px;
}

.status-badge--on {
  color: var(--c-success);
  background: rgba(52, 199, 89, 0.1);
}

.status-badge--off {
  color: var(--c-danger);
  background: rgba(255, 59, 48, 0.1);
}

.status-cards {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.status-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-radius: var(--c-r-md);
  border: 1px solid;
}

.status-card--success {
  border-color: rgba(52, 199, 89, 0.2);
  background: rgba(52, 199, 89, 0.06);
}

.status-card--danger {
  border-color: rgba(255, 59, 48, 0.2);
  background: rgba(255, 59, 48, 0.06);
}

.status-card__icon {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-card--success .status-card__icon {
  background: rgba(52, 199, 89, 0.15);
  color: var(--c-success);
}

.status-card--danger .status-card__icon {
  background: rgba(255, 59, 48, 0.15);
  color: var(--c-danger);
}

.status-card__icon svg {
  width: 16px;
  height: 16px;
}

.status-card__content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-card__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-1);
}

.status-card__desc {
  font-size: 11px;
  color: var(--c-text-3);
}

.status-card--success .status-card__title {
  color: var(--c-success);
}

.status-card--danger .status-card__title {
  color: var(--c-danger);
}

.credential-dialog__content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.credential-dialog__actions {
  display: flex;
  gap: 12px;
}

@media screen and (max-width: 600px) {
  .credential-dialog__actions {
    flex-direction: column;
  }
  
  .credential-dialog__actions .btn {
    padding: 12px 16px;
    font-size: 15px;
    font-weight: 600;
  }
}

.credential-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.credential-item {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--c-border);
  border-radius: var(--c-r-md);
  padding: 12px;
}

.credential-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.credential-item__left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.credential-item__icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.credential-item__icon svg {
  width: 12px;
  height: 12px;
}

.credential-item__icon--cookie {
  background: rgba(255, 149, 0, 0.1);
  color: var(--c-warning);
}

.credential-item__icon--token {
  background: rgba(52, 199, 89, 0.1);
  color: var(--c-success);
}

.credential-item__icon--h5 {
  background: rgba(0, 122, 255, 0.1);
  color: var(--c-accent);
}

.credential-item__name {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-1);
}

.credential-item__status {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0, 0, 0, 0.05);
}

.credential-item__value {
  font-family: 'SF Mono', 'Menlo', monospace;
  font-size: 11px;
  color: var(--c-text-2);
  word-break: break-all;
  line-height: 1.5;
  margin-top: 8px;
}

.credential-item__value--empty {
  color: var(--c-text-3);
  font-style: italic;
}

.credential-item__meta {
  display: inline-block;
  margin-left: 8px;
  color: var(--c-text-3);
  font-size: 10px;
}

.credential-item__expire {
  margin-top: 6px;
  font-size: 11px;
  color: var(--c-text-3);
}

.action-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-bar--mobile {
  flex-direction: column;
}

.action-bar__row {
  display: flex;
  gap: 8px;
}

.btn--flex {
  flex: 1;
}

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--c-r-sm);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.btn svg {
  width: 14px;
  height: 14px;
}

.btn--block {
  width: 100%;
}

.btn--mini {
  padding: 4px 10px;
  font-size: 11px;
  background: rgba(0, 0, 0, 0.05);
  color: var(--c-text-1);
  border: 1px solid var(--c-border);
  border-radius: 6px;
}

.btn--mini:hover {
  background: rgba(0, 0, 0, 0.08);
}

.btn--mini svg {
  width: 12px;
  height: 12px;
}

.btn--small {
  padding: 6px 12px;
  font-size: 12px;
}

.btn--small svg {
  width: 12px;
  height: 12px;
}

.btn--secondary {
  background: rgba(0, 0, 0, 0.05);
  color: var(--c-text-1);
  border-color: var(--c-border);
}

.btn--secondary:hover {
  background: rgba(0, 0, 0, 0.08);
}

.btn--ghost {
  background: transparent;
  color: var(--c-text-2);
  border-color: var(--c-border);
}

.btn--ghost:hover {
  background: rgba(0, 0, 0, 0.04);
}

.btn--start {
  background: var(--c-success);
  color: white;
}

.btn--start:hover {
  background: #2db84e;
}

.btn--stop {
  background: var(--c-danger);
  color: white;
}

.btn--stop:hover {
  background: #e6352a;
}

.btn--qr {
  background: var(--c-accent);
  color: white;
}

.btn--qr:hover {
  background: #0066d6;
}

.log-section {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-r-md);
  overflow: hidden;
}

.log-section__header {
  padding: 12px;
  border-bottom: 1px solid var(--c-border);
  background: rgba(0, 0, 0, 0.02);
}

.log-section__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-1);
}

.log-section__title svg {
  width: 14px;
  height: 14px;
}

.log-container {
  max-height: 200px;
  overflow-y: auto;
  padding: 8px 12px;
}

.log-entry {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 0;
  font-size: 12px;
  border-bottom: 1px solid var(--c-border);
}

.log-entry:last-child {
  border-bottom: none;
}

.log-entry__time {
  flex-shrink: 0;
  color: var(--c-text-3);
  font-size: 11px;
}

.log-entry__module {
  flex-shrink: 0;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.05);
  font-size: 10px;
  font-weight: 600;
}

.log-entry__desc {
  flex: 1;
  color: var(--c-text-2);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.log-entry__status {
  flex-shrink: 0;
  font-weight: 500;
}

.log-empty {
  text-align: center;
  color: var(--c-text-3);
  font-size: 12px;
  padding: 12px;
}

.mobile-overlay {
  position: fixed;
  inset: 0;
  background: #f5f5f7;
  z-index: 100;
  display: flex;
  flex-direction: column;
}

.mobile-overlay__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: white;
  border-bottom: 1px solid rgba(0, 0, 0, 0.08);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.mobile-overlay__back {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #007aff;
  font-size: 15px;
  cursor: pointer;
  padding: 4px 8px;
  margin-left: -8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.mobile-overlay__back:active {
  background: rgba(0, 122, 255, 0.1);
}

.mobile-overlay__title {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
}

.mobile-overlay__refresh {
  background: none;
  border: none;
  color: #007aff;
  cursor: pointer;
  padding: 8px;
  border-radius: 6px;
  transition: background 0.2s;
}

.mobile-overlay__refresh:active {
  background: rgba(0, 122, 255, 0.1);
}

.mobile-overlay__refresh svg {
  width: 18px;
  height: 18px;
}

.mobile-overlay__scroll {
  flex: 1;
  overflow-y: auto;
  background: #f5f5f7;
}

@media screen and (max-width: 600px) {
  .status-cards {
    grid-template-columns: 1fr;
  }
  
  .detail-body {
    padding: 12px;
    gap: 12px;
  }
  
  .status-card {
    padding: 14px;
  }
  
  .action-bar--mobile .btn {
    padding: 12px 16px;
    font-size: 15px;
    font-weight: 600;
  }
  
  .action-bar__row .btn {
    padding: 10px 14px;
    font-size: 13px;
  }
  
  .btn--start {
    background: #34c759;
  }
  
  .btn--stop {
    background: #ff3b30;
  }
  
  .btn--qr {
    background: #007aff;
  }
  
  .btn--secondary {
    background: rgba(0, 0, 0, 0.05);
    color: #1d1d1f;
  }
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s ease;
}

.slide-up-enter-from,
.slide-up-leave-to {
  transform: translateY(100%);
}
</style>
