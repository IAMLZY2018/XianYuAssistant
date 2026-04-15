<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
import { ElMessageBox } from 'element-plus'
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket'
import { showSuccess, showError, showInfo } from '@/utils'
import ManualUpdateCookieDialog from './ManualUpdateCookieDialog.vue'
import ManualUpdateTokenDialog from './ManualUpdateTokenDialog.vue'
import QRUpdateDialog from './QRUpdateDialog.vue'

import IconWifi from '@/components/icons/IconWifi.vue'
import IconWifiOff from '@/components/icons/IconWifiOff.vue'
import IconCookie from '@/components/icons/IconCookie.vue'
import IconKey from '@/components/icons/IconKey.vue'
import IconPlay from '@/components/icons/IconPlay.vue'
import IconStop from '@/components/icons/IconStop.vue'
import IconQrCode from '@/components/icons/IconQrCode.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconHelp from '@/components/icons/IconHelp.vue'
import IconEdit from '@/components/icons/IconEdit.vue'
import IconLog from '@/components/icons/IconLog.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconAlert from '@/components/icons/IconAlert.vue'
import IconShield from '@/components/icons/IconShield.vue'

interface ConnectionStatus {
  xianyuAccountId: number
  connected: boolean
  status: string
  cookieStatus?: number
  cookieText?: string
  websocketToken?: string
  tokenExpireTime?: number
}

interface Props {
  accountId: number | null
  isMobile?: boolean
}

interface Emits {
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const connectionStatus = ref<ConnectionStatus | null>(null)
const statusLoading = ref(false)
const logs = ref<Array<{ time: string; message: string; isError?: boolean }>>([])
let statusInterval: number | null = null

const showManualUpdateCookieDialog = ref(false)
const showManualUpdateTokenDialog = ref(false)
const showQRUpdateDialog = ref(false)

// Mobile detail as overlay
const showMobileDetail = ref(false)

// Load connection status
const loadConnectionStatus = async (silent = false) => {
  if (!props.accountId) return
  if (!silent) statusLoading.value = true
  try {
    const response = await getConnectionStatus(props.accountId)
    if (response.code === 0 || response.code === 200) {
      connectionStatus.value = response.data as ConnectionStatus
      if (!silent) addLog('状态已更新')
    } else {
      throw new Error(response.msg || '获取连接状态失败')
    }
  } catch (error: any) {
    if (!silent) addLog('加载状态失败: ' + error.message, true)
  } finally {
    statusLoading.value = false
  }
}

// Start connection
const handleStartConnection = async () => {
  if (!props.accountId) return
  statusLoading.value = true
  addLog('正在启动连接...')
  try {
    const response = await startConnection(props.accountId)
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接启动成功')
      addLog('连接启动成功')
      await loadConnectionStatus()
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
  if (!props.accountId) return
  try {
    await ElMessageBox.confirm(
      '断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？',
      '确认断开连接',
      { confirmButtonText: '确定断开', cancelButtonText: '取消', type: 'warning' }
    )
  } catch { return }

  statusLoading.value = true
  addLog('正在断开连接...')
  try {
    const response = await stopConnection(props.accountId)
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接已断开')
      addLog('连接已断开')
      await loadConnectionStatus()
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
  loadConnectionStatus()
  showInfo('状态已刷新')
}

// Add log
const addLog = (message: string, isError = false) => {
  const time = new Date().toLocaleTimeString()
  logs.value.push({ time, message, isError })
  if (logs.value.length > 50) logs.value.shift()
}

// Cookie help
const showCookieHelp = () => {
  ElMessageBox({
    title: '如何获取Cookie',
    message: `
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取Cookie：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问闲鱼网站并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>刷新页面</li>
          <li>在请求列表中找到任意请求</li>
          <li>在请求头中找到Cookie字段</li>
          <li>复制完整的Cookie值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img src="/cookieGet.png" class="cookie-help-image" alt="Cookie获取示例"
            onerror="this.style.display='none'" onclick="window.open('/cookieGet.png','_blank')" title="点击查看大图" />
        </div>
        <p style="margin-top: 12px; color: #86868b; font-size: 12px; text-align: center;">点击图片可查看大图</p>
        <p style="margin-top: 8px; color: #ff3b30; font-size: 12px; text-align: center;">Cookie包含敏感信息，请勿泄露给他人</p>
      </div>`,
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了',
    customClass: 'cookie-help-dialog'
  })
}

// Token help
const showTokenHelp = () => {
  ElMessageBox({
    title: '如何获取WebSocket Token',
    message: `
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取WebSocket Token：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问<a href="https://www.goofish.com/im" target="_blank" style="color: #007aff;">闲鱼IM页面</a>并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>在页面中进行任意操作（如点击聊天）</li>
          <li>在请求列表中找到WebSocket连接请求</li>
          <li>查看请求参数或响应中的Token信息</li>
          <li>复制完整的Token值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img src="/tokenGet.png" class="token-help-image" alt="Token获取示例"
            onerror="this.style.display='none'" onclick="window.open('/tokenGet.png','_blank')" title="点击查看大图" />
        </div>
        <p style="margin-top: 12px; color: #86868b; font-size: 12px; text-align: center;">点击图片可查看大图</p>
        <p style="margin-top: 8px; color: #ff3b30; font-size: 12px; text-align: center;">Token包含敏感信息，请勿泄露给他人</p>
      </div>`,
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了',
    customClass: 'token-help-dialog'
  })
}

// Callbacks
const handleManualUpdateCookieSuccess = async () => {
  addLog('Cookie已手动更新')
  await loadConnectionStatus()
}

const handleManualUpdateTokenSuccess = async () => {
  addLog('Token已手动更新')
  await loadConnectionStatus()
}

const handleQRUpdateSuccess = async () => {
  addLog('Cookie和Token已通过扫码更新')
  await loadConnectionStatus()
}

// Helpers
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

const getCookieStatusBg = (status?: number) => {
  if (status === 1) return 'rgba(52, 199, 89, 0.1)'
  if (status === 2) return 'rgba(255, 149, 0, 0.1)'
  if (status === 3) return 'rgba(255, 59, 48, 0.1)'
  return 'rgba(134, 134, 139, 0.1)'
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

const getTokenStatusColor = (timestamp?: number) => {
  if (!timestamp) return '#86868b'
  return isTokenExpired(timestamp) ? '#ff3b30' : '#34c759'
}

const getTokenStatusBg = (timestamp?: number) => {
  if (!timestamp) return 'rgba(134, 134, 139, 0.1)'
  return isTokenExpired(timestamp) ? 'rgba(255, 59, 48, 0.1)' : 'rgba(52, 199, 89, 0.1)'
}

// Watch accountId changes
watch(() => props.accountId, (newId) => {
  if (newId) {
    loadConnectionStatus()
    if (statusInterval) clearInterval(statusInterval)
    statusInterval = window.setInterval(() => {
      if (props.accountId) loadConnectionStatus(true)
    }, 5000)
    // Show mobile detail
    if (props.isMobile) showMobileDetail.value = true
  } else {
    connectionStatus.value = null
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
  <!-- Desktop: Inline detail panel -->
  <div v-if="!isMobile" class="detail-panel">
    <!-- No selection -->
    <div v-if="!accountId" class="detail-empty">
      <div class="detail-empty__icon"><IconLink /></div>
      <p class="detail-empty__text">请选择一个账号查看连接状态</p>
    </div>

    <!-- Detail content -->
    <div v-else class="detail-scroll" :class="{ 'detail-scroll--loading': statusLoading }">
      <div v-if="connectionStatus" class="detail-body">
        <!-- Status Header -->
        <div class="status-header">
          <div class="status-header__left">
            <div
              class="status-icon"
              :class="connectionStatus.connected ? 'status-icon--on' : 'status-icon--off'"
            >
              <component :is="connectionStatus.connected ? IconWifi : IconWifiOff" />
            </div>
            <div class="status-header__info">
              <span class="status-header__title">连接状态</span>
              <span class="status-header__sub">账号 ID: {{ connectionStatus.xianyuAccountId }}</span>
            </div>
          </div>
          <span
            class="status-badge"
            :class="connectionStatus.connected ? 'status-badge--on' : 'status-badge--off'"
          >
            <component :is="connectionStatus.connected ? IconCheck : IconAlert" />
            {{ connectionStatus.connected ? '已连接' : '未连接' }}
          </span>
        </div>

        <!-- Status Message -->
        <div
          class="status-message"
          :class="connectionStatus.connected ? 'status-message--on' : 'status-message--off'"
        >
          <component :is="connectionStatus.connected ? IconCheck : IconAlert" />
          <span>{{ connectionStatus.connected ? '已连接到闲鱼服务器' : '当前未连接，无法监听消息和执行自动化流程' }}</span>
        </div>

        <!-- Info Grid -->
        <div class="info-grid">
          <!-- Cookie Section -->
          <div class="info-card">
            <div class="info-card__header">
              <div class="info-card__icon info-card__icon--cookie"><IconCookie /></div>
              <div class="info-card__title-group">
                <span class="info-card__title">Cookie 凭证</span>
                <span class="info-card__note">用于识别账号身份</span>
              </div>
              <span
                class="info-card__status"
                :style="{
                  color: getCookieStatusColor(connectionStatus.cookieStatus),
                  background: getCookieStatusBg(connectionStatus.cookieStatus)
                }"
              >
                {{ getCookieStatusText(connectionStatus.cookieStatus) }}
              </span>
            </div>
            <div class="info-card__body">
              <div class="info-box">
                <div class="info-box__label">Cookie 内容</div>
                <div class="info-box__value">{{ connectionStatus.cookieText || '未获取到Cookie' }}</div>
                <div class="info-box__meta" v-if="connectionStatus.cookieText">
                  {{ connectionStatus.cookieText.length }} 字符
                </div>
              </div>
              <div class="info-card__actions">
                <button class="btn btn--secondary" @click="showManualUpdateCookieDialog = true">
                  <IconEdit /><span>手动更新</span>
                </button>
                <button class="btn btn--ghost" @click="showCookieHelp">
                  <IconHelp /><span>获取帮助</span>
                </button>
              </div>
            </div>
          </div>

          <!-- Token Section -->
          <div class="info-card">
            <div class="info-card__header">
              <div class="info-card__icon info-card__icon--token"><IconKey /></div>
              <div class="info-card__title-group">
                <span class="info-card__title">WebSocket Token</span>
                <span class="info-card__note">消息收取凭证</span>
              </div>
              <span
                class="info-card__status"
                :style="{
                  color: getTokenStatusColor(connectionStatus.tokenExpireTime),
                  background: getTokenStatusBg(connectionStatus.tokenExpireTime)
                }"
              >
                {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
              </span>
            </div>
            <div class="info-card__body">
              <div class="info-box">
                <div class="info-box__label">
                  <IconClock />
                  过期时间
                </div>
                <div class="info-box__value info-box__value--time">{{ formatTimestamp(connectionStatus.tokenExpireTime) }}</div>
              </div>
              <div class="info-box">
                <div class="info-box__label">Token 内容</div>
                <div class="info-box__value">{{ connectionStatus.websocketToken || '未获取到Token' }}</div>
                <div class="info-box__meta" v-if="connectionStatus.websocketToken">
                  {{ connectionStatus.websocketToken.length }} 字符
                </div>
              </div>
              <div class="info-card__actions">
                <button class="btn btn--secondary" @click="showManualUpdateTokenDialog = true">
                  <IconEdit /><span>手动更新</span>
                </button>
                <button class="btn btn--ghost" @click="showTokenHelp">
                  <IconHelp /><span>获取帮助</span>
                </button>
              </div>
            </div>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-bar">
          <button
            v-if="connectionStatus.connected"
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
            <IconPlay /><span>启动连接</span>
          </button>
          <button class="btn btn--qr" @click="showQRUpdateDialog = true">
            <IconQrCode /><span>扫码更新</span>
          </button>
          <button class="btn btn--secondary" @click="handleRefresh" :disabled="statusLoading">
            <IconRefresh /><span>刷新</span>
          </button>
        </div>

        <div class="action-tip">
          请勿频繁启用/断开连接，否则容易触发人机校验导致账号暂时不可用
        </div>

        <!-- Logs -->
        <div class="log-section">
          <div class="log-section__header">
            <div class="log-section__title">
              <IconLog />
              <span>操作日志</span>
            </div>
          </div>
          <div class="log-container">
            <div
              v-for="(log, i) in logs"
              :key="i"
              class="log-entry"
              :class="{ 'log-entry--error': log.isError }"
            >
              <span class="log-entry__time">{{ log.time }}</span>
              <span class="log-entry__msg">{{ log.message }}</span>
            </div>
            <div v-if="logs.length === 0" class="log-empty">暂无日志记录</div>
          </div>
        </div>
      </div>
    </div>

    <!-- Dialogs -->
    <ManualUpdateCookieDialog
      v-if="connectionStatus"
      v-model="showManualUpdateCookieDialog"
      :account-id="accountId || 0"
      :current-cookie="connectionStatus.cookieText || ''"
      @success="handleManualUpdateCookieSuccess"
    />
    <ManualUpdateTokenDialog
      v-if="connectionStatus"
      v-model="showManualUpdateTokenDialog"
      :account-id="accountId || 0"
      :current-token="connectionStatus.websocketToken || ''"
      @success="handleManualUpdateTokenSuccess"
    />
    <QRUpdateDialog
      v-model="showQRUpdateDialog"
      :account-id="accountId || 0"
      @success="handleQRUpdateSuccess"
    />
  </div>

  <!-- Mobile: Overlay Detail -->
  <template v-else>
    <Transition name="slide-up">
      <div v-if="showMobileDetail && accountId" class="mobile-overlay">
        <div class="mobile-overlay__header">
          <button class="mobile-overlay__back" @click="showMobileDetail = false">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
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
            <!-- Same content structure as desktop -->
            <div class="status-header">
              <div class="status-header__left">
                <div class="status-icon" :class="connectionStatus.connected ? 'status-icon--on' : 'status-icon--off'">
                  <component :is="connectionStatus.connected ? IconWifi : IconWifiOff" />
                </div>
                <div class="status-header__info">
                  <span class="status-header__title">连接状态</span>
                  <span class="status-header__sub">账号 ID: {{ connectionStatus.xianyuAccountId }}</span>
                </div>
              </div>
              <span class="status-badge" :class="connectionStatus.connected ? 'status-badge--on' : 'status-badge--off'">
                <component :is="connectionStatus.connected ? IconCheck : IconAlert" />
                {{ connectionStatus.connected ? '已连接' : '未连接' }}
              </span>
            </div>

            <div class="status-message" :class="connectionStatus.connected ? 'status-message--on' : 'status-message--off'">
              <component :is="connectionStatus.connected ? IconCheck : IconAlert" />
              <span>{{ connectionStatus.connected ? '已连接到闲鱼服务器' : '当前未连接，无法监听消息和执行自动化流程' }}</span>
            </div>

            <div class="info-grid info-grid--mobile">
              <div class="info-card">
                <div class="info-card__header">
                  <div class="info-card__icon info-card__icon--cookie"><IconCookie /></div>
                  <div class="info-card__title-group">
                    <span class="info-card__title">Cookie 凭证</span>
                    <span class="info-card__note">用于识别账号身份</span>
                  </div>
                  <span class="info-card__status" :style="{ color: getCookieStatusColor(connectionStatus.cookieStatus), background: getCookieStatusBg(connectionStatus.cookieStatus) }">
                    {{ getCookieStatusText(connectionStatus.cookieStatus) }}
                  </span>
                </div>
                <div class="info-card__body">
                  <div class="info-box">
                    <div class="info-box__label">Cookie 内容</div>
                    <div class="info-box__value">{{ connectionStatus.cookieText || '未获取到Cookie' }}</div>
                    <div class="info-box__meta" v-if="connectionStatus.cookieText">{{ connectionStatus.cookieText.length }} 字符</div>
                  </div>
                  <div class="info-card__actions">
                    <button class="btn btn--secondary" @click="showManualUpdateCookieDialog = true"><IconEdit /><span>手动更新</span></button>
                    <button class="btn btn--ghost" @click="showCookieHelp"><IconHelp /><span>获取帮助</span></button>
                  </div>
                </div>
              </div>

              <div class="info-card">
                <div class="info-card__header">
                  <div class="info-card__icon info-card__icon--token"><IconKey /></div>
                  <div class="info-card__title-group">
                    <span class="info-card__title">WebSocket Token</span>
                    <span class="info-card__note">消息收取凭证</span>
                  </div>
                  <span class="info-card__status" :style="{ color: getTokenStatusColor(connectionStatus.tokenExpireTime), background: getTokenStatusBg(connectionStatus.tokenExpireTime) }">
                    {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
                  </span>
                </div>
                <div class="info-card__body">
                  <div class="info-box">
                    <div class="info-box__label"><IconClock /> 过期时间</div>
                    <div class="info-box__value info-box__value--time">{{ formatTimestamp(connectionStatus.tokenExpireTime) }}</div>
                  </div>
                  <div class="info-box">
                    <div class="info-box__label">Token 内容</div>
                    <div class="info-box__value">{{ connectionStatus.websocketToken || '未获取到Token' }}</div>
                    <div class="info-box__meta" v-if="connectionStatus.websocketToken">{{ connectionStatus.websocketToken.length }} 字符</div>
                  </div>
                  <div class="info-card__actions">
                    <button class="btn btn--secondary" @click="showManualUpdateTokenDialog = true"><IconEdit /><span>手动更新</span></button>
                    <button class="btn btn--ghost" @click="showTokenHelp"><IconHelp /><span>获取帮助</span></button>
                  </div>
                </div>
              </div>
            </div>

            <div class="action-bar action-bar--mobile">
              <button v-if="connectionStatus.connected" class="btn btn--stop" @click="handleStopConnection"><IconStop /><span>断开连接</span></button>
              <button v-else class="btn btn--start" @click="handleStartConnection"><IconPlay /><span>启动连接</span></button>
              <button class="btn btn--qr" @click="showQRUpdateDialog = true"><IconQrCode /><span>扫码更新</span></button>
            </div>

            <div class="action-tip">请勿频繁启用/断开连接，否则容易触发人机校验导致账号暂时不可用</div>

            <div class="log-section">
              <div class="log-section__header">
                <div class="log-section__title"><IconLog /><span>操作日志</span></div>
              </div>
              <div class="log-container">
                <div v-for="(log, i) in logs" :key="i" class="log-entry" :class="{ 'log-entry--error': log.isError }">
                  <span class="log-entry__time">{{ log.time }}</span>
                  <span class="log-entry__msg">{{ log.message }}</span>
                </div>
                <div v-if="logs.length === 0" class="log-empty">暂无日志记录</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Dialogs (mobile) -->
    <ManualUpdateCookieDialog
      v-if="connectionStatus"
      v-model="showManualUpdateCookieDialog"
      :account-id="accountId || 0"
      :current-cookie="connectionStatus.cookieText || ''"
      @success="handleManualUpdateCookieSuccess"
    />
    <ManualUpdateTokenDialog
      v-if="connectionStatus"
      v-model="showManualUpdateTokenDialog"
      :account-id="accountId || 0"
      :current-token="connectionStatus.websocketToken || ''"
      @success="handleManualUpdateTokenSuccess"
    />
    <QRUpdateDialog
      v-model="showQRUpdateDialog"
      :account-id="accountId || 0"
      @success="handleQRUpdateSuccess"
    />
  </template>
</template>

<style scoped>
/* ============================================================
   Design Tokens
   ============================================================ */
.detail-panel,
.mobile-overlay {
  --c-bg: transparent;
  --c-surface: #ffffff;
  --c-border: rgba(0, 0, 0, 0.06);
  --c-border-strong: rgba(0, 0, 0, 0.1);
  --c-text-1: #1d1d1f;
  --c-text-2: #6e6e73;
  --c-text-3: #86868b;
  --c-accent: #007aff;
  --c-danger: #ff3b30;
  --c-success: #34c759;
  --c-warning: #ff9500;
  --c-r-sm: 8px;
  --c-r-md: 12px;
  --c-r-lg: 16px;
  --c-ease: 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  --c-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* ============================================================
   Desktop Detail Panel
   ============================================================ */
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
  display: flex;
  align-items: center;
  justify-content: center;
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
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.12) transparent;
}

.detail-scroll::-webkit-scrollbar {
  width: 6px;
}

.detail-scroll::-webkit-scrollbar-track {
  background: transparent;
}

.detail-scroll::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
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

/* --- Status Header --- */
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
  min-width: 0;
}

.status-icon {
  width: 40px;
  height: 40px;
  border-radius: var(--c-r-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
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
  min-width: 0;
}

.status-header__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text-1);
  line-height: 1.3;
}

.status-header__sub {
  font-size: 12px;
  color: var(--c-text-3);
  line-height: 1.3;
  margin-top: 2px;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 20px;
  flex-shrink: 0;
  line-height: 1;
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

/* --- Status Message --- */
.status-message {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
  font-weight: 500;
  padding: 8px 12px;
  border-radius: var(--c-r-sm);
  line-height: 1.4;
}

.status-message svg {
  width: 14px;
  height: 14px;
  flex-shrink: 0;
}

.status-message--on {
  color: var(--c-success);
  background: rgba(52, 199, 89, 0.06);
  border: 1px solid rgba(52, 199, 89, 0.12);
}

.status-message--off {
  color: var(--c-danger);
  background: rgba(255, 59, 48, 0.06);
  border: 1px solid rgba(255, 59, 48, 0.12);
}

/* --- Info Grid --- */
.info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.info-grid--mobile {
  grid-template-columns: 1fr;
}

.info-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border);
  border-radius: var(--c-r-md);
  overflow: hidden;
  box-shadow: var(--c-shadow);
}

.info-card__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--c-border);
}

.info-card__icon {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.info-card__icon svg {
  width: 14px;
  height: 14px;
}

.info-card__icon--cookie {
  background: rgba(255, 149, 0, 0.1);
  color: var(--c-warning);
}

.info-card__icon--token {
  background: rgba(52, 199, 89, 0.1);
  color: var(--c-success);
}

.info-card__title-group {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.info-card__title {
  font-size: 13px;
  font-weight: 600;
  color: var(--c-text-1);
  line-height: 1.3;
}

.info-card__note {
  font-size: 11px;
  color: var(--c-text-3);
  line-height: 1.3;
  margin-top: 1px;
}

.info-card__status {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  flex-shrink: 0;
  line-height: 1;
}

.info-card__body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
}

/* --- Info Box --- */
.info-box {
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid var(--c-border);
  border-radius: var(--c-r-sm);
  padding: 8px 10px;
}

.info-box__label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 10px;
  font-weight: 600;
  color: var(--c-text-3);
  text-transform: uppercase;
  letter-spacing: 0.3px;
  margin-bottom: 4px;
}

.info-box__label svg {
  width: 10px;
  height: 10px;
}

.info-box__value {
  font-family: 'SF Mono', 'Menlo', 'Courier New', monospace;
  font-size: 10px;
  color: var(--c-text-2);
  line-height: 1.5;
  word-break: break-all;
  max-height: 60px;
  overflow-y: auto;
  scrollbar-width: none;
}

.info-box__value::-webkit-scrollbar {
  display: none;
}

.info-box__value--time {
  font-family: inherit;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-1);
}

.info-box__meta {
  font-size: 10px;
  color: var(--c-text-3);
  margin-top: 4px;
  text-align: right;
}

/* --- Card Actions --- */
.info-card__actions {
  display: flex;
  gap: 8px;
}

.info-card__actions .btn {
  flex: 1;
  height: 32px;
  font-size: 12px;
}

.info-card__actions .btn svg {
  width: 14px;
  height: 14px;
}

/* --- Buttons --- */
.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 36px;
  padding: 0 14px;
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--c-r-sm);
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--c-ease);
  white-space: nowrap;
  min-width: 40px;
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.btn svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
}

.btn--secondary {
  color: var(--c-text-1);
  background: rgba(0, 0, 0, 0.03);
  border-color: var(--c-border-strong);
}

@media (hover: hover) {
  .btn--secondary:hover {
    background: rgba(0, 0, 0, 0.06);
  }
}

.btn--ghost {
  color: var(--c-accent);
}

@media (hover: hover) {
  .btn--ghost:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

.btn--start {
  color: #fff;
  background: var(--c-success);
  border-color: var(--c-success);
}

@media (hover: hover) {
  .btn--start:hover {
    background: #2db84e;
  }
}

.btn--stop {
  color: #fff;
  background: var(--c-danger);
  border-color: var(--c-danger);
}

@media (hover: hover) {
  .btn--stop:hover {
    background: #e0332a;
  }
}

.btn--qr {
  color: #fff;
  background: var(--c-accent);
  border-color: var(--c-accent);
}

@media (hover: hover) {
  .btn--qr:hover {
    background: #0066d6;
  }
}

.btn:active {
  transform: scale(0.97);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* --- Action Bar --- */
.action-bar {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.action-bar--mobile {
  flex-direction: row;
}

.action-bar .btn {
  flex: 1;
  min-width: 100px;
}

.action-tip {
  font-size: 11px;
  color: var(--c-text-3);
  text-align: center;
  line-height: 1.5;
  padding: 0 8px;
}

/* --- Log Section --- */
.log-section {
  margin-top: 4px;
}

.log-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
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
  background: rgba(28, 28, 30, 0.92);
  color: rgba(255, 255, 255, 0.85);
  border-radius: var(--c-r-sm);
  padding: 10px 12px;
  font-family: 'SF Mono', 'Menlo', 'Courier New', monospace;
  font-size: 11px;
  max-height: 160px;
  overflow-y: auto;
  scrollbar-width: none;
}

.log-container::-webkit-scrollbar {
  display: none;
}

.log-entry {
  display: flex;
  gap: 6px;
  padding: 2px 0;
  line-height: 1.5;
}

.log-entry__time {
  color: rgba(255, 255, 255, 0.4);
  font-size: 10px;
  flex-shrink: 0;
}

.log-entry__msg {
  color: rgba(255, 255, 255, 0.85);
}

.log-entry--error .log-entry__msg {
  color: #ff6b6b;
}

.log-empty {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  padding: 12px;
  font-size: 12px;
}

/* ============================================================
   Mobile Overlay
   ============================================================ */
.mobile-overlay {
  position: fixed;
  inset: 0;
  z-index: 100;
  background: var(--c-bg);
  display: flex;
  flex-direction: column;
}

.mobile-overlay__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid var(--c-border);
  background: var(--c-surface);
  flex-shrink: 0;
}

.mobile-overlay__back {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
  color: var(--c-accent);
  cursor: pointer;
  background: none;
  border: none;
  padding: 4px;
  -webkit-tap-highlight-color: transparent;
}

.mobile-overlay__back svg {
  width: 20px;
  height: 20px;
}

.mobile-overlay__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text-1);
}

.mobile-overlay__refresh {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-accent);
  cursor: pointer;
  background: none;
  border: none;
  border-radius: 50%;
  -webkit-tap-highlight-color: transparent;
}

.mobile-overlay__refresh svg {
  width: 18px;
  height: 18px;
}

.mobile-overlay__scroll {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s var(--c-ease), opacity 0.3s;
}

.slide-up-enter-from {
  transform: translateY(100%);
  opacity: 0;
}

.slide-up-leave-to {
  transform: translateY(100%);
  opacity: 0;
}

/* ============================================================
   Responsive
   ============================================================ */
@media screen and (max-width: 768px) {
  .info-grid {
    grid-template-columns: 1fr;
  }

  .status-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

@media screen and (max-width: 480px) {
  .detail-body {
    padding: 12px;
    gap: 12px;
  }

  .action-bar {
    flex-direction: column;
  }

  .action-bar .btn {
    width: 100%;
  }
}
</style>

<style>
/* Global dialog styles */
.cookie-help-dialog,
.token-help-dialog {
  max-width: 900px;
  width: 90%;
}

.cookie-help-dialog .el-message-box__message,
.token-help-dialog .el-message-box__message {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.cookie-help-dialog .el-message-box__message::-webkit-scrollbar,
.token-help-dialog .el-message-box__message::-webkit-scrollbar {
  display: none;
}

.cookie-help-dialog .cookie-help-image,
.token-help-dialog .token-help-image {
  max-width: 100%;
  max-height: 50vh;
  width: auto;
  height: auto;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  cursor: pointer;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  object-fit: contain;
  display: block;
  margin: 0 auto;
}

.captcha-guide-dialog {
  max-width: 650px;
  width: 90%;
}

.captcha-guide-dialog .el-message-box__message {
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-line;
  text-align: left;
}
</style>
