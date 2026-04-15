<script setup lang="ts">
import { useAutoDelivery } from './useAutoDelivery'
import './auto-delivery.css'

import IconTruck from '@/components/icons/IconTruck.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'
import IconText from '@/components/icons/IconText.vue'
import IconRobot from '@/components/icons/IconRobot.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconImage from '@/components/icons/IconImage.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconPackage from '@/components/icons/IconPackage.vue'

import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue'

const {
  saving,
  accounts,
  selectedAccountId,
  goodsList,
  selectedGoods,
  currentConfig,
  configForm,
  goodsTotal,
  goodsLoading,
  goodsListRef,
  detailDialogVisible,
  selectedGoodsId,
  deliveryRecords,
  recordsLoading,
  recordsTotal,
  recordsPageNum,
  recordsPageSize,
  recordsTotalPages,
  isMobile,
  mobileView,
  confirmDialog,
  handleAccountChange,
  selectGoods,
  saveConfig,
  toggleAutoDelivery,
  loadDeliveryRecords,
  handleRecordsPageChange,
  viewGoodsDetail,
  handleConfirmShipment,
  handleTriggerDelivery,
  handleDialogConfirm,
  handleDialogCancel,
  handleGoodsScroll,
  goBackToGoods,
  formatTime,
  formatPrice,
  getStatusText,
  getStatusClass,
  getRecordStatusText,
  getRecordStatusClass
} = useAutoDelivery()
</script>

<template>
  <div class="ad">
    <!-- Header -->
    <div class="ad__header">
      <div class="ad__title-row">
        <div class="ad__title-icon">
          <IconTruck />
        </div>
        <h1 class="ad__title">自动发货</h1>
      </div>
      <div class="ad__actions">
        <div class="ad__select-wrap">
          <select
            v-model="selectedAccountId"
            class="ad__select"
            @change="handleAccountChange"
          >
            <option :value="null" disabled>选择账号</option>
            <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
              {{ acc.accountNote || acc.unb }}
            </option>
          </select>
          <span class="ad__select-icon">
            <IconChevronDown />
          </span>
        </div>
      </div>
    </div>

    <!-- Body -->
    <div class="ad__body">
      <!-- Goods Panel -->
      <div
        class="ad__goods-panel"
        :class="{ 'ad__goods-panel--hidden': isMobile && mobileView === 'config' }"
      >
        <div class="ad__goods-toolbar">
          <span class="ad__goods-toolbar-title">商品列表</span>
          <span v-if="goodsTotal > 0" class="ad__goods-toolbar-count">共 {{ goodsTotal }} 件</span>
        </div>

        <div
          class="ad__goods-list"
          ref="goodsListRef"
          @scroll="handleGoodsScroll"
        >
          <!-- Loading first page -->
          <div v-if="goodsLoading && goodsList.length === 0" class="ad__loading">
            <div class="ad__spinner"></div>
            <span>加载中...</span>
          </div>

          <!-- Goods items -->
          <div
            v-for="goods in goodsList"
            :key="goods.item.xyGoodId"
            class="ad__goods-item"
            :class="{ 'ad__goods-item--active': selectedGoods?.item.xyGoodId === goods.item.xyGoodId }"
            @click="selectGoods(goods)"
          >
            <img
              :src="goods.item.coverPic"
              :alt="goods.item.title"
              class="ad__goods-cover"
            />
            <div class="ad__goods-info">
              <div class="ad__goods-title">{{ goods.item.title }}</div>
              <div class="ad__goods-meta">
                <span class="ad__goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                <span
                  class="ad__goods-status"
                  :class="`ad__goods-status--${getStatusClass(goods.item.status)}`"
                >
                  {{ getStatusText(goods.item.status) }}
                </span>
                <span
                  v-if="goods.xianyuAutoDeliveryOn === 1"
                  class="ad__goods-auto-badge ad__goods-auto-badge--on"
                >
                  <IconSparkle />
                  自动
                </span>
              </div>
            </div>
          </div>

          <!-- Loading more -->
          <div v-if="goodsLoading && goodsList.length > 0" class="ad__loading">
            <div class="ad__spinner"></div>
            <span>加载中...</span>
          </div>

          <!-- No more data -->
          <div
            v-if="!goodsLoading && goodsList.length > 0 && goodsList.length >= goodsTotal"
            class="ad__no-more"
          >
            已加载全部
          </div>

          <!-- Empty -->
          <div v-if="!goodsLoading && goodsList.length === 0" class="ad__empty">
            <IconPackage />
            <span class="ad__empty-text">暂无商品</span>
          </div>
        </div>
      </div>

      <!-- Config Panel -->
      <div
        class="ad__config-panel"
        :class="{ 'ad__config-panel--hidden': isMobile && mobileView === 'goods' }"
      >
        <!-- Mobile back button -->
        <div v-if="isMobile && selectedGoods" class="ad__config-header">
          <button class="ad__back-btn" @click="goBackToGoods">
            <IconChevronLeft />
            返回
          </button>
          <img
            v-if="selectedGoods"
            :src="selectedGoods.item.coverPic"
            :alt="selectedGoods.item.title"
            class="ad__config-goods-cover"
          />
          <div class="ad__config-goods-info">
            <div class="ad__config-goods-title">{{ selectedGoods.item.title }}</div>
            <div class="ad__config-goods-sub">{{ formatPrice(selectedGoods.item.soldPrice) }}</div>
          </div>
        </div>

        <!-- Desktop config header -->
        <div v-if="!isMobile && selectedGoods" class="ad__config-header">
          <img
            :src="selectedGoods.item.coverPic"
            :alt="selectedGoods.item.title"
            class="ad__config-goods-cover"
          />
          <div class="ad__config-goods-info">
            <div class="ad__config-goods-title">{{ selectedGoods.item.title }}</div>
            <div class="ad__config-goods-sub">{{ formatPrice(selectedGoods.item.soldPrice) }}</div>
          </div>
          <button class="btn btn--ghost btn--sm" @click="viewGoodsDetail">
            <IconImage />
            <span class="mobile-hidden">详情</span>
          </button>
        </div>

        <!-- Empty state -->
        <div v-if="!selectedGoods" class="ad__config-empty">
          <IconPackage />
          <span class="ad__config-empty-text">选择商品以配置自动发货</span>
        </div>

        <!-- Config content -->
        <div v-if="selectedGoods" class="ad__config-scroll">
          <!-- Delivery Toggle Section -->
          <div class="ad__config-section">
            <div class="ad__config-section-title">发货设置</div>

            <div class="ad__toggle-row">
              <div class="ad__toggle-info">
                <div class="ad__toggle-label">自动发货</div>
                <div class="ad__toggle-hint">买家下单后自动发送发货内容</div>
              </div>
              <label class="ad__switch">
                <input
                  type="checkbox"
                  :checked="selectedGoods.xianyuAutoDeliveryOn === 1"
                  @change="toggleAutoDelivery(($event.target as HTMLInputElement).checked)"
                />
                <span class="ad__switch-track"></span>
                <span class="ad__switch-thumb"></span>
              </label>
            </div>

            <div class="ad__toggle-row">
              <div class="ad__toggle-info">
                <div class="ad__toggle-label">自动确认发货</div>
                <div class="ad__toggle-hint">
                  {{ selectedGoods.xianyuAutoDeliveryOn === 1
                    ? '发货成功后自动确认已发货'
                    : '需先开启自动发货' }}
                </div>
              </div>
              <label class="ad__switch">
                <input
                  type="checkbox"
                  :checked="configForm.autoConfirmShipment === 1"
                  :disabled="selectedGoods.xianyuAutoDeliveryOn !== 1"
                  @change="configForm.autoConfirmShipment = ($event.target as HTMLInputElement).checked ? 1 : 0"
                />
                <span class="ad__switch-track"></span>
                <span class="ad__switch-thumb"></span>
              </label>
            </div>
          </div>

          <!-- Content Type Section -->
          <div class="ad__config-section">
            <div class="ad__config-section-title">发货内容</div>

            <div class="ad__radio-group" style="margin-bottom: 12px;">
              <button
                class="ad__radio-btn"
                :class="{ 'ad__radio-btn--active': configForm.type === 1 }"
                @click="configForm.type = 1"
              >
                <IconText />
                文本内容
              </button>
              <button
                class="ad__radio-btn"
                :class="{ 'ad__radio-btn--active': configForm.type === 2 }"
                @click="configForm.type = 2"
              >
                <IconRobot />
                自定义
              </button>
            </div>

            <textarea
              v-model="configForm.autoDeliveryContent"
              class="ad__textarea"
              placeholder="请输入自动发货内容，买家下单后将自动发送此内容"
              maxlength="1000"
            ></textarea>
            <div class="ad__textarea-footer">
              <span class="ad__textarea-hint">支持文本、链接、卡密等内容</span>
              <span class="ad__textarea-count">{{ configForm.autoDeliveryContent.length }} / 1000</span>
            </div>

            <div class="ad__save-row">
              <button
                class="btn btn--primary"
                :class="{ 'btn--loading': saving }"
                :disabled="saving"
                @click="saveConfig"
              >
                <IconCheck />
                保存配置
              </button>
              <span v-if="currentConfig" class="ad__save-time">
                更新于 {{ formatTime(currentConfig.updateTime) }}
              </span>
            </div>
          </div>

          <!-- Records Section -->
          <div class="ad__records">
            <div class="ad__records-header">
              <div class="ad__records-title-row">
                <IconClock style="width:16px;height:16px;color:var(--d-text-tertiary)" />
                <span class="ad__records-title">发货记录</span>
                <span v-if="recordsTotal > 0" class="ad__records-count">共 {{ recordsTotal }} 条</span>
              </div>
            </div>

            <!-- Records Loading -->
            <div v-if="recordsLoading" class="ad__loading">
              <div class="ad__spinner"></div>
              <span>加载中...</span>
            </div>

            <!-- Desktop Table -->
            <table v-if="!isMobile && !recordsLoading && deliveryRecords.length > 0" class="ad__records-table">
              <thead>
                <tr>
                  <th>订单ID</th>
                  <th>买家</th>
                  <th>发货内容</th>
                  <th>状态</th>
                  <th>时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="record in deliveryRecords" :key="record.id">
                  <td>
                    <span class="ad__record-order-id">{{ record.orderId || '-' }}</span>
                  </td>
                  <td>{{ record.buyerUserName || '-' }}</td>
                  <td>
                    <span class="ad__record-content" :title="record.content">{{ record.content || '-' }}</span>
                  </td>
                  <td>
                    <span
                      class="ad__record-status"
                      :class="`ad__record-status--${getRecordStatusClass(record.state)}`"
                    >
                      {{ getRecordStatusText(record.state) }}
                    </span>
                  </td>
                  <td>
                    <span class="ad__record-time">{{ formatTime(record.createTime) }}</span>
                  </td>
                  <td>
                    <button
                      class="ad__record-action-btn"
                      @click="handleTriggerDelivery(record)"
                    >
                      发货
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- Mobile Record Cards -->
            <div v-if="isMobile && !recordsLoading && deliveryRecords.length > 0">
              <div
                v-for="record in deliveryRecords"
                :key="record.id"
                class="ad__record-card"
              >
                <div class="ad__record-card-header">
                  <span
                    class="ad__record-status"
                    :class="`ad__record-status--${getRecordStatusClass(record.state)}`"
                  >
                    {{ getRecordStatusText(record.state) }}
                  </span>
                  <span class="ad__record-time">{{ formatTime(record.createTime) }}</span>
                </div>
                <div class="ad__record-card-row">
                  <span class="ad__record-card-label">订单：</span>
                  <span class="ad__record-card-value">{{ record.orderId || '-' }}</span>
                </div>
                <div class="ad__record-card-row">
                  <span class="ad__record-card-label">买家：</span>
                  <span class="ad__record-card-value">{{ record.buyerUserName || '-' }}</span>
                </div>
                <div class="ad__record-card-row">
                  <span class="ad__record-card-label">内容：</span>
                  <span class="ad__record-card-value">{{ record.content || '-' }}</span>
                </div>
                <div class="ad__record-card-footer">
                  <button
                    class="ad__record-action-btn"
                    @click="handleTriggerDelivery(record)"
                  >
                    发货
                  </button>
                </div>
              </div>
            </div>

            <!-- Records Empty -->
            <div v-if="!recordsLoading && deliveryRecords.length === 0" class="ad__empty">
              <IconSend />
              <span class="ad__empty-text">暂无发货记录</span>
            </div>

            <!-- Records Pagination -->
            <div v-if="recordsTotalPages > 1" class="ad__pagination">
              <button
                class="ad__page-btn"
                :class="{ 'ad__page-btn--disabled': recordsPageNum <= 1 }"
                @click="handleRecordsPageChange(recordsPageNum - 1)"
              >
                <IconChevronLeft />
              </button>

              <template v-for="page in (() => {
                const btns: number[] = []
                const max = 5
                let start = Math.max(1, recordsPageNum - Math.floor(max / 2))
                const end = Math.min(recordsTotalPages, start + max - 1)
                start = Math.max(1, end - max + 1)
                for (let i = start; i <= end; i++) btns.push(i)
                return btns
              })()" :key="page">
                <button
                  class="ad__page-btn"
                  :class="{ 'ad__page-btn--active': page === recordsPageNum }"
                  @click="handleRecordsPageChange(page)"
                >
                  {{ page }}
                </button>
              </template>

              <button
                class="ad__page-btn"
                :class="{ 'ad__page-btn--disabled': recordsPageNum >= recordsTotalPages }"
                @click="handleRecordsPageChange(recordsPageNum + 1)"
              >
                <IconChevronRight />
              </button>

              <span class="ad__page-info">{{ recordsPageNum }} / {{ recordsTotalPages }}</span>
            </div>
          </div>
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
        class="ad__dialog-overlay"
        @click.self="handleDialogCancel"
      >
        <div class="ad__dialog">
          <div class="ad__dialog-header">
            <h3 class="ad__dialog-title">{{ confirmDialog.title }}</h3>
          </div>
          <div class="ad__dialog-body">
            <p class="ad__dialog-text">{{ confirmDialog.message }}</p>
          </div>
          <div class="ad__dialog-footer">
            <button
              class="ad__dialog-btn ad__dialog-btn--cancel"
              @click="handleDialogCancel"
            >
              取消
            </button>
            <button
              class="ad__dialog-btn"
              :class="confirmDialog.type === 'danger' ? 'ad__dialog-btn--danger' : 'ad__dialog-btn--primary'"
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
