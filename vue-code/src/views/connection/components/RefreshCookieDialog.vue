<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { generateQRCode, getQRCodeStatus, getQRCodeCookies } from '@/api/qrlogin'
import { updateCookie } from '@/api/websocket'
import { showSuccess, showError, showWarning } from '@/utils'
import type { QRLoginSession } from '@/types'
import { ElMessageBox } from 'element-plus'

interface Props {
  modelValue: boolean
  accountId: number
  currentUnb: string
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
    // request拦截器会自动处理错误，这里只处理成功的情况
    if (response.code === 200) {
      qrCodeUrl.value = response.data?.qrCodeUrl || ''
      sessionId.value = response.data?.sessionId || ''
      startPolling()
    }
  } catch (error: any) {
    // request拦截器已经显示了错误消息，这里不需要再显示
    console.error('生成二维码失败:', error)
  }
}

const startPolling = () => {
  pollTimer = window.setInterval(async () => {
    try {
      const response = await getQRCodeStatus(sessionId.value)
      if (response.code === 200) {
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
            statusText.value = '登录成功！正在获取信息...'
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
    if (cookieRes.code !== 200) {
      showError(cookieRes.msg || '获取Cookie失败')
      handleClose()
      return
    }
    
    // 2. 解析Cookie数据
    const cookieData = cookieRes.data as any
    const cookieText = typeof cookieData?.cookies === 'string' ? cookieData.cookies : JSON.stringify(cookieData?.cookies || {})
    const scannedUnb = cookieData?.unb || ''
    
    console.log('扫码UNB:', scannedUnb, '当前UNB:', props.currentUnb)
    
    if (!cookieText) {
      showError('Cookie为空，请重试')
      handleClose()
      return
    }
    
    // 3. 判断扫码账号是否与当前账号匹配
    if (scannedUnb === props.currentUnb) {
      // 匹配，更新Cookie
      const updateRes = await updateCookie({
        xianyuAccountId: props.accountId,
        cookieText
      })
      
      if (updateRes.code === 200) {
        showSuccess('Cookie刷新成功')
        emit('success')
      } else {
        showError(updateRes.msg || 'Cookie刷新失败')
      }
      handleClose()
    } else {
      // 不匹配，弹窗提示
      await ElMessageBox.alert(
        `扫码登录账号(${scannedUnb})与当前账号(${props.currentUnb})不匹配，已刷新或新增账号`,
        '账号不匹配',
        {
          confirmButtonText: '确定',
          type: 'warning'
        }
      )
      handleClose()
      emit('success')
    }
    
  } catch (error: any) {
    console.error('处理登录失败:', error)
    showError(error.message || '处理登录失败')
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
            <h2 class="modal-title">扫码刷新Cookie</h2>
            <button class="modal-close" @click="handleClose">×</button>
          </div>
          <div class="modal-body">
            <div class="qr-code-wrap">
              <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="二维码" class="qr-code" />
              <div v-else class="qr-loading">
                <div class="loading-spinner"></div>
              </div>
            </div>
            <p class="qr-tip">请使用闲鱼APP扫描二维码登录</p>
            <p class="qr-warning">请确保扫码账号与当前账号一致</p>
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

.qr-warning {
  margin: 4px 0;
  color: #FF9F0A;
  font-size: 13px;
  font-weight: 500;
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

/* Transitions */
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
