<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useCardSecret } from './useCardSecret'
import '../orders/orders.css' // 复用订单管理的样式
import './card-secret.css'

import IconKey from '@/components/icons/IconKey.vue'
import IconSearch from '@/components/icons/IconSearch.vue'
import IconRefresh from '@/components/icons/IconRefresh.vue'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'
import IconChevronLeft from '@/components/icons/IconChevronLeft.vue'
import IconChevronRight from '@/components/icons/IconChevronRight.vue'
import IconTrash from '@/components/icons/IconTrash.vue'
import IconEdit from '@/components/icons/IconEdit.vue'
import IconPlus from '@/components/icons/IconPlus.vue'
import IconSync from '@/components/icons/IconSync.vue'

const {
  loading,
  cardSecretList,
  total,
  totalPages,
  queryParams,
  accounts,
  dialogs,
  editForm,
  importForm,
  loadAccounts,
  loadCardSecrets,
  handleReset,
  handlePageChange,
  handleAdd,
  handleEdit,
  saveEdit,
  handleImport,
  saveImport,
  handleDelete,
  handleBatchDelete,
  handleSelectionChange,
  selectedIds
} = useCardSecret()

onMounted(() => {
  loadAccounts()
  loadCardSecrets()
})

const getPageButtons = () => {
  const buttons: number[] = []
  const maxVisible = 5
  let start = Math.max(1, queryParams.pageNum - Math.floor(maxVisible / 2))
  const end = Math.min(totalPages.value, start + maxVisible - 1)
  start = Math.max(1, end - maxVisible + 1)
  for (let i = start; i <= end; i++) {
    buttons.push(i)
  }
  return buttons
}

const formatStatus = (status: number) => {
  return status === 1 ? '已使用' : '未使用'
}

const getStatusClass = (status: number) => {
  return status === 1 ? 'status--used' : 'status--unused'
}
</script>

<template>
  <div class="card-secret orders"> <!-- 复用 orders 容器类 -->
    <!-- Header -->
    <div class="orders__header">
      <div class="orders__title-row">
        <div class="orders__title-icon">
          <IconKey />
        </div>
        <h1 class="orders__title">卡密管理</h1>
      </div>
      <div class="orders__actions">
        <button class="btn btn--primary" @click="handleAdd">
          <IconPlus />
          <span>添加卡密</span>
        </button>
        <button class="btn btn--secondary" @click="handleImport">
          <IconSync />
          <span>批量导入</span>
        </button>
        <button
          class="btn btn--danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          <IconTrash />
          <span class="mobile-hidden">批量删除</span>
        </button>
        <button
          class="btn btn--secondary"
          :class="{ 'btn--loading': loading }"
          :disabled="loading"
          @click="loadCardSecrets"
        >
          <IconRefresh />
          <span class="mobile-hidden">刷新</span>
        </button>
      </div>
    </div>

    <!-- Filter Bar -->
    <div class="orders__filter-bar">
      <div class="orders__select-wrap">
        <select v-model="queryParams.xianyuAccountId" class="orders__select" @change="loadCardSecrets">
          <option :value="null">所有账号</option>
          <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
            {{ acc.accountNote || acc.unb }}
          </option>
        </select>
        <span class="orders__select-icon">
          <IconChevronDown />
        </span>
      </div>

      <div class="orders__input-wrap">
        <input
          v-model="queryParams.xyGoodsId"
          class="orders__input"
          placeholder="商品ID/名称"
          @keyup.enter="loadCardSecrets"
        />
      </div>

      <div class="orders__select-wrap">
        <select v-model="queryParams.isUsed" class="orders__select" @change="loadCardSecrets">
          <option :value="null">全部状态</option>
          <option :value="0">未使用</option>
          <option :value="1">已使用</option>
        </select>
        <span class="orders__select-icon">
          <IconChevronDown />
        </span>
      </div>



      <button class="btn btn--primary" @click="loadCardSecrets">
        <IconSearch />
        <span>查询</span>
      </button>

      <button class="btn btn--ghost" @click="handleReset">
        重置
      </button>

      <span v-if="total > 0" class="orders__count">
        共 {{ total }} 条
      </span>
    </div>

    <!-- Content -->
    <div class="orders__content">
      <div class="orders__table-wrap">
        <table class="card-secret-table">
          <thead>
            <tr>
              <th style="width: 40px">
                <input 
                  type="checkbox" 
                  :checked="selectedIds.length > 0 && selectedIds.length === cardSecretList.length"
                  @change="(e: any) => handleSelectionChange(e.target.checked ? cardSecretList : [])"
                />
              </th>
              <th>ID</th>
              <th>所属账号</th>
              <th>商品信息/商品ID</th>
              <th>卡密内容</th>
              <th>状态</th>
              <th>关联订单</th>
              <th>时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in cardSecretList" :key="item.id">
              <td>
                <input 
                  type="checkbox" 
                  :checked="selectedIds.includes(item.id)"
                  @change="(e: any) => {
                    if (e.target.checked) {
                      selectedIds.push(item.id)
                    } else {
                      const index = selectedIds.indexOf(item.id)
                      if (index > -1) selectedIds.splice(index, 1)
                    }
                  }"
                />
              </td>
              <td>{{ item.id }}</td>
              <td>
                <div class="cell-text">{{ item.accountNote || '-' }}</div>
                <div class="cell-subtext">{{ item.xianyuAccountId }}</div>
              </td>
              <td>
                <div class="cell-text goods-title-text" :title="item.goodsTitle">{{ item.goodsTitle || '-' }}</div>
                <div class="cell-subtext">{{ item.xyGoodsId || '-' }}</div>
              </td>
              <td>
                <div class="card-content-box" :title="item.cardContent">{{ item.cardContent }}</div>
              </td>
              <td>
                <span class="status-badge" :class="getStatusClass(item.isUsed)">
                  {{ formatStatus(item.isUsed) }}
                </span>
              </td>
              <td>
                <div class="cell-text">{{ item.orderId || '-' }}</div>
              </td>
              <td>
                <div class="cell-subtext">创: {{ item.createTime }}</div>
                <div class="cell-subtext" v-if="item.isUsed === 1">用: {{ item.updateTime }}</div>
              </td>
              <td>
                <div class="row-actions">
                  <button class="btn-icon btn-icon--primary" @click="handleEdit(item)" title="编辑">
                    <IconEdit />
                  </button>
                  <button class="btn-icon btn-icon--danger" @click="handleDelete(item.id)" title="删除">
                    <IconTrash />
                  </button>
                </div>
              </td>
            </tr>
            <tr v-if="cardSecretList.length === 0">
              <td colspan="9" class="empty-cell">暂无卡密数据</td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Pagination -->
      <div v-if="totalPages > 1" class="orders__pagination">
        <button
          class="orders__page-btn"
          :class="{ 'orders__page-btn--disabled': queryParams.pageNum <= 1 }"
          @click="handlePageChange(queryParams.pageNum - 1)"
        >
          <IconChevronLeft />
        </button>

        <template v-for="page in getPageButtons()" :key="page">
          <button
            class="orders__page-btn"
            :class="{ 'orders__page-btn--active': page === queryParams.pageNum }"
            @click="handlePageChange(page)"
          >
            {{ page }}
          </button>
        </template>

        <button
          class="orders__page-btn"
          :class="{ 'orders__page-btn--disabled': queryParams.pageNum >= totalPages }"
          @click="handlePageChange(queryParams.pageNum + 1)"
        >
          <IconChevronRight />
        </button>

        <span class="orders__page-info">{{ queryParams.pageNum }} / {{ totalPages }}</span>
      </div>
    </div>

    <!-- Edit Dialog -->
    <Transition name="overlay-fade">
      <div v-if="dialogs.edit" key="card-edit-dialog" class="orders__dialog-overlay" @click.self="dialogs.edit = false">
        <div class="orders__dialog">
          <div class="orders__dialog-header">
            <h3 class="orders__dialog-title">{{ editForm.id ? '编辑卡密' : '添加卡密' }}</h3>
          </div>
          <div class="orders__dialog-body orders__dialog-body--form">
            <div class="form-group">
              <label class="form-label">所属账号</label>
              <div class="orders__select-wrap w-full">
                <select v-model="editForm.xianyuAccountId" class="orders__select w-full">
                  <option :value="null" disabled>选择账号</option>
                  <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                    {{ acc.accountNote || acc.unb }}
                  </option>
                </select>
                <span class="orders__select-icon">
                  <IconChevronDown />
                </span>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">商品ID</label>
              <input v-model="editForm.xyGoodsId" class="orders__input w-full" placeholder="请输入闲鱼商品ID" />
            </div>
            <div class="form-group">
              <label class="form-label">卡密内容</label>
              <textarea v-model="editForm.cardContent" class="orders__textarea w-full" placeholder="请输入卡密内容" rows="3"></textarea>
            </div>
            <div class="form-group" v-if="editForm.id">
              <label class="form-label">使用状态</label>
              <div class="orders__select-wrap w-full">
                <select v-model="editForm.isUsed" class="orders__select w-full">
                  <option :value="0">未使用</option>
                  <option :value="1">已使用</option>
                </select>
                <span class="orders__select-icon">
                  <IconChevronDown />
                </span>
              </div>
            </div>
          </div>
          <div class="orders__dialog-footer">
            <button class="orders__dialog-btn orders__dialog-btn--cancel" @click="dialogs.edit = false">取消</button>
            <button class="orders__dialog-btn orders__dialog-btn--confirm" @click="saveEdit">保存</button>
          </div>
        </div>
      </div>
    </Transition>

    <!-- Import Dialog -->
    <Transition name="overlay-fade">
      <div v-if="dialogs.import" key="card-import-dialog" class="orders__dialog-overlay" @click.self="dialogs.import = false">
        <div class="orders__dialog orders__dialog--wide">
          <div class="orders__dialog-header">
            <h3 class="orders__dialog-title">批量导入卡密</h3>
          </div>
          <div class="orders__dialog-body orders__dialog-body--form">
            <div class="form-group">
              <label class="form-label">所属账号</label>
              <div class="orders__select-wrap w-full">
                <select v-model="importForm.xianyuAccountId" class="orders__select w-full">
                  <option :value="null" disabled>选择账号</option>
                  <option v-for="acc in accounts" :key="acc.id" :value="acc.id">
                    {{ acc.accountNote || acc.unb }}
                  </option>
                </select>
                <span class="orders__select-icon">
                  <IconChevronDown />
                </span>
              </div>
            </div>
            <div class="form-group">
              <label class="form-label">商品ID</label>
              <input v-model="importForm.xyGoodsId" class="orders__input w-full" placeholder="请输入闲鱼商品ID" />
            </div>
            <div class="form-group mb-0">
              <label class="form-label">卡密列表 (一行一个)</label>
              <textarea v-model="importForm.secretsText" class="orders__textarea w-full" placeholder="内容1&#10;内容2&#10;内容3..." rows="6"></textarea>
            </div>
          </div>
          <div class="orders__dialog-footer">
            <button class="orders__dialog-btn orders__dialog-btn--cancel" @click="dialogs.import = false">取消</button>
            <button class="orders__dialog-btn orders__dialog-btn--confirm" @click="saveImport">导入</button>
          </div>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.card-secret-table {
  width: 100%;
  border-collapse: collapse;
}

.card-secret-table th,
.card-secret-table td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid var(--border-color);
}

.card-secret-table th {
  font-weight: 600;
  color: var(--text-secondary);
  background-color: var(--bg-secondary);
}

.cell-text {
  font-weight: 500;
  color: var(--text-primary);
}

.cell-subtext {
  font-size: 12px;
  color: var(--text-secondary);
  margin-top: 2px;
}

.goods-title-text {
  max-width: 200px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.card-content-box {
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  background: var(--bg-secondary);
  padding: 4px 8px;
  border-radius: 4px;
  font-family: monospace;
  font-size: 13px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.status--unused {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.status--used {
  background-color: #f5f5f5;
  color: #8c8c8c;
  border: 1px solid #d9d9d9;
}

.empty-cell {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary);
}

.btn-icon {
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px;
  border-radius: 4px;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
}

.btn-icon svg {
  width: 16px;
  height: 16px;
}

.btn-icon:hover {
  background-color: var(--bg-secondary);
}

.btn-icon--danger {
  color: var(--danger-color);
}

.btn-icon--danger:hover {
  background-color: #fff1f0;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.w-full {
  width: 100% !important;
}

.orders__dialog-body--form {
  text-align: left !important;
  padding: 16px 24px 24px !important;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.form-group {
  margin-bottom: 0;
  text-align: left;
}

.orders__select-wrap.w-full {
  display: flex;
  width: 100%;
}

.orders__select-wrap.w-full .orders__select {
  flex: 1;
}

.orders__input.w-full,
.orders__select.w-full,
.orders__textarea.w-full {
  width: 100% !important;
  box-sizing: border-box;
}

.mb-0 {
  margin-bottom: 0 !important;
}

.form-label {
  display: block;
  font-size: 13px;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.orders__textarea {
  width: 100%;
  padding: 10px 12px;
  font-size: 13px;
  border: 1px solid var(--border-color);
  border-radius: 8px;
  background: var(--bg-secondary);
  resize: vertical;
  font-family: inherit;
  outline: none;
  transition: border-color 0.2s;
}

.orders__textarea:focus {
  border-color: var(--primary-color);
}

.overlay-fade-enter-active,
.overlay-fade-leave-active {
  transition: opacity 0.2s ease;
}

.overlay-fade-enter-from,
.overlay-fade-leave-to {
  opacity: 0;
}

.btn-icon--primary {
  color: var(--primary-color);
}

.btn-icon--primary:hover {
  background-color: #e6f7ff;
}
</style>
