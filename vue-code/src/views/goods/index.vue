<script setup lang="ts">
import { onMounted, ref, computed, inject, defineComponent, h } from 'vue'
import { useGoodsManager } from './useGoodsManager'
import './goods.css'
import '@/styles/header-selectors.css'

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
  executeDelete,
  getGoodsStatusText,
  formatPrice,
  formatTime,
  syncSingleGoods
} = useGoodsManager()

// 下拉刷新相关状态
const pullRefreshState = ref<'idle' | 'pulling' | 'ready' | 'refreshing'>('idle')
const pullDistance = ref(0)
const tableWrapRef = ref<HTMLElement | null>(null)
const startY = ref(0)
const isMobile = ref(false)

// 检测是否为手机模式
const checkMobile = () => {
  isMobile.value = window.innerWidth <= 768
}

// 计算下拉刷新的显示距离
const pullRefreshDistance = computed(() => {
  const maxDistance = 80
  return Math.min(pullDistance.value, maxDistance)
})

// 处理触摸开始
const handleTouchStart = (e: TouchEvent) => {
  if (!isMobile.value || !tableWrapRef.value) return
  
  // 只在列表顶部时才允许下拉
  if (tableWrapRef.value.scrollTop === 0) {
    startY.value = e.touches[0]?.clientY ?? 0
    pullDistance.value = 0
    pullRefreshState.value = 'idle'
  }
}

// 处理触摸移动
const handleTouchMove = (e: TouchEvent) => {
  if (!isMobile.value || !tableWrapRef.value || startY.value === 0) return
  
  if (tableWrapRef.value.scrollTop === 0) {
    const currentY = e.touches[0]?.clientY ?? 0
    const distance = currentY - startY.value
    
    if (distance > 0) {
      e.preventDefault()
      pullDistance.value = distance
      
      if (distance < 60) {
        pullRefreshState.value = 'pulling'
      } else {
        pullRefreshState.value = 'ready'
      }
    }
  }
}

// 处理触摸结束
const handleTouchEnd = async () => {
  if (!isMobile.value) return
  
  if (pullRefreshState.value === 'ready' && pullDistance.value >= 60) {
    pullRefreshState.value = 'refreshing'
    await handleRefresh()
    // 动画反弹
    pullDistance.value = 0
    pullRefreshState.value = 'idle'
  } else {
    // 回弹动画
    pullDistance.value = 0
    pullRefreshState.value = 'idle'
  }
  
  startY.value = 0
}

// 获取导航栏内容设置函数
const setHeaderContent = inject<(content: any) => void>('setHeaderContent')

// 创建导航栏选择器组件
const HeaderSelectors = defineComponent({
  setup() {
    return () => h('div', { class: 'header-selectors' }, [
      h('div', { class: 'header-select-wrap' }, [
        h('select', {
          class: 'header-select',
          value: selectedAccountId.value,
          onChange: (e: Event) => {
            const target = e.target as HTMLSelectElement
            selectedAccountId.value = target.value ? parseInt(target.value) : null
            handleAccountChange()
          }
        }, [
          h('option', { value: '', disabled: true }, '选择账号'),
          ...accounts.value.map(acc => 
            h('option', { value: acc.id.toString() }, acc.accountNote || acc.unb)
          )
        ]),
        h(IconChevronDown, { class: 'header-select-icon' })
      ]),
      h('div', { class: 'header-select-wrap' }, [
        h('select', {
          class: 'header-select',
          value: statusFilter.value,
          onChange: (e: Event) => {
            const target = e.target as HTMLSelectElement
            statusFilter.value = target.value
            handleStatusFilter()
          }
        }, [
          h('option', { value: '' }, '全部状态'),
          h('option', { value: '0' }, '在售'),
          h('option', { value: '1' }, '已下架'),
          h('option', { value: '2' }, '已售出')
        ]),
        h(IconChevronDown, { class: 'header-select-icon' })
      ]),
      h('button', {
        class: ['header-refresh-btn', { 'header-refresh-btn--loading': refreshing.value || syncing.value }],
        disabled: refreshing.value || syncing.value || !selectedAccountId.value,
        onClick: handleRefresh
      }, [
        h(IconRefresh, { class: 'header-refresh-icon' })
      ])
    ])
  }
})

onMounted(() => {
  loadAccounts()
  checkMobile()
  window.addEventListener('resize', checkMobile)
  
  // 只在手机模式下设置导航栏内容
  if (setHeaderContent) {
    setHeaderContent(HeaderSelectors)
  }
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
      <div class="goods__title-row desktop-only">
        <div class="goods__title-icon">
          <IconShoppingBag />
        </div>
        <h1 class="goods__title">商品管理</h1>
      </div>

      <div class="goods__actions">
        <template v-if="!isMobile">
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
        </template>

        <button
          class="btn btn--primary desktop-only"
          :class="{ 'btn--loading': refreshing || syncing }"
          :disabled="refreshing || syncing || !selectedAccountId"
          @click="handleRefresh"
        >
          <IconRefresh />
          <span class="mobile-hidden">同步闲鱼商品</span>
        </button>

        <span v-if="total > 0 && !isMobile" class="goods__count">
          共 {{ total }} 件
        </span>

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

    <!-- Content Card -->
    <div class="goods__content">
      <!-- Pull Refresh Indicator (Mobile Only) -->
      <div 
        v-if="isMobile && pullDistance > 0"
        class="goods__pull-refresh"
        :style="{ height: `${pullRefreshDistance}px` }"
        :class="{
          'goods__pull-refresh--pulling': pullRefreshState === 'pulling',
          'goods__pull-refresh--ready': pullRefreshState === 'ready',
          'goods__pull-refresh--refreshing': pullRefreshState === 'refreshing'
        }"
      >
        <div class="goods__pull-refresh-content">
          <div class="goods__pull-refresh-icon">
            <IconRefresh />
          </div>
          <div class="goods__pull-refresh-text">
            {{ pullRefreshState === 'pulling' ? '下拉刷新' : pullRefreshState === 'ready' ? '释放刷新' : '刷新中...' }}
          </div>
        </div>
      </div>

      <!-- Table/Cards -->
      <div 
        ref="tableWrapRef"
        class="goods__table-wrap"
        @touchstart="handleTouchStart"
        @touchmove="handleTouchMove"
        @touchend="handleTouchEnd"
      >
        <GoodsTable
          :goods-list="goodsList"
          :loading="loading"
          @view="viewDetail"
          @sync="syncSingleGoods"
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
