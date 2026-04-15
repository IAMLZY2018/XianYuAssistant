<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ConnectionDetail from './components/ConnectionDetail.vue'

const route = useRoute()
const router = useRouter()

const accountId = computed(() => Number(route.params.id) || null)

const handleBack = () => {
  router.push('/connection')
}
</script>

<template>
  <div class="detail-route">
    <header class="detail-route__header">
      <button class="detail-route__back" @click="handleBack">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
          <path d="M19 12H5"/><path d="M12 19l-7-7 7-7"/>
        </svg>
        <span>返回</span>
      </button>
      <h1 class="detail-route__title">连接详情</h1>
    </header>
    <div class="detail-route__body">
      <ConnectionDetail
        :account-id="accountId"
        :is-mobile="true"
      />
    </div>
  </div>
</template>

<style scoped>
.detail-route {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: transparent;
  overflow: hidden;
}

.detail-route__header {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
}

.detail-route__back {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #007aff;
  cursor: pointer;
  background: none;
  border: none;
  padding: 4px;
  -webkit-tap-highlight-color: transparent;
}

.detail-route__back svg {
  width: 20px;
  height: 20px;
}

.detail-route__title {
  font-size: 17px;
  font-weight: 600;
  color: #1d1d1f;
  margin: 0;
  letter-spacing: -0.01em;
}

.detail-route__body {
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
</style>
