<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { ElMessageBox } from 'element-plus';
import { QuestionFilled, Loading, ArrowLeft } from '@element-plus/icons-vue';
import { getConnectionStatus, startConnection, stopConnection } from '@/api/websocket';
import { showSuccess, showError, showInfo } from '@/utils';
import type { Account } from '@/types';
import ManualUpdateCookieDialog from './components/ManualUpdateCookieDialog.vue';
import ManualUpdateTokenDialog from './components/ManualUpdateTokenDialog.vue';
import QRUpdateDialog from './components/QRUpdateDialog.vue';

interface ConnectionStatus {
  xianyuAccountId: number;
  connected: boolean;
  status: string;
  cookieStatus?: number;
  cookieText?: string;
  websocketToken?: string;
  tokenExpireTime?: number;
}

const route = useRoute();
const router = useRouter();
const accountId = computed(() => Number(route.params.id));

const connectionStatus = ref<ConnectionStatus | null>(null);
const statusLoading = ref(false);
const logs = ref<Array<{ time: string; message: string; isError?: boolean }>>([]);
let statusInterval: number | null = null;

// 手动更新Cookie对话框
const showManualUpdateCookieDialog = ref(false);
// 手动更新Token对话框
const showManualUpdateTokenDialog = ref(false);
// 扫码更新对话框
const showQRUpdateDialog = ref(false);

// 加载连接状态
const loadConnectionStatus = async (silent = false) => {
  if (!silent) {
    statusLoading.value = true;
  }
  try {
    const response = await getConnectionStatus(accountId.value);
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
  } finally {
    statusLoading.value = false;
  }
};

// 启动连接
const handleStartConnection = async () => {
  statusLoading.value = true;
  addLog('正在启动连接...');
  try {
    const response = await startConnection(accountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接启动成功');
      addLog('连接启动成功');
      await loadConnectionStatus();
    } else if (response.code === 1001 && response.data?.needCaptcha) {
      addLog('⚠️ 检测到需要滑块验证', true);
      
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
    return;
  }
  
  statusLoading.value = true;
  addLog('正在断开连接...');
  try {
    const response = await stopConnection(accountId.value);
    if (response.code === 0 || response.code === 200) {
      showSuccess('连接已断开');
      addLog('连接已断开');
      await loadConnectionStatus();
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

// 刷新状态
const handleRefresh = () => {
  loadConnectionStatus();
  showInfo('状态已刷新');
};

// 返回上一页
const handleBack = () => {
  router.back();
};

// 添加日志
const addLog = (message: string, isError = false) => {
  const now = new Date();
  const time = now.toLocaleTimeString();
  logs.value.push({ time, message, isError });
  
  if (logs.value.length > 50) {
    logs.value.shift();
  }
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
  await loadConnectionStatus();
};

// Token手动更新成功回调
const handleManualUpdateTokenSuccess = async () => {
  addLog('Token已手动更新');
  await loadConnectionStatus();
};

// 扫码更新成功回调
const handleQRUpdateSuccess = async () => {
  addLog('Cookie和Token已通过扫码更新');
  await loadConnectionStatus();
};

onMounted(async () => {
  await loadConnectionStatus();
  
  // 启动定时刷新
  statusInterval = window.setInterval(() => {
    loadConnectionStatus(true);
  }, 5000);
});

onUnmounted(() => {
  if (statusInterval) {
    clearInterval(statusInterval);
  }
});
</script>

<template>
  <div class="connection-detail-page">
    <!-- 顶部导航栏 -->
    <div class="detail-header">
      <el-button
        class="back-btn"
        @click="handleBack"
        circle
      >
        <el-icon><ArrowLeft /></el-icon>
      </el-button>
      <div class="header-title">连接详情</div>
      <el-button
        v-if="connectionStatus"
        size="small"
        @click="handleRefresh"
        circle
      >
        <el-icon><Loading /></el-icon>
      </el-button>
    </div>

    <!-- 内容区域 -->
    <div v-loading="statusLoading" class="detail-content">
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
                  @click="showManualUpdateCookieDialog = true"
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
                  @click="showManualUpdateTokenDialog = true"
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

    <!-- 手动更新Cookie对话框 -->
    <ManualUpdateCookieDialog
      v-if="connectionStatus"
      v-model="showManualUpdateCookieDialog"
      :account-id="accountId"
      :current-cookie="connectionStatus.cookieText || ''"
      @success="handleManualUpdateCookieSuccess"
    />

    <!-- 手动更新Token对话框 -->
    <ManualUpdateTokenDialog
      v-if="connectionStatus"
      v-model="showManualUpdateTokenDialog"
      :account-id="accountId"
      :current-token="connectionStatus.websocketToken || ''"
      @success="handleManualUpdateTokenSuccess"
    />

    <!-- 扫码更新对话框 -->
    <QRUpdateDialog
      v-model="showQRUpdateDialog"
      :account-id="accountId"
      @success="handleQRUpdateSuccess"
    />
  </div>
</template>

<style scoped>
.connection-detail-page {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.detail-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  background: white;
  border-bottom: 1px solid #ebeef5;
  gap: 12px;
  flex-shrink: 0;
}

.back-btn {
  width: 36px;
  height: 36px;
}

.header-title {
  flex: 1;
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.detail-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  padding-bottom: 24px;
  min-height: 0;
}

/* 连接状态主卡片 */
.connection-main-card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border-radius: 12px;
  border: 2px solid #409eff;
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.12);
  overflow: hidden;
  margin-bottom: 20px;
}

/* 主标题区域 */
.main-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  background: linear-gradient(135deg, #ecf5ff 0%, #ffffff 100%);
  border-bottom: 2px solid #d9ecff;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.icon-wrapper-large {
  width: 50px;
  height: 50px;
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
  min-width: 0;
}

.main-title {
  font-size: 18px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 6px 0;
  letter-spacing: 0.3px;
}

.main-subtitle {
  font-size: 13px;
  color: #909399;
  margin: 0 0 4px 0;
  font-weight: 500;
}

.main-note {
  font-size: 12px;
  margin: 0;
  font-weight: 500;
  padding: 5px 10px;
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
  grid-template-columns: 1fr;
  gap: 16px;
  padding: 16px 20px;
}

.detail-section {
  background: white;
  border-radius: 10px;
  border: 2px solid #e4e7ed;
  padding: 16px;
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
  margin-bottom: 14px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f5f7fa;
}

.section-icon {
  font-size: 26px;
  flex-shrink: 0;
  line-height: 1;
}

.section-title-group {
  flex: 1;
  min-width: 0;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 6px 0;
}

.section-note {
  font-size: 12px;
  color: #909399;
  margin: 0;
  line-height: 1.5;
}

.section-body {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.info-box {
  background: #f8f9fa;
  padding: 12px;
  border-radius: 6px;
  border: 1px solid #e4e7ed;
}

.info-box-label {
  font-size: 11px;
  color: #606266;
  font-weight: 600;
  margin-bottom: 8px;
}

.info-box-value {
  font-family: 'Courier New', Consolas, monospace;
  font-size: 10px;
  color: #303133;
  line-height: 1.6;
  word-break: break-all;
  background: white;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  max-height: 100px;
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

/* 主操作区域 */
.main-actions {
  padding: 16px 20px;
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
  gap: 12px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  width: 100%;
  justify-content: center;
  flex-wrap: wrap;
}

.main-action-btn {
  flex: 1;
  min-width: 140px;
  max-width: 200px;
  height: 42px;
  font-size: 14px;
  font-weight: 600;
}

.action-tip {
  font-size: 11px;
  color: #909399;
  text-align: center;
  line-height: 1.6;
  max-width: 90%;
  padding: 0 8px;
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
  margin-top: 20px;
  padding-bottom: 8px;
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
  max-height: 180px;
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
.logs-container::-webkit-scrollbar,
.detail-content::-webkit-scrollbar {
  width: 6px;
}

.logs-container::-webkit-scrollbar-thumb {
  background: #34495e;
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

.detail-content::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

/* 响应式优化 */
@media (max-width: 480px) {
  .detail-header {
    padding: 10px 12px;
  }

  .header-title {
    font-size: 16px;
  }

  .detail-content {
    padding: 12px;
    padding-bottom: 20px;
  }

  .main-card-header {
    padding: 14px 16px;
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .header-left {
    width: 100%;
  }

  .header-right {
    width: 100%;
  }

  .details-grid {
    padding: 12px 16px;
    gap: 12px;
  }

  .detail-section {
    padding: 14px;
  }

  .main-actions {
    padding: 14px 16px;
  }

  .action-buttons {
    flex-direction: column;
    gap: 10px;
  }

  .main-action-btn {
    max-width: none;
    width: 100%;
  }

  .logs-container {
    max-height: 150px;
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
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.cookie-help-dialog .el-message-box__message::-webkit-scrollbar {
  display: none;
}

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

/* Token帮助对话框样式 */
.token-help-dialog {
  max-width: 900px;
  width: 90%;
}

.token-help-dialog .el-message-box__message {
  max-height: 70vh;
  overflow-y: auto;
  overflow-x: hidden;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.token-help-dialog .el-message-box__message::-webkit-scrollbar {
  display: none;
}

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
