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
import IconCopy from '@/components/icons/IconCopy.vue'

import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue'
import MultiImageUploader from '@/components/MultiImageUploader.vue'

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
  getRecordStatusClass,
  apiHintUrl,
  apiHintParamsJson,
  confirmShipmentUrl,
  confirmShipmentParamsJson,
  kamiConfigOptions,
  selectedKamiConfigId,
  copyApiUrl,
  copyApiParams,
  copyConfirmShipmentUrl,
  copyConfirmShipmentParams
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
                  {{ goods.autoDeliveryType === 2 ? '卡密' : '文本' }}
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
          <!-- Top Tab Switch -->
          <div class="ad__config-section ad__config-section--no-pad-bottom">
            <div class="ad__tab-group">
              <button
                class="ad__tab-btn"
                :class="{ 'ad__tab-btn--active': configForm.deliveryMode === 1 }"
                @click="configForm.deliveryMode = 1"
              >
                <IconText />
                文本发货
              </button>
              <button
                class="ad__tab-btn"
                :class="{ 'ad__tab-btn--active': configForm.deliveryMode === 2 }"
                @click="configForm.deliveryMode = 2"
              >
                🔑
                卡密发货
              </button>
              <button
                class="ad__tab-btn"
                :class="{ 'ad__tab-btn--active': configForm.deliveryMode === 3 }"
                @click="configForm.deliveryMode = 3"
              >
                <IconRobot />
                自定义发货
              </button>
            </div>
          </div>

          <!-- ====== 自动发货视图 ====== -->
          <template v-if="configForm.deliveryMode === 1">
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

            <!-- Content Section -->
            <div class="ad__config-section">
              <div class="ad__config-section-title">发货内容</div>

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

              <div class="ad__image-section">
                <div class="ad__image-section-title">发货图片</div>
                <div class="ad__image-section-hint">可选，买家下单后先发送图片再发送文本</div>
                <MultiImageUploader
                  v-if="selectedAccountId"
                  :account-id="selectedAccountId"
                  v-model="configForm.autoDeliveryImageUrl"
                />
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
          </template>

          <!-- ====== 卡密发货视图 ====== -->
          <template v-if="configForm.deliveryMode === 2">
            <div class="ad__config-section">
              <div class="ad__config-section-title">发货设置</div>

              <div class="ad__toggle-row">
                <div class="ad__toggle-info">
                  <div class="ad__toggle-label">自动发货</div>
                  <div class="ad__toggle-hint">买家下单后自动发送卡密</div>
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
                      ? '卡密发送成功后自动确认已发货'
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

            <div class="ad__config-section">
              <div class="ad__config-section-title">卡密配置绑定</div>
              <div style="margin-bottom: 12px;">
                <el-select
                  v-model="selectedKamiConfigId"
                  placeholder="请选择卡密配置"
                  clearable
                  style="width: 100%;"
                  popper-class="kami-config-select-popper"
                >
                  <el-option
                    v-for="opt in kamiConfigOptions"
                    :key="opt.id"
                    :label="opt.aliasName || `配置#${opt.id}`"
                    :value="String(opt.id)"
                  >
                    <div class="kami-option">
                      <span class="kami-option__name">{{ opt.aliasName || `配置#${opt.id}` }}</span>
                      <span class="kami-option__stats">
                        <span class="kami-option__avail">可用{{ opt.availableCount }}</span>
                        <span class="kami-option__divider">/</span>
                        <span class="kami-option__total">总{{ opt.totalCount }}</span>
                      </span>
                    </div>
                  </el-option>
                </el-select>
                <div v-if="kamiConfigOptions.length === 0" style="color: #86868b; font-size: 13px; margin-top: 8px;">
                  暂无卡密配置，请先在「卡密配置」页面创建
                </div>
              </div>

              <div style="margin-bottom: 12px;">
                <div style="display: flex; align-items: center; gap: 6px; margin-bottom: 6px;">
                  <span style="font-size: 13px; color: #6e6e73;">发货文案</span>
                  <el-tag size="small" type="info" effect="plain" style="font-size: 11px;">占位符 {kmKey}</el-tag>
                </div>
                <el-input
                  v-model="configForm.kamiDeliveryTemplate"
                  type="textarea"
                  :rows="3"
                  placeholder="可选，填写后发货时将用卡密替换{kmKey}发送，不填则直接发送卡密内容。例：您的卡密为：{kmKey}，请妥善保管"
                />
              </div>

              <div class="ad__image-section">
                <div class="ad__image-section-title">发货图片</div>
                <div class="ad__image-section-hint">可选，买家下单后先发送图片再发送卡密</div>
                <MultiImageUploader
                  v-if="selectedAccountId"
                  :account-id="selectedAccountId"
                  v-model="configForm.autoDeliveryImageUrl"
                />
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
          </template>

          <!-- ====== 自定义发货视图 ====== -->
          <template v-if="configForm.deliveryMode === 3">
            <!-- API Hint Panel -->
            <div class="ad__config-section">
              <div class="ad__api-hint">
                <div class="ad__api-hint-header">
                  <span class="ad__api-hint-title">API 接入指南</span>
                </div>
                <div class="ad__api-hint-desc">
                  自定义发货需调用 <code>/api/order/list</code> 获取待发货订单，再调用 <code>/api/order/confirmShipment</code> 确认发货。
                </div>

                <div class="ad__api-hint-cols">
                  <!-- Left: /api/order/list -->
                  <div class="ad__api-hint-col">
                    <div class="ad__api-hint-col-title">获取订单列表</div>

                    <div class="ad__api-hint-section">
                      <div class="ad__api-hint-label">
                        接口地址
                        <button class="ad__api-hint-copy-btn" @click="copyApiUrl">
                          <IconCopy /> 复制
                        </button>
                      </div>
                      <div class="ad__api-hint-code">POST {{ apiHintUrl }}</div>
                    </div>

                    <div class="ad__api-hint-section">
                      <div class="ad__api-hint-label">
                        请求参数
                        <button class="ad__api-hint-copy-btn" @click="copyApiParams">
                          <IconCopy /> 复制
                        </button>
                      </div>
                      <pre class="ad__api-hint-pre"><code>{{ apiHintParamsJson }}</code></pre>
                    </div>

                    <div class="ad__api-hint-params-desc">
                      <div class="ad__api-hint-params-title">参数说明</div>
                      <table class="ad__api-hint-table">
                        <thead>
                          <tr><th>参数</th><th>类型</th><th>必填</th><th>说明</th></tr>
                        </thead>
                        <tbody>
                          <tr><td>xianyuAccountId</td><td>number</td><td>否</td><td>闲鱼账号ID</td></tr>
                          <tr><td>xyGoodsId</td><td>string</td><td>否</td><td>闲鱼商品ID</td></tr>
                          <tr><td>orderStatus</td><td>number</td><td>否</td><td>1=待付款 2=待发货 3=已发货 4=已完成 5=已关闭</td></tr>
                          <tr><td>pageNum</td><td>number</td><td>是</td><td>页码</td></tr>
                          <tr><td>pageSize</td><td>number</td><td>是</td><td>每页条数</td></tr>
                        </tbody>
                      </table>
                    </div>
                  </div>

                  <!-- Right: /api/order/confirmShipment -->
                  <div class="ad__api-hint-col">
                    <div class="ad__api-hint-col-title">确认发货</div>

                    <div class="ad__api-hint-section">
                      <div class="ad__api-hint-label">
                        接口地址
                        <button class="ad__api-hint-copy-btn" @click="copyConfirmShipmentUrl">
                          <IconCopy /> 复制
                        </button>
                      </div>
                      <div class="ad__api-hint-code">POST {{ confirmShipmentUrl }}</div>
                    </div>

                    <div class="ad__api-hint-section">
                      <div class="ad__api-hint-label">
                        请求参数
                        <button class="ad__api-hint-copy-btn" @click="copyConfirmShipmentParams">
                          <IconCopy /> 复制
                        </button>
                      </div>
                      <pre class="ad__api-hint-pre"><code>{{ confirmShipmentParamsJson }}</code></pre>
                    </div>

                    <div class="ad__api-hint-params-desc">
                      <div class="ad__api-hint-params-title">参数说明</div>
                      <table class="ad__api-hint-table">
                        <thead>
                          <tr><th>参数</th><th>类型</th><th>必填</th><th>说明</th></tr>
                        </thead>
                        <tbody>
                          <tr><td>xianyuAccountId</td><td>number</td><td>是</td><td>闲鱼账号ID</td></tr>
                          <tr><td>orderId</td><td>string</td><td>是</td><td>订单ID</td></tr>
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- Records Section (shared) -->
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
                  <span class="ad__record-card-value ad__record-card-value--content" :title="record.content">{{ record.content || '-' }}</span>
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

<style>
.kami-config-select-popper {
  min-width: 180px !important;
}
.kami-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 8px;
}
.kami-option__name {
  font-size: 14px;
  color: #1d1d1f;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.kami-option__stats {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 11px;
  white-space: nowrap;
  flex-shrink: 0;
}
.kami-option__avail {
  color: #34c759;
  font-weight: 600;
}
.kami-option__divider {
  color: #c0c4cc;
}
.kami-option__total {
  color: #909399;
}
</style>
