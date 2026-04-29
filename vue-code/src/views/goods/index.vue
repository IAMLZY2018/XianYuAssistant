<script setup lang="ts">
import { onMounted } from 'vue'
import { useGoodsManager } from './useGoodsManager'
import './goods.css'

import IconShoppingBag from '@/components/icons/IconShoppingBag.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconFilter from '@/components/icons/IconFilter.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'

import GoodsTable from './components/GoodsTable.vue'
import GoodsDetail from './components/GoodsDetail.vue'

const {
  loading,
  refreshing,
  syncing,
  syncProgress,
  accounts,
  selectedAccountId,
  statusFilter,
  goodsList,
  currentPage,
  pageSize,
  total,
  totalPages,
  dialogs,
  selectedGoodsId,
  deleteTarget,
  loadAccounts,
  loadGoods,
  handleRefresh,
  handleAccountChange,
  handleStatusFilter,
  handlePageChange,
  viewDetail,
  configAutoDelivery,
  toggleAutoDelivery,
  toggleAutoReply,
  confirmDelete,
  executeDelete
} = useGoodsManager()

onMounted(() => {
  loadAccounts()
})

// 分页按钮列表
const getPageButtons = () => {
  const buttons: number[] = []
  const maxVisible = 5
  let start = Math.max(1, currentPage.value - Math.floor(maxVisible / 2))
  const end = Math.min(totalPages.value, start + maxVisible - 1)
  start = Math.max(1, end - maxVisible + 1)
  for (let i = start; i <= end; i++) {
    buttons.push(i)
  }
  return buttons
}
</script>

<template>
  <div class="goods">
    <!-- Header -->
    <div class="goods__header">
      <div class="goods__title-row">
        <div class="goods__title-icon">
          <IconShoppingBag />
        </div>
        <h1 class="goods__title">商品管理</h1>
      </div>
      <div class="goods__actions">
        <button
          class="btn btn--primary"
          :class="{ 'btn--loading': refreshing || syncing }"
          :disabled="refreshing || syncing || !selectedAccountId"
          @click="handleRefresh"
        >
          <IconRefresh />
          <span class="mobile-hidden">同步闲鱼商品</span>
        </button>
        <div v-if="syncing && syncProgress" class="goods__sync-progress">
          <span class="goods__sync-text">
            详情同步: {{ syncProgress.completedCount }}/{{ syncProgress.totalCount }}
          </span>
          <div class="goods__sync-bar">
            <div 
              class="goods__sync-bar-fill" 
              :style="{ width: `${(syncProgress.completedCount / syncProgress.totalCount) * 100}%` }"
            ></div>
          </div>
        </div>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="goods__filter-bar">
      <!-- Account Select -->
      <div class="goods__select-wrap">
        <select
          v-model="selectedAccountId"
          class="goods__select"
          @change="handleAccountChange"
        >
          <option :value="null" disabled>选择账号</option>
          <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
            {{ acc.accountNote || acc.unb }}
          </option>
        </select>
        <span class="goods__select-icon">
          <IconChevronDown />
        </span>
      </div>

      <!-- Status Filter -->
      <div class="goods__select-wrap">
        <select
          v-model="statusFilter"
          class="goods__select"
          @change="handleStatusFilter"
        >
          <option value="">全部状态</option>
          <option value="0">在售</option>
          <option value="1">已下架</option>
          <option value="2">已售出</option>
        </select>
        <span class="goods__select-icon">
          <IconChevronDown />
        </span>
      </div>

      <!-- Count -->
      <span v-if="total > 0" class="goods__count">
        共 {{ total }} 件
      </span>
    </div>

    <!-- Content Card -->
    <div class="goods__content">
      <!-- Toolbar -->
      <div class="goods__toolbar">
        <span class="goods__list-title">商品列表</span>
        <span v-if="goodsList.length > 0" class="goods__count">
          {{ (currentPage - 1) * pageSize + 1 }}-{{ Math.min(currentPage * pageSize, total) }} / {{ total }}
        </span>
      </div>

      <!-- Table/Cards -->
      <div class="goods__table-wrap">
        <GoodsTable
          :goods-list="goodsList"
          :loading="loading"
          @view="viewDetail"
          @toggle-auto-delivery="toggleAutoDelivery"
          @toggle-auto-reply="toggleAutoReply"
          @config-auto-delivery="configAutoDelivery"
          @delete="confirmDelete"
        />
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="goods__pagination">
        <button
          class="goods__page-btn"
          :class="{ 'goods__page-btn--disabled': currentPage <= 1 }"
          @click="handlePageChange(currentPage - 1)"
        >
          <IconChevronLeft />
        </button>

        <template v-for="page in getPageButtons()" :key="page">
          <button
            class="goods__page-btn"
            :class="{ 'goods__page-btn--active': page === currentPage }"
            @click="handlePageChange(page)"
          >
            {{ page }}
          </button>
        </template>

        <button
          class="goods__page-btn"
          :class="{ 'goods__page-btn--disabled': currentPage >= totalPages }"
          @click="handlePageChange(currentPage + 1)"
        >
          <IconChevronRight />
        </button>

        <span class="goods__page-info">{{ currentPage }} / {{ totalPages }}</span>
      </div>
    </div>

    <!-- Detail Dialog -->
    <GoodsDetail
      v-model="dialogs.detail"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
      @refresh="loadGoods"
    />

    <!-- Delete Confirm -->
    <Transition name="overlay-fade">
      <div v-if="dialogs.deleteConfirm" class="goods__dialog-overlay" @click.self="dialogs.deleteConfirm = false">
        <div class="goods__dialog">
          <div class="goods__dialog-header">
            <h3 class="goods__dialog-title">删除商品</h3>
          </div>
          <div class="goods__dialog-body">
            <p class="goods__dialog-text">
              确定要删除「{{ deleteTarget?.title }}」吗？此操作不可恢复。
            </p>
          </div>
          <div class="goods__dialog-footer">
            <button
              class="goods__dialog-btn goods__dialog-btn--cancel"
              @click="dialogs.deleteConfirm = false"
            >
              取消
            </button>
            <button
              class="goods__dialog-btn goods__dialog-btn--danger"
              @click="executeDelete"
            >
              删除
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
