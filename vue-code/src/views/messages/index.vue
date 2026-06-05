<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed, inject, defineComponent, h } from 'vue'
import { useMessageManager } from './useMessageManager'
import './messages.css'
import '@/styles/header-selectors.css'

import IconMessage from '@/components/icons/IconMessage.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'
import IconImage from '@/components/icons/IconImage.vue'

import GoodsSidebar from './components/GoodsSidebar.vue'
import MessageList from './components/MessageList.vue'

const {
  loading,
  silentLoading,
  accounts,
  selectedAccountId,
  goodsIdFilter,
  messageList,
  currentPage,
  pageSize,
  total,
  totalPages,
  filterCurrentAccount,
  goodsList,
  goodsTotal,
  goodsLoading,
  goodsListRef,
  isMobile,
  mobileView,
  selectedGoodsForMobile,
  getCurrentAccountUnb,
  loadAccounts,
  loadMessages,
  loadGoodsList,
  handleGoodsScroll,
  handleAccountChange,
  selectGoods,
  goBackToGoods,
  clearFilter,
  handlePageChange,
  isUserMessage,
  getContentTypeText,
  getContentTypeColor,
  getContentTypeBg,
  formatMessageTime
} = useMessageManager()

// 分页按钮
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

// 手机端商品列表滚动容器
const mobileGoodsRef = ref<HTMLElement | null>(null)
const sidebarCollapsed = ref(true)

const handleMobileGoodsScroll = () => {
  if (!mobileGoodsRef.value || goodsLoading.value) return
  const { scrollTop, scrollHeight, clientHeight } = mobileGoodsRef.value
  if (scrollTop + clientHeight >= scrollHeight - 50) {
    if (goodsList.value.length < goodsTotal.value) {
      loadGoodsList()
    }
  }
}

const handleImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}

// 注入导航栏内容
const setHeaderContent = inject<(content: any) => void>('setHeaderContent')

const HeaderSelectors = defineComponent({
  setup() {
    return () => h('div', { class: 'header-selectors' }, [
      h('div', { class: 'header-select-wrap' }, [
        h('select', {
          class: 'header-select',
          onChange: (e: Event) => {
            const val = (e.target as HTMLSelectElement).value
            selectedAccountId.value = val ? parseInt(val) : null
            handleAccountChange()
          }
        }, [
          h('option', { value: '', disabled: true, selected: !selectedAccountId.value }, '账号'),
          ...accounts.value.map(acc =>
            h('option', {
              value: acc.id.toString(),
              selected: selectedAccountId.value === acc.id
            }, acc.accountNote || acc.unb)
          )
        ]),
        h(IconChevronDown, { class: 'header-select-icon' })
      ]),
      h('button', {
        class: ['header-toggle-btn', { 'header-toggle-btn--on': filterCurrentAccount.value }],
        title: '隐藏我发送的',
        onClick: () => {
          filterCurrentAccount.value = !filterCurrentAccount.value
          currentPage.value = 1
          loadMessages()
        }
      }, [
        h('span', { class: 'header-toggle-track' }, [
          h('span', { class: 'header-toggle-thumb' })
        ])
      ]),
      h('button', {
        class: ['header-refresh-btn', { 'header-refresh-btn--loading': loading.value }],
        disabled: loading.value,
        onClick: loadMessages
      }, [
        h(IconRefresh, { class: 'header-refresh-icon' })
      ])
    ])
  }
})

let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(async () => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  if (setHeaderContent) {
    setHeaderContent(HeaderSelectors)
  }
  await loadAccounts()
  // 账号加载完后重新注入，确保选项渲染
  if (setHeaderContent) {
    setHeaderContent(HeaderSelectors)
  }
  refreshTimer = setInterval(() => {
    if (selectedAccountId.value) {
      loadMessages(true)
    }
  }, 3000)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize)
  if (refreshTimer) {
    clearInterval(refreshTimer)
    refreshTimer = null
  }
})

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
  if (!isMobile.value) {
    mobileView.value = 'goods'
  }
}
</script>

<template>
  <div class="messages">
    <!-- ========== Desktop Layout ========== -->
    <template v-if="!isMobile">
      <!-- Header -->
      <div class="messages__header">
        <div class="messages__title-row">
          <div class="messages__title-icon">
            <IconMessage />
          </div>
          <h1 class="messages__title">消息管理</h1>
        </div>
        <div class="messages__actions">
          <div class="messages__select-wrap">
            <select
              v-model="selectedAccountId"
              class="messages__select"
              @change="handleAccountChange"
            >
              <option :value="null" disabled>选择账号</option>
              <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                {{ acc.accountNote || acc.unb }}
              </option>
            </select>
            <span class="messages__select-icon">
              <IconChevronDown />
            </span>
          </div>

          <button
            class="btn btn--secondary"
            :class="{ 'btn--loading': loading }"
            :disabled="loading"
            @click="loadMessages()"
          >
            <IconRefresh />
            <span>刷新</span>
          </button>

          <div class="messages__toggle-wrap">
            <span class="messages__toggle-label">隐藏我发送的</span>
            <button
              class="messages__toggle"
              :class="{ 'messages__toggle--on': filterCurrentAccount }"
              @click="filterCurrentAccount = !filterCurrentAccount; currentPage = 1; loadMessages()"
            >
              <span class="messages__toggle-track">
                <span class="messages__toggle-thumb"></span>
              </span>
            </button>
          </div>
        </div>
      </div>

      <!-- Content: Sidebar + Main -->
      <div class="messages__content">
        <!-- Goods Sidebar -->
        <div class="messages__sidebar" :class="{ 'messages__sidebar--collapsed': sidebarCollapsed }">
          <template v-if="!sidebarCollapsed">
            <GoodsSidebar
              :goods-list="goodsList"
              :goods-total="goodsTotal"
              :goods-loading="goodsLoading"
              :goods-id-filter="goodsIdFilter"
              @select="selectGoods"
              @clear-filter="clearFilter"
            />
          </template>
          <template v-else>
            <div class="messages__sidebar-icons">
              <div
                v-for="goods in goodsList"
                :key="goods.item.id"
                class="messages__sidebar-icon-item"
                :class="{ 'messages__sidebar-icon-item--active': goodsIdFilter === goods.item.xyGoodId }"
                :title="goods.item.title"
                @click="selectGoods(goods.item.xyGoodId, goods)"
              >
                <img
                  v-if="goods.item.coverPic"
                  :src="goods.item.coverPic"
                  class="messages__sidebar-icon-img"
                  @error="handleImgError"
                />
                <div v-else class="messages__sidebar-icon-placeholder">
                  <IconImage />
                </div>
              </div>
            </div>
          </template>
          <button
            class="messages__sidebar-toggle"
            :title="sidebarCollapsed ? '展开商品列表' : '折叠商品列表'"
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <svg v-if="sidebarCollapsed" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M9 18l6-6-6-6"/></svg>
            <svg v-else xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M15 18l-6-6 6-6"/></svg>
          </button>
        </div>

        <!-- Message Main -->
        <div class="messages__main">

          <div
            ref="goodsListRef"
            class="messages__table-wrap"
            @scroll="handleGoodsScroll"
          >
            <MessageList
              :message-list="messageList"
              :loading="loading"
              :xianyu-account-id="selectedAccountId || undefined"
              :goods-list="goodsList"
              :current-account-unb="getCurrentAccountUnb"
            />
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="messages__pagination">
            <button
              class="messages__page-btn"
              :class="{ 'messages__page-btn--disabled': currentPage <= 1 }"
              @click="handlePageChange(currentPage - 1)"
            >
              <IconChevronLeft />
            </button>
            <template v-for="page in getPageButtons()" :key="page">
              <button
                class="messages__page-btn"
                :class="{ 'messages__page-btn--active': page === currentPage }"
                @click="handlePageChange(page)"
              >
                {{ page }}
              </button>
            </template>
            <button
              class="messages__page-btn"
              :class="{ 'messages__page-btn--disabled': currentPage >= totalPages }"
              @click="handlePageChange(currentPage + 1)"
            >
              <IconChevronRight />
            </button>
            <span class="messages__page-info">{{ currentPage }} / {{ totalPages }}</span>
          </div>
        </div>
      </div>
    </template>

    <!-- ========== Mobile Layout ========== -->
    <template v-else>
      <!-- Mobile: Goods View -->
      <div v-show="mobileView === 'goods'" class="mobile-goods">
        <div class="messages__header" style="margin-bottom: 0; border-bottom: 1px solid var(--d-border);">
          <div class="messages__title-row mobile-hidden">
            <div class="messages__title-icon">
              <IconMessage />
            </div>
            <h1 class="messages__title">消息</h1>
          </div>
          <div class="messages__actions mobile-hidden">
            <div class="messages__select-wrap">
              <select
                v-model="selectedAccountId"
                class="messages__select"
                style="min-width: 100px"
                @change="handleAccountChange"
              >
                <option :value="null" disabled>账号</option>
                <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                  {{ acc.accountNote || acc.unb }}
                </option>
              </select>
              <span class="messages__select-icon">
                <IconChevronDown />
              </span>
            </div>

            <button
              class="messages__toggle"
              :class="{ 'messages__toggle--on': filterCurrentAccount }"
              @click="filterCurrentAccount = !filterCurrentAccount; currentPage = 1; loadMessages()"
            >
              <span class="messages__toggle-track">
                <span class="messages__toggle-thumb"></span>
              </span>
            </button>
          </div>
        </div>

        <div class="mobile-goods__title-bar">
          <span>选择商品查看消息</span>
          <span class="mobile-goods__count">{{ goodsTotal }} 件</span>
        </div>

        <div
          ref="mobileGoodsRef"
          class="mobile-goods__list"
          @scroll="handleMobileGoodsScroll"
        >
          <div
            v-for="goods in goodsList"
            :key="goods.item.id"
            class="mobile-goods__item"
            :class="{ 'mobile-goods__item--active': goodsIdFilter === goods.item.xyGoodId }"
            @click="selectGoods(goods.item.xyGoodId, goods)"
          >
            <div class="mobile-goods__thumb">
              <img
                v-if="goods.item.coverPic"
                :src="goods.item.coverPic"
                :alt="goods.item.title"
                @error="handleImgError"
              />
              <div v-else class="mobile-goods__placeholder">
                <IconImage />
              </div>
            </div>
            <div class="mobile-goods__info">
              <div class="mobile-goods__name">{{ goods.item.title }}</div>
              <div class="mobile-goods__id">{{ goods.item.xyGoodId }}</div>
            </div>
          </div>

          <div v-if="goodsLoading" class="mobile-goods__loading">
            <div class="mobile-goods__spinner"></div>
            <span>加载中...</span>
          </div>

          <div
            v-if="!goodsLoading && goodsList.length > 0 && goodsList.length >= goodsTotal"
            class="mobile-goods__end"
          >
            已加载全部
          </div>
        </div>
      </div>

      <!-- Mobile: Messages View -->
      <div v-show="mobileView === 'messages'" class="mobile-messages">
        <div class="mobile-messages__header">
          <button class="mobile-messages__back" @click="goBackToGoods">
            <IconChevronLeft />
            <span>返回</span>
          </button>
          <div v-if="selectedGoodsForMobile" class="mobile-messages__goods">
            <img
              v-if="selectedGoodsForMobile.item.coverPic"
              :src="selectedGoodsForMobile.item.coverPic"
              class="mobile-messages__goods-img"
            />
            <span class="mobile-messages__goods-name">{{ selectedGoodsForMobile.item.title }}</span>
          </div>
          <button class="mobile-messages__refresh" @click="loadMessages()">
            <IconRefresh />
          </button>
        </div>

        <div class="mobile-messages__body">
           <MessageList
            :message-list="messageList"
            :loading="loading"
            :xianyu-account-id="selectedAccountId || undefined"
            :goods-list="goodsList"
            :current-account-unb="getCurrentAccountUnb"
          />
        </div>

        <!-- Mobile Pagination -->
        <div v-if="totalPages > 1" class="messages__pagination" style="flex-shrink: 0;">
          <button
            class="messages__page-btn"
            :class="{ 'messages__page-btn--disabled': currentPage <= 1 }"
            @click="handlePageChange(currentPage - 1)"
          >
            <IconChevronLeft />
          </button>
          <template v-for="page in getPageButtons()" :key="page">
            <button
              class="messages__page-btn"
              :class="{ 'messages__page-btn--active': page === currentPage }"
              @click="handlePageChange(page)"
            >
              {{ page }}
            </button>
          </template>
          <button
            class="messages__page-btn"
            :class="{ 'messages__page-btn--disabled': currentPage >= totalPages }"
            @click="handlePageChange(currentPage + 1)"
          >
            <IconChevronRight />
          </button>
          <span class="messages__page-info">{{ currentPage }} / {{ totalPages }}</span>
        </div>
      </div>
    </template>

  </div>
</template>

<style scoped>
/* ============================================================
   Mobile Goods View
   ============================================================ */
.mobile-goods {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mobile-goods__title-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  font-size: 12px;
  font-weight: 500;
  color: var(--d-text-tertiary, #86868b);
  background: rgba(0, 0, 0, 0.02);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  flex-shrink: 0;
  letter-spacing: 0.01em;
}

.mobile-goods__count {
  font-size: 12px;
  color: var(--d-text-tertiary, #86868b);
  background: rgba(0, 0, 0, 0.05);
  padding: 2px 8px;
  border-radius: 10px;
}

.mobile-goods__list {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.mobile-goods__list::-webkit-scrollbar {
  display: none;
}

.mobile-goods__item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s ease;
  -webkit-tap-highlight-color: transparent;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

/* 斑马线：偶数行浅灰背景 */
.mobile-goods__item:nth-child(even) {
  background: rgba(0, 0, 0, 0.018);
}

.mobile-goods__item:active {
  background: rgba(0, 122, 255, 0.06);
}

.mobile-goods__item--active {
  background: rgba(0, 122, 255, 0.07) !important;
  border-bottom-color: rgba(0, 122, 255, 0.12);
  box-shadow: inset 3px 0 0 #007aff;
}

.mobile-goods__thumb {
  width: 46px;
  height: 46px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.mobile-goods__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mobile-goods__placeholder svg {
  width: 18px;
  height: 18px;
  color: rgba(0, 0, 0, 0.2);
}

.mobile-goods__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.mobile-goods__name {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 3px;
}

.mobile-goods__id {
  font-size: 11px;
  color: #86868b;
  font-family: 'SF Mono', 'Menlo', monospace;
}

.mobile-goods__loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 16px;
  font-size: 12px;
  color: #86868b;
}

.mobile-goods__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 0, 0, 0.06);
  border-top-color: #007aff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.mobile-goods__end {
  text-align: center;
  padding: 12px;
  font-size: 12px;
  color: #86868b;
  opacity: 0.6;
}

/* ============================================================
   Mobile Messages View
   ============================================================ */
.mobile-messages {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.mobile-messages__header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  background: rgba(248, 248, 248, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.mobile-messages__back {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 34px;
  padding: 0 6px;
  font-size: 15px;
  font-weight: 500;
  color: #007aff;
  background: none;
  border: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
  flex-shrink: 0;
}

.mobile-messages__back svg {
  width: 18px;
  height: 18px;
}

.mobile-messages__goods {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.mobile-messages__goods-img {
  width: 30px;
  height: 30px;
  border-radius: 6px;
  object-fit: cover;
  flex-shrink: 0;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.mobile-messages__goods-name {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-messages__body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.mobile-messages__body::-webkit-scrollbar {
  display: none;
}

.mobile-messages__refresh {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background 0.15s ease;
  -webkit-tap-highlight-color: transparent;
}

.mobile-messages__refresh:active {
  background: rgba(0, 0, 0, 0.12);
}

.mobile-messages__refresh svg {
  width: 16px;
  height: 16px;
}

</style>
