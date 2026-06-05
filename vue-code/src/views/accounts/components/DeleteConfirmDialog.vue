<script setup lang="ts">
import { ref, watch } from 'vue';
import { deleteAccount } from '@/api/account';
import { showSuccess, showError } from '@/utils';

interface Props {
  modelValue: boolean;
  accountId?: number | null;
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const loading = ref(false);

const handleClose = () => {
  emit('update:modelValue', false);
};

const handleConfirm = async () => {
  if (!props.accountId) return;
  
  loading.value = true;
  try {
    const response = await deleteAccount({ id: props.accountId });
    if (response.code === 0 || response.code === 200) {
      showSuccess('账号删除成功');
      handleClose();
      emit('success');
    } else {
      throw new Error(response.msg || '删除失败');
    }
  } catch (error: any) {
    console.error('删除失败:', error);
  } finally {
    loading.value = false;
  }
};
</script>

<template>
  <teleport to="body">
    <div v-if="modelValue" class="modal-overlay" @click="handleClose">
      <div class="modal" @click.stop>
        <div class="modal-header">
          <h2 class="modal-title">删除账号</h2>
        </div>
        
        <div class="modal-body">
          <p class="delete-title">确定要删除这个账号吗？</p>
          <p class="delete-desc">删除后将无法恢复以下数据：</p>
          <ul class="delete-list">
            <li>聊天消息</li>
            <li>商品信息</li>
            <li>自动发货配置</li>
            <li>自动回复配置</li>
            <li>Cookie信息</li>
          </ul>
        </div>
        
        <div class="modal-footer">
          <button class="modal-btn modal-btn-cancel" @click="handleClose">取消</button>
          <div class="modal-divider"></div>
          <button class="modal-btn modal-btn-danger" :disabled="loading" @click="handleConfirm">
            {{ loading ? '删除中...' : '删除' }}
          </button>
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
  text-align: center;
}

.delete-title {
  margin: 0 0 8px 0;
  font-size: 17px;
  font-weight: 600;
  color: #000;
  line-height: 1.2;
}

.delete-desc {
  margin: 0 0 8px 0;
  font-size: 13px;
  color: #666;
  line-height: 1.4;
}

.delete-list {
  margin: 0;
  padding: 0 0 0 16px;
  list-style: none;
  text-align: left;
  font-size: 13px;
  color: #666;
}

.delete-list li {
  margin: 4px 0;
  line-height: 1.4;
}

.delete-list li::before {
  content: "•";
  color: #ff3b30;
  margin-right: 8px;
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

.modal-btn:active:not(:disabled) {
  opacity: 0.5;
}

.modal-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-btn-cancel {
  color: #666;
}

.modal-btn-danger {
  color: #ff3b30;
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
