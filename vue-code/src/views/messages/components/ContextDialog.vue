<script setup lang="ts">
import { ref, watch, nextTick, computed, onMounted, onUnmounted } from 'vue'
import { getContextMessages } from '@/api/message'
import type { ChatMessage } from '@/api/message'
import { sendMessage as sendMessageApi } from '@/api/message'
import { sendImageMessage as sendImageMessageApi } from '@/api/image'
import { ElMessage } from 'element-plus'
import IconUser from '@/components/icons/IconUser.vue'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconSend from '@/components/icons/IconSend.vue'
import IconImage from '@/components/icons/IconImage.vue'
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

watch(() => props.visible, (newVal) => {
  if (newVal && props.sid) {
    loadContext()
  }
})

const formatTime = (timestamp: number) => {
  if (!timestamp) return ''
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
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
  if (msg.contentType === 999 || msg.contentType === 888) {
    return true
  }
  return msg.contentType === 1 && msg.senderUserId === props.currentAccountUnb
}

const isSystemMessage = (msg: ChatMessage) => {
  return !isUserMessage(msg) && !isMyMessage(msg)
}

const getMessageType = (msg: ChatMessage) => {
  if (msg.contentType === 999) return '手动回复'
  if (msg.contentType === 888) return 'AI回复'
  return null
}

const handleSend = async () => {
  const hasImages = inputImageUrls.value.trim() && inputImageUrls.value.split(',').some((s: string) => s.trim())
  if (!inputText.value.trim() && !hasImages) {
    ElMessage.warning('请输入消息内容或上传图片')
    return
  }
  
  if (!props.xianyuAccountId || !props.senderUserId) {
    ElMessage.error('缺少必要参数，无法发送')
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
    
    ElMessage.success('发送成功')
    inputText.value = ''
    inputImageUrls.value = ''
    showImageUploader.value = false
    await loadContext()
  } catch (error: any) {
    ElMessage.error(error.message || '发送失败')
  } finally {
    sending.value = false
  }
}
</script>

<template>
  <el-dialog
    :model-value="visible"
    @update:model-value="emit('update:visible', $event)"
    :width="isMobile ? '95%' : '800px'"
    :top="isMobile ? '3vh' : '5vh'"
    :close-on-click-modal="false"
    append-to-body
    destroy-on-close
    custom-class="context-dialog"
  >
    <template #header>
      <div class="dialog-header">
        <div class="dialog-header__left">
          <span class="dialog-title">上下文消息</span>
          <span v-if="goodsName" class="dialog-goods">{{ goodsName }}</span>
        </div>
        <span class="dialog-count">{{ totalCount }}条消息</span>
      </div>
    </template>
    
    <div class="context-wrapper">
      <div class="context-content" v-loading="loading" ref="messageListRef" @scroll="handleScroll">
        <div v-if="loadingMore" class="loading-more">加载中...</div>
        
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
        
        <div v-else-if="!loading" class="empty-context">
          <div class="empty-context__icon"><IconEmpty /></div>
          <p class="empty-context__text">暂无上下文消息</p>
        </div>
      </div>
      
      <div class="context-input">
        <div class="context-input__left">
          <MultiImageUploader
            v-if="showImageUploader && xianyuAccountId"
            :account-id="xianyuAccountId"
            v-model="inputImageUrls"
            class="context-input__uploader"
          />
          <el-input
            v-model="inputText"
            type="textarea"
            :rows="2"
            placeholder="输入消息内容，Ctrl+Enter发送"
            @keydown.enter.ctrl="handleSend"
          />
        </div>
        <div class="context-input__actions">
          <button
            class="context-input__img-btn"
            :class="{ 'context-input__img-btn--active': showImageUploader || inputImageUrls }"
            @click="showImageUploader = !showImageUploader"
          >
            <IconImage />
          </button>
          <el-button
            type="primary"
            :loading="sending"
            @click="handleSend"
          >
            <IconSend />
            <span>发送</span>
          </el-button>
        </div>
      </div>
    </div>
  </el-dialog>
</template>

<style scoped>
.dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding-right: 40px;
}

.dialog-header__left {
  display: flex;
  align-items: baseline;
  gap: 12px;
}

.dialog-title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.dialog-goods {
  font-size: 13px;
  color: #666;
  max-width: 300px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dialog-count {
  font-size: 13px;
  color: #ff3b30;
  font-weight: 500;
}

.context-wrapper {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 65vh;
}

.context-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  background: #f5f5f7;
  border-radius: 12px;
  border: 1px solid #e5e5e5;
}

.loading-more {
  text-align: center;
  padding: 8px;
  color: #666;
  font-size: 13px;
}

.message-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
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
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #34c759;
  color: #fff;
}

.avatar svg {
  width: 18px;
  height: 18px;
}

.avatar--user {
  background: #007aff;
}

.message-body {
  max-width: 65%;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.message-sender {
  font-weight: 600;
  color: #1a1a1a;
}

.message-type {
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(88, 86, 214, 0.1);
  color: #5856d6;
  font-size: 11px;
  font-weight: 500;
}

.message-time {
  color: #999;
}

.message-text {
  padding: 10px 14px;
  background: #fff;
  border-radius: 18px;
  font-size: 14px;
  line-height: 1.5;
  color: #1a1a1a;
  word-break: break-word;
  border: 1px solid #e5e5e5;
}

.message-item--mine .message-text {
  background: #d1f4e0;
  color: #1a1a1a;
  border-color: #a8e6c1;
}

.system-text {
  font-size: 12px;
  color: #999;
  text-align: center;
  padding: 4px 12px;
  background: rgba(0, 0, 0, 0.04);
  border-radius: 12px;
}

.empty-context {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 16px;
  gap: 12px;
  height: 100%;
}

.empty-context__icon {
  width: 48px;
  height: 48px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
  opacity: 0.3;
}

.empty-context__icon svg {
  width: 36px;
  height: 36px;
}

.empty-context__text {
  font-size: 14px;
  color: #999;
}

.context-input {
  display: flex;
  gap: 12px;
  align-items: flex-end;
}

.context-input__left {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 0;
}

.context-input__uploader {
  width: 100%;
}

.context-input__actions {
  display: flex;
  gap: 8px;
  align-items: flex-end;
  flex-shrink: 0;
}

.context-input__img-btn {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  border: 1px solid #d4d4d4;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  color: #666;
}

.context-input__img-btn:hover {
  border-color: #1a1a1a;
  color: #1a1a1a;
}

.context-input__img-btn--active {
  border-color: #34c759;
  color: #34c759;
  background: rgba(52, 199, 89, 0.06);
}

.context-input__img-btn svg {
  width: 18px;
  height: 18px;
}

.context-input :deep(.el-textarea) {
  flex: 1;
}

.context-input :deep(.el-textarea__inner) {
  border-radius: 12px;
  border: 1px solid #d4d4d4;
  transition: all 0.2s;
}

.context-input :deep(.el-textarea__inner:focus) {
  border-color: #1a1a1a;
  box-shadow: 0 0 0 1px #1a1a1a inset;
}

.context-input :deep(.el-button) {
  height: 40px;
  padding: 0 20px;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 8px;
  font-weight: 500;
}

.context-input :deep(.el-button svg) {
  width: 14px;
  height: 14px;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .dialog-title {
    font-size: 15px;
  }
  
  .dialog-goods {
    font-size: 12px;
    max-width: 150px;
  }
  
  .dialog-count {
    font-size: 12px;
  }
  
  .context-wrapper {
    height: 72vh;
    gap: 12px;
  }
  
  .context-content {
    padding: 8px;
    gap: 12px;
    border-radius: 8px;
  }
  
  .message-list {
    gap: 12px;
  }
  
  .message-item {
    gap: 8px;
  }
  
  .message-body {
    max-width: 82%;
  }
  
  .message-header {
    font-size: 11px;
  }
  
  .message-text {
    padding: 8px 10px;
    font-size: 13px;
    border-radius: 14px;
  }
  
  .avatar {
    width: 30px;
    height: 30px;
  }
  
  .avatar svg {
    width: 15px;
    height: 15px;
  }
  
  .context-input {
    gap: 8px;
  }
  
  .context-input__actions {
    gap: 6px;
  }
  
  .context-input__img-btn {
    width: 36px;
    height: 36px;
  }
  
  .context-input :deep(.el-textarea__inner) {
    font-size: 14px;
    padding: 8px 10px;
  }
  
  .context-input :deep(.el-button) {
    height: 36px;
    padding: 0 14px;
  }
  
  .dialog-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 6px;
    padding-right: 0;
  }
  
  .dialog-header__left {
    width: 100%;
    flex-direction: row;
    justify-content: space-between;
  }
  
  .dialog-count {
    position: absolute;
    right: 50px;
    top: 20px;
  }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .context-wrapper {
    height: 68vh;
  }
  
  .message-body {
    max-width: 70%;
  }
}
</style>
