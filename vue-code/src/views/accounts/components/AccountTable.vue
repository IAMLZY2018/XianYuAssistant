<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getAccountStatusText, formatTime } from '@/utils'
import type { Account } from '@/types'

interface Props {
  accounts: Account[]
  loading?: boolean
}

interface Emits {
  (e: 'edit', account: Account): void
  (e: 'delete', id: number): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

// 响应式检测
const isMobile = ref(false)
const checkScreenSize = () => {
  // 手机端使用卡片视图，电脑端使用表格视图
  isMobile.value = window.innerWidth < 768
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
  <!-- 手机端: 卡片视图 -->
  <div v-if="isMobile" class="mobile-account-list" v-loading="loading">
    <div
      v-for="account in accounts"
      :key="account.id"
      class="mobile-account-card"
    >
      <div class="card-header">
        <div class="account-avatar">
          {{ (account.accountNote || account.unb || '未').charAt(0) }}
        </div>
        <div class="account-basic">
          <div class="account-name">{{ account.accountNote || '未命名账号' }}</div>
          <div class="account-unb">UNB: {{ account.unb }}</div>
        </div>
        <el-tag :type="getAccountStatusText(account.status).type" size="small">
          {{ getAccountStatusText(account.status).text }}
        </el-tag>
      </div>

      <div class="card-body">
        <div class="info-row">
          <span class="info-label">ID:</span>
          <span class="info-value">{{ account.id }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">创建时间:</span>
          <span class="info-value">{{ formatTime(account.createdTime) }}</span>
        </div>
        <div class="info-row">
          <span class="info-label">更新时间:</span>
          <span class="info-value">{{ formatTime(account.updatedTime) }}</span>
        </div>
      </div>

      <div class="card-footer">
        <el-button size="small" @click="emit('edit', account)" class="action-btn">
          ✏️ 编辑
        </el-button>
        <el-button size="small" type="danger" @click="emit('delete', account.id)" class="action-btn">
          🗑️ 删除
        </el-button>
      </div>
    </div>

    <el-empty v-if="!loading && accounts.length === 0" description="暂无账号数据" />
  </div>

  <!-- 电脑端/平板端: 表格视图 -->
  <el-table v-else :data="accounts" v-loading="loading" stripe>
    <el-table-column prop="id" label="ID" width="80" />
    <el-table-column prop="unb" label="UNB" min-width="150" />
    <el-table-column prop="accountNote" label="账号备注" min-width="150">
      <template #default="{ row }">
        {{ row.accountNote || '未命名账号' }}
      </template>
    </el-table-column>
    <el-table-column prop="status" label="状态" width="100">
      <template #default="{ row }">
        <el-tag :type="getAccountStatusText(row.status).type">
          {{ getAccountStatusText(row.status).text }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column prop="createdTime" label="创建时间" width="180">
      <template #default="{ row }">
        {{ formatTime(row.createdTime) }}
      </template>
    </el-table-column>
    <el-table-column prop="updatedTime" label="更新时间" width="180">
      <template #default="{ row }">
        {{ formatTime(row.updatedTime) }}
      </template>
    </el-table-column>
    <el-table-column label="操作" width="180" fixed="right">
      <template #default="{ row }">
        <el-button size="small" @click="emit('edit', row)">编辑</el-button>
        <el-button size="small" type="danger" @click="emit('delete', row.id)">删除</el-button>
      </template>
    </el-table-column>

    <template #empty>
      <el-empty description="暂无账号数据" />
    </template>
  </el-table>
</template>

<style scoped>
/* 手机端卡片样式 */
.mobile-account-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  padding-bottom: 24px;  /* 增加底部padding，确保最后一个卡片完全显示 */
  height: 100%;
  overflow-y: auto;
}

/* 自定义滚动条样式 - 显示细滚动条 */
.mobile-account-list::-webkit-scrollbar {
  width: 6px;
}

.mobile-account-list::-webkit-scrollbar-track {
  background: transparent;
}

.mobile-account-list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 3px;
}

.mobile-account-list::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.3);
}

.mobile-account-card {
  background: white;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  transition: all 0.3s ease;
  flex-shrink: 0;  /* 防止卡片被压缩 */
}

.mobile-account-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  transform: translateY(-2px);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

.account-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.account-basic {
  flex: 1;
  min-width: 0;
}

.account-name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-unb {
  font-size: 12px;
  color: #909399;
}

.card-body {
  margin-bottom: 12px;
}

.info-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  font-size: 13px;
}

.info-label {
  color: #909399;
  flex-shrink: 0;
}

.info-value {
  color: #606266;
  text-align: right;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-left: 12px;
}

.card-footer {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid #f0f0f0;
}

.action-btn {
  flex: 1;
}

/* 响应式优化 */
@media (max-width: 480px) {
  .mobile-account-list {
    padding: 12px;
    padding-bottom: 20px;
    gap: 10px;
  }

  .mobile-account-card {
    padding: 14px;
  }

  .account-avatar {
    width: 44px;
    height: 44px;
    font-size: 18px;
  }

  .account-name {
    font-size: 15px;
  }
}
</style>
