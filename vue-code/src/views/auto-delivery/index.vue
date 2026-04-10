<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
  import { useRoute } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { getAccountList } from '@/api/account';
import { getGoodsList, getGoodsDetail, updateAutoDeliveryStatus } from '@/api/goods';
import {
  getAutoDeliveryConfig,
  saveOrUpdateAutoDeliveryConfig,
  type AutoDeliveryConfig,
  type SaveAutoDeliveryConfigReq,
  type GetAutoDeliveryConfigReq
} from '@/api/auto-delivery-config';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account } from '@/types';
import type { GoodsItemWithConfig } from '@/api/goods';
import GoodsDetailDialog from '../goods/components/GoodsDetailDialog.vue';
import { getAutoDeliveryRecords, type AutoDeliveryRecordReq, type AutoDeliveryRecordResp, confirmShipment, type ConfirmShipmentReq, triggerAutoDelivery, type TriggerAutoDeliveryReq } from '@/api/auto-delivery-record';

const route = useRoute();
const loading = ref(false);
const saving = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsList = ref<GoodsItemWithConfig[]>([]);
const selectedGoods = ref<GoodsItemWithConfig | null>(null);
const currentConfig = ref<AutoDeliveryConfig | null>(null);

// 商品详情对话框
const detailDialogVisible = ref(false);
const selectedGoodsId = ref<string>('');

// 表单数据
const configForm = ref({
  type: 1,
  autoDeliveryContent: '',
  autoConfirmShipment: 0
});

// 自动发货记录
const recordsLoading = ref(false);
const deliveryRecords = ref<any[]>([]);
const recordsTotal = ref(0);
const recordsPageNum = ref(1);
const recordsPageSize = ref(20);
const recordsExpanded = ref(false); // 记录表格是否展开全屏

// 格式化时间
const formatTime = (time: string) => {
  if (!time) return '-';
  // 将ISO时间格式转换为 YYYY-MM-DD HH:mm:ss
  return time.replace('T', ' ').substring(0, 19);
};

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];

      // 检查路由参数,如果有则使用路由参数中的账号ID
      const accountIdFromQuery = route.query.accountId;
      if (accountIdFromQuery) {
        const accountId = parseInt(accountIdFromQuery as string);
        if (accounts.value.some(acc => acc.id === accountId)) {
          selectedAccountId.value = accountId;
          await loadGoods();
          return;
        }
      }

      // 默认选择第一个账号
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0]?.id || null;
        loadGoods();
      }
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
  }
};

// 加载商品列表
const loadGoods = async () => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  loading.value = true;
  try {
    const params = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: 1,
      pageSize: 100 // 获取所有商品
    };

    const response = await getGoodsList(params);
    if (response.code === 0 || response.code === 200) {
      goodsList.value = response.data?.itemsWithConfig || [];

      // 检查路由参数,如果有则使用路由参数中的商品ID
      const goodsIdFromQuery = route.query.goodsId;
      if (goodsIdFromQuery) {
        const targetGoods = goodsList.value.find(g => g.item.xyGoodId === goodsIdFromQuery);
        if (targetGoods) {
          await selectGoods(targetGoods);
          return;
        }
      }

      // 默认选择第一个商品
      if (goodsList.value.length > 0 && !selectedGoods.value) {
        if (goodsList.value.length > 0) {
          selectGoods(goodsList.value[0]!);
        }
      }
    } else {
      throw new Error(response.msg || '获取商品列表失败');
    }
  } catch (error: any) {
    console.error('加载商品列表失败:', error);
    goodsList.value = [];
  } finally {
    loading.value = false;
  }
};

// 账号变更
const handleAccountChange = () => {
  selectedGoods.value = null;
  currentConfig.value = null;
  loadGoods();
};

// 选择商品
const selectGoods = async (goods: GoodsItemWithConfig) => {
  selectedGoods.value = goods;
  recordsPageNum.value = 1; // 重置页码
  await loadConfig();
  await loadDeliveryRecords();
};

// 加载配置
const loadConfig = async () => {
  if (!selectedGoods.value || !selectedAccountId.value) return;

  try {
    const req: GetAutoDeliveryConfigReq = {
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId
    };

    const response = await getAutoDeliveryConfig(req);
    if (response.code === 0 || response.code === 200) {
      currentConfig.value = response.data || null;
      if (response.data) {
        configForm.value.type = response.data.type;
        configForm.value.autoDeliveryContent = response.data.autoDeliveryContent || '';
        configForm.value.autoConfirmShipment = response.data.autoConfirmShipment || 0;
      } else {
        // 重置表单
        configForm.value.type = 1;
        configForm.value.autoDeliveryContent = '';
        configForm.value.autoConfirmShipment = 0;
      }
    } else {
      throw new Error(response.msg || '获取配置失败');
    }
  } catch (error: any) {
    console.error('加载配置失败:', error);
    currentConfig.value = null;
  }
};

// 保存配置
const saveConfig = async () => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    showInfo('请先选择商品');
    return;
  }

  if (!configForm.value.autoDeliveryContent.trim()) {
    showInfo('请输入自动发货内容');
    return;
  }

  saving.value = true;
  try {
    const req: SaveAutoDeliveryConfigReq = {
      xianyuAccountId: selectedAccountId.value,
      xianyuGoodsId: selectedGoods.value.item.id,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      type: configForm.value.type,
      autoDeliveryContent: configForm.value.autoDeliveryContent.trim(),
      autoConfirmShipment: configForm.value.autoConfirmShipment
    };

    const response = await saveOrUpdateAutoDeliveryConfig(req);
    if (response.code === 0 || response.code === 200) {
      showSuccess('保存配置成功');
      currentConfig.value = response.data || null;
    } else {
      throw new Error(response.msg || '保存配置失败');
    }
  } catch (error: any) {
    console.error('保存配置失败:', error);
  } finally {
    saving.value = false;
  }
};

// 获取状态标签类型
const getStatusType = (status: number) => {
  const statusMap: Record<number, string> = {
    0: 'success',
    1: 'info',
    2: 'warning'
  };
  return statusMap[status] || 'info';
};

// 获取状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '在售',
    1: '已下架',
    2: '已售出'
  };
  return statusMap[status] || '未知';
};

// 格式化价格
const formatPrice = (price: string) => {
  return price ? `¥${price}` : '-';
};

// 查看商品详情
const viewGoodsDetail = () => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    showInfo('请先选择商品');
    return;
  }

  selectedGoodsId.value = selectedGoods.value.item.xyGoodId;
  detailDialogVisible.value = true;
};

// 切换自动发货状态
const toggleAutoDelivery = async (value: boolean) => {
  if (!selectedGoods.value || !selectedAccountId.value) {
    showInfo('请先选择商品');
    return;
  }

  try {
    const response = await updateAutoDeliveryStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      xianyuAutoDeliveryOn: value ? 1 : 0
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess(`自动发货${value ? '开启' : '关闭'}成功`);
      // 更新本地状态
      if (selectedGoods.value) {
        selectedGoods.value.xianyuAutoDeliveryOn = value ? 1 : 0;
      }
      // 同时更新商品列表中的状态
      const goodsItem = goodsList.value.find(item => item.item.xyGoodId === selectedGoods.value?.item.xyGoodId);
      if (goodsItem) {
        goodsItem.xianyuAutoDeliveryOn = value ? 1 : 0;
      }
    } else {
      throw new Error(response.msg || '操作失败');
    }
  } catch (error: any) {
    console.error('操作失败:', error);
    // 恢复开关状态
    if (selectedGoods.value) {
      selectedGoods.value.xianyuAutoDeliveryOn = value ? 0 : 1;
    }
  }
};

// 加载自动发货记录
const loadDeliveryRecords = async () => {
  if (!selectedAccountId.value || !selectedGoods.value) {
    deliveryRecords.value = [];
    recordsTotal.value = 0;
    return;
  }

  recordsLoading.value = true;
  try {
    const req: AutoDeliveryRecordReq = {
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      pageNum: recordsPageNum.value,
      pageSize: recordsPageSize.value
    };

    const response = await getAutoDeliveryRecords(req);
    if (response.code === 0 || response.code === 200) {
      deliveryRecords.value = response.data?.records || [];
      recordsTotal.value = response.data?.total || 0;
    } else {
      throw new Error(response.msg || '获取记录失败');
    }
  } catch (error: any) {
    console.error('加载自动发货记录失败:', error);
    deliveryRecords.value = [];
    recordsTotal.value = 0;
  } finally {
    recordsLoading.value = false;
  }
};

// 记录分页变化
const handleRecordsPageChange = (page: number) => {
  recordsPageNum.value = page;
  loadDeliveryRecords();
};

// 记录每页数量变化
const handleRecordsSizeChange = (size: number) => {
  recordsPageSize.value = size;
  recordsPageNum.value = 1;
  loadDeliveryRecords();
};

// 获取状态标签类型
const getRecordStatusType = (state: number) => {
  return state === 1 ? 'success' : 'danger';
};

// 获取状态文本
const getRecordStatusText = (state: number) => {
  return state === 1 ? '成功' : '失败';
};

// 确认已发货
const handleConfirmShipment = async (record: any) => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  if (!record.orderId) {
    showError('该记录没有订单ID，无法确认已发货');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确定要确认已发货吗？订单ID: ${record.orderId}`,
      '确认已发货',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const req: ConfirmShipmentReq = {
      xianyuAccountId: selectedAccountId.value,
      orderId: record.orderId
    };

    const response = await confirmShipment(req);
    if (response.code === 0 || response.code === 200) {
      showSuccess(response.data || '确认已发货成功');
      // 刷新记录列表
      await loadDeliveryRecords();
    } else {
      // 检查是否是token过期错误
      if (response.msg && (response.msg.includes('Token') || response.msg.includes('令牌'))) {
        throw new Error('Cookie已过期，请重新扫码登录获取新的Cookie');
      }
      throw new Error(response.msg || '确认已发货失败');
    }
  } catch (error: any) {
    if (error === 'cancel') {
      // 用户取消操作
      return;
    }
    console.error('确认已发货失败:', error);
    showError(error.message || '确认已发货失败');
  }
};

// 从消息内容中提取订单ID
const extractOrderId = (content: string): string | null => {
  try {
    // 尝试解析 JSON
    const data = JSON.parse(content);
    
    // 从 reminderUrl 中提取订单ID
    // 格式: fleamarket://order_detail?id=3052762719755595568&role=seller
    const reminderUrl = data?.['1']?.['6']?.['10']?.reminderUrl || '';
    const match = reminderUrl.match(/[?&]id=(\d+)/);
    if (match && match[1]) {
      return match[1];
    }
    
    // 如果 reminderUrl 中没有，尝试从 targetUrl 中提取
    const targetUrl = data?.['1']?.['6']?.['5']?.['1']?.['1']?.['1']?.main?.targetUrl || '';
    const match2 = targetUrl.match(/[?&]id=(\d+)/);
    if (match2 && match2[1]) {
      return match2[1];
    }
    
    return null;
  } catch (error) {
    console.error('解析订单ID失败:', error);
    return null;
  }
};

// 触发自动发货
const handleTriggerDelivery = async (record: any) => {
  if (!selectedAccountId.value || !selectedGoods.value) {
    showInfo('请先选择账号和商品');
    return;
  }

  if (!record.orderId) {
    showError('该记录没有订单ID，无法触发发货');
    return;
  }

  // 检查发货内容是否为空
  if (!configForm.value.autoDeliveryContent || !configForm.value.autoDeliveryContent.trim()) {
    showError('请配置发货内容！');
    return;
  }

  try {
    await ElMessageBox.confirm(
      `确认触发自动发货流程吗？订单ID: ${record.orderId}`,
      '触发发货',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const req: TriggerAutoDeliveryReq = {
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: selectedGoods.value.item.xyGoodId,
      orderId: record.orderId
    };

    const response = await triggerAutoDelivery(req);
    if (response.code === 0 || response.code === 200) {
      showSuccess(response.data || '触发发货成功');
      // 刷新记录列表
      await loadDeliveryRecords();
    } else {
      throw new Error(response.msg || '触发发货失败');
    }
  } catch (error: any) {
    if (error === 'cancel') {
      return;
    }
    console.error('触发发货失败:', error);
    showError(error.message || '触发发货失败');
  }
};

onMounted(() => {
  loadAccounts();
});
</script>

<template>
  <div class="auto-delivery-page">
    <div class="page-header">
      <h1 class="page-title">自动发货配置</h1>
      <div class="header-actions">
        <span class="account-label">选择闲鱼账号</span>
        <el-select
          v-model="selectedAccountId"
          placeholder="选择账号"
          style="width: 200px"
          @change="handleAccountChange"
        >
          <el-option
            v-for="account in accounts"
            :key="account.id"
            :label="account.accountNote || account.unb"
            :value="account.id"
          />
        </el-select>
      </div>
    </div>

    <div class="content-container">
      <!-- 左侧商品列表 -->
      <div class="goods-panel">
        <el-card class="goods-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">商品列表</span>
              <span class="card-subtitle">共 {{ goodsList.length }} 件商品</span>
            </div>
          </template>

          <div class="goods-list" v-loading="loading">
            <div
              v-for="goods in goodsList"
              :key="goods.item.xyGoodId"
              :id="`goods-item-${goods.item.id}-${Math.random().toString(36).substr(2, 9)}`"
              class="goods-item"
              :class="{ active: selectedGoods?.item.xyGoodId === goods.item.xyGoodId }"
              @click="selectGoods(goods)"
            >
              <el-image
                :src="goods.item.coverPic"
                fit="cover"
                class="goods-image"
              />
              <div class="goods-info">
                <div class="goods-title">{{ goods.item.title }}</div>
                <div class="goods-meta">
                  <span class="goods-price">{{ formatPrice(goods.item.soldPrice) }}</span>
                  <el-tag :type="getStatusType(goods.item.status)" size="small">
                    {{ getStatusText(goods.item.status) }}
                  </el-tag>
                </div>
              </div>
            </div>

            <div v-if="goodsList.length === 0 && !loading" class="empty-goods">
              <el-empty description="暂无商品" />
            </div>
          </div>
        </el-card>
      </div>

      <!-- 右侧配置面板 -->
      <div class="config-panel">
        <el-card class="config-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">自动发货配置</span>
              <span class="card-subtitle" v-if="!selectedGoods">
                请选择商品
              </span>
            </div>
          </template>

          <div class="config-form" v-if="selectedGoods" :class="{ 'records-expanded': recordsExpanded }">
            <div class="config-content" v-show="!recordsExpanded">
              <div class="goods-title-section">
                <div class="goods-title-text">{{ selectedGoods.item.title }}</div>
                <el-button
                  type="primary"
                  size="small"
                  @click="viewGoodsDetail"
                >
                  查看商品详情
                </el-button>
              </div>

              <el-form :model="configForm" label-width="100px">
                <el-form-item label="自动发货">
                  <el-switch
                    v-model="selectedGoods.xianyuAutoDeliveryOn"
                    :active-value="1"
                    :inactive-value="0"
                    @change="toggleAutoDelivery"
                  />
                  <span class="switch-label">
                    {{ selectedGoods.xianyuAutoDeliveryOn === 1 ? '已开启' : '已关闭' }}
                  </span>
                </el-form-item>

                <el-form-item label="发货类型">
                  <el-radio-group v-model="configForm.type">
                    <el-radio :value="1">文本内容</el-radio>
                    <el-radio :value="2">自定义</el-radio>
                  </el-radio-group>
                </el-form-item>

                <el-form-item label="发货内容">
                  <el-input
                    v-model="configForm.autoDeliveryContent"
                    type="textarea"
                    :rows="8"
                    placeholder="请输入自动发货内容，买家下单后将自动发送此内容"
                    maxlength="1000"
                    show-word-limit
                  />
                </el-form-item>

                <el-form-item label="自动确认发货">
                  <el-switch
                    v-model="configForm.autoConfirmShipment"
                    :active-value="1"
                    :inactive-value="0"
                    :disabled="selectedGoods.xianyuAutoDeliveryOn !== 1"
                  />
                  <span class="switch-label">
                    {{ configForm.autoConfirmShipment === 1 ? '已开启' : '已关闭' }}
                  </span>
                  <div class="form-tip">
                    {{ selectedGoods.xianyuAutoDeliveryOn === 1 
                      ? '开启后，自动发货成功将自动确认已发货' 
                      : '需要先开启自动发货' }}
                  </div>
                </el-form-item>

                <el-form-item>
                  <div class="save-config-container">
                    <el-button type="primary" :loading="saving" @click="saveConfig">
                      保存配置
                    </el-button>
                    <span v-if="currentConfig" class="last-update-time">
                      上次更新: {{ formatTime(currentConfig.updateTime) }}
                    </span>
                  </div>
                </el-form-item>
              </el-form>
            </div>

            <!-- 自动发货记录表格 -->
            <div class="delivery-records-section">
              <div class="records-header">
                <div class="records-info">
                  <span class="records-title">自动发货记录</span>
                  <span class="records-count">共 {{ recordsTotal }} 条记录</span>
                </div>
                <div class="records-actions">
                  <div class="records-pagination" v-if="recordsTotal > 0 && !recordsExpanded">
                    <el-pagination
                      v-model:current-page="recordsPageNum"
                      v-model:page-size="recordsPageSize"
                      :page-sizes="[10, 20, 50, 100]"
                      :total="recordsTotal"
                      layout="sizes, prev, pager, next"
                      small
                      @size-change="handleRecordsSizeChange"
                      @current-change="handleRecordsPageChange"
                    />
                  </div>
                  <el-button
                    :icon="recordsExpanded ? 'ArrowDown' : 'ArrowUp'"
                    size="small"
                    @click="recordsExpanded = !recordsExpanded"
                  >
                    {{ recordsExpanded ? '收起' : '展开' }}
                  </el-button>
                </div>
              </div>
              
              <div class="records-table-wrapper" v-loading="recordsLoading">
                <el-table
                  :data="deliveryRecords"
                  stripe
                  style="width: 100%"
                  :max-height="recordsExpanded ? 'calc(100vh - 250px)' : 400"
                >
                  <el-table-column type="index" label="序号" width="60" align="center" />
                  <el-table-column prop="orderId" label="订单ID" width="180">
                    <template #default="{ row }">
                      <span class="order-id">{{ row.orderId || '-' }}</span>
                    </template>
                  </el-table-column>
                  <el-table-column prop="buyerUserId" label="买家ID" width="120">
                    <template #default="{ row }">
                      {{ row.buyerUserId || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="buyerUserName" label="买家名称" width="120">
                    <template #default="{ row }">
                      {{ row.buyerUserName || '-' }}
                    </template>
                  </el-table-column>
                  <el-table-column prop="content" label="发货内容" min-width="200">
                    <template #default="{ row }">
                      <div class="content-text">{{ row.content || '-' }}</div>
                    </template>
                  </el-table-column>
                  <el-table-column prop="state" label="自动发货结果" width="120" align="center">
                    <template #default="{ row }">
                      <el-tag :type="getRecordStatusType(row.state)" size="small">
                        {{ getRecordStatusText(row.state) }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="createTime" label="发货时间" width="180">
                    <template #default="{ row }">
                      {{ formatTime(row.createTime) }}
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="100" align="center">
                    <template #default="{ row }">
                      <el-button
                        type="primary"
                        size="small"
                        @click="handleTriggerDelivery(row)"
                      >
                        发货
                      </el-button>
                    </template>
                  </el-table-column>
                  <template #empty>
                    <el-empty description="暂无发货记录" :image-size="80" />
                  </template>
                </el-table>
                
                <div class="pagination-container-bottom" v-if="recordsTotal > 0 && recordsExpanded">
                  <el-pagination
                    v-model:current-page="recordsPageNum"
                    v-model:page-size="recordsPageSize"
                    :page-sizes="[10, 20, 50, 100]"
                    :total="recordsTotal"
                    layout="total, sizes, prev, pager, next, jumper"
                    small
                    @size-change="handleRecordsSizeChange"
                    @current-change="handleRecordsPageChange"
                  />
                </div>
              </div>
            </div>
          </div>

          <div v-else class="empty-config">
            <el-empty description="请选择左侧商品进行配置" />
          </div>
        </el-card>
      </div>
    </div>

    <!-- 商品详情对话框 -->
    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
    />
  </div>
</template>

<style scoped>
.auto-delivery-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 15px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 10px;
  align-items: center;
}

.account-label {
  font-size: 14px;
  color: #606266;
  font-weight: 500;
}

.content-container {
  flex: 1;
  display: flex;
  gap: 15px;
  min-height: 0;
}

.goods-panel {
  flex: 1;
  min-width: 0;
  max-width: 400px;
}

.config-panel {
  flex: 2;
  min-width: 0;
}

.goods-card,
.config-card {
  height: 100%;
  display: flex;
  flex-direction: column;
}



.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 13px;
  color: #909399;
}

.goods-list {
  flex: 1;
  overflow-y: auto;
}

.goods-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
}

.goods-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.goods-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.goods-image {
  width: 50px;
  height: 50px;
  border-radius: 3px;
  margin-right: 10px;
  flex-shrink: 0;
}

.goods-info {
  flex: 1;
  min-width: 0;
}

.goods-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.goods-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.goods-price {
  font-size: 15px;
  font-weight: 600;
  color: #f56c6c;
}

.empty-goods,
.empty-config {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
}

.config-form {
  padding: 0;
}

.config-content {
  margin-bottom: 20px;
}

.goods-title-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 15px;
  margin-bottom: 15px;
  padding-top: 5px;
}

.goods-title-text {
  flex: 1;
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.config-form .el-form-item:first-child {
  margin-bottom: 20px;
}

.config-time {
  color: #909399;
  font-size: 13px;
}

.switch-label {
  margin-left: 10px;
  font-size: 14px;
  color: #606266;
}

.form-tip {
  margin-left: 10px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.save-config-container {
  display: flex;
  align-items: center;
  gap: 15px;
}

.last-update-time {
  font-size: 12px;
  color: #909399;
}

.delivery-records-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.config-form.records-expanded .delivery-records-section {
  margin-top: 0;
  padding-top: 0;
  border-top: none;
}

.records-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 15px;
}

.records-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.records-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
}

.records-count {
  font-size: 13px;
  color: #909399;
}

.records-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.records-pagination {
  flex-shrink: 0;
}

.records-table-wrapper {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 15px;
}

.config-form.records-expanded .records-table-wrapper {
  margin-bottom: 0;
}

.pagination-container-bottom {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 12px 15px;
  background-color: #fafafa;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
}

.buyer-info {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.buyer-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.buyer-id {
  font-size: 12px;
  color: #909399;
}

.content-text {
  font-size: 13px;
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
}

.order-id {
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}
</style>
