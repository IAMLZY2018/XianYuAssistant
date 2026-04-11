<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { ElMessageBox } from 'element-plus';
import { QuestionFilled, Loading } from '@element-plus/icons-vue';
import { getAccountList } from '@/api/account';
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account, WebSocketStatus } from '@/types';
import ManualUpdateCookieDialog from './components/ManualUpdateCookieDialog.vue';
import ManualUpdateTokenDialog from './components/ManualUpdateTokenDialog.vue';
import QRUpdateDialog from './components/QRUpdateDialog.vue';

interface ConnectionStatus {
  xianyuAccountId: number;
  connected: boolean;
  status: string;
  cookieStatus?: number;      // Cookie状态 1:有效 2:过期 3:失效
  cookieText?: string;        // Cookie值
  websocketToken?: string;    // WebSocket Token
  tokenExpireTime?: number;   // Token过期时间戳（毫秒）
}

const loading = ref(false);
const accounts = ref<Account[]>([]);
const selectedAccountId = ref<number | null>(null);
const connectionStatus = ref<ConnectionStatus | null>(null);
const statusLoading = ref(false);
const refreshTokenLoading = ref(false);
const logs = ref<Array<{ time: string; message: string; isError?: boolean }>>([]);
let statusInterval: number | null = null;

// 滚动加载相关
const accountListRef = ref<HTMLElement | null>(null);
const currentPage = ref(1);
const pageSize = ref(20);
const totalAccounts = ref(0);
const hasMore = ref(true);
const loadingMore = ref(false);

// 手动更新Cookie对话框
const showManualUpdateCookieDialog = ref(false);
// 手动更新Token对话框
const showManualUpdateTokenDialog = ref(false);
// 扫码更新对话框
const showQRUpdateDialog = ref(false);

// 当前选中的账号信息
const currentAccount = computed(() => {
  return accounts.value.find(acc => acc.id === selectedAccountId.value);
});

// 加载账号列表
const loadAccounts = async (isLoadMore = false) => {
  if (isLoadMore) {
    loadingMore.value = true;
  } else {
    loading.value = true;
    currentPage.value = 1;
    accounts.value = [];
  }
  
  try {
    const response = await getAccountList();
    if (response.code === 0 || response.code === 200) {
      const newAccounts = response.data?.accounts || [];
      
      if (isLoadMore) {
        accounts.value = [...accounts.value, ...newAccounts];
      } else {
        accounts.value = newAccounts;
      }
      
      totalAccounts.value = response.data?.total || newAccounts.length;
      hasMore.value = accounts.value.length < totalAccounts.value;
    } else {
      throw new Error(response.msg || '获取账号列表失败');
    }
  } catch (error: any) {
    console.error('加载账号列表失败:', error);
    if (!isLoadMore) {
      accounts.value = [];
    }
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

// 滚动加载更多
const handleScroll = () => {
  if (!accountListRef.value || loadingMore.value || !hasMore.value) return;
  
  const { scrollTop, scrollHeight, clientHeight } = accountListRef.value;
  const scrollBottom = scrollHeight - scrollTop - clientHeight;
  
  // 距离底部100px时触发加载
  if (scrollBottom < 100) {
    currentPage.value++;
    loadAccounts(true);
  }
};

// 选择账号
const selectAccount = (accountId: number) => {
  selectedAccountId.value = accountId;
  loadConnectionStatus(accountId);
  
  // 启动定时刷新
  if (statusInterval) {
    clearInterval(statusInterval);
  }
  statusInterval = window.setInterval(() => {
    if (selectedAccountId.value) {
      loadConnectionStatus(selectedAccountId.value, true);
    }
  }, 5000);
};

// 加载连接状态
const loadConnectionStatus = async (accountId: number, silent = false) => {
  if (!silent) {
    statusLoading.value = true;
  }
  try {
    const response = await getConnectionStatus(accountId);
    if (response.code === 0 || response.code === 200) {
      connectionStatus.value = response.data as ConnectionStatus;
      if (!silent) {
        addLog('状态已更新');
      }
    } else {
      throw new Error(response.msg || '获取连接状态失败');
    }
  } catch (error: any) {
    if (!silent) {
      console.error('加载连接状态失败:', error);
      addLog('加载状态失败: ' + error.message, true);
    }
  } finally{
    statusLoading.value = false;
  }
};

// 启动连接
const handleStartConnection = async () => {
  if (!selectedAccountId.value) return;
  
  statusLoading.value = true;
  addLog('正在启动连接...');
  try {
    const response = await startConnection(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接启动成功');
      addLog('连接启动成功');
      await loadConnectionStatus(selectedAccountId.value);
    } else if (response.code === 1001 && response.data?.needCaptcha) {
      // 需要滑块验证
      addLog('⚠️ 检测到需要滑块验证', true);
      
      // 显示验证对话框
      await ElMessageBox.confirm(
        `检测到账号需要完成滑块验证才能启动连接。\n\n` +
        `📋 操作步骤：\n\n` +
        `1️⃣ 点击下方"访问闲鱼IM"按钮，打开闲鱼消息页面\n\n` +
        `2️⃣ 在闲鱼页面完成滑块验证\n\n` +
        `3️⃣ 验证成功后，点击本页面 Cookie 和 Token 区域的"❓ 如何获取？"按钮\n\n` +
        `4️⃣ 按照帮助教程获取 Cookie 和 Token\n\n` +
        `5️⃣ 点击"✏️ 手动更新"按钮，粘贴 Cookie 和 Token\n\n` +
        `6️⃣ 更新完成后，重新点击"启动连接"即可\n\n` +
        `💡 提示：帮助按钮中有详细的图文教程，非常简单！`,
        '🔐 需要滑块验证',
        {
          confirmButtonText: '🌐 访问闲鱼IM',
          cancelButtonText: '取消',
          type: 'warning',
          distinguishCancelAndClose: true,
          customClass: 'captcha-guide-dialog'
        }
      );
      
      // 打开闲鱼IM页面
      window.open('https://www.goofish.com/im', '_blank');
      addLog('✅ 已打开闲鱼IM页面');
      addLog('📌 完成验证后，请点击"❓ 如何获取？"按钮查看教程');
      showInfo('请在闲鱼IM页面完成验证，然后使用帮助按钮获取Cookie和Token');
    } else {
      throw new Error(response.msg || '启动连接失败');
    }
  } catch (error: any) {
    if (error !== 'cancel' && error !== 'close') {
      console.error('启动连接失败:', error);
      addLog('启动连接失败: ' + error.message, true);
    }
  } finally {
    statusLoading.value = false;
  }
};

// 停止连接
const handleStopConnection = async () => {
  if (!selectedAccountId.value) return;
  
  // 显示确认对话框
  try {
    await ElMessageBox.confirm(
      '断开连接后将无法接收消息和执行自动化流程，确定要断开连接吗？',
      '确认断开连接',
      {
        confirmButtonText: '确定断开',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
  } catch {
    // 用户取消操作
    return;
  }
  
  statusLoading.value = true;
  addLog('正在断开连接...');
  try {
    const response = await stopConnection(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接已断开');
      addLog('连接已断开');
      await loadConnectionStatus(selectedAccountId.value);
    } else {
      throw new Error(response.msg || '断开连接失败');
    }
  } catch (error: any) {
    console.error('断开连接失败:', error);
    addLog('断开连接失败: ' + error.message, true);
  } finally {
    statusLoading.value = false;
  }
};

// 清除验证等待状态
const handleClearCaptchaWait = async () => {
  if (!selectedAccountId.value) return;
  
  statusLoading.value = true;
  addLog('正在清除验证等待状态...');
  try {
    const { clearCaptchaWait } = await import('@/api/websocket');
    const response = await clearCaptchaWait(selectedAccountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('验证等待状态已清除，可以重新启动连接');
      addLog('✅ 验证等待状态已清除');
    } else {
      throw new Error(response.msg || '清除失败');
    }
  } catch (error: any) {
    console.error('清除验证等待状态失败:', error);
    addLog('清除验证等待状态失败: ' + error.message, true);
  } finally {
    statusLoading.value = false;
  }
};

// 刷新状态
const handleRefresh = () => {
  if (selectedAccountId.value) {
    loadConnectionStatus(selectedAccountId.value);
    showInfo('状态已刷新');
  }
};

// 添加日志
const addLog = (message: string, isError = false) => {
  const now = new Date();
  const time = now.toLocaleTimeString();
  logs.value.push({ time, message, isError });
  
  // 限制日志数量
  if (logs.value.length > 50) {
    logs.value.shift();
  }
};

// 获取账号显示名称
const getAccountName = (account: Account) => {
  return account.accountNote || account.unb || '未命名账号';
};

// 获取账号头像字符
const getAccountAvatar = (account: Account) => {
  const name = getAccountName(account);
  return name.charAt(0);
};

// 获取Cookie状态文本
const getCookieStatusText = (status?: number) => {
  if (status === undefined || status === null) return '未知';
  const statusMap: Record<number, string> = {
    1: '有效',
    2: '过期',
    3: '失效'
  };
  return statusMap[status] || '未知';
};

// 获取Cookie状态标签类型
const getCookieStatusType = (status?: number) => {
  if (status === undefined || status === null) return 'info';
  const typeMap: Record<number, string> = {
    1: 'success',
    2: 'warning',
    3: 'danger'
  };
  return typeMap[status] || 'info';
};

// 格式化时间戳
const formatTimestamp = (timestamp?: number) => {
  if (!timestamp) return '未设置';
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

// 判断Token是否过期
const isTokenExpired = (timestamp?: number) => {
  if (!timestamp) return false;
  return Date.now() > timestamp;
};

// 获取Token状态文本
const getTokenStatusText = (timestamp?: number) => {
  if (!timestamp) return '未设置';
  return isTokenExpired(timestamp) ? '已过期' : '有效';
};

// 获取Token状态类型
const getTokenStatusType = (timestamp?: number) => {
  if (!timestamp) return 'info';
  return isTokenExpired(timestamp) ? 'danger' : 'success';
};

// 打开手动更新Cookie对话框
const handleManualUpdateCookie = () => {
  showManualUpdateCookieDialog.value = true;
};

// 打开手动更新Token对话框
const handleManualUpdateToken = () => {
  showManualUpdateTokenDialog.value = true;
};

// 显示Cookie获取帮助
const showCookieHelp = () => {
  ElMessageBox({
    title: '如何获取Cookie',
    message: `
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取Cookie：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问闲鱼网站并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>刷新页面</li>
          <li>在请求列表中找到任意请求</li>
          <li>在请求头中找到Cookie字段</li>
          <li>复制完整的Cookie值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img 
            src="/cookieGet.png" 
            class="cookie-help-image"
            alt="Cookie获取示例" 
            onerror="this.style.display='none'"
            onclick="window.open('/cookieGet.png', '_blank')"
            title="点击查看大图"
          />
        </div>
        <p style="margin-top: 12px; color: #909399; font-size: 12px; text-align: center;">
          💡 点击图片可查看大图
        </p>
        <p style="margin-top: 8px; color: #f56c6c; font-size: 12px; text-align: center;">
          ⚠️ Cookie包含敏感信息，请勿泄露给他人
        </p>
      </div>
    `,
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了',
    customClass: 'cookie-help-dialog'
  });
};

// 显示Token获取帮助
const showTokenHelp = () => {
  ElMessageBox({
    title: '如何获取WebSocket Token',
    message: `
      <div style="text-align: left;">
        <p style="margin-bottom: 12px;">请按照以下步骤获取WebSocket Token：</p>
        <ol style="margin-left: 20px; line-height: 1.8;">
          <li>打开浏览器，访问 <a href="https://www.goofish.com/im" target="_blank" style="color: #409eff;">闲鱼IM页面</a> 并登录</li>
          <li>按F12打开开发者工具</li>
          <li>切换到"网络"(Network)标签</li>
          <li>在页面中进行任意操作（如点击聊天）</li>
          <li>在请求列表中找到WebSocket连接请求</li>
          <li>查看请求参数或响应中的Token信息</li>
          <li>复制完整的Token值</li>
        </ol>
        <div style="margin-top: 16px; text-align: center;">
          <img 
            src="/tokenGet.png" 
            class="token-help-image"
            alt="Token获取示例" 
            onerror="this.style.display='none'"
            onclick="window.open('/tokenGet.png', '_blank')"
            title="点击查看大图"
          />
        </div>
        <p style="margin-top: 12px; color: #909399; font-size: 12px; text-align: center;">
          💡 点击图片可查看大图
        </p>
        <p style="margin-top: 8px; color: #f56c6c; font-size: 12px; text-align: center;">
          ⚠️ Token包含敏感信息，请勿泄露给他人
        </p>
      </div>
    `,
    dangerouslyUseHTMLString: true,
    confirmButtonText: '知道了',
    customClass: 'token-help-dialog'
  });
};

// Cookie手动更新成功回调
const handleManualUpdateCookieSuccess = async () => {
  addLog('Cookie已手动更新');
  if (selectedAccountId.value) {
    await loadConnectionStatus(selectedAccountId.value);
  }
};

// Token手动更新成功回调
const handleManualUpdateTokenSuccess = async () => {
  addLog('Token已手动更新');
  if (selectedAccountId.value) {
    await loadConnectionStatus(selectedAccountId.value);
  }
};

// 扫码更新成功回调
const handleQRUpdateSuccess = async () => {
  addLog('Cookie和Token已通过扫码更新');
  if (selectedAccountId.value) {
    await loadConnectionStatus(selectedAccountId.value);
  }
};

onMounted(async () => {
  await loadAccounts();
  // 默认选择第一个账号
  if (accounts.value.length > 0) {
    selectAccount(accounts.value[0]?.id || 0);
  }
});

onUnmounted(() => {
  if (statusInterval) {
    clearInterval(statusInterval);
  }
});
</script>

<template>
  <div class="connection-page">
    <div class="page-header">
      <h1 class="page-title">连接管理</h1>
    </div>

    <div class="connection-container">
      <!-- 左侧账号列表 -->
      <el-card class="account-panel">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">闲鱼账号</span>
          </div>
        </template>
        
        <div 
          v-loading="loading" 
          class="account-list"
          ref="accountListRef"
          @scroll="handleScroll"
        >
          <div
            v-for="account in accounts"
            :key="account.id"
            class="account-item"
            :class="{ active: selectedAccountId === account.id }"
            @click="selectAccount(account.id)"
          >
            <div class="account-avatar">{{ getAccountAvatar(account) }}</div>
            <div class="account-info">
              <div class="account-name">{{ getAccountName(account) }}</div>
              <div class="account-id">ID: {{ account.id }}</div>
            </div>
          </div>
          
          <!-- 加载更多提示 -->
          <div v-if="loadingMore" class="loading-more">
            <el-icon class="is-loading"><loading /></el-icon>
            <span>加载中...</span>
          </div>
          
          <!-- 没有更多数据提示 -->
          <div v-if="!hasMore && accounts.length > 0" class="no-more">
            已加载全部 {{ accounts.length }} 个账号
          </div>
          
          <el-empty
            v-if="!loading && accounts.length === 0"
            description="暂无账号数据"
            :image-size="80"
          />
        </div>
      </el-card>

      <!-- 右侧连接状态 -->
      <el-card class="status-panel">
        <template #header>
          <div class="panel-header">
            <span class="panel-title">连接状态</span>
            <el-button
              v-if="selectedAccountId"
              size="small"
              :icon="'Refresh'"
              @click="handleRefresh"
              circle
            />
          </div>
        </template>
        
        <div v-if="!selectedAccountId" class="empty-state">
          <el-empty description="请选择一个账号查看连接状态" :image-size="100">
            <template #image>
              <div class="empty-icon">🔗</div>
            </template>
          </el-empty>
        </div>

        <div v-else v-loading="statusLoading" class="status-content">
          <!-- 连接状态大卡片 - 包含所有依赖信息 -->
          <div v-if="connectionStatus" class="connection-main-card">
            <!-- 主标题区域 -->
            <div class="main-card-header">
              <div class="header-left">
                <div class="icon-wrapper-large" :class="connectionStatus.connected ? 'icon-success' : 'icon-danger'">
                  <span class="icon-large">{{ connectionStatus.connected ? '✓' : '✕' }}</span>
                </div>
                <div class="header-info">
                  <h2 class="main-title">连接状态</h2>
                  <p class="main-subtitle">账号 ID: {{ connectionStatus.xianyuAccountId }} · {{ connectionStatus.status }}</p>
                  <p class="main-note" :class="connectionStatus.connected ? 'note-success' : 'note-danger'">
                    {{ connectionStatus.connected ? '已连接到闲鱼服务器' : '当前未连接到闲鱼服务器，无法监听消息以及执行自动化流程' }}
                  </p>
                </div>
              </div>
              <div class="header-right">
                <el-tag
                  :type="connectionStatus.connected ? 'success' : 'danger'"
                  size="large"
                  effect="dark"
                  round
                  class="status-tag-large"
                >
                  {{ connectionStatus.connected ? '● 已连接' : '● 未连接' }}
                </el-tag>
              </div>
            </div>

            <!-- 详细信息区域 -->
            <div class="details-grid">
              <!-- Cookie 详情 -->
              <div class="detail-section cookie-section">
                <div class="section-header">
                  <div class="section-icon">🍪</div>
                  <div class="section-title-group">
                    <h3 class="section-title">Cookie 凭证</h3>
                    <p class="section-note">用于识别账号，如果过期无法使用任何功能</p>
                  </div>
                  <el-tag 
                    :type="getCookieStatusType(connectionStatus.cookieStatus)" 
                    size="small"
                    round
                  >
                    {{ getCookieStatusText(connectionStatus.cookieStatus) }}
                  </el-tag>
                </div>
                <div class="section-body">
                  <div class="info-box">
                    <div class="info-box-label">Cookie 内容</div>
                    <div class="info-box-value cookie-value">
                      {{ connectionStatus.cookieText || '未获取到Cookie' }}
                    </div>
                    <div class="info-box-meta" v-if="connectionStatus.cookieText">
                      长度: {{ connectionStatus.cookieText.length }} 字符
                    </div>
                  </div>
                  <div class="section-actions">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleManualUpdateCookie"
                      class="manual-update-btn"
                    >
                      ✏️ 手动更新
                    </el-button>
                    <el-button
                      type="info"
                      size="small"
                      @click="showCookieHelp"
                    >
                      ❓ 如何获取？
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- Token 详情 -->
              <div class="detail-section token-section">
                <div class="section-header">
                  <div class="section-icon">🔑</div>
                  <div class="section-title-group">
                    <h3 class="section-title">WebSocket Token</h3>
                    <p class="section-note">这个是收取消息的凭证，如果异常，可能是账号被锁人机验证，需要隔段时间再试一试</p>
                  </div>
                  <el-tag 
                    :type="getTokenStatusType(connectionStatus.tokenExpireTime)" 
                    size="small"
                    round
                  >
                    {{ getTokenStatusText(connectionStatus.tokenExpireTime) }}
                  </el-tag>
                </div>
                <div class="section-body">
                  <div class="info-box">
                    <div class="info-box-label">⏰ 过期时间</div>
                    <div class="info-box-value time-value">
                      {{ formatTimestamp(connectionStatus.tokenExpireTime) }}
                    </div>
                  </div>
                  <div class="info-box">
                    <div class="info-box-label">Token 内容</div>
                    <div class="info-box-value token-value">
                      {{ connectionStatus.websocketToken || '未获取到Token' }}
                    </div>
                    <div class="info-box-meta" v-if="connectionStatus.websocketToken">
                      长度: {{ connectionStatus.websocketToken.length }} 字符
                    </div>
                  </div>
                  <div class="section-actions">
                    <el-button
                      type="primary"
                      size="small"
                      @click="handleManualUpdateToken"
                      class="manual-update-btn"
                    >
                      ✏️ 手动更新
                    </el-button>
                    <el-button
                      type="info"
                      size="small"
                      @click="showTokenHelp"
                    >
                      ❓ 如何获取？
                    </el-button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 操作区域 -->
            <div class="main-actions">
              <div class="action-wrapper">
                <div class="action-buttons">
                  <el-button
                    v-if="connectionStatus.connected"
                    type="danger"
                    size="default"
                    @click="handleStopConnection"
                    class="main-action-btn"
                  >
                    ⏸ 断开连接
                  </el-button>
                  <el-button
                    v-else
                    type="success"
                    size="default"
                    @click="handleStartConnection"
                    class="main-action-btn start-connection-btn"
                  >
                    ▶ 启动连接
                  </el-button>
                  <el-button
                    type="primary"
                    size="default"
                    @click="showQRUpdateDialog = true"
                    class="main-action-btn qr-update-btn"
                  >
                    📱 扫码更新
                  </el-button>
                </div>
                <div class="action-tip">
                  ⚠️ 请勿频繁启用连接和断开连接，否则容易触发滑动窗口人机校验，导致账号暂时不可用
                </div>
                <div class="action-tip qr-update-tip">
                  💡 扫码更新：通过扫码登录完成更新Cookie与Token
                </div>
              </div>
            </div>
          </div>

          <!-- 操作日志 -->
          <div class="logs-section">
            <div class="logs-header">操作日志</div>
            <div class="logs-container">
              <div
                v-for="(log, index) in logs"
                :key="index"
                class="log-entry"
                :class="{ 'log-error': log.isError }"
              >
                <span class="log-time">[{{ log.time }}]</span>
                <span class="log-message">{{ log.message }}</span>
              </div>
              <div v-if="logs.length === 0" class="log-empty">
                暂无日志记录
              </div>
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 手动更新Cookie对话框 -->
    <ManualUpdateCookieDialog
      v-if="currentAccount && connectionStatus"
      v-model="showManualUpdateCookieDialog"
      :account-id="currentAccount.id"
      :current-cookie="connectionStatus.cookieText || ''"
      @success="handleManualUpdateCookieSuccess"
    />

    <!-- 手动更新Token对话框 -->
    <ManualUpdateTokenDialog
      v-if="currentAccount && connectionStatus"
      v-model="showManualUpdateTokenDialog"
      :account-id="currentAccount.id"
      :current-token="connectionStatus.websocketToken || ''"
      @success="handleManualUpdateTokenSuccess"
    />

    <!-- 扫码更新对话框 -->
    <QRUpdateDialog
      v-if="currentAccount"
      v-model="showQRUpdateDialog"
      :account-id="currentAccount.id"
      @success="handleQRUpdateSuccess"
    />
  </div>
</template>

<style scoped>
.connection-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  padding: 15px;
  overflow: hidden; /* 防止最外层出现滚动条 */
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
  flex-shrink: 0; /* 防止被压缩 */
}

.page-title {
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.connection-container {
  flex: 1;
  display: flex;
  gap: 15px;
  min-height: 0;
  overflow: hidden; /* 防止容器出现滚动条 */
}

.account-panel,
.status-panel {
  height: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 防止卡片出现滚动条 */
}

.account-panel {
  flex: 1;
  min-width: 0;
  max-width: 400px;
}

.status-panel {
  flex: 2;
  min-width: 0;
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

.account-list {
  flex: 1;
  overflow-y: auto;
  min-height: 0; /* 允许flex子项收缩 */
  position: relative;
}

.loading-more {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 12px;
  color: #909399;
  font-size: 13px;
}

.no-more {
  text-align: center;
  padding: 12px;
  color: #c0c4cc;
  font-size: 12px;
  border-top: 1px solid #f0f0f0;
}

.account-item {
  display: flex;
  align-items: center;
  padding: 10px;
  border: 1px solid #ebeef5;
  border-radius: 3px;
  margin-bottom: 6px;
  cursor: pointer;
  transition: all 0.3s ease;
  gap: 12px;
}

.account-item:hover {
  background-color: #f5f7fa;
  border-color: #c0c4cc;
}

.account-item.active {
  background-color: #ecf5ff;
  border-color: #409eff;
}

.account-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409eff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  font-size: 16px;
  margin-right: 0;
  flex-shrink: 0;
}

.account-info {
  flex: 1;
  min-width: 0;
}

.account-name {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
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

.empty-icon {
  font-size: 80px;
}

.status-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 20px;
  overflow-y: auto; /* 允许内容区域滚动 */
  min-height: 0; /* 允许flex子项收缩 */
}

/* 连接状态主卡片 */
.connection-main-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 12px;
  border: 2px solid #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
  overflow: hidden;
  flex-shrink: 0; /* 防止被压缩 */
}

/* 主标题区域 */
.main-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  border-bottom: 2px solid #d9ecff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.icon-wrapper-large {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.icon-success {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%);
}

.icon-danger {
  background: linear-gradient(135deg, #f56c6c 0%, #f78989 100%);
}

.icon-large {
  font-size: 28px;
  font-weight: bold;
  color: white;
}

.header-info {
  flex: 1;
}

.main-title {
  font-size: 17px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 4px 0;
  letter-spacing: 0.3px;
}

.main-subtitle {
  font-size: 12px;
  color: #909399;
  margin: 0 0 3px 0;
  font-weight: 500;
}

.main-note {
  font-size: 11px;
  margin: 0;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 4px;
  display: inline-block;
  margin-top: 4px;
}

.note-danger {
  color: #f56c6c;
  background: #fef0f0;
  border: 1px solid #fde2e2;
}

.note-success {
  color: #67c23a;
  background: #f0f9ff;
  border: 1px solid #c6f6d5;
}

.header-right {
  display: flex;
  align-items: center;
}

.status-tag-large {
  font-size: 14px;
  padding: 8px 16px;
  font-weight: 600;
}

/* 详细信息网格 */
.details-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  padding: 16px 20px;
}

.detail-section {
  background: white;
  border-radius: 10px;
  border: 2px solid #e4e7ed;
  padding: 14px;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.detail-section:hover {
  box-shadow: 0 3px 10px rgba(0, 0, 0, 0.08);
}

.cookie-section {
  border-color: #e6a23c;
}

.token-section {
  border-color: #67c23a;
}

.section-header {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f7fa;
}

.section-icon {
  font-size: 24px;
  flex-shrink: 0;
  line-height: 1;
}

.section-title-group {
  flex: 1;
  min-width: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 4px 0;
}

.section-note {
  font-size: 11px;
  color: #909399;
  margin: 0;
  line-height: 1.4;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-box {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-box-label {
  font-size: 10px;
  color: #909399;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 6px;
}

.info-box-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 10px;
  color: #606266;
  line-height: 1.5;
  word-break: break-all;
  background: white;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  max-height: 80px;
  overflow-y: auto;
}

.cookie-value,
.token-value {
  font-size: 10px;
}

.time-value {
  font-size: 11px;
  font-weight: 600;
  color: #303133;
}

.info-box-meta {
  font-size: 10px;
  color: #909399;
  margin-top: 4px;
  text-align: right;
}

.section-actions {
  display: flex;
  gap: 6px;
  margin-top: 2px;
}

.section-actions .el-button {
  flex: 1;
}

.manual-update-btn {
  color: white !important;
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%) !important;
  border-color: #409eff !important;
  transition: all 0.3s ease !important;
}

.manual-update-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%) !important;
  border-color: #66b1ff !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.4) !important;
  transform: translateY(-1px);
}

.manual-update-btn:active {
  background: linear-gradient(135deg, #3a8ee6 0%, #409eff 100%) !important;
  border-color: #3a8ee6 !important;
  box-shadow: 0 1px 4px rgba(64, 158, 255, 0.3) !important;
  transform: translateY(0);
}

/* 主操作区域 */
.main-actions {
  padding: 14px 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #ffffff 100%);
  border-top: 1px solid #e4e7ed;
  display: flex;
  justify-content: center;
}

.action-wrapper {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  width: 100%;
  justify-content: center;
}

.main-action-btn {
  flex: 1;
  max-width: 200px;
  height: 40px;
  font-size: 14px;
  font-weight: 600;
}

.action-tip {
  font-size: 11px;
  color: #909399;
  text-align: center;
  line-height: 1.5;
  max-width: 80%;
}

.qr-update-tip {
  color: #409eff;
  font-weight: 500;
}

.start-connection-btn {
  background: linear-gradient(135deg, #67c23a 0%, #85ce61 100%) !important;
  border-color: #67c23a !important;
  box-shadow: 0 2px 8px rgba(103, 194, 58, 0.3) !important;
}

.start-connection-btn:hover {
  background: linear-gradient(135deg, #85ce61 0%, #95d475 100%) !important;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.4) !important;
  transform: translateY(-1px);
}

.qr-update-btn {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%) !important;
  border-color: #409eff !important;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3) !important;
}

.qr-update-btn:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%) !important;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4) !important;
  transform: translateY(-1px);
}

.logs-section {
  margin-top: 16px;
  flex-shrink: 0; /* 防止被压缩 */
}

.logs-header {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}

.logs-container {
  background: #2c3e50;
  color: #ecf0f1;
  border-radius: 8px;
  padding: 12px;
  font-family: 'Courier New', Consolas, monospace;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
}

.log-entry {
  margin-bottom: 6px;
  line-height: 1.5;
}

.log-entry:last-child {
  margin-bottom: 0;
}

.log-time {
  color: #95a5a6;
  margin-right: 6px;
  font-size: 11px;
}

.log-message {
  color: #ecf0f1;
}

.log-entry.log-error .log-message {
  color: #e74c3c;
}

.log-empty {
  text-align: center;
  color: #95a5a6;
  padding: 16px;
  font-size: 12px;
}

/* 滚动条样式 */
.account-list::-webkit-scrollbar,
.logs-container::-webkit-scrollbar,
.status-content::-webkit-scrollbar {
  width: 6px;
}

.account-list::-webkit-scrollbar-thumb,
.logs-container::-webkit-scrollbar-thumb,
.status-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;
}

.logs-container::-webkit-scrollbar-thumb {
  background: #34495e;
}

.status-content::-webkit-scrollbar-thumb {
  background: #c0c4cc;
}

.status-content::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .details-grid {
    grid-template-columns: 1fr;
  }
  
  .dependency-flow {
    flex-wrap: wrap;
  }
  
  .flow-arrow {
    display: none;
  }
}

@media (max-width: 768px) {
  .connection-container {
    flex-direction: column;
  }
  
  .account-panel {
    max-width: none;
  }
  
  .account-panel,
  .status-panel {
    min-width: auto;
  }
  
  .main-card-header {
    flex-direction: column;
    gap: 16px;
    align-items: flex-start;
  }
  
  .header-right {
    width: 100%;
    justify-content: flex-end;
  }
  
  .dependency-flow {
    padding: 20px;
  }
  
  .flow-content {
    padding: 12px 16px;
  }
  
  .details-grid {
    padding: 20px;
  }
}
</style>

<style>
/* Cookie帮助对话框样式 */
.cookie-help-dialog {
  max-width: 900px;
  width: 90%;
}

.cookie-help-dialog .el-message-box__message {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

/* 隐藏滚动条 - Webkit浏览器 (Chrome, Safari) */
.cookie-help-dialog .el-message-box__message::-webkit-scrollbar {
  display: none;
}

/* Cookie帮助图片样式 */
.cookie-help-dialog .cookie-help-image {
  max-width: 100%;
  max-height: 50vh;
  width: auto;
  height: auto;
  border: 2px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  object-fit: contain;
  display: block;
  margin: 0 auto;
}

.cookie-help-dialog .cookie-help-image:hover {
  transform: scale(1.02);
  border-color: #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
}

.cookie-help-dialog .cookie-help-image:active {
  transform: scale(0.98);
}

/* Token帮助对话框样式 */
.token-help-dialog {
  max-width: 900px;
  width: 90%;
}

.token-help-dialog .el-message-box__message {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  /* 隐藏滚动条 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */
}

/* 隐藏滚动条 - Webkit浏览器 (Chrome, Safari) */
.token-help-dialog .el-message-box__message::-webkit-scrollbar {
  display: none;
}

/* Token帮助图片样式 */
.token-help-dialog .token-help-image {
  max-width: 100%;
  max-height: 50vh;
  width: auto;
  height: auto;
  border: 2px solid #ddd;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  object-fit: contain;
  display: block;
  margin: 0 auto;
}

.token-help-dialog .token-help-image:hover {
  transform: scale(1.02);
  border-color: #67c23a;
  box-shadow: 0 4px 12px rgba(103, 194, 58, 0.3);
}

.token-help-dialog .token-help-image:active {
  transform: scale(0.98);
}

/* 滑块验证引导对话框样式 */
.captcha-guide-dialog {
  max-width: 650px;
  width: 90%;
}

.captcha-guide-dialog .el-message-box__message {
  font-size: 14px;
  line-height: 1.8;
  color: #606266;
  white-space: pre-line;
  text-align: left;
}

.captcha-guide-dialog .el-message-box__title {
  font-size: 18px;
  font-weight: 600;
}

.captcha-guide-dialog .el-button--primary {
  background: linear-gradient(135deg, #409eff 0%, #66b1ff 100%);
  border-color: #409eff;
  font-weight: 500;
  padding: 12px 24px;
}

.captcha-guide-dialog .el-button--primary:hover {
  background: linear-gradient(135deg, #66b1ff 0%, #409eff 100%);
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.4);
}
</style>

