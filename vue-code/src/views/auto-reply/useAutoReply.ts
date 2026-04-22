import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getAccountList } from '@/api/account'
import { getGoodsList, updateAutoReplyStatus, getRagAutoReplyConfig, updateRagAutoReplyConfig } from '@/api/goods'
import { chatWithAI, putNewDataToRAG, queryRAGData, deleteRAGData } from '@/api/ai'
import type { RAGDataItem } from '@/api/ai'
import { showSuccess, showError, showInfo } from '@/utils'
import type { Account } from '@/types'
import type { GoodsItemWithConfig } from '@/api/goods'

// 聊天消息类型
export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  timestamp: number
  loading?: boolean
}

export function useAutoReply() {
  const route = useRoute()

  const saving = ref(false)
  const accounts = ref<Account[]>([])
  const selectedAccountId = ref<number | null>(null)
  const goodsList = ref<GoodsItemWithConfig[]>([])
  const selectedGoods = ref<GoodsItemWithConfig | null>(null)

  // Goods list scroll loading
  const goodsCurrentPage = ref(1)
  const goodsTotal = ref(0)
  const goodsLoading = ref(false)
  const goodsListRef = ref<HTMLElement | null>(null)

  // Goods detail dialog
  const detailDialogVisible = ref(false)
  const selectedGoodsId = ref<string>('')

  // Right panel tab: 'data' | 'chat'
  const rightTab = ref<'data' | 'chat'>('data')

  // Upload data form
  const dataContent = ref('')
  const uploading = ref(false)

  // Query existing RAG data
  const ragDataList = ref<RAGDataItem[]>([])
  const ragDataLoading = ref(false)
  const ragDataVisible = ref(false)

  // Chat
  const chatMessages = ref<ChatMessage[]>([])
  const chatInput = ref('')
  const chatSending = ref(false)
  const chatListRef = ref<HTMLElement | null>(null)

  // Responsive
  const isMobile = ref(false)
  const mobileView = ref<'goods' | 'config'>('goods')

  // Confirm dialog
  const confirmDialog = ref({
    visible: false,
    title: '',
    message: '',
    type: 'danger' as 'danger' | 'primary',
    onConfirm: () => {}
  })

  // RAG config
  const ragDelaySeconds = ref(15)
  const ragConfigLoading = ref(false)
  const ragConfigSaving = ref(false)

  // Check screen size
  const checkScreenSize = () => {
    isMobile.value = window.innerWidth < 768
    if (!isMobile.value) {
      mobileView.value = 'goods'
    }
  }

  // Mobile go back
  const goBackToGoods = () => {
    mobileView.value = 'goods'
  }

  // Format time
  const formatTime = (time: string) => {
    if (!time) return '-'
    return time.replace('T', ' ').substring(0, 19)
  }

  // Format price
  const formatPrice = (price: string) => {
    return price ? `¥${price}` : '-'
  }

  // Get status text
  const getStatusText = (status: number) => {
    const map: Record<number, string> = { 0: '在售', 1: '已下架', 2: '已售出' }
    return map[status] || '未知'
  }

  // Get status class
  const getStatusClass = (status: number) => {
    const map: Record<number, string> = { 0: 'on-sale', 1: 'off-shelf', 2: 'sold' }
    return map[status] || 'off-shelf'
  }

  // Load accounts
  const loadAccounts = async () => {
    try {
      const response = await getAccountList()
      if (response.code === 0 || response.code === 200) {
        accounts.value = response.data?.accounts || []

        const accountIdFromQuery = route.query.accountId
        if (accountIdFromQuery) {
          const accountId = parseInt(accountIdFromQuery as string)
          if (accounts.value.some(acc => acc.id === accountId)) {
            selectedAccountId.value = accountId
            await loadGoods()
            return
          }
        }

        if (accounts.value.length > 0 && !selectedAccountId.value) {
          selectedAccountId.value = accounts.value[0]?.id || null
          loadGoods()
        }
      }
    } catch (error: any) {
      console.error('加载账号列表失败:', error)
    }
  }

  // Load goods list
  const loadGoods = async () => {
    if (!selectedAccountId.value) {
      showInfo('请先选择账号')
      return
    }

    goodsLoading.value = true
    try {
      const params = {
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

        const goodsIdFromQuery = route.query.goodsId
        if (goodsIdFromQuery && goodsCurrentPage.value === 1) {
          const targetGoods = goodsList.value.find(g => g.item.xyGoodId === goodsIdFromQuery)
          if (targetGoods) {
            await selectGoods(targetGoods)
            return
          }
        }

        if (goodsCurrentPage.value === 1 && goodsList.value.length > 0 && !selectedGoods.value) {
          selectGoods(goodsList.value[0]!)
        }

        checkAndLoadMore()
      } else {
        throw new Error(response.msg || '获取商品列表失败')
      }
    } catch (error: any) {
      console.error('加载商品列表失败:', error)
      goodsList.value = []
    } finally {
      goodsLoading.value = false
    }
  }

  // Check and load more
  const checkAndLoadMore = () => {
    nextTick(() => {
      if (!goodsListRef.value) return
      const { scrollHeight, clientHeight } = goodsListRef.value
      if (scrollHeight <= clientHeight && goodsList.value.length < goodsTotal.value) {
        goodsCurrentPage.value++
        loadGoods()
      }
    })
  }

  // Handle goods scroll
  const handleGoodsScroll = () => {
    if (!goodsListRef.value || goodsLoading.value) return
    const { scrollTop, scrollHeight, clientHeight } = goodsListRef.value
    if (scrollTop + clientHeight >= scrollHeight - 50) {
      if (goodsList.value.length < goodsTotal.value) {
        goodsCurrentPage.value++
        loadGoods()
      }
    }
  }

  // Account change
  const handleAccountChange = () => {
    selectedGoods.value = null
    goodsCurrentPage.value = 1
    chatMessages.value = []
    dataContent.value = ''
    loadGoods()
  }

  // Select goods
  const selectGoods = async (goods: GoodsItemWithConfig) => {
    selectedGoods.value = goods
    // 切换商品时重置聊天和资料
    chatMessages.value = []
    dataContent.value = ''
    rightTab.value = 'data'
    ragDataVisible.value = false
    ragDataList.value = []

    if (isMobile.value) {
      mobileView.value = 'config'
    }

    // 加载RAG配置
    loadRagConfig()
  }

  // Load RAG config
  const loadRagConfig = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    ragConfigLoading.value = true
    try {
      const response = await getRagAutoReplyConfig({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId
      })
      if (response.code === 0 || response.code === 200) {
        ragDelaySeconds.value = response.data?.ragDelaySeconds ?? 15
      }
    } catch (error: any) {
      console.error('加载RAG配置失败:', error)
    } finally {
      ragConfigLoading.value = false
    }
  }

  // Update RAG delay seconds
  const updateRagDelaySeconds = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    // 验证范围
    let seconds = ragDelaySeconds.value
    if (seconds < 5) seconds = 5
    if (seconds > 120) seconds = 120
    ragDelaySeconds.value = seconds

    ragConfigSaving.value = true
    try {
      const response = await updateRagAutoReplyConfig({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        ragDelaySeconds: seconds
      })
      if (response.code === 0 || response.code === 200) {
        showSuccess('延时设置已保存')
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('更新RAG延时失败:', error)
      showError(error.message || '操作失败')
    } finally {
      ragConfigSaving.value = false
    }
  }

  // Toggle auto reply
  const toggleAutoReply = async (value: boolean) => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    try {
      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoReplyOn: value ? 1 : 0
      })

      if (response.code === 0 || response.code === 200) {
        showSuccess(`自动回复${value ? '开启' : '关闭'}成功`)
        if (selectedGoods.value) {
          selectedGoods.value.xianyuAutoReplyOn = value ? 1 : 0
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.xianyuAutoReplyOn = value ? 1 : 0
        }
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      if (selectedGoods.value) {
        selectedGoods.value.xianyuAutoReplyOn = value ? 0 : 1
      }
    }
  }

  // Upload data to RAG
  const handleUploadData = async () => {
    if (!selectedGoods.value) {
      showInfo('请先选择商品')
      return
    }
    if (!dataContent.value.trim()) {
      showInfo('请输入资料内容')
      return
    }

    uploading.value = true
    try {
      const response = await putNewDataToRAG({
        content: dataContent.value.trim(),
        goodsId: selectedGoods.value.item.xyGoodId
      })
      if (!response.ok) {
        if (response.status === 405 || response.status === 404) {
          throw new Error('AI 功能未开启，请在后端配置 ai.enabled=true 并确保 Chroma 数据库可用')
        }
        throw new Error(`上传资料失败: ${response.status}`)
      }
      const result = await response.json()
      if (result.code === 0 || result.code === 200) {
        showSuccess('资料上传成功')
        dataContent.value = ''
        // 上传成功后如果正在查看资料列表，自动刷新
        if (ragDataVisible.value) {
          handleQueryRAGData()
        }
      } else {
        throw new Error(result.msg || '上传资料失败')
      }
    } catch (error: any) {
      console.error('上传资料失败:', error)
      showError(error.message || '上传资料失败')
    } finally {
      uploading.value = false
    }
  }

  // Query existing RAG data
  const handleQueryRAGData = async () => {
    if (!selectedGoods.value) {
      showInfo('请先选择商品')
      return
    }

    ragDataLoading.value = true
    try {
      const response = await queryRAGData({
        goodsId: selectedGoods.value.item.xyGoodId
      })
      if (!response.ok) {
        if (response.status === 405 || response.status === 404) {
          throw new Error('AI 功能未开启，请在后端配置 ai.enabled=true 并确保 Chroma 数据库可用')
        }
        throw new Error(`查询资料失败: ${response.status}`)
      }
      const result = await response.json()
      if (result.code === 0 || result.code === 200) {
        ragDataList.value = result.data || []
      } else {
        throw new Error(result.msg || '查询资料失败')
      }
    } catch (error: any) {
      console.error('查询资料失败:', error)
      showError(error.message || '查询资料失败')
      ragDataList.value = []
    } finally {
      ragDataLoading.value = false
    }
  }

  // Delete RAG data
  const handleDeleteRAGData = (documentId: string) => {
    confirmDialog.value = {
      visible: true,
      title: '删除资料',
      message: '确定要删除该资料吗？删除后不可恢复。',
      type: 'danger',
      onConfirm: async () => {
        confirmDialog.value.visible = false
        try {
          const response = await deleteRAGData({ documentId })
          if (!response.ok) {
            if (response.status === 405 || response.status === 404) {
              throw new Error('AI 功能未开启，请在后端配置 ai.enabled=true 并确保 Chroma 数据库可用')
            }
            throw new Error(`删除资料失败: ${response.status}`)
          }
          const result = await response.json()
          if (result.code === 0 || result.code === 200) {
            showSuccess('资料删除成功')
            // 从列表中移除已删除项
            ragDataList.value = ragDataList.value.filter(item => item.documentId !== documentId)
          } else {
            throw new Error(result.msg || '删除资料失败')
          }
        } catch (error: any) {
          console.error('删除资料失败:', error)
          showError(error.message || '删除资料失败')
        }
      }
    }
  }

  // Generate unique ID
  const genId = () => Date.now().toString(36) + Math.random().toString(36).substring(2, 7)

  // Scroll chat to bottom
  const scrollChatToBottom = () => {
    nextTick(() => {
      if (chatListRef.value) {
        chatListRef.value.scrollTop = chatListRef.value.scrollHeight
      }
    })
  }

  // Send chat message
  const handleSendChat = async () => {
    if (!selectedGoods.value) {
      showInfo('请先选择商品')
      return
    }
    if (!chatInput.value.trim()) return
    if (chatSending.value) return

    const userMsg: ChatMessage = {
      id: genId(),
      role: 'user',
      content: chatInput.value.trim(),
      timestamp: Date.now()
    }
    chatMessages.value.push(userMsg)
    const inputText = chatInput.value.trim()
    chatInput.value = ''
    scrollChatToBottom()

    // Add assistant placeholder
    const assistantMsg: ChatMessage = {
      id: genId(),
      role: 'assistant',
      content: '',
      timestamp: Date.now(),
      loading: true
    }
    chatMessages.value.push(assistantMsg)
    scrollChatToBottom()

    chatSending.value = true
    try {
      const response = await chatWithAI({
        msg: inputText,
        goodsId: selectedGoods.value.item.xyGoodId
      })

      if (!response.ok) {
        if (response.status === 405 || response.status === 404) {
          throw new Error('AI 功能未开启，请在后端配置 ai.enabled=true 并确保 Chroma 数据库可用')
        }
        throw new Error(`请求失败: ${response.status}`)
      }

      // 处理 SSE 流式响应
      assistantMsg.loading = false

      const reader = response.body?.getReader()
      const decoder = new TextDecoder()

      if (reader) {
        let buffer = ''
        while (true) {
          const { done, value } = await reader.read()
          if (done) break

          buffer += decoder.decode(value, { stream: true })

          // 处理 SSE 格式: data:xxx\n\n
          const lines = buffer.split('\n')
          buffer = lines.pop() || ''

          for (const line of lines) {
            if (line.startsWith('data:')) {
              const data = line.substring(5).trim()
              if (data === '[DONE]') continue
              try {
                // 尝试解析 JSON，提取 reply/content/text 字段
                const parsed = JSON.parse(data)
                assistantMsg.content += parsed.reply || parsed.content || parsed.text || ''
              } catch {
                // 直接作为文本追加
                assistantMsg.content += data
              }
              scrollChatToBottom()
            }
          }
        }

        // 处理剩余 buffer
        if (buffer.startsWith('data:')) {
          const data = buffer.substring(5).trim()
          if (data && data !== '[DONE]') {
            try {
              const parsed = JSON.parse(data)
              assistantMsg.content += parsed.reply || parsed.content || parsed.text || ''
            } catch {
              assistantMsg.content += data
            }
          }
        }
      } else {
        // 没有 reader（不支持流式读取），直接读取文本
        const text = await response.text()
        assistantMsg.content = text || '暂无回复'
      }

      // 如果流式读取后内容仍为空
      if (!assistantMsg.content) {
        assistantMsg.content = '暂无回复'
      }

      scrollChatToBottom()
    } catch (error: any) {
      console.error('AI 对话失败:', error)
      assistantMsg.loading = false
      assistantMsg.content = '对话失败，请稍后重试'
      scrollChatToBottom()
    } finally {
      chatSending.value = false
    }
  }

  // Handle chat input keydown (Enter to send)
  const handleChatKeydown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault()
      handleSendChat()
    }
  }

  // View goods detail
  const viewGoodsDetail = () => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }
    selectedGoodsId.value = selectedGoods.value.item.xyGoodId
    detailDialogVisible.value = true
  }

  // Confirm dialog actions
  const handleDialogConfirm = () => {
    confirmDialog.value.onConfirm()
  }

  const handleDialogCancel = () => {
    confirmDialog.value.visible = false
  }

  // Lifecycle
  onMounted(() => {
    loadAccounts()
    checkScreenSize()
    window.addEventListener('resize', checkScreenSize)
  })

  onUnmounted(() => {
    window.removeEventListener('resize', checkScreenSize)
  })

  return {
    // State
    saving,
    accounts,
    selectedAccountId,
    goodsList,
    selectedGoods,
    goodsTotal,
    goodsLoading,
    goodsListRef,
    detailDialogVisible,
    selectedGoodsId,
    rightTab,
    dataContent,
    uploading,
    ragDataList,
    ragDataLoading,
    ragDataVisible,
    chatMessages,
    chatInput,
    chatSending,
    chatListRef,
    isMobile,
    mobileView,
    confirmDialog,
    ragDelaySeconds,
    ragConfigLoading,
    ragConfigSaving,

    // Methods
    handleAccountChange,
    selectGoods,
    toggleAutoReply,
    handleUploadData,
    handleQueryRAGData,
    handleDeleteRAGData,
    handleSendChat,
    handleChatKeydown,
    handleGoodsScroll,
    goBackToGoods,
    viewGoodsDetail,
    handleDialogConfirm,
    handleDialogCancel,
    formatTime,
    formatPrice,
    getStatusText,
    getStatusClass,
    checkScreenSize,
    loadRagConfig,
    updateRagDelaySeconds
  }
}
