<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Account } from '@/types'

import IconClock from '@/components/icons/IconClock.vue'
import IconArrowRight from '@/components/icons/IconArrowRight.vue'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconCookie from '@/components/icons/IconCookie.vue'
import IconWs from '@/components/icons/IconWs.vue'

interface ConnectionInfo {
  connected?: boolean
  status?: string
  cookieStatus?: number
  tokenExpireTime?: number
}

interface Props {
  accounts: Account[]
  connections: Map<number, ConnectionInfo>
  selectedId: number | null
  loading?: boolean
}

interface Emits {
  (e: 'select', account: Account): void
}

const props = defineProps<Props>()
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

const getCookieColor = (status?: number) => {
  if (status === 1) return 'var(--c-success)'
  if (status === 2) return 'var(--c-warning)'
  if (status === 3) return 'var(--c-danger)'
  return 'var(--c-text-3)'
}

const getCookieBg = (status?: number) => {
  if (status === 1) return 'rgba(48,209,88,.2)'
  if (status === 2) return 'rgba(255,159,10,.18)'
  if (status === 3) return 'rgba(255,69,58,.15)'
  return 'rgba(120,120,128,.12)'
}

const getCookieText = (status?: number) => {
  if (status === 1) return '有效'
  if (status === 2) return '过期'
  if (status === 3) return '失效'
  return '未知'
}

const getWsColor = (info?: ConnectionInfo) => {
  if (!info) return 'var(--c-text-3)'
  return info.connected ? 'var(--c-success)' : 'var(--c-danger)'
}

const getWsBg = (info?: ConnectionInfo) => {
  if (!info) return 'rgba(120,120,128,.12)'
  return info.connected ? 'rgba(48,209,88,.2)' : 'rgba(255,69,58,.15)'
}

const getWsText = (info?: ConnectionInfo) => {
  if (!info) return '未检测'
  return info.connected ? '已连接' : '未连接'
}
</script>

<template>
  <!-- Mobile: Card View -->
  <div v-if="isMobile" class="card-list" :class="{ 'card-list--loading': loading }">
    <div
      v-for="account in accounts"
      :key="account.id"
      class="conn-card"
      :class="{ 'conn-card--active': selectedId === account.id }"
      @click="emit('select', account)"
    >
      <div class="conn-card__header">
        <div class="conn-card__avatar">
          {{ (account.accountNote || account.unb || '未').charAt(0) }}
        </div>
        <div class="conn-card__info">
          <span class="conn-card__name">{{ account.accountNote || '未命名账号' }}</span>
          <span class="conn-card__unb">UNB: {{ account.unb }}</span>
        </div>
      </div>

      <div class="conn-card__body">
        <div class="conn-card__row">
          <div class="conn-card__status-icon"><IconCookie /></div>
          <span class="conn-card__label">Cookie</span>
          <span
            class="conn-card__badge"
            :style="{
              color: getCookieColor(connections.get(account.id)?.cookieStatus),
              background: getCookieBg(connections.get(account.id)?.cookieStatus)
            }"
          >
            {{ getCookieText(connections.get(account.id)?.cookieStatus) }}
          </span>
        </div>
        <div class="conn-card__row">
          <div class="conn-card__status-icon"><IconWs /></div>
          <span class="conn-card__label">WebSocket</span>
          <span
            class="conn-card__badge"
            :style="{
              color: getWsColor(connections.get(account.id)),
              background: getWsBg(connections.get(account.id))
            }"
          >
            {{ getWsText(connections.get(account.id)) }}
          </span>
        </div>
        <div class="conn-card__row">
          <div class="conn-card__label-icon"><IconClock /></div>
          <span class="conn-card__label">创建</span>
          <span class="conn-card__value">{{ new Date(account.createdTime).toLocaleDateString() }}</span>
        </div>
      </div>

      <div class="conn-card__footer">
        <div class="conn-card__action">
          <span>查看详情</span>
          <IconArrowRight />
        </div>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && accounts.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无账号数据</p>
    </div>
  </div>

  <!-- Desktop/Tablet: Grid Card View -->
  <div v-else class="grid-list" :class="{ 'grid-list--loading': loading }">
    <div
      v-for="account in accounts"
      :key="account.id"
      class="grid-card"
      :class="{ 'grid-card--active': selectedId === account.id }"
      @click="emit('select', account)"
    >
      <div class="grid-card__top">
        <div class="grid-card__avatar">
          {{ (account.accountNote || account.unb || '未').charAt(0) }}
        </div>
      </div>
      <div class="grid-card__name">{{ account.accountNote || '未命名账号' }}</div>
      <div class="grid-card__id">ID: {{ account.id }}</div>
      <div class="grid-card__tags">
        <span
          class="grid-card__tag"
          :style="{
            color: getCookieColor(connections.get(account.id)?.cookieStatus),
            background: getCookieBg(connections.get(account.id)?.cookieStatus)
          }"
        >
          <IconCookie />
          {{ getCookieText(connections.get(account.id)?.cookieStatus) }}
        </span>
        <span
          class="grid-card__tag"
          :style="{
            color: getWsColor(connections.get(account.id)),
            background: getWsBg(connections.get(account.id))
          }"
        >
          <IconWs />
          {{ getWsText(connections.get(account.id)) }}
        </span>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && accounts.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无账号数据</p>
    </div>
  </div>
</template>

<style scoped>
/* ============================================================
   Shared Tokens
   ============================================================ */
.card-list,
.grid-list {
  --c-bg: transparent;
  --c-surface: rgba(255,255,255,0.55);
  --c-surface-hover: rgba(255,255,255,0.72);
  --c-border: rgba(255,255,255,0.75);
  --c-border-strong: rgba(60,60,67,.12);
  --c-text-1: #1c1c1e;
  --c-text-2: rgba(28,28,30,.55);
  --c-text-3: rgba(28,28,30,.55);
  --c-accent: #0A84FF;
  --c-danger: #FF453A;
  --c-success: #30D158;
  --c-warning: #FF9F0A;
  --c-r-sm: 10px;
  --c-r-md: 14px;
  --c-r-lg: 22px;
  --c-ease: 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* ============================================================
   Mobile Card View (iOS 26 Glass Card Style)
   ============================================================ */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  padding-bottom: 24px;
  background: transparent;
  /* 不设置 min-height，让内容自然流动 */
}

.conn-card {
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid rgba(255, 255, 255, 0.5);
  border-radius: 16px;
  padding: 16px;
  transition: all var(--c-ease);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.conn-card--active {
  background: rgba(255, 255, 255, 0.8);
  border-color: rgba(255, 255, 255, 0.6);
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
}

@media (hover: hover) {
  .conn-card:hover {
    background: rgba(255, 255, 255, 0.8);
    box-shadow: 0 12px 40px rgba(0, 0, 0, 0.12);
    border-color: rgba(255, 255, 255, 0.6);
  }
}

.conn-card:active {
  transform: scale(0.98);
}

.conn-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
}

.conn-card__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0A84FF 0%, #0051d5 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
}

.conn-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.conn-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.conn-card__unb {
  font-size: 12px;
  color: var(--c-text-3);
  margin-top: 2px;
  line-height: 1.3;
}

.conn-card__status-icon {
  width: 16px;
  height: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: var(--c-text-3);
}

.conn-card__status-icon svg {
  width: 14px;
  height: 14px;
}

.conn-card__body {
  margin-bottom: 12px;
}

.conn-card__row {
  display: flex;
  align-items: center;
  padding: 5px 0;
  font-size: 13px;
  gap: 4px;
  line-height: 1.4;
}

.conn-card__label-icon {
  width: 14px;
  height: 14px;
  color: var(--c-text-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.conn-card__label-icon svg {
  width: 12px;
  height: 12px;
}

.conn-card__label {
  color: var(--c-text-3);
  flex-shrink: 0;
}

.conn-card__badge {
  margin-left: auto;
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1;
}

.conn-card__value {
  color: var(--c-text-2);
  margin-left: auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.conn-card__footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 12px;
  border-top: 0.5px solid rgba(60,60,67,.12);
}

.conn-card__action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  font-weight: 500;
  color: var(--c-accent);
}

.conn-card__action svg {
  width: 14px;
  height: 14px;
}

/* ============================================================
   Desktop Grid View
   ============================================================ */
.grid-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  gap: 12px;
  padding: 16px;
  padding-bottom: 24px;
  min-height: 100%;
  align-content: start;
}

.grid-card {
  background: var(--c-surface);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid var(--c-border);
  border-radius: var(--c-r-md);
  padding: 16px;
  transition: all var(--c-ease);
  box-shadow: 0 8px 32px rgba(0,0,0,0.10), 0 1.5px 4px rgba(0,0,0,0.06);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  -webkit-tap-highlight-color: transparent;
}

.grid-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.grid-card__tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 11px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 10px;
  line-height: 1;
}

.grid-card__tag svg {
  width: 12px;
  height: 12px;
}

@media (hover: hover) {
  .grid-card:hover {
    box-shadow: 0 12px 40px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.08);
    border-color: rgba(255,255,255,0.85);
  }
}

.grid-card:active {
  transform: scale(0.97);
}

.grid-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.grid-card--active {
  border-color: rgba(10,132,255,0.4);
  box-shadow: 0 0 0 1px rgba(10,132,255,0.4), 0 12px 40px rgba(0,0,0,0.14), 0 2px 6px rgba(0,0,0,0.08);
}

.grid-card__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #0A84FF 0%, #0051d5 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  box-shadow: 0 4px 12px rgba(10,132,255,0.3);
}

.grid-card__name {
  font-size: 14px;
  font-weight: 600;
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 2px;
  line-height: 1.3;
}

.grid-card__id {
  font-size: 12px;
  color: var(--c-text-3);
  margin-bottom: 8px;
  line-height: 1.3;
}

.grid-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
}

.grid-card__cookie-tag {
  font-size: 11px;
  font-weight: 500;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1;
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
  grid-column: 1 / -1;
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
.grid-list--loading {
  opacity: 0.5;
  pointer-events: none;
}

/* ============================================================
   Responsive
   ============================================================ */
@media screen and (max-width: 480px) {
  .card-list {
    padding: 12px;
    gap: 10px;
  }

  .conn-card {
    padding: 14px;
  }

  .conn-card__avatar {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .conn-card__name {
    font-size: 14px;
  }

  .conn-card__unb {
    font-size: 12px;
  }

  .grid-list {
    grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  }

  .grid-card {
    padding: 12px;
  }

  .grid-card__avatar {
    width: 32px;
    height: 32px;
    font-size: 12px;
  }

  .grid-card__name {
    font-size: 13px;
  }

  .grid-card__id {
    font-size: 11px;
  }
}
</style>
