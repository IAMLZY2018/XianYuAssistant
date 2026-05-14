<script setup lang="ts">
import { ref, watch } from 'vue'
import { updateCookie } from '@/api/websocket'
import { showSuccess, showError } from '@/utils'

interface Props {
  modelValue: boolean
  accountId: number
  currentCookie: string
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const cookieText = ref('')
const loading = ref(false)
const showHelpImage = ref(false)

watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    cookieText.value = props.currentCookie || ''
  }
})

const handleSubmit = async () => {
  if (!cookieText.value.trim()) {
    showError('Cookie不能为空')
    return
  }

  loading.value = true
  try {
    const response = await updateCookie({
      xianyuAccountId: props.accountId,
      cookieText: cookieText.value.trim()
    })

    if (response.code === 200) {
      showSuccess('Cookie更新成功')
      handleClose()
      emit('success')
    }
  } catch (error: any) {
    console.error('Cookie更新失败:', error)
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  emit('update:modelValue', false)
  showHelpImage.value = false
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container modal-container--lg">
          <div class="modal-header">
            <h2 class="modal-title">手动更新Cookie</h2>
            <div class="modal-header-actions">
              <button class="help-link" @click="showHelpImage = !showHelpImage">不会获取Cookie？</button>
              <button class="modal-close" @click="handleClose">×</button>
            </div>
          </div>
          <div class="modal-body">
            <div v-if="showHelpImage" class="help-image-wrap">
              <img src="/cookieGet.png" alt="Cookie获取指南" class="help-image" />
            </div>
            <template v-else>
              <div class="form-row">
                <label class="form-label">Cookie值</label>
                <textarea v-model="cookieText" class="form-textarea form-textarea--lg" placeholder="请输入完整的Cookie字符串"></textarea>
              </div>
              <div class="alert-box">
                <p class="alert-title">提示</p>
                <p class="alert-text">请从浏览器中复制完整的Cookie字符串</p>
                <p class="alert-text"><span class="highlight">重要字段：</span><code>unb</code>、<code>_m_h5_tk</code>、<code>cookie2</code>、<code>t</code></p>
                <p class="alert-subtitle">格式示例：</p>
                <p class="alert-code"><code>unb</code>=2218021801256; cookies=sgcookie=E100JgD87TWZ...; <code>t</code>=97df36d73d5e5bfb...; tracknick=xy246940070033; <code>_m_h5_tk</code>=5f73f84e8caa...; <code>cookie2</code>=153aeae482f715e0...</p>
              </div>
            </template>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="handleClose">{{ showHelpImage ? '返回' : '取消' }}</button>
            <button v-if="!showHelpImage" class="btn btn-primary" :class="{ 'is-loading': loading }" :disabled="loading" @click="handleSubmit">确定更新</button>
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
  background: rgba(0, 0, 0, 0.4);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.modal-container {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 420px;
  max-height: 88vh;
  box-shadow: 0 32px 100px rgba(0, 0, 0, 0.14), 0 12px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container--lg {
  max-width: 520px;
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
  color: #1d1d1f;
  margin: 0;
}

.modal-header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.help-link {
  background: none;
  border: none;
  color: #86868b;
  font-size: 12px;
  cursor: pointer;
  padding: 0;
  transition: color 0.15s ease;
}

.help-link:hover {
  color: #0071e3;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #86868b;
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
}

.modal-body {
  flex: 1;
  padding: 0 20px 20px;
  overflow-y: auto;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  flex-shrink: 0;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 14px;
}

.form-label {
  font-size: 13px;
  color: #1d1d1f;
  font-weight: 500;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  font-size: 12px;
  line-height: 1.5;
  resize: none;
  background: #fff;
  color: #1d1d1f;
  font-family: 'Courier New', Consolas, monospace;
}

.form-textarea--lg {
  height: 180px;
}

.form-textarea:focus {
  outline: none;
  border-color: #0071e3;
}

.alert-box {
  background: rgba(0, 113, 227, 0.06);
  border-radius: 10px;
  padding: 12px 14px;
}

.alert-title {
  font-size: 13px;
  font-weight: 600;
  color: #0071e3;
  margin: 0 0 8px;
}

.alert-text {
  font-size: 12px;
  color: #1d1d1f;
  margin: 4px 0;
}

.alert-text .highlight {
  color: #ff9500;
  font-weight: 600;
}

.alert-text code {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
  padding: 1px 4px;
  border-radius: 3px;
  font-family: inherit;
}

.alert-subtitle {
  font-size: 12px;
  font-weight: 500;
  color: #1d1d1f;
  margin: 8px 0 4px;
}

.alert-code {
  font-size: 11px;
  color: #86868b;
  line-height: 1.7;
  word-break: break-all;
  margin: 0;
  font-family: 'Courier New', Consolas, monospace;
}

.alert-code code {
  background: rgba(255, 59, 48, 0.1);
  color: #ff3b30;
  padding: 1px 3px;
  border-radius: 3px;
}

.help-image-wrap {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 16px 0;
  min-height: 300px;
}

.help-image {
  max-width: 100%;
  max-height: 500px;
  border-radius: 12px;
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
  background: rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
}

.btn-secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: #0071e3;
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: #0077ed;
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
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
