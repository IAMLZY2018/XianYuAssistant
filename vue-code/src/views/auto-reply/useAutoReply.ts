import { ref, computed, onMounted, onUnmounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAccountList } from '@/api/account'
import { getGoodsList, updateAutoReplyStatus, getAutoReplyConfig, updateAutoReplyConfig, getAutoReplyRecords } from '@/api/goods'
import { chatWithAI, chatTestWithAI, putNewDataToRAG, queryRAGData, deleteRAGData, saveFixedMaterial, getFixedMaterial, syncDetailToFixedMaterial } from '@/api/ai'
import type { RAGDataItem } from '@/api/ai'
import type { AutoReplyRecord } from '@/api/goods'
import { showSuccess, showError, showInfo } from '@/utils'
import { ElMessage } from 'element-plus'
import type { Account } from '@/types'
import type { GoodsItemWithConfig } from '@/api/goods'
import { getKeywordReplyRules, addKeywordRule, deleteKeywordRule, updateKeyword, addKeywordContent, deleteKeywordContent, updateKeywordContent, updateKeywordRuleMatchMode, ensureFallbackRule } from '@/api/keywordReply'
import type { KeywordReplyRule, KeywordReplyContent } from '@/api/keywordReply'

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
  const router = useRouter()

  const showAiConfigTip = () => {
    ElMessage({
      type: 'warning',
      duration: 5000,
      dangerouslyUseHTMLString: true,
      message: '请完成AI配置再上传资料，<a href="#/settings" style="color:#34c759;text-decoration:underline;font-weight:600;" onclick="event.preventDefault();window.__gotoSettings&&window.__gotoSettings()">点击前往</a>'
    })
  }
  ;(window as any).__gotoSettings = () => router.push('/settings')

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
  const onlyOnSale = ref(true)

  // Goods detail dialog
  const detailDialogVisible = ref(false)
  const selectedGoodsId = ref<string>('')

  // Right panel tab: 'data' | 'chat'
  const rightTab = ref<'data' | 'chat'>('data')

  // Upload data form
  const dataContent = ref('')
  const uploading = ref(false)

  // Fixed material
  const fixedMaterial = ref('')
  const fixedMaterialSaving = ref(false)
  const fixedMaterialSyncing = ref(false)
  const fixedMaterialExpanded = ref(true)

  // Query existing knowledge data
  const dataList = ref<RAGDataItem[]>([])
  const dataLoading = ref(false)
  const dataVisible = ref(false)

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

  // Auto reply config
  const delaySeconds = ref(15)
  const configLoading = ref(false)
  const configSaving = ref(false)

  // Auto reply records
  const recordsVisible = ref(false)
  const recordsList = ref<AutoReplyRecord[]>([])
  const recordsLoading = ref(false)
  const recordsTotal = ref(0)
  const recordsPage = ref(1)
  const recordsPageSize = ref(20)
  const recordDetailVisible = ref(false)
  const recordDetail = ref<AutoReplyRecord | null>(null)
  const contextExpanded = ref(false)

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
        onlyOnSale: onlyOnSale.value,
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

        if (goodsCurrentPage.value === 1 && goodsList.value.length > 0 && !selectedGoods.value && !isMobile.value) {
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
    dataVisible.value = false
    dataList.value = []

    if (isMobile.value) {
      mobileView.value = 'config'
    }

    // 加载自动回复配置
    loadConfig()
    // 加载固定资料
    loadFixedMaterial()
  }

  // Load fixed material
  const loadFixedMaterial = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    try {
      const response = await getFixedMaterial({
        accountId: selectedAccountId.value,
        goodsId: selectedGoods.value.item.xyGoodId
      })
      const data = await response.json()
      if (data.code === 0 || data.code === 200) {
        fixedMaterial.value = data.data?.fixedMaterial || ''
        fixedMaterialExpanded.value = !fixedMaterial.value
      }
    } catch (error: any) {
      console.error('加载固定资料失败:', error)
    }
  }

  // Save fixed material
  const handleSaveFixedMaterial = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    fixedMaterialSaving.value = true
    try {
      const response = await saveFixedMaterial({
        accountId: selectedAccountId.value,
        goodsId: selectedGoods.value.item.xyGoodId,
        fixedMaterial: fixedMaterial.value
      })
      const data = await response.json()
      if (data.code === 0 || data.code === 200) {
        showSuccess('固定资料保存成功')
      } else {
        showError(data.msg || '保存失败')
      }
    } catch (error: any) {
      // 只有在错误消息未显示过时才弹出提示（避免重复显示）
      if (!error.messageShown) {
        showError('保存固定资料失败: ' + error.message)
      }
    } finally {
      fixedMaterialSaving.value = false
    }
  }

  // Sync detail to fixed material
  const handleSyncDetailToFixedMaterial = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    fixedMaterialSyncing.value = true
    try {
      const response = await syncDetailToFixedMaterial({
        accountId: selectedAccountId.value,
        goodsId: selectedGoods.value.item.xyGoodId
      })
      const data = await response.json()
      if (data.code === 0 || data.code === 200) {
        showSuccess('商品详情已同步到固定资料')
        await loadFixedMaterial()
      } else {
        showError(data.msg || '同步失败')
      }
    } catch (error: any) {
      // 只有在错误消息未显示过时才弹出提示（避免重复显示）
      if (!error.messageShown) {
        showError('同步商品详情失败: ' + error.message)
      }
    } finally {
      fixedMaterialSyncing.value = false
    }
  }

  // Toggle fixed material expanded
  const toggleFixedMaterialExpanded = () => {
    fixedMaterialExpanded.value = !fixedMaterialExpanded.value
  }

  // ===== Keyword Reply =====
  const replyModeTab = ref<'ai' | 'keyword'>('ai')
  const keywordRules = ref<KeywordReplyRule[]>([])
  const newKeyword = ref('')
  const newContentText = ref<Record<number, string>>({})
  const newContentImage = ref<Record<number, string>>({})
  const selectedKeywordRuleId = ref<string | number | null>(null)
  const showAddKeywordInput = ref(false)
  const addKeywordDialogVisible = ref(false)
  const addReplyDialogVisible = ref(false)
  const addReplyText = ref('')
  const addReplyImageUrls = ref<string[]>([])
  const fallbackRule = ref<KeywordReplyRule | null>(null)
  const fallbackText = ref('')
  const fallbackImageUrls = ref<string[]>([])
  const fallbackExpanded = ref(true)
  const editKeywordDialogVisible = ref(false)
  const editKeywordId = ref<number | null>(null)
  const editKeywordName = ref('')

  const selectedKeywordRule = computed(() => {
    if (!selectedKeywordRuleId.value) return null
    return keywordRules.value.find(r => r.id === selectedKeywordRuleId.value) || null
  })

  const loadKeywordRules = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return
    try {
      const res = await getKeywordReplyRules({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId
      })
      const allRules: KeywordReplyRule[] = (res as any)?.data || res || []
      keywordRules.value = allRules.filter(r => !r.isFallback || r.isFallback === 0)
      const fb = allRules.find(r => r.isFallback === 1)
      if (fb) {
        fallbackRule.value = fb
        fallbackText.value = fb.contents?.length ? fb.contents.find(c => c.replyText)?.replyText || '' : ''
        fallbackImageUrls.value = fb.contents?.length ? fb.contents.filter(c => c.replyImageUrl).map(c => c.replyImageUrl!) : []
        fallbackExpanded.value = !fb.contents?.length
      } else {
        fallbackRule.value = null
        fallbackText.value = ''
        fallbackImageUrls.value = []
        fallbackExpanded.value = true
      }
    } catch (e) {
      keywordRules.value = []
      fallbackRule.value = null
      fallbackText.value = ''
      fallbackImageUrls.value = []
      fallbackExpanded.value = true
    }
  }

  const toggleKeywordReply = async (checked: boolean) => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    try {
      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoReplyOn: selectedGoods.value.xianyuAutoReplyOn,
        xianyuAutoReplyContextOn: selectedGoods.value.xianyuAutoReplyContextOn,
        xianyuKeywordReplyOn: checked ? 1 : 0
      } as any)

      if (response.code === 0 || response.code === 200) {
        showSuccess(`关键词回复${checked ? '开启' : '关闭'}成功`)
        if (selectedGoods.value) {
          selectedGoods.value.xianyuKeywordReplyOn = checked ? 1 : 0
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.xianyuKeywordReplyOn = checked ? 1 : 0
        }
        await loadKeywordRules()
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      if (selectedGoods.value) {
        selectedGoods.value.xianyuKeywordReplyOn = checked ? 0 : 1
      }
    }
  }

  const toggleHumanIntervention = async (checked: boolean) => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    try {
      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoReplyOn: selectedGoods.value.xianyuAutoReplyOn,
        xianyuAutoReplyContextOn: selectedGoods.value.xianyuAutoReplyContextOn,
        xianyuKeywordReplyOn: selectedGoods.value.xianyuKeywordReplyOn,
        humanInterventionOn: checked ? 1 : 0,
        humanInterventionMinutes: selectedGoods.value.humanInterventionMinutes || 10
      } as any)

      if (response.code === 0 || response.code === 200) {
        showSuccess(`人工干预${checked ? '开启' : '关闭'}成功`)
        if (selectedGoods.value) {
          selectedGoods.value.humanInterventionOn = checked ? 1 : 0
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.humanInterventionOn = checked ? 1 : 0
        }
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      if (selectedGoods.value) {
        selectedGoods.value.humanInterventionOn = checked ? 0 : 1
      }
    }
  }

  const updateHumanInterventionMinutes = async (minutes: number) => {
    if (!selectedGoods.value || !selectedAccountId.value) return
    if (minutes < 1 || minutes > 120) return

    try {
      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoReplyOn: selectedGoods.value.xianyuAutoReplyOn,
        xianyuAutoReplyContextOn: selectedGoods.value.xianyuAutoReplyContextOn,
        xianyuKeywordReplyOn: selectedGoods.value.xianyuKeywordReplyOn,
        humanInterventionOn: selectedGoods.value.humanInterventionOn,
        humanInterventionMinutes: minutes
      } as any)

      if (response.code === 0 || response.code === 200) {
        if (selectedGoods.value) {
          selectedGoods.value.humanInterventionMinutes = minutes
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.humanInterventionMinutes = minutes
        }
      }
    } catch (error: any) {
      console.error('更新人工干预时长失败:', error)
    }
  }

  const handleAddKeyword = async () => {
    if (!selectedGoods.value || !selectedAccountId.value || !newKeyword.value.trim()) return
    try {
      const res = await addKeywordRule({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        keyword: newKeyword.value.trim()
      })
      const rule = (res as any)?.data || res
      if (rule) {
        keywordRules.value.push(rule)
        selectedKeywordRuleId.value = rule.id
      }
      newKeyword.value = ''
      showAddKeywordInput.value = false
    } catch (e: any) {
      showError(e?.message || '添加关键词失败')
    }
  }

  const handleAddKeywordFromDialog = async () => {
    if (!newKeyword.value.trim()) return
    await handleAddKeyword()
    addKeywordDialogVisible.value = false
  }

  const handleUpdateMatchMode = async (ruleId: string | number, matchMode: number) => {
    try {
      await updateKeywordRuleMatchMode({ ruleId, matchMode })
      const rule = keywordRules.value.find(r => r.id === ruleId)
      if (rule) {
        rule.matchMode = matchMode
      }
      showSuccess('匹配模式修改成功')
    } catch (e: any) {
      showError(e?.message || '更新匹配模式失败')
    }
  }

  const handleSaveFallbackText = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return
    try {
      let rule = fallbackRule.value
      if (!rule) {
        const res = await ensureFallbackRule({
          xianyuAccountId: selectedAccountId.value,
          xyGoodsId: selectedGoods.value.item.xyGoodId
        })
        rule = (res as any)?.data || res || null
        fallbackRule.value = rule
      }
      if (!rule) return

      const text = fallbackText.value.trim()
      const images = fallbackImageUrls.value.filter(u => u.trim())
      const hasContent = text || images.length > 0

      if (rule.contents?.length) {
        await Promise.all(rule.contents.map(c => deleteKeywordContent({ contentId: c.id })))
        rule.contents = []
      }

      if (hasContent) {
        rule.contents = []
        if (images.length > 0) {
          const res = await addKeywordContent({ ruleId: rule.id, replyText: text, replyImageUrl: images[0] })
          const content = (res as any)?.data || res
          if (content) rule.contents.push(content)
          for (let i = 1; i < images.length; i++) {
            const res2 = await addKeywordContent({ ruleId: rule.id, replyText: '', replyImageUrl: images[i] })
            const content2 = (res2 as any)?.data || res2
            if (content2) rule.contents.push(content2)
          }
        } else if (text) {
          const res = await addKeywordContent({ ruleId: rule.id, replyText: text, replyImageUrl: '' })
          const content = (res as any)?.data || res
          if (content) rule.contents.push(content)
        }
      }
      showSuccess('未匹配回复保存成功')
      fallbackExpanded.value = false
    } catch (e: any) {
      showError(e?.message || '保存未匹配回复失败')
    }
  }

  const handleAddReplyFromDialog = async () => {
    if (!selectedKeywordRuleId.value) return
    const text = addReplyText.value.trim()
    const images = addReplyImageUrls.value.filter(u => u.trim())
    if (!text && images.length === 0) return
    try {
      const rule = keywordRules.value.find(r => r.id === selectedKeywordRuleId.value)
      if (!rule) return
      rule.contents = rule.contents || []
      if (images.length > 0) {
        const res = await addKeywordContent({ ruleId: selectedKeywordRuleId.value, replyText: text, replyImageUrl: images[0] })
        const content = (res as any)?.data || res
        if (content) rule.contents.push(content)
        for (let i = 1; i < images.length; i++) {
          const res2 = await addKeywordContent({ ruleId: selectedKeywordRuleId.value, replyText: '', replyImageUrl: images[i] })
          const content2 = (res2 as any)?.data || res2
          if (content2) rule.contents.push(content2)
        }
      } else {
        const res = await addKeywordContent({ ruleId: selectedKeywordRuleId.value, replyText: text, replyImageUrl: '' })
        const content = (res as any)?.data || res
        if (content) rule.contents.push(content)
      }
      addReplyText.value = ''
      addReplyImageUrls.value = []
      addReplyDialogVisible.value = false
    } catch (e: any) {
      showError(e?.message || '添加回复失败')
    }
  }

  const handleDeleteRule = async (ruleId: number) => {
    try {
      await deleteKeywordRule({ ruleId })
      keywordRules.value = keywordRules.value.filter(r => r.id !== ruleId)
      if (selectedKeywordRuleId.value === ruleId) {
        selectedKeywordRuleId.value = keywordRules.value.length > 0 ? keywordRules.value[0]!.id : null
      }
    } catch (e: any) {
      showError(e?.message || '删除关键词规则失败')
    }
  }

  const handleOpenEditKeyword = (rule: KeywordReplyRule) => {
    editKeywordId.value = rule.id as number
    editKeywordName.value = rule.keyword
    editKeywordDialogVisible.value = true
  }

  const handleSaveEditKeyword = async () => {
    if (!editKeywordId.value || !editKeywordName.value.trim()) return
    try {
      await updateKeyword({ ruleId: editKeywordId.value, keyword: editKeywordName.value.trim() })
      const rule = keywordRules.value.find(r => r.id === editKeywordId.value)
      if (rule) {
        rule.keyword = editKeywordName.value.trim()
      }
      editKeywordDialogVisible.value = false
      showSuccess('关键词修改成功')
    } catch (e: any) {
      showError(e?.message || '修改关键词失败')
    }
  }

  const handleDeleteFromEditDialog = async () => {
    if (!editKeywordId.value) return
    try {
      await deleteKeywordRule({ ruleId: editKeywordId.value })
      keywordRules.value = keywordRules.value.filter(r => r.id !== editKeywordId.value)
      if (selectedKeywordRuleId.value === editKeywordId.value) {
        selectedKeywordRuleId.value = keywordRules.value.length > 0 ? keywordRules.value[0]!.id : null
      }
      editKeywordDialogVisible.value = false
      showSuccess('关键词删除成功')
    } catch (e: any) {
      showError(e?.message || '删除关键词失败')
    }
  }

  const handleUpdateKeyword = async (ruleId: number, newKeyword: string) => {
    if (!newKeyword.trim()) return
    try {
      await updateKeyword({ ruleId, keyword: newKeyword.trim() })
      const rule = keywordRules.value.find(r => r.id === ruleId)
      if (rule) {
        rule.keyword = newKeyword.trim()
      }
    } catch (e: any) {
      showError(e?.message || '修改关键词失败')
    }
  }

  const handleAddContent = async (ruleId: number) => {
    try {
      const res = await addKeywordContent({ ruleId, replyText: '', replyImageUrl: '' })
      const content = (res as any)?.data || res
      const rule = keywordRules.value.find(r => r.id === ruleId)
      if (rule && content) {
        rule.contents = rule.contents || []
        rule.contents.push(content)
      }
    } catch (e: any) {
      showError(e?.message || '添加回复内容失败')
    }
  }

  const handleContentTextChange = async (content: KeywordReplyContent, newText: string) => {
    try {
      await updateKeywordContent({ contentId: content.id, replyText: newText, replyImageUrl: content.replyImageUrl })
      content.replyText = newText
    } catch (e: any) {
      showError(e?.message || '更新回复文本失败')
    }
  }

  const handleContentImageUpload = async (content: KeywordReplyContent, imageUrl: string) => {
    try {
      await updateKeywordContent({ contentId: content.id, replyText: content.replyText, replyImageUrl: imageUrl })
      content.replyImageUrl = imageUrl
    } catch (e: any) {
      showError(e?.message || '更新回复图片失败')
    }
  }

  const handleContentImageDelete = async (content: KeywordReplyContent) => {
    try {
      await updateKeywordContent({ contentId: content.id, replyText: content.replyText, replyImageUrl: '' })
      content.replyImageUrl = ''
    } catch (e: any) {
      showError(e?.message || '删除回复图片失败')
    }
  }

  const handleDeleteContent = async (contentId: string | number, ruleId: string | number) => {
    try {
      await deleteKeywordContent({ contentId })
      const rule = keywordRules.value.find(r => r.id === ruleId)
      if (rule) {
        rule.contents = rule.contents.filter(c => c.id !== contentId)
      }
    } catch (e: any) {
      showError(e?.message || '删除回复内容失败')
    }
  }

  // Watch selected goods to load keyword rules
  watch(() => selectedGoods.value, (newVal) => {
    if (newVal) {
      loadKeywordRules()
    } else {
      keywordRules.value = []
    }
  })

  // Load auto reply config
  const loadConfig = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    configLoading.value = true
    try {
      const response = await getAutoReplyConfig({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId
      })
      if (response.code === 0 || response.code === 200) {
        delaySeconds.value = response.data?.ragDelaySeconds ?? 15
      }
    } catch (error: any) {
      console.error('加载自动回复配置失败:', error)
    } finally {
      configLoading.value = false
    }
  }

  // Update delay seconds
  const updateDelaySeconds = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    // 验证范围
    let seconds = delaySeconds.value
    if (seconds < 5) seconds = 5
    if (seconds > 120) seconds = 120
    delaySeconds.value = seconds

    configSaving.value = true
    try {
      const response = await updateAutoReplyConfig({
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
      console.error('更新延时失败:', error)
      // 只有在错误消息未显示过时才弹出提示（避免重复显示）
      if (!error.messageShown) {
        showError(error.message || '操作失败')
      }
    } finally {
      configSaving.value = false
    }
  }

  // Toggle auto reply
  const toggleAutoReply = async (value: boolean) => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    try {
      const requestContextOn = selectedGoods.value.xianyuAutoReplyContextOn ?? (value ? 1 : 0)

      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoReplyOn: value ? 1 : 0,
        xianyuAutoReplyContextOn: requestContextOn
      })

      if (response.code === 0 || response.code === 200) {
        showSuccess(`自动回复${value ? '开启' : '关闭'}成功`)
        if (selectedGoods.value) {
          selectedGoods.value.xianyuAutoReplyOn = value ? 1 : 0
          if (value && selectedGoods.value.xianyuAutoReplyContextOn == null) {
            selectedGoods.value.xianyuAutoReplyContextOn = 1
          }
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.xianyuAutoReplyOn = value ? 1 : 0
          if (value && goodsItem.xianyuAutoReplyContextOn == null) {
            goodsItem.xianyuAutoReplyContextOn = 1
          }
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

  // Toggle context switch
  const toggleContextOn = async (value: boolean) => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    try {
      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoReplyOn: selectedGoods.value.xianyuAutoReplyOn,
        xianyuAutoReplyContextOn: value ? 1 : 0
      })

      if (response.code === 0 || response.code === 200) {
        showSuccess(`携带上下文${value ? '开启' : '关闭'}成功`)
        if (selectedGoods.value) {
          selectedGoods.value.xianyuAutoReplyContextOn = value ? 1 : 0
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.xianyuAutoReplyContextOn = value ? 1 : 0
        }
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      if (selectedGoods.value) {
        selectedGoods.value.xianyuAutoReplyContextOn = value ? 0 : 1
      }
    }
  }

  // Upload knowledge data
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
          throw new Error('请前往系统设置->AI服务配置中完成配置')
        }
        throw new Error(`上传资料失败: ${response.status}`)
      }
      const result = await response.json()
      if (result.code === 0 || result.code === 200) {
        showSuccess('添加成功')
        dataContent.value = ''
        if (dataVisible.value) {
          handleQueryData()
        }
      } else if (result.code === 1001) {
        showAiConfigTip()
      } else {
        // 检查是否是AI未配置的错误
        const errorMsg = result.msg || '上传资料失败'
        if (errorMsg.includes('AI') || errorMsg.includes('API') || errorMsg.includes('配置')) {
          throw new Error('请前往系统设置->AI服务配置中完成配置')
        }
        throw new Error(errorMsg)
      }
    } catch (error: any) {
      console.error('上传资料失败:', error)
      // 如果错误消息包含配置相关提示，使用友好提示
      const errorMsg = error.message || '上传资料失败'
      if (errorMsg.includes('配置') || errorMsg.includes('AI') || errorMsg.includes('API')) {
        showError('请前往系统设置->AI服务配置中完成配置')
      } else {
        showError(errorMsg)
      }
    } finally {
      uploading.value = false
    }
  }

  // Query existing knowledge data
  const handleQueryData = async () => {
    if (!selectedGoods.value) {
      showInfo('请先选择商品')
      return
    }

    dataLoading.value = true
    try {
      const response = await queryRAGData({
        goodsId: selectedGoods.value.item.xyGoodId
      })
      if (!response.ok) {
        if (response.status === 405 || response.status === 404) {
          throw new Error('AI 功能未开启，请前往系统设置->AI服务配置中完成配置')
        }
        throw new Error(`查询资料失败: ${response.status}`)
      }
      const result = await response.json()
      if (result.code === 0 || result.code === 200) {
        dataList.value = result.data || []
      } else {
        // 检查是否是AI未配置的错误
        const errorMsg = result.msg || '查询资料失败'
        if (errorMsg.includes('AI') || errorMsg.includes('API') || errorMsg.includes('配置')) {
          throw new Error('请前往系统设置->AI服务配置中完成配置')
        }
        throw new Error(errorMsg)
      }
    } catch (error: any) {
      console.error('查询资料失败:', error)
      // 如果错误消息包含配置相关提示，使用友好提示
      const errorMsg = error.message || '查询资料失败'
      if (errorMsg.includes('配置') || errorMsg.includes('AI') || errorMsg.includes('API')) {
        showError('请前往系统设置->AI服务配置中完成配置')
      } else {
        showError(errorMsg)
      }
      dataList.value = []
    } finally {
      dataLoading.value = false
    }
  }

  // Delete knowledge data
  const handleDeleteData = (documentId: string) => {
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
              throw new Error('请前往系统设置->AI服务配置中完成配置')
            }
            throw new Error(`删除资料失败: ${response.status}`)
          }
          const result = await response.json()
          if (result.code === 0 || result.code === 200) {
            showSuccess('资料删除成功')
            // 从列表中移除已删除项
            dataList.value = dataList.value.filter(item => item.documentId !== documentId)
          } else {
            // 检查是否是AI未配置的错误
            const errorMsg = result.msg || '删除资料失败'
            if (errorMsg.includes('AI') || errorMsg.includes('API') || errorMsg.includes('配置')) {
              throw new Error('请前往系统设置->AI服务配置中完成配置')
            }
            throw new Error(errorMsg)
          }
        } catch (error: any) {
          console.error('删除资料失败:', error)
          // 如果错误消息包含配置相关提示，使用友好提示
          const errorMsg = error.message || '删除资料失败'
          if (errorMsg.includes('配置') || errorMsg.includes('AI') || errorMsg.includes('API')) {
            showError('请前往系统设置->AI服务配置中完成配置')
          } else {
            showError(errorMsg)
          }
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
      const response = await chatTestWithAI({
        msg: inputText,
        goodsId: selectedGoods.value.item.xyGoodId,
        accountId: selectedAccountId.value!
      })

      if (!response.ok) {
        if (response.status === 405 || response.status === 404) {
          throw new Error('请前往系统设置->AI服务配置中完成配置')
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

  const goToAutoDelivery = () => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }
    router.push({
      path: '/auto-delivery',
      query: {
        accountId: String(selectedAccountId.value),
        goodsId: selectedGoods.value.item.xyGoodId
      }
    })
  }

  // Load auto reply records
  const loadRecords = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    recordsLoading.value = true
    try {
      const response = await getAutoReplyRecords({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        pageNum: recordsPage.value,
        pageSize: recordsPageSize.value
      })
      if (response.code === 0 || response.code === 200) {
        recordsList.value = response.data?.list || []
        recordsTotal.value = response.data?.totalCount || 0
      }
    } catch (error: any) {
      console.error('加载自动回复记录失败:', error)
      recordsList.value = []
    } finally {
      recordsLoading.value = false
    }
  }

  // Toggle records panel
  const toggleRecords = () => {
    recordsVisible.value = !recordsVisible.value
    if (recordsVisible.value) {
      recordsPage.value = 1
      loadRecords()
    }
  }

  // View record detail
  const viewRecordDetail = (record: AutoReplyRecord) => {
    recordDetail.value = record
    recordDetailVisible.value = true
    contextExpanded.value = false
  }

  // Records page change
  const handleRecordsPageChange = (page: number) => {
    recordsPage.value = page
    loadRecords()
  }

  // Parse trigger context JSON
  const parseTriggerContext = (jsonStr: string | null | undefined) => {
    if (!jsonStr) return null
    try {
      return JSON.parse(jsonStr)
    } catch {
      return null
    }
  }

  // Confirm dialog actions
  const handleDialogConfirm = () => {
    confirmDialog.value.onConfirm()
  }

  const handleDialogCancel = () => {
    confirmDialog.value.visible = false
  }

  const toggleOnlyOnSale = () => {
    onlyOnSale.value = !onlyOnSale.value
    goodsCurrentPage.value = 1
    selectedGoods.value = null
    loadGoods()
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
    onlyOnSale,
    detailDialogVisible,
    selectedGoodsId,
    rightTab,
    dataContent,
    uploading,
    fixedMaterial,
    fixedMaterialSaving,
    fixedMaterialSyncing,
    fixedMaterialExpanded,
    dataList,
    dataLoading,
    dataVisible,
    chatMessages,
    chatInput,
    chatSending,
    chatListRef,
    isMobile,
    mobileView,
    confirmDialog,
    delaySeconds,
    configLoading,
    configSaving,
    recordsVisible,
    recordsList,
    recordsLoading,
    recordsTotal,
    recordsPage,
    recordsPageSize,
    recordDetailVisible,
    recordDetail,
    contextExpanded,

    // Methods
    handleAccountChange,
    selectGoods,
    toggleAutoReply,
    toggleContextOn,
    handleUploadData,
    handleQueryData,
    handleDeleteData,
    handleSendChat,
    handleChatKeydown,
    handleGoodsScroll,
    goBackToGoods,
    toggleOnlyOnSale,
    viewGoodsDetail,
    goToAutoDelivery,
    handleDialogConfirm,
    handleDialogCancel,
    formatTime,
    formatPrice,
    getStatusText,
    getStatusClass,
    checkScreenSize,
    loadConfig,
    updateDelaySeconds,
    toggleRecords,
    loadRecords,
    viewRecordDetail,
    handleRecordsPageChange,
    parseTriggerContext,
    handleSaveFixedMaterial,
    handleSyncDetailToFixedMaterial,
    toggleFixedMaterialExpanded,

    keywordRules,
    newKeyword,
    newContentText,
    newContentImage,
    toggleKeywordReply,
    toggleHumanIntervention,
    updateHumanInterventionMinutes,
    handleAddKeyword,
    handleDeleteRule,
    handleUpdateKeyword,
    handleAddContent,
    handleDeleteContent,
    replyModeTab,
    selectedKeywordRuleId,
    selectedKeywordRule,
    handleContentTextChange,
    handleContentImageUpload,
    handleContentImageDelete,
    showAddKeywordInput,
    addKeywordDialogVisible,
    addReplyDialogVisible,
    addReplyText,
    addReplyImageUrls,
    handleAddKeywordFromDialog,
    handleAddReplyFromDialog,
    handleUpdateMatchMode,
    fallbackRule,
    fallbackText,
    fallbackImageUrls,
    fallbackExpanded,
    handleSaveFallbackText,
    editKeywordDialogVisible,
    editKeywordId,
    editKeywordName,
    handleOpenEditKeyword,
    handleSaveEditKeyword,
    handleDeleteFromEditDialog
  }
}
