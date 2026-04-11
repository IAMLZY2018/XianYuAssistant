<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'

const route = useRoute()

// 响应式导航栏状态
const isMobile = ref(false)
const isTablet = ref(false)
const drawerVisible = ref(false)
const screenWidth = ref(window.innerWidth)

// 页面标题映射
const pageTitleMap: Record<string, string> = {
  '/dashboard': '仪表板',
  '/accounts': '闲鱼账号',
  '/connection': '连接管理',
  '/goods': '商品管理',
  '/orders': '订单管理',
  '/messages': '消息管理',
  '/auto-delivery': '自动发货',
  '/auto-reply': '自动回复',
  '/operation-log': '操作日志'
}

// 当前页面标题
const currentPageTitle = computed(() => {
  return pageTitleMap[route.path] || '闲鱼自动化'
})

// 检测屏幕宽度
const checkScreenSize = () => {
  screenWidth.value = window.innerWidth
  isMobile.value = window.innerWidth < 768
  isTablet.value = window.innerWidth >= 768 && window.innerWidth < 1024
}

// 切换抽屉显示
const toggleDrawer = () => {
  drawerVisible.value = !drawerVisible.value
}

// 关闭抽屉
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
  <div class="app-container">
    <!-- 手机端/平板端: 顶部导航栏 -->
    <div v-if="isMobile || isTablet" class="mobile-header">
      <el-button
        class="menu-toggle-btn"
        @click="toggleDrawer"
        circle
      >
        <span class="menu-icon">{{ drawerVisible ? '✕' : '☰' }}</span>
      </el-button>
      <div class="mobile-page-title">{{ currentPageTitle }}</div>
    </div>

    <!-- 抽屉式导航 (平板端) -->
    <el-drawer
      v-if="isTablet"
      v-model="drawerVisible"
      direction="ltr"
      :with-header="false"
      size="280px"
      class="nav-drawer"
    >
      <div class="drawer-content">
        <div class="logo">
          <div class="logo-icon">闲</div>
          <div class="logo-text">自动化管理</div>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          class="nav-menu"
          @select="closeDrawer"
        >
          <el-menu-item index="/dashboard">
            <span>📊 仪表板</span>
          </el-menu-item>
          <el-menu-item index="/accounts">
            <span>👤 闲鱼账号</span>
          </el-menu-item>
          <el-menu-item index="/connection">
            <span>🔗 连接管理</span>
          </el-menu-item>
          <el-menu-item index="/goods">
            <span>📦 商品管理</span>
          </el-menu-item>
          <el-menu-item index="/orders">
            <span>📋 订单管理</span>
          </el-menu-item>
          <el-menu-item index="/messages">
            <span>💬 消息管理</span>
          </el-menu-item>
          
          <el-divider content-position="left">自动化</el-divider>
          
          <el-menu-item index="/auto-delivery">
            <span>🤖 自动发货</span>
          </el-menu-item>
          <el-menu-item index="/auto-reply">
            <span>💭 自动回复</span>
          </el-menu-item>
          
          <el-divider content-position="left">系统</el-divider>
          
          <el-menu-item index="/operation-log">
            <span>📜 操作日志</span>
          </el-menu-item>
        </el-menu>
      </div>
    </el-drawer>

    <!-- 全屏菜单 (手机端) -->
    <div v-if="isMobile && drawerVisible" class="mobile-menu-overlay" @click="closeDrawer">
      <div class="mobile-menu" @click.stop>
        <!-- 固定标题 -->
        <div class="mobile-menu-header">
          <div class="logo">
            <div class="logo-icon">闲</div>
            <div class="logo-text">自动化管理</div>
          </div>
        </div>
        <!-- 可滚动菜单内容 -->
        <div class="mobile-menu-content">
          <el-menu
            :default-active="$route.path"
            router
            class="nav-menu"
            @select="closeDrawer"
          >
            <el-menu-item index="/dashboard">
              <span>📊 仪表板</span>
            </el-menu-item>
            <el-menu-item index="/accounts">
              <span>👤 闲鱼账号</span>
            </el-menu-item>
            <el-menu-item index="/connection">
              <span>🔗 连接管理</span>
            </el-menu-item>
            <el-menu-item index="/goods">
              <span>📦 商品管理</span>
            </el-menu-item>
            <el-menu-item index="/orders">
              <span>📋 订单管理</span>
            </el-menu-item>
            <el-menu-item index="/messages">
              <span>💬 消息管理</span>
            </el-menu-item>
            
            <el-divider content-position="left">自动化</el-divider>
            
            <el-menu-item index="/auto-delivery">
              <span>🤖 自动发货</span>
            </el-menu-item>
            <el-menu-item index="/auto-reply">
              <span>💭 自动回复</span>
            </el-menu-item>
            
            <el-divider content-position="left">系统</el-divider>
            
            <el-menu-item index="/operation-log">
              <span>📜 操作日志</span>
            </el-menu-item>
          </el-menu>
        </div>
      </div>
    </div>

    <!-- 电脑端: 固定侧边栏 -->
    <el-container v-if="!isMobile && !isTablet">
      <el-aside width="240px" class="sidebar">
        <div class="logo">
          <div class="logo-icon">闲</div>
          <div class="logo-text">自动化管理</div>
        </div>
        <el-menu
          :default-active="$route.path"
          router
          class="nav-menu"
        >
          <el-menu-item index="/dashboard">
            <span>📊 仪表板</span>
          </el-menu-item>
          <el-menu-item index="/accounts">
            <span>👤 闲鱼账号</span>
          </el-menu-item>
          <el-menu-item index="/connection">
            <span>🔗 连接管理</span>
          </el-menu-item>
          <el-menu-item index="/goods">
            <span>📦 商品管理</span>
          </el-menu-item>
          <el-menu-item index="/orders">
            <span>📋 订单管理</span>
          </el-menu-item>
          <el-menu-item index="/messages">
            <span>💬 消息管理</span>
          </el-menu-item>
          
          <el-divider content-position="left">自动化</el-divider>
          
          <el-menu-item index="/auto-delivery">
            <span>🤖 自动发货</span>
          </el-menu-item>
          <el-menu-item index="/auto-reply">
            <span>💭 自动回复</span>
          </el-menu-item>
          
          <el-divider content-position="left">系统</el-divider>
          
          <el-menu-item index="/operation-log">
            <span>📜 操作日志</span>
          </el-menu-item>
        </el-menu>
      </el-aside>
      
      <el-container>
        <el-main>
          <RouterView />
        </el-main>
      </el-container>
    </el-container>

    <!-- 手机端/平板端: 主内容区 -->
    <el-container v-if="isMobile || isTablet">
      <el-main>
        <RouterView />
      </el-main>
    </el-container>
  </div>
</template>

<style scoped>
.app-container {
  height: 100vh;
  background: #e8e8e8;
}

.el-container {
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
  margin-right: 0;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.nav-menu {
  border-right: none;
  background: transparent;
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

/* ========== 手机端/平板端: 顶部导航栏 ========== */
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

/* ========== 平板端: 抽屉样式 ========== */
.drawer-content {
  height: 100%;
  background: #f8f8f8;
}

.nav-drawer :deep(.el-drawer__body) {
  padding: 0;
  background: #f8f8f8;
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
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.mobile-menu {
  width: 90%;
  max-width: 320px;
  max-height: 80vh;
  background: #f8f8f8;
  border-radius: 16px;
  overflow: hidden; /* 改为hidden,由内部元素控制滚动 */
  animation: slideUp 0.3s ease;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.2);
  display: flex;
  flex-direction: column;
}

/* 固定标题区域 */
.mobile-menu-header {
  flex-shrink: 0;
  border-bottom: 1px solid #e8e8e8;
}

.mobile-menu-header .logo {
  padding: 20px 24px;
}

/* 可滚动菜单内容区域 */
.mobile-menu-content {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* 隐藏滚动条但保留滚动功能 */
.mobile-menu-content::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.mobile-menu-content {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
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

/* 移除原来的 .mobile-menu .logo 样式,已在 .mobile-menu-header .logo 中定义 */

/* ========== 响应式适配 ========== */

/* 平板端 */
@media (max-width: 1024px) {
  .el-main {
    padding: 20px 24px;
  }
}

/* 手机端 */
@media (max-width: 768px) {
  .mobile-header {
    padding: 10px 12px;
  }

  .mobile-logo .logo-icon {
    width: 28px;
    height: 28px;
    font-size: 16px;
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

/* 小屏手机 */
@media (max-width: 480px) {
  .mobile-header {
    padding: 8px 10px;
  }

  .mobile-logo .logo-icon {
    width: 26px;
    height: 26px;
    font-size: 15px;
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