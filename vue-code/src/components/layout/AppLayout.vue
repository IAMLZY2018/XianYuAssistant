<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import NavMenu from './NavMenu.vue'

const route = useRoute()

const isMobile = ref(false)
const drawerVisible = ref(false)

const pageTitleMap: Record<string, string> = {
  '/dashboard': '仪表板',
  '/accounts': '闲鱼账号',
  '/connection': '连接管理',
  '/goods': '商品管理',
  '/orders': '订单管理',
  '/messages': '消息管理',
  '/auto-delivery': '自动发货',
  '/auto-reply': '自动回复',
  '/operation-log': '操作日志',
  '/settings': '系统设置'
}

const currentPageTitle = computed(() => pageTitleMap[route.path] || '闲鱼自动化')

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

const toggleDrawer = () => {
  drawerVisible.value = !drawerVisible.value
}

const closeDrawer = () => {
  drawerVisible.value = false
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize)
})
</script>

<template>
  <div class="app-layout">
    <!-- 手机端: 顶部导航栏 -->
    <div v-if="isMobile" class="mobile-header">
      <el-button class="menu-toggle-btn" @click="toggleDrawer" circle>
        <span class="menu-icon">{{ drawerVisible ? '✕' : '☰' }}</span>
      </el-button>
      <div class="mobile-page-title">{{ currentPageTitle }}</div>
    </div>

    <!-- 手机端: 全屏菜单 -->
    <div v-if="isMobile && drawerVisible" class="mobile-menu-overlay" @click="closeDrawer">
      <div class="mobile-menu" @click.stop>
        <div class="mobile-menu-header">
          <div class="logo">
            <div class="logo-icon">闲</div>
            <div class="logo-text">自动化管理</div>
          </div>
        </div>
        <div class="mobile-menu-content">
          <NavMenu @select="closeDrawer" />
        </div>
      </div>
    </div>

    <!-- 电脑端: 固定侧边栏 -->
    <el-container v-if="!isMobile" class="layout-container">
      <el-aside width="240px" class="sidebar">
        <div class="logo">
          <div class="logo-icon">闲</div>
          <div class="logo-text">自动化管理</div>
        </div>
        <NavMenu />
      </el-aside>

      <el-container>
        <el-main>
          <RouterView />
        </el-main>
      </el-container>
    </el-container>

    <!-- 手机端: 主内容区 -->
    <el-container v-if="isMobile" class="layout-container">
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </div>
</template>

<style scoped>
.app-layout {
  height: 100vh;
  background: #e8e8e8;
}

.layout-container {
  height: 100%;
}

/* ========== 电脑端: 固定侧边栏 ========== */
.sidebar {
  background: #f8f8f8;
  border-right: 1px solid #d4d4d4;
  box-shadow: none;
}

.logo {
  display: flex;
  align-items: center;
  padding: 20px 24px;
  border-bottom: none;
  gap: 12px;
}

.logo-icon {
  width: 32px;
  height: 32px;
  background: #2a2a2a;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 18px;
  font-weight: bold;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

:deep(.el-menu-item) {
  margin: 2px 16px;
  border-radius: 8px;
  color: #666666;
  transition: all 0.2s;
}

:deep(.el-menu-item:hover) {
  background: #ececec !important;
  color: #1a1a1a;
}

:deep(.el-menu-item.is-active) {
  background: #1a1a1a !important;
  color: white;
}

.el-main {
  padding: 32px 40px;
  overflow: hidden;
  background: #e8e8e8;
}

:deep(.el-divider__text) {
  font-size: 12px;
  color: #999999;
  text-transform: uppercase;
  font-weight: 600;
}

:deep(.el-divider) {
  margin: 24px 0 8px;
  border-color: transparent;
}

/* ========== 手机端: 顶部导航栏 ========== */
.mobile-header {
  display: flex;
  justify-content: flex-start;
  align-items: center;
  padding: 12px 16px;
  background: #f8f8f8;
  border-bottom: 1px solid #d4d4d4;
  position: sticky;
  top: 0;
  z-index: 100;
  gap: 12px;
}

.mobile-page-title {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a1a;
  flex: 1;
}

.menu-toggle-btn {
  width: 40px;
  height: 40px;
  background: #2a2a2a !important;
  border-color: #2a2a2a !important;
  color: white !important;
  font-size: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.menu-toggle-btn:hover {
  background: #1a1a1a !important;
  border-color: #1a1a1a !important;
}

.menu-icon {
  font-size: 20px;
  line-height: 1;
}

/* ========== 手机端: 全屏菜单 ========== */
.mobile-menu-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

.mobile-menu {
  width: 90%;
  max-width: 320px;
  max-height: 80vh;
  background: #f8f8f8;
  border-radius: 16px;
  overflow: hidden;
  animation: slideUp 0.3s ease;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

.mobile-menu-header {
  flex-shrink: 0;
  border-bottom: 1px solid #e8e8e8;
}

.mobile-menu-header .logo {
  padding: 20px 24px;
}

.mobile-menu-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.mobile-menu-content::-webkit-scrollbar {
  width: 0;
  height: 0;
}

@keyframes slideUp {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* ========== 响应式适配 ========== */
@media (max-width: 768px) {
  .mobile-header {
    padding: 10px 12px;
  }

  .menu-toggle-btn {
    width: 36px;
    height: 36px;
  }

  .menu-icon {
    font-size: 18px;
  }

  .el-main {
    padding: 12px 16px;
  }

  :deep(.el-menu-item) {
    margin: 2px 12px;
  }

  :deep(.el-menu-item span) {
    font-size: 14px;
  }
}

@media (max-width: 480px) {
  .mobile-header {
    padding: 8px 10px;
  }

  .menu-toggle-btn {
    width: 32px;
    height: 32px;
  }

  .menu-icon {
    font-size: 16px;
  }

  .el-main {
    padding: 10px 12px;
  }

  :deep(.el-menu-item) {
    margin: 2px 8px;
  }

  :deep(.el-menu-item span) {
    font-size: 13px;
  }
}
</style>
