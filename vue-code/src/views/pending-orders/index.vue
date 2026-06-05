<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { getAccountList } from '@/api/account'
import { getPendingOrders, deliverPendingOrders, consignDummyDelivery } from '@/api/order'


const accounts = ref<any[]>([])
const selectedAccountId = ref<number | null>(null)
const pendingOrders = ref<any[]>([])
const loading = ref(false)
const delivering = ref(false)
const deliveringOrderId = ref<string | null>(null)

const autoRefresh = ref(false)
const refreshInterval = ref(10)
const refreshOptions = [5, 10, 15, 30, 60]
let refreshTimer: ReturnType<typeof setInterval> | null = null

const autoDeliver = ref(false)
let deliverTimer: ReturnType<typeof setInterval> | null = null

const lastDeliverCount = ref<number | null>(null)

onMounted(() => { loadAccounts() })
onUnmounted(() => { stopAutoRefresh(); stopAutoDeliver() })

const loadAccounts = async () => {
  try {
    const res = await getAccountList()
    if ((res.code === 200 || res.code === 0) && res.data) {
      accounts.value = res.data.accounts || res.data || []
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = Number(accounts.value[0].id)
        loadOrders()
      }
    }
  } catch (e) { console.error('加载账号列表失败:', e) }
}

const loadOrders = async () => {
  if (!selectedAccountId.value) return
  loading.value = true
  try {

    const res = await getPendingOrders(selectedAccountId.value)
    if ((res.code === 200 || res.code === 0) && res.data) {
      pendingOrders.value = res.data
    } else { pendingOrders.value = [] }
  } catch (e) { console.error('加载待发货订单失败:', e); pendingOrders.value = [] }
  finally { loading.value = false }
}

const onAccountChange = () => { loadOrders() }

const startAutoRefresh = () => {
  stopAutoRefresh()
  if (autoRefresh.value) { refreshTimer = setInterval(loadOrders, refreshInterval.value * 1000) }
}
const stopAutoRefresh = () => { if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null } }
const toggleAutoRefresh = () => { autoRefresh.value ? startAutoRefresh() : stopAutoRefresh() }
const onIntervalChange = () => { if (autoRefresh.value) startAutoRefresh() }

const handleDeliver = async () => {
  if (!selectedAccountId.value) return
  delivering.value = true
  lastDeliverCount.value = null
  try {
    const res = await deliverPendingOrders(selectedAccountId.value)
    if ((res.code === 200 || res.code === 0) && res.data != null) {
      lastDeliverCount.value = res.data
      loadOrders()
    }
  } catch (e) { console.error('自动发货失败:', e) }
  finally { delivering.value = false }
}

const startAutoDeliver = () => {
  stopAutoDeliver()
  if (autoDeliver.value) {
    handleDeliver()
    deliverTimer = setInterval(handleDeliver, refreshInterval.value * 1000)
  }
}
const stopAutoDeliver = () => { if (deliverTimer) { clearInterval(deliverTimer); deliverTimer = null } }
const toggleAutoDeliver = () => { autoDeliver.value ? startAutoDeliver() : stopAutoDeliver() }

const totalCount = computed(() => pendingOrders.value.length)
const formatTime = (t: string) => { if (!t) return ''; return t.replace(/^\d{4}-/, '').replace(/-/g, '/') }

const handleSingleDeliver = async (order: any) => {
  if (!selectedAccountId.value) return
  const xyGoodsId = order.commonData?.itemId || order.itemVO?.itemId
  const orderId = order.commonData?.orderId
  if (!orderId) return

  deliveringOrderId.value = orderId
  try {
    const res = await consignDummyDelivery({ xianyuAccountId: selectedAccountId.value, xyGoodsId: xyGoodsId || '', orderId })
    if ((res.code === 200 || res.code === 0) && res.data) {
      loadOrders()
    } else {
      alert(res.msg || '凭证发货失败')
    }
  } catch (e) { console.error('发货失败:', e) }
  finally { deliveringOrderId.value = null }
}
</script>

<template>
  <div class="po">
    <div class="po__header">
      <h1 class="po__title">待发货订单</h1>
    </div>

    <div class="po__toolbar">
      <select v-model="selectedAccountId" @change="onAccountChange" class="po__select">
        <option v-for="a in accounts" :key="a.id" :value="Number(a.id)">
          {{ a.accountNote || a.unb || ('账号#' + a.id) }}
        </option>
      </select>
      <span v-if="totalCount > 0" class="po__count">{{ totalCount }} 笔待发货</span>
    </div>

    <div class="po__controls">
      <div class="po__control-group">
        <button class="po__refresh-btn" @click="loadOrders" :disabled="loading" title="刷新">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M21.5 2v6h-6"/><path d="M2.5 22v-6h6"/><path d="M21.5 8A10 10 0 0 0 3.5 6"/><path d="M2.5 16A10 10 0 0 0 20.5 18"/></svg>
        </button>
        <label class="po__toggle">
          <input type="checkbox" v-model="autoRefresh" @change="toggleAutoRefresh" />
          <span>自动刷新</span>
        </label>
        <select v-model.number="refreshInterval" @change="onIntervalChange" class="po__interval-select" :disabled="!autoRefresh && !autoDeliver">
          <option v-for="s in refreshOptions" :key="s" :value="s">{{ s }}s</option>
        </select>
      </div>
      <div class="po__control-group">
        <label class="po__toggle po__toggle--deliver">
          <input type="checkbox" v-model="autoDeliver" @change="toggleAutoDeliver" />
          <span>自动发货</span>
        </label>

      </div>
      <div v-if="lastDeliverCount !== null" class="po__deliver-result">
        已发货 {{ lastDeliverCount }} 笔
      </div>
    </div>

    <div v-if="pendingOrders.length === 0 && !loading" class="po__empty">暂无待发货订单</div>

    <div v-else class="po__list">
      <div v-for="order in pendingOrders" :key="order.commonData?.orderId" class="po__card">
        <div class="po__card-header">
          <span class="po__order-id">{{ order.commonData?.orderId }}</span>
          <div class="po__card-header-right">
            <span class="po__status-tag">{{ order.commonData?.orderStatus }}</span>
            <button class="po__single-deliver-btn" @click="handleSingleDeliver(order)" :disabled="deliveringOrderId === order.commonData?.orderId">
              {{ deliveringOrderId === order.commonData?.orderId ? '发货中' : '发货' }}
            </button>
          </div>
        </div>
        <div class="po__card-body">
          <div class="po__field"><span class="po__label">商品</span><span class="po__value po__value--title">{{ order.itemVO?.title }}</span></div>
          <div class="po__field"><span class="po__label">买家</span><span class="po__value">{{ order.buyerInfoVO?.userNick }}</span></div>
          <div class="po__field"><span class="po__label">金额</span><span class="po__value po__value--price">¥{{ order.priceVO?.totalPrice }}</span></div>
          <div class="po__field"><span class="po__label">数量</span><span class="po__value">{{ order.priceVO?.buyNum }}</span></div>
          <div class="po__field"><span class="po__label">下单</span><span class="po__value">{{ formatTime(order.commonData?.createTime) }}</span></div>
          <div class="po__field"><span class="po__label">付款</span><span class="po__value">{{ formatTime(order.commonData?.paySuccessTime) }}</span></div>
        </div>
        <div v-if="order.commonData?.tags?.length" class="po__card-tags">
          <span v-for="tag in order.commonData.tags" :key="tag" class="po__urgency-tag">{{ tag }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.po { max-width: 960px; margin: 0 auto; padding: 24px 20px; }
.po__header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.po__title { font-size: 22px; font-weight: 700; color: var(--c-text-1, #1c1c1e); letter-spacing: -0.02em; }
.po__toolbar { display: flex; align-items: center; gap: 12px; margin-bottom: 16px; }
.po__select { padding: 8px 12px; font-size: 14px; border-radius: 10px; border: 1px solid rgba(0,0,0,.1); background: rgba(255,255,255,.7); color: var(--c-text-1, #1c1c1e); min-width: 180px; }
.po__count { font-size: 13px; font-weight: 500; color: var(--c-accent, #0A84FF); padding: 4px 10px; border-radius: 8px; background: rgba(10,132,255,.1); }
.po__controls { display: flex; align-items: center; gap: 16px; flex-wrap: wrap; margin-bottom: 20px; padding: 12px 16px; border-radius: 12px; border: 1px solid rgba(0,0,0,.06); background: rgba(255,255,255,.5); }
.po__control-group { display: flex; align-items: center; gap: 8px; }
.po__toggle { display: flex; align-items: center; gap: 6px; font-size: 13px; color: var(--c-text-2, rgba(28,28,30,.7)); cursor: pointer; user-select: none; }
.po__toggle input { width: 14px; height: 14px; }
.po__toggle--deliver span { font-weight: 600; color: #34C759; }
.po__interval-select { padding: 4px 8px; font-size: 12px; border-radius: 6px; border: 1px solid rgba(0,0,0,.1); background: rgba(255,255,255,.8); }
.po__refresh-btn { display: flex; align-items: center; justify-content: center; width: 28px; height: 28px; border-radius: 8px; border: 1px solid rgba(0,0,0,.1); background: rgba(255,255,255,.8); color: var(--c-text-2, rgba(28,28,30,.7)); cursor: pointer; transition: all .15s; }
.po__refresh-btn:hover { background: rgba(10,132,255,.1); color: #0A84FF; border-color: rgba(10,132,255,.3); }
.po__refresh-btn:disabled { opacity: .5; cursor: not-allowed; }

.po__deliver-result { font-size: 12px; font-weight: 500; color: #34C759; padding: 4px 10px; border-radius: 8px; background: rgba(52,199,89,.1); }
.po__empty { text-align: center; padding: 60px 20px; color: var(--c-text-3, rgba(28,28,30,.55)); font-size: 15px; }
.po__list { display: flex; flex-direction: column; gap: 12px; }
.po__card { border-radius: 14px; border: 1px solid rgba(0,0,0,.06); background: rgba(255,255,255,.65); backdrop-filter: blur(20px); padding: 16px; transition: box-shadow .2s; }
.po__card:hover { box-shadow: 0 4px 20px rgba(0,0,0,.08); }
.po__card-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12px; padding-bottom: 10px; border-bottom: 1px solid rgba(0,0,0,.05); }
.po__order-id { font-size: 13px; font-family: monospace; color: var(--c-text-2, rgba(28,28,30,.7)); }
.po__card-header-right { display: flex; align-items: center; gap: 8px; }
.po__single-deliver-btn { padding: 3px 10px; font-size: 12px; font-weight: 600; border-radius: 6px; border: none; background: #34C759; color: #fff; cursor: pointer; transition: all .15s; white-space: nowrap; }
.po__single-deliver-btn:hover { background: #2DB84D; }
.po__single-deliver-btn:disabled { opacity: .5; cursor: not-allowed; }
.po__status-tag { font-size: 12px; font-weight: 600; padding: 3px 10px; border-radius: 8px; background: rgba(255,149,0,.12); color: #FF9500; white-space: nowrap; }
.po__card-body { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 16px; }
.po__field { display: flex; align-items: baseline; gap: 6px; }
.po__label { font-size: 12px; color: var(--c-text-3, rgba(28,28,30,.55)); flex-shrink: 0; min-width: 32px; }
.po__value { font-size: 13px; color: var(--c-text-1, #1c1c1e); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.po__value--title { font-weight: 500; }
.po__value--price { font-weight: 600; color: #FF9500; }
.po__card-tags { margin-top: 10px; padding-top: 8px; border-top: 1px solid rgba(0,0,0,.05); display: flex; gap: 6px; }
.po__urgency-tag { font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 6px; background: rgba(255,59,48,.12); color: #FF453A; }
@media (max-width: 768px) {
  .po { padding: 16px 12px; }
  .po__title { font-size: 18px; }
  .po__card-body { grid-template-columns: 1fr; }
  .po__select { min-width: 140px; }
  .po__controls { flex-direction: column; align-items: flex-start; gap: 10px; }
}
</style>
