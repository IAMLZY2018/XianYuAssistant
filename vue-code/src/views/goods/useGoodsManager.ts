import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { getAccountList } from '@/api/account'
import {
  getGoodsList,
  refreshGoods,
  getGoodsDetail,
  updateAutoDeliveryStatus,
  updateAutoReplyStatus,
  deleteItem
} from '@/api/goods'
import { showSuccess, showError, showInfo, showConfirm } from '@/utils'
import { getGoodsStatusText, formatPrice, formatTime } from '@/utils'
import type { Account } from '@/types'
import type { GoodsItemWithConfig } from '@/api/goods'

export function useGoodsManager() {
  const router = useRouter()

  const loading = ref(false)
  const refreshing = ref(false)
  const accounts = ref<Account[]>([])
  const selectedAccountId = ref<number | null>(null)
  const statusFilter = ref<string>('')
  const goodsList = ref<GoodsItemWithConfig[]>([])
  const currentPage = ref(1)
  const pageSize = ref(20)
  const total = ref(0)

  const dialogs = reactive({
    detail: false,
    deleteConfirm: false,
    filter: false
  })

  const selectedGoodsId = ref<string>('')
  const selectedGoods = ref<GoodsItemWithConfig | null>(null)
  const deleteTarget = ref<{ id: string; title: string } | null>(null)

  // Computed
  const totalPages = computed(() => Math.ceil(total.value / pageSize.value))
  const accountName = computed(() => {
    if (!selectedAccountId.value) return ''
    const acc = accounts.value.find(a => a.id === selectedAccountId.value)
    return acc?.accountNote || acc?.unb || ''
  })

  // 加载账号列表
  const loadAccounts = async () => {
    try {
      const response = await getAccountList()
      if (response.code === 0 || response.code === 200) {
        accounts.value = response.data?.accounts || []
        if (accounts.value.length > 0 && !selectedAccountId.value) {
          selectedAccountId.value = accounts.value[0]?.id || null
          await loadGoods()
        }
      }
    } catch (error: any) {
      console.error('加载账号列表失败:', error)
    }
  }

  // 加载商品列表
  const loadGoods = async () => {
    if (!selectedAccountId.value) {
      showInfo('请先选择账号')
      return
    }

    loading.value = true
    try {
      const params: any = {
        xianyuAccountId: selectedAccountId.value,
        pageNum: currentPage.value,
        pageSize: pageSize.value
      }
      if (statusFilter.value !== '') {
        params.status = parseInt(statusFilter.value)
      }
      const response = await getGoodsList(params)
      if (response.code === 0 || response.code === 200) {
        goodsList.value = response.data?.itemsWithConfig || []
        total.value = response.data?.totalCount || 0
      }
    } catch (error: any) {
      console.error('加载商品列表失败:', error)
      goodsList.value = []
    } finally {
      loading.value = false
    }
  }

  // 刷新商品数据
  const handleRefresh = async () => {
    if (!selectedAccountId.value) {
      showInfo('请先选择账号')
      return
    }
    refreshing.value = true
    try {
      const response = await refreshGoods(selectedAccountId.value)
      if (response.code === 0 || response.code === 200) {
        if (response.data && response.data.success) {
          showSuccess('商品数据刷新成功')
          await loadGoods()
        } else {
          showError(response.data?.message || '刷新商品数据失败')
        }
      }
    } catch (error: any) {
      console.error('刷新商品数据失败:', error)
    } finally {
      refreshing.value = false
    }
  }

  // 账号变更
  const handleAccountChange = () => {
    currentPage.value = 1
    loadGoods()
  }

  // 状态筛选
  const handleStatusFilter = () => {
    currentPage.value = 1
    loadGoods()
  }

  // 分页
  const handlePageChange = (page: number) => {
    currentPage.value = page
    loadGoods()
  }

  // 查看详情
  const viewDetail = (xyGoodId: string) => {
    selectedGoodsId.value = xyGoodId
    dialogs.detail = true
  }

  // 配置自动发货
  const configAutoDelivery = (item: GoodsItemWithConfig) => {
    router.push({
      path: '/auto-delivery',
      query: {
        accountId: selectedAccountId.value?.toString(),
        goodsId: item.item.xyGoodId
      }
    })
  }

  // 切换自动发货
  const toggleAutoDelivery = async (item: GoodsItemWithConfig, value: boolean) => {
    if (!selectedAccountId.value) return
    try {
      const response = await updateAutoDeliveryStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: item.item.xyGoodId,
        xianyuAutoDeliveryOn: value ? 1 : 0
      })
      if (response.code === 0 || response.code === 200) {
        showSuccess(`自动发货${value ? '开启' : '关闭'}成功`)
        item.xianyuAutoDeliveryOn = value ? 1 : 0
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      item.xianyuAutoDeliveryOn = value ? 0 : 1
    }
  }

  // 切换自动回复
  const toggleAutoReply = async (item: GoodsItemWithConfig, value: boolean) => {
    if (!selectedAccountId.value) return
    try {
      const response = await updateAutoReplyStatus({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: item.item.xyGoodId,
        xianyuAutoReplyOn: value ? 1 : 0
      })
      if (response.code === 0 || response.code === 200) {
        showSuccess(`自动回复${value ? '开启' : '关闭'}成功`)
        item.xianyuAutoReplyOn = value ? 1 : 0
      } else {
        throw new Error(response.msg || '操作失败')
      }
    } catch (error: any) {
      console.error('操作失败:', error)
      item.xianyuAutoReplyOn = value ? 0 : 1
    }
  }

  // 删除商品
  const confirmDelete = (xyGoodId: string, title: string) => {
    deleteTarget.value = { id: xyGoodId, title }
    dialogs.deleteConfirm = true
  }

  const executeDelete = async () => {
    if (!selectedAccountId.value || !deleteTarget.value) return
    try {
      const response = await deleteItem({
        xianyuAccountId: selectedAccountId.value,
        xyGoodsId: deleteTarget.value.id
      })
      if (response.code === 0 || response.code === 200) {
        showSuccess('商品删除成功')
        dialogs.deleteConfirm = false
        deleteTarget.value = null
        await loadGoods()
      } else {
        throw new Error(response.msg || '删除失败')
      }
    } catch (error: any) {
      showError('删除失败: ' + error.message)
    }
  }

  return {
    loading,
    refreshing,
    accounts,
    selectedAccountId,
    statusFilter,
    goodsList,
    currentPage,
    pageSize,
    total,
    totalPages,
    accountName,
    dialogs,
    selectedGoodsId,
    selectedGoods,
    deleteTarget,
    loadAccounts,
    loadGoods,
    handleRefresh,
    handleAccountChange,
    handleStatusFilter,
    handlePageChange,
    viewDetail,
    configAutoDelivery,
    toggleAutoDelivery,
    toggleAutoReply,
    confirmDelete,
    executeDelete,
    // utils
    getGoodsStatusText,
    formatPrice,
    formatTime
  }
}
