<template>
  <div class="orders-page">
    <!-- 桌面端：页面头部显示筛选和操作按钮 -->
    <div v-if="!isMobile" class="page-header">
      <div class="header-actions">
        <el-input
          v-model="queryParams.xyGoodsId"
          placeholder="请输入商品ID"
          clearable
          style="width: 200px"
        />

        <el-select
          v-model="queryParams.orderStatus"
          placeholder="全部状态"
          clearable
          style="width: 150px"
        >
          <el-option label="待付款" :value="1" />
          <el-option label="待发货" :value="2" />
          <el-option label="已发货" :value="3" />
          <el-option label="已完成" :value="4" />
          <el-option label="已取消" :value="5" />
        </el-select>

        <el-button type="primary" @click="handleQuery">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
    </div>

    <el-card class="orders-card">
      <template #header>
        <div class="card-header">
          <div class="card-header-left">
            <span class="card-title">订单列表</span>
            <span class="card-subtitle">共 {{ total }} 条订单</span>
          </div>
          <!-- 手机端：按钮放在标题旁边 -->
          <div v-if="isMobile" class="card-header-actions">
            <el-button size="small" @click="showFilterDialog = true">筛选</el-button>
            <el-button size="small" type="primary" @click="handleQuery">查询</el-button>
          </div>
        </div>
      </template>

      <!-- 桌面端：表格展示 -->
      <el-table
        v-if="!isMobile"
        v-loading="loading"
        :data="orderList"
        border
        style="width: 100%"
      >
        <el-table-column prop="accountRemark" label="闲鱼账号备注" width="150" />
        <el-table-column prop="orderId" label="订单ID" width="200" />
        <el-table-column prop="goodsTitle" label="商品名称" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            {{ row.goodsTitle || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="sid" label="会话ID" width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" @click="copySId(row.sid)">{{ row.sid }}</el-link>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column prop="autoDeliverySuccess" label="自动发货" width="100">
          <template #default="{ row }">
            <el-tag :type="row.autoDeliverySuccess ? 'success' : 'info'">
              {{ row.autoDeliverySuccess ? '成功' : '未发货' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="orderStatusText" label="订单状态" width="150">
          <template #default="{ row }">
            <div style="display: flex; flex-direction: column; gap: 8px; align-items: center;">
              <!-- 待发货状态：显示状态标签 + 确认发货按钮 -->
              <template v-if="row.orderStatus === OrderStatus.WAITING_DELIVERY">
                <el-tag :type="getStatusType(row.orderStatus)">
                  {{ row.orderStatusText }}
                </el-tag>
                <el-button
                  type="success"
                  size="small"
                  @click="handleConfirmShipment(row)"
                  :loading="row.confirming"
                >
                  确认已发货
                </el-button>
              </template>
              <!-- 已发货状态：只显示已发货标签 -->
              <template v-else-if="row.orderStatus === OrderStatus.DELIVERED">
                <el-tag type="success">
                  已发货
                </el-tag>
              </template>
              <!-- 其他状态：显示状态标签 -->
              <template v-else>
                <el-tag v-if="row.orderStatusText" :type="getStatusType(row.orderStatus)">
                  {{ row.orderStatusText }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="buyerUserName" label="买家" width="120" />
      </el-table>

      <!-- 手机端：卡片列表展示 -->
      <div v-else class="mobile-orders-list" v-loading="loading">
        <div
          v-for="order in orderList"
          :key="order.id"
          class="mobile-order-item"
        >
          <div class="mobile-order-header">
            <div class="mobile-order-id">订单ID: {{ order.orderId }}</div>
            <el-tag :type="getStatusType(order.orderStatus)" size="small">
              {{ order.orderStatusText || '未知' }}
            </el-tag>
          </div>

          <div class="mobile-order-body">
            <div class="mobile-order-info">
              <div class="info-row">
                <span class="info-label">商品名称：</span>
                <span class="info-value">{{ order.goodsTitle || '-' }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">买家：</span>
                <span class="info-value">{{ order.buyerUserName }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">创建时间：</span>
                <span class="info-value">{{ formatTime(order.createTime) }}</span>
              </div>
              <div class="info-row">
                <span class="info-label">自动发货：</span>
                <el-tag :type="order.autoDeliverySuccess ? 'success' : 'info'" size="small">
                  {{ order.autoDeliverySuccess ? '成功' : '未发货' }}
                </el-tag>
              </div>
            </div>
          </div>

          <div class="mobile-order-footer">
            <el-button
              size="small"
              type="primary"
              link
              @click="copySId(order.sid)"
            >
              复制会话ID
            </el-button>
            <el-button
              v-if="order.orderStatus === OrderStatus.WAITING_DELIVERY"
              size="small"
              type="success"
              @click="handleConfirmShipment(order)"
              :loading="order.confirming"
            >
              确认已发货
            </el-button>
          </div>
        </div>

        <el-empty v-if="!loading && orderList.length === 0" description="暂无订单数据" />
      </div>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          :layout="isMobile ? 'prev, pager, next' : 'total, sizes, prev, pager, next, jumper'"
          @size-change="handleQuery"
          @current-change="handleQuery"
        />
      </div>
    </el-card>

    <!-- 手机端筛选弹窗 -->
    <el-dialog
      v-model="showFilterDialog"
      title="筛选条件"
      width="90%"
      :style="{ maxWidth: '400px' }"
    >
      <div class="filter-dialog-content">
        <div class="filter-item">
          <label class="filter-label">商品ID</label>
          <el-input
            v-model="queryParams.xyGoodsId"
            placeholder="请输入商品ID"
            clearable
          />
        </div>

        <div class="filter-item">
          <label class="filter-label">订单状态</label>
          <el-select
            v-model="queryParams.orderStatus"
            placeholder="全部状态"
            clearable
            style="width: 100%"
          >
            <el-option label="待付款" :value="1" />
            <el-option label="待发货" :value="2" />
            <el-option label="已发货" :value="3" />
            <el-option label="已完成" :value="4" />
            <el-option label="已取消" :value="5" />
          </el-select>
        </div>
      </div>

      <template #footer>
        <el-button @click="handleReset">重置</el-button>
        <el-button type="primary" @click="showFilterDialog = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { OrderStatus } from '@/constants/orderStatus'

interface OrderItem {
  id: number
  accountRemark: string
  orderId: string
  goodsTitle: string | null
  sid: string
  createTime: number
  autoDeliverySuccess: boolean
  orderStatus: number | null
  orderStatusText: string | null
  buyerUserName: string
  xyGoodsId: string
  xianyuAccountId: number
  confirming?: boolean  // 确认发货loading状态
}

interface QueryParams {
  xianyuAccountId?: number
  xyGoodsId?: string
  orderStatus?: number
  pageNum: number
  pageSize: number
}

const loading = ref(false)
const orderList = ref<OrderItem[]>([])
const total = ref(0)

const queryParams = ref<QueryParams>({
  pageNum: 1,
  pageSize: 20
})

// 手机端相关
const isMobile = ref(false)
const showFilterDialog = ref(false) // 筛选弹窗

// 检测屏幕宽度
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768
}

const handleQuery = async () => {
  loading.value = true
  try {
    const response = await axios.post('/api/order/list', queryParams.value)
    const res = response.data
    
    if (res.code === 200 || res.code === 0) {
      orderList.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.msg || '查询失败')
    }
  } catch (error: any) {
    ElMessage.error('查询订单列表失败: ' + (error.message || '未知错误'))
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  queryParams.value = {
    pageNum: 1,
    pageSize: 20
  }
  handleQuery()
}

const copySId = (sId: string) => {
  navigator.clipboard.writeText(sId).then(() => {
    ElMessage.success('会话ID已复制到剪贴板')
  })
}

const formatTime = (timestamp: number | null) => {
  if (!timestamp) return '-'
  
  const date = new Date(timestamp)
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  })
}

const getStatusType = (status: number | null) => {
  switch (status) {
    case OrderStatus.WAITING_PAYMENT: return 'warning'
    case OrderStatus.WAITING_DELIVERY: return 'primary'
    case OrderStatus.DELIVERED: return 'success'
    case OrderStatus.COMPLETED: return 'success'
    case OrderStatus.CLOSED: return 'info'
    default: return 'info'
  }
}

// 确认发货
const handleConfirmShipment = async (row: OrderItem) => {
  try {
    await ElMessageBox.confirm(
      `确认订单 ${row.orderId} 已发货吗？`,
      '确认发货',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )
    
    row.confirming = true
    const response = await axios.post('/api/order/confirmShipment', {
      xianyuAccountId: row.xianyuAccountId,
      orderId: row.orderId
    })
    
    const res = response.data
    if (res.code === 200 || res.code === 0) {
      ElMessage.success('确认发货成功')
      // 更新订单状态
      row.orderStatus = OrderStatus.DELIVERED
      row.orderStatusText = '已发货'
      // 刷新列表
      handleQuery()
    } else {
      ElMessage.error(res.msg || '确认发货失败')
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('确认发货失败: ' + (error.message || '未知错误'))
    }
  } finally {
    row.confirming = false
  }
}

onMounted(() => {
  checkScreenSize()
  handleQuery()
  window.addEventListener('resize', checkScreenSize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', checkScreenSize)
})
</script>

<style scoped>
.orders-page {
  padding: 20px 20px 40px 20px;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.orders-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.card-header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.card-header-actions {
  display: flex;
  gap: 8px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 12px;
  color: #909399;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 15px 0;
  margin-top: 15px;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
}

/* 手机端订单卡片列表 */
.mobile-orders-list {
  max-height: calc(100vh - 280px);
  overflow-y: auto;
  padding: 0 4px;
}

.mobile-order-item {
  padding: 12px;
  margin-bottom: 12px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #ebeef5;
}

.mobile-order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 10px;
}

.mobile-order-id {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
}

.mobile-order-body {
  margin-bottom: 10px;
}

.mobile-order-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-row {
  display: flex;
  align-items: center;
  font-size: 13px;
}

.info-label {
  color: #909399;
  flex-shrink: 0;
  min-width: 80px;
}

.info-value {
  color: #606266;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-order-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding-top: 10px;
  border-top: 1px solid #f0f0f0;
}

/* 筛选弹窗 */
.filter-dialog-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #606266;
}

/* 响应式布局 */
@media (max-width: 768px) {
  .orders-page {
    padding: 10px;
  }

  .orders-card {
    margin-bottom: 10px;
  }

  .pagination-container {
    padding: 10px 0;
    margin-top: 10px;
  }
}

@media (max-width: 480px) {
  .orders-page {
    padding: 8px;
  }

  .mobile-order-item {
    padding: 10px;
    margin-bottom: 8px;
  }

  .info-row {
    font-size: 12px;
  }

  .info-label {
    min-width: 70px;
  }
}
</style>