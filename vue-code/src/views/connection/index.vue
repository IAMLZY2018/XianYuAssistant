<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useConnectionManager } from './useConnectionManager'
import ConnectionCard from './components/ConnectionCard.vue'
import ConnectionDetail from './components/ConnectionDetail.vue'

import IconLink from '@/components/icons/IconLink.vue'

const router = useRouter()

const {
  loading,
  accounts,
  selectedAccountId,
  connectionStatus,
  statusLoading,
  allConnectionStatuses,
  loadAccounts,
  selectAccount,
  handleRefresh,
  getAccountName,
  getAccountAvatar,
  getCookieStatusText,
  getCookieStatusType,
  getTokenStatusText,
  getTokenStatusType,
  formatTimestamp,
  addLog,
  loadConnectionStatus
} = useConnectionManager()

// Responsive
const isMobile = ref(false)
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

// Build connection map for card display
const connectionMap = computed(() => {
  const map = new Map<number, {
    connected?: boolean
    status?: string
    cookieStatus?: number
    tokenExpireTime?: number
  }>()
  for (const [accountId, status] of allConnectionStatuses.value) {
    map.set(accountId, {
      connected: status.connected,
      status: status.status,
      cookieStatus: status.cookieStatus,
      tokenExpireTime: status.tokenExpireTime
    })
  }
  return map
})

const selectedAccountName = computed(() => {
  if (!selectedAccountId.value) return ''
  const acc = accounts.value.find(a => Number(a.id) === selectedAccountId.value)
  return acc?.accountNote || acc?.unb || ''
})

// Handle account select
const handleSelectAccount = (account: any) => {
  const id = Number(account.id)
  if (isMobile.value) {
    router.push(`/connection/${id}`)
  } else {
    selectAccount(id)
  }
}

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
})

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize)
})
</script>

<template>
  <div class="connection">
    <!-- Page Header -->
    <header class="connection__header">
      <div class="connection__title-row">
        <h1 class="connection__title mobile-hidden">连接管理</h1>
      </div>
    </header>

    <!-- Content Card -->
    <section class="connection__content">
      <div class="connection__toolbar">
        <span class="connection__list-title">
          账号列表
          <span v-if="accounts.length" class="connection__count">{{ accounts.length }}</span>
        </span>
      </div>

      <!-- Desktop: Split layout -->
      <div v-if="!isMobile" class="connection__split">
        <div class="connection__list">
          <ConnectionCard
            :accounts="accounts"
            :connections="connectionMap"
            :selected-id="selectedAccountId"
            :loading="loading"
            @select="handleSelectAccount"
          />
        </div>
        <div class="connection__detail">
          <ConnectionDetail
            :account-id="selectedAccountId"
            :account-name="selectedAccountName"
          />
        </div>
      </div>

      <!-- Mobile: List only, click to navigate -->
      <div v-else class="connection__scroll-wrap">
        <ConnectionCard
          :accounts="accounts"
          :connections="connectionMap"
          :selected-id="selectedAccountId"
          :loading="loading"
          @select="handleSelectAccount"
        />
      </div>
    </section>
  </div>
</template>

<style scoped src="./connection.css"></style>

<style scoped>
/* Split layout for desktop */
.connection__split {
  flex: 1;
  display: flex;
  min-height: 0;
  overflow: hidden;
}

.connection__list {
  flex: 0 0 320px;
  min-width: 280px;
  max-width: 400px;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.12) transparent;
}

.connection__list::-webkit-scrollbar {
  width: 6px;
}

.connection__list::-webkit-scrollbar-track {
  background: transparent;
}

.connection__list::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.12);
  border-radius: 3px;
}

.connection__detail {
  flex: 1;
  min-width: 0;
  border-left: 1px solid rgba(60,60,67,.12);
  overflow: hidden;
}

.connection__count {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 20px;
  height: 20px;
  padding: 0 6px;
  font-size: 11px;
  font-weight: 600;
  color: #fff;
  background: rgba(0, 0, 0, 0.15);
  border-radius: 10px;
  margin-left: 6px;
  vertical-align: middle;
}

@media screen and (max-width: 768px) {
  .connection__split {
    flex-direction: column;
  }

  .connection__detail {
    display: none;
  }
}

@media screen and (max-width: 1100px) {
  .connection__detail {
    width: 360px;
  }
}

@media screen and (max-width: 900px) {
  .connection__detail {
    width: 320px;
  }
}
</style>
