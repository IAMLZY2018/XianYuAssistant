<script setup lang="ts">
import { ref, watch } from 'vue'
import { updateCookie } from '@/api/websocket'
import { showSuccess, showError } from '@/utils'
import IconClose from '@/components/icons/IconClose.vue'
import IconHelp from '@/components/icons/IconHelp.vue'

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
    showHelpImage.value = false
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

const toggleHelpImage = () => {
  showHelpImage.value = !showHelpImage.value
}
</script>

<template>
  <Transition name="modal-fade">
    <div v-if="modelValue" class="modal-overlay" @click="handleClose">
      <div class="modal-container" @click.stop>
        <!-- Header -->
        <div class="modal-header">
          <h2 class="modal-title">手动更新Cookie</h2>
          <button class="help-link" @click="toggleHelpImage">
            <IconHelp />
            <span>不会获取Cookie？</span>
          </button>
          <button class="modal-close" @click="handleClose">
            <IconClose />
          </button>
        </div>

        <!-- Content -->
        <div class="modal-content">
          <!-- Help Image -->
          <div v-if="showHelpImage" class="help-image-container">
            <img src="/cookieGet.png" alt="Cookie获取指南" class="help-image" />
          </div>

          <!-- Form -->
          <div v-else class="form-container">
            <div class="form-group">
              <label class="form-label">Cookie值</label>
              <textarea
                v-model="cookieText"
                class="form-textarea"
                placeholder="请输入完整的Cookie字符串"
                rows="8"
              />
            </div>

            <!-- Tips -->
            <div class="tips-container">
              <div class="tips-title">提示</div>
              <div class="tips-content">
                <p class="tips-text">请从浏览器中复制完整的Cookie字符串</p>
                <p class="tips-text tips-text--highlight">
                  <span class="tips-label">重要字段：</span>
                  <span class="tips-field">unb</span>、
                  <span class="tips-field">_m_h5_tk</span>、
                  <span class="tips-field">cookie2</span>、
                  <span class="tips-field">t</span>
                </p>
                <p class="tips-text tips-text--bold">格式示例：</p>
                <p class="tips-example">
                  <span class="tips-field">unb</span>=2218021801256; 
                  cookies=sgcookie=E100JgD87TWZ...; 
                  <span class="tips-field">t</span>=97df36d73d5e5bfb...; 
                  tracknick=xy246940070033; 
                  csg=f7aeab6d; 
                  _m_h5_tk_enc=51ce7936ea...; 
                  XSRF-TOKEN=bb76b331-48fb-496b...; 
                  _samesite_flag_=true; 
                  mtop_partitioned_detect=1; 
                  <span class="tips-field">_m_h5_tk</span>=5f73f84e8caa...; 
                  _tb_token_=e3f1fd5ee5a34; 
                  <span class="tips-field">cookie2</span>=153aeae482f715e0...
                </p>
              </div>
            </div>
          </div>
        </div>

        <!-- Footer -->
        <div class="modal-footer">
          <button class="btn btn--secondary" @click="handleClose">
            {{ showHelpImage ? '返回' : '取消' }}
          </button>
          <button v-if="!showHelpImage" class="btn btn--primary" :disabled="loading" @click="handleSubmit">
            {{ loading ? '更新中...' : '确定更新' }}
          </button>
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
  gap: 12px;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
  letter-spacing: -0.01em;
  flex: 1;
}

.help-link {
  display: flex;
  align-items: center;
  gap: 6px;
  background: none;
  border: none;
  color: rgba(28,28,30,.55);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 6px 10px;
  border-radius: 8px;
  transition: all 0.2s;
  -webkit-tap-highlight-color: transparent;
  flex-shrink: 0;
}

.help-link:hover {
  color: #0A84FF;
  background: rgba(0, 122, 255, 0.08);
}

.help-link svg {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
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
  flex-shrink: 0;
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

.help-image-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}

.help-image {
  max-width: 100%;
  max-height: 600px;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.form-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
  letter-spacing: -0.01em;
}

.form-textarea {
  padding: 12px;
  font-size: 12px;
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  line-height: 1.5;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 12px;
  background: rgba(255,255,255,0.15);
  color: #1c1c1e;
  resize: vertical;
  transition: all 0.2s;
}

.form-textarea:focus {
  outline: none;
  border-color: #0A84FF;
  background: rgba(255,255,255,0.55);
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.1);
}

.form-textarea::placeholder {
  color: rgba(28,28,30,.55);
}

.tips-container {
  background: rgba(0, 122, 255, 0.08);
  border: 1px solid rgba(0, 122, 255, 0.2);
  border-radius: 12px;
  padding: 12px;
}

.tips-title {
  font-size: 13px;
  font-weight: 600;
  color: #0A84FF;
  margin-bottom: 8px;
}

.tips-content {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tips-text {
  font-size: 12px;
  color: rgba(28,28,30,.55);
  margin: 0;
  line-height: 1.5;
}

.tips-text--highlight {
  color: #1c1c1e;
}

.tips-text--bold {
  font-weight: 500;
  color: #1c1c1e;
}

.tips-label {
  color: #FF9F0A;
  font-weight: 600;
}

.tips-field {
  color: #FF453A;
  font-weight: 500;
}

.tips-example {
  font-size: 11px;
  font-family: 'SF Mono', 'Menlo', 'Monaco', monospace;
  color: rgba(28,28,30,.55);
  line-height: 1.6;
  word-break: break-all;
  margin: 0;
}

.modal-footer {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  border-top: 0.5px solid rgba(60,60,67,.12);
  background: rgba(255,255,255,0.72);
  flex-shrink: 0;
}

.btn {
  flex: 1;
  padding: 12px 16px;
  font-size: 15px;
  font-weight: 600;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
  -webkit-tap-highlight-color: transparent;
  letter-spacing: -0.01em;
}

.btn--primary {
  background: #0A84FF;
  color: white;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.btn--primary:hover:not(:disabled) {
  background: #0066d6;
  box-shadow: 0 6px 16px rgba(0, 122, 255, 0.4);
}

.btn--primary:active:not(:disabled) {
  transform: scale(0.96);
}

.btn--primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn--secondary {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.btn--secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn--secondary:active {
  transform: scale(0.96);
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

  .modal-footer {
    padding: 12px 16px;
    gap: 10px;
  }

  .btn {
    padding: 10px 14px;
    font-size: 14px;
  }

  .form-textarea {
    font-size: 11px;
  }

  .tips-text {
    font-size: 11px;
  }

  .tips-example {
    font-size: 10px;
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

  .form-textarea {
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
