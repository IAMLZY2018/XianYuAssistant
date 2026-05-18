<script setup lang="ts">
import { ref, computed } from 'vue'
import IconCookie from '@/components/icons/IconCookie.vue'
import IconKey from '@/components/icons/IconKey.vue'
import IconQrCode from '@/components/icons/IconQrCode.vue'
import IconClose from '@/components/icons/IconClose.vue'

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
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'qr-update'): void
  (e: 'manual-update'): void
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
</script>

<template>
  <Transition name="modal-fade">
    <div v-if="modelValue" class="modal-overlay" @click="handleClose">
      <div class="modal-container" @click.stop>
        <!-- Header -->
        <div class="modal-header">
          <h2 class="modal-title">凭证详情</h2>
          <button class="modal-close" @click="handleClose">
            <IconClose />
          </button>
        </div>

        <!-- Content -->
        <div class="modal-content">
          <!-- Action Buttons -->
          <div class="action-buttons">
            <button class="btn btn--primary" @click="handleQRUpdate">
              <IconQrCode />
              <span>扫码更新</span>
            </button>
            <button class="btn btn--secondary" @click="handleManualUpdate">
              <IconCookie />
              <span>手动更新Cookie</span>
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
                {{ connectionStatus.cookieText.substring(0, 80) }}...
                <span class="credential-item__meta">{{ connectionStatus.cookieText.length }} 字符</span>
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
                {{ connectionStatus.websocketToken.substring(0, 60) }}...
                <span class="credential-item__meta">{{ connectionStatus.websocketToken.length }} 字符</span>
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
                  <span class="credential-item__name">H5 Token (_m_h5_tk)</span>
                </div>
                <span class="credential-item__status" :style="{ color: getMH5TkStatusColor(h5Token) }">
                  {{ getMH5TkStatusText(h5Token) }}
                </span>
              </div>
              <div v-if="h5Token" class="credential-item__value">
                {{ h5Token.substring(0, 60) }}...
                <span class="credential-item__meta">{{ h5Token.length }} 字符</span>
              </div>
              <div v-else class="credential-item__value credential-item__value--empty">未设置</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.20);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  animation: fadeIn 0.2s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.modal-container {
  background: rgba(255,255,255,0.72);
  border-radius: 20px;
  overflow: hidden;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
  display: flex;
  flex-direction: column;
  max-height: 85vh;
  animation: slideUp 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  flex-shrink: 0;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
  letter-spacing: -0.01em;
}

.modal-close {
  background: none;
  border: none;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(28,28,30,.55);
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
}

.modal-close:hover {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.modal-close svg {
  width: 20px;
  height: 20px;
}

.modal-content {
  flex: 1;
  overflow-y: auto;
  scrollbar-width: none;
  padding: 24px;
}

.modal-content::-webkit-scrollbar {
  display: none;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px 16px;
  font-size: 15px;
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
  width: 18px;
  height: 18px;
}

.btn--primary {
  background: #0A84FF;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.btn--primary:hover {
  background: #0066d6;
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.4);
}

.btn--primary:active {
  transform: scale(0.96);
}

.btn--secondary {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
  box-shadow: none;
}

.btn--secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn--secondary:active {
  transform: scale(0.96);
}

.credential-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.credential-item {
  background: rgba(255, 255, 255, 0.5);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 20px;
  padding: 16px;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.credential-item:hover {
  background: rgba(255, 255, 255, 0.6);
  border-color: rgba(255, 255, 255, 0.6);
}

.credential-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
}

.credential-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.credential-item__icon {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.credential-item__icon svg {
  width: 16px;
  height: 16px;
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
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
  letter-spacing: -0.01em;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.credential-item__status {
  font-size: 12px;
  font-weight: 600;
  padding: 4px 10px;
  border-radius: 8px;
  background: rgba(60,60,67,.12);
  flex-shrink: 0;
}

.credential-item__value {
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  font-size: 12px;
  color: rgba(28,28,30,.55);
  word-break: break-all;
  line-height: 1.6;
  padding: 10px;
  background: rgba(255,255,255,0.38);
  border-radius: 10px;
  border: 1px solid rgba(60,60,67,.12);
}

.credential-item__value--empty {
  color: rgba(28,28,30,.55);
  font-style: italic;
  background: rgba(255,255,255,0.15);
}

.credential-item__meta {
  display: inline-block;
  margin-left: 8px;
  color: rgba(28,28,30,.55);
  font-size: 11px;
  font-weight: 500;
}

.credential-item__expire {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 0.5px solid rgba(60,60,67,.12);
  font-size: 12px;
  color: rgba(28,28,30,.55);
}

/* 手机端适配 */
@media screen and (max-width: 767px) {
  .modal-container {
    width: 90%;
    max-height: 90vh;
    border-radius: 20px;
  }

  .modal-header {
    padding: 16px;
  }

  .modal-title {
    font-size: 16px;
  }

  .modal-content {
    padding: 16px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 10px;
    margin-bottom: 16px;
  }

  .btn {
    padding: 12px 14px;
    font-size: 14px;
  }

  .credential-item {
    padding: 12px;
  }

  .credential-item__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    margin-bottom: 10px;
    padding-bottom: 10px;
  }

  .credential-item__left {
    width: 100%;
  }

  .credential-item__status {
    align-self: flex-start;
    font-size: 11px;
    padding: 3px 8px;
  }

  .credential-item__icon {
    width: 28px;
    height: 28px;
  }

  .credential-item__icon svg {
    width: 14px;
    height: 14px;
  }

  .credential-item__name {
    font-size: 13px;
  }

  .credential-item__value {
    font-size: 11px;
    padding: 8px;
    line-height: 1.5;
  }

  .credential-item__meta {
    font-size: 10px;
  }

  .credential-item__expire {
    margin-top: 8px;
    padding-top: 8px;
    font-size: 11px;
  }
}

/* 平板端适配 */
@media screen and (min-width: 768px) and (max-width: 1023px) {
  .modal-container {
    width: 70%;
    max-height: 85vh;
  }

  .modal-content {
    padding: 20px;
  }

  .credential-item {
    padding: 14px;
  }

  .credential-item__name {
    font-size: 14px;
  }

  .credential-item__value {
    font-size: 11px;
  }
}

/* 电脑端适配 */
@media screen and (min-width: 1024px) {
  .modal-container {
    width: 60%;
    max-height: 85vh;
  }

  .modal-content {
    padding: 32px;
  }
}

.modal-fade-enter-active,
.modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.modal-fade-enter-from,
.modal-fade-leave-to {
  opacity: 0;
}
</style>
