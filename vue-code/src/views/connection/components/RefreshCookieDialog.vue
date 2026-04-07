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
  <el-dialog
    :model-value="modelValue"
    title="扫码刷新Cookie"
    width="400px"
    @close="handleClose"
  >
    <div class="qr-login-content">
      <div class="qr-code-container">
        <img v-if="qrCodeUrl" :src="qrCodeUrl" alt="二维码" class="qr-code" />
        <el-skeleton v-else animated />
      </div>
      
      <p class="qr-tip">请使用闲鱼APP扫描二维码登录</p>
      <p class="qr-warning">请确保扫码账号与当前账号一致</p>
      
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

.qr-warning {
  margin: 5px 0;
  color: #e6a23c;
  font-size: 13px;
  font-weight: 500;
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
