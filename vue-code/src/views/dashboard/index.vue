<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useDashboard } from './useDashboard';
import { getDataPanelStats, getDataPanelTrend, getRealtimeRevenue, getSalesRevenue } from '@/api/data-panel';
import type { DataPanelStats, DataPanelTrend, SalesRevenueData } from '@/api/data-panel';

import IconAccount from '@/components/icons/IconAccount.vue';
import IconPackage from '@/components/icons/IconPackage.vue';
import IconFire from '@/components/icons/IconFire.vue';
import IconLink from '@/components/icons/IconLink.vue';
import IconRobot from '@/components/icons/IconRobot.vue';
import IconChat from '@/components/icons/IconChat.vue';
import IconChart from '@/components/icons/IconChart.vue';
import IconRefresh from '@/components/icons/IconRefresh.vue';
import IconLog from '@/components/icons/IconLog.vue';
import IconRocket from '@/components/icons/IconRocket.vue';
import IconHelp from '@/components/icons/IconHelp.vue';
import IconInfo from '@/components/icons/IconInfo.vue';
import IconArrowRight from '@/components/icons/IconArrowRight.vue';
import IconChevronDown from '@/components/icons/IconChevronDown.vue';
import IconUsers from '@/components/icons/IconUsers.vue';
import IconSend from '@/components/icons/IconSend.vue';
import IconShield from '@/components/icons/IconShield.vue';
import IconSparkle from '@/components/icons/IconSparkle.vue';
import IconCheck from '@/components/icons/IconCheck.vue';
import IconAlert from '@/components/icons/IconAlert.vue';


const router = useRouter();
const { loading: dashboardLoading, stats, loadStatistics } = useDashboard();
loadStatistics();

const activeStep = ref(0);
const expandedFaq = ref<number | null>(null);

const toggleFaq = (index: number) => {
  expandedFaq.value = expandedFaq.value === index ? null : index;
};

const statCards = [
  { key: 'accountCount', label: '闲鱼账号', icon: IconAccount, color: '#007aff' },
  { key: 'goodsCount', label: '商品总数', icon: IconPackage, color: '#34c759' },
  { key: 'onlineGoodsCount', label: '在售商品', icon: IconFire, color: '#ff9500' }
] as const;

const quickStartSteps = [
  { title: '添加闲鱼账号', icon: IconAccount, description: '通过扫码登录添加您的闲鱼账号', route: '/accounts', details: ['点击左侧菜单"闲鱼账号"', '点击"扫码登录"按钮', '使用闲鱼APP扫描二维码', '等待登录成功，账号自动添加'] },
  { title: '启动WebSocket连接', icon: IconLink, description: '建立与闲鱼服务器的实时连接', route: '/connection', details: ['进入"连接管理"页面', '选择要连接的账号', '点击"启动连接"按钮', '等待连接成功，开始监听消息'] },
  { title: '同步商品信息', icon: IconPackage, description: '获取您的闲鱼商品列表', route: '/goods', details: ['进入"商品管理"页面', '选择已连接的账号', '点击"刷新商品"按钮', '等待商品同步完成'] },
  { title: '配置自动化功能', icon: IconRobot, description: '设置自动发货和自动回复', route: '/auto-delivery', details: ['在商品列表中找到目标商品', '开启"自动发货"或"自动回复"', '配置发货内容或回复规则', '保存配置，自动化开始工作'] }
];

const features = [
  { icon: IconUsers, title: '多账号管理', description: '支持同时管理多个闲鱼账号，轻松切换', color: '#007aff' },
  { icon: IconSend, title: '自动发货', description: '买家付款后自动发送发货信息，节省时间', color: '#34c759' },
  { icon: IconChat, title: '自动回复', description: '智能匹配关键词，自动回复买家消息', color: '#ff9500' },
  { icon: IconChart, title: '数据统计', description: '实时查看商品、订单、消息等数据统计', color: '#ff3b30' },
  { icon: IconShield, title: 'Token自动刷新', description: '智能维护登录状态，无需频繁重新登录', color: '#5856d6' },
  { icon: IconLog, title: '操作日志', description: '详细记录所有操作，方便追踪和排查', color: '#8e8e93' }
];

const faqs = [
  { question: '如何获取Cookie？', answer: '在连接管理页面，点击Cookie部分的帮助按钮，查看详细的获取步骤和示例图片。' },
  { question: 'WebSocket连接失败怎么办？', answer: '1. 检查Cookie是否有效；2. 尝试刷新Token；3. 如果提示需要滑块验证，访问 goofish.com/im 完成验证后手动更新Cookie和Token。' },
  { question: '自动发货什么时候触发？', answer: '当买家付款后，系统会自动检测到"已付款待发货"消息，并根据配置自动发送发货信息。' },
  { question: 'Token过期了怎么办？', answer: '系统会自动刷新Token（1.5-2.5小时刷新一次），也可以在连接管理页面手动刷新。' }
];

const tips = [
  '系统会自动刷新Token，保持登录状态，无需频繁重新登录',
  '建议不要频繁启动/断开连接，避免触发人机验证',
  '自动发货和自动回复需要先启动WebSocket连接才能生效',
  '所有操作都会记录在"操作日志"中，方便追踪和排查问题'
];

const navigateTo = (route: string) => {
  router.push(route);
};

// === 数据面板 ===
const dataPanelLoading = ref(false)
const dataPanelStats = ref<DataPanelStats>({
  orderCount: 0,
  deliverySuccessCount: 0,
  deliveryFailCount: 0,
  aiReplyCount: 0,
  hasData: false
})
const trendData = ref<DataPanelTrend>({ dates: [], deliverySuccess: [], deliveryFail: [], aiReplies: [] })
const deliveryChartMode = ref<'bar' | 'line'>('bar')
const aiChartMode = ref<'bar' | 'line'>('bar')

const getYesterday = () => {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}
const getToday = () => {
  const d = new Date()
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

const selectedDate = ref(getYesterday())
const dateQuick = ref<'today' | 'yesterday'>('yesterday')
let refreshTimer: ReturnType<typeof setInterval> | null = null

const loadDataPanel = async () => {
  dataPanelLoading.value = true
  try {
    const [statsRes, trendRes] = await Promise.all([
      getDataPanelStats(selectedDate.value),
      getDataPanelTrend()
    ])
    if (statsRes && (statsRes.code === 200 || statsRes.code === 0) && statsRes.data) {
      dataPanelStats.value = statsRes.data
    }
    if (trendRes && (trendRes.code === 200 || trendRes.code === 0) && trendRes.data) {
      trendData.value = trendRes.data
    }
  } catch (e) {
    console.error('加载数据面板失败:', e)
  } finally {
    dataPanelLoading.value = false
  }
}

const onDateChange = (val: string) => {
  selectedDate.value = val
  if (val === getToday()) dateQuick.value = 'today'
  else if (val === getYesterday()) dateQuick.value = 'yesterday'
  else dateQuick.value = 'today' // non-quick
  loadDataPanel()
}

const setQuickDate = (quick: 'today' | 'yesterday') => {
  dateQuick.value = quick
  selectedDate.value = quick === 'today' ? getToday() : getYesterday()
  loadDataPanel()
}

const startRealtimeRefresh = () => {
  stopRealtimeRefresh()
  if (dateQuick.value === 'today') {
    refreshTimer = setInterval(() => {
      getDataPanelStats(selectedDate.value).then(res => {
        if (res && (res.code === 200 || res.code === 0) && res.data) {
          dataPanelStats.value = res.data
        }
      }).catch(() => {})
    }, 5000)
  }
}

const stopRealtimeRefresh = () => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

const deliveryTotal = computed(() => dataPanelStats.value.deliverySuccessCount + dataPanelStats.value.deliveryFailCount)
const successRate = computed(() => {
  if (deliveryTotal.value === 0) return 0
  return Math.round(dataPanelStats.value.deliverySuccessCount / deliveryTotal.value * 100)
})

// 折线图SVG path生成
const generatePath = (data: number[], niceMax: number) => {
  if (!data.length) return ''
  const n = data.length
  const xStep = 280 / (n - 1 || 1)
  return data.map((v, i) => {
    const x = 30 + i * xStep
    const y = getLineY(v, niceMax)
    return `${i === 0 ? 'M' : 'L'}${x},${y}`
  }).join(' ')
}

const getLineX = (i: number, total: number) => 30 + i * (280 / (total - 1 || 1))
const getLineY = (v: number, niceMax: number) => 150 - (v / Math.max(niceMax, 1)) * 140
const getLineMax = (arrs: number[][]) => Math.max(...arrs.flat(), 1)

const niceScale = (maxVal: number, tickCount = 4): { niceMax: number; ticks: number[] } => {
  if (maxVal <= 0) return { niceMax: 1, ticks: [0, 0.25, 0.5, 0.75, 1] }
  const roughStep = maxVal / tickCount
  const mag = Math.pow(10, Math.floor(Math.log10(roughStep)))
  const residual = roughStep / mag
  let niceStep: number
  if (residual <= 1.5) niceStep = mag
  else if (residual <= 3) niceStep = 2 * mag
  else if (residual <= 7) niceStep = 5 * mag
  else niceStep = 10 * mag
  const niceMax = Math.ceil(maxVal / niceStep) * niceStep
  const ticks: number[] = []
  for (let v = 0; v <= niceMax + niceStep * 0.01; v += niceStep) {
    ticks.push(Math.round(v * 1e10) / 1e10)
  }
  return { niceMax, ticks }
}

const salesNiceScale = computed(() => niceScale(salesMaxVal.value))
const deliveryNiceScale = computed(() => niceScale(getLineMax([trendData.value.deliverySuccess, trendData.value.deliveryFail])))
const aiNiceScale = computed(() => niceScale(getLineMax([trendData.value.aiReplies])))

const SALES_MAX_VISIBLE_LABELS = 15
const isSalesDataLabelVisible = (i: number) => {
  const n = salesData.value.values.length
  if (n <= SALES_MAX_VISIBLE_LABELS) return true
  const step = Math.ceil(n / SALES_MAX_VISIBLE_LABELS)
  return i % step === 0
}

const MAX_VISIBLE_LINE_LABELS = 10
const isLineDataLabelVisible = (i: number, total: number) => {
  if (total <= MAX_VISIBLE_LINE_LABELS) return true
  const step = Math.ceil(total / MAX_VISIBLE_LINE_LABELS)
  return i % step === 0
}

const formatTickValue = (v: number): string => {
  if (v >= 10000) return (v / 10000).toFixed(1) + 'w'
  if (v >= 1000) return (v / 1000).toFixed(1) + 'k'
  if (Number.isInteger(v)) return v.toString()
  return v.toFixed(1)
}

// === Tab 切换 ===
const activeTab = ref<'guide' | 'data'>('guide')

// === 实时销售额 ===
const revenueValue = ref(0)
const revenueDisplay = ref('0.00')
let revenueTimer: ReturnType<typeof setInterval> | null = null

const formatRevenue = (val: number): string => {
  return val.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

const loadRevenue = async () => {
  try {
    const res = await getRealtimeRevenue()
    if (res && (res.code === 200 || res.code === 0) && res.data != null) {
      revenueValue.value = res.data
      revenueDisplay.value = formatRevenue(res.data)
    }
  } catch {
    // ignore
  }
}

const startRevenueRefresh = () => {
  stopRevenueRefresh()
  loadRevenue()
  revenueTimer = setInterval(loadRevenue, 3000)
}

const stopRevenueRefresh = () => {
  if (revenueTimer) { clearInterval(revenueTimer); revenueTimer = null }
}

const switchTab = (tab: 'guide' | 'data') => {
  activeTab.value = tab
  if (tab === 'data') {
    loadDataPanel().then(() => startRealtimeRefresh())
    startRevenueRefresh()
    loadSalesData()
  } else {
    stopRealtimeRefresh()
    stopRevenueRefresh()
  }
}

onMounted(() => {
  loadDataPanel().then(() => {
    if (dataPanelStats.value.hasData) {
      activeTab.value = 'data'
      startRealtimeRefresh()
      startRevenueRefresh()
      loadSalesData()
    }
  })
})

onUnmounted(() => {
  stopRealtimeRefresh()
  stopRevenueRefresh()
})

// === 销售额趋势图 ===
const salesDimension = ref<'day' | 'week' | 'month'>('day')
const salesData = ref<SalesRevenueData>({ labels: [], values: [] })
const salesLoading = ref(false)

const formatDateStr = (date: Date): string => {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const salesEndDate = ref(formatDateStr(new Date(new Date().getTime() - 86400000)))
const salesStartDate = ref(formatDateStr(new Date(new Date().getTime() - 14 * 86400000)))

type SalesQuickRange = '15d' | '1m' | '8w' | '6m_w' | '6m' | '1y'
const salesQuickRange = ref<SalesQuickRange>('15d')

const salesQuickOptions = computed<{ key: SalesQuickRange; label: string }[]>(() => {
  switch (salesDimension.value) {
    case 'day': return [{ key: '15d', label: '近15天' }, { key: '1m', label: '近1月' }]
    case 'week': return [{ key: '8w', label: '近8周' }, { key: '6m_w', label: '近半年' }]
    case 'month': return [{ key: '6m', label: '近半年' }, { key: '1y', label: '近1年' }]
  }
})

const getQuickRangeDays = (range: SalesQuickRange): number => {
  switch (range) {
    case '15d': return 14
    case '1m': return 30
    case '8w': return 55
    case '6m_w': return 182
    case '6m': return 182
    case '1y': return 365
  }
}

const setSalesQuickRange = (range: SalesQuickRange) => {
  salesQuickRange.value = range
  const end = new Date(new Date().getTime() - 86400000)
  const start = new Date(end.getTime() - getQuickRangeDays(range) * 86400000)
  salesStartDate.value = formatDateStr(start)
  salesEndDate.value = formatDateStr(end)
  loadSalesData()
}

const onSalesDateChange = () => {
  salesQuickRange.value = salesQuickOptions.value[0]?.key ?? '15d'
  loadSalesData()
}

const switchSalesDimension = (dim: 'day' | 'week' | 'month') => {
  salesDimension.value = dim
  const defaultRange = salesQuickOptions.value[0]?.key ?? '15d'
  salesQuickRange.value = defaultRange
  const end = new Date(new Date().getTime() - 86400000)
  const start = new Date(end.getTime() - getQuickRangeDays(defaultRange) * 86400000)
  salesStartDate.value = formatDateStr(start)
  salesEndDate.value = formatDateStr(end)
  loadSalesData()
}

const loadSalesData = async () => {
  salesLoading.value = true
  try {
    const res = await getSalesRevenue(salesDimension.value, salesStartDate.value, salesEndDate.value)
    if (res && (res.code === 200 || res.code === 0) && res.data) {
      salesData.value = res.data
    }
  } catch (e) {
    console.error('加载销售额数据失败:', e)
  } finally {
    salesLoading.value = false
  }
}

const salesMaxVal = computed(() => Math.max(...salesData.value.values, 1))
const salesTotal = computed(() => salesData.value.values.reduce((a, b) => a + b, 0))

const salesBarMaxHeight = 160
const SALES_MIN_GAP = 2
const SALES_CHART_PAD_LEFT = 30
const SALES_CHART_PAD_RIGHT = 30
const SALES_CHART_HEIGHT = 180
const SALES_CHART_BASE_WIDTH = 600

const salesXStep = computed(() => {
  const n = salesData.value.labels.length
  if (n <= 1) return 0
  const drawWidth = SALES_CHART_BASE_WIDTH - SALES_CHART_PAD_LEFT - SALES_CHART_PAD_RIGHT
  const gap = drawWidth / (n - 1)
  return gap >= SALES_MIN_GAP ? gap : SALES_MIN_GAP
})

const salesDrawWidth = computed(() => {
  const n = salesData.value.labels.length
  if (n <= 0) return SALES_CHART_BASE_WIDTH
  return SALES_CHART_PAD_LEFT + (n - 1) * salesXStep.value + SALES_CHART_PAD_RIGHT
})

const salesSvgWidth = computed(() => Math.max(SALES_CHART_BASE_WIDTH, salesDrawWidth.value))
const salesViewBox = computed(() => `0 0 ${salesSvgWidth.value} ${SALES_CHART_HEIGHT}`)
const salesNeedScroll = computed(() => salesDrawWidth.value > SALES_CHART_BASE_WIDTH)

const getSalesX = (i: number) => SALES_CHART_PAD_LEFT + i * salesXStep.value
const getSalesY = (v: number, niceMax: number) => (SALES_CHART_HEIGHT - 30) - (v / Math.max(niceMax, 1)) * (SALES_CHART_HEIGHT - 40)

const SALES_LABEL_CHAR_WIDTH = 5
const SALES_LABEL_MIN_GAP = 4

const salesLabelStep = computed(() => {
  const step = salesXStep.value
  if (step <= 0) return 1
  const maxLabelLen = Math.max(...salesData.value.labels.map(l => l.length), 1)
  const labelPx = maxLabelLen * SALES_LABEL_CHAR_WIDTH
  const needed = labelPx + SALES_LABEL_MIN_GAP
  if (step >= needed) return 1
  return Math.ceil(needed / step)
})

const isSalesLabelVisible = (i: number) => {
  const step = salesLabelStep.value
  if (step <= 1) return true
  return i % step === 0
}

const generateSalesPath = (data: number[], niceMax: number) => {
  if (!data.length) return ''
  return data.map((v, i) => {
    const x = getSalesX(i)
    const y = getSalesY(v, niceMax)
    return `${i === 0 ? 'M' : 'L'}${x},${y}`
  }).join(' ')
}

// === SVG Tooltip ===
interface ChartTooltip {
  visible: boolean
  chartId: string
  x: number
  y: number
  label: string
  lines: { color: string; text: string }[]
}

const tooltip = ref<ChartTooltip>({ visible: false, chartId: '', x: 0, y: 0, label: '', lines: [] })

const showTooltip = (e: MouseEvent, chartId: string, label: string, lines: { color: string; text: string }[]) => {
  const container = (e.currentTarget as HTMLElement).closest('.dp__chart-tooltip-zone') as HTMLElement
  if (!container) return
  const rect = container.getBoundingClientRect()
  const x = e.clientX - rect.left
  const y = e.clientY - rect.top
  tooltip.value = { visible: true, chartId, x, y, label, lines }
}

const hideTooltip = () => {
  tooltip.value = { ...tooltip.value, visible: false }
}

</script>

<template>
  <div class="dashboard">
    <!-- Tab 切换 -->
    <div class="dashboard__tabs">
      <button
        class="dashboard__tab"
        :class="{ 'dashboard__tab--active': activeTab === 'guide' }"
        @click="switchTab('guide')"
      >
        <IconRocket />
        <span>入门导航</span>
      </button>
      <button
        class="dashboard__tab"
        :class="{ 'dashboard__tab--active': activeTab === 'data' }"
        @click="switchTab('data')"
      >
        <IconChart />
        <span>数据面板</span>
      </button>
    </div>

    <!-- ====== 入门导航 ====== -->
    <template v-if="activeTab === 'guide'">
      <div :class="{ 'is-loading': dashboardLoading }">
        <!-- 统计卡片 -->
        <section class="stats-grid">
          <div v-for="card in statCards" :key="card.key" class="stat-card">
            <div class="stat-card__icon" :style="{ color: card.color }">
              <component :is="card.icon" />
            </div>
            <div class="stat-card__body">
              <span class="stat-card__value">{{ stats[card.key] }}</span>
              <span class="stat-card__label">{{ card.label }}</span>
            </div>
          </div>
        </section>

        <!-- 快速开始 -->
        <section class="section">
          <div class="section__header">
            <div class="section__icon"><IconRocket /></div>
            <div class="section__text">
              <h2 class="section__title">快速开始</h2>
              <p class="section__subtitle">4步完成系统配置，开启自动化之旅</p>
            </div>
          </div>
          <div class="steps-grid">
            <div
              v-for="(step, index) in quickStartSteps"
              :key="index"
              class="step-card"
              :class="{ 'step-card--active': activeStep === index }"
              @click="activeStep = index"
            >
              <div class="step-card__number">{{ index + 1 }}</div>
              <div class="step-card__icon"><component :is="step.icon" /></div>
              <h3 class="step-card__title">{{ step.title }}</h3>
              <p class="step-card__desc">{{ step.description }}</p>
              <ul class="step-card__details">
                <li v-for="(detail, idx) in step.details" :key="idx">{{ detail }}</li>
              </ul>
              <button class="step-card__btn" @click.stop="navigateTo(step.route)">
                <span>前往操作</span>
                <IconArrowRight />
              </button>
            </div>
          </div>
        </section>

        <!-- 功能特性 -->
        <section class="section">
          <div class="section__header">
            <div class="section__icon"><IconSparkle /></div>
            <div class="section__text">
              <h2 class="section__title">功能特性</h2>
              <p class="section__subtitle">强大的自动化功能，提升您的工作效率</p>
            </div>
          </div>
          <div class="features-grid">
            <div v-for="(feature, index) in features" :key="index" class="feature-card">
              <div class="feature-card__icon" :style="{ color: feature.color }">
                <component :is="feature.icon" />
              </div>
              <h3 class="feature-card__title">{{ feature.title }}</h3>
              <p class="feature-card__desc">{{ feature.description }}</p>
            </div>
          </div>
        </section>

        <!-- 常见问题 -->
        <section class="section">
          <div class="section__header">
            <div class="section__icon"><IconHelp /></div>
            <div class="section__text">
              <h2 class="section__title">常见问题</h2>
              <p class="section__subtitle">快速解答您的疑问</p>
            </div>
          </div>
          <div class="faq-list">
            <div
              v-for="(faq, index) in faqs"
              :key="index"
              class="faq-item"
              :class="{ 'faq-item--expanded': expandedFaq === index }"
            >
              <button class="faq-item__trigger" @click="toggleFaq(index)">
                <span class="faq-item__question">{{ faq.question }}</span>
                <div class="faq-item__chevron"><IconChevronDown /></div>
              </button>
              <div class="faq-item__body">
                <p class="faq-item__answer">{{ faq.answer }}</p>
              </div>
            </div>
          </div>
        </section>

        <!-- 温馨提示 -->
        <section class="section section--tips">
          <div class="section__header">
            <div class="section__icon section__icon--info"><IconInfo /></div>
            <div class="section__text">
              <h2 class="section__title">温馨提示</h2>
            </div>
          </div>
          <ul class="tips-list">
            <li v-for="(tip, index) in tips" :key="index">{{ tip }}</li>
          </ul>
        </section>
      </div>
    </template>

    <!-- ====== 数据面板 ====== -->
    <template v-if="activeTab === 'data'">
      <div class="dp" v-loading="dataPanelLoading">
        <div class="dp__header">
          <h1 class="dp__title">数据面板</h1>
          <div class="dp__date-bar">
            <button
              class="dp__quick-btn"
              :class="{ 'dp__quick-btn--active': dateQuick === 'yesterday' }"
              @click="setQuickDate('yesterday')"
            >昨日</button>
            <button
              class="dp__quick-btn"
              :class="{ 'dp__quick-btn--active': dateQuick === 'today' }"
              @click="setQuickDate('today')"
            >
              今日
              <span v-if="dateQuick === 'today'" class="dp__live-dot"></span>
            </button>
            <el-date-picker
              v-model="selectedDate"
              type="date"
              value-format="YYYY-MM-DD"
              :clearable="false"
              size="small"
              class="dp__date-picker"
              @change="onDateChange"
            />
          </div>
        </div>

        <div class="dp__cards">
          <div class="dp__card">
            <div class="dp__card-icon dp__card-icon--blue"><IconPackage /></div>
            <div class="dp__card-body">
              <div class="dp__card-value">{{ dataPanelStats.orderCount }}</div>
              <div class="dp__card-label">订单数</div>
            </div>
          </div>
          <div class="dp__card">
            <div class="dp__card-icon dp__card-icon--green"><IconCheck /></div>
            <div class="dp__card-body">
              <div class="dp__card-value">{{ dataPanelStats.deliverySuccessCount }}</div>
              <div class="dp__card-label">发货成功</div>
            </div>
          </div>
          <div class="dp__card">
            <div class="dp__card-icon dp__card-icon--red"><IconAlert /></div>
            <div class="dp__card-body">
              <div class="dp__card-value">{{ dataPanelStats.deliveryFailCount }}</div>
              <div class="dp__card-label">发货失败</div>
            </div>
          </div>
          <div class="dp__card">
            <div class="dp__card-icon dp__card-icon--purple"><IconRobot /></div>
            <div class="dp__card-body">
              <div class="dp__card-value">{{ dataPanelStats.aiReplyCount }}</div>
              <div class="dp__card-label">AI回复</div>
            </div>
          </div>
        </div>

        <!-- 实时销售额 -->
        <div class="dp__revenue">
          <div class="dp__revenue-inner">
            <div class="dp__revenue-header">
              <div class="dp__revenue-label">
                <IconFire />
                <span>本助手为你打理销售额：</span>
              </div>
              <div class="dp__revenue-tip-wrap">
                <IconHelp class="dp__revenue-tip-icon" />
                <div class="dp__revenue-tooltip">发货成功 + 确认发货的订单金额总和，每3秒刷新</div>
              </div>
            </div>
            <div class="dp__revenue-value">
              <span class="dp__revenue-symbol">¥</span>
              <span class="dp__revenue-num">{{ revenueDisplay }}</span>
            </div>
          </div>
        </div>

        <!-- 销售额趋势图 -->
        <div class="dp__sales-chart-card" v-loading="salesLoading">
          <div class="dp__sales-header">
            <span class="dp__chart-title">销售额趋势</span>
            <div class="dp__sales-controls">
              <div class="dp__chart-toggle">
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': salesDimension === 'day' }]" @click="switchSalesDimension('day')">日</button>
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': salesDimension === 'week' }]" @click="switchSalesDimension('week')">周</button>
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': salesDimension === 'month' }]" @click="switchSalesDimension('month')">月</button>
              </div>
            </div>
          </div>

          <div class="dp__sales-date-bar">
            <div class="dp__sales-quick-bar">
              <button v-for="opt in salesQuickOptions" :key="opt.key" :class="['dp__quick-btn', { 'dp__quick-btn--active': salesQuickRange === opt.key }]" @click="setSalesQuickRange(opt.key)">{{ opt.label }}</button>
            </div>
            <el-date-picker
              v-model="salesStartDate"
              type="date"
              value-format="YYYY-MM-DD"
              :clearable="false"
              size="small"
              class="dp__date-picker"
              @change="onSalesDateChange"
            />
            <span class="dp__date-sep">~</span>
            <el-date-picker
              v-model="salesEndDate"
              type="date"
              value-format="YYYY-MM-DD"
              :clearable="false"
              size="small"
              class="dp__date-picker"
              @change="onSalesDateChange"
            />
          </div>

          <div class="dp__sales-summary">
            <span class="dp__sales-summary-label">区间销售额合计</span>
            <span class="dp__sales-summary-value">¥{{ salesTotal.toFixed(2) }}</span>
          </div>

          <!-- 折线图 -->
          <div class="dp__sales-line-chart dp__chart-tooltip-zone">
            <div class="dp__sales-chart-scroll">
              <svg :viewBox="salesViewBox" class="dp__line-svg" :style="salesNeedScroll ? { minWidth: salesDrawWidth + 'px' } : {}">
                <!-- Y轴刻度 -->
                <text v-for="(tick, ti) in salesNiceScale.ticks" :key="'sy'+ti" :x="SALES_CHART_PAD_LEFT - 4" :y="getSalesY(tick, salesNiceScale.niceMax) + 3" text-anchor="end" fill="#86868b" font-size="8" class="dp__y-tick">{{ formatTickValue(tick) }}</text>
                <line v-for="(tick, ti) in salesNiceScale.ticks" :key="'sgh'+ti" :x1="SALES_CHART_PAD_LEFT" :y1="getSalesY(tick, salesNiceScale.niceMax)" :x2="salesSvgWidth - SALES_CHART_PAD_RIGHT" :y2="getSalesY(tick, salesNiceScale.niceMax)" stroke="#f0f0f0" stroke-width="1" />
                <text v-for="(label, i) in salesData.labels" :key="'slx'+i" v-show="isSalesLabelVisible(i)" :x="getSalesX(i)" y="170" text-anchor="middle" fill="#86868b" font-size="8">{{ label }}</text>
                <path :d="generateSalesPath(salesData.values, salesNiceScale.niceMax)" fill="none" stroke="#ff9500" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <circle v-for="(v, i) in salesData.values" :key="'svc'+i" :cx="getSalesX(i)" :cy="getSalesY(v, salesNiceScale.niceMax)" r="6" fill="#ff9500" fill-opacity="0" stroke="none" class="dp__chart-hitarea" @mouseenter="(e: any) => showTooltip(e, 'sales', salesData.labels[i] || '', [{ color: '#ff9500', text: '¥' + v.toFixed(2) }])" @mouseleave="hideTooltip" />
                <circle v-for="(v, i) in salesData.values" :key="'svd'+i" :cx="getSalesX(i)" :cy="getSalesY(v, salesNiceScale.niceMax)" r="3" fill="#ff9500" pointer-events="none" />
                <text v-for="(v, i) in salesData.values" :key="'svl'+i" v-show="v > 0" :x="getSalesX(i)" :y="getSalesY(v, salesNiceScale.niceMax) - 6" text-anchor="middle" fill="#ff9500" font-size="8" class="dp__data-label">{{ v >= 100 ? formatTickValue(v) : v.toFixed(0) }}</text>
              </svg>
            </div>
            <div class="dp__legend">
              <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--orange"></span>销售额</span>
            </div>
            <div v-if="tooltip.visible && tooltip.chartId === 'sales'" class="dp__chart-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
              <div class="dp__chart-tooltip-label">{{ tooltip.label }}</div>
              <div v-for="(line, li) in tooltip.lines" :key="li" class="dp__chart-tooltip-row">
                <span class="dp__chart-tooltip-dot" :style="{ background: line.color }"></span>
                <span>{{ line.text }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="dp__charts">
          <!-- 发货情况 -->
          <div class="dp__chart-card">
            <div class="dp__chart-head">
              <span class="dp__chart-title">发货情况</span>
              <div class="dp__chart-toggle">
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': deliveryChartMode === 'bar' }]" @click="deliveryChartMode = 'bar'">柱状</button>
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': deliveryChartMode === 'line' }]" @click="deliveryChartMode = 'line'">折线</button>
              </div>
            </div>

            <!-- 柱状图 -->
            <div v-if="deliveryChartMode === 'bar'" class="dp__bar-chart">
              <div class="dp__bar-group">
                <div class="dp__bar-wrap">
                  <div class="dp__bar dp__bar--success" :style="{ height: deliveryTotal > 0 ? (dataPanelStats.deliverySuccessCount / deliveryTotal * 160) + 'px' : '0px' }"></div>
                </div>
                <span class="dp__bar-label">成功</span>
                <span class="dp__bar-count">{{ dataPanelStats.deliverySuccessCount }}</span>
              </div>
              <div class="dp__bar-group">
                <div class="dp__bar-wrap">
                  <div class="dp__bar dp__bar--fail" :style="{ height: deliveryTotal > 0 ? (dataPanelStats.deliveryFailCount / deliveryTotal * 160) + 'px' : '0px' }"></div>
                </div>
                <span class="dp__bar-label">失败</span>
                <span class="dp__bar-count">{{ dataPanelStats.deliveryFailCount }}</span>
              </div>
            </div>
            <div v-if="deliveryChartMode === 'bar'" class="dp__chart-footer">
              <span>成功率 <strong>{{ successRate }}%</strong></span>
            </div>

            <!-- 折线图 -->
            <div v-if="deliveryChartMode === 'line'" class="dp__line-chart dp__chart-tooltip-zone">
              <svg viewBox="0 0 320 180" class="dp__line-svg">
                <!-- Y轴刻度 -->
                <text v-for="(tick, ti) in deliveryNiceScale.ticks" :key="'dy'+ti" x="26" :y="getLineY(tick, deliveryNiceScale.niceMax) + 3" text-anchor="end" fill="#86868b" font-size="9" class="dp__y-tick">{{ formatTickValue(tick) }}</text>
                <line v-for="(tick, ti) in deliveryNiceScale.ticks" :key="'gh'+ti" x1="30" :y1="getLineY(tick, deliveryNiceScale.niceMax)" x2="310" :y2="getLineY(tick, deliveryNiceScale.niceMax)" stroke="#f0f0f0" stroke-width="1" />
                <text v-for="(d, i) in trendData.dates" :key="'lx'+i" :x="getLineX(i, trendData.dates.length)" y="170" text-anchor="middle" fill="#86868b" font-size="10">{{ d }}</text>
                <path :d="generatePath(trendData.deliverySuccess, deliveryNiceScale.niceMax)" fill="none" stroke="#34c759" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <path :d="generatePath(trendData.deliveryFail, deliveryNiceScale.niceMax)" fill="none" stroke="#ff3b30" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <circle v-for="(v, i) in trendData.deliverySuccess" :key="'sc'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, deliveryNiceScale.niceMax)" r="6" fill="#34c759" fill-opacity="0" stroke="none" class="dp__chart-hitarea" @mouseenter="(e: any) => showTooltip(e, 'delivery', trendData.dates[i] || '', [{ color: '#34c759', text: '成功: ' + v }, { color: '#ff3b30', text: '失败: ' + trendData.deliveryFail[i] }])" @mouseleave="hideTooltip" />
                <circle v-for="(v, i) in trendData.deliverySuccess" :key="'scd'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, deliveryNiceScale.niceMax)" r="3" fill="#34c759" pointer-events="none" />
                <circle v-for="(v, i) in trendData.deliveryFail" :key="'fc'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, deliveryNiceScale.niceMax)" r="3" fill="#ff3b30" pointer-events="none" />
                <text v-for="(v, i) in trendData.deliverySuccess" :key="'scl'+i" v-show="v > 0" :x="getLineX(i, trendData.dates.length)" :y="getLineY(v, deliveryNiceScale.niceMax) - 6" text-anchor="middle" fill="#34c759" font-size="8" class="dp__data-label">{{ v }}</text>
                <text v-for="(v, i) in trendData.deliveryFail" :key="'fcl'+i" v-show="v > 0" :x="getLineX(i, trendData.dates.length) + 12" :y="getLineY(v, deliveryNiceScale.niceMax) + 3" text-anchor="start" fill="#ff3b30" font-size="8" class="dp__data-label">{{ v }}</text>
              </svg>
              <div class="dp__legend">
                <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--green"></span>发货成功</span>
                <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--red"></span>发货失败</span>
              </div>
              <div v-if="tooltip.visible && tooltip.chartId === 'delivery'" class="dp__chart-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
                <div class="dp__chart-tooltip-label">{{ tooltip.label }}</div>
                <div v-for="(line, li) in tooltip.lines" :key="li" class="dp__chart-tooltip-row">
                  <span class="dp__chart-tooltip-dot" :style="{ background: line.color }"></span>
                  <span>{{ line.text }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- AI回复 -->
          <div class="dp__chart-card">
            <div class="dp__chart-head">
              <span class="dp__chart-title">AI自动回复</span>
              <div class="dp__chart-toggle">
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': aiChartMode === 'bar' }]" @click="aiChartMode = 'bar'">柱状</button>
                <button :class="['dp__toggle-btn', { 'dp__toggle-btn--active': aiChartMode === 'line' }]" @click="aiChartMode = 'line'">折线</button>
              </div>
            </div>

            <!-- 环形图 -->
            <div v-if="aiChartMode === 'bar'" class="dp__ring-chart">
              <svg viewBox="0 0 120 120" class="dp__ring-svg">
                <circle cx="60" cy="60" r="48" fill="none" stroke="#f0f0f0" stroke-width="10" />
                <circle cx="60" cy="60" r="48" fill="none" stroke="#5856d6" stroke-width="10" stroke-linecap="round" :stroke-dasharray="302" :stroke-dashoffset="302 - (dataPanelStats.aiReplyCount > 0 ? 302 : 0)" transform="rotate(-90 60 60)" />
              </svg>
              <div class="dp__ring-center">
                <div class="dp__ring-value">{{ dataPanelStats.aiReplyCount }}</div>
                <div class="dp__ring-label">条回复</div>
              </div>
            </div>
            <div v-if="aiChartMode === 'bar'" class="dp__chart-footer">
              <span>当日AI自动回复条数</span>
            </div>

            <!-- 折线图 -->
            <div v-if="aiChartMode === 'line'" class="dp__line-chart dp__chart-tooltip-zone">
              <svg viewBox="0 0 320 180" class="dp__line-svg">
                <!-- Y轴刻度 -->
                <text v-for="(tick, ti) in aiNiceScale.ticks" :key="'ay'+ti" x="26" :y="getLineY(tick, aiNiceScale.niceMax) + 3" text-anchor="end" fill="#86868b" font-size="9" class="dp__y-tick">{{ formatTickValue(tick) }}</text>
                <line v-for="(tick, ti) in aiNiceScale.ticks" :key="'gh2'+ti" x1="30" :y1="getLineY(tick, aiNiceScale.niceMax)" x2="310" :y2="getLineY(tick, aiNiceScale.niceMax)" stroke="#f0f0f0" stroke-width="1" />
                <text v-for="(d, i) in trendData.dates" :key="'lx2'+i" :x="getLineX(i, trendData.dates.length)" y="170" text-anchor="middle" fill="#86868b" font-size="10">{{ d }}</text>
                <path :d="generatePath(trendData.aiReplies, aiNiceScale.niceMax)" fill="none" stroke="#5856d6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <circle v-for="(v, i) in trendData.aiReplies" :key="'ac'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, aiNiceScale.niceMax)" r="6" fill="#5856d6" fill-opacity="0" stroke="none" class="dp__chart-hitarea" @mouseenter="(e: any) => showTooltip(e, 'ai', trendData.dates[i] || '', [{ color: '#5856d6', text: 'AI回复: ' + v }])" @mouseleave="hideTooltip" />
                <circle v-for="(v, i) in trendData.aiReplies" :key="'acd'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, aiNiceScale.niceMax)" r="3" fill="#5856d6" pointer-events="none" />
                <text v-for="(v, i) in trendData.aiReplies" :key="'acl'+i" v-show="v > 0" :x="getLineX(i, trendData.dates.length)" :y="getLineY(v, aiNiceScale.niceMax) - 6" text-anchor="middle" fill="#5856d6" font-size="8" class="dp__data-label">{{ v }}</text>
              </svg>
              <div class="dp__legend">
                <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--purple"></span>AI回复数</span>
              </div>
              <div v-if="tooltip.visible && tooltip.chartId === 'ai'" class="dp__chart-tooltip" :style="{ left: tooltip.x + 'px', top: tooltip.y + 'px' }">
                <div class="dp__chart-tooltip-label">{{ tooltip.label }}</div>
                <div v-for="(line, li) in tooltip.lines" :key="li" class="dp__chart-tooltip-row">
                  <span class="dp__chart-tooltip-dot" :style="{ background: line.color }"></span>
                  <span>{{ line.text }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped src="./dashboard.css"></style>

<style scoped>
.dashboard__tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 24px;
  background: #f5f5f7;
  border-radius: 10px;
  padding: 3px;
  width: fit-content;
}

.dashboard__tab {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: #86868b;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.dashboard__tab svg {
  width: 16px;
  height: 16px;
}

.dashboard__tab--active {
  background: #fff;
  color: #1a1a1a;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.dashboard__tab:hover:not(.dashboard__tab--active) {
  color: #555;
}

.dp__header { margin-bottom: 24px; display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 12px; }
.dp__title { font-size: 24px; font-weight: 700; color: #1a1a1a; margin: 0; }
.dp__subtitle { font-size: 13px; color: #86868b; margin-top: 4px; display: block; }

.dp__date-bar { display: flex; align-items: center; gap: 6px; }
.dp__quick-btn { padding: 5px 14px; border: 1px solid #d4d4d4; border-radius: 8px; background: #fff; color: #666; font-size: 13px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; gap: 4px; }
.dp__quick-btn:hover { border-color: #1a1a1a; color: #1a1a1a; }
.dp__quick-btn--active { background: #1a1a1a; color: #fff; border-color: #1a1a1a; }
.dp__live-dot { width: 6px; height: 6px; border-radius: 50%; background: #34c759; display: inline-block; animation: dp-pulse 1.5s infinite; }
@keyframes dp-pulse { 0%, 100% { opacity: 1; } 50% { opacity: 0.3; } }
.dp__date-picker { width: 140px; }

.dp__cards { display: grid; grid-template-columns: repeat(4, 1fr); gap: 16px; margin-bottom: 24px; }
.dp__card { display: flex; align-items: center; gap: 14px; padding: 20px; background: #fff; border-radius: 14px; border: 1px solid #e5e5e5; }
.dp__card-icon { width: 44px; height: 44px; border-radius: 12px; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.dp__card-icon svg { width: 22px; height: 22px; color: #fff; }
.dp__card-icon--blue { background: #007aff; }
.dp__card-icon--green { background: #34c759; }
.dp__card-icon--red { background: #ff3b30; }
.dp__card-icon--purple { background: #5856d6; }
.dp__card-value { font-size: 28px; font-weight: 700; color: #1a1a1a; line-height: 1; }
.dp__card-label { font-size: 13px; color: #86868b; margin-top: 4px; }

.dp__revenue { margin-bottom: 24px; }
.dp__revenue-inner { background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%); border-radius: 14px; padding: 28px 32px; display: flex; flex-direction: column; gap: 16px; position: relative; overflow: hidden; }
.dp__revenue-inner::before { content: ''; position: absolute; top: -50%; right: -20%; width: 300px; height: 300px; background: radial-gradient(circle, rgba(255,149,0,0.08) 0%, transparent 70%); pointer-events: none; }
.dp__revenue-header { display: flex; align-items: center; justify-content: space-between; }
.dp__revenue-label { display: flex; align-items: center; gap: 8px; color: rgba(255,255,255,0.7); font-size: 14px; font-weight: 500; }
.dp__revenue-label svg { width: 18px; height: 18px; color: #ff9500; }
.dp__revenue-tip-wrap { position: relative; cursor: pointer; }
.dp__revenue-tip-icon { width: 16px; height: 16px; color: rgba(255,255,255,0.5); transition: color 0.2s; }
.dp__revenue-tip-wrap:hover .dp__revenue-tip-icon { color: rgba(255,255,255,0.8); }
.dp__revenue-tooltip { position: absolute; top: 100%; right: 0; margin-top: 8px; background: rgba(0,0,0,0.85); color: #fff; font-size: 12px; padding: 8px 12px; border-radius: 6px; white-space: nowrap; opacity: 0; visibility: hidden; transition: opacity 0.2s, visibility 0.2s; transition-delay: 0.3s; z-index: 10; }
.dp__revenue-tip-wrap:hover .dp__revenue-tooltip { opacity: 1; visibility: visible; }
.dp__revenue-value { display: flex; align-items: baseline; gap: 4px; position: relative; z-index: 1; }
.dp__revenue-symbol { font-size: 28px; font-weight: 600; color: rgba(255,255,255,0.8); }
.dp__revenue-num { font-size: 42px; font-weight: 800; color: #fff; letter-spacing: -0.02em; font-variant-numeric: tabular-nums; transition: all 0.6s cubic-bezier(0.16, 1, 0.3, 1); }

.dp__charts { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; }
.dp__chart-card { padding: 24px; background: #fff; border-radius: 14px; border: 1px solid #e5e5e5; }
.dp__chart-title { font-size: 16px; font-weight: 600; color: #1a1a1a; }
.dp__chart-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.dp__chart-toggle { display: flex; gap: 2px; background: #f5f5f7; border-radius: 6px; padding: 2px; }
.dp__toggle-btn { padding: 3px 10px; border: none; border-radius: 5px; background: transparent; color: #86868b; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.dp__toggle-btn--active { background: #fff; color: #1a1a1a; box-shadow: 0 1px 2px rgba(0,0,0,0.06); }
.dp__toggle-btn:hover:not(.dp__toggle-btn--active) { color: #555; }
.dp__line-chart { padding: 0 4px; }
.dp__line-svg { width: 100%; height: auto; }
.dp__legend { display: flex; justify-content: center; gap: 16px; margin-top: 8px; }
.dp__legend-item { display: flex; align-items: center; gap: 4px; font-size: 12px; color: #86868b; }
.dp__legend-dot { width: 8px; height: 8px; border-radius: 50%; display: inline-block; }
.dp__legend-dot--green { background: #34c759; }
.dp__legend-dot--red { background: #ff3b30; }
.dp__legend-dot--purple { background: #5856d6; }
.dp__bar-chart { display: flex; justify-content: center; gap: 48px; padding: 0 20px; }
.dp__bar-group { display: flex; flex-direction: column; align-items: center; gap: 8px; }
.dp__bar-wrap { width: 56px; height: 160px; background: #f5f5f7; border-radius: 8px; display: flex; align-items: flex-end; overflow: hidden; }
.dp__bar { width: 100%; border-radius: 8px; transition: height 0.5s ease; min-height: 0; }
.dp__bar--success { background: #34c759; }
.dp__bar--fail { background: #ff3b30; }
.dp__bar-label { font-size: 13px; color: #86868b; }
.dp__bar-count { font-size: 18px; font-weight: 600; color: #1a1a1a; }
.dp__chart-footer { margin-top: 16px; text-align: center; font-size: 13px; color: #86868b; }
.dp__chart-footer strong { color: #1a1a1a; }
.dp__ring-chart { display: flex; justify-content: center; align-items: center; position: relative; padding: 12px 0; }
.dp__ring-svg { width: 140px; height: 140px; }
.dp__ring-center { position: absolute; text-align: center; }
.dp__ring-value { font-size: 28px; font-weight: 700; color: #1a1a1a; line-height: 1; }
.dp__ring-label { font-size: 12px; color: #86868b; margin-top: 2px; }

@media (max-width: 768px) {
  .dashboard__tabs { width: 100%; }
  .dashboard__tab { flex: 1; justify-content: center; padding: 8px 12px; font-size: 13px; }
  .dp__cards { grid-template-columns: repeat(2, 1fr); gap: 10px; }
  .dp__card { padding: 14px; gap: 10px; }
  .dp__card-icon { width: 36px; height: 36px; border-radius: 10px; }
  .dp__card-value { font-size: 22px; }
  .dp__charts { grid-template-columns: 1fr; gap: 14px; }
  .dp__chart-card { padding: 18px; }
  .dp__revenue-inner { padding: 20px; }
  .dp__revenue-num { font-size: 32px; }
  .dp__revenue-symbol { font-size: 22px; }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .dp__cards { grid-template-columns: repeat(2, 1fr); }
  .dp__charts { grid-template-columns: 1fr; }
}

/* 销售额趋势图 */
.dp__sales-chart-card { padding: 24px; background: #fff; border-radius: 14px; border: 1px solid #e5e5e5; margin-bottom: 24px; }
.dp__sales-header { display: flex; align-items: center; justify-content: space-between; flex-wrap: wrap; gap: 8px; margin-bottom: 12px; }
.dp__sales-controls { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.dp__sales-date-bar { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; margin-bottom: 16px; }
.dp__sales-quick-bar { display: flex; align-items: center; gap: 4px; }
.dp__date-sep { color: #86868b; font-size: 13px; }
.dp__sales-summary { display: flex; align-items: baseline; gap: 8px; margin-bottom: 16px; padding: 10px 14px; background: rgba(255, 149, 0, 0.06); border-radius: 8px; }
.dp__sales-summary-label { font-size: 13px; color: #86868b; }
.dp__sales-summary-value { font-size: 20px; font-weight: 700; color: #ff9500; font-variant-numeric: tabular-nums; }
.dp__export-btn { display: inline-flex; align-items: center; gap: 4px; padding: 3px 10px; border: 1px solid #d4d4d4; border-radius: 6px; background: #fff; color: #666; font-size: 12px; cursor: pointer; transition: all 0.2s; }
.dp__export-btn svg { width: 14px; height: 14px; }
.dp__export-btn:hover { border-color: #1a1a1a; color: #1a1a1a; }
.dp__sales-line-chart { padding: 0 4px; }
.dp__sales-chart-scroll { overflow-x: auto; overflow-y: hidden; scrollbar-width: none; -ms-overflow-style: none; }
.dp__sales-chart-scroll::-webkit-scrollbar { display: none; }
.dp__sales-bar-chart { padding: 0 4px; }
.dp__sales-bar-label { font-size: 10px; color: #86868b; white-space: nowrap; max-width: 60px; overflow: hidden; text-overflow: ellipsis; }
.dp__sales-bar-value { font-size: 10px; font-weight: 600; color: #1a1a1a; }
.dp__legend-dot--orange { background: #ff9500; }

@media (max-width: 768px) {
  .dp__sales-chart-card { padding: 18px; }
  .dp__sales-header { flex-direction: column; align-items: flex-start; }
  .dp__sales-controls { width: 100%; justify-content: flex-start; }
  .dp__sales-date-bar { flex-wrap: wrap; }
  .dp__sales-summary { padding: 8px 12px; }
  .dp__sales-summary-value { font-size: 18px; }
}

@media (min-width: 769px) and (max-width: 1024px) {
  .dp__sales-chart-card { padding: 20px; }
}
</style>
