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
html {
  overflow-y: scroll;
}

body {
  overflow-x: hidden;
}
</style>
