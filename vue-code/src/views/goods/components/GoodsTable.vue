<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getGoodsStatusText, formatPrice } from '@/utils'
import type { GoodsItemWithConfig } from '@/api/goods'

import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconTrash from '@/components/icons/IconTrash.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconRobot from '@/components/icons/IconRobot.vue'
import IconImage from '@/components/icons/IconImage.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'

interface Props {
  goodsList: GoodsItemWithConfig[]
  loading?: boolean
}

interface Emits {
  (e: 'view', xyGoodId: string): void
  (e: 'toggleAutoDelivery', item: GoodsItemWithConfig, value: boolean): void
  (e: 'toggleAutoReply', item: GoodsItemWithConfig, value: boolean): void
  (e: 'configAutoDelivery', item: GoodsItemWithConfig): void
  (e: 'delete', xyGoodId: string, title: string): void
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

const getStatusColor = (status: number) => {
  const info = getGoodsStatusText(status)
  switch (info.type) {
    case 'success': return '#34c759'
    case 'warning': return '#ff9500'
    case 'info': return '#86868b'
    default: return '#007aff'
  }
}

const getStatusBg = (status: number) => {
  const info = getGoodsStatusText(status)
  switch (info.type) {
    case 'success': return 'rgba(52, 199, 89, 0.1)'
    case 'warning': return 'rgba(255, 149, 0, 0.1)'
    case 'info': return 'rgba(134, 134, 139, 0.1)'
    default: return 'rgba(0, 122, 255, 0.1)'
  }
}

// 图片加载失败处理
const handleImgError = (e: Event) => {
  const img = e.target as HTMLImageElement
  img.style.display = 'none'
}
</script>

<template>
  <!-- Mobile: Card View -->
  <div v-if="isMobile" class="card-list" :class="{ 'card-list--loading': loading }">
    <div
      v-for="item in goodsList"
      :key="item.item.xyGoodId"
      class="goods-card"
      @click="emit('view', item.item.xyGoodId)"
    >
      <div class="goods-card__image-wrap">
        <img
          v-if="item.item.coverPic"
          :src="item.item.coverPic"
          class="goods-card__image"
          @error="handleImgError"
        />
        <div v-else class="goods-card__image-placeholder">
          <IconImage />
        </div>
        <span
          class="goods-card__status"
          :style="{
            color: getStatusColor(item.item.status),
            background: getStatusBg(item.item.status)
          }"
        >
          {{ getGoodsStatusText(item.item.status).text }}
        </span>
      </div>

      <div class="goods-card__body">
        <div class="goods-card__title">{{ item.item.title }}</div>
        <div class="goods-card__meta">
          <span class="goods-card__price">{{ formatPrice(item.item.soldPrice) }}</span>
          <span class="goods-card__id">{{ item.item.xyGoodId }}</span>
        </div>

        <div class="goods-card__switches">
          <button
            class="goods-card__switch"
            :class="{ 'goods-card__switch--on': item.xianyuAutoDeliveryOn === 1 }"
            @click.stop="emit('toggleAutoDelivery', item, item.xianyuAutoDeliveryOn !== 1)"
          >
            <IconSend />
            <span>发货</span>
          </button>
          <button
            class="goods-card__switch"
            :class="{ 'goods-card__switch--on': item.xianyuAutoReplyOn === 1 }"
            @click.stop="emit('toggleAutoReply', item, item.xianyuAutoReplyOn !== 1)"
          >
            <IconRobot />
            <span>回复</span>
          </button>
        </div>

        <div class="goods-card__actions">
          <button
            class="goods-card__action goods-card__action--config"
            @click.stop="emit('configAutoDelivery', item)"
          >
            <IconSparkle />
            <span>配置</span>
          </button>
          <button
            class="goods-card__action goods-card__action--delete"
            @click.stop="emit('delete', item.item.xyGoodId, item.item.title)"
          >
            <IconTrash />
            <span>删除</span>
          </button>
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && goodsList.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无商品数据</p>
    </div>
  </div>

  <!-- Desktop/Tablet: Table View -->
  <div v-else class="table-container" :class="{ 'table-container--loading': loading }">
    <table class="table" v-if="goodsList.length > 0">
      <thead class="table__head">
        <tr>
          <th class="table__th table__th--image">图片</th>
          <th class="table__th">商品标题</th>
          <th class="table__th table__th--price">价格</th>
          <th class="table__th table__th--status">状态</th>
          <th class="table__th table__th--switch">自动发货</th>
          <th class="table__th table__th--switch">自动回复</th>
          <th class="table__th table__th--actions">操作</th>
        </tr>
      </thead>
      <tbody class="table__body">
        <tr v-for="item in goodsList" :key="item.item.xyGoodId" class="table__tr">
          <td class="table__td table__td--image">
            <div class="goods-thumb">
              <img
                v-if="item.item.coverPic"
                :src="item.item.coverPic"
                class="goods-thumb__img"
                @error="handleImgError"
                @click="emit('view', item.item.xyGoodId)"
              />
              <div v-else class="goods-thumb__placeholder">
                <IconImage />
              </div>
            </div>
          </td>
          <td class="table__td table__td--title" @click="emit('view', item.item.xyGoodId)">
            <div class="goods-title-cell">
              <span class="goods-title-cell__name">{{ item.item.title }}</span>
              <span class="goods-title-cell__id">ID: {{ item.item.xyGoodId }}</span>
            </div>
          </td>
          <td class="table__td table__td--price">
            <span class="price-text">{{ formatPrice(item.item.soldPrice) }}</span>
          </td>
          <td class="table__td table__td--status">
            <span
              class="status-tag"
              :style="{
                color: getStatusColor(item.item.status),
                background: getStatusBg(item.item.status)
              }"
            >
              {{ getGoodsStatusText(item.item.status).text }}
            </span>
          </td>
          <td class="table__td table__td--switch">
            <button
              class="toggle-btn"
              :class="{ 'toggle-btn--on': item.xianyuAutoDeliveryOn === 1 }"
              @click="emit('toggleAutoDelivery', item, item.xianyuAutoDeliveryOn !== 1)"
            >
              <span class="toggle-btn__track">
                <span class="toggle-btn__thumb"></span>
              </span>
            </button>
          </td>
          <td class="table__td table__td--switch">
            <button
              class="toggle-btn"
              :class="{ 'toggle-btn--on': item.xianyuAutoReplyOn === 1 }"
              @click="emit('toggleAutoReply', item, item.xianyuAutoReplyOn !== 1)"
            >
              <span class="toggle-btn__track">
                <span class="toggle-btn__thumb"></span>
              </span>
            </button>
          </td>
          <td class="table__td table__td--actions">
            <button class="table__action table__action--config" @click="emit('configAutoDelivery', item)">
              <IconSparkle />
              <span>配置</span>
            </button>
            <button class="table__action table__action--detail" @click="emit('view', item.item.xyGoodId)">
              <IconCheck />
              <span>详情</span>
            </button>
            <button class="table__action table__action--delete" @click="emit('delete', item.item.xyGoodId, item.item.title)">
              <IconTrash />
              <span>删除</span>
            </button>
          </td>
        </tr>
      </tbody>
    </table>

    <!-- Empty State -->
    <div v-if="!loading && goodsList.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无商品数据</p>
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
  --c-price: #ff3b30;
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

.goods-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-r-md);
  overflow: hidden;
  transition: all var(--c-ease);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
}

@media (hover: hover) {
  .goods-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    border-color: rgba(0, 0, 0, 0.15);
  }
}

.goods-card:active {
  transform: scale(0.98);
}

.goods-card__image-wrap {
  position: relative;
  width: 100%;
  height: 160px;
  background: rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.goods-card__image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.goods-card__image-placeholder {
  color: var(--c-text-3);
  opacity: 0.3;
}

.goods-card__image-placeholder svg {
  width: 32px;
  height: 32px;
}

.goods-card__status {
  position: absolute;
  top: 8px;
  right: 8px;
  font-size: 11px;
  font-weight: 600;
  padding: 3px 8px;
  border-radius: 12px;
  line-height: 1;
}

.goods-card__body {
  padding: 12px;
}

.goods-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}

.goods-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.goods-card__price {
  font-size: 16px;
  font-weight: 700;
  color: var(--c-price);
}

.goods-card__id {
  font-size: 11px;
  color: var(--c-text-3);
  font-family: 'SF Mono', 'Menlo', monospace;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-card__switches {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.goods-card__switch {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 30px;
  padding: 0 10px;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-3);
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid transparent;
  border-radius: 15px;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
}

.goods-card__switch svg {
  width: 13px;
  height: 13px;
}

.goods-card__switch--on {
  color: var(--c-accent);
  background: rgba(0, 122, 255, 0.08);
  border-color: rgba(0, 122, 255, 0.2);
}

.goods-card__actions {
  display: flex;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid var(--c-border);
}

.goods-card__action {
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

.goods-card__action svg {
  width: 13px;
  height: 13px;
}

.goods-card__action--config {
  color: var(--c-accent);
  border-color: rgba(0, 122, 255, 0.2);
}

@media (hover: hover) {
  .goods-card__action--config:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

.goods-card__action--delete {
  color: var(--c-danger);
  border-color: rgba(255, 59, 48, 0.2);
}

@media (hover: hover) {
  .goods-card__action--delete:hover {
    background: rgba(255, 59, 48, 0.06);
  }
}

.goods-card__action:active {
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

.table__th--image { width: 64px; }
.table__th--price { width: 100px; text-align: right; }
.table__th--status { width: 80px; }
.table__th--switch { width: 90px; text-align: center; }
.table__th--actions { width: 180px; text-align: center; }

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

/* Thumbnail */
.goods-thumb {
  width: 44px;
  height: 44px;
  border-radius: 6px;
  overflow: hidden;
  background: rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
}

.goods-thumb__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  cursor: pointer;
  transition: transform var(--c-ease);
}

@media (hover: hover) {
  .goods-thumb__img:hover {
    transform: scale(1.08);
  }
}

.goods-thumb__placeholder {
  color: var(--c-text-3);
  opacity: 0.3;
}

.goods-thumb__placeholder svg {
  width: 18px;
  height: 18px;
}

/* Title Cell */
.table__td--title {
  cursor: pointer;
}

.goods-title-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.goods-title-cell__name {
  font-weight: 500;
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 300px;
}

.goods-title-cell__id {
  font-size: 11px;
  color: var(--c-text-3);
  font-family: 'SF Mono', 'Menlo', monospace;
}

/* Price */
.table__td--price {
  text-align: right;
}

.price-text {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-price);
  font-variant-numeric: tabular-nums;
}

/* Status */
.table__td--status {}

.status-tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  line-height: 1;
}

/* Toggle Switch */
.table__td--switch {
  text-align: center;
}

.toggle-btn {
  display: inline-flex;
  align-items: center;
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  -webkit-tap-highlight-color: transparent;
}

.toggle-btn__track {
  width: 44px;
  height: 26px;
  border-radius: 13px;
  background: rgba(0, 0, 0, 0.12);
  position: relative;
  transition: background var(--c-ease);
  display: block;
}

.toggle-btn--on .toggle-btn__track {
  background: var(--c-success);
}

.toggle-btn__thumb {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #fff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
  position: absolute;
  top: 2px;
  left: 2px;
  transition: transform var(--c-ease);
}

.toggle-btn--on .toggle-btn__thumb {
  transform: translateX(18px);
}

/* Action Buttons in Table */
.table__td--actions {
  text-align: center;
}

.table__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  height: 30px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 500;
  border-radius: 6px;
  border: none;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.table__action svg {
  width: 13px;
  height: 13px;
}

.table__action--config {
  color: var(--c-accent);
}

@media (hover: hover) {
  .table__action--config:hover {
    background: rgba(0, 122, 255, 0.08);
  }
}

.table__action--detail {
  color: var(--c-success);
}

@media (hover: hover) {
  .table__action--detail:hover {
    background: rgba(52, 199, 89, 0.08);
  }
}

.table__action--delete {
  color: var(--c-danger);
}

@media (hover: hover) {
  .table__action--delete:hover {
    background: rgba(255, 59, 48, 0.08);
  }
}

.table__action:active {
  transform: scale(0.95);
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

  .goods-card__image-wrap {
    height: 140px;
  }

  .goods-card__body {
    padding: 10px;
  }

  .goods-card__title {
    font-size: 13px;
  }

  .goods-card__price {
    font-size: 15px;
  }

  .goods-card__switch {
    height: 28px;
    padding: 0 8px;
    font-size: 11px;
  }

  .goods-card__action {
    height: 30px;
    font-size: 11px;
  }
}
</style>
