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

const dialogVisible = ref(false);
const loading = ref(false);
const tokenInput = ref('');

// 监听modelValue变化
watch(() => props.modelValue, (val) => {
  dialogVisible.value = val;
  if (val) {
    tokenInput.value = props.currentToken || '';
  }
});

// 监听dialogVisible变化
watch(dialogVisible, (val) => {
  emit('update:modelValue', val);
});

// 提交更新
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
      dialogVisible.value = false;
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

// 关闭对话框
const handleClose = () => {
  dialogVisible.value = false;
};
</script>

<template>
  <el-dialog
    v-model="dialogVisible"
    title="手动更新 WebSocket Token"
    width="600px"
    :close-on-click-modal="false"
  >
    <div class="dialog-content">
      <el-alert
        title="提示"
        type="info"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <p>WebSocket Token用于WebSocket连接认证，有效期约20小时。</p>
      </el-alert>
      
      <el-form label-width="120px">
        <el-form-item label="账号ID">
          <el-input :value="accountId" disabled />
        </el-form-item>
        
        <el-form-item label="当前Token">
          <el-input
            :value="currentToken"
            type="textarea"
            :rows="3"
            disabled
            placeholder="未获取到Token"
          />
        </el-form-item>
        
        <el-form-item label="新Token">
          <el-input
            v-model="tokenInput"
            type="textarea"
            :rows="3"
            placeholder="请输入新的WebSocket Token"
          />
          <div style="margin-top: 8px; color: #909399; font-size: 12px;">
            请确保Token格式正确，错误的Token会导致WebSocket连接失败
          </div>
        </el-form-item>
      </el-form>
    </div>
    
    <template #footer>
      <span class="dialog-footer">
        <el-button @click="handleClose">取消</el-button>
        <el-button type="primary" :loading="loading" @click="handleSubmit">
          确定更新
        </el-button>
      </span>
    </template>
  </el-dialog>
</template>

<style scoped>
.dialog-content {
  padding: 0 20px;
}
</style>
