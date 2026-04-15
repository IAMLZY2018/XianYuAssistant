<script setup lang="ts">
import { ref, watch, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getGoodsDetail, deleteItem } from '@/api/goods'
import { showSuccess, showError, showConfirm } from '@/utils'
import { getGoodsStatusText, formatPrice, formatTime } from '@/utils'
import type { GoodsItemWithConfig } from '@/api/goods'

import IconImage from '@/components/icons/IconImage.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconRobot from '@/components/icons/IconRobot.vue'
import IconSparkle from '@/components/icons/IconSparkle.vue'
import IconTrash from '@/components/icons/IconTrash.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'

interface Props {
  modelValue: boolean
  goodsId: string
  accountId: number | null
}

interface Emits {
  (e: 'update:modelValue', value: boolean): void
  (e: 'refresh'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()
const router = useRouter()

const loading = ref(false)
const goodsDetail = ref<GoodsItemWithConfig | null>(null)
const currentImageIndex = ref(0)
const images = ref<string[]>([])

const isMobile = ref(false)
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

// 状态颜色
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

// 加载商品详情
const loadDetail = async () => {
  if (!props.goodsId) return
  loading.value = true
  try {
    const response = await getGoodsDetail(props.goodsId)
    if (response.code === 0 || response.code === 200) {
      goodsDetail.value = response.data?.itemWithConfig || null

      if (goodsDetail.value?.item.infoPic) {
        try {
          const infoPicArray = JSON.parse(goodsDetail.value.item.infoPic)
          images.value = infoPicArray.map((pic: any) => pic.url)
        } catch (e) {
          images.value = []
        }
      }
      if (images.value.length === 0 && goodsDetail.value?.item.coverPic) {
        images.value = [goodsDetail.value.item.coverPic]
      }
      currentImageIndex.value = 0
    } else {
      throw new Error(response.msg || '获取商品详情失败')
    }
  } catch (error: any) {
    console.error('加载商品详情失败:', error)
  } finally {
    loading.value = false
  }
}

// 配置自动发货
const handleConfigAutoDelivery = () => {
  if (!goodsDetail.value) return
  router.push({
    path: '/auto-delivery',
    query: {
      accountId: props.accountId?.toString(),
      goodsId: goodsDetail.value.item.xyGoodId
    }
  })
  handleClose()
}

// 删除商品
const handleDelete = async () => {
  if (!props.accountId || !goodsDetail.value) return
  try {
    await showConfirm(
      `确定要删除商品 "${goodsDetail.value.item.title}" 吗？此操作不可恢复。`,
      '删除确认'
    )
    const response = await deleteItem({
      xianyuAccountId: props.accountId,
      xyGoodsId: goodsDetail.value.item.xyGoodId
    })
    if (response.code === 0 || response.code === 200) {
      showSuccess('商品删除成功')
      handleClose()
      emit('refresh')
    } else {
      throw new Error(response.msg || '删除失败')
    }
  } catch (error: any) {
    if (error === 'cancel') return
    showError('删除失败: ' + error.message)
  }
}

// 图片切换
const prevImage = () => {
  if (currentImageIndex.value > 0) {
    currentImageIndex.value--
  }
}

const nextImage = () => {
  if (currentImageIndex.value < images.value.length - 1) {
    currentImageIndex.value++
  }
}

const selectImage = (index: number) => {
  currentImageIndex.value = index
}

// 关闭
const handleClose = () => {
  emit('update:modelValue', false)
  goodsDetail.value = null
  images.value = []
}

// 点击遮罩关闭（仅桌面端）
const handleOverlayClick = (e: MouseEvent) => {
  if ((e.target as HTMLElement).classList.contains('detail-overlay')) {
    handleClose()
  }
}

watch(() => props.modelValue, (val) => {
  if (val) loadDetail()
})

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize)
})
</script>

<template>
  <!-- Overlay -->
  <Transition name="overlay-fade">
    <div v-if="modelValue" class="detail-overlay" @click="handleOverlayClick">
      <!-- Mobile: Full-screen -->
      <div v-if="isMobile" class="detail-mobile" @click.stop>
        <!-- Mobile Header -->
        <div class="detail-mobile__header">
          <button class="detail-mobile__back" @click="handleClose">
            <IconChevronLeft />
            <span>返回</span>
          </button>
          <span class="detail-mobile__title">商品详情</span>
          <div style="width: 60px"></div>
        </div>

        <!-- Mobile Body -->
        <div class="detail-mobile__body" v-if="!loading && goodsDetail">
          <!-- Image Gallery -->
          <div class="detail-gallery" v-if="images.length > 0">
            <div class="detail-gallery__main">
              <img :src="images[currentImageIndex]" class="detail-gallery__img" />
              <button
                v-if="images.length > 1 && currentImageIndex > 0"
                class="detail-gallery__nav detail-gallery__nav--prev"
                @click="prevImage"
              >
                <IconChevronLeft />
              </button>
              <button
                v-if="images.length > 1 && currentImageIndex < images.length - 1"
                class="detail-gallery__nav detail-gallery__nav--next"
                @click="nextImage"
              >
                <IconChevronRight />
              </button>
              <span v-if="images.length > 1" class="detail-gallery__counter">
                {{ currentImageIndex + 1 }} / {{ images.length }}
              </span>
            </div>
            <div v-if="images.length > 1" class="detail-gallery__thumbs">
              <div
                v-for="(img, idx) in images"
                :key="idx"
                class="detail-gallery__thumb"
                :class="{ 'detail-gallery__thumb--active': idx === currentImageIndex }"
                @click="selectImage(idx)"
              >
                <img :src="img" />
              </div>
            </div>
          </div>

          <!-- Info Section -->
          <div class="detail-info">
            <div class="detail-info__title">{{ goodsDetail.item.title }}</div>
            <div class="detail-info__id">ID: {{ goodsDetail.item.xyGoodId }}</div>

            <div class="detail-info__row">
              <span class="detail-info__price">{{ formatPrice(goodsDetail.item.soldPrice) }}</span>
              <span
                class="detail-info__status"
                :style="{
                  color: getStatusColor(goodsDetail.item.status),
                  background: getStatusBg(goodsDetail.item.status)
                }"
              >
                {{ getGoodsStatusText(goodsDetail.item.status).text }}
              </span>
            </div>

            <!-- Description -->
            <div v-if="goodsDetail.item.detailInfo" class="detail-info__desc-section">
              <div class="detail-info__section-title">商品描述</div>
              <div class="detail-info__desc">{{ goodsDetail.item.detailInfo }}</div>
            </div>

            <!-- Config -->
            <div class="detail-info__config">
              <div class="detail-info__config-item">
                <div class="detail-info__config-left">
                  <IconSend />
                  <span>自动发货</span>
                </div>
                <span
                  class="detail-info__config-value"
                  :class="{ 'detail-info__config-value--on': goodsDetail.xianyuAutoDeliveryOn === 1 }"
                >
                  {{ goodsDetail.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </div>
              <div class="detail-info__config-item">
                <div class="detail-info__config-left">
                  <IconRobot />
                  <span>自动回复</span>
                </div>
                <span
                  class="detail-info__config-value"
                  :class="{ 'detail-info__config-value--on': goodsDetail.xianyuAutoReplyOn === 1 }"
                >
                  {{ goodsDetail.xianyuAutoReplyOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </div>
            </div>

            <!-- Time -->
            <div class="detail-info__time">
              <div v-if="goodsDetail.item.createdTime" class="detail-info__time-item">
                <IconClock />
                <span>创建: {{ goodsDetail.item.createdTime }}</span>
              </div>
              <div v-if="goodsDetail.item.updatedTime" class="detail-info__time-item">
                <IconClock />
                <span>更新: {{ goodsDetail.item.updatedTime }}</span>
              </div>
            </div>

            <!-- Actions -->
            <div class="detail-info__actions">
              <button class="detail-info__action detail-info__action--config" @click="handleConfigAutoDelivery">
                <IconSparkle />
                <span>配置发货</span>
              </button>
              <button class="detail-info__action detail-info__action--delete" @click="handleDelete">
                <IconTrash />
                <span>删除</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="detail-loading">
          <div class="detail-loading__spinner"></div>
          <span>加载中...</span>
        </div>
      </div>

      <!-- Desktop: Side Panel -->
      <div v-else class="detail-panel" @click.stop>
        <div class="detail-panel__header">
          <span class="detail-panel__title">商品详情</span>
          <button class="detail-panel__close" @click="handleClose">
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M1 1L13 13M13 1L1 13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
        </div>

        <div class="detail-panel__body" v-if="!loading && goodsDetail">
          <!-- Image Gallery -->
          <div class="detail-gallery" v-if="images.length > 0">
            <div class="detail-gallery__main">
              <img :src="images[currentImageIndex]" class="detail-gallery__img" />
              <button
                v-if="images.length > 1 && currentImageIndex > 0"
                class="detail-gallery__nav detail-gallery__nav--prev"
                @click="prevImage"
              >
                <IconChevronLeft />
              </button>
              <button
                v-if="images.length > 1 && currentImageIndex < images.length - 1"
                class="detail-gallery__nav detail-gallery__nav--next"
                @click="nextImage"
              >
                <IconChevronRight />
              </button>
              <span v-if="images.length > 1" class="detail-gallery__counter">
                {{ currentImageIndex + 1 }} / {{ images.length }}
              </span>
            </div>
            <div v-if="images.length > 1" class="detail-gallery__thumbs">
              <div
                v-for="(img, idx) in images"
                :key="idx"
                class="detail-gallery__thumb"
                :class="{ 'detail-gallery__thumb--active': idx === currentImageIndex }"
                @click="selectImage(idx)"
              >
                <img :src="img" />
              </div>
            </div>
          </div>

          <!-- No Image -->
          <div v-else class="detail-gallery detail-gallery--empty">
            <div class="detail-gallery__placeholder">
              <IconImage />
              <span>暂无图片</span>
            </div>
          </div>

          <!-- Info Section -->
          <div class="detail-info">
            <div class="detail-info__title">{{ goodsDetail.item.title }}</div>
            <div class="detail-info__id">ID: {{ goodsDetail.item.xyGoodId }}</div>

            <div class="detail-info__row">
              <span class="detail-info__price">{{ formatPrice(goodsDetail.item.soldPrice) }}</span>
              <span
                class="detail-info__status"
                :style="{
                  color: getStatusColor(goodsDetail.item.status),
                  background: getStatusBg(goodsDetail.item.status)
                }"
              >
                {{ getGoodsStatusText(goodsDetail.item.status).text }}
              </span>
            </div>

            <!-- Description -->
            <div v-if="goodsDetail.item.detailInfo" class="detail-info__desc-section">
              <div class="detail-info__section-title">商品描述</div>
              <div class="detail-info__desc">{{ goodsDetail.item.detailInfo }}</div>
            </div>

            <!-- Config -->
            <div class="detail-info__config">
              <div class="detail-info__config-item">
                <div class="detail-info__config-left">
                  <IconSend />
                  <span>自动发货</span>
                </div>
                <span
                  class="detail-info__config-value"
                  :class="{ 'detail-info__config-value--on': goodsDetail.xianyuAutoDeliveryOn === 1 }"
                >
                  {{ goodsDetail.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </div>
              <div class="detail-info__config-item">
                <div class="detail-info__config-left">
                  <IconRobot />
                  <span>自动回复</span>
                </div>
                <span
                  class="detail-info__config-value"
                  :class="{ 'detail-info__config-value--on': goodsDetail.xianyuAutoReplyOn === 1 }"
                >
                  {{ goodsDetail.xianyuAutoReplyOn === 1 ? '已开启' : '已关闭' }}
                </span>
              </div>
            </div>

            <!-- Time -->
            <div class="detail-info__time">
              <div v-if="goodsDetail.item.createdTime" class="detail-info__time-item">
                <IconClock />
                <span>创建: {{ goodsDetail.item.createdTime }}</span>
              </div>
              <div v-if="goodsDetail.item.updatedTime" class="detail-info__time-item">
                <IconClock />
                <span>更新: {{ goodsDetail.item.updatedTime }}</span>
              </div>
            </div>

            <!-- Actions -->
            <div class="detail-info__actions">
              <button class="detail-info__action detail-info__action--config" @click="handleConfigAutoDelivery">
                <IconSparkle />
                <span>配置发货</span>
              </button>
              <button class="detail-info__action detail-info__action--delete" @click="handleDelete">
                <IconTrash />
                <span>删除</span>
              </button>
            </div>
          </div>
        </div>

        <!-- Loading -->
        <div v-if="loading" class="detail-loading">
          <div class="detail-loading__spinner"></div>
          <span>加载中...</span>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
/* ============================================================
   Shared Tokens
   ============================================================ */
.detail-overlay,
.detail-panel,
.detail-mobile {
  --d-bg: transparent;
  --d-surface: rgba(255, 255, 255, 0.82);
  --d-surface-hover: rgba(255, 255, 255, 0.95);
  --d-border: rgba(0, 0, 0, 0.06);
  --d-border-strong: rgba(0, 0, 0, 0.12);
  --d-text-1: #1d1d1f;
  --d-text-2: #6e6e73;
  --d-text-3: #86868b;
  --d-accent: #007aff;
  --d-danger: #ff3b30;
  --d-success: #34c759;
  --d-price: #ff3b30;
  --d-r-sm: 8px;
  --d-r-md: 12px;
  --d-r-lg: 16px;
  --d-ease: 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* ============================================================
   Overlay
   ============================================================ */
.detail-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.4);
  z-index: 900;
  backdrop-filter: blur(4px);
  -webkit-backdrop-filter: blur(4px);
  display: flex;
  align-items: center;
  justify-content: center;
}

/* ============================================================
   Desktop Panel
   ============================================================ */
.detail-panel {
  width: 560px;
  max-height: 90vh;
  background: #fff;
  border-radius: var(--d-r-lg);
  box-shadow: 0 24px 80px rgba(0, 0, 0, 0.18), 0 4px 12px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: panel-in 0.25s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes panel-in {
  from { opacity: 0; transform: scale(0.96) translateY(8px); }
  to { opacity: 1; transform: scale(1) translateY(0); }
}

.detail-panel__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--d-border);
  flex-shrink: 0;
}

.detail-panel__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--d-text-1);
}

.detail-panel__close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: none;
  background: rgba(0, 0, 0, 0.05);
  color: var(--d-text-2);
  cursor: pointer;
  transition: all var(--d-ease);
}

@media (hover: hover) {
  .detail-panel__close:hover {
    background: rgba(0, 0, 0, 0.1);
    color: var(--d-text-1);
  }
}

.detail-panel__body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.12) transparent;
}

.detail-panel__body::-webkit-scrollbar {
  width: 5px;
}

.detail-panel__body::-webkit-scrollbar-track {
  background: transparent;
}

.detail-panel__body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}

/* ============================================================
   Mobile Full-screen
   ============================================================ */
.detail-mobile {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  z-index: 901;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  animation: mobile-slide-in 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

@keyframes mobile-slide-in {
  from { transform: translateX(100%); }
  to { transform: translateX(0); }
}

.detail-mobile__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  padding-top: max(8px, env(safe-area-inset-top, 8px));
  border-bottom: 1px solid var(--d-border);
  flex-shrink: 0;
  height: 48px;
}

.detail-mobile__back {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  height: 36px;
  padding: 0 8px;
  font-size: 16px;
  font-weight: 500;
  color: var(--d-accent);
  background: none;
  border: none;
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.detail-mobile__back svg {
  width: 20px;
  height: 20px;
}

.detail-mobile__title {
  font-size: 16px;
  font-weight: 600;
  color: var(--d-text-1);
}

.detail-mobile__body {
  flex: 1;
  overflow-y: auto;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.12) transparent;
}

.detail-mobile__body::-webkit-scrollbar {
  width: 4px;
}

.detail-mobile__body::-webkit-scrollbar-track {
  background: transparent;
}

.detail-mobile__body::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.1);
  border-radius: 2px;
}

/* ============================================================
   Gallery (Shared)
   ============================================================ */
.detail-gallery {
  padding: 16px;
  padding-bottom: 8px;
}

.detail-gallery__main {
  position: relative;
  width: 100%;
  height: 320px;
  border-radius: var(--d-r-md);
  overflow: hidden;
  background: rgba(0, 0, 0, 0.03);
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-gallery__img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.detail-gallery__nav {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.85);
  color: var(--d-text-1);
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
  transition: all var(--d-ease);
  -webkit-tap-highlight-color: transparent;
}

.detail-gallery__nav svg {
  width: 16px;
  height: 16px;
}

.detail-gallery__nav--prev { left: 8px; }
.detail-gallery__nav--next { right: 8px; }

@media (hover: hover) {
  .detail-gallery__nav:hover {
    background: rgba(255, 255, 255, 0.95);
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.16);
  }
}

.detail-gallery__counter {
  position: absolute;
  bottom: 8px;
  right: 8px;
  font-size: 11px;
  font-weight: 500;
  color: #fff;
  background: rgba(0, 0, 0, 0.5);
  padding: 3px 8px;
  border-radius: 10px;
}

.detail-gallery__thumbs {
  display: flex;
  gap: 6px;
  margin-top: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  -webkit-overflow-scrolling: touch;
}

.detail-gallery__thumb {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  border: 2px solid transparent;
  cursor: pointer;
  transition: border-color var(--d-ease);
  background: rgba(0, 0, 0, 0.03);
}

.detail-gallery__thumb--active {
  border-color: var(--d-accent);
}

.detail-gallery__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-gallery--empty {
  display: flex;
  align-items: center;
  justify-content: center;
}

.detail-gallery__placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: var(--d-text-3);
  opacity: 0.4;
}

.detail-gallery__placeholder svg {
  width: 32px;
  height: 32px;
}

.detail-gallery__placeholder span {
  font-size: 13px;
}

/* ============================================================
   Info Section (Shared)
   ============================================================ */
.detail-info {
  padding: 16px 20px 24px;
}

.detail-info__title {
  font-size: 17px;
  font-weight: 600;
  color: var(--d-text-1);
  line-height: 1.4;
  margin-bottom: 4px;
}

.detail-info__id {
  font-size: 11px;
  color: var(--d-text-3);
  font-family: 'SF Mono', 'Menlo', monospace;
  margin-bottom: 12px;
}

.detail-info__row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.detail-info__price {
  font-size: 22px;
  font-weight: 700;
  color: var(--d-price);
  font-variant-numeric: tabular-nums;
}

.detail-info__status {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  line-height: 1;
}

.detail-info__desc-section {
  margin-bottom: 16px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.03);
  border-radius: var(--d-r-sm);
}

.detail-info__section-title {
  font-size: 12px;
  font-weight: 600;
  color: var(--d-text-2);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.02em;
}

.detail-info__desc {
  font-size: 13px;
  color: var(--d-text-2);
  line-height: 1.6;
  white-space: pre-wrap;
  max-height: 120px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.08) transparent;
}

.detail-info__desc::-webkit-scrollbar {
  width: 4px;
}

.detail-info__desc::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.08);
  border-radius: 2px;
}

.detail-info__config {
  display: flex;
  flex-direction: column;
  gap: 0;
  border: 1px solid var(--d-border);
  border-radius: var(--d-r-sm);
  overflow: hidden;
  margin-bottom: 16px;
}

.detail-info__config-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 12px;
}

.detail-info__config-item:not(:last-child) {
  border-bottom: 1px solid var(--d-border);
}

.detail-info__config-left {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  font-weight: 500;
  color: var(--d-text-1);
}

.detail-info__config-left svg {
  width: 15px;
  height: 15px;
  color: var(--d-text-3);
}

.detail-info__config-value {
  font-size: 12px;
  font-weight: 500;
  color: var(--d-text-3);
}

.detail-info__config-value--on {
  color: var(--d-success);
}

.detail-info__time {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-bottom: 16px;
}

.detail-info__time-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--d-text-3);
}

.detail-info__time-item svg {
  width: 13px;
  height: 13px;
}

.detail-info__actions {
  display: flex;
  gap: 8px;
}

.detail-info__action {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1;
  height: 40px;
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--d-r-sm);
  border: 1px solid;
  cursor: pointer;
  transition: all var(--d-ease);
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.detail-info__action svg {
  width: 15px;
  height: 15px;
}

.detail-info__action--config {
  color: var(--d-accent);
  border-color: rgba(0, 122, 255, 0.2);
}

@media (hover: hover) {
  .detail-info__action--config:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

.detail-info__action--delete {
  color: var(--d-danger);
  border-color: rgba(255, 59, 48, 0.2);
}

@media (hover: hover) {
  .detail-info__action--delete:hover {
    background: rgba(255, 59, 48, 0.06);
  }
}

.detail-info__action:active {
  transform: scale(0.97);
}

/* ============================================================
   Loading
   ============================================================ */
.detail-loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  padding: 60px;
  color: var(--d-text-3);
  font-size: 13px;
}

.detail-loading__spinner {
  width: 24px;
  height: 24px;
  border: 2px solid rgba(0, 0, 0, 0.08);
  border-top-color: var(--d-accent);
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
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

/* ============================================================
   Responsive
   ============================================================ */
@media screen and (max-width: 768px) {
  .detail-gallery__main {
    height: 280px;
  }

  .detail-info {
    padding: 12px 16px 24px;
  }

  .detail-info__price {
    font-size: 20px;
  }

  .detail-gallery__thumb {
    width: 40px;
    height: 40px;
  }
}

@media screen and (max-width: 480px) {
  .detail-gallery__main {
    height: 240px;
  }

  .detail-gallery {
    padding: 12px;
  }

  .detail-info__title {
    font-size: 15px;
  }

  .detail-info__price {
    font-size: 18px;
  }

  .detail-info__action {
    height: 36px;
    font-size: 12px;
  }
}
</style>
