<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { checkUpdate } from '@/api/system'
import IconClose from '@/components/icons/IconClose.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'

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

const formattedDate = computed(() => {
  if (!updateInfo.value?.publishedAt) return ''
  const d = new Date(updateInfo.value.publishedAt)
  return `${d.getFullYear()}.${String(d.getMonth() + 1).padStart(2, '0')}.${String(d.getDate()).padStart(2, '0')}`
})

const open = async () => {
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
        <div class="modal-container">
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
            <!-- Version Info -->
            <div class="version-grid">
              <div class="version-item">
                <span class="version-label">当前版本</span>
                <span class="version-value">v{{ updateInfo.currentVersion }}</span>
              </div>
              <div class="version-item">
                <span class="version-label">最新版本</span>
                <span class="version-value" :class="{ 'is-new': updateInfo.hasUpdate }">
                  v{{ updateInfo.latestVersion }}
                </span>
              </div>
              <div v-if="formattedDate" class="version-item">
                <span class="version-label">发布时间</span>
                <span class="version-value">{{ formattedDate }}</span>
              </div>
            </div>

            <!-- Status Badge -->
            <div class="status-badge" :class="{ 'is-updated': !updateInfo.hasUpdate }">
              <IconCheck v-if="!updateInfo.hasUpdate" />
              <span>{{ updateInfo.hasUpdate ? '发现新版本' : '已是最新版本' }}</span>
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
            <button v-if="updateInfo.hasUpdate" class="btn btn-primary" @click="openDownload">
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
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 20px;
}

.modal-container {
  background: #ffffff;
  border-radius: 20px;
  width: 100%;
  max-width: 420px;
  box-shadow:
    0 24px 80px rgba(0, 0, 0, 0.12),
    0 8px 24px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

/* Header */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
}

.modal-title-wrap {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-icon {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #0071e3 0%, #64d2ff 100%);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.modal-icon svg {
  width: 18px;
  height: 18px;
  color: #fff;
}

.modal-title {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
  letter-spacing: -0.02em;
}

.modal-close {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: transparent;
  color: #86868b;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s ease;
}

.modal-close:hover {
  background: #f5f5f7;
  color: #1d1d1f;
}

.modal-close svg {
  width: 14px;
  height: 14px;
}

/* Loading */
.modal-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 48px 24px;
  color: #86868b;
  font-size: 14px;
}

.loading-spinner {
  width: 32px;
  height: 32px;
  border: 2.5px solid #f5f5f7;
  border-top-color: #0071e3;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Body */
.modal-body {
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.version-grid {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #f5f5f7;
  border-radius: 10px;
}

.version-label {
  font-size: 13px;
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
  padding: 6px 14px;
  background: rgba(0, 113, 227, 0.08);
  border-radius: 20px;
  font-size: 13px;
  font-weight: 500;
  color: #0071e3;
}

.status-badge svg {
  width: 14px;
  height: 14px;
}

.status-badge.is-updated {
  background: rgba(52, 199, 89, 0.08);
  color: #34c759;
}

/* Changelog */
.changelog {
  margin-top: 4px;
}

.changelog-title {
  font-size: 13px;
  color: #86868b;
  font-weight: 500;
  margin-bottom: 10px;
}

.changelog-content {
  font-size: 13px;
  color: #1d1d1f;
  line-height: 1.65;
  white-space: pre-wrap;
  background: #f5f5f7;
  padding: 14px 16px;
  border-radius: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.changelog-content::-webkit-scrollbar {
  width: 4px;
}

.changelog-content::-webkit-scrollbar-track {
  background: transparent;
}

.changelog-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 2px;
}

/* Error */
.modal-error {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  color: #86868b;
  font-size: 14px;
}

/* Footer */
.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
  background: #fafafa;
}

.btn {
  padding: 8px 20px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  border: none;
}

.btn-secondary {
  background: #f5f5f7;
  color: #1d1d1f;
}

.btn-secondary:hover {
  background: #e8e8ed;
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
  transition: opacity 0.25s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.96);
  opacity: 0;
}
</style>
