<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue';

interface Props {
  modelValue: boolean;
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void;
  (e: 'confirm'): void;
}

const props = defineProps<Props>();
const emit = defineEmits<Emits>();

const isMobile = ref(false);

const checkDevice = () => {
  isMobile.value = window.innerWidth < 768;
};

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

onUnmounted(() => {
  window.removeEventListener('resize', checkDevice);
});
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="modelValue" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container" :class="{ 'is-mobile': isMobile }">
          <div class="modal-header">
            <h2 class="modal-title">需要滑块验证</h2>
            <button class="modal-close" @click="handleClose">×</button>
          </div>
          <div class="modal-body">
            <div class="captcha-alert">
              <span class="alert-icon">⚠️</span>
              <span class="alert-text">检测到账号需要完成滑块验证</span>
            </div>
            <div class="captcha-steps">
              <div class="step-item">
                <span class="step-number">1</span>
                <span class="step-text">点击下方按钮访问闲鱼IM页面</span>
              </div>
              <div class="step-item">
                <span class="step-number">2</span>
                <span class="step-text">在闲鱼页面完成滑块验证</span>
              </div>
              <div class="step-item">
                <span class="step-number">3</span>
                <span class="step-text">按F12打开开发者工具，复制Cookie</span>
              </div>
              <div class="step-item">
                <span class="step-number">4</span>
                <span class="step-text">返回本页面，点击"手动更新"粘贴Cookie</span>
              </div>
            </div>
            <div class="captcha-tip">
              <span class="tip-icon">💡</span>
              <span class="tip-text">更新Cookie后点击"启动连接"，会自动更新WebSocket Token，滑块校验生效会延迟，稍等片刻会自动连接闲鱼服务器</span>
            </div>
          </div>
          <div class="modal-footer">
            <button class="btn btn-secondary" @click="handleClose">取消</button>
            <button class="btn btn-primary" @click="handleConfirm">访问闲鱼IM</button>
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
  box-shadow: 0 32px 100px rgba(0, 0, 0, 0.14), 0 12px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container.is-mobile {
  max-width: 94vw;
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
  padding: 0 20px 20px;
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

.captcha-alert {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: rgba(255,255,255,0.55)3cd;
  border-radius: 10px;
}

.alert-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.alert-text {
  font-size: 13px;
  color: #856404;
  font-weight: 500;
  line-height: 1.4;
}

.captcha-steps {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f5f5f7;
  border-radius: 10px;
}

.step-number {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #0071e3;
  color: rgba(255,255,255,0.55);
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.step-text {
  font-size: 13px;
  color: #1c1c1e;
}

.captcha-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 10px 12px;
  background: rgba(0, 113, 227, 0.06);
  border-radius: 10px;
}

.tip-icon {
  font-size: 14px;
  flex-shrink: 0;
}

.tip-text {
  font-size: 12px;
  color: #1c1c1e;
  line-height: 1.5;
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

.btn-primary:hover {
  background: #0077ed;
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
