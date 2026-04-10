<template>
  <div class="orders-container">
    <el-card class="filter-card">
      <el-form :inline="true" :model="queryParams" class="filter-form">
        <el-form-item label="商品ID">
          <el-input
            v-model="queryParams.xyGoodsId"
            placeholder="请输入商品ID"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="订单状态">
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
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table v-loading="loading" :data="orderList" border style="width: 100%">
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
              <template v-if="row.orderStatus === 2">
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
              <template v-else-if="row.orderStatus === 3">
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

      <el-pagination
        v-model:current-page="queryParams.pageNum"
        v-model:page-size="queryParams.pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleQuery"
        @current-change="handleQuery"
        style="margin-top: 20px; justify-content: flex-end"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'

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
    case 1: return 'warning'
    case 2: return 'primary'
    case 3: return 'success'
    case 4: return 'success'
    case 5: return 'info'
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
      row.orderStatus = 3
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
  handleQuery()
})
</script>

<style scoped>
.orders-container {
  padding: 20px;
}

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.table-card {
  margin-bottom: 20px;
}
</style>