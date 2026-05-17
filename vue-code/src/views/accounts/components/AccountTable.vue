<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getAccountStatusText, formatTime } from '@/utils'
import type { Account } from '@/types'

import IconEdit from '@/components/icons/IconEdit.vue'
import IconTrash from '@/components/icons/IconTrash.vue'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconCheck from '@/components/icons/IconCheck.vue'
import IconAlert from '@/components/icons/IconAlert.vue'

interface Props {
  accounts: Account[]
  loading?: boolean
}

interface Emits {
  (e: 'edit', account: Account): void
  (e: 'delete', id: number): void
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
  const info = getAccountStatusText(status)
  switch (info.type) {
    case 'success': return '#34c759'
    case 'warning': return '#ff9500'
    case 'danger': return '#ff3b30'
    default: return '#007aff'
  }
}

const getStatusBg = (status: number) => {
  const info = getAccountStatusText(status)
  switch (info.type) {
    case 'success': return 'rgba(52, 199, 89, 0.1)'
    case 'warning': return 'rgba(255, 149, 0, 0.1)'
    case 'danger': return 'rgba(255, 59, 48, 0.1)'
    default: return 'rgba(0, 122, 255, 0.1)'
  }
}
</script>

<template>
  <!-- Mobile: Card View -->
  <div v-if="isMobile" class="card-list" :class="{ 'card-list--loading': loading }">
    <div
      v-for="account in accounts"
      :key="account.id"
      class="account-card"
    >
      <div class="account-card__header">
        <div class="account-card__avatar">
          {{ (account.accountNote || account.unb || '未').charAt(0) }}
        </div>
        <div class="account-card__info">
          <span class="account-card__name">{{ account.accountNote || '未命名账号' }}</span>
          <span class="account-card__unb">UNB: {{ account.unb }}</span>
        </div>
        <span
          class="account-card__status"
          :style="{
            color: getStatusColor(account.status),
            background: getStatusBg(account.status)
          }"
        >
          <component :is="getAccountStatusText(account.status).type === 'success' ? IconCheck : IconAlert" />
          {{ getAccountStatusText(account.status).text }}
        </span>
      </div>

      <div class="account-card__body">
        <div class="account-card__row">
          <span class="account-card__label">ID</span>
          <span class="account-card__value">{{ account.id }}</span>
        </div>
        <div class="account-card__row">
          <div class="account-card__label-icon"><IconClock /></div>
          <span class="account-card__label">创建</span>
          <span class="account-card__value">{{ formatTime(account.createdTime) }}</span>
        </div>
        <div class="account-card__row">
          <div class="account-card__label-icon"><IconClock /></div>
          <span class="account-card__label">更新</span>
          <span class="account-card__value">{{ formatTime(account.updatedTime) }}</span>
        </div>
      </div>

      <div class="account-card__footer">
        <button class="account-card__btn account-card__btn--edit" @click="emit('edit', account)">
          <IconEdit />
          <span>编辑</span>
        </button>
        <button class="account-card__btn account-card__btn--delete" @click="emit('delete', account.id)">
          <IconTrash />
          <span>删除</span>
        </button>
      </div>
    </div>

    <!-- Empty State -->
    <div v-if="!loading && accounts.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无账号数据</p>
    </div>
  </div>

  <!-- Desktop/Tablet: Table View -->
  <div v-else class="table-container" :class="{ 'table-container--loading': loading }">
    <table class="table" v-if="accounts.length > 0">
      <thead class="table__head">
        <tr>
          <th class="table__th table__th--id">ID</th>
          <th class="table__th">UNB</th>
          <th class="table__th">账号备注</th>
          <th class="table__th table__th--status">状态</th>
          <th class="table__th table__th--time">创建时间</th>
          <th class="table__th table__th--time">更新时间</th>
          <th class="table__th table__th--actions">操作</th>
        </tr>
      </thead>
      <tbody class="table__body">
        <tr v-for="account in accounts" :key="account.id" class="table__tr">
          <td class="table__td table__td--id">{{ account.id }}</td>
          <td class="table__td">{{ account.unb }}</td>
          <td class="table__td">{{ account.accountNote || '未命名账号' }}</td>
          <td class="table__td table__td--status">
            <span
              class="status-tag"
              :style="{
                color: getStatusColor(account.status),
                background: getStatusBg(account.status)
              }"
            >
              {{ getAccountStatusText(account.status).text }}
            </span>
          </td>
          <td class="table__td table__td--time">{{ formatTime(account.createdTime) }}</td>
          <td class="table__td table__td--time">{{ formatTime(account.updatedTime) }}</td>
          <td class="table__td table__td--actions">
            <button class="table__action table__action--edit" @click="emit('edit', account)">
              <IconEdit />
              <span>编辑</span>
            </button>
            <button class="table__action table__action--delete" @click="emit('delete', account.id)">
              <IconTrash />
              <span>删除</span>
            </button>
          </td>
        </tr>
      </tbody>
    </table>

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
.table-container {
  --c-bg: transparent;
  --c-surface: rgba(255,255,255,0.55);
  --c-border: rgba(60,60,67,.12);
  --c-border-strong: rgba(60,60,67,.12);
  --c-text-1: #1c1c1e;
  --c-text-2: rgba(28,28,30,.55);
  --c-text-3: rgba(28,28,30,.55);
  --c-accent: #0A84FF;
  --c-danger: #FF453A;
  --c-success: #30D158;
  --c-r-sm: 10px;
  --c-r-md: 14px;
  --c-ease: 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* ============================================================
   Mobile List View (iOS 26 Glass Card Style)
   ============================================================ */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  padding-bottom: 24px;
  min-height: 100%;
  background: transparent;
}

.account-card {
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 22px;
  padding: 16px;
  transition: all var(--c-ease);
  box-shadow: 0 8px 32px rgba(0,0,0,0.10), 0 1.5px 4px rgba(0,0,0,0.06);
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 12px;
  overflow: hidden;
}

.account-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.9) 30%, rgba(255,255,255,0.9) 70%, transparent);
  border-radius: 1px;
  pointer-events: none;
}

@media (hover: hover) {
  .account-card:hover {
    background: rgba(255,255,255,0.72);
    box-shadow: 0 16px 48px rgba(0,0,0,0.16), 0 2px 8px rgba(0,0,0,0.08);
    border-color: rgba(255,255,255,0.85);
  }
}

.account-card:active {
  transform: scale(0.98);
}

.account-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 0;
  padding-bottom: 12px;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
}

.account-card__avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  background: rgba(10,132,255,0.85);
  border: 1px solid rgba(255,255,255,0.35);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 600;
  flex-shrink: 0;
  box-shadow: 0 4px 16px rgba(10,132,255,0.35);
}

.account-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.account-card__name {
  font-size: 16px;
  font-weight: 600;
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.2;
}

.account-card__unb {
  font-size: 13px;
  color: var(--c-text-3);
  line-height: 1.2;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-card__status {
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 500;
  padding: 6px 12px;
  border-radius: 20px;
  line-height: 1;
  background: rgba(0, 122, 255, 0.15);
  color: var(--c-accent);
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
}

.account-card__status svg {
  width: 13px;
  height: 13px;
}

.account-card__body {
  margin-bottom: 0;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px 16px;
  padding: 12px 0;
  border-top: 0.5px solid rgba(60,60,67,.12);
  border-bottom: 0.5px solid rgba(60,60,67,.12);
}

.account-card__row {
  display: flex;
  flex-direction: column;
  gap: 4px;
  font-size: 13px;
  line-height: 1.4;
}

.account-card__label-icon {
  width: 14px;
  height: 14px;
  color: var(--c-text-3);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.account-card__label-icon svg {
  width: 12px;
  height: 12px;
}

.account-card__label {
  color: var(--c-text-3);
  flex-shrink: 0;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.account-card__value {
  color: var(--c-text-1);
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
}

.account-card__footer {
  display: flex;
  gap: 10px;
  padding-top: 0;
  border-top: none;
}

.account-card__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex: 1;
  height: 40px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 10px;
  border: none;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.account-card__btn svg {
  width: 16px;
  height: 16px;
}

.account-card__btn--edit {
  color: white;
  background: rgba(10,132,255,0.85);
  border: 1px solid rgba(255,255,255,0.35);
  box-shadow: 0 4px 16px rgba(10,132,255,0.35);
}

.account-card__btn--edit:active {
  background: rgba(10,132,255,0.95);
  transform: scale(0.97);
}

.account-card__btn--delete {
  color: #FF453A;
  background: rgba(255,69,58,.15);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border: 1px solid rgba(255,69,58,.25);
  box-shadow: none;
}

.account-card__btn--delete:active {
  background: rgba(255,69,58,.25);
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
  background: transparent;
  border-bottom: 0.5px solid var(--c-border);
  white-space: nowrap;
  user-select: none;
}

.table__th--id { width: 64px; }
.table__th--status { width: 96px; }
.table__th--time { width: 168px; }
.table__th--actions { width: 140px; text-align: center; }

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
  padding: 12px 16px;
  color: var(--c-text-1);
  white-space: nowrap;
  background: transparent;
  transition: background var(--c-ease);
  line-height: 1.5;
}

.table__td--id {
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
  font-size: 12px;
}

.table__td--time {
  color: var(--c-text-2);
  font-size: 12px;
  font-variant-numeric: tabular-nums;
}

.table__td--actions {
  text-align: center;
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

/* Action Buttons in Table */
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
  border: none;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.table__action svg {
  width: 14px;
  height: 14px;
}

.table__action--edit {
  color: var(--c-accent);
}

@media (hover: hover) {
  .table__action--edit:hover {
    background: rgba(0, 122, 255, 0.08);
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
    gap: 10px;
  }

  .account-card {
    padding: 14px;
  }

  .account-card__avatar {
    width: 40px;
    height: 40px;
    font-size: 16px;
  }

  .account-card__name {
    font-size: 15px;
  }

  .account-card__unb {
    font-size: 12px;
  }

  .account-card__body {
    gap: 10px 12px;
    padding: 10px 0;
  }

  .account-card__row {
    font-size: 12px;
  }

  .account-card__value {
    font-size: 13px;
  }

  .account-card__btn {
    height: 38px;
    font-size: 13px;
  }

  .empty-state {
    padding: 40px 16px;
  }
}
</style>
