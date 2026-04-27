<script setup lang="ts">
import { useOperationLog } from './useOperationLog'
import './operation-log.css'

import IconLog from '@/components/icons/IconLog.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'
import IconClock from '@/components/icons/IconClock.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconFilter from '@/components/icons/IconFilter.vue'
import IconEmpty from '@/components/icons/IconEmpty.vue'
import IconInfo from '@/components/icons/IconInfo.vue'

const {
  loading,
  accounts,
  selectedAccountId,
  logs,
  total,
  page,
  pageSize,
  totalPages,
  filterType,
  filterModule,
  filterStatus,
  isMobile,
  mobileView,
  selectedAccountForMobile,
  detailDialogVisible,
  detailLog,
  operationTypes,
  operationModules,
  operationStatuses,
  selectAccount,
  handleFilter,
  handleResetFilter,
  handlePageChange,
  handleRefresh,
  viewDetail,
  closeDetail,
  goBackToAccounts,
  getAccountAvatar,
  getAccountName,
  getOperationTypeText,
  getOperationTypeClass,
  getStatusText,
  getStatusClass,
  formatTime,
  formatDuration,
  handleAccountSelectChange
} = useOperationLog()
</script>

<template>
  <div class="ol">
    <!-- Header -->
    <div class="ol__header">
      <div class="ol__title-row">
        <div class="ol__title-icon">
          <IconLog />
        </div>
        <h1 class="ol__title">操作记录</h1>
      </div>
      <div class="ol__actions">
        <!-- Account Select -->
        <div class="ol__header-select-wrap">
          <select
            v-model="selectedAccountId"
            class="ol__header-select"
            @change="handleAccountSelectChange"
          >
            <option :value="null" disabled>选择账号</option>
            <option
              v-for="account in accounts"
              :key="account.id"
              :value="account.id"
            >
              {{ getAccountName(account) }}
            </option>
          </select>
          <span class="ol__select-icon">
            <IconChevronDown />
          </span>
        </div>
        <button class="btn btn--secondary" @click="handleRefresh">
          <IconRefresh />
          <span class="mobile-hidden">刷新</span>
        </button>
      </div>
    </div>

    <!-- Body -->
    <div class="ol__body">
      <!-- Mobile: Account Panel -->
      <div
        v-if="isMobile"
        class="ol__account-panel"
        :class="{ 'ol__account-panel--hidden': mobileView === 'logs' }"
      >
        <div class="ol__account-toolbar">
          <span class="ol__account-toolbar-title">闲鱼账号</span>
          <span v-if="accounts.length > 0" class="ol__account-toolbar-count">共 {{ accounts.length }} 个</span>
        </div>

        <!-- Loading -->
        <div v-if="loading && accounts.length === 0" class="ol__loading">
          <div class="ol__spinner"></div>
          <span>加载中...</span>
        </div>

        <!-- Mobile: Account List -->
        <div v-else class="ol__account-list">
          <div
            v-for="account in accounts"
            :key="account.id"
            class="ol__account-item"
            :class="{ 'ol__account-item--active': selectedAccountId === account.id }"
            @click="selectAccount(account.id, account)"
          >
            <div class="ol__account-avatar">{{ getAccountAvatar(account) }}</div>
            <div class="ol__account-info">
              <div class="ol__account-name">{{ getAccountName(account) }}</div>
              <div class="ol__account-id">ID: {{ account.id }}</div>
            </div>
          </div>

          <!-- Empty -->
          <div v-if="accounts.length === 0" class="ol__empty">
            <IconEmpty />
            <span class="ol__empty-text">暂无账号数据</span>
          </div>
        </div>
      </div>

      <!-- Logs Panel -->
      <div
        class="ol__logs-panel"
        :class="{ 'ol__logs-panel--hidden': isMobile && mobileView === 'accounts' }"
      >
        <!-- Mobile Header -->
        <div v-if="isMobile" class="ol__logs-toolbar">
          <button class="ol__back-btn" @click="goBackToAccounts">
            <IconChevronLeft />
            返回
          </button>
          <div v-if="selectedAccountForMobile" style="display:flex;align-items:center;gap:6px;">
            <div class="ol__account-avatar" style="width:28px;height:28px;font-size:13px;">
              {{ getAccountAvatar(selectedAccountForMobile) }}
            </div>
            <span style="font-size:13px;font-weight:500;color:var(--d-text-primary);max-width:120px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;">
              {{ getAccountName(selectedAccountForMobile) }}
            </span>
          </div>
        </div>

        <!-- Desktop Header -->
        <template v-if="!isMobile">
          <div v-if="!selectedAccountId" class="ol__empty" style="border:none;">
            <IconInfo />
            <span class="ol__empty-text">请选择一个账号查看操作记录</span>
          </div>
        </template>

        <template v-if="selectedAccountId">
          <!-- Filter Bar -->
          <div class="ol__filter-bar">
            <div class="ol__select-wrap">
              <select v-model="filterType" class="ol__select">
                <option
                  v-for="item in operationTypes"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </option>
              </select>
              <span class="ol__select-icon">
                <IconChevronDown />
              </span>
            </div>

            <div class="ol__select-wrap mobile-hidden">
              <select v-model="filterModule" class="ol__select">
                <option
                  v-for="item in operationModules"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </option>
              </select>
              <span class="ol__select-icon">
                <IconChevronDown />
              </span>
            </div>

            <div class="ol__select-wrap">
              <select v-model="filterStatus" class="ol__select" style="min-width:100px;">
                <option
                  v-for="item in operationStatuses"
                  :key="item.value"
                  :value="item.value"
                >
                  {{ item.label }}
                </option>
              </select>
              <span class="ol__select-icon">
                <IconChevronDown />
              </span>
            </div>

            <button class="btn btn--primary btn--sm" @click="handleFilter">
              <IconFilter />
              筛选
            </button>
            <button class="btn btn--secondary btn--sm" @click="handleResetFilter">
              重置
            </button>
          </div>

          <!-- Desktop Logs Table -->
          <div v-if="!isMobile" class="ol__logs-content">
            <!-- Loading -->
            <div v-if="loading" class="ol__loading">
              <div class="ol__spinner"></div>
              <span>加载中...</span>
            </div>

            <!-- Table -->
            <table v-if="!loading && logs.length > 0" class="ol__logs-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>操作类型</th>
                  <th class="mobile-hidden">模块</th>
                  <th>操作描述</th>
                  <th>状态</th>
                  <th class="mobile-hidden">耗时</th>
                  <th>时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in logs" :key="log.id">
                  <td style="font-size:12px;color:var(--d-text-tertiary);">{{ log.id }}</td>
                  <td>
                    <span class="ol__log-type" :class="`ol__log-type--${getOperationTypeClass(log.operationType)}`">
                      {{ getOperationTypeText(log.operationType) }}
                    </span>
                  </td>
                  <td class="mobile-hidden">{{ log.operationModule || '-' }}</td>
                  <td>
                    <span class="ol__log-desc" :title="log.operationDesc">{{ log.operationDesc || '-' }}</span>
                  </td>
                  <td>
                    <span class="ol__log-status" :class="`ol__log-status--${getStatusClass(log.operationStatus)}`">
                      {{ getStatusText(log.operationStatus) }}
                    </span>
                  </td>
                  <td class="mobile-hidden">
                    <span class="ol__log-duration">{{ formatDuration(log.durationMs) }}</span>
                  </td>
                  <td>
                    <span class="ol__log-time">{{ formatTime(log.createTime) }}</span>
                  </td>
                  <td>
                    <button class="ol__log-action-btn" @click="viewDetail(log)">
                      详情
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>

            <!-- Empty -->
            <div v-if="!loading && logs.length === 0" class="ol__empty">
              <IconEmpty />
              <span class="ol__empty-text">暂无操作记录</span>
            </div>
          </div>

          <!-- Mobile Logs List -->
          <div v-if="isMobile" class="ol__logs-content">
            <!-- Loading -->
            <div v-if="loading" class="ol__loading">
              <div class="ol__spinner"></div>
              <span>加载中...</span>
            </div>

            <!-- Log Cards -->
            <template v-if="!loading">
              <div
                v-for="log in logs"
                :key="log.id"
                class="ol__log-card"
                @click="viewDetail(log)"
              >
                <div class="ol__log-card-header">
                  <span class="ol__log-type" :class="`ol__log-type--${getOperationTypeClass(log.operationType)}`">
                    {{ getOperationTypeText(log.operationType) }}
                  </span>
                  <span class="ol__log-status" :class="`ol__log-status--${getStatusClass(log.operationStatus)}`">
                    {{ getStatusText(log.operationStatus) }}
                  </span>
                </div>
                <div class="ol__log-card-desc">{{ log.operationDesc || '-' }}</div>
                <div class="ol__log-card-meta">
                  <span class="ol__log-card-meta-item">
                    <IconClock />
                    {{ formatTime(log.createTime) }}
                  </span>
                  <span v-if="log.durationMs" class="ol__log-card-meta-item">
                    {{ formatDuration(log.durationMs) }}
                  </span>
                </div>
              </div>

              <!-- Empty -->
              <div v-if="logs.length === 0" class="ol__empty">
                <IconEmpty />
                <span class="ol__empty-text">暂无操作记录</span>
              </div>
            </template>
          </div>

          <!-- Pagination -->
          <div v-if="totalPages > 1" class="ol__pagination">
            <button
              class="ol__page-btn"
              :class="{ 'ol__page-btn--disabled': page <= 1 }"
              @click="handlePageChange(page - 1)"
            >
              <IconChevronLeft />
            </button>

            <template v-for="p in (() => {
              const btns: number[] = []
              const max = 5
              let start = Math.max(1, page - Math.floor(max / 2))
              const end = Math.min(totalPages, start + max - 1)
              start = Math.max(1, end - max + 1)
              for (let i = start; i <= end; i++) btns.push(i)
              return btns
            })()" :key="p">
              <button
                class="ol__page-btn"
                :class="{ 'ol__page-btn--active': p === page }"
                @click="handlePageChange(p)"
              >
                {{ p }}
              </button>
            </template>

            <button
              class="ol__page-btn"
              :class="{ 'ol__page-btn--disabled': page >= totalPages }"
              @click="handlePageChange(page + 1)"
            >
              <IconChevronRight />
            </button>

            <span class="ol__page-info">{{ page }} / {{ totalPages }}</span>
          </div>
        </template>
      </div>
    </div>

    <!-- Detail Dialog -->
    <Transition name="overlay-fade">
      <div
        v-if="detailDialogVisible && detailLog"
        class="ol__dialog-overlay"
        @click.self="closeDetail"
      >
        <div class="ol__dialog">
          <div class="ol__dialog-header">
            <h3 class="ol__dialog-title">操作详情</h3>
            <button class="ol__dialog-close" @click="closeDetail">
              <svg xmlns="http://www.w3.org/2000/svg" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
                <line x1="18" y1="6" x2="6" y2="18" />
                <line x1="6" y1="6" x2="18" y2="18" />
              </svg>
            </button>
          </div>
          <div class="ol__dialog-body">
            <div class="ol__detail-row">
              <span class="ol__detail-label">操作类型</span>
              <span class="ol__detail-value">
                <span class="ol__log-type" :class="`ol__log-type--${getOperationTypeClass(detailLog.operationType)}`">
                  {{ getOperationTypeText(detailLog.operationType) }}
                </span>
              </span>
            </div>
            <div class="ol__detail-row">
              <span class="ol__detail-label">操作描述</span>
              <span class="ol__detail-value">{{ detailLog.operationDesc || '-' }}</span>
            </div>
            <div class="ol__detail-row">
              <span class="ol__detail-label">状态</span>
              <span class="ol__detail-value">
                <span class="ol__log-status" :class="`ol__log-status--${getStatusClass(detailLog.operationStatus)}`">
                  {{ getStatusText(detailLog.operationStatus) }}
                </span>
              </span>
            </div>
            <div v-if="detailLog.operationModule" class="ol__detail-row">
              <span class="ol__detail-label">模块</span>
              <span class="ol__detail-value">{{ detailLog.operationModule }}</span>
            </div>
            <div v-if="detailLog.targetType" class="ol__detail-row">
              <span class="ol__detail-label">目标类型</span>
              <span class="ol__detail-value">{{ detailLog.targetType }}</span>
            </div>
            <div v-if="detailLog.targetId" class="ol__detail-row">
              <span class="ol__detail-label">目标ID</span>
              <span class="ol__detail-value">{{ detailLog.targetId }}</span>
            </div>
            <div v-if="detailLog.durationMs" class="ol__detail-row">
              <span class="ol__detail-label">耗时</span>
              <span class="ol__detail-value" style="font-family:'SF Mono','Menlo',monospace;">{{ formatDuration(detailLog.durationMs) }}</span>
            </div>
            <div class="ol__detail-row">
              <span class="ol__detail-label">时间</span>
              <span class="ol__detail-value">{{ formatTime(detailLog.createTime) }}</span>
            </div>
            <div v-if="detailLog.requestParams" class="ol__detail-row" style="flex-direction:column;gap:6px;">
              <span class="ol__detail-label">请求参数</span>
              <pre class="ol__detail-pre">{{ detailLog.requestParams }}</pre>
            </div>
            <div v-if="detailLog.responseResult" class="ol__detail-row" style="flex-direction:column;gap:6px;">
              <span class="ol__detail-label">响应结果</span>
              <pre class="ol__detail-pre">{{ detailLog.responseResult }}</pre>
            </div>
            <div v-if="detailLog.errorMessage" class="ol__detail-row" style="flex-direction:column;gap:6px;">
              <span class="ol__detail-label">错误信息</span>
              <pre class="ol__detail-pre ol__detail-pre--error">{{ detailLog.errorMessage }}</pre>
            </div>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}
</style>
