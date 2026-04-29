<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'
import { useMessageManager } from './useMessageManager'
import './messages.css'

import IconMessage from '@/components/icons/IconMessage.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconImage from '@/components/icons/IconImage.vue'

import GoodsSidebar from './components/GoodsSidebar.vue'
import MessageList from './components/MessageList.vue'
import MultiImageUploader from '@/components/MultiImageUploader.vue'

const {
  loading,
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
  quickReplyVisible,
  quickReplyMessage,
  quickReplySending,
  currentReplyMessage,
  quickReplyImage,
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
  openQuickReply,
  handleQuickReply,
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

onMounted(() => {
  loadAccounts()
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize)
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
            @click="loadMessages"
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
        <div class="messages__sidebar">
          <GoodsSidebar
            :goods-list="goodsList"
            :goods-total="goodsTotal"
            :goods-loading="goodsLoading"
            :goods-id-filter="goodsIdFilter"
            @select="selectGoods"
            @clear-filter="clearFilter"
          />
        </div>

        <!-- Message Main -->
        <div class="messages__main">
          <div class="messages__main-header">
            <span class="messages__main-title">消息列表</span>
            <span v-if="total > 0" class="messages__main-count">
              共 {{ total }} 条
            </span>
          </div>

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
              @reply="openQuickReply"
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
          <div class="messages__title-row">
            <div class="messages__title-icon">
              <IconMessage />
            </div>
            <h1 class="messages__title">消息</h1>
          </div>
          <div class="messages__actions">
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
          <button class="btn btn--secondary" style="height:32px;padding:0 12px;" @click="loadMessages">
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
            @reply="openQuickReply"
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

    <!-- ========== Quick Reply Dialog ========== -->
    <Transition name="overlay-fade">
      <div v-if="quickReplyVisible" class="messages__dialog-overlay" @click.self="quickReplyVisible = false">
        <div class="messages__dialog">
          <div class="messages__dialog-header">
            <h3 class="messages__dialog-title">快速回复</h3>
          </div>
          <div class="messages__dialog-body">
            <div class="messages__reply-info" v-if="currentReplyMessage">
              <div class="messages__reply-row">
                <span class="messages__reply-label">回复给：</span>
                <span class="messages__reply-value">{{ currentReplyMessage.senderUserName }}</span>
              </div>
              <div class="messages__reply-row">
                <span class="messages__reply-label">原消息：</span>
                <span class="messages__reply-value">{{ currentReplyMessage.msgContent }}</span>
              </div>
            </div>
            <textarea
              v-model="quickReplyMessage"
              class="messages__reply-textarea"
              placeholder="请输入回复内容..."
              maxlength="500"
            ></textarea>
            <div class="messages__reply-image">
              <MultiImageUploader
                :account-id="selectedAccountId || 0"
                v-model="quickReplyImage"
              />
            </div>
          </div>
          <div class="messages__dialog-footer">
            <button class="btn btn--secondary" @click="quickReplyVisible = false">取消</button>
            <button
              class="btn btn--primary"
              :class="{ 'btn--loading': quickReplySending }"
              :disabled="quickReplySending"
              @click="handleQuickReply"
            >
              <IconSend />
              <span>发送</span>
            </button>
          </div>
        </div>
      </div>
    </Transition>
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
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--d-text-secondary, #6e6e73);
  background: rgba(0, 0, 0, 0.02);
  flex-shrink: 0;
}

.mobile-goods__count {
  font-size: 12px;
  color: var(--d-text-tertiary, #86868b);
}

.mobile-goods__list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 12px;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.08) transparent;
}

.mobile-goods__item {
  display: flex;
  gap: 10px;
  padding: 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  -webkit-tap-highlight-color: transparent;
  border: 1px solid transparent;
  margin-bottom: 4px;
}

.mobile-goods__item:active {
  transform: scale(0.98);
}

.mobile-goods__item--active {
  background: rgba(0, 122, 255, 0.06);
  border-color: rgba(0, 122, 255, 0.15);
}

.mobile-goods__thumb {
  width: 44px;
  height: 44px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  background: rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
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
}

.mobile-goods__name {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
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
  padding: 8px 12px;
  padding-top: max(8px, env(safe-area-inset-top, 8px));
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  flex-shrink: 0;
  background: rgba(255, 255, 255, 0.82);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
}

.mobile-messages__back {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 36px;
  padding: 0 8px;
  font-size: 16px;
  font-weight: 500;
  color: #007aff;
  background: none;
  border: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.mobile-messages__back svg {
  width: 20px;
  height: 20px;
}

.mobile-messages__goods {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.mobile-messages__goods-img {
  width: 28px;
  height: 28px;
  border-radius: 4px;
  object-fit: cover;
  flex-shrink: 0;
}

.mobile-messages__goods-name {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-messages__body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
}

/* ============================================================
   Transition
   ============================================================ */
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
