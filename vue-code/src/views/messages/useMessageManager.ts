import { ref, computed, nextTick } from 'vue'
import { getAccountList } from '@/api/account'
import { getMessageList } from '@/api/message'
import { getGoodsList } from '@/api/goods'
import { showInfo } from '@/utils'
import type { Account } from '@/types'
import type { ChatMessage } from '@/api/message'
import type { GoodsItemWithConfig } from '@/api/goods'

export function useMessageManager() {
  const loading = ref(false)
  const silentLoading = ref(false)
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
    if (contentType === 999) return '手动回复'
    if (contentType === 997) return '图片回复'
    if (contentType === 888) return '自动回复'
    if (contentType === 887) return '自动回复图片'
    if (!isUserMessage(row)) return '我发送的'
    if (contentType === 1) return '用户消息'
    return `系统消息(${contentType})`
  }

  const getContentTypeColor = (contentType: number, row: ChatMessage) => {
    if (contentType === 999 || contentType === 997) return '#5856d6'
    if (contentType === 888 || contentType === 887) return '#af52de'
    if (!isUserMessage(row)) return '#007aff'
    if (contentType === 1) return '#34c759'
    return '#ff9500'
  }

  const getContentTypeBg = (contentType: number, row: ChatMessage) => {
    if (contentType === 999 || contentType === 997) return 'rgba(88, 86, 214, 0.1)'
    if (contentType === 888 || contentType === 887) return 'rgba(175, 82, 222, 0.1)'
    if (!isUserMessage(row)) return 'rgba(0, 122, 255, 0.1)'
    if (contentType === 1) return 'rgba(52, 199, 89, 0.1)'
    return 'rgba(255, 149, 0, 0.1)'
  }

  // 格式化消息时间
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
  const loadMessages = async (silent = false) => {
    if (!selectedAccountId.value) {
      showInfo('请先选择账号')
      return
    }
    if (!silent) {
      loading.value = true
    } else {
      silentLoading.value = true
    }
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
        const newList = response.data?.list || []
        const newTotal = response.data?.totalCount || 0

        if (silent && messageList.value.length > 0) {
          const existingIds = new Set(messageList.value.map(m => m.id))
          const newItems = newList.filter((m: ChatMessage) => !existingIds.has(m.id))
          if (newItems.length > 0) {
            newItems.forEach((m: ChatMessage) => { m.isNew = true })
            messageList.value = [...newItems, ...messageList.value.map(m => ({ ...m, isNew: false }))]
            total.value = newTotal
          } else if (newTotal !== total.value) {
            total.value = newTotal
          }
        } else {
          messageList.value = newList
          total.value = newTotal
        }
      } else {
        throw new Error(response.msg || '获取消息列表失败')
      }
    } catch (error: any) {
      console.error('加载消息列表失败:', error)
      if (!silent) {
        messageList.value = []
      }
    } finally {
      loading.value = false
      silentLoading.value = false
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

  // 图片加载失败
  const handleImgError = (e: Event) => {
    const img = e.target as HTMLImageElement
    img.style.display = 'none'
  }

  return {
    loading,
    silentLoading,
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
    isMobile,
    mobileView,
    selectedGoodsForMobile,
    getCurrentAccountUnb,
    loadAccounts,
    loadMessages,
    loadGoodsList,
    handleGoodsScroll,
    handleAccountChange,
    selectGoods,
    goBackToGoods,
    clearFilter,
    handlePageChange,
    isUserMessage,
    getContentTypeText,
    getContentTypeColor,
    getContentTypeBg,
    formatMessageTime,
    handleImgError
  }
}
