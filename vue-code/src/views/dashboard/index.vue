<script setup lang="ts">
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import { useDashboard } from './useDashboard';

// SVG Icons
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

const router = useRouter();
const { loading, stats, loadStatistics } = useDashboard();

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
  {
    title: '添加闲鱼账号',
    icon: IconAccount,
    description: '通过扫码登录添加您的闲鱼账号',
    route: '/accounts',
    details: [
      '点击左侧菜单"闲鱼账号"',
      '点击"扫码登录"按钮',
      '使用闲鱼APP扫描二维码',
      '等待登录成功，账号自动添加'
    ]
  },
  {
    title: '启动WebSocket连接',
    icon: IconLink,
    description: '建立与闲鱼服务器的实时连接',
    route: '/connection',
    details: [
      '进入"连接管理"页面',
      '选择要连接的账号',
      '点击"启动连接"按钮',
      '等待连接成功，开始监听消息'
    ]
  },
  {
    title: '同步商品信息',
    icon: IconPackage,
    description: '获取您的闲鱼商品列表',
    route: '/goods',
    details: [
      '进入"商品管理"页面',
      '选择已连接的账号',
      '点击"刷新商品"按钮',
      '等待商品同步完成'
    ]
  },
  {
    title: '配置自动化功能',
    icon: IconRobot,
    description: '设置自动发货和自动回复',
    route: '/auto-delivery',
    details: [
      '在商品列表中找到目标商品',
      '开启"自动发货"或"自动回复"',
      '配置发货内容或回复规则',
      '保存配置，自动化开始工作'
    ]
  }
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
  {
    question: '如何获取Cookie？',
    answer: '在连接管理页面，点击Cookie部分的帮助按钮，查看详细的获取步骤和示例图片。'
  },
  {
    question: 'WebSocket连接失败怎么办？',
    answer: '1. 检查Cookie是否有效；2. 尝试刷新Token；3. 如果提示需要滑块验证，访问 goofish.com/im 完成验证后手动更新Cookie和Token。'
  },
  {
    question: '自动发货什么时候触发？',
    answer: '当买家付款后，系统会自动检测到"已付款待发货"消息，并根据配置自动发送发货信息。'
  },
  {
    question: 'Token过期了怎么办？',
    answer: '系统会自动刷新Token（1.5-2.5小时刷新一次），也可以在连接管理页面手动刷新。'
  }
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
</script>

<template>
  <div class="dashboard" :class="{ 'is-loading': loading }">
    <!-- 统计卡片 -->
    <section class="stats-grid">
      <div
        v-for="card in statCards"
        :key="card.key"
        class="stat-card"
      >
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
          <div class="step-card__icon">
            <component :is="step.icon" />
          </div>
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
        <div
          v-for="(feature, index) in features"
          :key="index"
          class="feature-card"
        >
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
            <div class="faq-item__chevron">
              <IconChevronDown />
            </div>
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

<style scoped src="./dashboard.css"></style>
