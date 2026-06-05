<script setup lang="ts">
import { ref, watch, nextTick, computed, onMounted, onUnmounted } from 'vue'
import { getContextMessages } from '@/api/message'
import type { ChatMessage } from '@/api/message'
import { sendMessage as sendMessageApi } from '@/api/message'
import { sendImageMessage as sendImageMessageApi } from '@/api/image'
import { toast } from '@/utils/toast'
import IconUser from '@/components/icons/IconUser.vue'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconImage from '@/components/icons/IconImage.vue'
import IconClose from '@/components/icons/IconClose.vue'
import MultiImageUploader from '@/components/MultiImageUploader.vue'

interface Props {
  visible: boolean
  sid: string
  goodsName?: string
  xianyuAccountId?: number
  senderUserId?: string
  xyGoodsId?: string
  currentAccountUnb?: string
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

const loading = ref(false)
const messages = ref<ChatMessage[]>([])
const inputText = ref('')
const sending = ref(false)
const inputImageUrls = ref('')
const showImageUploader = ref(false)
const messageListRef = ref<HTMLElement | null>(null)
const hasMore = ref(true)
const loadingMore = ref(false)

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
  stopRefresh()
})

const totalCount = computed(() => messages.value.length)

const handleClose = () => {
  emit('update:visible', false)
}

const scrollToBottom = () => {
  nextTick(() => {
    if (messageListRef.value) {
      messageListRef.value.scrollTop = messageListRef.value.scrollHeight
    }
  })
}

const loadContext = async (append = false) => {
  if (!props.sid) return
  
  if (append) {
    loadingMore.value = true
  } else {
    loading.value = true
    messages.value = []
    hasMore.value = true
  }
  
  try {
    const limit = 20
    const offset = append ? messages.value.length : 0
    
    const res = await getContextMessages({ sid: props.sid, limit, offset })
    const msgList = res?.data || []
    const newMessages = Array.isArray(msgList) ? msgList : []
    
    if (append) {
      messages.value = [...newMessages.reverse(), ...messages.value]
    } else {
      messages.value = newMessages.reverse()
    }
    
    hasMore.value = newMessages.length >= limit
    
    if (!append) {
      scrollToBottom()
    }
  } catch (error) {
    console.error('加载上下文失败:', error)
    if (!append) {
      messages.value = []
    }
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

const handleScroll = () => {
  if (!messageListRef.value || loadingMore.value || !hasMore.value) return
  
  const { scrollTop } = messageListRef.value
  if (scrollTop < 50) {
    loadMore()
  }
}

const loadMore = async () => {
  if (loadingMore.value || !hasMore.value) return
  
  if (messageListRef.value) {
    const oldScrollHeight = messageListRef.value.scrollHeight
    await loadContext(true)
    nextTick(() => {
      if (messageListRef.value) {
        const newScrollHeight = messageListRef.value.scrollHeight
        messageListRef.value.scrollTop = newScrollHeight - oldScrollHeight
      }
    })
  }
}

let refreshTimer: ReturnType<typeof setInterval> | null = null

const startRefresh = () => {
  stopRefresh()
  refreshTimer = setInterval(() => {
    if (props.visible && props.sid) {
      refreshMessages()
    }
  }, 1000)
}

const stopRefresh = () => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

const refreshMessages = async () => {
  if (!props.sid) return
  try {
    const res = await getContextMessages({ sid: props.sid, limit: 20, offset: 0 })
    const msgList = res?.data || []
    const newMessages = Array.isArray(msgList) ? msgList.reverse() : []
    if (newMessages.length !== messages.value.length || JSON.stringify(newMessages) !== JSON.stringify(messages.value)) {
      messages.value = newMessages
      scrollToBottom()
    }
  } catch {
    // 静默失败，不影响体验
  }
}

watch(() => props.visible, (newVal) => {
  if (newVal && props.sid) {
    loadContext()
    startRefresh()
  } else {
    stopRefresh()
  }
})

const formatTime = (timestamp: string | number) => {
  const ts = Number(timestamp)
  if (!ts || isNaN(ts)) return ''
  const date = new Date(ts)
  if (isNaN(date.getTime())) return ''
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

const isUserMessage = (msg: ChatMessage) => {
  return msg.contentType === 1 && msg.senderUserId !== props.currentAccountUnb
}

const isMyMessage = (msg: ChatMessage) => {
  if (msg.contentType === 999 || msg.contentType === 997 || msg.contentType === 888 || msg.contentType === 887) {
    return true
  }
  return msg.contentType === 1 && msg.senderUserId === props.currentAccountUnb
}

const isSystemMessage = (msg: ChatMessage) => {
  return !isUserMessage(msg) && !isMyMessage(msg)
}

const getMessageType = (msg: ChatMessage) => {
  if (msg.contentType === 999) return '手动回复'
  if (msg.contentType === 997) return '图片回复'
  if (msg.contentType === 888) return 'AI回复'
  if (msg.contentType === 887) return '自动回复图片'
  return null
}

const handleSend = async () => {
  const hasImages = inputImageUrls.value.trim() && inputImageUrls.value.split(',').some((s: string) => s.trim())
  if (!inputText.value.trim() && !hasImages) {
    toast.warning('请输入消息内容或上传图片')
    return
  }
  
  if (!props.xianyuAccountId || !props.senderUserId) {
    toast.error('缺少必要参数，无法发送')
    return
  }
  
  sending.value = true
  try {
    const cid = props.sid.replace('@goofish', '')
    const toId = props.senderUserId.replace('@goofish', '')
    
    if (hasImages) {
      const urls = inputImageUrls.value.split(',').map((s: string) => s.trim()).filter((s: string) => s)
      for (const url of urls) {
        await sendImageMessageApi({
          xianyuAccountId: props.xianyuAccountId,
          cid,
          toId,
          imageUrl: url,
          width: 800,
          height: 800,
          xyGoodsId: props.xyGoodsId
        })
      }
    }
    
    if (inputText.value.trim()) {
      await sendMessageApi({
        xianyuAccountId: props.xianyuAccountId,
        cid,
        toId,
        text: inputText.value.trim(),
        xyGoodsId: props.xyGoodsId
      })
    }
    
    toast.success('发送成功')
    inputText.value = ''
    inputImageUrls.value = ''
    showImageUploader.value = false
    await loadContext()
  } catch (error: any) {
    toast.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="visible" class="modal-overlay" @click.self="handleClose">
        <div class="modal-container" :class="{ 'is-mobile': isMobile }">
          <!-- Header -->
          <div class="modal-header">
            <div class="modal-header-left">
              <span class="modal-title">上下文消息</span>
              <span v-if="goodsName" class="modal-goods">{{ goodsName }}</span>
            </div>
            <div class="modal-header-right">
              <span class="modal-count">{{ totalCount }}条</span>
              <button class="modal-close" @click="handleClose">
                <IconClose />
              </button>
            </div>
          </div>

          <!-- Body -->
          <div class="modal-body">
            <div class="context-content" ref="messageListRef" @scroll="handleScroll">
              <div v-if="loading" class="loading-wrap">
                <div class="loading-spinner"></div>
                <span>加载中...</span>
              </div>
              
              <template v-else>
                <div v-if="loadingMore" class="loading-more">加载更多...</div>
                
                <div v-if="messages.length > 0" class="message-list">
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    class="message-item"
                    :class="{
                      'message-item--user': isUserMessage(msg),
                      'message-item--mine': isMyMessage(msg),
                      'message-item--system': isSystemMessage(msg)
                    }"
                  >
                    <template v-if="isSystemMessage(msg)">
                      <div class="system-text">{{ msg.msgContent.replace(/^\[|\]$/g, '') }}</div>
                    </template>
                    
                    <template v-else>
                      <div class="message-avatar">
                        <div class="avatar" :class="{ 'avatar--user': isUserMessage(msg) }">
                          <IconUser />
                        </div>
                      </div>
                      
                      <div class="message-body">
                        <div class="message-header">
                          <span class="message-sender">{{ isUserMessage(msg) ? msg.senderUserName : '我' }}</span>
                          <span v-if="getMessageType(msg)" class="message-type">{{ getMessageType(msg) }}</span>
                          <span class="message-time">{{ formatTime(msg.messageTime) }}</span>
                        </div>
                        <div class="message-text">{{ msg.msgContent }}</div>
                      </div>
                    </template>
                  </div>
                </div>
                
                <div v-else class="empty-context">
                  <div class="empty-icon"><IconEmpty /></div>
                  <p class="empty-text">暂无上下文消息</p>
                </div>
              </template>
            </div>
          </div>

          <!-- Footer -->
          <div class="modal-footer">
            <div v-if="showImageUploader && xianyuAccountId" class="input-uploader-row">
              <MultiImageUploader
                :account-id="xianyuAccountId"
                v-model="inputImageUrls"
                class="input-uploader"
              />
            </div>
            <div class="input-row">
              <textarea
                v-model="inputText"
                class="input-textarea"
                :rows="1"
                placeholder="输入消息..."
                @keydown.enter.ctrl="handleSend"
              ></textarea>
              <div class="input-btns">
                <button
                  class="img-btn"
                  :class="{ 'img-btn--active': showImageUploader || inputImageUrls }"
                  @click="showImageUploader = !showImageUploader"
                >
                  <IconImage />
                </button>
                <button class="send-btn" :class="{ 'is-loading': sending }" :disabled="sending || (!inputText.trim() && !inputImageUrls)" @click="handleSend">
                  <IconSend />
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.20);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.modal-container {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(40px) saturate(2);
  -webkit-backdrop-filter: blur(40px) saturate(2);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 20px;
  width: 100%;
  max-width: 720px;
  height: 80vh;
  max-height: 700px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.16), 0 2px 8px rgba(0,0,0,0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container.is-mobile {
  max-width: 94vw;
  height: 88vh;
  border-radius: 20px;
}

/* Header */
.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  flex-shrink: 0;
}

.modal-header-left {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
}

.modal-goods {
  font-size: 12px;
  color: rgba(28,28,30,.55);
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.modal-header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.modal-count {
  font-size: 12px;
  color: #FF453A;
  font-weight: 500;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: rgba(28,28,30,.55);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: rgba(0, 0, 0, 0.06);
  color: #1c1c1e;
}

.modal-close svg {
  width: 12px;
  height: 12px;
}

/* Body */
.modal-body {
  flex: 1;
  padding: 0 20px;
  overflow: hidden;
}

.context-content {
  height: 100%;
  overflow-y: auto;
  padding: 14px;
  background: rgba(255,255,255,0.38);
  border-radius: 12px;
}

.context-content::-webkit-scrollbar {
  width: 5px;
}

.context-content::-webkit-scrollbar-track {
  background: transparent;
}

.context-content::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}

.loading-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: 100%;
  color: rgba(28,28,30,.55);
  font-size: 13px;
}

.loading-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid rgba(60,60,67,.12);
  border-top-color: #0A84FF;
  border-radius: 50%;
  animation: spin 0.7s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-more {
  text-align: center;
  padding: 8px;
  color: rgba(28,28,30,.55);
  font-size: 12px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.message-item {
  display: flex;
  gap: 10px;
}

.message-item--user {
  justify-content: flex-start;
}

.message-item--mine {
  flex-direction: row-reverse;
  justify-content: flex-start;
}

.message-item--system {
  justify-content: center;
  padding: 4px 0;
}

.message-avatar {
  flex-shrink: 0;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #30D158;
  color: #fff;
}

.avatar svg {
  width: 16px;
  height: 16px;
}

.avatar--user {
  background: #0A84FF;
}

.message-body {
  max-width: 65%;
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
}

.message-sender {
  font-weight: 600;
  color: #1c1c1e;
}

.message-type {
  padding: 2px 7px;
  border-radius: 10px;
  background: rgba(191,90,242,.15);
  color: #BF5AF2;
  font-size: 10px;
  font-weight: 500;
}

.message-time {
  color: rgba(28,28,30,.55);
}

.message-text {
  padding: 9px 12px;
  background: rgba(255,255,255,0.72);
  border-radius: 16px;
  font-size: 13px;
  line-height: 1.5;
  color: #1c1c1e;
  word-break: break-word;
}

.message-item--mine .message-text {
  background: rgba(48,209,88,.15);
  color: #1c1c1e;
}

.system-text {
  font-size: 11px;
  color: rgba(28,28,30,.55);
  text-align: center;
  padding: 4px 10px;
  background: rgba(255,255,255,0.38);
  border-radius: 10px;
}

.empty-context {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 48px 16px;
  gap: 10px;
  height: 100%;
}

.empty-icon {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: rgba(28,28,30,.55);
  opacity: 0.3;
}

.empty-icon svg {
  width: 32px;
  height: 32px;
}

.empty-text {
  font-size: 13px;
  color: rgba(28,28,30,.55);
}

/* Footer */
.modal-footer {
  display: flex;
  flex-direction: column;
  padding: 10px 16px;
  flex-shrink: 0;
  gap: 8px;
  border-top: 0.5px solid rgba(255,255,255,0.35);
}

.input-uploader-row {
  width: 100%;
}

.input-uploader {
  width: 100%;
}

.input-row {
  display: flex;
  align-items: stretch;
  gap: 8px;
}

.input-textarea {
  flex: 1;
  min-height: 56px;
  max-height: 100px;
  padding: 8px 12px;
  border: 1px solid rgba(255,255,255,0.35);
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  resize: none;
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  color: #1c1c1e;
  transition: border-color 0.15s ease;
  font-family: inherit;
  overflow-y: auto;
  scrollbar-width: none;
}

.input-textarea::-webkit-scrollbar {
  display: none;
}

.input-textarea:focus {
  outline: none;
  border-color: rgba(10,132,255,0.5);
}

.input-textarea::placeholder {
  color: rgba(28,28,30,.45);
}

.input-btns {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
  width: 36px;
}

.img-btn {
  width: 100%;
  flex: 1;
  border-radius: 8px;
  border: none;
  background: rgba(255,255,255,0.38);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
  color: rgba(28,28,30,.55);
}

.img-btn:hover {
  background: rgba(255,255,255,0.55);
  color: #0A84FF;
}

.img-btn--active {
  color: #0A84FF;
  background: rgba(10,132,255,0.1);
}

.img-btn svg {
  width: 18px;
  height: 18px;
}

.send-btn {
  width: 100%;
  flex: 1;
  border-radius: 8px;
  border: none;
  background: rgba(10,132,255,0.85);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.send-btn:hover:not(:disabled) {
  background: rgba(10,132,255,0.95);
}

.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
  background: rgba(120,120,128,.24);
}

.send-btn svg {
  width: 16px;
  height: 16px;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.32, 0.94, 0.6, 1), opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.92) translateY(8px);
  opacity: 0;
}

/* Mobile */
@media (max-width: 480px) {
  .modal-container {
    max-width: 94vw;
    height: 88vh;
  }
  
  .modal-header {
    padding: 12px 16px;
  }
  
  .modal-title {
    font-size: 14px;
  }
  
  .modal-body {
    padding: 0 16px;
  }
  
  .context-content {
    padding: 10px;
  }
  
  .message-list {
    gap: 12px;
  }
  
  .message-body {
    max-width: 80%;
  }
  
  .avatar {
    width: 28px;
    height: 28px;
  }
  
  .avatar svg {
    width: 14px;
    height: 14px;
  }
  
  .modal-footer {
    padding: 12px 16px;
    gap: 8px;
  }
  
  .input-textarea {
    padding: 8px 10px;
    font-size: 14px;
  }

  .input-btns {
    width: 32px;
  }
}
</style>
