<script setup lang="ts">
import { useAutoReply } from './useAutoReply'
import './auto-reply.css'

import IconChat from '@/components/icons/IconChat.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconRobot from '@/components/icons/IconRobot.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconImage from '@/components/icons/IconImage.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconPackage from '@/components/icons/IconPackage.vue'
import IconClipboard from '@/components/icons/IconClipboard.vue'

import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue'

const {
  saving,
  accounts,
  selectedAccountId,
  goodsList,
  selectedGoods,
  goodsTotal,
  goodsLoading,
  goodsListRef,
  detailDialogVisible,
  selectedGoodsId,
  rightTab,
  dataContent,
  uploading,
  chatMessages,
  chatInput,
  chatSending,
  chatListRef,
  isMobile,
  mobileView,
  confirmDialog,
  handleAccountChange,
  selectGoods,
  toggleAutoReply,
  handleUploadData,
  handleSendChat,
  handleChatKeydown,
  handleGoodsScroll,
  goBackToGoods,
  viewGoodsDetail,
  handleDialogConfirm,
  handleDialogCancel,
  formatTime,
  formatPrice,
  getStatusText,
  getStatusClass
} = useAutoReply()
</script>

<template>
  <div class="ar">
    <!-- Header -->
    <div class="ar__header">
      <div class="ar__title-row">
        <div class="ar__title-icon">
          <IconChat />
        </div>
        <h1 class="ar__title">自动回复</h1>
      </div>
      <div class="ar__actions">
        <div class="ar__select-wrap">
          <select
            v-model="selectedAccountId"
            class="ar__select"
            @change="handleAccountChange"
          >
            <option :value="null" disabled>选择账号</option>
            <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
              {{ acc.accountNote || acc.unb }}
            </option>
          </select>
          <span class="ar__select-icon">
            <IconChevronDown />
          </span>
        </div>
      </div>
    </div>

    <!-- Body -->
    <div class="ar__body">
      <!-- Goods Panel (Left) -->
      <div
        class="ar__goods-panel"
        :class="{ 'ar__goods-panel--hidden': isMobile && mobileView === 'config' }"
      >
        <div class="ar__goods-toolbar">
          <span class="ar__goods-toolbar-title">商品列表</span>
          <span v-if="goodsTotal > 0" class="ar__goods-toolbar-count">共 {{ goodsTotal }} 件</span>
        </div>

        <div
          class="ar__goods-list"
          ref="goodsListRef"
          @scroll="handleGoodsScroll"
        >
          <!-- Loading first page -->
          <div v-if="goodsLoading && goodsList.length === 0" class="ar__loading">
            <div class="ar__spinner"></div>
            <span>加载中...</span>
          </div>

          <!-- Goods items -->
          <div
            v-for="goods in goodsList"
            :key="goods.item.xyGoodId"
            class="ar__goods-item"
            :class="{ 'ar__goods-item--active': selectedGoods?.item.xyGoodId === goods.item.xyGoodId }"
            @click="selectGoods(goods)"
          >
            <img
              :src="goods.item.coverPic"
              :alt="goods.item.title"
              class="ar__goods-cover"
            />
            <div class="ar__goods-info">
              <div class="ar__goods-title">{{ goods.item.title }}</div>
              <div class="ar__goods-meta">
                <span class="ar__goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                <span
                  class="ar__goods-status"
                  :class="`ar__goods-status--${getStatusClass(goods.item.status)}`"
                >
                  {{ getStatusText(goods.item.status) }}
                </span>
                <span
                  v-if="goods.xianyuAutoReplyOn === 1"
                  class="ar__goods-auto-badge ar__goods-auto-badge--on"
                >
                  <IconSparkle />
                  自动
                </span>
              </div>
            </div>
          </div>

          <!-- Loading more -->
          <div v-if="goodsLoading && goodsList.length > 0" class="ar__loading">
            <div class="ar__spinner"></div>
            <span>加载中...</span>
          </div>

          <!-- No more data -->
          <div
            v-if="!goodsLoading && goodsList.length > 0 && goodsList.length >= goodsTotal"
            class="ar__no-more"
          >
            已加载全部
          </div>

          <!-- Empty -->
          <div v-if="!goodsLoading && goodsList.length === 0" class="ar__empty">
            <IconPackage />
            <span class="ar__empty-text">暂无商品</span>
          </div>
        </div>
      </div>

      <!-- Config Panel (Right) -->
      <div
        class="ar__config-panel"
        :class="{ 'ar__config-panel--hidden': isMobile && mobileView === 'goods' }"
      >
        <!-- Mobile back button -->
        <div v-if="isMobile && selectedGoods" class="ar__config-header">
          <button class="ar__back-btn" @click="goBackToGoods">
            <IconChevronLeft />
            返回
          </button>
          <img
            v-if="selectedGoods"
            :src="selectedGoods.item.coverPic"
            :alt="selectedGoods.item.title"
            class="ar__config-goods-cover"
          />
          <div class="ar__config-goods-info">
            <div class="ar__config-goods-title">{{ selectedGoods.item.title }}</div>
            <div class="ar__config-goods-sub">{{ formatPrice(selectedGoods.item.soldPrice) }}</div>
          </div>
        </div>

        <!-- Desktop config header -->
        <div v-if="!isMobile && selectedGoods" class="ar__config-header">
          <img
            :src="selectedGoods.item.coverPic"
            :alt="selectedGoods.item.title"
            class="ar__config-goods-cover"
          />
          <div class="ar__config-goods-info">
            <div class="ar__config-goods-title">{{ selectedGoods.item.title }}</div>
            <div class="ar__config-goods-sub">{{ formatPrice(selectedGoods.item.soldPrice) }}</div>
          </div>
          <button class="btn btn--ghost btn--sm" @click="viewGoodsDetail">
            <IconImage />
            <span class="mobile-hidden">详情</span>
          </button>
        </div>

        <!-- Empty state -->
        <div v-if="!selectedGoods" class="ar__config-empty">
          <IconChat />
          <span class="ar__config-empty-text">选择商品以配置自动回复</span>
        </div>

        <!-- Config content -->
        <div v-if="selectedGoods" class="ar__config-scroll">
          <!-- Auto Reply Toggle -->
          <div class="ar__config-section">
            <div class="ar__config-section-title">回复设置</div>

            <div class="ar__toggle-row">
              <div class="ar__toggle-info">
                <div class="ar__toggle-label">自动回复</div>
                <div class="ar__toggle-hint">买家咨询时基于AI知识库自动回复</div>
              </div>
              <label class="ar__switch">
                <input
                  type="checkbox"
                  :checked="selectedGoods.xianyuAutoReplyOn === 1"
                  @change="toggleAutoReply(($event.target as HTMLInputElement).checked)"
                />
                <span class="ar__switch-track"></span>
                <span class="ar__switch-thumb"></span>
              </label>
            </div>
          </div>

          <!-- Tab Switch: Data / Chat -->
          <div class="ar__tab-group">
            <button
              class="ar__tab-btn"
              :class="{ 'ar__tab-btn--active': rightTab === 'data' }"
              @click="rightTab = 'data'"
            >
              <IconClipboard />
              知识资料
            </button>
            <button
              class="ar__tab-btn"
              :class="{ 'ar__tab-btn--active': rightTab === 'chat' }"
              @click="rightTab = 'chat'"
            >
              <IconRobot />
              AI 对话
            </button>
          </div>

          <!-- ====== 知识资料视图 ====== -->
          <template v-if="rightTab === 'data'">
            <div class="ar__config-section">
              <div class="ar__config-section-title">上传资料</div>
              <div class="ar__toggle-hint" style="margin-bottom: 8px;">
                上传商品相关资料到AI知识库，AI将基于这些资料自动回复买家咨询
              </div>

              <textarea
                v-model="dataContent"
                class="ar__textarea"
                placeholder="请输入商品资料内容，如商品介绍、规格参数、使用说明、常见问题等"
                maxlength="5000"
              ></textarea>
              <div class="ar__textarea-footer">
                <span class="ar__textarea-hint">支持文本内容，将存入AI知识库</span>
                <span class="ar__textarea-count">{{ dataContent.length }} / 5000</span>
              </div>

              <div class="ar__save-row">
                <button
                  class="btn btn--primary"
                  :class="{ 'btn--loading': uploading }"
                  :disabled="uploading"
                  @click="handleUploadData"
                >
                  <IconCheck />
                  上传资料
                </button>
              </div>
            </div>
          </template>

          <!-- ====== AI 对话视图 ====== -->
          <template v-if="rightTab === 'chat'">
            <div class="ar__chat-container">
              <!-- Chat messages -->
              <div
                v-if="chatMessages.length > 0"
                class="ar__chat-list"
                ref="chatListRef"
              >
                <div
                  v-for="msg in chatMessages"
                  :key="msg.id"
                  class="ar__chat-msg"
                  :class="`ar__chat-msg--${msg.role}`"
                >
                  <div class="ar__chat-bubble" :class="{ 'ar__chat-bubble--loading': msg.loading }">
                    <template v-if="msg.loading">
                      <div class="ar__chat-dots">
                        <span class="ar__chat-dot"></span>
                        <span class="ar__chat-dot"></span>
                        <span class="ar__chat-dot"></span>
                      </div>
                    </template>
                    <template v-else>
                      {{ msg.content }}
                    </template>
                  </div>
                </div>
              </div>

              <!-- Chat empty -->
              <div v-if="chatMessages.length === 0" class="ar__chat-empty">
                <IconRobot />
                <span class="ar__chat-empty-text">AI 对话</span>
                <span class="ar__chat-empty-hint">基于商品知识库回答问题，输入消息开始对话</span>
              </div>

              <!-- Chat input -->
              <div class="ar__chat-input-area">
                <textarea
                  v-model="chatInput"
                  class="ar__chat-input"
                  placeholder="输入消息..."
                  rows="1"
                  :disabled="chatSending"
                  @keydown="handleChatKeydown"
                ></textarea>
                <button
                  class="ar__chat-send-btn"
                  :disabled="!chatInput.trim() || chatSending"
                  @click="handleSendChat"
                >
                  <IconSend />
                </button>
              </div>
            </div>
          </template>
        </div>
      </div>
    </div>

    <!-- Goods Detail Dialog -->
    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
    />

    <!-- Confirm Dialog -->
    <Transition name="overlay-fade">
      <div
        v-if="confirmDialog.visible"
        class="ar__dialog-overlay"
        @click.self="handleDialogCancel"
      >
        <div class="ar__dialog">
          <div class="ar__dialog-header">
            <h3 class="ar__dialog-title">{{ confirmDialog.title }}</h3>
          </div>
          <div class="ar__dialog-body">
            <p class="ar__dialog-text">{{ confirmDialog.message }}</p>
          </div>
          <div class="ar__dialog-footer">
            <button
              class="ar__dialog-btn ar__dialog-btn--cancel"
              @click="handleDialogCancel"
            >
              取消
            </button>
            <button
              class="ar__dialog-btn"
              :class="confirmDialog.type === 'danger' ? 'ar__dialog-btn--danger' : 'ar__dialog-btn--primary'"
              @click="handleDialogConfirm"
            >
              确定
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
