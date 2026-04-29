<script setup lang="ts">
import { ref, watch } from 'vue'
import { generateQRCode, getQRCodeStatus, getQRCodeCookies } from '@/api/qrlogin'
import { addAccount } from '@/api/account'
import { showSuccess, showError } from '@/utils'
import type { QRLoginSession } from '@/types'

interface Props {
  modelValue: boolean
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

    // 3. 添加账号
    const accountNote = `账号_${unb || Date.now()}`
    const addRes = await addAccount({
      accountNote,
      unb,
      cookie: cookieText
    } as any)

    // 4. 处理结果
    if (addRes.code === 0 || addRes.code === 200) {
      showSuccess('账号添加成功')
      emit('success')
    } else {
      showError(addRes.msg || '添加账号失败')
    }

    // 5. 关闭弹窗（无论成功失败都关闭）
    handleClose()

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
  <el-dialog
    :model-value="modelValue"
    title="扫码添加闲鱼账号"
    width="400px"
    @close="handleClose"
  >
    <div class="qr-login-content">
      <div class="qr-code-container">
        <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="二维码" class="qr-code" />
        <el-skeleton v-else animated />
      </div>
      
      <p class="qr-tip">请使用闲鱼APP扫描二维码登录</p>
      
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
