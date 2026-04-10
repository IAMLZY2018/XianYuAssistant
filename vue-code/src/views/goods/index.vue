<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRouter } from 'vue-router';
import { getAccountList } from '@/api/account';
import { getGoodsList, refreshGoods, getGoodsDetail, updateAutoDeliveryStatus, updateAutoReplyStatus, deleteItem } from '@/api/goods';
import { showSuccess, showError, showInfo, showConfirm } from '@/utils';
import type { Account } from '@/types';
import type { GoodsItemWithConfig } from '@/api/goods';
import GoodsDetailDialog from './components/GoodsDetailDialog.vue';

const router = useRouter();

const loading = ref(false);
const refreshing = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const statusFilter = ref<string>('');
const goodsList = ref<GoodsItemWithConfig[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);

// 表格高度
const tableHeight = ref<number>(500);

// 商品详情对话框
const detailDialogVisible = ref(false);
const selectedGoodsId = ref<string>('');

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
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
    const params: any = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    };

    if (statusFilter.value !== '') {
      params.status = parseInt(statusFilter.value);
    }

    const response = await getGoodsList(params);
    if (response.code === 0 || response.code === 200) {
      goodsList.value = response.data?.itemsWithConfig || [];
      total.value = response.data?.totalCount || 0;
    }
  } catch (error: any) {
    console.error('加载商品列表失败:', error);
    goodsList.value = [];
  } finally {
    loading.value = false;
  }
};

// 刷新商品数据
const handleRefresh = async () => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  refreshing.value = true;
  try {
    const response = await refreshGoods(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      // 检查业务逻辑是否成功
      if (response.data && response.data.success) {
        showSuccess('商品数据刷新成功');
        await loadGoods();
      } else {
        showError(response.data?.message || '刷新商品数据失败');
      }
    }
  } catch (error: any) {
    console.error('刷新商品数据失败:', error);
  } finally {
    refreshing.value = false;
  }
};

// 账号变更
const handleAccountChange = () => {
  currentPage.value = 1;
  loadGoods();
};

// 状态筛选
const handleStatusFilter = () => {
  currentPage.value = 1;
  loadGoods();
};

// 分页变更
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadGoods();
};

// 查看详情
const handleViewDetail = (xyGoodId: string) => {
  selectedGoodsId.value = xyGoodId;
  detailDialogVisible.value = true;
};

// 删除商品
const handleDelete = async (xyGoodId: string, title: string) => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }
  
  try {
    await showConfirm(`确定要删除商品 "${title}" 吗？此操作不可恢复。`, '删除确认');
    
    const response = await deleteItem({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: xyGoodId
    });
    
    if (response.code === 0 || response.code === 200) {
      showSuccess('商品删除成功');
      // 重新加载商品列表
      await loadGoods();
    } else {
      throw new Error(response.msg || '删除失败');
    }
  } catch (error: any) {
    if (error === 'cancel') {
      // 用户取消操作
      return;
    }
    console.error('删除失败:', error);
  }
};

// 配置自动发货
const handleConfigAutoDelivery = (item: GoodsItemWithConfig) => {
  router.push({
    path: '/auto-delivery',
    query: {
      accountId: selectedAccountId.value?.toString(),
      goodsId: item.item.xyGoodId
    }
  });
};

// 切换自动发货
const handleToggleAutoDelivery = async (item: GoodsItemWithConfig, value: boolean) => {
  if (!selectedAccountId.value) return;

  try {
    const response = await updateAutoDeliveryStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: item.item.xyGoodId,
      xianyuAutoDeliveryOn: value ? 1 : 0
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess(`自动发货${value ? '开启' : '关闭'}成功`);
      item.xianyuAutoDeliveryOn = value ? 1 : 0;
    } else {
      throw new Error(response.msg || '操作失败');
    }
  } catch (error: any) {
    console.error('操作失败:', error);
    // 恢复开关状态
    item.xianyuAutoDeliveryOn = value ? 0 : 1;
  }
};

// 切换自动回复
const handleToggleAutoReply = async (item: GoodsItemWithConfig, value: boolean) => {
  if (!selectedAccountId.value) return;

  try {
    const response = await updateAutoReplyStatus({
      xianyuAccountId: selectedAccountId.value,
      xyGoodsId: item.item.xyGoodId,
      xianyuAutoReplyOn: value ? 1 : 0
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess(`自动回复${value ? '开启' : '关闭'}成功`);
      item.xianyuAutoReplyOn = value ? 1 : 0;
    } else {
      throw new Error(response.msg || '操作失败');
    }
  } catch (error: any) {
    console.error('操作失败:', error);
    // 恢复开关状态
    item.xianyuAutoReplyOn = value ? 0 : 1;
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

// 计算表格高度
const calculateTableHeight = () => {
  // 窗口高度 - 页面头部(约80px) - 卡片头部(约60px) - 分页(约60px) - 底部边距(约60px)
  const height = window.innerHeight - 80 - 60 - 60 - 60;
  tableHeight.value = Math.max(height, 400); // 最小高度400px
};

onMounted(() => {
  loadAccounts();
  calculateTableHeight();
  window.addEventListener('resize', calculateTableHeight);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', calculateTableHeight);
});
</script>

<template>
  <div class="goods-page">
    <div class="page-header">
      <h1 class="page-title">商品管理</h1>
      <div class="header-actions">
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
        
        <el-select
          v-model="statusFilter"
          placeholder="全部状态"
          style="width: 150px"
          clearable
          @change="handleStatusFilter"
        >
          <el-option label="在售商品" value="0" />
          <el-option label="已下架" value="1" />
          <el-option label="已售出" value="2" />
        </el-select>
        
        <el-button @click="loadGoods">刷新列表</el-button>
        <el-button type="primary" :loading="refreshing" @click="handleRefresh">
          同步闲鱼商品
        </el-button>
      </div>
    </div>

    <el-card class="goods-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">商品列表</span>
          <span class="card-subtitle">共 {{ total }} 件商品</span>
        </div>
      </template>

      <el-table
        v-loading="loading"
        :data="goodsList"
        stripe
        style="width: 100%"
        size="small"
        :height="tableHeight"
      >
        <el-table-column type="index" label="序号" width="60" align="center" />
        
        <el-table-column prop="item.xyGoodId" label="商品ID" width="140">
          <template #default="{ row }">
            <div class="goods-id">{{ row.item.xyGoodId }}</div>
          </template>
        </el-table-column>
        
        <el-table-column label="商品图片" width="80" align="center">
          <template #default="{ row }">
            <el-image
              :src="row.item.coverPic"
              fit="cover"
              class="goods-image"
              :preview-src-list="[row.item.coverPic]"
            />
          </template>
        </el-table-column>
        
        <el-table-column prop="item.title" label="商品标题" min-width="200" show-overflow-tooltip />
        
        <el-table-column label="价格" width="100" align="right">
          <template #default="{ row }">
            <span class="goods-price">{{ formatPrice(row.item.soldPrice) }}</span>
          </template>
        </el-table-column>
        
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.item.status)" size="small">
              {{ getStatusText(row.item.status) }}
            </el-tag>
          </template>
        </el-table-column>
        
        <el-table-column label="自动发货" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.xianyuAutoDeliveryOn === 1"
              @change="(val: boolean) => handleToggleAutoDelivery(row, val)"
              size="small"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="自动回复" width="80" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.xianyuAutoReplyOn === 1"
              @change="(val: boolean) => handleToggleAutoReply(row, val)"
              size="small"
            />
          </template>
        </el-table-column>
        
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              link
              size="small"
              @click="handleViewDetail(row.item.xyGoodId)"
            >
              查看详情
            </el-button>
            <el-button
              type="success"
              link
              size="small"
              @click="handleConfigAutoDelivery(row)"
            >
              配置自动发货
            </el-button>
            <el-button
              type="danger"
              link
              size="small"
              @click="handleDelete(row.item.xyGoodId, row.item.title)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="handlePageChange"
        />
      </div>
    </el-card>

    <!-- 商品详情对话框 -->
    <GoodsDetailDialog
      v-model="detailDialogVisible"
      :goods-id="selectedGoodsId"
      :account-id="selectedAccountId"
      @refresh="loadGoods"
    />
  </div>
</template>

<style scoped>
.goods-page {
  padding: 20px 20px 40px 20px;
  min-height: 100vh;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.goods-card {
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

.goods-id {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 10px;
  color: #606266;
  word-break: break-all;
}

.goods-image {
  width: 50px;
  height: 50px;
  border-radius: 3px;
}

.goods-price {
  font-size: 14px;
  font-weight: 600;
  color: #f56c6c;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 15px 0;
  margin-top: 15px;
  border-top: 1px solid #ebeef5;
  background-color: #fff;
}
</style>
