<script setup lang="ts">
import { ref } from 'vue';
import { manualAddAccount } from '@/api/account';
import { showSuccess, showError } from '@/utils';

interface Props {
  modelValue: boolean;
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'success'): void;
}

defineProps<Props>();
const emit = defineEmits<Emits>();

const formData = ref({
  accountNote: '',
  cookie: ''
});

const handleClose = () => {
  emit('update:modelValue', false);
  formData.value = {
    accountNote: '',
    cookie: ''
  };
};

const handleSubmit = async () => {
  if (!formData.value.accountNote.trim()) {
    showError('请输入账号备注');
    return;
  }
  
  if (!formData.value.cookie.trim()) {
    showError('请输入 Cookie');
    return;
  }

  try {
    const response = await manualAddAccount(formData.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('账号添加成功');
      handleClose();
      emit('success');
    } else {
      throw new Error(response.msg || '添加失败');
    }
  } catch (error: any) {
    console.error('添加失败:', error);
  }
};
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="手动添加闲鱼账号"
    width="600px"
    @close="handleClose"
  >
    <el-form :model="formData" label-width="100px">
      <el-form-item label="账号备注">
        <el-input v-model="formData.accountNote" placeholder="请输入账号备注" />
      </el-form-item>
      
      <el-form-item label="Cookie">
        <el-input
          v-model="formData.cookie"
          type="textarea"
          :rows="6"
          placeholder="请输入完整的 Cookie 字符串"
        />
      </el-form-item>
    </el-form>
    
    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" @click="handleSubmit">添加账号</el-button>
    </template>
  </el-dialog>
</template>
