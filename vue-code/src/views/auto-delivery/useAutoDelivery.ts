import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { getAccountList } from '@/api/account'
import { getGoodsList, updateAutoDeliveryStatus } from '@/api/goods'
import {
  getAutoDeliveryConfig,
  saveOrUpdateAutoDeliveryConfig,
  type AutoDeliveryConfig,
  type SaveAutoDeliveryConfigReq,
  type GetAutoDeliveryConfigReq
} from '@/api/auto-delivery-config'
import {
  getAutoDeliveryRecords,
  confirmShipment,
  triggerAutoDelivery,
  type AutoDeliveryRecordReq,
  type AutoDeliveryRecordResp,
  type ConfirmShipmentReq,
  type TriggerAutoDeliveryReq
} from '@/api/auto-delivery-record'
import { showSuccess, showError, showInfo } from '@/utils'
import {
  getKamiConfigsByAccountId,
  type KamiConfig
} from '@/api/kami-config'
import type { Account } from '@/types'
import type { GoodsItemWithConfig } from '@/api/goods'

const copyToClipboard = (text: string) => {
  navigator.clipboard.writeText(text).then(() => {
    showSuccess('已复制到剪贴板')
  }).catch(() => {
    const textarea = document.createElement('textarea')
    textarea.value = text
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    showSuccess('已复制到剪贴板')
  })
}

export function useAutoDelivery() {
  const route = useRoute()

  const loading = ref(false)
  const saving = ref(false)
  const accounts = ref<Account[]>([])
  const selectedAccountId = ref<number | null>(null)
  const goodsList = ref<GoodsItemWithConfig[]>([])
  const selectedGoods = ref<GoodsItemWithConfig | null>(null)
  const currentConfig = ref<AutoDeliveryConfig | null>(null)

  // Goods list scroll loading
  const goodsCurrentPage = ref(1)
  const goodsTotal = ref(0)
  const goodsLoading = ref(false)
  const goodsListRef = ref<HTMLElement | null>(null)

  // Goods detail dialog
  const detailDialogVisible = ref(false)
  const selectedGoodsId = ref<string>('')

  // Config form
  const configForm = ref({
    deliveryMode: 1,
    autoDeliveryContent: '',
    kamiConfigIds: '',
    kamiDeliveryTemplate: '',
    autoConfirmShipment: 0
  })

  const kamiConfigOptions = ref<KamiConfig[]>([])

  const selectedKamiConfigId = computed({
    get: () => configForm.value.kamiConfigIds || '',
    set: (val: string) => { configForm.value.kamiConfigIds = val }
  })

  // Delivery records
  const recordsLoading = ref(false)
  const deliveryRecords = ref<any[]>([])
  const recordsTotal = ref(0)
  const recordsPageNum = ref(1)
  const recordsPageSize = ref(20)

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

  // API hint panel
  const apiHintUrl = computed(() => '/api/order/list')

  const apiHintParams = computed(() => {
    const params: Record<string, any> = {
      xianyuAccountId: selectedAccountId.value || undefined,
      xyGoodsId: selectedGoods.value?.item.xyGoodId || undefined,
      orderStatus: 2,
      pageNum: 1,
      pageSize: 20
    }
    return params
  })

  const apiHintParamsJson = computed(() => JSON.stringify(apiHintParams.value, null, 2))

  const confirmShipmentUrl = computed(() => '/api/order/confirmShipment')

  const confirmShipmentParams = computed(() => {
    const params: Record<string, any> = {
      xianyuAccountId: selectedAccountId.value || undefined,
      orderId: '订单ID'
    }
    return params
  })

  const confirmShipmentParamsJson = computed(() => JSON.stringify(confirmShipmentParams.value, null, 2))

  const copyApiUrl = () => {
    copyToClipboard(apiHintUrl.value)
  }

  const copyApiParams = () => {
    copyToClipboard(apiHintParamsJson.value)
  }

  const copyConfirmShipmentUrl = () => {
    copyToClipboard(confirmShipmentUrl.value)
  }

  const copyConfirmShipmentParams = () => {
    copyToClipboard(confirmShipmentParamsJson.value)
  }

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

  // Get record status
  const getRecordStatusText = (state: number) => {
    return state === 1 ? '成功' : '失败'
  }

  const getRecordStatusClass = (state: number) => {
    return state === 1 ? 'success' : 'fail'
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
          await loadGoods()
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
          await selectGoods(goodsList.value[0]!)
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
    currentConfig.value = null
    goodsCurrentPage.value = 1
    loadGoods()
  }

  // Select goods
  const selectGoods = async (goods: GoodsItemWithConfig) => {
    selectedGoods.value = goods
    recordsPageNum.value = 1
    await loadConfig()
    await loadDeliveryRecords()
    await loadKamiConfigOptions()

    if (isMobile.value) {
      mobileView.value = 'config'
    }
  }

  const loadKamiConfigOptions = async () => {
    if (!selectedAccountId.value) return
    try {
      const res = await getKamiConfigsByAccountId(selectedAccountId.value)
      if (res.code === 200) {
        kamiConfigOptions.value = res.data || []
      }
    } catch {}
  }

  // Load config
  const loadConfig = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) return

    try {
      const req: GetAutoDeliveryConfigReq = {
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId
      }

      const response = await getAutoDeliveryConfig(req)
      if (response.code === 0 || response.code === 200) {
        currentConfig.value = response.data || null
        if (response.data) {
          configForm.value.deliveryMode = response.data.deliveryMode || 1
          configForm.value.autoDeliveryContent = response.data.autoDeliveryContent || ''
          configForm.value.kamiConfigIds = response.data.kamiConfigIds || ''
          configForm.value.kamiDeliveryTemplate = response.data.kamiDeliveryTemplate || ''
          configForm.value.autoConfirmShipment = response.data.autoConfirmShipment || 0
        } else {
          configForm.value.deliveryMode = 1
          configForm.value.autoDeliveryContent = ''
          configForm.value.kamiConfigIds = ''
          configForm.value.kamiDeliveryTemplate = ''
          configForm.value.autoConfirmShipment = 0
        }
      } else {
        throw new Error(response.msg || '获取配置失败')
      }
    } catch (error: any) {
      console.error('加载配置失败:', error)
      currentConfig.value = null
    }
  }

  // Save config
  const saveConfig = async () => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    if (configForm.value.deliveryMode === 1 && !configForm.value.autoDeliveryContent.trim()) {
      showInfo('请输入自动发货内容')
      return
    }
    if (configForm.value.deliveryMode === 2 && !configForm.value.kamiConfigIds) {
      showInfo('请绑定卡密配置')
      return
    }

    saving.value = true
    try {
      const req: SaveAutoDeliveryConfigReq = {
        xianyuAccountId: selectedAccountId.value,
        xianyuGoodsId: selectedGoods.value.item.id,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        deliveryMode: configForm.value.deliveryMode,
        autoDeliveryContent: configForm.value.autoDeliveryContent.trim(),
        kamiConfigIds: configForm.value.kamiConfigIds,
        kamiDeliveryTemplate: configForm.value.kamiDeliveryTemplate.trim(),
        autoConfirmShipment: configForm.value.autoConfirmShipment
      }

      const response = await saveOrUpdateAutoDeliveryConfig(req)
      if (response.code === 0 || response.code === 200) {
        showSuccess('保存配置成功')
        currentConfig.value = response.data || null
      } else {
        throw new Error(response.msg || '保存配置失败')
      }
    } catch (error: any) {
      console.error('保存配置失败:', error)
    } finally {
      saving.value = false
    }
  }

  // Toggle auto delivery
  const toggleAutoDelivery = async (value: boolean) => {
    if (!selectedGoods.value || !selectedAccountId.value) {
      showInfo('请先选择商品')
      return
    }

    try {
      const response = await updateAutoDeliveryStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        xianyuAutoDeliveryOn: value ? 1 : 0
      })

      if (response.code === 0 || response.code === 200) {
        showSuccess(`自动发货${value ? '开启' : '关闭'}成功`)
        if (selectedGoods.value) {
          selectedGoods.value.xianyuAutoDeliveryOn = value ? 1 : 0
        }
        const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId)
        if (goodsItem) {
          goodsItem.xianyuAutoDeliveryOn = value ? 1 : 0
        }
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      if (selectedGoods.value) {
        selectedGoods.value.xianyuAutoDeliveryOn = value ? 0 : 1
      }
    }
  }

  // Load delivery records
  const loadDeliveryRecords = async () => {
    if (!selectedAccountId.value || !selectedGoods.value) {
      deliveryRecords.value = []
      recordsTotal.value = 0
      return
    }

    recordsLoading.value = true
    try {
      const req: AutoDeliveryRecordReq = {
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: selectedGoods.value.item.xyGoodId,
        pageNum: recordsPageNum.value,
        pageSize: recordsPageSize.value
      }

      const response = await getAutoDeliveryRecords(req)
      if (response.code === 0 || response.code === 200) {
        deliveryRecords.value = response.data?.records || []
        recordsTotal.value = response.data?.total || 0
      } else {
        throw new Error(response.msg || '获取记录失败')
      }
    } catch (error: any) {
      console.error('加载自动发货记录失败:', error)
      deliveryRecords.value = []
      recordsTotal.value = 0
    } finally {
      recordsLoading.value = false
    }
  }

  // Records page change
  const handleRecordsPageChange = (page: number) => {
    recordsPageNum.value = page
    loadDeliveryRecords()
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

  // Confirm shipment
  const handleConfirmShipment = (record: any) => {
    if (!selectedAccountId.value) {
      showInfo('请先选择账号')
      return
    }
    if (!record.orderId) {
      showError('该记录没有订单ID，无法确认已发货')
      return
    }

    confirmDialog.value = {
      visible: true,
      title: '确认已发货',
      message: `确定要确认已发货吗？订单ID: ${record.orderId}`,
      type: 'primary',
      onConfirm: async () => {
        try {
          const req: ConfirmShipmentReq = {
            xianyuAccountId: selectedAccountId.value!,
            orderId: record.orderId
          }
          const response = await confirmShipment(req)
          if (response.code === 0 || response.code === 200) {
            showSuccess(response.data || '确认已发货成功')
            await loadDeliveryRecords()
          } else {
            if (response.msg && (response.msg.includes('Token') || response.msg.includes('令牌'))) {
              throw new Error('Cookie已过期，请重新扫码登录获取新的Cookie')
            }
            throw new Error(response.msg || '确认已发货失败')
          }
        } catch (error: any) {
          console.error('确认已发货失败:', error)
          showError(error.message || '确认已发货失败')
        } finally {
          confirmDialog.value.visible = false
        }
      }
    }
  }

  // Trigger auto delivery
  const handleTriggerDelivery = (record: any) => {
    if (!selectedAccountId.value || !selectedGoods.value) {
      showInfo('请先选择账号和商品')
      return
    }
    if (!record.orderId) {
      showError('该记录没有订单ID，无法触发发货')
      return
    }
    if (!configForm.value.autoDeliveryContent || !configForm.value.autoDeliveryContent.trim()) {
      showError('请配置发货内容！')
      return
    }

    confirmDialog.value = {
      visible: true,
      title: '触发发货',
      message: `确认触发自动发货流程吗？订单ID: ${record.orderId}`,
      type: 'danger',
      onConfirm: async () => {
        try {
          const req: TriggerAutoDeliveryReq = {
            xianyuAccountId: selectedAccountId.value!,
            xyGoodsId: selectedGoods.value!.item.xyGoodId,
            orderId: record.orderId
          }
          const response = await triggerAutoDelivery(req)
          if (response.code === 0 || response.code === 200) {
            showSuccess(response.data || '触发发货成功')
            await loadDeliveryRecords()
          } else {
            throw new Error(response.msg || '触发发货失败')
          }
        } catch (error: any) {
          console.error('触发发货失败:', error)
          showError(error.message || '触发发货失败')
        } finally {
          confirmDialog.value.visible = false
        }
      }
    }
  }

  // Confirm dialog actions
  const handleDialogConfirm = () => {
    confirmDialog.value.onConfirm()
  }

  const handleDialogCancel = () => {
    confirmDialog.value.visible = false
  }

  // Records total pages
  const recordsTotalPages = computed(() => Math.ceil(recordsTotal.value / recordsPageSize.value))

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
    loading,
    saving,
    accounts,
    selectedAccountId,
    goodsList,
    selectedGoods,
    currentConfig,
    configForm,
    goodsCurrentPage,
    goodsTotal,
    goodsLoading,
    goodsListRef,
    detailDialogVisible,
    selectedGoodsId,
    deliveryRecords,
    recordsLoading,
    recordsTotal,
    recordsPageNum,
    recordsPageSize,
    recordsTotalPages,
    isMobile,
    mobileView,
    confirmDialog,
    apiHintUrl,
    apiHintParams,
    apiHintParamsJson,
    confirmShipmentUrl,
    confirmShipmentParams,
    confirmShipmentParamsJson,
    kamiConfigOptions,
    selectedKamiConfigId,

    // Methods
    loadAccounts,
    loadGoods,
    handleAccountChange,
    selectGoods,
    saveConfig,
    toggleAutoDelivery,
    loadDeliveryRecords,
    handleRecordsPageChange,
    viewGoodsDetail,
    handleConfirmShipment,
    handleTriggerDelivery,
    handleDialogConfirm,
    handleDialogCancel,
    copyApiUrl,
    copyApiParams,
    copyConfirmShipmentUrl,
    copyConfirmShipmentParams,
    handleGoodsScroll,
    goBackToGoods,
    formatTime,
    formatPrice,
    getStatusText,
    getStatusClass,
    getRecordStatusText,
    getRecordStatusClass,
    checkScreenSize
  }
}


