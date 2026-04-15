<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { Account } from '@/types'
import { getAccountStatusText } from '@/utils'

import IconWifi from '@/components/icons/IconWifi.vue'
import IconWifiOff from '@/components/icons/IconWifiOff.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconArrowRight from '@/components/icons/IconArrowRight.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconAlert from '@/components/icons/IconAlert.vue'
import IconEmpty from '@/components/icons/IconEmpty.vue'

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

const getConnectionColor = (info?: ConnectionInfo) => {
  if (!info) return 'var(--c-text-3)'
  return info.connected ? 'var(--c-success)' : 'var(--c-danger)'
}

const getConnectionBg = (info?: ConnectionInfo) => {
  if (!info) return 'rgba(134, 134, 139, 0.1)'
  return info.connected ? 'rgba(52, 199, 89, 0.1)' : 'rgba(255, 59, 48, 0.1)'
}

const getConnectionText = (info?: ConnectionInfo) => {
  if (!info) return '未检测'
  return info.connected ? '已连接' : '未连接'
}

const getCookieColor = (status?: number) => {
  if (status === 1) return 'var(--c-success)'
  if (status === 2) return 'var(--c-warning)'
  if (status === 3) return 'var(--c-danger)'
  return 'var(--c-text-3)'
}

const getCookieBg = (status?: number) => {
  if (status === 1) return 'rgba(52, 199, 89, 0.1)'
  if (status === 2) return 'rgba(255, 149, 0, 0.1)'
  if (status === 3) return 'rgba(255, 59, 48, 0.1)'
  return 'rgba(134, 134, 139, 0.1)'
}

const getCookieText = (status?: number) => {
  if (status === 1) return '有效'
  if (status === 2) return '过期'
  if (status === 3) return '失效'
  return '未知'
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
        <span
          class="conn-card__status"
          :style="{
            color: getConnectionColor(connections.get(account.id)),
            background: getConnectionBg(connections.get(account.id))
          }"
        >
          <component :is="connections.get(account.id)?.connected ? IconCheck : IconAlert" />
          {{ getConnectionText(connections.get(account.id)) }}
        </span>
      </div>

      <div class="conn-card__body">
        <div class="conn-card__row">
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
      <div class="grid-card__indicator" :style="{ background: getConnectionColor(connections.get(account.id)) }" />
      <div class="grid-card__top">
        <div class="grid-card__avatar">
          {{ (account.accountNote || account.unb || '未').charAt(0) }}
        </div>
        <span
          class="grid-card__status"
          :style="{
            color: getConnectionColor(connections.get(account.id)),
            background: getConnectionBg(connections.get(account.id))
          }"
        >
          <component :is="connections.get(account.id)?.connected ? IconWifi : IconWifiOff" />
        </span>
      </div>
      <div class="grid-card__name">{{ account.accountNote || '未命名账号' }}</div>
      <div class="grid-card__id">ID: {{ account.id }}</div>
      <div class="grid-card__tags">
        <span
          class="grid-card__cookie-tag"
          :style="{
            color: getCookieColor(connections.get(account.id)?.cookieStatus),
            background: getCookieBg(connections.get(account.id)?.cookieStatus)
          }"
        >
          Cookie: {{ getCookieText(connections.get(account.id)?.cookieStatus) }}
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
  --c-surface: #ffffff;
  --c-border: rgba(0, 0, 0, 0.06);
  --c-border-strong: rgba(0, 0, 0, 0.1);
  --c-text-1: #1d1d1f;
  --c-text-2: #6e6e73;
  --c-text-3: #86868b;
  --c-accent: #007aff;
  --c-danger: #ff3b30;
  --c-success: #34c759;
  --c-warning: #ff9500;
  --c-r-sm: 8px;
  --c-r-md: 12px;
  --c-r-lg: 16px;
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

.conn-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-r-md);
  padding: 16px;
  transition: all var(--c-ease);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  -webkit-tap-highlight-color: transparent;
}

.conn-card--active {
  border-color: var(--c-accent);
  box-shadow: 0 0 0 1px var(--c-accent), 0 2px 8px rgba(0, 122, 255, 0.12);
}

@media (hover: hover) {
  .conn-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    border-color: rgba(0, 0, 0, 0.15);
  }
}

.conn-card:active {
  transform: scale(0.985);
}

.conn-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}

.conn-card__avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  color: var(--c-text-1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  font-weight: 600;
  flex-shrink: 0;
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

.conn-card__status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  padding: 4px 10px;
  border-radius: 20px;
  line-height: 1;
}

.conn-card__status svg {
  width: 12px;
  height: 12px;
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
  border-top: 1px solid var(--c-border);
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
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-r-md);
  padding: 16px;
  transition: all var(--c-ease);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  -webkit-tap-highlight-color: transparent;
}

.grid-card__indicator {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  border-radius: 3px 3px 0 0;
}

.grid-card--active {
  border-color: var(--c-accent);
  box-shadow: 0 0 0 1px var(--c-accent), 0 4px 12px rgba(0, 122, 255, 0.12);
}

@media (hover: hover) {
  .grid-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    border-color: rgba(0, 0, 0, 0.15);
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

.grid-card__avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.05);
  color: var(--c-text-1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.grid-card__status {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.grid-card__status svg {
  width: 14px;
  height: 14px;
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
@media screen and (max-width: 768px) {
  .grid-list {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}

@media screen and (max-width: 480px) {
  .card-list {
    padding: 12px;
    gap: 8px;
  }

  .conn-card {
    padding: 14px;
  }

  .conn-card__avatar {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }

  .conn-card__name {
    font-size: 14px;
  }
}
</style>
