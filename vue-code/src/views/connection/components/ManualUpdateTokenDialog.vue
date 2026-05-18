<script setup lang="ts">
import { ref, watch } from 'vue';
import { updateToken } from '@/api/websocket';
import { showSuccess, showError } from '@/utils';

const props = defineProps<{
  modelValue: boolean;
  accountId: number;
  currentToken: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}>();

const loading = ref(false);
const tokenInput = ref('');

watch(() => props.modelValue, (val) => {
  if (val) {
    tokenInput.value = props.currentToken || '';
  }
});

const handleSubmit = async () => {
  if (!tokenInput.value.trim()) {
    showError('请输入WebSocket Token');
    return;
  }
  
  loading.value = true;
  try {
    const response = await updateToken({
      xianyuAccountId: props.accountId,
      websocketToken: tokenInput.value.trim()
    });
    
    if (response.code === 0 || response.code === 200) {
      showSuccess('Token更新成功');
      handleClose();
      emit('success');
    } else {
      throw new Error(response.msg || '更新失败');
    }
  } catch (error: any) {
    console.error('更新Token失败:', error);
    showError('更新失败: ' + error.message);
  } finally {
    loading.value = false;
  }
};

const handleClose = () => {
  emit('update:modelValue', false);
};
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container modal-container--lg">
          <div class="modal-header">
            <h2 class="modal-title">手动更新 WebSocket Token</h2>
            <button class="modal-close" @click="handleClose">×</button>
          </div>
          <div class="modal-body">
            <div class="alert-box">
              <p class="alert-text">WebSocket Token用于WebSocket连接认证，有效期约20小时。</p>
            </div>
            <div class="form-row">
              <label class="form-label">账号ID</label>
              <input :value="accountId" class="form-input" disabled />
            </div>
            <div class="form-row">
              <label class="form-label">当前Token</label>
              <textarea :value="currentToken" class="form-textarea" disabled placeholder="未获取到Token"></textarea>
            </div>
            <div class="form-row">
              <label class="form-label">新Token</label>
              <textarea v-model="tokenInput" class="form-textarea" placeholder="请输入新的WebSocket Token"></textarea>
              <p class="form-hint">请确保Token格式正确，错误的Token会导致WebSocket连接失败</p>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="handleClose">取消</button>
            <button class="btn btn-primary" :class="{ 'is-loading': loading }" :disabled="loading" @click="handleSubmit">确定更新</button>
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
  max-width: 420px;
  max-height: 88vh;
  box-shadow: 0 32px 100px rgba(0, 0, 0, 0.14), 0 12px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container--lg {
  max-width: 540px;
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
  flex: 1;
  padding: 0 20px 20px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  flex-shrink: 0;
}

.alert-box {
  background: rgba(0, 113, 227, 0.06);
  border-radius: 10px;
  padding: 10px 14px;
}

.alert-text {
  font-size: 13px;
  color: #0071e3;
  margin: 0;
}

.form-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.form-label {
  font-size: 13px;
  color: #1c1c1e;
  font-weight: 500;
}

.form-input {
  padding: 8px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 8px;
  font-size: 13px;
  background: #f5f5f7;
  color: rgba(28,28,30,.55);
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  resize: none;
  background: rgba(255,255,255,0.55);
  color: #1c1c1e;
  min-height: 80px;
  font-family: inherit;
}

.form-textarea:focus {
  outline: none;
  border-color: #0071e3;
}

.form-textarea:disabled {
  background: #f5f5f7;
  color: rgba(28,28,30,.55);
}

.form-hint {
  font-size: 12px;
  color: rgba(28,28,30,.55);
  margin: 0;
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

.btn-primary {
  background: #0071e3;
  color: rgba(255,255,255,0.55);
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
