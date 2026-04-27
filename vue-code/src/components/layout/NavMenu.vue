<script setup lang="ts">
/**
 * 导航菜单组件 - 电脑端侧边栏和手机端抽屉共用
 */

const emit = defineEmits<{
  select: [index: string]
}>()

const onSelect = (index: string) => {
  emit('select', index)
}
</script>

<template>
  <el-menu
    :default-active="$route.path"
    router
    class="nav-menu"
    @select="onSelect"
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
    <el-menu-item index="/messages">
      <span>💬 消息管理</span>
    </el-menu-item>

    <el-divider content-position="left">自动化</el-divider>

    <el-menu-item index="/kami-config">
      <span>🔑 卡密仓库</span>
    </el-menu-item>
    <el-menu-item index="/auto-delivery">
      <span>🤖 自动发货</span>
    </el-menu-item>
    <el-menu-item index="/orders">
      <span>📋 发货记录</span>
    </el-menu-item>
    <el-menu-item index="/auto-reply">
      <span>💭 自动回复</span>
    </el-menu-item>

    <el-divider content-position="left">系统</el-divider>

    <el-menu-item index="/operation-log">
      <span>📜 操作日志</span>
    </el-menu-item>
    <el-menu-item index="/settings">
      <span>⚙️ 系统设置</span>
    </el-menu-item>
  </el-menu>
</template>

<style scoped>
.nav-menu {
  border-right: none;
  background: transparent;
}
</style>
