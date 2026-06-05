<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="login-layout">
    <RouterView />
  </div>
</template>

<style scoped>
.login-layout {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  background: #e8e8e8;
}
</style>
