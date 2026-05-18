<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { ChatMessage } from '@/api/message'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconMessage from '@/components/icons/IconMessage.vue'
import IconUser from '@/components/icons/IconUser.vue'
import ContextDialog from './ContextDialog.vue'

interface Props {
  messageList: ChatMessage[]
  loading?: boolean
  xianyuAccountId?: number
  goodsList?: Array<{ item: { xyGoodId: string; title: string }; xyGoodsId?: string; autoDeliveryContent?: string }>
  currentAccountUnb?: string
}

const props = defineProps<Props>()

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

const contextDialogVisible = ref(false)
const currentSid = ref('')
const currentGoodsName = ref('')
const currentSenderUserId = ref('')
const currentXyGoodsId = ref('')

const showContext = (msg: ChatMessage) => {
  currentSid.value = msg.sid
  currentSenderUserId.value = msg.senderUserId
  currentXyGoodsId.value = msg.xyGoodsId || ''
  
  // 查找商品名称
  const goods = props.goodsList?.find(g => (g.xyGoodsId || g.item?.xyGoodId) === msg.xyGoodsId)
  currentGoodsName.value = goods?.item?.title || goods?.item?.xyGoodId || ''
  
  contextDialogVisible.value = true
}

// These will be injected from parent via provide/inject or props
// For simplicity, we define them here and accept as part of the interface
const getContentTypeColor = (contentType: number, isUser: boolean) => {
  if (contentType === 999 || contentType === 997) return '#BF5AF2'
  if (contentType === 888 || contentType === 887) return '#BF5AF2'
  if (!isUser) return '#0A84FF'
  if (contentType === 1) return '#30D158'
  return '#FF9F0A'
}

const getContentTypeBg = (contentType: number, isUser: boolean) => {
  if (contentType === 999 || contentType === 997) return 'rgba(191,90,242,.15)'
  if (contentType === 888 || contentType === 887) return 'rgba(191,90,242,.15)'
  if (!isUser) return 'rgba(10,132,255,.15)'
  if (contentType === 1) return 'rgba(48,209,88,.2)'
  return 'rgba(255,159,10,.18)'
}

const getContentTypeText = (contentType: number, isUser: boolean) => {
  if (contentType === 999) return '手动回复'
  if (contentType === 997) return '图片回复'
  if (contentType === 888) return '自动回复'
  if (contentType === 887) return '自动回复图片'
  if (!isUser) return '我发送的'
  if (contentType === 1) return '用户消息'
  return `系统消息`
}

const formatMessageTime = (timestamp: string | number) => {
  const ts = Number(timestamp)
  if (!ts || isNaN(ts)) return '-'
  const date = new Date(ts)
  if (isNaN(date.getTime())) return '-'
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}
</script>

<template>
  <!-- Mobile: Card View -->
  <div v-if="isMobile" class="card-list" :class="{ 'card-list--loading': loading }">
    <div
      v-for="msg in messageList"
      :key="msg.id"
      class="msg-card"
      :class="{ 'msg-card--user': true, 'msg-card--new': msg.isNew }"
    >
      <div class="msg-card__header">
        <span
          class="msg-card__type"
          :style="{
            color: getContentTypeColor(msg.contentType, msg.senderUserId !== ''),
            background: getContentTypeBg(msg.contentType, msg.senderUserId !== '')
          }"
        >
          {{ getContentTypeText(msg.contentType, true) }}
        </span>
        <span class="msg-card__time">{{ formatMessageTime(msg.messageTime) }}</span>
      </div>

      <div class="msg-card__sender">
        <IconUser />
        <span>{{ msg.senderUserName }}</span>
      </div>

      <div class="msg-card__content">{{ msg.msgContent }}</div>

       <div class="msg-card__footer">
         <span class="msg-card__id">ID: {{ msg.id }}</span>
         <div class="msg-card__actions">
           <button
             class="msg-card__btn msg-card__btn--reply"
             @click="showContext(msg)"
           >
             <IconMessage />
             <span>回复消息</span>
           </button>
         </div>
       </div>
    </div>

    <div v-if="!loading && messageList.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无消息</p>
    </div>
  </div>

  <!-- Desktop: Table View -->
  <div v-else class="table-container" :class="{ 'table-container--loading': loading }">
    <table class="table" v-if="messageList.length > 0">
      <thead class="table__head">
        <tr>
          <th class="table__th table__th--center" style="width:50px">#</th>
          <th class="table__th" style="width:100px">类型</th>
          <th class="table__th" style="width:100px">发送者</th>
          <th class="table__th">消息内容</th>
          <th class="table__th table__th--center" style="width:120px">商品ID</th>
          <th class="table__th table__th--center" style="width:120px">时间</th>
          <th class="table__th table__th--actions" style="width:120px">操作</th>
        </tr>
      </thead>
      <tbody class="table__body">
        <tr v-for="(msg, idx) in messageList" :key="msg.id" class="table__tr" :class="{ 'table__tr--new': msg.isNew }">
          <td class="table__td table__td--center">
            <span class="msg-idx">{{ idx + 1 }}</span>
          </td>
          <td class="table__td">
            <span
              class="type-tag"
              :style="{
                color: getContentTypeColor(msg.contentType, msg.senderUserId !== ''),
                background: getContentTypeBg(msg.contentType, msg.senderUserId !== '')
              }"
            >
              {{ getContentTypeText(msg.contentType, true) }}
            </span>
          </td>
          <td class="table__td">
            <span class="sender-text">{{ msg.senderUserName }}</span>
          </td>
          <td class="table__td">
            <div class="msg-content">{{ msg.msgContent }}</div>
          </td>
          <td class="table__td table__td--center">
            <span class="goods-id-text">{{ msg.xyGoodsId || '-' }}</span>
          </td>
          <td class="table__td table__td--center">
            <span class="time-text">{{ formatMessageTime(msg.messageTime) }}</span>
          </td>
          <td class="table__td table__td--actions">
            <div class="table__actions">
              <button class="table__action table__action--reply" @click="showContext(msg)">
                <IconMessage />
                <span>回复消息</span>
              </button>
            </div>
          </td>
        </tr>
      </tbody>
    </table>

    <div v-if="!loading && messageList.length === 0" class="empty-state">
      <div class="empty-state__icon"><IconEmpty /></div>
      <p class="empty-state__text">暂无消息</p>
    </div>
  </div>

  <ContextDialog
    v-model:visible="contextDialogVisible"
    :sid="currentSid"
    :goods-name="currentGoodsName"
    :xianyu-account-id="xianyuAccountId"
    :sender-user-id="currentSenderUserId"
    :xy-goods-id="currentXyGoodsId"
    :current-account-unb="currentAccountUnb"
  />
</template>

<style scoped>
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

/* Mobile Cards */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 0;
  padding: 0;
  min-height: 100%;
}

.msg-card {
  background: rgba(255,255,255,0.55);
  border-bottom: 0.5px solid var(--c-border-strong);
  transition: background 0.15s ease;
  overflow: hidden;
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
}

.msg-card--new {
  animation: msg-enter 0.35s cubic-bezier(0.25, 0.1, 0.25, 1) both;
}

@keyframes msg-enter {
  from {
    opacity: 0;
    transform: translateY(-8px);
    max-height: 0;
  }
  to {
    opacity: 1;
    transform: translateY(0);
    max-height: 200px;
  }
}

.msg-card:nth-child(even) {
  background: rgba(255,255,255,0.45);
}

.msg-card:active {
  background: rgba(10,132,255,0.06);
}

.msg-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 16px 4px;
}

.msg-card__type {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1.4;
}

.msg-card__time {
  font-size: 11px;
  color: var(--c-text-3);
}

.msg-card__sender {
  display: flex;
  align-items: center;
  gap: 5px;
  padding: 2px 16px 0;
  font-size: 12px;
  font-weight: 500;
  color: var(--c-text-2);
  min-width: 0;
  overflow: hidden;
}

.msg-card__sender span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.msg-card__sender svg {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
}

.msg-card__content {
  padding: 6px 16px 8px;
  font-size: 13px;
  color: var(--c-text-1);
  line-height: 1.55;
  word-break: break-word;
}

.msg-card__footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  padding: 0 12px 10px;
  gap: 6px;
}

.msg-card__id {
  font-size: 11px;
  color: var(--c-text-3);
  font-family: 'SF Mono', 'Menlo', monospace;
  margin-right: auto;
}

.msg-card__actions {
  display: flex;
  gap: 6px;
}

.msg-card__btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 500;
  background: transparent;
  border-radius: 14px;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
}

.msg-card__btn svg {
  width: 12px;
  height: 12px;
}

.msg-card__btn--reply {
  color: var(--c-accent);
  border: 1px solid rgba(0, 122, 255, 0.2);
  background: rgba(0, 122, 255, 0.05);
}

@media (hover: hover) {
  .msg-card__btn--reply:hover {
    background: rgba(0, 122, 255, 0.1);
  }
}

/* Desktop Table — same as AccountTable.vue */
.table-container {
  min-height: 100%;
}

.table {
  width: 100%;
  border-collapse: separate;
  border-spacing: 0;
  font-size: 13px;
}

.table__head {
  position: sticky;
  top: 0;
  z-index: 10;
}

.table__th {
  text-align: left;
  padding: 12px 16px;
  font-size: 12px;
  font-weight: 600;
  color: #1c1c1e;
  letter-spacing: .4px;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border-bottom: 1px solid rgba(60,60,67,.12);
  white-space: nowrap;
  user-select: none;
}

.table__th--center { text-align: center; }
.table__th--actions { width: 120px; text-align: center; }

.table__tr {
  transition: background var(--c-ease);
}

.table__tr--new {
  animation: row-enter 0.35s cubic-bezier(0.25, 0.1, 0.25, 1) both;
}

@keyframes row-enter {
  from {
    opacity: 0;
    transform: translateX(-12px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

.table__tr:not(:last-child) .table__td {
  border-bottom: 1px solid var(--c-border);
}

.table__td {
  padding: 12px 16px;
  color: var(--c-text-1);
  white-space: nowrap;
  background: transparent;
  transition: background var(--c-ease);
  line-height: 1.5;
}

.table__td--center { text-align: center; }
.table__td--actions { text-align: center; }

@media (hover: hover) {
  .table__tr:hover .table__td {
    background: rgba(0, 0, 0, 0.02);
  }
}

.msg-idx {
  font-size: 12px;
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
}

.type-tag {
  display: inline-flex;
  align-items: center;
  font-size: 12px;
  font-weight: 500;
  padding: 3px 10px;
  border-radius: 20px;
  line-height: 1;
}

.sender-text {
  font-size: 13px;
  color: var(--c-text-2);
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  display: inline-block;
}

.msg-content {
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-id-text {
  font-size: 11px;
  color: var(--c-accent);
  font-family: 'SF Mono', 'Menlo', monospace;
}

.time-text {
  font-size: 12px;
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
}

.table__actions {
  display: flex;
  gap: 6px;
  justify-content: center;
}

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

.table__action--reply {
  color: var(--c-accent);
}

@media (hover: hover) {
  .table__action--reply:hover {
    background: rgba(0, 122, 255, 0.08);
  }
}

.table__action:active {
  transform: scale(0.95);
}

/* Empty State */
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
</style>
