<script setup lang="ts">
import { RouterView } from 'vue-router'
</script>

<template>
  <div class="app-container">
    <el-container>
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
          <el-menu-item index="/records">
            <span>📝 操作记录</span>
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
  overflow: hidden; /* 禁用滚动条，由各页面内部处理 */
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

/* 响应式适配 */
@media (max-width: 768px) {
  .el-aside {
    width: 60px !important;
  }
  
  .logo-text {
    display: none;
  }
  
  .el-main {
    padding: 16px 20px;
  }
  
  :deep(.el-menu-item span) {
    font-size: 12px;
  }
}

@media (max-width: 480px) {
  .el-container {
    flex-direction: column;
  }
  
  .el-aside {
    width: 100% !important;
    height: auto !important;
  }
  
  .logo {
    justify-content: center;
  }
  
  .el-main {
    padding: 10px;
  }
}
</style>