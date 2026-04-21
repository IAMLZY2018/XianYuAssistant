<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { useRoute } from 'vue-router'
import AppLayout from '@/components/layout/AppLayout.vue'
import LoginLayout from '@/components/layout/LoginLayout.vue'

const route = useRoute()

// 等路由首次就绪后再渲染，避免初始帧布局闪烁
const isReady = ref(false)
watch(() => route.path, () => {
  if (!isReady.value) isReady.value = true
}, { immediate: true })

const isLoginPage = computed(() => route.path === '/login')
</script>

<template>
  <template v-if="isReady">
    <LoginLayout v-if="isLoginPage" />
    <AppLayout v-else />
  </template>
</template>

<style>
/* 全局样式：防止弹窗打开时滚动条导致的页面抖动 */
html {
  overflow-y: scroll;
}

body {
  overflow-x: hidden;
}

/* Element Plus 弹窗遮罩层样式优化 */
.el-overlay {
  overflow-y: scroll !important;
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

.el-overlay::-webkit-scrollbar {
  width: 0 !important;
  height: 0 !important;
}
</style>
