<script setup lang="ts">
import { ref, onMounted, computed, onUnmounted } from 'vue';
import { getAccountList } from '@/api/account';
import { queryOperationLogs, deleteOldLogs } from '@/api/operation-log';
import type { OperationLog } from '@/api/operation-log';
import type { Account } from '@/types';
import { showSuccess, showError, showInfo } from '@/utils';
import { ElMessageBox } from 'element-plus';
import { ArrowLeft } from '@element-plus/icons-vue';

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const logs = ref<OperationLog[]>([]);
const total = ref(0);
const page = ref(1);
const pageSize = ref(20);

// 筛选条件
const filterType = ref('');
const filterModule = ref('');
const filterStatus = ref<string | number>('');

// 手机端适配
const isMobile = ref(false);
const mobileView = ref<'accounts' | 'logs'>('accounts'); // 手机端当前视图：账号列表或操作记录列表
const selectedAccountForMobile = ref<Account | null>(null); // 手机端选中的账号

// 检测屏幕宽度
const checkScreenSize = () => {
  isMobile.value = window.innerWidth < 768;
  // 如果切换到桌面端，重置手机端视图状态
  if (!isMobile.value) {
    mobileView.value = 'accounts';
  }
};

// 手机端返回账号列表
const goBackToAccounts = () => {
  mobileView.value = 'accounts';
};

// 操作类型选项
const operationTypes = [
  { label: '全部', value: '' },
  { label: '扫码登录', value: 'LOGIN' },
  { label: 'WebSocket连接', value: 'WEBSOCKET_CONNECT' },
  { label: 'WebSocket断开', value: 'WEBSOCKET_DISCONNECT' },
  { label: '发送消息', value: 'SEND_MESSAGE' },
  { label: '接收消息', value: 'RECEIVE_MESSAGE' },
  { label: '自动发货', value: 'AUTO_DELIVERY' },
  { label: '自动回复', value: 'AUTO_REPLY' },
  { label: '确认收货', value: 'CONFIRM_SHIPMENT' },
  { label: 'Token刷新', value: 'TOKEN_REFRESH' },
  { label: 'Cookie更新', value: 'COOKIE_UPDATE' },
  { label: '商品同步', value: 'GOODS_SYNC' },
  { label: '消息同步', value: 'MESSAGE_SYNC' }
];

// 操作模块选项
const operationModules = [
  { label: '全部', value: '' },
  { label: '账号', value: 'ACCOUNT' },
  { label: '消息', value: 'MESSAGE' },
  { label: '订单', value: 'ORDER' },
  { label: '商品', value: 'GOODS' },
  { label: '系统', value: 'SYSTEM' }
];

// 操作状态选项
const operationStatuses = [
  { label: '全部', value: '' },
  { label: '成功', value: 1 },
  { label: '失败', value: 0 },
  { label: '部分成功', value: 2 }
];

// 当前选中的账号信息
const currentAccount = computed(() => {
  return accounts.value.find(acc => acc.id === selectedAccountId.value);
});

// 加载账号列表
const loadAccounts = async () => {
  loading.value = true;
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0]?.id ?? null;
        loadLogs();
      }
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
  } finally {
    loading.value = false;
  }
};

// 选择账号
const selectAccount = (accountId: number, account?: Account) => {
  selectedAccountId.value = accountId;
  page.value = 1;
  loadLogs();
  
  // 手机端：记录选中的账号并切换到操作记录视图
  if (isMobile.value && account) {
    selectedAccountForMobile.value = account;
    mobileView.value = 'logs';
  }
};

// 加载操作记录
const loadLogs = async () => {
  if (!selectedAccountId.value) return;
  
  loading.value = true;
  try {
    const response = await queryOperationLogs({
      accountId: selectedAccountId.value,
      operationType: filterType.value || undefined,
      operationModule: filterModule.value || undefined,
      operationStatus: filterStatus.value !== '' ? Number(filterStatus.value) : undefined,
      page: page.value,
      pageSize: pageSize.value
    });
    
    if (response.code === 0 || response.code === 200) {
      logs.value = response.data?.logs || [];
      total.value = response.data?.total || 0;
    } else {
      throw new Error(response.msg || '加载失败');
    }
  } catch (error: any) {
    console.error('加载操作记录失败:', error);
    showError('加载失败: ' + error.message);
  } finally {
    loading.value = false;
  }
};

// 筛选
const handleFilter = () => {
  page.value = 1;
  loadLogs();
};

// 重置筛选
const handleResetFilter = () => {
  filterType.value = '';
  filterModule.value = '';
  filterStatus.value = '';
  page.value = 1;
  loadLogs();
};

// 分页
const handlePageChange = (newPage: number) => {
  page.value = newPage;
  loadLogs();
};

const handleSizeChange = (newSize: number) => {
  pageSize.value = newSize;
  page.value = 1;
  loadLogs();
};

// 刷新
const handleRefresh = () => {
  loadLogs();
  showInfo('已刷新');
};

// 删除旧日志
const handleDeleteOld = async () => {
  try {
    await ElMessageBox.prompt('请输入要删除多少天之前的日志', '删除旧日志', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^\d+$/,
      inputErrorMessage: '请输入有效的天数'
    }).then(async ({ value }) => {
      const days = parseInt(value);
      const response = await deleteOldLogs(days);
      if (response.code === 0 || response.code === 200) {
        showSuccess(`成功删除${response.data}条记录`);
        loadLogs();
      } else {
        throw new Error(response.msg || '删除失败');
      }
    });
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除旧日志失败:', error);
      showError('删除失败: ' + error.message);
    }
  }
};

// 获取账号头像
const getAccountAvatar = (account: Account) => {
  return account.accountNote?.substring(0, 1) || account.id?.toString().substring(0, 1) || '?';
};

// 获取账号名称
const getAccountName = (account: Account) => {
  return account.accountNote || `账号${account.id}`;
};

// 获取操作类型文本
const getOperationTypeText = (type: string) => {
  const item = operationTypes.find(t => t.value === type);
  return item?.label || type;
};

// 获取操作类型标签类型
const getOperationTypeTag = (type: string) => {
  const typeMap: Record<string, string> = {
    'LOGIN': 'success',
    'WEBSOCKET_CONNECT': 'success',
    'WEBSOCKET_DISCONNECT': 'warning',
    'SEND_MESSAGE': 'primary',
    'RECEIVE_MESSAGE': 'info',
    'AUTO_DELIVERY': 'success',
    'AUTO_REPLY': 'success',
    'CONFIRM_SHIPMENT': 'success',
    'TOKEN_REFRESH': 'warning',
    'COOKIE_UPDATE': 'warning',
    'GOODS_SYNC': 'info',
    'MESSAGE_SYNC': 'info'
  };
  return typeMap[type] || 'info';
};

// 获取操作状态文本
const getStatusText = (status: number) => {
  const statusMap: Record<number, string> = {
    0: '失败',
    1: '成功',
    2: '部分成功'
  };
  return statusMap[status] || '未知';
};

// 获取操作状态标签类型
const getStatusTag = (status: number) => {
  const statusMap: Record<number, string> = {
    0: 'danger',
    1: 'success',
    2: 'warning'
  };
  return statusMap[status] || 'info';
};

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp);
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit'
  });
};

// 格式化耗时
const formatDuration = (ms?: number) => {
  if (!ms) return '-';
  if (ms < 1000) return `${ms}ms`;
  return `${(ms / 1000).toFixed(2)}s`;
};

// 查看详情
const viewDetail = (log: OperationLog) => {
  let content = `<div style="text-align: left;">`;
  content += `<p><strong>操作描述：</strong>${log.operationDesc || '-'}</p>`;
  if (log.targetType) {
    content += `<p><strong>目标类型：</strong>${log.targetType}</p>`;
  }
  if (log.targetId) {
    content += `<p><strong>目标ID：</strong>${log.targetId}</p>`;
  }
  if (log.requestParams) {
    content += `<p><strong>请求参数：</strong></p><pre style="max-height: 200px; overflow: auto; background: #f5f5f5; padding: 10px; border-radius: 4px;">${log.requestParams}</pre>`;
  }
  if (log.responseResult) {
    content += `<p><strong>响应结果：</strong></p><pre style="max-height: 200px; overflow: auto; background: #f5f5f5; padding: 10px; border-radius: 4px;">${log.responseResult}</pre>`;
  }
  if (log.errorMessage) {
    content += `<p><strong>错误信息：</strong></p><pre style="max-height: 200px; overflow: auto; background: #fff3f3; padding: 10px; border-radius: 4px; color: #f56c6c;">${log.errorMessage}</pre>`;
  }
  if (log.durationMs) {
    content += `<p><strong>耗时：</strong>${formatDuration(log.durationMs)}</p>`;
  }
  content += `</div>`;
  
  ElMessageBox({
    title: '操作详情',
    message: content,
    dangerouslyUseHTMLString: true,
    confirmButtonText: '关闭',
    customClass: 'operation-detail-dialog'
  });
};

onMounted(() => {
  loadAccounts();
  // 检测屏幕宽度
  checkScreenSize();
  window.addEventListener('resize', checkScreenSize);
});

onUnmounted(() => {
  window.removeEventListener('resize', checkScreenSize);
});
</script>

<template>
  <div class="operation-log-page" :class="{ 'mobile-mode': isMobile }">
    <!-- 桌面端布局 -->
    <template v-if="!isMobile">
      <div class="page-header">
        <h1 class="page-title">操作记录</h1>
      </div>

      <div class="operation-log-container">
        <!-- 左侧账号列表 -->
        <el-card class="account-panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">闲鱼账号</span>
            </div>
          </template>
          
          <div v-loading="loading" class="account-list">
            <div
              v-for="account in accounts"
              :key="account.id"
              class="account-item"
              :class="{ active: selectedAccountId === account.id }"
              @click="selectAccount(account.id, account)"
            >
              <div class="account-avatar">{{ getAccountAvatar(account) }}</div>
              <div class="account-info">
                <div class="account-name">{{ getAccountName(account) }}</div>
                <div class="account-id">ID: {{ account.id }}</div>
              </div>
            </div>
            
            <el-empty
              v-if="!loading && accounts.length === 0"
              description="暂无账号数据"
              :image-size="80"
            />
          </div>
        </el-card>

        <!-- 右侧操作记录 -->
        <el-card class="logs-panel">
          <template #header>
            <div class="panel-header">
              <span class="panel-title">操作记录</span>
              <div class="header-actions">
                <el-button size="small" @click="handleRefresh" :icon="'Refresh'">刷新</el-button>
                <el-button size="small" type="danger" @click="handleDeleteOld" :icon="'Delete'">删除旧日志</el-button>
              </div>
            </div>
          </template>
          
          <div v-if="!selectedAccountId" class="empty-state">
            <el-empty description="请选择一个账号查看操作记录" :image-size="100" />
          </div>

          <div v-else class="logs-content">
            <!-- 筛选条件 -->
            <div class="filter-bar">
              <el-select v-model="filterType" placeholder="操作类型" clearable style="width: 150px;">
                <el-option
                  v-for="item in operationTypes"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              
              <el-select v-model="filterModule" placeholder="操作模块" clearable style="width: 120px;">
                <el-option
                  v-for="item in operationModules"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              
              <el-select v-model="filterStatus" placeholder="操作状态" clearable style="width: 120px;">
                <el-option
                  v-for="item in operationStatuses"
                  :key="item.value"
                  :label="item.label"
                  :value="item.value"
                />
              </el-select>
              
              <el-button type="primary" @click="handleFilter">筛选</el-button>
              <el-button @click="handleResetFilter">重置</el-button>
            </div>

            <!-- 操作记录表格 -->
            <el-table
              v-loading="loading"
              :data="logs"
              stripe
              style="width: 100%"
              :height="500"
            >
              <el-table-column prop="id" label="ID" width="80" />
              
              <el-table-column label="操作类型" width="140">
                <template #default="{ row }">
                  <el-tag :type="getOperationTypeTag(row.operationType)" size="small">
                    {{ getOperationTypeText(row.operationType) }}
                  </el-tag>
                </template>
              </el-table-column>
              
              <el-table-column prop="operationModule" label="模块" width="100" />
              
              <el-table-column prop="operationDesc" label="操作描述" min-width="200" show-overflow-tooltip />
              
              <el-table-column label="状态" width="100">
                <template #default="{ row }">
                  <el-tag :type="getStatusTag(row.operationStatus)" size="small">
                    {{ getStatusText(row.operationStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              
              <el-table-column label="耗时" width="100">
                <template #default="{ row }">
                  {{ formatDuration(row.durationMs) }}
                </template>
              </el-table-column>
              
              <el-table-column label="时间" width="180">
                <template #default="{ row }">
                  {{ formatTime(row.createTime) }}
                </template>
              </el-table-column>
              
              <el-table-column label="操作" width="100" fixed="right">
                <template #default="{ row }">
                  <el-button type="primary" size="small" link @click="viewDetail(row)">
                    详情
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <!-- 分页 -->
            <div class="pagination-container">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="pageSize"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @current-change="handlePageChange"
                @size-change="handleSizeChange"
              />
            </div>
          </div>
        </el-card>
      </div>
    </template>

    <!-- 手机端布局 -->
    <template v-else>
      <!-- 手机端账号列表视图 -->
      <div v-show="mobileView === 'accounts'" class="mobile-accounts-view">
        <div class="mobile-header">
          <h1 class="mobile-title">操作记录</h1>
        </div>
        
        <div class="mobile-accounts-list-title">
          <span>闲鱼账号</span>
          <span class="accounts-count">共 {{ accounts.length }} 个</span>
        </div>
        
        <div v-loading="loading" class="mobile-accounts-list">
          <div
            v-for="account in accounts"
            :key="account.id"
            class="mobile-account-item"
            @click="selectAccount(account.id, account)"
          >
            <div class="mobile-account-avatar">{{ getAccountAvatar(account) }}</div>
            <div class="mobile-account-info">
              <div class="mobile-account-name">{{ getAccountName(account) }}</div>
              <div class="mobile-account-id">ID: {{ account.id }}</div>
            </div>
          </div>
          
          <el-empty
            v-if="!loading && accounts.length === 0"
            description="暂无账号数据"
            :image-size="80"
          />
        </div>
      </div>

      <!-- 手机端操作记录视图 -->
      <div v-show="mobileView === 'logs'" class="mobile-logs-view">
        <div class="mobile-header">
          <el-button 
            :icon="ArrowLeft" 
            size="small" 
            @click="goBackToAccounts"
          >
            返回
          </el-button>
          <div class="mobile-selected-account" v-if="selectedAccountForMobile">
            <div class="selected-account-avatar">{{ getAccountAvatar(selectedAccountForMobile) }}</div>
            <span class="selected-account-name">{{ getAccountName(selectedAccountForMobile) }}</span>
          </div>
          <el-button size="small" @click="handleRefresh">刷新</el-button>
        </div>
        
        <!-- 手机端筛选 -->
        <div class="mobile-filter-bar">
          <el-select v-model="filterType" placeholder="类型" clearable size="small" style="width: 100px;">
            <el-option
              v-for="item in operationTypes"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          
          <el-select v-model="filterStatus" placeholder="状态" clearable size="small" style="width: 100px;">
            <el-option
              v-for="item in operationStatuses"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
          
          <el-button type="primary" size="small" @click="handleFilter">筛选</el-button>
        </div>
        
        <div class="mobile-logs-list" v-loading="loading">
          <!-- 操作记录卡片 -->
          <div 
            v-for="log in logs" 
            :key="log.id"
            class="mobile-log-card"
            @click="viewDetail(log)"
          >
            <div class="log-card-header">
              <el-tag :type="getOperationTypeTag(log.operationType)" size="small">
                {{ getOperationTypeText(log.operationType) }}
              </el-tag>
              <el-tag :type="getStatusTag(log.operationStatus)" size="small">
                {{ getStatusText(log.operationStatus) }}
              </el-tag>
            </div>
            
            <div class="log-card-desc">
              {{ log.operationDesc || '-' }}
            </div>
            
            <div class="log-card-info">
              <div class="log-info-row">
                <span class="log-label">模块：</span>
                <span class="log-value">{{ log.operationModule || '-' }}</span>
              </div>
              <div class="log-info-row">
                <span class="log-label">耗时：</span>
                <span class="log-value">{{ formatDuration(log.durationMs) }}</span>
              </div>
            </div>
            
            <div class="log-card-footer">
              <span class="log-time">{{ formatTime(log.createTime) }}</span>
              <el-button type="primary" size="small">查看详情</el-button>
            </div>
          </div>
          
          <el-empty
            v-if="!loading && logs.length === 0"
            description="暂无操作记录"
            :image-size="80"
          />
        </div>
        
        <!-- 手机端分页 -->
        <div class="mobile-pagination" v-if="total > 0">
          <el-pagination
            v-model:current-page="page"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next"
            small
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.operation-log-page {
  padding: 20px;
  background-color: #f5f7fa;
  min-height: 100vh;
}

/* 手机端页面样式 */
.operation-log-page.mobile-mode {
  padding: 0;
  height: 100vh;
  overflow: hidden;
}

.page-header {
  margin-bottom: 20px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.operation-log-container {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  height: calc(100vh - 100px);
}

.account-panel,
.logs-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.account-list {
  overflow-y: auto;
  max-height: calc(100vh - 200px);
}

.account-item {
  display: flex;
  align-items: center;
  padding: 12px;
  margin-bottom: 8px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
  border: 2px solid transparent;
}

.account-item:hover {
  background-color: #f5f7fa;
}

.account-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: bold;
  margin-right: 12px;
  flex-shrink: 0;
}

.account-info {
  flex: 1;
  min-width: 0;
}

.account-name {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-id {
  font-size: 12px;
  color: #909399;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 400px;
}

.logs-content {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.pagination-container {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 手机端样式 */
.mobile-accounts-view,
.mobile-logs-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
}

.mobile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 15px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
  gap: 10px;
}

.mobile-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.mobile-accounts-list-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  background: #fff;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  border-bottom: 1px solid #ebeef5;
}

.accounts-count {
  font-size: 12px;
  color: #909399;
}

.mobile-accounts-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  padding-bottom: 30px; /* 增加底部内边距，防止内容被遮挡 */
}

/* 隐藏手机端账号列表滚动条 */
.mobile-accounts-list::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.mobile-accounts-list {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}

.mobile-account-item {
  display: flex;
  align-items: center;
  padding: 15px;
  background: #fff;
  border-radius: 8px;
  margin-bottom: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 12px;
}

.mobile-account-item:active {
  background: #f5f7fa;
}

.mobile-account-avatar {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  font-weight: bold;
  flex-shrink: 0;
}

.mobile-account-info {
  flex: 1;
  min-width: 0;
}

.mobile-account-name {
  font-size: 15px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-account-id {
  font-size: 12px;
  color: #909399;
}

.mobile-selected-account {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.selected-account-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  flex-shrink: 0;
}

.selected-account-name {
  flex: 1;
  font-size: 14px;
  font-weight: 500;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.mobile-filter-bar {
  display: flex;
  gap: 8px;
  padding: 10px;
  background: #fff;
  border-bottom: 1px solid #ebeef5;
}

.mobile-logs-list {
  flex: 1;
  overflow-y: auto;
  padding: 10px;
  padding-bottom: 30px; /* 增加底部内边距，防止内容被遮挡 */
}

/* 隐藏手机端操作记录列表滚动条 */
.mobile-logs-list::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.mobile-logs-list {
  -ms-overflow-style: none;  /* IE and Edge */
  scrollbar-width: none;  /* Firefox */
}

.mobile-log-card {
  background: #fff;
  border-radius: 8px;
  padding: 12px;
  margin-bottom: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.3s ease;
}

.mobile-log-card:active {
  background: #f5f7fa;
}

.log-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  gap: 8px;
}

.log-card-desc {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 10px;
  word-break: break-word;
}

.log-card-info {
  background: #f5f7fa;
  border-radius: 4px;
  padding: 8px;
  margin-bottom: 10px;
}

.log-info-row {
  display: flex;
  font-size: 13px;
  margin-bottom: 4px;
}

.log-info-row:last-child {
  margin-bottom: 0;
}

.log-label {
  color: #909399;
  flex-shrink: 0;
}

.log-value {
  color: #606266;
  word-break: break-all;
}

.log-card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-time {
  font-size: 12px;
  color: #909399;
}

.mobile-pagination {
  padding: 10px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  display: flex;
  justify-content: center;
}
</style>

<style>
.operation-detail-dialog {
  max-width: 800px;
}

.operation-detail-dialog .el-message-box__message {
  max-height: 600px;
  overflow-y: auto;
}
</style>
