import { createRouter, createWebHistory } from 'vue-router'
import { isLoggedIn } from '@/utils/request'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录', public: true }
    },
    {
      path: '/',
      redirect: '/dashboard'
    },
    {
      path: '/dashboard',
      name: 'dashboard',
      component: () => import('@/views/dashboard/index.vue'),
      meta: { title: '仪表板', icon: '📊' }
    },
    {
      path: '/accounts',
      name: 'accounts',
      component: () => import('@/views/accounts/index.vue'),
      meta: { title: '闲鱼账号', icon: '👤' }
    },
    {
      path: '/connection',
      name: 'connection',
      component: () => import('@/views/connection/index.vue'),
      meta: { title: '连接管理', icon: '🔗' }
    },
    {
      path: '/connection/:id',
      name: 'connection-detail',
      component: () => import('@/views/connection/ConnectionDetail.vue'),
      meta: { title: '连接详情', icon: '🔗' }
    },
    {
      path: '/goods',
      name: 'goods',
      component: () => import('@/views/goods/index.vue'),
      meta: { title: '商品管理', icon: '📦' }
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('@/views/orders/index.vue'),
      meta: { title: '发货记录', icon: '📋' }
    },
    {
      path: '/messages',
      name: 'messages',
      component: () => import('@/views/messages/index.vue'),
      meta: { title: '消息管理', icon: '💬' }
    },
    {
      path: '/auto-delivery',
      name: 'auto-delivery',
      component: () => import('@/views/auto-delivery/index.vue'),
      meta: { title: '自动发货', icon: '🤖' }
    },
    {
      path: '/kami-config',
      name: 'kami-config',
      component: () => import('@/views/kami-config/index.vue'),
      meta: { title: '卡密配置', icon: '🔑' }
    },
    {
      path: '/auto-reply',
      name: 'auto-reply',
      component: () => import('@/views/auto-reply/index.vue'),
      meta: { title: '自动回复', icon: '💭' }
    },
    {
      path: '/operation-log',
      name: 'operation-log',
      component: () => import('@/views/operation-log/index.vue'),
      meta: { title: '操作记录', icon: '📜' }
    },
    {
      path: '/settings',
      name: 'settings',
      component: () => import('@/views/settings/index.vue'),
      meta: { title: '系统设置', icon: '⚙️' }
    },
    {
      path: '/qrlogin',
      name: 'qrlogin',
      component: () => import('@/views/qrlogin/index.vue'),
      meta: { title: '扫码登录', icon: '📱' }
    }
  ]
})

// 路由守卫：未登录跳转登录页
router.beforeEach((to, _from, next) => {
  if (to.meta.public || isLoggedIn()) {
    next()
  } else {
    next('/login')
  }
})

export default router
