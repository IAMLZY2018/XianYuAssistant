<script setup lang="ts">
import { onMounted, ref, computed, inject, defineComponent, h } from 'vue'
import { useOrderManager } from './useOrderManager'
import './orders.css'
import '@/styles/header-selectors.css'

import IconClipboard from '@/components/icons/IconClipboard.vue'
import IconSearch from '@/components/icons/IconSearch.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconFilter from '@/components/icons/IconFilter.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'
import IconPackage from '@/components/icons/IconPackage.vue'

import OrderTable from './components/OrderTable.vue'

const goodsPanelCollapsed = ref(true)
const isDesktopCollapsed = computed(() => !isMobile.value && goodsPanelCollapsed.value)

const {
  loading,
  orderList,
  total,
  accounts,
  goodsList,
  goodsTotal,
  goodsLoading,
  goodsListRef,
  onlyOnSale,
  selectedGoodsId,
  queryParams,
  totalPages,
  loadAccounts,
  loadOrders,
  loadGoods,
  handleAccountChange,
  handleReset,
  handlePageChange,
  copySId,
  handleConfirmShipment,
  handleGoodsScroll,
  selectGoods,
  clearGoodsFilter,
  toggleOnlyOnSale
} = useOrderManager()

const showFilterSheet = ref(false)
const isMobile = ref(false)

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

// 导航栏注入
const setHeaderContent = inject<(content: any) => void>('setHeaderContent')

const HeaderSelectors = defineComponent({
  setup() {
    return () => h('div', { class: 'header-selectors' }, [
      h('div', { class: 'header-select-wrap' }, [
        h('select', {
          class: 'header-select',
          onChange: (e: Event) => {
            const val = (e.target as HTMLSelectElement).value
            queryParams.xianyuAccountId = val ? parseInt(val) : undefined
            handleAccountChange()
          }
        }, [
          h('option', { value: '', disabled: true, selected: !queryParams.xianyuAccountId }, '账号'),
          ...accounts.value.map(acc =>
            h('option', {
              value: acc.id.toString(),
              selected: queryParams.xianyuAccountId === acc.id
            }, acc.accountNote || acc.unb)
          )
        ]),
        h(IconChevronDown, { class: 'header-select-icon' })
      ]),
      h('button', {
        class: ['header-refresh-btn', { 'header-refresh-btn--loading': loading.value }],
        disabled: loading.value,
        onClick: loadOrders
      }, [
        h(IconRefresh, { class: 'header-refresh-icon' })
      ]),
      h('button', {
        class: 'header-filter-btn',
        onClick: openFilterSheet
      }, [
        h(IconFilter, { class: 'header-filter-icon' })
      ])
    ])
  }
})

onMounted(async () => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  if (setHeaderContent) setHeaderContent(HeaderSelectors)
  await loadAccounts()
  if (setHeaderContent) setHeaderContent(HeaderSelectors)
  loadGoods()
  loadOrders()
})

const filterKeyword = ref('')

const openFilterSheet = () => {
  filterKeyword.value = queryParams.keyword || ''
  showFilterSheet.value = true
}

const applyFilter = () => {
  queryParams.keyword = filterKeyword.value || undefined
  queryParams.pageNum = 1
  showFilterSheet.value = false
  loadOrders()
}

const resetFilter = () => {
  filterKeyword.value = ''
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
        <template v-if="!isMobile">
          <div class="orders__input-wrap">
            <input
              v-model="queryParams.keyword"
              class="orders__input"
              placeholder="商品名称/规格/买家/发货内容"
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
        </template>
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

    <div class="orders__body" :class="{ 'orders__body--no-goods': isMobile }">
      <div
        v-if="!isMobile"
        class="orders__goods-panel"
        :class="{ 'orders__goods-panel--collapsed': isDesktopCollapsed }"
      >
        <template v-if="!isDesktopCollapsed">
          <div class="orders__goods-toolbar">
            <span class="orders__goods-toolbar-title">商品列表</span>
            <span v-if="goodsTotal > 0" class="orders__goods-toolbar-count">共 {{ goodsTotal }} 件</span>
            <button class="orders__only-on-sale-btn" :class="{ 'orders__only-on-sale-btn--active': onlyOnSale }" @click="toggleOnlyOnSale">
              {{ onlyOnSale ? '在售' : '全部' }}
            </button>
          </div>

          <div
            class="orders__goods-list"
            ref="goodsListRef"
            @scroll="handleGoodsScroll"
          >
            <div v-if="goodsLoading && goodsList.length === 0" class="orders__loading">
              <div class="orders__spinner"></div>
              <span>加载中...</span>
            </div>

            <div
              v-for="goods in goodsList"
              :key="goods.item.xyGoodId"
              class="orders__goods-item"
              :class="{ 'orders__goods-item--active': selectedGoodsId === goods.item.xyGoodId, 'orders__goods-item--offline': goods.item.status !== 0 }"
              @click="selectGoods(goods)"
            >
              <img
                :src="goods.item.coverPic"
                :alt="goods.item.title"
                class="orders__goods-cover"
              />
              <div class="orders__goods-info">
                <div class="orders__goods-title">{{ goods.item.title }}</div>
                <div class="orders__goods-meta">
                  <span class="orders__goods-price">¥{{ goods.item.soldPrice }}</span>
                  <span
                    class="orders__goods-status"
                    :class="`orders__goods-status--${goods.item.status === 0 ? 'on-sale' : goods.item.status === 1 ? 'off-shelf' : 'sold'}`"
                  >
                    {{ goods.item.status === 0 ? '在售' : goods.item.status === 1 ? '已下架' : '已售出' }}
                  </span>
                </div>
              </div>
            </div>

            <div v-if="goodsLoading && goodsList.length > 0" class="orders__loading">
              <div class="orders__spinner"></div>
              <span>加载中...</span>
            </div>

            <div
              v-if="!goodsLoading && goodsList.length > 0 && goodsList.length >= goodsTotal"
              class="orders__no-more"
            >
              已加载全部
            </div>

            <div v-if="!goodsLoading && goodsList.length === 0" class="orders__goods-empty">
              <IconPackage />
              <span class="orders__goods-empty-text">暂无商品</span>
            </div>
          </div>
        </template>
        <template v-else>
          <div class="orders__goods-icons">
            <div
              v-for="goods in goodsList"
              :key="goods.item.xyGoodId"
              class="orders__goods-icon-item"
              :class="{ 'orders__goods-icon-item--active': selectedGoodsId === goods.item.xyGoodId }"
              :title="goods.item.title"
              @click="selectGoods(goods)"
            >
              <img :src="goods.item.coverPic" class="orders__goods-icon-img" />
            </div>
          </div>
        </template>
        <button
          class="orders__goods-toggle"
          :title="goodsPanelCollapsed ? '展开商品列表' : '折叠商品列表'"
          @click="goodsPanelCollapsed = !goodsPanelCollapsed"
        >
          <svg v-if="goodsPanelCollapsed" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
          <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
        </button>
      </div>

      <div class="orders__content">
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
            <label class="orders__filter-label">关键词</label>
            <input
              v-model="filterKeyword"
              class="orders__filter-input"
              placeholder="商品名称/规格/买家/发货内容"
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
