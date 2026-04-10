<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue';
import { Loading } from '@element-plus/icons-vue';
import { getAccountList } from '@/api/account';
import { getMessageList } from '@/api/message';
import { getGoodsList } from '@/api/goods';
import { sendMessage } from '@/api/websocket';
import { showError, showInfo, showSuccess } from '@/utils';
import type { Account } from '@/types';
import type { ChatMessage } from '@/api/message';
import type { GoodsItemWithConfig } from '@/api/goods';

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const goodsIdFilter = ref<string>('');
const messageList = ref<ChatMessage[]>([]);
const currentPage = ref(1);
const pageSize = ref(20);
const total = ref(0);
const filterCurrentAccount = ref(false); // 过滤当前账号消息开关

// 商品列表相关
const goodsList = ref<GoodsItemWithConfig[]>([]);
const goodsCurrentPage = ref(1);
const goodsTotal = ref(0);
const goodsLoading = ref(false);
const goodsListRef = ref<HTMLElement | null>(null);

// 快速回复相关
const quickReplyDialogVisible = ref(false);
const quickReplyMessage = ref('');
const quickReplySending = ref(false);
const currentReplyMessage = ref<ChatMessage | null>(null);

// 获取当前选中账号的UNB
const getCurrentAccountUnb = computed(() => {
  if (!selectedAccountId.value) return '';
  const account = accounts.value.find(acc => acc.id === selectedAccountId.value);
  return account ? account.unb : '';
});

// 加载账号列表
const loadAccounts = async () => {
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      accounts.value = response.data?.accounts || [];
      // 默认选择第一个账号
      if (accounts.value.length > 0 && !selectedAccountId.value) {
        selectedAccountId.value = accounts.value[0]?.id ?? null;
        loadMessages();
        loadGoodsList();
      }
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
  }
};

// 加载消息列表
const loadMessages = async () => {
  if (!selectedAccountId.value) {
    showInfo('请先选择账号');
    return;
  }

  loading.value = true;
  try {
    const params: any = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      filterCurrentAccount: filterCurrentAccount.value // 添加过滤参数
    };

    if (goodsIdFilter.value) {
      params.xyGoodsId = goodsIdFilter.value;
    }

    const response = await getMessageList(params);
    if (response.code === 0 || response.code === 200) {
      messageList.value = response.data?.list || [];
      total.value = response.data?.totalCount || 0;
    } else {
      throw new Error(response.msg || '获取消息列表失败');
    }
  } catch (error: any) {
    console.error('加载消息列表失败:', error);
    messageList.value = [];
  } finally {
    loading.value = false;
  }
};

// 加载商品列表
const loadGoodsList = async () => {
  if (!selectedAccountId.value) {
    return;
  }

  goodsLoading.value = true;
  try {
    const params: any = {
      xianyuAccountId: selectedAccountId.value,
      pageNum: goodsCurrentPage.value,
      pageSize: 20  // 每次加载20条
    };

    const response = await getGoodsList(params);
    if (response.code === 0 || response.code === 200) {
      // 如果是第一页，则替换列表，否则追加到列表末尾
      if (goodsCurrentPage.value === 1) {
        goodsList.value = response.data?.itemsWithConfig || [];
      } else {
        goodsList.value.push(...(response.data?.itemsWithConfig || []));
      }
      goodsTotal.value = response.data?.totalCount || 0;

      // 检查是否需要继续加载（当内容不足以触发滚动时）
      checkAndLoadMore();
    } else {
      throw new Error(response.msg || '获取商品列表失败');
    }
  } catch (error: any) {
    console.error('加载商品列表失败:', error);
    goodsList.value = [];
  } finally {
    goodsLoading.value = false;
  }
};

// 检查是否需要继续加载更多数据
const checkAndLoadMore = () => {
  nextTick(() => {
    if (!goodsListRef.value) return;

    const { scrollHeight, clientHeight } = goodsListRef.value;
    console.log('检查是否需要加载更多:', {
      scrollHeight,
      clientHeight,
      hasScroll: scrollHeight > clientHeight,
      currentCount: goodsList.value.length,
      totalCount: goodsTotal.value
    });

    // 如果内容高度小于容器高度，且还有更多数据，则继续加载
    if (scrollHeight <= clientHeight && goodsList.value.length < goodsTotal.value) {
      console.log('内容不足以触发滚动，自动加载更多');
      goodsCurrentPage.value++;
      loadGoodsList();
    }
  });
};

// 处理商品列表滚动事件
const handleGoodsScroll = () => {
  if (!goodsListRef.value || goodsLoading.value) return;  // 如果正在加载，则不触发

  const { scrollTop, scrollHeight, clientHeight } = goodsListRef.value;

  // 调试日志
  console.log('滚动事件触发:', {
    scrollTop,
    scrollHeight,
    clientHeight,
    currentCount: goodsList.value.length,
    totalCount: goodsTotal.value,
    isLoading: goodsLoading.value
  });

  // 当滚动到底部时加载更多（距离底部50px时触发）
  if (scrollTop + clientHeight >= scrollHeight - 50) {
    if (goodsList.value.length < goodsTotal.value) {
      console.log('触发加载更多');
      goodsCurrentPage.value++;
      loadGoodsList();
    }
  }
};

// 监听滚动事件
const addScrollListener = () => {
  nextTick(() => {
    if (goodsListRef.value) {
      console.log('添加滚动监听器');
      goodsListRef.value.addEventListener('scroll', handleGoodsScroll);
    } else {
      console.error('goodsListRef.value 为空，无法添加滚动监听');
    }
  });
};

// 移除滚动监听
const removeScrollListener = () => {
  if (goodsListRef.value) {
    console.log('移除滚动监听器');
    goodsListRef.value.removeEventListener('scroll', handleGoodsScroll);
  }
};

// 账号变更
const handleAccountChange = () => {
  currentPage.value = 1;
  goodsCurrentPage.value = 1;
  loadMessages();
  loadGoodsList();
};

// 选择商品进行筛选
const selectGoods = (goodsId: string) => {
  // 如果点击的是已选中的商品，则取消筛选
  if (goodsIdFilter.value === goodsId) {
    clearFilter();
  } else {
    goodsIdFilter.value = goodsId;
    showInfo('已筛选该商品的消息');
    currentPage.value = 1;
    loadMessages();
  }
};

// 清除筛选
const clearFilter = () => {
  goodsIdFilter.value = '';
  showInfo('已取消筛选，显示全部消息');
  currentPage.value = 1;
  loadMessages();
};

// 分页变更
const handlePageChange = (page: number) => {
  currentPage.value = page;
  loadMessages();
};

// 获取消息类型文本
const getContentTypeText = (contentType: number, row: ChatMessage) => {
  // 如果是当前账号发送的消息，显示"你发送的"
  if (!isUserMessage(row)) {
    return '你发送的';
  }
  
  // contentType=1 是用户消息，其他都是系统消息
  if (contentType === 1) {
    return '用户消息';
  }
  return `系统消息(${contentType})`;
};

// 获取消息类型标签类型
const getContentTypeTag = (contentType: number, row: ChatMessage) => {
  // 如果是当前账号发送的消息，使用primary类型（蓝色）
  if (!isUserMessage(row)) {
    return 'primary';
  }
  
  // contentType=1 是用户消息（绿色），其他都是系统消息（橙色）
  if (contentType === 1) {
    return 'success';
  }
  return 'warning';
};

// 判断是否为用户发送的消息
const isUserMessage = (row: ChatMessage) => {
  // 如果senderUserId不等于当前账号的UNB，则标记为用户发送的消息
  return row.senderUserId !== getCurrentAccountUnb.value;
};

// 格式化消息时间
const formatMessageTime = (timestamp: number) => {
  if (!timestamp) return '-';

  const date = new Date(timestamp);
  const now = new Date();
  const diff = now.getTime() - date.getTime();

  // 小于1分钟
  if (diff < 60000) {
    return '刚刚';
  }

  // 小于1小时
  if (diff < 3600000) {
    return `${Math.floor(diff / 60000)}分钟前`;
  }

  // 小于24小时
  if (diff < 86400000) {
    return `${Math.floor(diff / 3600000)}小时前`;
  }

  // 超过24小时，显示具体日期时间
  return date.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

// 打开快速回复对话框
const openQuickReply = (message: ChatMessage) => {
  currentReplyMessage.value = message;
  quickReplyMessage.value = '';
  quickReplyDialogVisible.value = true;
};

// 发送快速回复
const handleQuickReply = async () => {
  if (!quickReplyMessage.value.trim()) {
    showInfo('请输入回复内容');
    return;
  }

  if (!currentReplyMessage.value || !selectedAccountId.value) {
    showError('消息信息不完整');
    return;
  }

  if (!currentReplyMessage.value.sid) {
    showError('会话ID(sid)不存在，无法发送消息');
    return;
  }

  if (!currentReplyMessage.value.senderUserId) {
    showError('接收方ID(senderUserId)不存在，无法发送消息');
    return;
  }

  quickReplySending.value = true;
  try {
    const response = await sendMessage({
      xianyuAccountId: selectedAccountId.value,
      cid: currentReplyMessage.value.sid,  // 使用 sid 作为会话ID
      toId: currentReplyMessage.value.senderUserId,  // 使用 senderUserId 作为接收方ID
      text: quickReplyMessage.value.trim()  // 注意：参数名是 text，不是 content
    });

    if (response.code === 0 || response.code === 200) {
      showSuccess('消息发送成功');
      quickReplyDialogVisible.value = false;
      quickReplyMessage.value = '';
      currentReplyMessage.value = null;
      // 刷新消息列表
      loadMessages();
    }
    // 注意：错误情况已经在请求拦截器中处理并显示了，这里不需要再次显示
  } catch (error: any) {
    console.error('发送消息失败:', error);
    // 请求拦截器已经显示了错误消息，这里不需要重复显示
  } finally {
    quickReplySending.value = false;
  }
};

onMounted(() => {
  loadAccounts();
  // 等待DOM渲染完成后添加滚动监听
  setTimeout(() => {
    addScrollListener();
  }, 0);
  // 监听窗口大小变化
  window.addEventListener('resize', handleResize);
});

onUnmounted(() => {
  removeScrollListener();
  window.removeEventListener('resize', handleResize);
});

// 处理窗口大小变化
const handleResize = () => {
  checkAndLoadMore();
};
</script>

<template>
  <div class="messages-page">
    <div class="page-header">
      <h1 class="page-title">消息管理</h1>
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
        
        <el-button @click="loadMessages">刷新消息</el-button>
        
        <el-switch
          v-model="filterCurrentAccount"
          active-text="隐藏当前账号消息"
          inactive-text="显示全部消息"
          @change="loadMessages"
        />
      </div>
    </div>

    <div class="content-container">
      <!-- 左侧商品列表 -->
      <el-card class="goods-filter-panel" :body-style="{ flex: 1, overflow: 'hidden', display: 'flex', flexDirection: 'column' }">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">商品列表</span>
            <el-button 
              v-if="goodsIdFilter" 
              type="info" 
              size="small" 
              plain
              @click="clearFilter"
            >
              取消筛选
            </el-button>
          </div>
        </template>
        
        <div 
          v-loading="goodsLoading && goodsCurrentPage === 1"
          ref="goodsListRef" 
          class="goods-list-container"
        >
          <div 
            v-for="goods in goodsList" 
            :key="goods.item.id"
            class="goods-item"
            :class="{ active: goodsIdFilter === goods.item.xyGoodId }"
            @click="selectGoods(goods.item.xyGoodId)"
          >
            <div class="goods-cover">
              <img 
                :src="goods.item.coverPic" 
                :alt="goods.item.title"
                class="cover-img"
              >
            </div>
            <div class="goods-info">
              <div class="goods-title">{{ goods.item.title }}</div>
              <div class="goods-id">#{{ goods.item.xyGoodId }}</div>
            </div>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="goodsLoading && goodsCurrentPage > 1" class="loading-more">
            <el-icon class="is-loading"><Loading /></el-icon>
            加载中...
          </div>
          
          <!-- 没有更多数据提示 -->
          <div 
            v-if="!goodsLoading && goodsList.length > 0 && goodsList.length >= goodsTotal" 
            class="no-more-data"
          >
            已加载全部商品
          </div>
          
          <el-empty
            v-if="!goodsLoading && goodsList.length === 0"
            description="暂无商品数据"
            :image-size="80"
          />
        </div>
      </el-card>

      <!-- 右侧消息列表 -->
      <div class="messages-container">
        <el-card class="messages-card">
          <template #header>
            <div class="card-header">
              <span class="card-title">消息列表</span>
              <span class="card-subtitle">共 {{ total }} 条消息</span>
            </div>
          </template>

          <el-table
            v-loading="loading"
            :data="messageList"
            stripe
            style="width: 100%"
            max-height="calc(100vh - 300px)"
          >
            <el-table-column type="index" label="序号" width="60" align="center" />
            
            <el-table-column prop="id" label="消息ID" width="100">
              <template #default="{ row }">
                <div class="message-id">{{ row.id }}</div>
              </template>
            </el-table-column>
            
            <el-table-column label="消息类型" width="120">
              <template #default="{ row }">
                <el-tag :type="getContentTypeTag(row.contentType, row)" size="small">
                  {{ getContentTypeText(row.contentType, row) }}
                </el-tag>
              </template>
            </el-table-column>
            
            <el-table-column prop="senderUserName" label="发送者" width="120" show-overflow-tooltip />
            
            <el-table-column prop="msgContent" label="消息内容" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <div 
                  class="message-content" 
                  :class="{ 'user-message': isUserMessage(row) }"
                >
                  {{ row.msgContent }}
                </div>
              </template>
            </el-table-column>
            
            <el-table-column prop="xyGoodsId" label="商品ID" width="120">
              <template #default="{ row }">
                <div class="goods-id">{{ row.xyGoodsId || '-' }}</div>
              </template>
            </el-table-column>
            
            <el-table-column label="时间" width="150">
              <template #default="{ row }">
                <div class="message-time">{{ formatMessageTime(row.messageTime) }}</div>
              </template>
            </el-table-column>
            
            <el-table-column label="操作" width="100" align="center" fixed="right">
              <template #default="{ row }">
                <el-button
                  v-if="isUserMessage(row)"
                  type="primary"
                  size="small"
                  @click="openQuickReply(row)"
                >
                  快速回复
                </el-button>
                <span v-else class="no-action">-</span>
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
      </div>
    </div>

    <!-- 快速回复对话框 -->
    <el-dialog
      v-model="quickReplyDialogVisible"
      title="快速回复"
      width="500px"
      :close-on-click-modal="false"
    >
      <div class="quick-reply-content">
        <div class="reply-info">
          <div class="info-item">
            <span class="info-label">回复给：</span>
            <span class="info-value">{{ currentReplyMessage?.senderUserName }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">原消息：</span>
            <span class="info-value">{{ currentReplyMessage?.msgContent }}</span>
          </div>
        </div>
        
        <el-input
          v-model="quickReplyMessage"
          type="textarea"
          :rows="6"
          placeholder="请输入回复内容..."
          maxlength="500"
          show-word-limit
        />
      </div>
      
      <template #footer>
        <el-button @click="quickReplyDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          :loading="quickReplySending"
          @click="handleQuickReply"
        >
          发送
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.messages-page {
  height: calc(100vh - 40px);  /* 减去页面padding */
  display: flex;
  flex-direction: column;
  padding: 20px;
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

.content-container {
  display: flex;
  flex: 1;
  gap: 15px;
  min-height: 0;
}

.goods-filter-panel {
  flex: 1;
  min-width: 0;
  max-width: 400px;
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
  font-size: 17px;
  font-weight: 600;
  color: #303133;
}

.goods-list-container {
  flex: 1;
  overflow-y: auto;
  overflow-x: hidden;
  min-height: 0;  /* 确保flex子元素可以正确滚动 */
  padding-right: 5px;  /* 为滚动条留出空间 */
}

/* 自定义滚动条样式 */
.goods-list-container::-webkit-scrollbar {
  width: 6px;
}

.goods-list-container::-webkit-scrollbar-thumb {
  background-color: #c0c4cc;
  border-radius: 3px;
}

.goods-list-container::-webkit-scrollbar-thumb:hover {
  background-color: #909399;
}

.goods-list-container::-webkit-scrollbar-track {
  background-color: #f5f7fa;
}

.goods-item {
  display: flex;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 12px;
}

.goods-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.goods-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.goods-cover {
  width: 50px;
  height: 50px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
  background-color: #f5f7fa;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
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
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.goods-id {
  font-size: 12px;
  color: #909399;
}

.loading-more {
  text-align: center;
  padding: 12px;
  color: #909399;
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.no-more-data {
  text-align: center;
  padding: 12px;
  color: #c0c4cc;
  font-size: 13px;
}

.messages-container {
  flex: 2;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.messages-card {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.card-subtitle {
  font-size: 14px;
  color: #909399;
}

.message-id {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  color: #606266;
}

.goods-id {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  color: #409eff;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.no-action {
  color: #c0c4cc;
}

.message-content {
  padding: 4px;
}

.message-content.user-message {
  border: 2px solid #67c23a;
  border-radius: 4px;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 10px 0;
  margin-top: 10px;
  border-top: 1px solid #ebeef5;
}

.quick-reply-content {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.reply-info {
  background-color: #f5f7fa;
  padding: 12px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.info-item {
  display: flex;
  gap: 8px;
  font-size: 14px;
}

.info-label {
  color: #909399;
  font-weight: 500;
  flex-shrink: 0;
}

.info-value {
  color: #303133;
  flex: 1;
  word-break: break-all;
}
</style>