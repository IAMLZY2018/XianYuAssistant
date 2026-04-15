import { ref, reactive, computed, nextTick } from 'vue'
import { getAccountList } from '@/api/account'
import { getMessageList } from '@/api/message'
import { getGoodsList } from '@/api/goods'
import { sendMessage } from '@/api/websocket'
import { showSuccess, showError, showInfo } from '@/utils'
import type { Account } from '@/types'
import type { ChatMessage } from '@/api/message'
import type { GoodsItemWithConfig } from '@/api/goods'

export function useMessageManager() {
  const loading = ref(false)
  const accounts = ref<Account[]>([])
  const selectedAccountId = ref<number | null>(null)
  const goodsIdFilter = ref('')
  const messageList = ref<ChatMessage[]>([])
  const currentPage = ref(1)
  const pageSize = ref(20)
  const total = ref(0)
  const filterCurrentAccount = ref(false)

  // 商品列表
  const goodsList = ref<GoodsItemWithConfig[]>([])
  const goodsCurrentPage = ref(1)
  const goodsTotal = ref(0)
  const goodsLoading = ref(false)
  const goodsListRef = ref<HTMLElement | null>(null)

  // 快速回复
  const quickReplyVisible = ref(false)
  const quickReplyMessage = ref('')
  const quickReplySending = ref(false)
  const currentReplyMessage = ref<ChatMessage | null>(null)

  // 手机端
  const isMobile = ref(false)
  const mobileView = ref<'goods' | 'messages'>('goods')
  const selectedGoodsForMobile = ref<GoodsItemWithConfig | null>(null)

  // 计算属性
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

  const getCurrentAccountUnb = computed(() => {
    if (!selectedAccountId.value) return ''
    const account = accounts.value.find(acc => acc.id === selectedAccountId.value)
    return account ? account.unb : ''
  })

  // 判断是否为用户消息
  const isUserMessage = (row: ChatMessage) => {
    return row.senderUserId !== getCurrentAccountUnb.value
  }

  // 消息类型
  const getContentTypeText = (contentType: number, row: ChatMessage) => {
    if (!isUserMessage(row)) return '我发送的'
    if (contentType === 1) return '用户消息'
    return `系统消息(${contentType})`
  }

  // 消息类型颜色
  const getContentTypeColor = (contentType: number, row: ChatMessage) => {
    if (!isUserMessage(row)) return '#007aff'
    if (contentType === 1) return '#34c759'
    return '#ff9500'
  }

  const getContentTypeBg = (contentType: number, row: ChatMessage) => {
    if (!isUserMessage(row)) return 'rgba(0, 122, 255, 0.1)'
    if (contentType === 1) return 'rgba(52, 199, 89, 0.1)'
    return 'rgba(255, 149, 0, 0.1)'
  }

  // 格式化消息时间
  const formatMessageTime = (timestamp: number) => {
    if (!timestamp) return '-'
    const date = new Date(timestamp)
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

  // 加载账号列表
  const loadAccounts = async () => {
    try {
      const response = await getAccountList()
      if (response.code === 0 || response.code === 200) {
        accounts.value = response.data?.accounts || []
        if (accounts.value.length > 0 && !selectedAccountId.value) {
          selectedAccountId.value = accounts.value[0]?.id ?? null
          await loadMessages()
          await loadGoodsList()
        }
      }
    } catch (error: any) {
      console.error('加载账号列表失败:', error)
    }
  }

  // 加载消息列表
  const loadMessages = async () => {
    if (!selectedAccountId.value) {
      showInfo('请先选择账号')
      return
    }
    loading.value = true
    try {
      const params: any = {
        xianyuAccountId: selectedAccountId.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        filterCurrentAccount: filterCurrentAccount.value
      }
      if (goodsIdFilter.value) {
        params.xyGoodsId = goodsIdFilter.value
      }
      const response = await getMessageList(params)
      if (response.code === 0 || response.code === 200) {
        messageList.value = response.data?.list || []
        total.value = response.data?.totalCount || 0
      } else {
        throw new Error(response.msg || '获取消息列表失败')
      }
    } catch (error: any) {
      console.error('加载消息列表失败:', error)
      messageList.value = []
    } finally {
      loading.value = false
    }
  }

  // 加载商品列表
  const loadGoodsList = async () => {
    if (!selectedAccountId.value) return
    goodsLoading.value = true
    try {
      const params: any = {
        xianyuAccountId: selectedAccountId.value,
        pageNum: goodsCurrentPage.value,
        pageSize: 20
      }
      const response = await getGoodsList(params)
      if (response.code === 0 || response.code === 200) {
        if (goodsCurrentPage.value === 1) {
          goodsList.value = response.data?.itemsWithConfig || []
        } else {
          goodsList.value.push(...(response.data?.itemsWithConfig || []))
        }
        goodsTotal.value = response.data?.totalCount || 0
        checkAndLoadMore()
      }
    } catch (error: any) {
      console.error('加载商品列表失败:', error)
      goodsList.value = []
    } finally {
      goodsLoading.value = false
    }
  }

  // 检查是否需要加载更多
  const checkAndLoadMore = () => {
    nextTick(() => {
      if (!goodsListRef.value) return
      const { scrollHeight, clientHeight } = goodsListRef.value
      if (scrollHeight <= clientHeight && goodsList.value.length < goodsTotal.value) {
        goodsCurrentPage.value++
        loadGoodsList()
      }
    })
  }

  // 商品列表滚动加载
  const handleGoodsScroll = () => {
    if (!goodsListRef.value || goodsLoading.value) return
    const { scrollTop, scrollHeight, clientHeight } = goodsListRef.value
    if (scrollTop + clientHeight >= scrollHeight - 50) {
      if (goodsList.value.length < goodsTotal.value) {
        goodsCurrentPage.value++
        loadGoodsList()
      }
    }
  }

  // 账号变更
  const handleAccountChange = () => {
    currentPage.value = 1
    goodsCurrentPage.value = 1
    goodsIdFilter.value = ''
    loadMessages()
    loadGoodsList()
  }

  // 选择商品筛选
  const selectGoods = (goodsId: string, goods?: GoodsItemWithConfig) => {
    if (goodsIdFilter.value === goodsId) {
      clearFilter()
    } else {
      goodsIdFilter.value = goodsId
      showInfo('已筛选该商品的消息')
      currentPage.value = 1
      loadMessages()
      if (isMobile.value && goods) {
        selectedGoodsForMobile.value = goods
        mobileView.value = 'messages'
      }
    }
  }

  // 手机端返回
  const goBackToGoods = () => {
    mobileView.value = 'goods'
  }

  // 清除筛选
  const clearFilter = () => {
    goodsIdFilter.value = ''
    showInfo('已取消筛选')
    currentPage.value = 1
    loadMessages()
  }

  // 分页
  const handlePageChange = (page: number) => {
    currentPage.value = page
    loadMessages()
  }

  // 打开快速回复
  const openQuickReply = (message: ChatMessage) => {
    currentReplyMessage.value = message
    quickReplyMessage.value = ''
    quickReplyVisible.value = true
  }

  // 发送快速回复
  const handleQuickReply = async () => {
    if (!quickReplyMessage.value.trim()) {
      showInfo('请输入回复内容')
      return
    }
    if (!currentReplyMessage.value || !selectedAccountId.value) {
      showError('消息信息不完整')
      return
    }
    if (!currentReplyMessage.value.sid) {
      showError('会话ID不存在')
      return
    }
    if (!currentReplyMessage.value.senderUserId) {
      showError('接收方ID不存在')
      return
    }
    quickReplySending.value = true
    try {
      const response = await sendMessage({
        xianyuAccountId: selectedAccountId.value,
        cid: currentReplyMessage.value.sid,
        toId: currentReplyMessage.value.senderUserId,
        text: quickReplyMessage.value.trim()
      })
      if (response.code === 0 || response.code === 200) {
        showSuccess('消息发送成功')
        quickReplyVisible.value = false
        quickReplyMessage.value = ''
        currentReplyMessage.value = null
        loadMessages()
      }
    } catch (error: any) {
      console.error('发送消息失败:', error)
    } finally {
      quickReplySending.value = false
    }
  }

  // 图片加载失败
  const handleImgError = (e: Event) => {
    const img = e.target as HTMLImageElement
    img.style.display = 'none'
  }

  return {
    loading,
    accounts,
    selectedAccountId,
    goodsIdFilter,
    messageList,
    currentPage,
    pageSize,
    total,
    totalPages,
    filterCurrentAccount,
    goodsList,
    goodsCurrentPage,
    goodsTotal,
    goodsLoading,
    goodsListRef,
    quickReplyVisible,
    quickReplyMessage,
    quickReplySending,
    currentReplyMessage,
    isMobile,
    mobileView,
    selectedGoodsForMobile,
    loadAccounts,
    loadMessages,
    loadGoodsList,
    handleGoodsScroll,
    handleAccountChange,
    selectGoods,
    goBackToGoods,
    clearFilter,
    handlePageChange,
    openQuickReply,
    handleQuickReply,
    isUserMessage,
    getContentTypeText,
    getContentTypeColor,
    getContentTypeBg,
    formatMessageTime,
    handleImgError
  }
}
