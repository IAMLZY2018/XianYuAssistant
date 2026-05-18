<script setup lang="ts">
import { ref, computed } from 'vue'
import { checkUpdate } from '@/api/system'
import IconClose from '@/components/icons/IconClose.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'

declare const __APP_VERSION__: string

const appVersion = __APP_VERSION__ || '2.0.0'

const visible = ref(false)
const loading = ref(false)
const updateInfo = ref<{
  currentVersion: string
  latestVersion: string
  hasUpdate: boolean
  updateContent: string
  publishedAt: string
  downloadUrl: string
} | null>(null)

const isMobile = ref(false)

const checkMobile = () => {
  isMobile.value = window.innerWidth < 768
}

const isUpdateAvailable = computed(() => {
  if (!updateInfo.value?.latestVersion) return false
  return updateInfo.value.latestVersion > appVersion
})

const formattedDate = computed(() => {
  if (!updateInfo.value?.publishedAt) return ''
  const d = new Date(updateInfo.value.publishedAt)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
})

const open = async () => {
  checkMobile()
  visible.value = true
  loading.value = true
  try {
    const res = await checkUpdate()
    updateInfo.value = res.data || null
  } catch {
    updateInfo.value = null
  } finally {
    loading.value = false
  }
}

const close = () => {
  visible.value = false
}

const openDownload = () => {
  if (updateInfo.value?.downloadUrl) {
    window.open(updateInfo.value.downloadUrl, '_blank')
  }
}

defineExpose({ open })
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="close">
        <div class="modal-container" :class="{ 'is-mobile': isMobile }">
          <!-- Header -->
          <div class="modal-header">
            <div class="modal-title-wrap">
              <div class="modal-icon">
                <IconSparkle />
              </div>
              <h2 class="modal-title">版本更新</h2>
            </div>
            <button class="modal-close" @click="close">
              <IconClose />
            </button>
          </div>

          <!-- Loading -->
          <div v-if="loading" class="modal-loading">
            <div class="loading-spinner"></div>
            <span>正在检查更新...</span>
          </div>

          <!-- Content -->
          <div v-else-if="updateInfo" class="modal-body">
            <!-- Version Info - 横向紧凑布局 -->
            <div class="version-row">
              <div class="version-item">
                <span class="version-label">当前版本</span>
                <span class="version-value">v{{ appVersion }}</span>
              </div>
              <div class="version-item">
                <span class="version-label">最新版本</span>
                <span class="version-value" :class="{ 'is-new': isUpdateAvailable }">v{{ updateInfo.latestVersion }}</span>
              </div>
              <div v-if="formattedDate" class="version-item">
                <span class="version-label">发布时间</span>
                <span class="version-value">{{ formattedDate }}</span>
              </div>
            </div>

            <!-- Status Badge -->
            <div class="status-badge" :class="{ 'is-updated': !isUpdateAvailable }">
              <IconCheck v-if="!isUpdateAvailable" />
              <span>{{ isUpdateAvailable ? '发现新版本' : '已是最新版本' }}</span>
            </div>

            <!-- Changelog -->
            <div v-if="updateInfo.updateContent" class="changelog">
              <div class="changelog-title">更新内容</div>
              <div class="changelog-content">{{ updateInfo.updateContent }}</div>
            </div>
          </div>

          <!-- Error -->
          <div v-else class="modal-error">
            <span>检查更新失败，请稍后重试</span>
          </div>

          <!-- Footer -->
          <div v-if="!loading && updateInfo" class="modal-footer">
            <button class="btn btn-secondary" @click="close">关闭</button>
            <button v-if="isUpdateAvailable" class="btn btn-primary" @click="openDownload">
              查看更新
            </button>
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
  background: #ffffff;
  border-radius: 16px;
  width: 100%;
  max-width: 540px;
  aspect-ratio: 4 / 3;
  max-height: 88vh;
  box-shadow:
    0 32px 100px rgba(0, 0, 0, 0.14),
    0 12px 32px rgba(0, 0, 0, 0.1);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container.is-mobile {
  max-width: 400px;
  aspect-ratio: 9 / 16;
  border-radius: 20px;
}

/* Header - 紧凑 */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  flex-shrink: 0;
}

.modal-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-icon {
  width: 28px;
  height: 28px;
  background: #0071e3;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-icon svg {
  width: 15px;
  height: 15px;
  color: #fff;
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
  letter-spacing: -0.01em;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: #86868b;
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

.modal-close svg {
  width: 12px;
  height: 12px;
}

/* Loading */
.modal-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 14px;
  padding: 60px 24px;
  color: #86868b;
  font-size: 14px;
  flex: 1;
}

.loading-spinner {
  width: 28px;
  height: 28px;
  border: 2px solid #f5f5f7;
  border-top-color: #0071e3;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Body - 主要内容区域 */
.modal-body {
  padding: 0 24px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  flex: 1;
  overflow-y: auto;
}

.version-row {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  background: #f5f5f7;
  border-radius: 12px;
}

.version-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-item:not(:last-child)::after {
  content: '';
  width: 1px;
  height: 20px;
  background: rgba(0, 0, 0, 0.08);
  margin-left: 8px;
}

.version-label {
  font-size: 12px;
  color: #86868b;
  font-weight: 500;
}

.version-value {
  font-size: 14px;
  color: #1d1d1f;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.version-value.is-new {
  color: #0071e3;
}

/* Status Badge */
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  padding: 7px 14px;
  background: rgba(0, 113, 227, 0.08);
  border-radius: 100px;
  font-size: 13px;
  font-weight: 500;
  color: #0071e3;
}

.status-badge svg {
  width: 14px;
  height: 14px;
}

.status-badge.is-updated {
  background: rgba(52, 199, 89, 0.1);
  color: #34c759;
}

/* Changelog */
.changelog {
  margin-top: 2px;
}

.changelog-title {
  font-size: 13px;
  color: #86868b;
  font-weight: 500;
  margin-bottom: 10px;
}

.changelog-content {
  font-size: 14px;
  color: #1d1d1f;
  line-height: 1.6;
  white-space: pre-wrap;
  background: #f5f5f7;
  padding: 16px 18px;
  border-radius: 12px;
  max-height: 160px;
  overflow-y: auto;
}

.changelog-content::-webkit-scrollbar {
  width: 5px;
}

.changelog-content::-webkit-scrollbar-track {
  background: transparent;
}

.changelog-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}

/* Error */
.modal-error {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 24px;
  color: #86868b;
  font-size: 14px;
  flex: 1;
}

/* Footer - 紧凑 */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  flex-shrink: 0;
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

.btn-primary:hover {
  background: #0077ed;
}

/* Transitions */
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

/* Mobile */
@media (max-width: 480px) {
  .modal-container {
    max-width: 92vw;
    border-radius: 16px;
  }
  .modal-container.is-mobile {
    aspect-ratio: 9 / 16;
  }
  .modal-body {
    padding: 0 20px 20px;
    gap: 14px;
  }
  .version-row {
    flex-wrap: wrap;
    gap: 10px;
    padding: 12px 14px;
  }
  .version-item:not(:last-child)::after {
    display: none;
  }
  .changelog-content {
    max-height: 140px;
  }
}
</style>
