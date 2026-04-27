<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';

interface Props {
  modelValue: boolean;
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'confirm'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

// 响应式检测
const isMobile = ref(false);
const isTablet = ref(false);
const isDesktop = ref(false);

const checkDevice = () => {
  const width = window.innerWidth;
  isMobile.value = width < 768;
  isTablet.value = width >= 768 && width < 1024;
  isDesktop.value = width >= 1024;
};

// 计算对话框宽度
const dialogWidth = computed(() => {
  if (isMobile.value) return '92%';
  if (isTablet.value) return '480px';
  return '420px';
});

const handleClose = () => {
  emit('update:modelValue', false);
};

const handleConfirm = () => {
  emit('confirm');
  emit('update:modelValue', false);
};

onMounted(() => {
  checkDevice();
  window.addEventListener('resize', checkDevice);
});
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="需要滑块验证"
    :width="dialogWidth"
    @close="handleClose"
    class="captcha-guide-dialog"
    :class="{ 'is-mobile': isMobile, 'is-tablet': isTablet, 'is-desktop': isDesktop }"
    :close-on-click-modal="false"
    :close-on-press-escape="false"
    :show-close="true"
  >
    <div class="captcha-content">
      <!-- 警告提示 -->
      <div class="captcha-alert">
        <div class="alert-icon">⚠️</div>
        <div class="alert-text">检测到账号需要完成滑块验证</div>
      </div>
      
      <!-- 操作步骤 -->
      <div class="captcha-steps">
        <div class="step-item">
          <div class="step-number">1</div>
          <div class="step-text">点击下方按钮访问闲鱼IM页面</div>
        </div>
        
        <div class="step-item">
          <div class="step-number">2</div>
          <div class="step-text">在闲鱼页面完成滑块验证</div>
        </div>
        
        <div class="step-item">
          <div class="step-number">3</div>
          <div class="step-text">按F12打开开发者工具，复制Cookie</div>
        </div>
        
        <div class="step-item">
          <div class="step-number">4</div>
          <div class="step-text">返回本页面，点击"手动更新"粘贴Cookie</div>
        </div>
      </div>
      
      <!-- 提示 -->
      <div class="captcha-tip">
        <span class="tip-icon">💡</span>
        <span class="tip-text">更新Cookie后点击"启动连接"，会自动更新WebSocket Token，滑块校验生效会延迟，稍等片刻会自动连接闲鱼服务器</span>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" class="btn-cancel">取消</el-button>
        <el-button type="primary" @click="handleConfirm" class="btn-confirm">
          访问闲鱼IM
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.captcha-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 警告提示 */
.captcha-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: #fff3cd;
  border-radius: 8px;
  border-left: 3px solid #ffc107;
}

.alert-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.alert-text {
  font-size: 14px;
  color: #856404;
  font-weight: 500;
  line-height: 1.4;
}

/* 操作步骤 */
.captcha-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 8px;
  transition: all 0.2s ease;
}

.step-item:hover {
  background: #f0f2f5;
}

.step-number {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #007aff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}

.step-text {
  font-size: 13px;
  color: #333;
  line-height: 1.4;
}

/* 提示 */
.captcha-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  background: #e7f3ff;
  border-radius: 8px;
}

.tip-icon {
  font-size: 16px;
  flex-shrink: 0;
}

.tip-text {
  font-size: 12px;
  color: #007aff;
  font-weight: 500;
}

/* 底部按钮 */
.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.btn-cancel {
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 6px;
}

.btn-confirm {
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 6px;
  font-weight: 500;
}

/* 移动端样式 - iOS 18 风格 */
@media (max-width: 767px) {
  .captcha-alert {
    padding: 10px 12px;
    border-radius: 10px;
  }

  .alert-icon {
    font-size: 18px;
  }

  .alert-text {
    font-size: 13px;
  }

  .step-item {
    padding: 8px 10px;
    border-radius: 10px;
  }

  .step-number {
    width: 22px;
    height: 22px;
    font-size: 12px;
  }

  .step-text {
    font-size: 12px;
  }

  .captcha-tip {
    padding: 8px 10px;
    border-radius: 10px;
  }

  .tip-text {
    font-size: 11px;
  }

  .dialog-footer {
    flex-direction: column-reverse;
    gap: 8px;
  }

  .btn-cancel,
  .btn-confirm {
    width: 100%;
    padding: 12px 16px;
    font-size: 15px;
    border-radius: 10px;
  }
}

/* 平板端样式 - iPadOS 18 风格 */
@media (min-width: 768px) and (max-width: 1023px) {
  .captcha-alert {
    padding: 14px 16px;
    border-radius: 12px;
  }

  .step-item {
    padding: 12px 14px;
    border-radius: 10px;
  }

  .step-number {
    width: 26px;
    height: 26px;
    font-size: 14px;
  }

  .step-text {
    font-size: 14px;
  }

  .btn-cancel,
  .btn-confirm {
    padding: 10px 20px;
    font-size: 14px;
    border-radius: 8px;
  }
}

/* 桌面端样式 - macOS 风格 */
@media (min-width: 1024px) {
  .captcha-content {
    gap: 14px;
  }

  .captcha-alert {
    padding: 10px 12px;
    border-radius: 6px;
  }

  .alert-icon {
    font-size: 18px;
  }

  .alert-text {
    font-size: 13px;
  }

  .step-item {
    padding: 8px 10px;
    border-radius: 6px;
  }

  .step-number {
    width: 22px;
    height: 22px;
    font-size: 12px;
  }

  .step-text {
    font-size: 12px;
  }

  .captcha-tip {
    padding: 8px 10px;
    border-radius: 6px;
  }

  .tip-text {
    font-size: 11px;
  }

  .btn-cancel,
  .btn-confirm {
    padding: 6px 14px;
    font-size: 12px;
    border-radius: 4px;
  }
}
</style>

<style>
/* 全局样式：滑块验证引导对话框 */
.captcha-guide-dialog {
  border-radius: 12px !important;
}

.captcha-guide-dialog .el-dialog__header {
  padding: 16px 20px 12px !important;
  border-bottom: 1px solid #e5e5e5 !important;
}

.captcha-guide-dialog .el-dialog__title {
  font-size: 16px !important;
  font-weight: 600 !important;
  color: #1d1d1f !important;
}

.captcha-guide-dialog .el-dialog__body {
  padding: 16px 20px !important;
}

.captcha-guide-dialog .el-dialog__footer {
  padding: 12px 20px 16px !important;
  border-top: 1px solid #e5e5e5 !important;
}

/* iOS 风格 - 移动端 */
.captcha-guide-dialog.is-mobile {
  border-radius: 16px !important;
}

.captcha-guide-dialog.is-mobile .el-dialog__header {
  padding: 20px 20px 16px !important;
  border-bottom: none !important;
  text-align: center !important;
}

.captcha-guide-dialog.is-mobile .el-dialog__title {
  font-size: 17px !important;
  font-weight: 600 !important;
}

.captcha-guide-dialog.is-mobile .el-dialog__body {
  padding: 0 20px 16px !important;
}

.captcha-guide-dialog.is-mobile .el-dialog__footer {
  padding: 0 20px 20px !important;
  border-top: none !important;
}

/* iPadOS 风格 - 平板端 */
.captcha-guide-dialog.is-tablet {
  border-radius: 14px !important;
}

.captcha-guide-dialog.is-tablet .el-dialog__header {
  padding: 18px 24px 14px !important;
}

.captcha-guide-dialog.is-tablet .el-dialog__title {
  font-size: 17px !important;
}

.captcha-guide-dialog.is-tablet .el-dialog__body {
  padding: 18px 24px !important;
}

.captcha-guide-dialog.is-tablet .el-dialog__footer {
  padding: 14px 24px 18px !important;
}

/* macOS 风格 - 桌面端 */
.captcha-guide-dialog.is-desktop {
  border-radius: 10px !important;
}

.captcha-guide-dialog.is-desktop .el-dialog__header {
  padding: 14px 18px 10px !important;
}

.captcha-guide-dialog.is-desktop .el-dialog__title {
  font-size: 14px !important;
  font-weight: 600 !important;
}

.captcha-guide-dialog.is-desktop .el-dialog__body {
  padding: 14px 18px !important;
}

.captcha-guide-dialog.is-desktop .el-dialog__footer {
  padding: 10px 18px 14px !important;
}
</style>
