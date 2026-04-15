<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { formatTime } from '@/utils'
import { OrderStatus } from '@/constants/orderStatus'
import type { OrderItem } from '../useOrderManager'

import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconCopy from '@/components/icons/IconCopy.vue'
import IconTruck from '@/components/icons/IconTruck.vue'
import IconUser from '@/components/icons/IconUser.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconShoppingBag from '@/components/icons/IconShoppingBag.vue'

interface Props {
  orderList: OrderItem[]
  loading?: boolean
}

interface Emits {
  (e: 'copySid', sid: string): void
  (e: 'confirmShipment', item: OrderItem): void
}

defineProps<Props>()
const emit = defineEmits<Emits>()

const isMobile = ref(false)
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize)
})

const getStatusColor = (status: number | null) => {
  switch (status) {
    case OrderStatus.WAITING_PAYMENT: return '#ff9500'
    case OrderStatus.WAITING_DELIVERY: return '#007aff'
    case OrderStatus.DELIVERED: return '#34c759'
    case OrderStatus.COMPLETED: return '#34c759'
    case OrderStatus.CLOSED: return '#86868b'
    default: return '#86868b'
  }
}

const getStatusBg = (status: number | null) => {
  switch (status) {
    case OrderStatus.WAITING_PAYMENT: return 'rgba(255, 149, 0, 0.1)'
    case OrderStatus.WAITING_DELIVERY: return 'rgba(0, 122, 255, 0.1)'
    case OrderStatus.DELIVERED: return 'rgba(52, 199, 89, 0.1)'
    case OrderStatus.COMPLETED: return 'rgba(52, 199, 89, 0.1)'
    case OrderStatus.CLOSED: return 'rgba(134, 134, 139, 0.1)'
    default: return 'rgba(134, 134, 139, 0.1)'
  }
}

const getStatusText = (status: number | null) => {
  switch (status) {
    case OrderStatus.WAITING_PAYMENT: return '待付款'
    case OrderStatus.WAITING_DELIVERY: return '待发货'
    case OrderStatus.DELIVERED: return '已发货'
    case OrderStatus.COMPLETED: return '已完成'
    case OrderStatus.CLOSED: return '已关闭'
    default: return '未知'
  }
}
</script>

<template>
  <!-- Mobile: Card View -->
  <div v-if="isMobile" class="card-list" :class="{ 'card-list--loading': loading }">
    <div
      v-for="order in orderList"
      :key="order.id"
      class="order-card"
    >
      <div class="order-card__header">
        <span class="order-card__id">{{ order.orderId }}</span>
        <span
          class="order-card__status"
          :style="{
            color: getStatusColor(order.orderStatus),
            background: getStatusBg(order.orderStatus)
          }"
        >
          {{ order.orderStatusText || getStatusText(order.orderStatus) }}
        </span>
      </div>

      <div class="order-card__body">
        <div class="order-card__row">
          <IconShoppingBag />
          <span class="order-card__label">商品</span>
          <span class="order-card__value">{{ order.goodsTitle || '-' }}</span>
        </div>
        <div class="order-card__row">
          <IconUser />
          <span class="order-card__label">买家</span>
          <span class="order-card__value">{{ order.buyerUserName || '-' }}</span>
        </div>
        <div class="order-card__row">
          <IconClock />
          <span class="order-card__label">时间</span>
          <span class="order-card__value">{{ formatTime(order.createTime) }}</span>
        </div>
        <div class="order-card__row">
          <IconSend />
          <span class="order-card__label">发货</span>
          <span
            class="order-card__delivery"
            :class="{ 'order-card__delivery--success': order.autoDeliverySuccess }"
          >
            {{ order.autoDeliverySuccess ? '已自动发货' : '未发货' }}
          </span>
        </div>
      </div>

      <div class="order-card__footer">
        <button class="order-card__action order-card__action--copy" @click="emit('copySid', order.sid)">
          <IconCopy />
          <span>复制会话ID</span>
        </button>
        <button
          v-if="order.orderStatus === OrderStatus.WAITING_DELIVERY"
          class="order-card__action order-card__action--ship"
          :class="{ 'order-card__action--loading': order.confirming }"
          @click="emit('confirmShipment', order)"
        >
          <IconTruck />
          <span>{{ order.confirming ? '处理中' : '确认发货' }}</span>
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && orderList.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无订单数据</p>
    </div>
  </div>

  <!-- Desktop/Tablet: Table View -->
  <div v-else class="table-container" :class="{ 'table-container--loading': loading }">
    <table class="table" v-if="orderList.length > 0">
      <thead class="table__head">
        <tr>
          <th class="table__th">订单ID</th>
          <th class="table__th">商品名称</th>
          <th class="table__th table__th--center">买家</th>
          <th class="table__th table__th--center">会话ID</th>
          <th class="table__th table__th--center">创建时间</th>
          <th class="table__th table__th--center">自动发货</th>
          <th class="table__th table__th--center">状态</th>
          <th class="table__th table__th--actions">操作</th>
        </tr>
      </thead>
      <tbody class="table__body">
        <tr v-for="order in orderList" :key="order.id" class="table__tr">
          <td class="table__td">
            <span class="order-id">{{ order.orderId }}</span>
          </td>
          <td class="table__td table__td--title">
            <div class="order-title-cell">
              <span class="order-title-cell__name">{{ order.goodsTitle || '-' }}</span>
              <span class="order-title-cell__remark">{{ order.accountRemark }}</span>
            </div>
          </td>
          <td class="table__td table__td--center">
            <span class="buyer-name">{{ order.buyerUserName || '-' }}</span>
          </td>
          <td class="table__td table__td--center">
            <button class="sid-btn" @click="emit('copySid', order.sid)" :title="order.sid">
              <IconCopy />
              <span>复制</span>
            </button>
          </td>
          <td class="table__td table__td--center">
            <span class="time-text">{{ formatTime(order.createTime) }}</span>
          </td>
          <td class="table__td table__td--center">
            <span
              class="delivery-tag"
              :class="{ 'delivery-tag--success': order.autoDeliverySuccess }"
            >
              {{ order.autoDeliverySuccess ? '成功' : '未发货' }}
            </span>
          </td>
          <td class="table__td table__td--center">
            <span
              class="status-tag"
              :style="{
                color: getStatusColor(order.orderStatus),
                background: getStatusBg(order.orderStatus)
              }"
            >
              {{ order.orderStatusText || getStatusText(order.orderStatus) }}
            </span>
          </td>
          <td class="table__td table__td--actions">
            <button
              v-if="order.orderStatus === OrderStatus.WAITING_DELIVERY"
              class="table__action table__action--ship"
              :class="{ 'table__action--loading': order.confirming }"
              @click="emit('confirmShipment', order)"
            >
              <IconTruck />
              <span>{{ order.confirming ? '处理中' : '发货' }}</span>
            </button>
            <span v-else class="table__action-placeholder">-</span>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Empty State -->
    <div v-if="!loading && orderList.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无订单数据</p>
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   Shared Tokens
   ============================================================ */
.card-list,
.table-container {
  --c-bg: transparent;
  --c-surface: #ffffff;
  --c-border: rgba(0, 0, 0, 0.06);
  --c-border-strong: rgba(0, 0, 0, 0.1);
  --c-text-1: #1d1d1f;
  --c-text-2: #6e6e73;
  --c-text-3: #86868b;
  --c-accent: #007aff;
  --c-danger: #ff3b30;
  --c-success: #34c759;
  --c-r-sm: 8px;
  --c-r-md: 12px;
  --c-ease: 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* ============================================================
   Mobile Card View
   ============================================================ */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 16px;
  padding-bottom: 24px;
  min-height: 100%;
}

.order-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-r-md);
  overflow: hidden;
  transition: all var(--c-ease);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.order-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
  border-bottom: 1px solid var(--c-border);
}

.order-card__id {
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-1);
  font-family: 'SF Mono', 'Menlo', monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 60%;
}

.order-card__status {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
  line-height: 1;
  flex-shrink: 0;
}

.order-card__body {
  padding: 10px 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.order-card__row {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  min-height: 20px;
}

.order-card__row svg {
  width: 13px;
  height: 13px;
  color: var(--c-text-3);
  flex-shrink: 0;
}

.order-card__label {
  color: var(--c-text-3);
  flex-shrink: 0;
  min-width: 32px;
}

.order-card__value {
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card__delivery {
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-3);
}

.order-card__delivery--success {
  color: var(--c-success);
}

.order-card__footer {
  display: flex;
  gap: 8px;
  padding: 8px 12px;
  border-top: 1px solid var(--c-border);
}

.order-card__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  flex: 1;
  height: 32px;
  font-size: 12px;
  font-weight: 500;
  border-radius: var(--c-r-sm);
  border: 1px solid;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.order-card__action svg {
  width: 13px;
  height: 13px;
}

.order-card__action--copy {
  color: var(--c-accent);
  border-color: rgba(0, 122, 255, 0.2);
}

@media (hover: hover) {
  .order-card__action--copy:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

.order-card__action--ship {
  color: var(--c-success);
  border-color: rgba(52, 199, 89, 0.2);
}

@media (hover: hover) {
  .order-card__action--ship:hover {
    background: rgba(52, 199, 89, 0.06);
  }
}

.order-card__action--loading {
  opacity: 0.6;
  pointer-events: none;
}

.order-card__action:active {
  transform: scale(0.97);
}

/* ============================================================
   Desktop Table View
   ============================================================ */
.table-container {
  min-height: 100%;
}

.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}

/* Table Head */
.table__head {
  position: sticky;
  top: 0;
  z-index: 2;
}

.table__th {
  text-align: left;
  padding: 10px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-3);
  letter-spacing: 0.01em;
  background: #fafafa;
  border-bottom: 1px solid var(--c-border-strong);
  white-space: nowrap;
  user-select: none;
}

.table__th--center {
  text-align: center;
}

.table__th--actions {
  width: 100px;
  text-align: center;
}

/* Table Body */
.table__tr {
  transition: background var(--c-ease);
}

.table__tr:not(:last-child) .table__td {
  border-bottom: 1px solid var(--c-border);
}

@media (hover: hover) {
  .table__tr:hover .table__td {
    background: rgba(0, 0, 0, 0.02);
  }
}

.table__td {
  padding: 10px 16px;
  color: var(--c-text-1);
  white-space: nowrap;
  background: transparent;
  transition: background var(--c-ease);
  line-height: 1.5;
}

.table__td--center {
  text-align: center;
}

.table__td--actions {
  text-align: center;
}

/* Order ID */
.order-id {
  font-size: 12px;
  font-family: 'SF Mono', 'Menlo', monospace;
  color: var(--c-text-2);
}

/* Title Cell */
.order-title-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.order-title-cell__name {
  font-weight: 500;
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 280px;
}

.order-title-cell__remark {
  font-size: 11px;
  color: var(--c-text-3);
}

/* Buyer */
.buyer-name {
  font-size: 13px;
  color: var(--c-text-2);
}

/* Session ID Copy Button */
.sid-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-accent);
  background: transparent;
  border: 1px solid rgba(0, 122, 255, 0.15);
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
}

.sid-btn svg {
  width: 12px;
  height: 12px;
}

@media (hover: hover) {
  .sid-btn:hover {
    background: rgba(0, 122, 255, 0.06);
    border-color: rgba(0, 122, 255, 0.3);
  }
}

.sid-btn:active {
  transform: scale(0.96);
}

/* Time */
.time-text {
  font-size: 12px;
  color: var(--c-text-2);
  font-variant-numeric: tabular-nums;
}

/* Delivery Tag */
.delivery-tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 12px;
  line-height: 1;
  color: var(--c-text-3);
  background: rgba(0, 0, 0, 0.04);
}

.delivery-tag--success {
  color: var(--c-success);
  background: rgba(52, 199, 89, 0.1);
}

/* Status Tag */
.status-tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  line-height: 1;
}

/* Table Action Buttons */
.table__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 6px;
  border: 1px solid rgba(52, 199, 89, 0.2);
  color: var(--c-success);
  background: transparent;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
}

.table__action svg {
  width: 13px;
  height: 13px;
}

@media (hover: hover) {
  .table__action--ship:hover {
    background: rgba(52, 199, 89, 0.06);
  }
}

.table__action--loading {
  opacity: 0.6;
  pointer-events: none;
}

.table__action:active {
  transform: scale(0.95);
}

.table__action-placeholder {
  color: var(--c-text-3);
}

/* ============================================================
   Empty State
   ============================================================ */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 16px;
  gap: 12px;
}

.empty-state__icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--c-text-3);
  opacity: 0.35;
}

.empty-state__icon svg {
  width: 36px;
  height: 36px;
}

.empty-state__text {
  font-size: 14px;
  color: var(--c-text-3);
}

/* ============================================================
   Loading State
   ============================================================ */
.card-list--loading,
.table-container--loading {
  opacity: 0.5;
  pointer-events: none;
}

/* ============================================================
   Responsive
   ============================================================ */
@media screen and (max-width: 480px) {
  .card-list {
    padding: 12px;
    gap: 8px;
  }

  .order-card__header {
    padding: 8px 10px;
  }

  .order-card__body {
    padding: 8px 10px;
    gap: 6px;
  }

  .order-card__footer {
    padding: 6px 10px;
  }

  .order-card__action {
    height: 30px;
    font-size: 11px;
  }
}
</style>
