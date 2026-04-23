<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, changePassword } from '@/api/system'
import { logout } from '@/api/auth'
import { getSetting, saveSetting } from '@/api/setting'
import { getAIStatus } from '@/api/ai'
import { ElMessage, ElMessageBox } from 'element-plus'
import { clearAuthToken } from '@/utils/request'

const router = useRouter()

// 当前选中的菜单
const activeMenu = ref('account')

// 账号信息
const username = ref('')
const lastLoginTime = ref('')
const loading = ref(false)

// 修改密码
const showPasswordForm = ref(false)
const oldPassword = ref('')
const newPassword = ref('')
const confirmPassword = ref('')
const changingPassword = ref(false)

const showOldPassword = ref(false)
const showNewPassword = ref(false)
const showConfirmPassword = ref(false)

// 退出登录
const loggingOut = ref(false)

// 系统提示词
const SYS_PROMPT_KEY = 'sys_prompt'
const DEFAULT_SYS_PROMPT = '你是一个闲鱼卖家，你叫肥极喵，不要回复的像AI，简短回答\n参考相关信息回答,不要乱回答,不知道就换不同语气回复提示用户详细点询问'
const sysPromptValue = ref('')
const sysPromptSaving = ref(false)
const sysPromptLoaded = ref(false)

// AI API Key 配置
const AI_API_KEY_SETTING = 'ai_api_key'
const AI_BASE_URL_SETTING = 'ai_base_url'
const AI_MODEL_SETTING = 'ai_model'
const DEFAULT_BASE_URL = 'https://dashscope.aliyuncs.com/compatible-mode'
const DEFAULT_MODEL = 'deepseek-v3'

const aiApiKey = ref('')
const aiBaseUrl = ref(DEFAULT_BASE_URL)
const aiModel = ref(DEFAULT_MODEL)
const aiApiKeySaving = ref(false)
const showApiKey = ref(false)

// Embedding 模型配置（可选，默认共用 AI 配置）
const EMBEDDING_API_KEY_SETTING = 'ai_embedding_api_key'
const EMBEDDING_BASE_URL_SETTING = 'ai_embedding_base_url'
const EMBEDDING_MODEL_SETTING = 'ai_embedding_model'
const DEFAULT_EMBEDDING_MODEL = 'text-embedding-v3'

const embeddingApiKey = ref('')
const embeddingBaseUrl = ref('')
const embeddingModel = ref(DEFAULT_EMBEDDING_MODEL)
const embeddingSaving = ref(false)
const showEmbeddingApiKey = ref(false)
const showEmbeddingConfig = ref(false)

// AI 状态
const aiStatus = ref({
  enabled: false,
  available: false,
  apiKeyConfigured: false,
  message: '',
  baseUrl: '',
  model: ''
})

// 菜单配置
const menuItems = [
  { key: 'account', label: '系统账号', icon: '👤' },
  { key: 'ai', label: 'AI 服务配置', icon: '🤖' },
  { key: 'prompt', label: 'AI客服配置', icon: '💬' },
  { key: 'about', label: '关于', icon: 'ℹ️' }
]

onMounted(async () => {
  loading.value = true
  try {
    const res = await getCurrentUser()
    if (res.code === 200 && res.data) {
      username.value = res.data.username || ''
      lastLoginTime.value = res.data.lastLoginTime || ''
    }
  } catch (e) {
    console.error('获取用户信息失败:', e)
  } finally {
    loading.value = false
  }

  // 加载系统提示词配置
  try {
    const res = await getSetting({ settingKey: SYS_PROMPT_KEY })
    if (res.code === 200 && res.data) {
      sysPromptValue.value = res.data.settingValue || ''
      sysPromptLoaded.value = true
    }
  } catch (e) {
    console.error('获取系统提示词配置失败:', e)
  }

  // 加载 AI 配置
  await loadAIConfig()
  // 加载 Embedding 配置
  await loadEmbeddingConfig()
  // 加载 AI 状态
  await loadAIStatus()
})

async function loadAIConfig() {
  try {
    const [apiKeyRes, baseUrlRes, modelRes] = await Promise.all([
      getSetting({ settingKey: AI_API_KEY_SETTING }),
      getSetting({ settingKey: AI_BASE_URL_SETTING }),
      getSetting({ settingKey: AI_MODEL_SETTING })
    ])

    if (apiKeyRes.code === 200 && apiKeyRes.data) {
      aiApiKey.value = apiKeyRes.data.settingValue || ''
    }
    if (baseUrlRes.code === 200 && baseUrlRes.data && baseUrlRes.data.settingValue) {
      aiBaseUrl.value = baseUrlRes.data.settingValue
    }
    if (modelRes.code === 200 && modelRes.data && modelRes.data.settingValue) {
      aiModel.value = modelRes.data.settingValue
    }
  } catch (e) {
    console.error('获取AI配置失败:', e)
  }
}

async function loadEmbeddingConfig() {
  try {
    const [apiKeyRes, baseUrlRes, modelRes] = await Promise.all([
      getSetting({ settingKey: EMBEDDING_API_KEY_SETTING }),
      getSetting({ settingKey: EMBEDDING_BASE_URL_SETTING }),
      getSetting({ settingKey: EMBEDDING_MODEL_SETTING })
    ])

    if (apiKeyRes.code === 200 && apiKeyRes.data) {
      embeddingApiKey.value = apiKeyRes.data.settingValue || ''
    }
    if (baseUrlRes.code === 200 && baseUrlRes.data && baseUrlRes.data.settingValue) {
      embeddingBaseUrl.value = baseUrlRes.data.settingValue
    }
    if (modelRes.code === 200 && modelRes.data && modelRes.data.settingValue) {
      embeddingModel.value = modelRes.data.settingValue
    }
  } catch (e) {
    console.error('获取Embedding配置失败:', e)
  }
}

async function loadAIStatus() {
  try {
    const res = await getAIStatus()
    const data = await res.json()
    if (data.code === 200 && data.data) {
      aiStatus.value = data.data
    }
  } catch (e) {
    console.error('获取AI状态失败:', e)
  }
}

async function handleChangePassword() {
  if (!oldPassword.value) {
    ElMessage.warning('请输入原密码')
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    ElMessage.warning('新密码长度需在6-50之间')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    ElMessage.warning('两次密码不一致')
    return
  }
  changingPassword.value = true
  try {
    const res = await changePassword({
      oldPassword: oldPassword.value,
      newPassword: newPassword.value,
      confirmPassword: confirmPassword.value
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      showPasswordForm.value = false
      oldPassword.value = ''
      newPassword.value = ''
      confirmPassword.value = ''
    }
  } finally {
    changingPassword.value = false
  }
}

async function handleLogout() {
  try {
    await ElMessageBox.confirm(
      '确定要退出登录吗？',
      '退出确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }
    )

    loggingOut.value = true
    try {
      await logout()
      clearAuthToken()
      ElMessage.success('已退出登录')
      router.push('/login')
    } catch (e) {
      console.error('退出登录失败:', e)
      // 即使接口失败，也清除本地token并跳转
      clearAuthToken()
      router.push('/login')
    } finally {
      loggingOut.value = false
    }
  } catch {
    // 用户取消
  }
}

async function handleSaveSysPrompt() {
  if (!sysPromptValue.value.trim()) {
    ElMessage.warning('系统提示词不能为空')
    return
  }
  sysPromptSaving.value = true
  try {
    const res = await saveSetting({
      settingKey: SYS_PROMPT_KEY,
      settingValue: sysPromptValue.value,
      settingDesc: 'AI智能回复的系统提示词'
    })
    if (res.code === 200) {
      ElMessage.success('系统提示词保存成功')
      sysPromptLoaded.value = true
    }
  } finally {
    sysPromptSaving.value = false
  }
}

function handleResetSysPrompt() {
  sysPromptValue.value = DEFAULT_SYS_PROMPT
}

async function handleSaveAIConfig() {
  if (!aiApiKey.value.trim()) {
    ElMessage.warning('API Key 不能为空')
    return
  }
  if (!aiBaseUrl.value.trim()) {
    ElMessage.warning('API Base URL 不能为空')
    return
  }
  if (!aiModel.value.trim()) {
    ElMessage.warning('模型名称不能为空')
    return
  }

  aiApiKeySaving.value = true
  try {
    // 保存三个配置
    const [keyRes, urlRes, modelRes] = await Promise.all([
      saveSetting({
        settingKey: AI_API_KEY_SETTING,
        settingValue: aiApiKey.value.trim(),
        settingDesc: 'AI服务的API Key（配置后立即生效，无需重启）'
      }),
      saveSetting({
        settingKey: AI_BASE_URL_SETTING,
        settingValue: aiBaseUrl.value.trim(),
        settingDesc: 'AI服务的API Base URL'
      }),
      saveSetting({
        settingKey: AI_MODEL_SETTING,
        settingValue: aiModel.value.trim(),
        settingDesc: 'AI对话模型名称'
      })
    ])

    if (keyRes.code === 200 && urlRes.code === 200 && modelRes.code === 200) {
      ElMessage.success('AI 配置保存成功，已立即生效')
      // 刷新 AI 状态
      await loadAIStatus()
    }
  } catch (e) {
    console.error('保存AI配置失败:', e)
    ElMessage.error('保存AI配置失败')
  } finally {
    aiApiKeySaving.value = false
  }
}

function handleResetAIConfig() {
  aiApiKey.value = ''
  aiBaseUrl.value = DEFAULT_BASE_URL
  aiModel.value = DEFAULT_MODEL
}

async function handleSaveEmbeddingConfig() {
  embeddingSaving.value = true
  try {
    // 保存三个配置（可以为空，空值表示使用 AI 对话配置）
    const [keyRes, urlRes, modelRes] = await Promise.all([
      saveSetting({
        settingKey: EMBEDDING_API_KEY_SETTING,
        settingValue: embeddingApiKey.value.trim(),
        settingDesc: 'Embedding模型API Key（留空则使用AI对话的API Key）'
      }),
      saveSetting({
        settingKey: EMBEDDING_BASE_URL_SETTING,
        settingValue: embeddingBaseUrl.value.trim(),
        settingDesc: 'Embedding模型API Base URL（留空则使用AI对话的Base URL）'
      }),
      saveSetting({
        settingKey: EMBEDDING_MODEL_SETTING,
        settingValue: embeddingModel.value.trim(),
        settingDesc: 'Embedding模型名称'
      })
    ])

    if (keyRes.code === 200 && urlRes.code === 200 && modelRes.code === 200) {
      ElMessage.success('Embedding 配置保存成功，重启服务后生效')
    }
  } catch (e) {
    console.error('保存Embedding配置失败:', e)
    ElMessage.error('保存Embedding配置失败')
  } finally {
    embeddingSaving.value = false
  }
}

function handleResetEmbeddingConfig() {
  embeddingApiKey.value = ''
  embeddingBaseUrl.value = ''
  embeddingModel.value = DEFAULT_EMBEDDING_MODEL
}
</script>

<template>
  <div class="settings">
    <!-- 左侧菜单 -->
    <div class="settings__sidebar">
      <div class="settings__sidebar-title">设置</div>
      <div class="settings__menu">
        <div
          v-for="item in menuItems"
          :key="item.key"
          class="settings__menu-item"
          :class="{ 'settings__menu-item--active': activeMenu === item.key }"
          @click="activeMenu = item.key"
        >
          <span class="settings__menu-icon">{{ item.icon }}</span>
          <span class="settings__menu-label">{{ item.label }}</span>
        </div>
      </div>
    </div>

    <!-- 右侧内容 -->
    <div class="settings__content">
      <!-- 系统账号（包含修改密码和退出登录） -->
      <div v-if="activeMenu === 'account'" class="settings__panel">
        <div class="settings__panel-title">系统账号</div>
        <div v-if="loading" class="settings__loading">
          <div class="settings__spinner"></div>
          <span>加载中...</span>
        </div>
        <div v-else class="settings__info">
          <div class="settings__info-row">
            <span class="settings__info-label">账号</span>
            <span class="settings__info-value">{{ username }}</span>
          </div>
          <div class="settings__info-row">
            <span class="settings__info-label">最后登录时间</span>
            <span class="settings__info-value">{{ lastLoginTime || '-' }}</span>
          </div>
        </div>

        <!-- 修改密码 -->
        <div class="settings__section">
          <div class="settings__section-header">
            <div class="settings__section-title">修改密码</div>
            <button
              v-if="!showPasswordForm"
              class="settings__toggle-btn"
              @click="showPasswordForm = true"
            >
              修改
            </button>
          </div>
          <div v-if="showPasswordForm" class="settings__form">
            <div class="settings__field">
              <label class="settings__label">原密码</label>
              <div class="settings__input-wrap">
                <input
                  v-model="oldPassword"
                  :type="showOldPassword ? 'text' : 'password'"
                  class="settings__input"
                  placeholder="请输入原密码"
                  :disabled="changingPassword"
                />
                <button class="settings__eye-btn" @click="showOldPassword = !showOldPassword" tabindex="-1">
                  {{ showOldPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <div class="settings__field">
              <label class="settings__label">新密码</label>
              <div class="settings__input-wrap">
                <input
                  v-model="newPassword"
                  :type="showNewPassword ? 'text' : 'password'"
                  class="settings__input"
                  placeholder="请输入新密码（6-50位）"
                  :disabled="changingPassword"
                />
                <button class="settings__eye-btn" @click="showNewPassword = !showNewPassword" tabindex="-1">
                  {{ showNewPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <div class="settings__field">
              <label class="settings__label">确认新密码</label>
              <div class="settings__input-wrap">
                <input
                  v-model="confirmPassword"
                  :type="showConfirmPassword ? 'text' : 'password'"
                  class="settings__input"
                  placeholder="请再次输入新密码"
                  :disabled="changingPassword"
                  @keydown.enter="handleChangePassword"
                />
                <button class="settings__eye-btn" @click="showConfirmPassword = !showConfirmPassword" tabindex="-1">
                  {{ showConfirmPassword ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <div class="settings__actions">
              <button class="settings__btn settings__btn--secondary" :disabled="changingPassword" @click="showPasswordForm = false">
                取消
              </button>
              <button class="settings__btn settings__btn--primary" :disabled="changingPassword" @click="handleChangePassword">
                {{ changingPassword ? '请稍候...' : '确认修改' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 退出登录 -->
        <div class="settings__section">
          <div class="settings__section-title">退出登录</div>
          <p class="settings__logout-text">退出当前系统账号，退出后需要重新登录</p>
          <button
            class="settings__btn settings__btn--danger"
            :disabled="loggingOut"
            @click="handleLogout"
          >
            {{ loggingOut ? '退出中...' : '退出登录' }}
          </button>
        </div>
      </div>

      <!-- AI 服务配置（包含 Embedding 配置和系统提示词） -->
      <div v-if="activeMenu === 'ai'" class="settings__panel">
        <div class="settings__panel-title">AI 服务配置</div>

        <!-- AI 状态指示 -->
        <div class="settings__ai-status">
          <div class="settings__ai-status-row">
            <span class="settings__info-label">服务状态</span>
            <span class="settings__ai-status-badge" :class="aiStatus.available ? 'settings__ai-status-badge--ok' : 'settings__ai-status-badge--off'">
              {{ aiStatus.available ? '可用' : '不可用' }}
            </span>
          </div>
          <div v-if="aiStatus.message" class="settings__ai-status-row">
            <span class="settings__info-label">状态说明</span>
            <span class="settings__info-value settings__ai-status-msg">{{ aiStatus.message }}</span>
          </div>
          <div v-if="aiStatus.baseUrl" class="settings__ai-status-row">
            <span class="settings__info-label">Base URL</span>
            <span class="settings__info-value">{{ aiStatus.baseUrl }}</span>
          </div>
          <div v-if="aiStatus.model" class="settings__ai-status-row">
            <span class="settings__info-label">模型</span>
            <span class="settings__info-value">{{ aiStatus.model }}</span>
          </div>
        </div>

        <!-- AI 对话配置 -->
        <div class="settings__section">
          <div class="settings__section-title">对话模型配置</div>
          <p class="settings__desc">配置 AI 对话服务，配置后立即生效，无需重启服务</p>
          <div class="settings__form">
            <div class="settings__field">
              <label class="settings__label">
                API Key
                <span class="settings__label-hint">（本项目默认使用 AliApiKey，请从这里获取：<a href="https://bailian.console.aliyun.com/" target="_blank" class="settings__link">https://bailian.console.aliyun.com/</a>）</span>
              </label>
              <div class="settings__input-wrap">
                <input
                  v-model="aiApiKey"
                  :type="showApiKey ? 'text' : 'password'"
                  class="settings__input"
                  placeholder="请输入 AI API Key"
                  :disabled="aiApiKeySaving"
                />
                <button class="settings__eye-btn" @click="showApiKey = !showApiKey" tabindex="-1">
                  {{ showApiKey ? '隐藏' : '显示' }}
                </button>
              </div>
            </div>

            <div class="settings__field">
              <label class="settings__label">API Base URL</label>
              <input
                v-model="aiBaseUrl"
                type="text"
                class="settings__input"
                placeholder="AI 服务的 API Base URL"
                :disabled="aiApiKeySaving"
              />
            </div>

            <div class="settings__field">
              <label class="settings__label">模型名称</label>
              <input
                v-model="aiModel"
                type="text"
                class="settings__input"
                placeholder="AI 对话模型名称"
                :disabled="aiApiKeySaving"
              />
            </div>

            <div class="settings__actions">
              <button
                class="settings__btn settings__btn--secondary"
                :disabled="aiApiKeySaving"
                @click="handleResetAIConfig"
              >
                恢复默认
              </button>
              <button
                class="settings__btn settings__btn--primary"
                :disabled="aiApiKeySaving"
                @click="handleSaveAIConfig"
              >
                {{ aiApiKeySaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Embedding 配置 -->
        <div class="settings__section">
          <div class="settings__section-header">
            <div class="settings__section-title">Embedding 模型配置</div>
            <button
              class="settings__toggle-btn"
              @click="showEmbeddingConfig = !showEmbeddingConfig"
            >
              {{ showEmbeddingConfig ? '收起' : '高级配置' }}
            </button>
          </div>
          <p class="settings__desc">
            配置向量嵌入模型（用于 RAG 知识库）。默认共用对话模型的配置。
            <strong>注意：修改后需要重启服务才能生效。</strong>
          </p>
          <div class="settings__form">
            <!-- 折叠内容 -->
            <div v-if="showEmbeddingConfig" class="settings__collapse-content">
              <div class="settings__field">
                <label class="settings__label">API Key <span class="settings__label-hint">(留空则使用对话模型的 API Key)</span></label>
                <div class="settings__input-wrap">
                  <input
                    v-model="embeddingApiKey"
                    :type="showEmbeddingApiKey ? 'text' : 'password'"
                    class="settings__input"
                    placeholder="留空则使用对话模型的 API Key"
                    :disabled="embeddingSaving"
                  />
                  <button class="settings__eye-btn" @click="showEmbeddingApiKey = !showEmbeddingApiKey" tabindex="-1">
                    {{ showEmbeddingApiKey ? '隐藏' : '显示' }}
                  </button>
                </div>
              </div>

              <div class="settings__field">
                <label class="settings__label">API Base URL <span class="settings__label-hint">(留空则使用对话模型的 Base URL)</span></label>
                <input
                  v-model="embeddingBaseUrl"
                  type="text"
                  class="settings__input"
                  placeholder="留空则使用对话模型的 Base URL"
                  :disabled="embeddingSaving"
                />
              </div>
            </div>

            <div class="settings__field">
              <label class="settings__label">模型名称</label>
              <input
                v-model="embeddingModel"
                type="text"
                class="settings__input"
                placeholder="Embedding 模型名称，如 text-embedding-v3"
                :disabled="embeddingSaving"
              />
            </div>

            <div class="settings__actions">
              <button
                class="settings__btn settings__btn--secondary"
                :disabled="embeddingSaving"
                @click="handleResetEmbeddingConfig"
              >
                恢复默认
              </button>
              <button
                class="settings__btn settings__btn--primary"
                :disabled="embeddingSaving"
                @click="handleSaveEmbeddingConfig"
              >
                {{ embeddingSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- AI客服配置 -->
      <div v-if="activeMenu === 'prompt'" class="settings__panel">
        <div class="settings__panel-title">AI客服配置</div>

        <!-- 系统提示词 -->
        <div class="settings__section">
          <div class="settings__section-title">系统提示词</div>
          <p class="settings__desc">配置 AI 智能回复的系统提示词，用于设定 AI 的角色和行为规则</p>
          <div class="settings__form">
            <textarea
              v-model="sysPromptValue"
              class="settings__textarea"
              placeholder="请输入系统提示词"
              :disabled="sysPromptSaving"
              rows="8"
            ></textarea>
            <div class="settings__actions">
              <button
                class="settings__btn settings__btn--secondary"
                :disabled="sysPromptSaving"
                @click="handleResetSysPrompt"
              >
                恢复默认
              </button>
              <button
                class="settings__btn settings__btn--primary"
                :disabled="sysPromptSaving"
                @click="handleSaveSysPrompt"
              >
                {{ sysPromptSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 关于 -->
      <div v-if="activeMenu === 'about'" class="settings__panel">
        <div class="settings__panel-title">关于</div>

        <!-- 交流群 -->
        <div class="settings__section">
          <div class="settings__section-title">交流群</div>
          <p class="settings__desc">扫描下方二维码加入微信交流群</p>
          <div class="settings__qrcode-wrapper">
            <img src="/fjm/wx.png" alt="微信交流群二维码" class="settings__qrcode" />
          </div>
        </div>

        <!-- 更新教程 -->
        <div class="settings__section">
          <div class="settings__section-title">更新教程</div>
          <p class="settings__desc">按照以下步骤更新到最新版本：</p>

          <div class="settings__tutorial">
            <div class="settings__tutorial-step">
              <div class="settings__step-number">1</div>
              <div class="settings__step-content">
                <div class="settings__step-title">停止当前容器</div>
                <div class="settings__code-block">
                  <code>docker stop xianyu-assistant</code>
                </div>
              </div>
            </div>

            <div class="settings__tutorial-step">
              <div class="settings__step-number">2</div>
              <div class="settings__step-content">
                <div class="settings__step-title">删除旧容器</div>
                <div class="settings__code-block">
                  <code>docker rm xianyu-assistant</code>
                </div>
              </div>
            </div>

            <div class="settings__tutorial-step">
              <div class="settings__step-number">3</div>
              <div class="settings__step-content">
                <div class="settings__step-title">拉取最新镜像</div>
                <div class="settings__code-block">
                  <code>docker pull iamlzy/xianyuassistant:latest</code>
                </div>
              </div>
            </div>

            <div class="settings__tutorial-step">
              <div class="settings__step-number">4</div>
              <div class="settings__step-content">
                <div class="settings__step-title">启动新容器（使用之前的数据目录）</div>
                <div class="settings__code-block">
                  <pre><code>docker run -d \
  --name xianyu-assistant \
  -p 12400:12400 \
  -v $(pwd)/data/dbdata:/app/dbdata \
  -v $(pwd)/data/logs:/app/logs \
  --restart unless-stopped \
  iamlzy/xianyuassistant:latest</code></pre>
                </div>
                <p class="settings__step-tip">💡 提示：数据目录路径保持不变，数据会自动迁移</p>
              </div>
            </div>
          </div>

          <div class="settings__warning-box">
            <div class="settings__warning-icon">⚠️</div>
            <div class="settings__warning-content">
              <strong>重要提示：</strong>
              <ul>
                <li>更新前请确保数据目录路径与之前一致，否则数据会丢失</li>
                <li>建议定期备份 <code>data/dbdata</code> 目录</li>
                <li>Windows 用户请将 <code>$(pwd)</code> 替换为实际路径，如 <code>D:\data</code></li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 开源地址 -->
        <div class="settings__section">
          <div class="settings__section-title">开源地址</div>
          <p class="settings__desc">本项目已开源，欢迎 Star 支持</p>
          <div class="settings__github-link">
            <a href="https://github.com/IAMLZY2018/XianYuAssistant" target="_blank" class="settings__link">
              https://github.com/IAMLZY2018/XianYuAssistant
            </a>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.settings {
  display: flex;
  gap: 24px;
  height: 100%;
  min-height: 0;
}

/* 左侧菜单 */
.settings__sidebar {
  width: 200px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.settings__sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1d1d1f;
  padding: 0 12px;
  margin-bottom: 16px;
}

.settings__menu {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.settings__menu-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  color: #6e6e73;
}

.settings__menu-item:hover {
  background: rgba(0, 0, 0, 0.04);
}

.settings__menu-item--active {
  background: rgba(0, 0, 0, 0.06);
  color: #1d1d1f;
  font-weight: 500;
}

.settings__menu-icon {
  font-size: 16px;
}

.settings__menu-label {
  font-size: 14px;
}

/* 右侧内容 */
.settings__content {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  padding: 24px;
  overflow-y: auto;
}

.settings__panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings__panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #1d1d1f;
}

.settings__section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid rgba(0, 0, 0, 0.06);
}

.settings__section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.settings__section-title {
  font-size: 15px;
  font-weight: 600;
  color: #1d1d1f;
  margin-bottom: 0;
}

.settings__toggle-btn {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 500;
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.1);
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.settings__toggle-btn:hover {
  background: rgba(0, 0, 0, 0.06);
  border-color: rgba(0, 0, 0, 0.15);
}

.settings__collapse-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.settings__desc {
  font-size: 13px;
  color: #6e6e73;
  margin: 0;
}

/* Loading */
.settings__loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: #86868b;
  font-size: 13px;
}

.settings__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 0, 0, 0.12);
  border-top-color: #1d1d1f;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* Info */
.settings__info {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.settings__info-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.settings__info-label {
  font-size: 13px;
  color: #86868b;
  min-width: 100px;
  flex-shrink: 0;
}

.settings__info-value {
  font-size: 14px;
  color: #1d1d1f;
  font-weight: 500;
}

/* Form */
.settings__form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings__field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.settings__label {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
}

.settings__label-hint {
  font-size: 12px;
  font-weight: 400;
  color: #86868b;
}

.settings__link {
  color: #0066cc;
  text-decoration: none;
}

.settings__link:hover {
  text-decoration: underline;
}

.settings__github-link {
  padding: 12px 0;
  font-size: 14px;
}

.settings__github-link .settings__link {
  font-weight: 500;
  word-break: break-all;
}

.settings__input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.settings__input {
  width: 100%;
  height: 40px;
  padding: 0 14px;
  font-size: 14px;
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.settings__input:focus {
  border-color: #1d1d1f;
  background: #fff;
}

.settings__input::placeholder {
  color: #86868b;
}

.settings__input:disabled {
  opacity: 0.5;
}

.settings__eye-btn {
  position: absolute;
  right: 10px;
  background: none;
  border: none;
  font-size: 12px;
  color: #86868b;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: color 0.2s;
}

.settings__eye-btn:hover {
  color: #1d1d1f;
}

.settings__textarea {
  width: 100%;
  min-height: 200px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: #1d1d1f;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  resize: vertical;
  font-family: inherit;
}

.settings__textarea:focus {
  border-color: #1d1d1f;
  background: #fff;
}

.settings__textarea::placeholder {
  color: #86868b;
}

.settings__textarea:disabled {
  opacity: 0.5;
}

/* Actions */
.settings__actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 8px;
}

.settings__btn {
  height: 36px;
  padding: 0 20px;
  font-size: 13px;
  font-weight: 500;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
}

.settings__btn--primary {
  background: #1d1d1f;
  color: #fff;
}

.settings__btn--primary:hover {
  background: #2a2a2a;
}

.settings__btn--primary:active {
  transform: scale(0.97);
}

.settings__btn--secondary {
  background: rgba(0, 0, 0, 0.04);
  color: #1d1d1f;
  border: 1px solid rgba(0, 0, 0, 0.12);
}

.settings__btn--secondary:hover {
  background: rgba(0, 0, 0, 0.06);
}

.settings__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.settings__btn--danger {
  background: #ff4d4f;
  color: #fff;
}

.settings__btn--danger:hover {
  background: #ff7875;
}

.settings__btn--danger:active {
  transform: scale(0.97);
}

/* AI Status */
.settings__ai-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: rgba(0, 0, 0, 0.02);
  border-radius: 8px;
}

.settings__ai-status-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.settings__ai-status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.settings__ai-status-badge--ok {
  background: #e6f7e6;
  color: #52c41a;
}

.settings__ai-status-badge--off {
  background: #fff1f0;
  color: #ff4d4f;
}

.settings__ai-status-msg {
  font-weight: 400;
  color: #6e6e73;
}

/* Logout */
.settings__logout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings__logout-text {
  font-size: 14px;
  color: #6e6e73;
  margin: 0 0 12px 0;
}

/* QR Code */
.settings__qrcode-wrapper {
  display: flex;
  justify-content: center;
  padding: 20px 0;
}

.settings__qrcode {
  max-width: 300px;
  width: 100%;
  height: auto;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

/* Responsive */
@media (max-width: 768px) {
  .settings {
    flex-direction: column;
    gap: 16px;
  }

  .settings__sidebar {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .settings__sidebar-title {
    width: 100%;
    margin-bottom: 8px;
  }

  .settings__menu {
    flex-direction: row;
    flex-wrap: wrap;
    gap: 8px;
  }

  .settings__menu-item {
    padding: 8px 12px;
  }

  .settings__content {
    padding: 16px;
  }

  .settings__qrcode {
    max-width: 250px;
  }
}

@media (max-width: 480px) {
  .settings__info-row,
  .settings__ai-status-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .settings__actions {
    flex-direction: column;
  }

  .settings__btn {
    width: 100%;
  }

  .settings__qrcode {
    max-width: 200px;
  }
}

/* 更新教程样式 */
.settings__tutorial {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 16px;
}

.settings__tutorial-step {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.settings__step-number {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
  flex-shrink: 0;
}

.settings__step-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.settings__step-title {
  font-size: 14px;
  font-weight: 600;
  color: #1d1d1f;
}

.settings__code-block {
  background: #f5f5f7;
  border: 1px solid rgba(0, 0, 0, 0.08);
  border-radius: 8px;
  padding: 12px 16px;
  overflow-x: auto;
}

.settings__code-block pre {
  margin: 0;
  padding: 0;
  background: transparent;
  font-family: inherit;
}

.settings__code-block code {
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Code', monospace;
  font-size: 13px;
  color: #1d1d1f;
  display: block;
}

.settings__step-tip {
  font-size: 12px;
  color: #6e6e73;
  margin: 0;
  padding: 8px 12px;
  background: #f0f9ff;
  border-left: 3px solid #0ea5e9;
  border-radius: 4px;
}

.settings__warning-box {
  display: flex;
  gap: 12px;
  margin-top: 20px;
  padding: 16px;
  background: #fff9f0;
  border: 1px solid #ffedd5;
  border-radius: 8px;
}

.settings__warning-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.settings__warning-content {
  flex: 1;
  font-size: 13px;
  color: #1d1d1f;
}

.settings__warning-content strong {
  display: block;
  margin-bottom: 8px;
  color: #c2410c;
}

.settings__warning-content ul {
  margin: 0;
  padding-left: 20px;
}

.settings__warning-content li {
  margin-bottom: 6px;
  color: #6e6e73;
}

.settings__warning-content li:last-child {
  margin-bottom: 0;
}

.settings__warning-content code {
  background: rgba(0, 0, 0, 0.06);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Code', monospace;
}
</style>
