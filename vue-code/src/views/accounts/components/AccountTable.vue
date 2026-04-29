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

.account-card {
  background: var(--c-surface);
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-r-md);
  padding: 16px;
  transition: all var(--c-ease);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

@media (hover: hover) {
  .account-card:hover {
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
    border-color: rgba(0, 0, 0, 0.15);
  }
}

.account-card__header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid var(--c-border);
}

.account-card__avatar {
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

.account-card__info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.account-card__name {
  font-size: 15px;
  font-weight: 600;
  color: var(--c-text-1);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.3;
}

.account-card__unb {
  font-size: 12px;
  color: var(--c-text-3);
  margin-top: 2px;
  line-height: 1.3;
}

.account-card__status {
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

.account-card__status svg {
  width: 12px;
  height: 12px;
}

.account-card__body {
  margin-bottom: 12px;
}

.account-card__row {
  display: flex;
  align-items: center;
  padding: 5px 0;
  font-size: 13px;
  gap: 4px;
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
}

.account-card__value {
  color: var(--c-text-2);
  margin-left: auto;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  text-align: right;
}

.account-card__footer {
  display: flex;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px solid var(--c-border);
}

.account-card__btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  flex: 1;
  height: 36px;
  font-size: 13px;
  font-weight: 500;
  border-radius: var(--c-r-sm);
  border: 1px solid;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
  background: transparent;
}

.account-card__btn svg {
  width: 15px;
  height: 15px;
}

.account-card__btn--edit {
  color: var(--c-accent);
  border-color: rgba(0, 122, 255, 0.2);
}

@media (hover: hover) {
  .account-card__btn--edit:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

.account-card__btn--delete {
  color: var(--c-danger);
  border-color: rgba(255, 59, 48, 0.2);
}

@media (hover: hover) {
  .account-card__btn--delete:hover {
    background: rgba(255, 59, 48, 0.06);
  }
}

.account-card__btn:active {
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
    gap: 8px;
  }

  .account-card {
    padding: 14px;
  }

  .account-card__avatar {
    width: 36px;
    height: 36px;
    font-size: 14px;
  }

  .account-card__name {
    font-size: 14px;
  }

  .account-card__btn {
    height: 34px;
    font-size: 12px;
  }
}
</style>
