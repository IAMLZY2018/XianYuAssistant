<script setup lang="ts">
import { ref, watch, computed } from 'vue'
import { updateAccount } from '@/api/account'
import { showSuccess, showError } from '@/utils'
import type { Account } from '@/types'

interface Props {
  modelValue: boolean
  account?: Account | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formData = ref({
  accountId: 0,
  accountNote: ''
})

watch(() => props.account, (newAccount) => {
  if (newAccount) {
    formData.value = {
      accountId: newAccount.id,
      accountNote: newAccount.accountNote || ''
    }
  } else {
    formData.value = {
      accountId: 0,
      accountNote: ''
    }
  }
}, { immediate: true })

const handleClose = () => {
  emit('update:modelValue', false)
}

const handleSubmit = async () => {
  if (!formData.value.accountNote.trim()) {
    showError('请输入账号备注')
    return
  }

  try {
    const response = await updateAccount(formData.value)
    if (response.code === 0 || response.code === 200) {
      showSuccess('保存成功')
      handleClose()
      emit('success')
    } else {
      throw new Error(response.msg || '保存失败')
    }
  } catch (error: any) {
    console.error('保存失败:', error)
  }
}
</script>

<template>
  <teleport to="body">
    <div v-if="modelValue" class="modal-overlay" @click="handleClose">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">{{ props.account ? '编辑账号' : '添加账号' }}</h2>
        </div>
        
        <div class="modal-body">
          <input 
            v-model="formData.accountNote"
            type="text"
            class="modal-input"
            placeholder="账号备注"
          />
        </div>
        
        <div class="modal-footer">
          <button class="modal-btn modal-btn-cancel" @click="handleClose">取消</button>
          <div class="modal-divider"></div>
          <button class="modal-btn modal-btn-primary" @click="handleSubmit">保存</button>
        </div>
      </div>
    </div>
  </teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.25);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  animation: fadeIn 0.2s ease;
}

.modal {
  width: 320px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.75);
  backdrop-filter: blur(30px);
  -webkit-backdrop-filter: blur(30px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  animation: scaleIn 0.2s ease;
}

.modal-header {
  padding: 16px;
  text-align: center;
  border-bottom: 0.5px solid rgba(0, 0, 0, 0.1);
}

.modal-title {
  margin: 0;
  font-size: 17px;
  font-weight: 600;
  color: #000;
  line-height: 1.2;
}

.modal-body {
  padding: 20px 16px;
}

.modal-input {
  width: 100%;
  height: 42px;
  border-radius: 12px;
  border: none;
  padding: 0 12px;
  font-size: 15px;
  background: rgba(0, 0, 0, 0.05);
  color: #000;
  outline: none;
  transition: all 0.2s ease;
  box-sizing: border-box;
}

.modal-input:hover {
  background: rgba(0, 0, 0, 0.08);
}

.modal-input:focus {
  background: rgba(0, 0, 0, 0.08);
}

.modal-input::placeholder {
  color: #999;
}

.modal-footer {
  display: flex;
  height: 48px;
  border-top: 0.5px solid rgba(0, 0, 0, 0.1);
}

.modal-btn {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 16px;
  font-weight: 500;
  cursor: pointer;
  transition: opacity 0.15s;
  -webkit-tap-highlight-color: transparent;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-btn:active {
  opacity: 0.5;
}

.modal-btn-cancel {
  color: #666;
}

.modal-btn-primary {
  color: #007aff;
  font-weight: 500;
}

.modal-divider {
  width: 0.5px;
  background: rgba(0, 0, 0, 0.1);
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes scaleIn {
  from {
    transform: scale(0.9);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}
</style>
