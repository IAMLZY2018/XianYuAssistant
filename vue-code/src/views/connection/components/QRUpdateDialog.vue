<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateQRCode, getQRCodeStatus, getQRCodeCookies } from '@/api/qrlogin'
import { updateCookie, updateToken } from '@/api/websocket'
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
  pollTimer = window.setInterval(async () => {
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

    // 4. 更新Token（从Cookie中提取或使用返回的token）
    const token = cookieRes.data?.token || ''
    if (token) {
      await updateToken({
        xianyuAccountId: props.accountId,
        websocketToken: token
      })
    }

    // 5. 处理结果
    showSuccess('Cookie和Token更新成功')
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
  <el-dialog
    :model-value="modelValue"
    title="扫码更新 Cookie 和 Token"
    width="400px"
    @close="handleClose"
  >
    <div class="qr-login-content">
      <div class="qr-code-container">
        <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="二维码" class="qr-code" />
        <el-skeleton v-else animated />
      </div>

      <p class="qr-tip">请使用闲鱼APP扫描二维码完成更新</p>

      <div class="qr-status">
        <el-tag :type="status === 'confirmed' ? 'success' : 'info'">
          {{ statusText }}
        </el-tag>
      </div>

      <p v-if="sessionId" class="session-id">会话ID: {{ sessionId }}</p>
    </div>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.qr-login-content {
  text-align: center;
  padding: 20px 0;
}

.qr-code-container {
  margin: 20px 0;
  display: flex;
  justify-content: center;
}

.qr-code {
  max-width: 200px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
}

.qr-tip {
  margin: 10px 0;
  color: #606266;
  font-size: 14px;
}

.qr-status {
  margin: 10px 0;
  min-height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
}

.session-id {
  margin: 10px 0;
  font-size: 12px;
  color: #909399;
}
</style>
