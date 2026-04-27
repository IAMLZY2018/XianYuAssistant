<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useOrderManager } from './useOrderManager'
import './orders.css'

import IconClipboard from '@/components/icons/IconClipboard.vue'
import IconSearch from '@/components/icons/IconSearch.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconFilter from '@/components/icons/IconFilter.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'

import OrderTable from './components/OrderTable.vue'

const {
  loading,
  orderList,
  total,
  accounts,
  queryParams,
  totalPages,
  loadAccounts,
  loadOrders,
  handleAccountChange,
  handleReset,
  handlePageChange,
  copySId,
  handleConfirmShipment
} = useOrderManager()

const showFilterSheet = ref(false)
const isMobile = ref(false)

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

onMounted(async () => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  await loadAccounts()
  loadOrders()
})

const filterGoodsId = ref('')

const openFilterSheet = () => {
  filterGoodsId.value = queryParams.xyGoodsId || ''
  showFilterSheet.value = true
}

const applyFilter = () => {
  queryParams.xyGoodsId = filterGoodsId.value || undefined
  queryParams.pageNum = 1
  showFilterSheet.value = false
  loadOrders()
}

const resetFilter = () => {
  filterGoodsId.value = ''
  handleReset()
  showFilterSheet.value = false
}

const getPageButtons = () => {
  const buttons: number[] = []
  const maxVisible = 5
  let start = Math.max(1, queryParams.pageNum! - Math.floor(maxVisible / 2))
  const end = Math.min(totalPages.value, start + maxVisible - 1)
  start = Math.max(1, end - maxVisible + 1)
  for (let i = start; i <= end; i++) {
    buttons.push(i)
  }
  return buttons
}

const showConfirmDialog = ref(false)
const confirmTargetOrder = ref<any>(null)

const openConfirmDialog = (order: any) => {
  confirmTargetOrder.value = order
  showConfirmDialog.value = true
}

const executeConfirmShipment = async () => {
  if (confirmTargetOrder.value) {
    await handleConfirmShipment(confirmTargetOrder.value)
  }
  showConfirmDialog.value = false
  confirmTargetOrder.value = null
}
</script>

<template>
  <div class="orders">
    <div class="orders__header">
      <div class="orders__title-row">
        <div class="orders__title-icon">
          <IconClipboard />
        </div>
        <h1 class="orders__title">发货记录</h1>
      </div>
      <div class="orders__actions">
        <div class="orders__select-wrap">
          <select
            v-model="queryParams.xianyuAccountId"
            class="orders__select"
            @change="handleAccountChange"
          >
            <option :value="undefined" disabled>选择账号</option>
            <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
              {{ acc.accountNote || acc.unb || `账号${acc.id}` }}
            </option>
          </select>
          <span class="orders__select-icon">
            <IconChevronDown />
          </span>
        </div>
        <button
          class="btn btn--secondary"
          :class="{ 'btn--loading': loading }"
          :disabled="loading"
          @click="loadOrders"
        >
          <IconRefresh />
          <span class="mobile-hidden">刷新</span>
        </button>
        <button v-if="isMobile" class="btn btn--secondary" @click="openFilterSheet">
          <IconFilter />
          <span>筛选</span>
        </button>
      </div>
    </div>

    <div v-if="!isMobile" class="orders__filter-bar">
      <div class="orders__input-wrap">
        <input
          v-model="queryParams.xyGoodsId"
          class="orders__input"
          placeholder="商品ID"
          @keyup.enter="loadOrders"
        />
      </div>

      <button class="btn btn--primary" @click="loadOrders">
        <IconSearch />
        <span>查询</span>
      </button>

      <button class="btn btn--ghost" @click="handleReset">
        重置
      </button>

      <span v-if="total > 0" class="orders__count">
        共 {{ total }} 条
      </span>
    </div>

    <div class="orders__content">
      <div class="orders__toolbar">
        <span class="orders__list-title">发货列表</span>
        <span v-if="orderList.length > 0" class="orders__count">
          {{ (queryParams.pageNum! - 1) * queryParams.pageSize! + 1 }}-{{ Math.min(queryParams.pageNum! * queryParams.pageSize!, total) }} / {{ total }}
        </span>
      </div>

      <div class="orders__table-wrap">
        <OrderTable
          :order-list="orderList"
          :loading="loading"
          @copy-sid="copySId"
          @confirm-shipment="openConfirmDialog"
        />
      </div>

      <div v-if="totalPages > 1" class="orders__pagination">
        <button
          class="orders__page-btn"
          :class="{ 'orders__page-btn--disabled': queryParams.pageNum! <= 1 }"
          @click="handlePageChange(queryParams.pageNum! - 1)"
        >
          <IconChevronLeft />
        </button>

        <template v-for="page in getPageButtons()" :key="page">
          <button
            class="orders__page-btn"
            :class="{ 'orders__page-btn--active': page === queryParams.pageNum }"
            @click="handlePageChange(page)"
          >
            {{ page }}
          </button>
        </template>

        <button
          class="orders__page-btn"
          :class="{ 'orders__page-btn--disabled': queryParams.pageNum! >= totalPages }"
          @click="handlePageChange(queryParams.pageNum! + 1)"
        >
          <IconChevronRight />
        </button>

        <span class="orders__page-info">{{ queryParams.pageNum }} / {{ totalPages }}</span>
      </div>
    </div>

    <Transition name="overlay-fade">
      <div v-if="showFilterSheet" class="orders__filter-overlay" @click="showFilterSheet = false">
        <div
          class="orders__filter-sheet"
          :class="{ 'orders__filter-sheet--open': showFilterSheet }"
          @click.stop
        >
          <div class="orders__filter-sheet-handle"></div>
          <h3 class="orders__filter-sheet-title">筛选条件</h3>

          <div class="orders__filter-group">
            <label class="orders__filter-label">商品ID</label>
            <input
              v-model="filterGoodsId"
              class="orders__filter-input"
              placeholder="请输入商品ID"
            />
          </div>

          <div class="orders__filter-actions">
            <button class="btn btn--secondary" @click="resetFilter">重置</button>
            <button class="btn btn--primary" @click="applyFilter">查询</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="overlay-fade">
      <div v-if="showConfirmDialog" class="orders__dialog-overlay" @click.self="showConfirmDialog = false">
        <div class="orders__dialog">
          <div class="orders__dialog-header">
            <h3 class="orders__dialog-title">确认发货</h3>
          </div>
          <div class="orders__dialog-body">
            <p class="orders__dialog-text">
              确认订单「{{ confirmTargetOrder?.orderId }}」已发货吗？
            </p>
          </div>
          <div class="orders__dialog-footer">
            <button
              class="orders__dialog-btn orders__dialog-btn--cancel"
              @click="showConfirmDialog = false"
            >
              取消
            </button>
            <button
              class="orders__dialog-btn orders__dialog-btn--confirm"
              @click="executeConfirmShipment"
            >
              确认
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
