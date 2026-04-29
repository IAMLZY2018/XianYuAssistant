<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import type { ChatMessage } from '@/api/message'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconMessage from '@/components/icons/IconMessage.vue'
import IconUser from '@/components/icons/IconUser.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconShoppingBag from '@/components/icons/IconShoppingBag.vue'
import ContextDialog from './ContextDialog.vue'

interface Props {
  messageList: ChatMessage[]
  loading?: boolean
  xianyuAccountId?: number
  goodsList?: Array<{ item: { xyGoodId: string; title: string }; xyGoodsId?: string; autoDeliveryContent?: string }>
  currentAccountUnb?: string
}

interface Emits {
  (e: 'reply', message: ChatMessage): void
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
  if (contentType === 999) return '#5856d6'
  if (contentType === 888) return '#af52de'
  if (!isUser) return '#007aff'
  if (contentType === 1) return '#34c759'
  return '#ff9500'
}

const getContentTypeBg = (contentType: number, isUser: boolean) => {
  if (contentType === 999) return 'rgba(88, 86, 214, 0.1)'
  if (contentType === 888) return 'rgba(175, 82, 222, 0.1)'
  if (!isUser) return 'rgba(0, 122, 255, 0.1)'
  if (contentType === 1) return 'rgba(52, 199, 89, 0.1)'
  return 'rgba(255, 149, 0, 0.1)'
}

const getContentTypeText = (contentType: number, isUser: boolean) => {
  if (contentType === 999) return '手动回复'
  if (contentType === 888) return '自动回复'
  if (!isUser) return '我发送的'
  if (contentType === 1) return '用户消息'
  return `系统消息`
}

const formatMessageTime = (timestamp: number) => {
  if (!timestamp) return '-'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  return date.toLocaleString('zh-CN', {
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
      :class="{ 'msg-card--user': true }"
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
            class="msg-card__btn msg-card__btn--context"
            @click="showContext(msg)"
          >
            <IconMessage />
            <span>上下文</span>
          </button>
          <button
            class="msg-card__btn msg-card__btn--reply"
            @click="emit('reply', msg)"
          >
            <IconSend />
            <span>回复</span>
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
        <tr v-for="(msg, idx) in messageList" :key="msg.id" class="table__tr">
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
              <button class="table__action table__action--context" @click="showContext(msg)">
                <IconMessage />
                <span>上下文</span>
              </button>
              <button class="table__action table__action--reply" @click="emit('reply', msg)">
                <IconSend />
                <span>回复</span>
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
  --c-border: rgba(0, 0, 0, 0.06);
  --c-border-strong: rgba(0, 0, 0, 0.1);
  --c-text-1: #1d1d1f;
  --c-text-2: #6e6e73;
  --c-text-3: #86868b;
  --c-accent: #007aff;
  --c-success: #34c759;
  --c-r-sm: 8px;
  --c-r-md: 12px;
  --c-ease: 0.2s cubic-bezier(0.25, 0.1, 0.25, 1);
}

/* Mobile Cards */
.card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  min-height: 100%;
}

.msg-card {
  background: #fff;
  border: 1px solid var(--c-border-strong);
  border-radius: var(--c-r-md);
  overflow: hidden;
  transition: all var(--c-ease);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.msg-card__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  border-bottom: 1px solid var(--c-border);
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
  gap: 4px;
  padding: 6px 12px 0;
  font-size: 12px;
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
}

.msg-card__content {
  padding: 8px 12px;
  font-size: 13px;
  color: var(--c-text-1);
  line-height: 1.5;
  word-break: break-word;
  background: rgba(0, 0, 0, 0.02);
  margin: 8px 12px;
  border-radius: 6px;
}

.msg-card__footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 12px 8px;
}

.msg-card__id {
  font-size: 11px;
  color: var(--c-text-3);
  font-family: 'SF Mono', 'Menlo', monospace;
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
  padding: 0 10px;
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

.msg-card__btn--context {
  color: #5856d6;
  border: 1px solid rgba(88, 86, 214, 0.2);
}

.msg-card__btn--reply {
  color: var(--c-accent);
  border: 1px solid rgba(0, 122, 255, 0.2);
}

@media (hover: hover) {
  .msg-card__btn--context:hover {
    background: rgba(88, 86, 214, 0.06);
  }
  .msg-card__btn--reply:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

/* Desktop Table */
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
  z-index: 2;
}

.table__th {
  text-align: left;
  padding: 10px 16px;
  font-size: 12px;
  font-weight: 600;
  color: var(--c-text-3);
  background: #fafafa;
  border-bottom: 1px solid var(--c-border-strong);
  white-space: nowrap;
  user-select: none;
}

.table__th--center { text-align: center; }
.table__th--actions { text-align: center; }

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
  background: transparent;
  transition: background var(--c-ease);
  line-height: 1.5;
}

.table__td--center { text-align: center; }
.table__td--actions { text-align: center; }

.msg-idx {
  font-size: 12px;
  color: var(--c-text-3);
  font-variant-numeric: tabular-nums;
}

.type-tag {
  display: inline-flex;
  align-items: center;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1.4;
  white-space: nowrap;
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
  height: 28px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 500;
  background: transparent;
  border-radius: 6px;
  cursor: pointer;
  transition: all var(--c-ease);
  -webkit-tap-highlight-color: transparent;
  white-space: nowrap;
}

.table__action svg {
  width: 12px;
  height: 12px;
}

.table__action--context {
  color: #5856d6;
  border: 1px solid rgba(88, 86, 214, 0.15);
}

.table__action--reply {
  color: var(--c-accent);
  border: 1px solid rgba(0, 122, 255, 0.15);
}

@media (hover: hover) {
  .table__action--context:hover {
    background: rgba(88, 86, 214, 0.06);
  }
  .table__action--reply:hover {
    background: rgba(0, 122, 255, 0.06);
  }
}

.table__action:active {
  transform: scale(0.96);
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

/* Loading */
.card-list--loading,
.table-container--loading {
  opacity: 0.5;
  pointer-events: none;
}
</style>
