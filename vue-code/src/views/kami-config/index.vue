<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getKamiConfigsByAccountId,
  saveKamiConfig,
  deleteKamiConfig,
  queryKamiItems,
  addKamiItem,
  batchImportKamiItems,
  deleteKamiItem,
  resetKamiItem,
  type KamiConfig,
  type KamiItem
} from '@/api/kami-config'
import { getAccountList } from '@/api/account'
import type { Account } from '@/types'

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

const isMobile = ref(false)
const rulesExpanded = ref(false)

const filterStatus = ref<number | undefined>(undefined)
const filterKeyword = ref('')

const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

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
      if (kamiConfigs.value.length > 0 && !selectedConfigId.value) {
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

watch(selectedAccountId, () => {
  if (selectedAccountId.value) {
    selectedConfigId.value = null
    kamiItems.value = []
    loadKamiConfigs()
  }
})

onMounted(() => {
  checkScreenSize()
  window.addEventListener('resize', checkScreenSize)
  loadAccounts()
})
</script>

<template>
  <div class="kami-page">
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
          新建配置
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
              <el-table-column label="操作" width="160" fixed="right">
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

    <el-dialog v-model="showCreateDialog" title="新建卡密配置" width="460" :close-on-click-modal="false">
      <el-form label-width="80px">
        <el-form-item label="别名">
          <el-input v-model="createForm.aliasName" placeholder="请输入别名" maxlength="50" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="createLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAddDialog" title="添加卡密" width="460" :close-on-click-modal="false">
      <el-input v-model="addContent" type="textarea" :rows="3" placeholder="请输入卡密内容" />
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddKami" :loading="addLoading">确定</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showImportDialog" title="批量导入卡密" width="560" :close-on-click-modal="false">
      <p style="color: #909399; font-size: 13px; margin-bottom: 8px;">每行一条卡密，重复卡密将自动跳过</p>
      <el-input v-model="importContent" type="textarea" :rows="10" placeholder="卡密1&#10;卡密2&#10;卡密3" />
      <template #footer>
        <el-button @click="showImportDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBatchImport" :loading="importLoading">导入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showAlertDialog" title="预警配置" width="480" :close-on-click-modal="false">
      <el-form label-width="90px">
        <el-form-item label="开启预警">
          <el-switch v-model="alertForm.alertEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="阈值类型">
          <el-radio-group v-model="alertForm.alertThresholdType">
            <el-radio :value="1">数量</el-radio>
            <el-radio :value="2">百分比</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="阈值数值">
          <el-input-number v-model="alertForm.alertThresholdValue" :min="1" :max="alertForm.alertThresholdType === 2 ? 100 : 99999" />
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">
            {{ alertForm.alertThresholdType === 1 ? '可用卡密低于此数量时预警' : '可用比例低于此百分比时预警' }}
          </span>
        </el-form-item>
        <el-form-item label="预警邮箱">
          <el-input v-model="alertForm.alertEmail" placeholder="留空则使用系统设置的邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAlertDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSaveAlert" :loading="alertLoading">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.kami-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 16px;
  background: #fff;
  overflow: hidden;
}
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
  color: #1d1d1f;
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
  border-right: 1px solid rgba(0,0,0,0.06);
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
  color: #86868b;
  font-size: 14px;
  text-align: center;
  padding: 40px 0;
}
.config-card {
  padding: 12px;
  border: 1px solid rgba(0,0,0,0.06);
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
  border-color: #007aff;
  background: rgba(0,122,255,0.04);
}
.config-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 6px;
}
.config-card__stats {
  display: flex;
  gap: 8px;
  font-size: 12px;
  color: #6e6e73;
  margin-bottom: 4px;
}
.config-card__stat.used { color: #ff9500; }
.config-card__stat.avail { color: #34c759; }
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
  color: #1d1d1f;
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
@media screen and (max-width: 768px) {
  .kami-page__body {
    flex-direction: column;
  }
  .kami-page__sidebar {
    width: 100%;
    border-right: none;
    padding-right: 0;
    border-bottom: 1px solid rgba(0,0,0,0.06);
    padding-bottom: 12px;
    max-height: 200px;
  }
  .kami-page__header {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>
