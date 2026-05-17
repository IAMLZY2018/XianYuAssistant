<script setup lang="ts">
import { ref, computed, onMounted, watch, inject, defineComponent, h } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import '@/styles/header-selectors.css'
import {
  getKamiConfigsByAccountId,
  saveKamiConfig,
  deleteKamiConfig,
  queryKamiItems,
  addKamiItem,
  batchImportKamiItems,
  deleteKamiItem,
  resetKamiItem,
  exportKamiItems,
  type KamiConfig,
  type KamiItem
} from '@/api/kami-config'
import { getAccountList } from '@/api/account'
import type { Account } from '@/types'
import IconChevronDown from '@/components/icons/IconChevronDown.vue'

const accounts = ref<Account[]>([])
const selectedAccountId = ref<number | null>(null)
const kamiConfigs = ref<KamiConfig[]>([])
const configLoading = ref(false)

const selectedConfigId = ref<number | null>(null)
const kamiItems = ref<KamiItem[]>([])
const itemsLoading = ref(false)

const showCreateDialog = ref(false)
const createForm = ref({
  aliasName: ''
})
const createLoading = ref(false)

const showImportDialog = ref(false)
const importContent = ref('')
const importLoading = ref(false)

const showAddDialog = ref(false)
const addContent = ref('')
const addLoading = ref(false)

const showAlertDialog = ref(false)
const alertForm = ref({
  alertEnabled: 0,
  alertThresholdType: 1,
  alertThresholdValue: 10,
  alertEmail: ''
})
const alertLoading = ref(false)

const showExportDialog = ref(false)
const exportStatus = ref<{ unused: boolean; used: boolean }>({ unused: true, used: true })

const isMobile = ref(false)
const rulesExpanded = ref(false)

const filterStatus = ref<number | undefined>(undefined)
const filterKeyword = ref('')

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

// 导航栏注入 — 必须在 setup 顶层调用
const setHeaderContent = inject<(content: any) => void>('setHeaderContent')

const HeaderSelectors = defineComponent({
  setup() {
    return () => h('div', { class: 'header-selectors' }, [
      h('div', { class: 'header-select-wrap' }, [
        h('select', {
          class: 'header-select',
          onChange: (e: Event) => {
            const val = (e.target as HTMLSelectElement).value
            selectedAccountId.value = val ? parseInt(val) : null
          }
        }, [
          h('option', { value: '', disabled: true, selected: !selectedAccountId.value }, '账号'),
          ...accounts.value.map(acc =>
            h('option', {
              value: acc.id.toString(),
              selected: selectedAccountId.value === acc.id
            }, acc.accountNote || acc.unb)
          )
        ]),
        h(IconChevronDown, { class: 'header-select-icon' })
      ])
    ])
  }
})

const selectedConfig = computed(() => {
  return kamiConfigs.value.find(c => c.id === selectedConfigId.value)
})

const loadAccounts = async () => {
  try {
    const res = await getAccountList()
    if (res.code === 200 && res.data) {
      accounts.value = res.data.accounts || []
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0]!.id
      }
    }
  } catch (e) {
    console.error('加载账号失败', e)
  }
}

const loadKamiConfigs = async () => {
  if (!selectedAccountId.value) return
  configLoading.value = true
  try {
    const res = await getKamiConfigsByAccountId(selectedAccountId.value)
    if (res.code === 200) {
      kamiConfigs.value = res.data || []
      if (kamiConfigs.value.length > 0 && !selectedConfigId.value && !isMobile.value) {
        selectedConfigId.value = kamiConfigs.value[0]!.id
        loadKamiItems()
      } else if (kamiConfigs.value.length === 0) {
        selectedConfigId.value = null
        kamiItems.value = []
      }
    }
  } catch (e) {
    console.error('加载卡密配置失败', e)
  } finally {
    configLoading.value = false
  }
}

const loadKamiItems = async () => {
  if (!selectedConfigId.value) return
  itemsLoading.value = true
  try {
    const res = await queryKamiItems({
      kamiConfigId: selectedConfigId.value,
      status: filterStatus.value,
      keyword: filterKeyword.value || undefined
    })
    if (res.code === 200) {
      kamiItems.value = res.data || []
    }
  } catch (e) {
    console.error('加载卡密列表失败', e)
  } finally {
    itemsLoading.value = false
  }
}

const handleAccountChange = () => {
  selectedConfigId.value = null
  kamiItems.value = []
  loadKamiConfigs()
}

const selectConfig = (config: KamiConfig) => {
  selectedConfigId.value = config.id
  filterStatus.value = undefined
  filterKeyword.value = ''
  loadKamiItems()
}

const handleCreate = async () => {
  if (!selectedAccountId.value) {
    ElMessage.warning('请先选择账号')
    return
  }
  createLoading.value = true
  try {
    const res = await saveKamiConfig({
      xianyuAccountId: selectedAccountId.value,
      aliasName: createForm.value.aliasName || '未命名'
    })
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreateDialog.value = false
      createForm.value = { aliasName: '' }
      await loadKamiConfigs()
      if (res.data?.id) {
        selectedConfigId.value = res.data.id
        loadKamiItems()
      }
    } else {
      ElMessage.error(res.msg || '创建失败')
    }
  } catch (e) {
    ElMessage.error('创建失败')
  } finally {
    createLoading.value = false
  }
}

const handleDeleteConfig = async (config: KamiConfig) => {
  try {
    await ElMessageBox.confirm(
      `确定删除卡密配置「${config.aliasName || config.id}」及其所有卡密？`,
      '删除确认',
      { type: 'warning' }
    )
    const res = await deleteKamiConfig(config.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      if (selectedConfigId.value === config.id) {
        selectedConfigId.value = null
        kamiItems.value = []
      }
      loadKamiConfigs()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {}
}

const handleAddKami = async () => {
  if (!addContent.value.trim()) {
    ElMessage.warning('请输入卡密内容')
    return
  }
  addLoading.value = true
  try {
    const res = await addKamiItem({
      kamiConfigId: selectedConfigId.value!,
      kamiContent: addContent.value.trim()
    })
    if (res.code === 200) {
      ElMessage.success('添加成功')
      showAddDialog.value = false
      addContent.value = ''
      loadKamiItems()
      loadKamiConfigs()
    } else {
      ElMessage.error(res.msg || '添加失败')
    }
  } catch (e) {
    ElMessage.error('添加失败')
  } finally {
    addLoading.value = false
  }
}

const handleBatchImport = async () => {
  if (!importContent.value.trim()) {
    ElMessage.warning('请输入卡密内容')
    return
  }
  importLoading.value = true
  try {
    const res = await batchImportKamiItems({
      kamiConfigId: selectedConfigId.value!,
      kamiContents: importContent.value
    })
    if (res.code === 200) {
      ElMessage.success(res.msg || '导入成功')
      showImportDialog.value = false
      importContent.value = ''
      loadKamiItems()
      loadKamiConfigs()
    } else {
      ElMessage.error(res.msg || '导入失败')
    }
  } catch (e) {
    ElMessage.error('导入失败')
  } finally {
    importLoading.value = false
  }
}

const handleDeleteItem = async (item: KamiItem) => {
  try {
    await ElMessageBox.confirm('确定删除该卡密？', '删除确认', { type: 'warning' })
    const res = await deleteKamiItem(item.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadKamiItems()
      loadKamiConfigs()
    } else {
      ElMessage.error(res.msg || '删除失败')
    }
  } catch {}
}

const handleResetItem = async (item: KamiItem) => {
  try {
    await ElMessageBox.confirm('确定重置该卡密为未使用状态？', '重置确认', { type: 'warning' })
    const res = await resetKamiItem(item.id)
    if (res.code === 200) {
      ElMessage.success('重置成功')
      loadKamiItems()
      loadKamiConfigs()
    } else {
      ElMessage.error(res.msg || '重置失败')
    }
  } catch {}
}

const handleFilterChange = () => {
  loadKamiItems()
}

const openAlertDialog = () => {
  if (!selectedConfig.value) return
  alertForm.value = {
    alertEnabled: selectedConfig.value.alertEnabled || 0,
    alertThresholdType: selectedConfig.value.alertThresholdType || 1,
    alertThresholdValue: selectedConfig.value.alertThresholdValue || 10,
    alertEmail: selectedConfig.value.alertEmail || ''
  }
  showAlertDialog.value = true
}

const handleSaveAlert = async () => {
  if (!selectedConfigId.value) return
  alertLoading.value = true
  try {
    const res = await saveKamiConfig({
      id: selectedConfigId.value,
      xianyuAccountId: selectedAccountId.value!,
      aliasName: selectedConfig.value?.aliasName,
      alertEnabled: alertForm.value.alertEnabled,
      alertThresholdType: alertForm.value.alertThresholdType,
      alertThresholdValue: alertForm.value.alertThresholdValue,
      alertEmail: alertForm.value.alertEmail
    })
    if (res.code === 200) {
      ElMessage.success('设置保存成功')
      showAlertDialog.value = false
      loadKamiConfigs()
    } else {
      ElMessage.error(res.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    alertLoading.value = false
  }
}

const openExportDialog = () => {
  exportStatus.value = { unused: true, used: true }
  showExportDialog.value = true
}

const handleExport = async () => {
  if (!selectedConfigId.value) return
  if (!exportStatus.value.unused && !exportStatus.value.used) {
    ElMessage.warning('请至少选择一种状态')
    return
  }

  try {
    const res = await exportKamiItems({
      kamiConfigId: selectedConfigId.value,
      includeUnused: exportStatus.value.unused,
      includeUsed: exportStatus.value.used
    })
    const allItems = res.data || []

    if (allItems.length === 0) {
      ElMessage.warning('没有可导出的数据')
      return
    }

    const configName = selectedConfig.value?.aliasName || `配置${selectedConfigId.value}`
    const timestamp = new Date().toISOString().slice(0, 19).replace(/[:-]/g, '').replace('T', '_')

    const header = '序号\t卡密内容\t状态\t订单ID\t使用时间\t添加时间\n'
    const rows = allItems.map(item =>
      `${item.sortOrder}\t${item.kamiContent}\t${item.status === 0 ? '未使用' : '已使用'}\t${item.orderId || ''}\t${item.usedTime || ''}\t${item.createTime}`
    ).join('\n')
    const content = header + rows
    const blob = new Blob(['\ufeff' + content], { type: 'text/plain;charset=utf-8' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `${configName}_${timestamp}.txt`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success(`已导出 ${allItems.length} 条数据`)
    showExportDialog.value = false
  } catch (e) {
    ElMessage.error('导出失败')
  }
}

watch(selectedAccountId, () => {
  if (selectedAccountId.value) {
    selectedConfigId.value = null
    kamiItems.value = []
    loadKamiConfigs()
  }
})

onMounted(async () => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  if (setHeaderContent) setHeaderContent(HeaderSelectors)
  await loadAccounts()
})
</script>

<template>
  <div class="kami-page">

    <!-- ===== 手机端 ===== -->
    <template v-if="isMobile">

      <!-- 配置列表视图 -->
      <div v-if="!selectedConfigId" class="kami-mobile">
        <header class="kami-mobile__header">
          <div class="kami-mobile__header-top">
            <h1 class="kami-page__title">🔑 卡密仓库</h1>
            <el-button type="primary" size="small" @click="showCreateDialog = true" :disabled="!selectedAccountId">
              新建
            </el-button>
          </div>
        </header>

        <div class="kami-mobile__list">
          <div v-if="configLoading" class="kami-page__empty">加载中...</div>
          <div v-else-if="kamiConfigs.length === 0" class="kami-page__empty">暂无配置，点击右上角新建</div>
          <div
            v-for="config in kamiConfigs"
            :key="config.id"
            class="config-card"
            @click="selectConfig(config)"
          >
            <div class="config-card__name">{{ config.aliasName || `配置#${config.id}` }}</div>
            <div class="config-card__stats">
              <span class="config-card__stat">总量 {{ config.totalCount }}</span>
              <span class="config-card__stat used">已用 {{ config.usedCount }}</span>
              <span class="config-card__stat avail">可用 {{ config.availableCount }}</span>
              <el-tag v-if="config.alertEnabled === 1" type="warning" size="small" style="margin-left: 4px;">预警</el-tag>
            </div>
            <el-button
              class="config-card__del"
              type="danger"
              text
              size="small"
              @click.stop="handleDeleteConfig(config)"
            >删除</el-button>
          </div>
        </div>
      </div>

      <!-- 卡密详情视图 -->
      <div v-else class="kami-mobile">
        <header class="kami-mobile__header">
          <div class="kami-mobile__header-top">
            <button class="kami-mobile__back" @click="selectedConfigId = null; kamiItems = []">
              ← 返回
            </button>
            <span class="kami-mobile__config-name">{{ selectedConfig?.aliasName || `配置#${selectedConfigId}` }}</span>
          </div>
          <div class="kami-mobile__detail-actions">
            <el-button size="small" @click="showAddDialog = true">添加</el-button>
            <el-button size="small" type="primary" @click="showImportDialog = true">批量导入</el-button>
            <el-button size="small" type="success" @click="openExportDialog">导出</el-button>
            <el-button size="small" type="warning" @click="openAlertDialog">预警</el-button>
          </div>
        </header>

        <div class="kami-mobile__filters">
          <el-select
            v-model="filterStatus"
            placeholder="全部状态"
            clearable
            style="flex: 1;"
            @change="handleFilterChange"
          >
            <el-option :value="0" label="未使用" />
            <el-option :value="1" label="已使用" />
          </el-select>
          <el-input
            v-model="filterKeyword"
            placeholder="搜索卡密"
            clearable
            style="flex: 2;"
            @keyup.enter="handleFilterChange"
            @clear="handleFilterChange"
          />
          <el-button @click="handleFilterChange">搜索</el-button>
        </div>

        <div class="kami-mobile__items">
          <div v-if="itemsLoading" class="kami-page__empty">加载中...</div>
          <div v-else-if="kamiItems.length === 0" class="kami-page__empty">暂无卡密</div>
          <div
            v-for="item in kamiItems"
            :key="item.id"
            class="kami-item-card"
            :class="{ 'kami-item-card--used': item.status === 1 }"
          >
            <div class="kami-item-card__content">{{ item.kamiContent }}</div>
            <div class="kami-item-card__meta">
              <el-tag :type="item.status === 0 ? 'success' : 'info'" size="small">
                {{ item.status === 0 ? '未使用' : '已使用' }}
              </el-tag>
              <span v-if="item.usedTime" class="kami-item-card__time">{{ item.usedTime }}</span>
            </div>
            <div class="kami-item-card__actions">
              <el-button v-if="item.status === 1" type="warning" text size="small" @click="handleResetItem(item)">重置</el-button>
              <el-button type="danger" text size="small" @click="handleDeleteItem(item)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

    </template>

    <!-- ===== 桌面端 ===== -->
    <template v-else>
      <header class="kami-page__header">
        <h1 class="kami-page__title">🔑 卡密仓库</h1>
        <div class="kami-page__actions">
          <el-select
            v-model="selectedAccountId"
            placeholder="选择账号"
            class="account-select"
            @change="handleAccountChange"
          >
            <el-option
              v-for="acc in accounts"
              :key="acc.id"
              :label="acc.accountNote || `账号${acc.id}`"
              :value="acc.id"
            />
          </el-select>
          <el-button type="primary" @click="showCreateDialog = true" :disabled="!selectedAccountId">
            新建密钥仓库
          </el-button>
        </div>
      </header>

      <div class="kami-page__body">
        <div class="kami-page__sidebar">
          <div v-if="configLoading" class="kami-page__empty">加载中...</div>
          <div v-else-if="kamiConfigs.length === 0" class="kami-page__empty">暂无配置，点击右上角新建</div>
          <div
            v-for="config in kamiConfigs"
            :key="config.id"
            class="config-card"
            :class="{ 'config-card--active': selectedConfigId === config.id }"
            @click="selectConfig(config)"
          >
            <div class="config-card__name">{{ config.aliasName || `配置#${config.id}` }}</div>
            <div class="config-card__stats">
              <span class="config-card__stat">总量 {{ config.totalCount }}</span>
              <span class="config-card__stat used">已用 {{ config.usedCount }}</span>
              <span class="config-card__stat avail">可用 {{ config.availableCount }}</span>
              <el-tag v-if="config.alertEnabled === 1" type="warning" size="small" style="margin-left: 4px;">预警</el-tag>
            </div>
            <el-button
              class="config-card__del"
              type="danger"
              text
              size="small"
              @click.stop="handleDeleteConfig(config)"
            >删除</el-button>
          </div>
        </div>

        <div class="kami-page__main">
          <div v-if="!selectedConfig" class="kami-page__empty-main">请选择左侧卡密配置</div>
          <template v-else>
            <div class="kami-detail__header">
              <h2>{{ selectedConfig.aliasName || `配置#${selectedConfig.id}` }}</h2>
              <div class="kami-detail__actions">
                <el-button @click="showAddDialog = true">添加卡密</el-button>
                <el-button type="primary" @click="showImportDialog = true">批量导入</el-button>
                <el-button type="success" @click="openExportDialog">导出</el-button>
                <el-button type="warning" @click="openAlertDialog">预警配置</el-button>
              </div>
            </div>

            <div class="kami-detail__filters">
              <el-select
                v-model="filterStatus"
                placeholder="全部状态"
                clearable
                style="width: 120px; margin-right: 8px;"
                @change="handleFilterChange"
              >
                <el-option :value="0" label="未使用" />
                <el-option :value="1" label="已使用" />
              </el-select>
              <el-input
                v-model="filterKeyword"
                placeholder="搜索卡密内容"
                clearable
                style="width: 200px; margin-right: 8px;"
                @keyup.enter="handleFilterChange"
                @clear="handleFilterChange"
              />
              <el-button @click="handleFilterChange">搜索</el-button>
            </div>

            <div class="kami-detail__table">
              <div v-if="itemsLoading" class="kami-page__empty">加载中...</div>
              <el-table v-else :data="kamiItems" stripe style="width: 100%" max-height="500">
                <el-table-column prop="sortOrder" label="序号" width="70" />
                <el-table-column prop="kamiContent" label="卡密内容" min-width="200" show-overflow-tooltip />
                <el-table-column label="状态" width="100">
                  <template #default="{ row }">
                    <el-tag :type="row.status === 0 ? 'success' : 'info'" size="small">
                      {{ row.status === 0 ? '未使用' : '已使用' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="orderId" label="订单ID" width="160" show-overflow-tooltip />
                <el-table-column prop="usedTime" label="使用时间" width="170" />
                <el-table-column prop="createTime" label="添加时间" width="170" />
                <el-table-column label="操作" width="160">
                  <template #default="{ row }">
                    <el-button
                      v-if="row.status === 1"
                      type="warning"
                      text
                      size="small"
                      @click="handleResetItem(row)"
                    >重置</el-button>
                    <el-button type="danger" text size="small" @click="handleDeleteItem(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </template>
        </div>
      </div>
    </template>

    <!-- ===== 弹窗（共用） ===== -->
    <Teleport to="body">
      <!-- 新建卡密配置 -->
      <Transition name="modal">
        <div v-if="showCreateDialog" class="modal-overlay" @click.self="showCreateDialog = false">
          <div class="modal-container">
            <div class="modal-header">
              <h2 class="modal-title">新建卡密配置</h2>
              <button class="modal-close" @click="showCreateDialog = false">×</button>
            </div>
            <div class="modal-body">
              <div class="form-row">
                <label class="form-label">别名</label>
                <input v-model="createForm.aliasName" class="form-input" placeholder="请输入别名" maxlength="50" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showCreateDialog = false">取消</button>
              <button class="btn btn-primary" :class="{ 'is-loading': createLoading }" :disabled="createLoading" @click="handleCreate">确定</button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 添加卡密 -->
      <Transition name="modal">
        <div v-if="showAddDialog" class="modal-overlay" @click.self="showAddDialog = false">
          <div class="modal-container">
            <div class="modal-header">
              <h2 class="modal-title">添加卡密</h2>
              <button class="modal-close" @click="showAddDialog = false">×</button>
            </div>
            <div class="modal-body">
              <textarea v-model="addContent" class="form-textarea" :rows="3" placeholder="请输入卡密内容"></textarea>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showAddDialog = false">取消</button>
              <button class="btn btn-primary" :class="{ 'is-loading': addLoading }" :disabled="addLoading" @click="handleAddKami">确定</button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 批量导入 -->
      <Transition name="modal">
        <div v-if="showImportDialog" class="modal-overlay" @click.self="showImportDialog = false">
          <div class="modal-container modal-container--lg">
            <div class="modal-header">
              <h2 class="modal-title">批量导入卡密</h2>
              <button class="modal-close" @click="showImportDialog = false">×</button>
            </div>
            <div class="modal-body">
              <p class="form-hint">每行一条卡密，重复卡密不会跳过</p>
              <textarea v-model="importContent" class="form-textarea" :rows="10" placeholder="卡密1&#10;卡密2&#10;卡密3"></textarea>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showImportDialog = false">取消</button>
              <button class="btn btn-primary" :class="{ 'is-loading': importLoading }" :disabled="importLoading" @click="handleBatchImport">导入</button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 预警配置 -->
      <Transition name="modal">
        <div v-if="showAlertDialog" class="modal-overlay" @click.self="showAlertDialog = false">
          <div class="modal-container">
            <div class="modal-header">
              <h2 class="modal-title">预警配置</h2>
              <button class="modal-close" @click="showAlertDialog = false">×</button>
            </div>
            <div class="modal-body">
              <div class="form-row">
                <label class="form-label">开启预警</label>
                <label class="form-switch">
                  <input type="checkbox" :checked="alertForm.alertEnabled === 1" @change="alertForm.alertEnabled = alertForm.alertEnabled === 1 ? 0 : 1" />
                  <span class="form-switch-track"></span>
                </label>
              </div>
              <div class="form-row">
                <label class="form-label">阈值类型</label>
                <div class="form-radio-group">
                  <label class="form-radio" :class="{ 'is-active': alertForm.alertThresholdType === 1 }">
                    <input type="radio" :value="1" v-model="alertForm.alertThresholdType" />数量
                  </label>
                  <label class="form-radio" :class="{ 'is-active': alertForm.alertThresholdType === 2 }">
                    <input type="radio" :value="2" v-model="alertForm.alertThresholdType" />百分比
                  </label>
                </div>
              </div>
              <div class="form-row">
                <label class="form-label">阈值数值</label>
                <input type="number" v-model.number="alertForm.alertThresholdValue" class="form-input form-input--num" :min="1" :max="alertForm.alertThresholdType === 2 ? 100 : 99999" />
                <span class="form-suffix">{{ alertForm.alertThresholdType === 1 ? '可用卡密低于此数量时预警' : '可用比例低于此百分比时预警' }}</span>
              </div>
              <div class="form-row">
                <label class="form-label">预警邮箱</label>
                <input v-model="alertForm.alertEmail" class="form-input" placeholder="留空则使用系统设置的邮箱" />
              </div>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showAlertDialog = false">取消</button>
              <button class="btn btn-primary" :class="{ 'is-loading': alertLoading }" :disabled="alertLoading" @click="handleSaveAlert">保存</button>
            </div>
          </div>
        </div>
      </Transition>

      <!-- 导出卡密 -->
      <Transition name="modal">
        <div v-if="showExportDialog" class="modal-overlay" @click.self="showExportDialog = false">
          <div class="modal-container">
            <div class="modal-header">
              <h2 class="modal-title">导出卡密</h2>
              <button class="modal-close" @click="showExportDialog = false">×</button>
            </div>
            <div class="modal-body">
              <div class="form-row">
                <label class="form-label">导出状态</label>
                <div class="form-checkbox-group">
                  <label class="form-checkbox">
                    <input type="checkbox" v-model="exportStatus.unused" />未使用
                  </label>
                  <label class="form-checkbox">
                    <input type="checkbox" v-model="exportStatus.used" />已使用
                  </label>
                </div>
              </div>
              <p class="form-hint form-hint--indent">导出为Excel格式（.txt文件，Excel可直接打开）</p>
            </div>
            <div class="modal-footer">
              <button class="btn btn-secondary" @click="showExportDialog = false">取消</button>
              <button class="btn btn-primary" @click="handleExport">导出</button>
            </div>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.kami-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: rgba(255,255,255,0.55);
  overflow: hidden;
  box-sizing: border-box;
}

/* ===== 桌面端 ===== */
.kami-page__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.kami-page__title {
  font-size: 20px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
}
.kami-page__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}
.account-select {
  width: 180px;
}
.kami-page__body {
  flex: 1;
  display: flex;
  gap: 16px;
  min-height: 0;
  overflow: hidden;
}
.kami-page__sidebar {
  width: 260px;
  flex-shrink: 0;
  overflow-y: auto;
  border-right: 1px solid rgba(60,60,67,.12);
  padding-right: 12px;
}
.kami-page__main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.kami-page__empty,
.kami-page__empty-main {
  color: rgba(28,28,30,.55);
  font-size: 14px;
  text-align: center;
  padding: 40px 0;
}
.config-card {
  padding: 12px;
  border: 1px solid rgba(60,60,67,.12);
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  position: relative;
}
.config-card:hover {
  border-color: rgba(0,122,255,0.3);
}
.config-card--active {
  border-color: #0A84FF;
  background: rgba(10,132,255,0.06);
}
.config-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #1c1c1e;
  margin-bottom: 6px;
}
.config-card__stats {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: rgba(28,28,30,.55);
  margin-bottom: 4px;
  flex-wrap: wrap;
}
.config-card__stat.used { color: #FF9F0A; }
.config-card__stat.avail { color: #30D158; }
.config-card__del {
  position: absolute;
  top: 8px;
  right: 8px;
}
.kami-detail__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.kami-detail__header h2 {
  font-size: 16px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
}
.kami-detail__actions {
  display: flex;
  gap: 8px;
}
.kami-detail__filters {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  flex-shrink: 0;
}
.kami-detail__table {
  flex: 1;
  min-height: 0;
  overflow: auto;
}

/* ===== 手机端 ===== */
.kami-mobile {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.kami-mobile__header {
  flex-shrink: 0;
  padding: 0 0 12px;
  border-bottom: 1px solid rgba(60,60,67,.12);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.kami-mobile__header-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kami-mobile__select {
  width: 100%;
}

.kami-mobile__back {
  background: none;
  border: none;
  color: #0A84FF;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
  -webkit-tap-highlight-color: transparent;
}

.kami-mobile__config-name {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
  flex: 1;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  padding: 0 8px;
}

.kami-mobile__detail-actions {
  display: flex;
  gap: 6px;
}

.kami-mobile__filters {
  display: flex;
  gap: 6px;
  align-items: center;
  flex-shrink: 0;
  padding: 10px 0;
  border-bottom: 1px solid rgba(60,60,67,.12);
}

.kami-mobile__list {
  flex: 1;
  overflow-y: auto;
  padding-top: 12px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.kami-mobile__list::-webkit-scrollbar { display: none; }

.kami-mobile__items {
  flex: 1;
  overflow-y: auto;
  padding-top: 8px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}
.kami-mobile__items::-webkit-scrollbar { display: none; }

/* 卡密条目卡片 */
.kami-item-card {
  padding: 10px 12px;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.kami-item-card:nth-child(even) {
  background: rgba(255,255,255,0.15);
}
.kami-item-card--used {
  opacity: 0.6;
}
.kami-item-card__content {
  font-size: 13px;
  font-weight: 500;
  color: #1c1c1e;
  word-break: break-all;
}
.kami-item-card__meta {
  display: flex;
  align-items: center;
  gap: 8px;
}
.kami-item-card__time {
  font-size: 11px;
  color: rgba(28,28,30,.55);
}
.kami-item-card__actions {
  display: flex;
  gap: 4px;
}

/* ===== 弹窗样式 ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.20);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.modal-container {
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(40px) saturate(2);
  -webkit-backdrop-filter: blur(40px) saturate(2);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 20px;
  width: 100%;
  max-width: 400px;
  max-height: 85vh;
  box-shadow: 0 16px 48px rgba(0,0,0,0.16), 0 2px 8px rgba(0,0,0,0.08);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-container--lg {
  max-width: 480px;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 20px;
  flex-shrink: 0;
}

.modal-title {
  font-size: 15px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
}

.modal-close {
  width: 26px;
  height: 26px;
  border-radius: 7px;
  border: none;
  background: transparent;
  color: rgba(28,28,30,.55);
  font-size: 18px;
  line-height: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.15s ease;
}

.modal-close:hover {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.modal-body {
  padding: 0 20px 20px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  overflow-y: auto;
  min-height: 0;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 20px;
  flex-shrink: 0;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.form-label {
  font-size: 13px;
  color: #1c1c1e;
  font-weight: 500;
  min-width: 70px;
  flex-shrink: 0;
}

.form-input {
  flex: 1;
  min-width: 0;
  padding: 8px 12px;
  border: 1px solid rgba(60,60,67,.12);
  border-radius: 8px;
  font-size: 13px;
  background: rgba(255,255,255,0.55);
  color: #1c1c1e;
  transition: border-color 0.15s ease;
  box-sizing: border-box;
}

.form-input:focus {
  outline: none;
  border-color: #0A84FF;
}

.form-input--num {
  width: 100px;
  flex: none;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid rgba(60,60,67,.12);
  border-radius: 10px;
  font-size: 13px;
  line-height: 1.5;
  resize: vertical;
  background: rgba(255,255,255,0.55);
  color: #1c1c1e;
  font-family: inherit;
  box-sizing: border-box;
}

.form-textarea:focus {
  outline: none;
  border-color: #0A84FF;
}

.form-hint {
  font-size: 12px;
  color: rgba(28,28,30,.55);
  margin: 0;
}

.form-hint--indent {
  margin-left: 70px;
}

.form-suffix {
  font-size: 12px;
  color: rgba(28,28,30,.55);
}

.form-switch {
  position: relative;
  display: inline-block;
  width: 40px;
  height: 24px;
  cursor: pointer;
}

.form-switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.form-switch-track {
  position: absolute;
  inset: 0;
  background: #e5e5e5;
  border-radius: 12px;
  transition: background 0.2s ease;
}

.form-switch-track::after {
  content: '';
  position: absolute;
  width: 20px;
  height: 20px;
  left: 2px;
  top: 2px;
  background: rgba(255,255,255,0.55);
  border-radius: 50%;
  transition: transform 0.2s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
}

.form-switch input:checked + .form-switch-track {
  background: #30D158;
}

.form-switch input:checked + .form-switch-track::after {
  transform: translateX(16px);
}

.form-radio-group {
  display: flex;
  gap: 12px;
}

.form-radio {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #1c1c1e;
  cursor: pointer;
}

.form-radio input {
  margin: 0;
}

.form-checkbox-group {
  display: flex;
  gap: 16px;
}

.form-checkbox {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #1c1c1e;
  cursor: pointer;
}

.form-checkbox input {
  margin: 0;
}

.btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.15s ease;
  border: none;
}

.btn-secondary {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
}

.btn-secondary:hover {
  background: rgba(0, 0, 0, 0.1);
}

.btn-primary {
  background: rgba(10,132,255,0.85);
  color: #fff;
}

.btn-primary:hover:not(:disabled) {
  background: rgba(10,132,255,0.75);
}

.btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.is-loading {
  opacity: 0.6;
  pointer-events: none;
}

/* Transitions */
.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .modal-container,
.modal-leave-active .modal-container {
  transition: transform 0.3s cubic-bezier(0.32, 0.94, 0.6, 1), opacity 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .modal-container,
.modal-leave-to .modal-container {
  transform: scale(0.92) translateY(8px);
  opacity: 0;
}
</style>
