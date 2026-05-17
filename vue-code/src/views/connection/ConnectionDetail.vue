<script setup lang="ts">
import { ref, watch, computed, onMounted, onUnmounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket'
import { queryOperationLogs, type OperationLog } from '@/api/operation-log'
import { showSuccess, showError, showInfo } from '@/utils'
import { ElMessageBox } from 'element-plus'
import DesktopDetail from './components/ConnectionDetail.vue'
import ManualUpdateCookieModal from './components/ManualUpdateCookieModal.vue'
import QRUpdateDialog from './components/QRUpdateDialog.vue'
import CaptchaGuideDialog from './components/CaptchaGuideDialog.vue'

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
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconHelp from '@/components/icons/IconHelp.vue'
import IconCopy from '@/components/icons/IconCopy.vue'

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

const route = useRoute()
const router = useRouter()
const accountId = computed(() => Number(route.params.id) || null)

const isMobile = ref(false)
const checkScreenSize = () => { isMobile.value = window.innerWidth < 768 }
onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})
onUnmounted(() => { window.removeEventListener('resize', checkScreenSize) })

const connectionStatus = ref<ConnectionStatus | null>(null)
const statusLoading = ref(false)
const operationLogs = ref<OperationLog[]>([])
let statusInterval: number | null = null

const showManualUpdateCookieDialog = ref(false)
const showQRUpdateDialog = ref(false)
const showCaptchaGuideDialog = ref(false)
const showCredentialSection = ref(false)

const loadConnectionStatus = async (silent = false) => {
  if (!accountId.value) return
  if (!silent) statusLoading.value = true
  try {
    const response = await getConnectionStatus(accountId.value)
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
  if (!accountId.value) return
  try {
    const response = await queryOperationLogs({
      accountId: accountId.value,
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
  if (!accountId.value) return
  statusLoading.value = true
  try {
    const response = await startConnection(accountId.value)
    if (response.code === 0 || response.code === 200) {
      await loadConnectionStatus()
      ElMessageBox.alert(
        '1、请勿使用闲鱼网页版进行消息回复，不然容易触发风控；<br><br>2、刚开始使用会掉线且无法自动刷新的情况，属于正常，多挂几天就好了；<br><br>3、有问题可以在"系统设置"->"关于"加入交流群；',
        '连接已经建立',
        { confirmButtonText: '我知道了', type: 'success', dangerouslyUseHTMLString: true }
      )
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
  if (!accountId.value) return
  if (!confirm('断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？')) return

  statusLoading.value = true
  try {
    const response = await stopConnection(accountId.value)
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

const handleBack = () => {
  router.push('/connection')
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

const h5Token = computed(() => connectionStatus.value?.mH5Tk || connectionStatus.value?.mh5Tk)

const getMH5TkStatusText = (mH5Tk?: string) => {
  if (!mH5Tk) return '未设置'
  return '有效'
}

const getMH5TkStatusColor = (mH5Tk?: string) => {
  if (!mH5Tk) return 'rgba(28,28,30,.55)'
  return '#30D158'
}

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

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    showSuccess('已复制到剪贴板')
  }).catch(() => {
    showError('复制失败')
  })
}

watch(accountId, (newId) => {
  if (newId) {
    loadConnectionStatus()
    loadOperationLogs()
    if (statusInterval) clearInterval(statusInterval)
    statusInterval = window.setInterval(() => {
      if (accountId.value) {
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
  <!-- Desktop: reuse desktop component -->
  <DesktopDetail v-if="!isMobile" :account-id="accountId" />

  <!-- Mobile: custom native-style page -->
  <div v-else class="page">
    <header class="page__header">
      <button class="page__back" @click="handleBack">
        <IconChevronLeft />
        <span>返回</span>
      </button>
      <h1 class="page__title">连接详情</h1>
      <button class="page__action" @click="handleRefresh" :disabled="statusLoading">
        <IconRefresh />
      </button>
    </header>

    <div class="page__scroll" :class="{ 'page__scroll--loading': statusLoading }">
      <div v-if="connectionStatus" class="page__body">
        <div class="status-hero" :class="connectionStatus.connected ? 'status-hero--on' : 'status-hero--off'">
          <div class="status-hero__icon">
            <component :is="connectionStatus.connected ? IconWifi : IconWifiOff" />
          </div>
          <div class="status-hero__info">
            <span class="status-hero__title">{{ connectionStatus.connected ? '已连接' : '未连接' }}</span>
            <span class="status-hero__sub">账号 ID: {{ connectionStatus.xianyuAccountId }}</span>
          </div>
          <span class="status-hero__badge">{{ connectionStatus.status }}</span>
        </div>

        <div class="cap-section">
          <div class="cap-card" :class="canSyncGoods ? 'cap-card--ok' : 'cap-card--err'">
            <div class="cap-card__dot"></div>
            <div class="cap-card__text">
              <span class="cap-card__label">同步商品信息</span>
              <span class="cap-card__desc">{{ canSyncGoods ? '可正常同步商品信息' : 'Cookie无效，无法同步' }}</span>
            </div>
          </div>
          <div class="cap-card" :class="canAutoReply ? 'cap-card--ok' : 'cap-card--err'">
            <div class="cap-card__dot"></div>
            <div class="cap-card__text">
              <span class="cap-card__label">自动发货与回复</span>
              <span class="cap-card__desc">{{ canAutoReply ? '可正常自动发货与回复' : '未连接，无法工作' }}</span>
            </div>
          </div>
        </div>

        <div class="action-row">
          <button
            v-if="connectionStatus.connected === true"
            class="act-btn act-btn--danger"
            @click="handleStopConnection"
          >
            <IconStop /><span>断开连接</span>
          </button>
          <button
            v-else
            class="act-btn act-btn--success"
            @click="handleStartConnection"
          >
            <IconPlay /><span>开始连接</span>
          </button>
        </div>

        <div class="action-row action-row--sub">
          <button class="act-btn act-btn--outline" @click="showCredentialSection = !showCredentialSection">
            <IconKey /><span>{{ showCredentialSection ? '收起凭证' : '凭证详情' }}</span>
          </button>
          <button class="act-btn act-btn--outline" @click="showQRUpdateDialog = true">
            <IconQrCode /><span>扫码更新</span>
          </button>
        </div>

        <Transition name="slide">
          <div v-if="showCredentialSection" class="cred-section">
            <div class="cred-block">
              <div class="cred-block__head">
                <div class="cred-block__left">
                  <div class="cred-block__icon cred-block__icon--cookie"><IconCookie /></div>
                  <span class="cred-block__name">Cookie</span>
                </div>
                <span class="cred-block__status" :style="{ color: getCookieStatusColor(connectionStatus.cookieStatus) }">
                  {{ getCookieStatusText(connectionStatus.cookieStatus) }}
                </span>
              </div>
              <div class="cred-block__body" v-if="connectionStatus.cookieText">
                <div class="cred-block__code">{{ connectionStatus.cookieText }}</div>
                <div class="cred-block__meta">
                  <span>{{ connectionStatus.cookieText.length }} 字符</span>
                  <button class="cred-block__copy" @click="copyToClipboard(connectionStatus.cookieText)">
                    <IconCopy />
                  </button>
                </div>
              </div>
              <div class="cred-block__empty" v-else>未设置</div>
              <div class="cred-block__foot">
                <button class="act-btn act-btn--tiny" @click="showManualUpdateCookieDialog = true">
                  <span>手动更新</span>
                </button>
              </div>
            </div>

            <div class="cred-block">
              <div class="cred-block__head">
                <div class="cred-block__left">
                  <div class="cred-block__icon cred-block__icon--token"><IconKey /></div>
                  <span class="cred-block__name">WebSocket Token</span>
                </div>
                <span class="cred-block__status" :style="{ color: getTokenStatusColor(connectionStatus.tokenExpireTime) }">
                  {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
                </span>
              </div>
              <div class="cred-block__body" v-if="connectionStatus.websocketToken">
                <div class="cred-block__code">{{ connectionStatus.websocketToken }}</div>
                <div class="cred-block__meta">
                  <span>{{ connectionStatus.websocketToken.length }} 字符</span>
                  <button class="cred-block__copy" @click="copyToClipboard(connectionStatus.websocketToken)">
                    <IconCopy />
                  </button>
                </div>
              </div>
              <div class="cred-block__empty" v-else>未设置</div>
              <div class="cred-block__foot" v-if="connectionStatus.tokenExpireTime">
                <span class="cred-block__expire">过期时间: {{ formatTimestamp(connectionStatus.tokenExpireTime) }}</span>
              </div>
            </div>

            <div class="cred-block" v-if="h5Token">
              <div class="cred-block__head">
                <div class="cred-block__left">
                  <div class="cred-block__icon cred-block__icon--h5"><IconHelp /></div>
                  <span class="cred-block__name">H5 Token</span>
                </div>
                <span class="cred-block__status" :style="{ color: getMH5TkStatusColor(h5Token) }">
                  {{ getMH5TkStatusText(h5Token) }}
                </span>
              </div>
              <div class="cred-block__body">
                <div class="cred-block__code">{{ h5Token }}</div>
                <div class="cred-block__meta">
                  <span>{{ h5Token.length }} 字符</span>
                  <button class="cred-block__copy" @click="copyToClipboard(h5Token)">
                    <IconCopy />
                  </button>
                </div>
              </div>
            </div>
          </div>
        </Transition>

        <div class="log-section">
          <div class="log-section__header">
            <div class="log-section__title"><IconLog /><span>操作日志</span></div>
          </div>
          <div class="log-container">
            <div v-for="log in operationLogs" :key="log.id" class="log-entry">
              <span class="log-entry__time">{{ formatTimestamp(log.createTime) }}</span>
              <span class="log-entry__desc">{{ log.operationDesc }}</span>
              <span class="log-entry__status" :style="{ color: getOperationStatusColor(log.operationStatus) }">
                {{ getOperationStatusText(log.operationStatus) }}
              </span>
            </div>
            <div v-if="operationLogs.length === 0" class="log-empty">暂无日志</div>
          </div>
        </div>
      </div>

      <div v-else-if="!statusLoading" class="page__empty">
        <p>加载中...</p>
      </div>
    </div>

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
.page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f5f7;
  overflow: hidden;
}

.page__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  position: sticky;
  top: 0;
  z-index: 10;
}

.page__back {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 16px;
  font-weight: 500;
  color: #0A84FF;
  cursor: pointer;
  background: none;
  border: none;
  padding: 4px 0;
  -webkit-tap-highlight-color: transparent;
}

.page__back svg { width: 22px; height: 22px; }

.page__title {
  flex: 1;
  font-size: 17px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
  letter-spacing: -0.01em;
}

.page__action {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.05);
  border: none;
  border-radius: 10px;
  color: #0A84FF;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.page__action:disabled { opacity: 0.4; }
.page__action svg { width: 18px; height: 18px; }

.page__scroll {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}

.page__scroll::-webkit-scrollbar { display: none; }
.page__scroll--loading { opacity: 0.5; pointer-events: none; }

.page__body {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 16px;
  padding-bottom: 32px;
}

.page__empty {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(28,28,30,.55);
  font-size: 14px;
}

/* Status Hero */
.status-hero {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
  border-radius: 16px;
  border: 1px solid;
}

.status-hero--on {
  background: rgba(52, 199, 89, 0.08);
  border-color: rgba(52, 199, 89, 0.2);
}

.status-hero--off {
  background: rgba(255, 59, 48, 0.08);
  border-color: rgba(255, 59, 48, 0.2);
}

.status-hero__icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.status-hero--on .status-hero__icon {
  background: rgba(52, 199, 89, 0.15);
  color: #30D158;
}

.status-hero--off .status-hero__icon {
  background: rgba(255, 59, 48, 0.15);
  color: #FF453A;
}

.status-hero__icon svg { width: 22px; height: 22px; }

.status-hero__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.status-hero__title {
  font-size: 17px;
  font-weight: 600;
  color: #1c1c1e;
}

.status-hero__sub {
  font-size: 12px;
  color: rgba(28,28,30,.55);
}

.status-hero__badge {
  font-size: 11px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.05);
  color: rgba(28,28,30,.55);
  flex-shrink: 0;
}

/* Capability Cards */
.cap-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.cap-card {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 14px;
  border: 1px solid;
}

.cap-card--ok {
  background: rgba(52, 199, 89, 0.06);
  border-color: rgba(52, 199, 89, 0.2);
}

.cap-card--err {
  background: rgba(255, 59, 48, 0.06);
  border-color: rgba(255, 59, 48, 0.2);
}

.cap-card__dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.cap-card--ok .cap-card__dot { background: #30D158; }
.cap-card--err .cap-card__dot { background: #FF453A; }

.cap-card__text {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.cap-card__label {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
}

.cap-card--ok .cap-card__label { color: #30D158; }
.cap-card--err .cap-card__label { color: #FF453A; }

.cap-card__desc {
  font-size: 12px;
  color: rgba(28,28,30,.55);
  line-height: 1.3;
}

/* Action Buttons */
.action-row {
  display: flex;
  gap: 10px;
}

.action-row--sub {
  margin-top: -6px;
}

.act-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 16px;
  font-size: 15px;
  font-weight: 600;
  border-radius: 12px;
  border: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  letter-spacing: -0.01em;
  transition: all 0.15s ease;
  flex: 1;
}

.act-btn svg { width: 16px; height: 16px; }

.act-btn--success {
  background: #30D158;
  color: white;
  box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
}

.act-btn--danger {
  background: #FF453A;
  color: white;
  box-shadow: 0 4px 12px rgba(255, 59, 48, 0.3);
}

.act-btn--outline {
  background: rgba(0, 122, 255, 0.08);
  color: #0A84FF;
  border: 1px solid rgba(0, 122, 255, 0.2);
}

.act-btn--tiny {
  padding: 6px 12px;
  font-size: 13px;
  font-weight: 500;
  background: rgba(0, 122, 255, 0.08);
  color: #0A84FF;
  border-radius: 8px;
  flex: 0;
}

.act-btn:active { transform: scale(0.97); }

/* Credential Section */
.cred-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cred-block {
  background: white;
  border-radius: 14px;
  border: 1px solid rgba(60,60,67,.12);
  overflow: hidden;
}

.cred-block__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.cred-block__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.cred-block__icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.cred-block__icon svg { width: 16px; height: 16px; }

.cred-block__icon--cookie {
  background: rgba(255, 149, 0, 0.12);
  color: #FF9F0A;
}

.cred-block__icon--token {
  background: rgba(52, 199, 89, 0.12);
  color: #30D158;
}

.cred-block__icon--h5 {
  background: rgba(0, 122, 255, 0.12);
  color: #0A84FF;
}

.cred-block__name {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
}

.cred-block__status {
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 8px;
  background: rgba(0, 0, 0, 0.05);
}

.cred-block__body {
  padding: 12px 16px;
}

.cred-block__code {
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  font-size: 11px;
  color: rgba(28,28,30,.55);
  line-height: 1.5;
  word-break: break-all;
  background: rgba(0, 0, 0, 0.03);
  padding: 10px;
  border-radius: 8px;
  max-height: 120px;
  overflow-y: auto;
  scrollbar-width: none;
}

.cred-block__code::-webkit-scrollbar { display: none; }

.cred-block__meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
  font-size: 11px;
  color: rgba(28,28,30,.55);
}

.cred-block__copy {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 122, 255, 0.08);
  border: none;
  border-radius: 8px;
  color: #0A84FF;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.cred-block__copy svg { width: 14px; height: 14px; }

.cred-block__empty {
  padding: 14px 16px;
  font-size: 13px;
  color: rgba(28,28,30,.55);
  font-style: italic;
}

.cred-block__foot {
  padding: 10px 16px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
}

.cred-block__expire {
  font-size: 12px;
  color: rgba(28,28,30,.55);
}

/* Log Section */
.log-section {
  margin-top: 4px;
}

.log-section__header {
  margin-bottom: 10px;
}

.log-section__title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
}

.log-section__title svg { width: 16px; height: 16px; color: rgba(28,28,30,.55); }

.log-container {
  background: #1c1c1e;
  border-radius: 12px;
  padding: 12px;
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
  scrollbar-width: none;
}

.log-container::-webkit-scrollbar { display: none; }

.log-entry {
  display: flex;
  gap: 8px;
  margin-bottom: 6px;
  line-height: 1.5;
}

.log-entry:last-child { margin-bottom: 0; }

.log-entry__time {
  color: rgba(28,28,30,.55);
  font-size: 11px;
  flex-shrink: 0;
}

.log-entry__desc {
  color: #e5e5e7;
  flex: 1;
  min-width: 0;
  word-break: break-all;
}

.log-entry__status {
  flex-shrink: 0;
  font-weight: 500;
}

.log-empty {
  text-align: center;
  color: rgba(28,28,30,.55);
  padding: 16px;
  font-size: 12px;
}

/* Transition */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.25s ease;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* Small screens */
@media screen and (max-width: 380px) {
  .page__body {
    padding: 12px;
    gap: 12px;
  }

  .status-hero { padding: 14px 12px; }
  .cap-card { padding: 12px; }
  .act-btn { padding: 10px 14px; font-size: 14px; }
}
</style>
