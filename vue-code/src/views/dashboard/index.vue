<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { useDashboard } from './useDashboard';
import { getDataPanelStats, getDataPanelTrend } from '@/api/data-panel';
import type { DataPanelStats, DataPanelTrend } from '@/api/data-panel';

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
  return d.toISOString().slice(0, 10)
}
const getToday = () => new Date().toISOString().slice(0, 10)

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
const generatePath = (data: number[], allData: number[][]) => {
  if (!data.length) return ''
  const maxVal = Math.max(...allData.flat(), 1)
  const n = data.length
  const xStep = 280 / (n - 1 || 1)
  return data.map((v, i) => {
    const x = 30 + i * xStep
    const y = 150 - (v / maxVal) * 140
    return `${i === 0 ? 'M' : 'L'}${x},${y}`
  }).join(' ')
}

const getLineX = (i: number, total: number) => 30 + i * (280 / (total - 1 || 1))
const getLineY = (v: number, maxVal: number) => 150 - (v / Math.max(maxVal, 1)) * 140
const getLineMax = (arrs: number[][]) => Math.max(...arrs.flat(), 1)

// === Tab 切换 ===
const activeTab = ref<'guide' | 'data'>('guide')

const switchTab = (tab: 'guide' | 'data') => {
  activeTab.value = tab
  if (tab === 'data') {
    loadDataPanel().then(() => startRealtimeRefresh())
  } else {
    stopRealtimeRefresh()
  }
}

onMounted(() => {
  loadDataPanel().then(() => {
    if (dataPanelStats.value.hasData) {
      activeTab.value = 'data'
      startRealtimeRefresh()
    }
  })
})

onUnmounted(() => {
  stopRealtimeRefresh()
})
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
            <div v-if="deliveryChartMode === 'line'" class="dp__line-chart">
              <svg viewBox="0 0 320 180" class="dp__line-svg">
                <line v-for="i in 4" :key="'gh'+i" x1="30" :y1="10 + (i-1)*38" x2="310" :y2="10 + (i-1)*38" stroke="#f0f0f0" stroke-width="1" />
                <text v-for="(d, i) in trendData.dates" :key="'lx'+i" :x="getLineX(i, trendData.dates.length)" y="170" text-anchor="middle" fill="#86868b" font-size="10">{{ d }}</text>
                <path :d="generatePath(trendData.deliverySuccess, [trendData.deliverySuccess, trendData.deliveryFail])" fill="none" stroke="#34c759" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <path :d="generatePath(trendData.deliveryFail, [trendData.deliverySuccess, trendData.deliveryFail])" fill="none" stroke="#ff3b30" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <circle v-for="(v, i) in trendData.deliverySuccess" :key="'sc'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, getLineMax([trendData.deliverySuccess, trendData.deliveryFail]))" r="3" fill="#34c759" />
                <circle v-for="(v, i) in trendData.deliveryFail" :key="'fc'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, getLineMax([trendData.deliverySuccess, trendData.deliveryFail]))" r="3" fill="#ff3b30" />
              </svg>
              <div class="dp__legend">
                <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--green"></span>发货成功</span>
                <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--red"></span>发货失败</span>
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
            <div v-if="aiChartMode === 'line'" class="dp__line-chart">
              <svg viewBox="0 0 320 180" class="dp__line-svg">
                <line v-for="i in 4" :key="'gh2'+i" x1="30" :y1="10 + (i-1)*38" x2="310" :y2="10 + (i-1)*38" stroke="#f0f0f0" stroke-width="1" />
                <text v-for="(d, i) in trendData.dates" :key="'lx2'+i" :x="getLineX(i, trendData.dates.length)" y="170" text-anchor="middle" fill="#86868b" font-size="10">{{ d }}</text>
                <path :d="generatePath(trendData.aiReplies, [trendData.aiReplies])" fill="none" stroke="#5856d6" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
                <circle v-for="(v, i) in trendData.aiReplies" :key="'ac'+i" :cx="getLineX(i, trendData.dates.length)" :cy="getLineY(v, getLineMax([trendData.aiReplies]))" r="3" fill="#5856d6" />
              </svg>
              <div class="dp__legend">
                <span class="dp__legend-item"><span class="dp__legend-dot dp__legend-dot--purple"></span>AI回复数</span>
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
}

@media (min-width: 769px) and (max-width: 1024px) {
  .dp__cards { grid-template-columns: repeat(2, 1fr); }
  .dp__charts { grid-template-columns: 1fr; }
}
</style>
