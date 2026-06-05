<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateQRCode, getQRCodeStatus, getQRCodeCookies } from '@/api/qrlogin'
import { updateCookie, updateToken, startConnection } from '@/api/websocket'
import { showSuccess, showError } from '@/utils'
import type { QRLoginSession } from '@/types'

interface Props {
  modelValue: boolean
  accountId: number
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const qrCodeUrl = ref('')
const sessionId = ref('')
const status = ref<QRLoginSession['status']>('pending')
const statusText = ref('正在生成二维码...')
let pollTimer: number | null = null

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    generateQR()
  } else {
    stopPolling()
  }
})

const generateQR = async () => {
  try {
    const response = await generateQRCode()
    if (response.code === 0 || response.code === 200) {
      qrCodeUrl.value = response.data?.qrCodeUrl || ''
      sessionId.value = response.data?.sessionId || ''
      startPolling()
    } else {
      throw new Error(response.msg || '生成二维码失败')
    }
  } catch (error: any) {
    console.error('生成二维码失败:', error)
    showError('生成二维码失败')
  }
}

const startPolling = () => {
  if (!sessionId.value) {
    showError('会话ID为空，无法查询状态')
    return
  }
  pollTimer = window.setInterval(async () => {
    if (!sessionId.value) return
    try {
      const response = await getQRCodeStatus(sessionId.value)
      if (response.code === 0 || response.code === 200) {
        const data = response.data
        status.value = data?.status || 'pending'

        switch (data?.status) {
          case 'pending':
            statusText.value = '等待扫码...'
            break
          case 'scanned':
            statusText.value = '已扫码，等待确认...'
            break
          case 'confirmed':
            statusText.value = '登录成功！正在更新Cookie和Token...'
            await handleLoginSuccess()
            break
          case 'expired':
            statusText.value = '二维码已过期'
            stopPolling()
            break
        }
      }
    } catch (error) {
      console.error('检查登录状态失败:', error)
    }
  }, 2000)
}

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const handleLoginSuccess = async () => {
  try {
    // 1. 获取Cookie
    const cookieRes = await getQRCodeCookies(sessionId.value)
    if (cookieRes.code !== 0 && cookieRes.code !== 200) {
      showError(cookieRes.msg || '获取Cookie失败')
      handleClose()
      return
    }

    // 2. 直接使用返回的Cookie字符串和UNB
    const cookieText = cookieRes.data?.cookies || ''
    const unb = cookieRes.data?.unb || ''

    if (!cookieText) {
      showError('Cookie为空，请重试')
      handleClose()
      return
    }

    // 3. 更新Cookie
    const cookieUpdateRes = await updateCookie({
      xianyuAccountId: props.accountId,
      cookieText: cookieText
    })

    if (cookieUpdateRes.code !== 0 && cookieUpdateRes.code !== 200) {
      showError(cookieUpdateRes.msg || '更新Cookie失败')
      handleClose()
      return
    }

    // 4. 自动启动连接
    statusText.value = 'Cookie更新成功，正在启动连接...'
    try {
      const startRes = await startConnection(props.accountId)
      if (startRes.code === 0 || startRes.code === 200) {
        showSuccess('Cookie更新成功，连接已启动')
      } else {
        showSuccess('Cookie更新成功，请手动启动连接')
      }
    } catch (startError) {
      showSuccess('Cookie更新成功，请手动启动连接')
    }

    emit('success')
    handleClose()

  } catch (error: any) {
    console.error('更新失败:', error)
    showError(error.message || '更新失败')
    handleClose()
  }
}

const handleClose = () => {
  stopPolling()
  emit('update:modelValue', false)
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container">
          <div class="modal-header">
            <h2 class="modal-title">扫码更新 Cookie 和 Token</h2>
            <button class="modal-close" @click="handleClose">×</button>
          </div>
          <div class="modal-body">
            <div class="qr-code-wrap">
              <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="二维码" class="qr-code" />
              <div v-else class="qr-loading"><div class="loading-spinner"></div></div>
            </div>
            <p class="qr-tip">请使用闲鱼APP扫描二维码完成更新</p>
            <div class="qr-status">
              <span class="status-tag" :class="status === 'confirmed' ? 'is-success' : ''">{{ statusText }}</span>
            </div>
            <p v-if="sessionId" class="session-id">会话ID: {{ sessionId }}</p>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="handleClose">取消</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.20);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.modal-container {
  background: rgba(255,255,255,0.72);
  border-radius: 20px;
  width: 100%;
  max-width: 360px;
  box-shadow: 0 32px 100px rgba(0, 0, 0, 0.14), 0 12px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  flex-shrink: 0;
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: rgba(28,28,30,.55);
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.modal-body {
  padding: 0 20px 20px;
  text-align: center;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  flex-shrink: 0;
}

.btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: none;
}

.btn-secondary {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.btn-secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.qr-code-wrap {
  margin: 16px 0;
  display: flex;
  justify-content: center;
}

.qr-code {
  max-width: 180px;
  border-radius: 12px;
}

.qr-loading {
  width: 180px;
  height: 180px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f7;
  border-radius: 12px;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 2px solid #e5e5e5;
  border-top-color: #0071e3;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.qr-tip {
  margin: 8px 0;
  color: #1c1c1e;
  font-size: 14px;
}

.qr-status {
  margin: 10px 0;
  min-height: 28px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.status-tag {
  padding: 5px 12px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 500;
  background: rgba(255,255,255,0.38);
  color: rgba(28,28,30,.55);
}

.status-tag.is-success {
  background: rgba(52, 199, 89, 0.1);
  color: #30D158;
}

.session-id {
  margin: 8px 0;
  font-size: 11px;
  color: rgba(28,28,30,.55);
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.32, 0.94, 0.6, 1), opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.92) translateY(8px);
  opacity: 0;
}
</style>
