<script setup lang="ts">
import { ref, computed } from 'vue'
import IconCookie from '@/components/icons/IconCookie.vue'
import IconKey from '@/components/icons/IconKey.vue'
import IconQrCode from '@/components/icons/IconQrCode.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'

interface ConnectionStatus {
  xianyuAccountId?: number
  connected?: boolean
  status?: string
  cookieStatus?: number
  cookieText?: string
  mH5Tk?: string
  mh5Tk?: string
  websocketToken?: string
  tokenExpireTime?: number
}

interface Props {
  modelValue: boolean
  connectionStatus: ConnectionStatus | null
  statusLoading?: boolean
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'qr-update'): void
  (e: 'manual-update'): void
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const h5Token = computed(() => props.connectionStatus?.mH5Tk || props.connectionStatus?.mh5Tk)

const getCookieStatusColor = (status?: number) => {
  if (status === 1) return '#30D158'
  if (status === 2) return '#FF9F0A'
  if (status === 3) return '#FF453A'
  return 'rgba(28,28,30,.55)'
}

const getCookieStatusText = (status?: number) => {
  if (status === 1) return '有效'
  if (status === 2) return '过期'
  if (status === 3) return '失效'
  return '未知'
}

const getTokenStatusText = (timestamp?: number) => {
  if (!timestamp) return '未设置'
  return Date.now() > timestamp ? '已过期' : '有效'
}

const getTokenStatusColor = (timestamp?: number) => {
  if (!timestamp) return 'rgba(28,28,30,.55)'
  return Date.now() > timestamp ? '#FF453A' : '#30D158'
}

const getMH5TkStatusText = (token?: string) => {
  if (!token) return '未设置'
  return '有效'
}

const getMH5TkStatusColor = (token?: string) => {
  if (!token) return 'rgba(28,28,30,.55)'
  return '#30D158'
}

const formatTimestamp = (timestamp?: number) => {
  if (!timestamp) return '未设置'
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit'
  }).replace(/\//g, '-')
}

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleQRUpdate = () => {
  emit('qr-update')
}

const handleManualUpdate = () => {
  emit('manual-update')
}

const handleRefresh = () => {
  emit('refresh')
}
</script>

<template>
  <Transition name="slide-up">
    <div v-if="modelValue" class="mobile-modal">
      <!-- Header -->
      <div class="mobile-modal__header">
        <button class="mobile-modal__back" @click="handleClose">
          <IconChevronLeft />
          <span>返回</span>
        </button>
        <h2 class="mobile-modal__title">凭证详情</h2>
        <button class="mobile-modal__refresh" @click="handleRefresh" :disabled="statusLoading">
          <IconRefresh />
        </button>
      </div>

      <!-- Content -->
      <div class="mobile-modal__content">
        <!-- Action Buttons -->
        <div class="action-buttons">
          <button class="btn btn--primary" @click="handleQRUpdate">
            <IconQrCode />
            <span>扫码更新</span>
          </button>
          <button class="btn btn--secondary" @click="handleManualUpdate">
            <IconCookie />
            <span>手动更新</span>
          </button>
        </div>

        <!-- Credential Items -->
        <div class="credential-list">
          <!-- Cookie -->
          <div class="credential-item">
            <div class="credential-item__header">
              <div class="credential-item__left">
                <div class="credential-item__icon credential-item__icon--cookie">
                  <IconCookie />
                </div>
                <span class="credential-item__name">Cookie 凭证</span>
              </div>
              <span class="credential-item__status" :style="{ color: getCookieStatusColor(connectionStatus?.cookieStatus) }">
                {{ getCookieStatusText(connectionStatus?.cookieStatus) }}
              </span>
            </div>
            <div v-if="connectionStatus?.cookieText" class="credential-item__value">
              {{ connectionStatus.cookieText.substring(0, 50) }}...
            </div>
            <div v-else class="credential-item__value credential-item__value--empty">未设置</div>
          </div>

          <!-- WebSocket Token -->
          <div class="credential-item">
            <div class="credential-item__header">
              <div class="credential-item__left">
                <div class="credential-item__icon credential-item__icon--token">
                  <IconKey />
                </div>
                <span class="credential-item__name">WebSocket Token</span>
              </div>
              <span class="credential-item__status" :style="{ color: getTokenStatusColor(connectionStatus?.tokenExpireTime) }">
                {{ getTokenStatusText(connectionStatus?.tokenExpireTime) }}
              </span>
            </div>
            <div v-if="connectionStatus?.websocketToken" class="credential-item__value">
              {{ connectionStatus.websocketToken.substring(0, 40) }}...
            </div>
            <div v-else class="credential-item__value credential-item__value--empty">未设置</div>
            <div v-if="connectionStatus?.tokenExpireTime" class="credential-item__expire">
              过期时间: {{ formatTimestamp(connectionStatus.tokenExpireTime) }}
            </div>
          </div>

          <!-- H5 Token -->
          <div class="credential-item">
            <div class="credential-item__header">
              <div class="credential-item__left">
                <div class="credential-item__icon credential-item__icon--h5">
                  <IconKey />
                </div>
                <span class="credential-item__name">H5 Token</span>
              </div>
              <span class="credential-item__status" :style="{ color: getMH5TkStatusColor(h5Token) }">
                {{ getMH5TkStatusText(h5Token) }}
              </span>
            </div>
            <div v-if="h5Token" class="credential-item__value">
              {{ h5Token.substring(0, 40) }}...
            </div>
            <div v-else class="credential-item__value credential-item__value--empty">未设置</div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.mobile-modal {
  position: fixed;
  inset: 0;
  background: #f5f5f7;
  z-index: 1000;
  display: flex;
  flex-direction: column;
  animation: slideUp 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes slideUp {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

.mobile-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border-bottom: 0.5px solid rgba(60,60,67,.12);
  flex-shrink: 0;
  gap: 12px;
}

.mobile-modal__back {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #0A84FF;
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  padding: 6px 10px;
  margin-left: -10px;
  border-radius: 8px;
  transition: background 0.2s;
  -webkit-tap-highlight-color: transparent;
  letter-spacing: -0.01em;
  flex-shrink: 0;
}

.mobile-modal__back:active {
  background: rgba(0, 122, 255, 0.1);
}

.mobile-modal__back svg {
  width: 20px;
  height: 20px;
}

.mobile-modal__title {
  font-size: 17px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
  letter-spacing: -0.01em;
  flex: 1;
  text-align: center;
}

.mobile-modal__refresh {
  background: none;
  border: none;
  color: #0A84FF;
  cursor: pointer;
  padding: 8px;
  border-radius: 8px;
  transition: background 0.2s;
  -webkit-tap-highlight-color: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.mobile-modal__refresh:active {
  background: rgba(0, 122, 255, 0.1);
}

.mobile-modal__refresh:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.mobile-modal__refresh svg {
  width: 20px;
  height: 20px;
}

.mobile-modal__content {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  padding: 16px;
}

.mobile-modal__content::-webkit-scrollbar {
  display: none;
}

.action-buttons {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 12px 14px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  -webkit-tap-highlight-color: transparent;
  letter-spacing: -0.01em;
  flex: 1;
}

.btn svg {
  width: 16px;
  height: 16px;
}

.btn--primary {
  background: #0A84FF;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.btn--primary:active {
  transform: scale(0.96);
}

.btn--secondary {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.btn--secondary:active {
  transform: scale(0.96);
}

.credential-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.credential-item {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 12px;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.credential-item:active {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 255, 255, 0.6);
}

.credential-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
  padding-bottom: 10px;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
}

.credential-item__left {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  min-width: 0;
}

.credential-item__icon {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.credential-item__icon svg {
  width: 14px;
  height: 14px;
}

.credential-item__icon--cookie {
  background: rgba(255, 149, 0, 0.15);
  color: #FF9F0A;
}

.credential-item__icon--token {
  background: rgba(52, 199, 89, 0.15);
  color: #30D158;
}

.credential-item__icon--h5 {
  background: rgba(0, 122, 255, 0.15);
  color: #0A84FF;
}

.credential-item__name {
  font-size: 13px;
  font-weight: 600;
  color: #1c1c1e;
  letter-spacing: -0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credential-item__status {
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 6px;
  background: rgba(60,60,67,.12);
  flex-shrink: 0;
}

.credential-item__value {
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  font-size: 11px;
  color: rgba(28,28,30,.55);
  word-break: break-all;
  line-height: 1.5;
  padding: 8px;
  background: rgba(255,255,255,0.38);
  border-radius: 8px;
  border: 1px solid rgba(60,60,67,.12);
}

.credential-item__value--empty {
  color: rgba(28,28,30,.55);
  font-style: italic;
  background: rgba(255,255,255,0.15);
}

.credential-item__expire {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 0.5px solid rgba(60,60,67,.12);
  font-size: 11px;
  color: rgba(28,28,30,.55);
}

.slide-up-enter-active,
.slide-up-leave-active {
  transition: transform 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.slide-up-enter-from {
  transform: translateY(100%);
}

.slide-up-leave-to {
  transform: translateY(100%);
}

/* 桌面端隐藏 */
@media screen and (min-width: 768px) {
  .mobile-modal {
    display: none !important;
  }
}
</style>
