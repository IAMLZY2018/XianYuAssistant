<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getCurrentUser, changePassword } from '@/api/system'
import { logout } from '@/api/auth'
import { getSetting, saveSetting, testEmail } from '@/api/setting'
import { getAIStatus } from '@/api/ai'
import { getBackupModules, exportBackup, importBackup, getLogDates, downloadLog, type BackupModule } from '@/api/backup'
import { toast } from '@/utils/toast'
import { showConfirm } from '@/utils/confirm'
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

// 相似度阈值
const SIMILARITY_THRESHOLD_KEY = 'similarity_threshold'
const DEFAULT_SIMILARITY_THRESHOLD = 0.1
const similarityThreshold = ref(DEFAULT_SIMILARITY_THRESHOLD)
const similarityThresholdSaving = ref(false)

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

// 邮箱通知配置
const EMAIL_SMTP_HOST_KEY = 'email_smtp_host'
const EMAIL_SMTP_PORT_KEY = 'email_smtp_port'
const EMAIL_SMTP_USERNAME_KEY = 'email_smtp_username'
const EMAIL_SMTP_PASSWORD_KEY = 'email_smtp_password'
const EMAIL_SMTP_FROM_KEY = 'email_smtp_from'
const EMAIL_SMTP_SSL_KEY = 'email_smtp_ssl'
const EMAIL_WS_DISCONNECT_NOTIFY_KEY = 'email_notify_ws_disconnect_enabled'
const EMAIL_COOKIE_EXPIRE_NOTIFY_KEY = 'email_notify_cookie_expire_enabled'

const emailSmtpHost = ref('')
const emailSmtpPort = ref('465')
const emailSmtpUsername = ref('')
const emailSmtpPassword = ref('')
const emailSmtpFrom = ref('')
const emailSmtpSsl = ref(true)
const emailSaving = ref(false)
const emailTesting = ref(false)
const showEmailPassword = ref(false)
const emailConfigured = ref(false)
const emailConfigExpanded = ref(true)
const wsDisconnectNotifyEnabled = ref(false)
const cookieExpireNotifyEnabled = ref(false)

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
  { key: 'email', label: '邮箱通知', icon: '📧' },
  { key: 'backup', label: '备份与恢复', icon: '💾' },
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

  // 加载相似度阈值配置
  try {
    const res = await getSetting({ settingKey: SIMILARITY_THRESHOLD_KEY })
    if (res.code === 200 && res.data && res.data.settingValue) {
      similarityThreshold.value = parseFloat(res.data.settingValue) || DEFAULT_SIMILARITY_THRESHOLD
    }
  } catch (e) {
    console.error('获取相似度阈值配置失败:', e)
  }

  // 加载 AI 配置
  await loadAIConfig()
  // 加载 Embedding 配置
  await loadEmbeddingConfig()
  // 加载 AI 状态
  await loadAIStatus()
  // 加载邮箱通知配置
  await loadEmailConfig()
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
    toast.warning('请输入原密码')
    return
  }
  if (!newPassword.value || newPassword.value.length < 6) {
    toast.warning('新密码长度需在6-50之间')
    return
  }
  if (newPassword.value !== confirmPassword.value) {
    toast.warning('两次密码不一致')
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
      toast.success('密码修改成功')
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
    await showConfirm(
      '确定要退出登录吗？',
      '退出确认'
    )

    loggingOut.value = true
    try {
      await logout()
      clearAuthToken()
      toast.success('已退出登录')
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
    toast.warning('系统提示词不能为空')
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
      toast.success('系统提示词保存成功')
      sysPromptLoaded.value = true
    }
  } finally {
    sysPromptSaving.value = false
  }
}

function handleResetSysPrompt() {
  sysPromptValue.value = DEFAULT_SYS_PROMPT
}

async function handleSaveSimilarityThreshold() {
  if (similarityThreshold.value < 0 || similarityThreshold.value > 1) {
    toast.warning('相似度阈值必须在 0 到 1 之间')
    return
  }
  similarityThresholdSaving.value = true
  try {
    const res = await saveSetting({
      settingKey: SIMILARITY_THRESHOLD_KEY,
      settingValue: similarityThreshold.value.toString(),
      settingDesc: 'RAG向量搜索的相似度阈值（0-1之间，值越小匹配越宽松）'
    })
    if (res.code === 200) {
      toast.success('相似度阈值保存成功')
    }
  } catch (e) {
    console.error('保存相似度阈值失败:', e)
    toast.error('保存相似度阈值失败')
  } finally {
    similarityThresholdSaving.value = false
  }
}

function handleResetSimilarityThreshold() {
  similarityThreshold.value = DEFAULT_SIMILARITY_THRESHOLD
}

async function handleSaveAIConfig() {
  if (!aiApiKey.value.trim()) {
    toast.warning('API Key 不能为空')
    return
  }
  if (!aiBaseUrl.value.trim()) {
    toast.warning('API Base URL 不能为空')
    return
  }
  if (!aiModel.value.trim()) {
    toast.warning('模型名称不能为空')
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
      toast.success('AI 配置保存成功，已立即生效')
      // 刷新 AI 状态
      await loadAIStatus()
    }
  } catch (e) {
    console.error('保存AI配置失败:', e)
    toast.error('保存AI配置失败')
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
      toast.success('Embedding 配置保存成功，重启服务后生效')
    }
  } catch (e) {
    console.error('保存Embedding配置失败:', e)
    toast.error('保存Embedding配置失败')
  } finally {
    embeddingSaving.value = false
  }
}

function handleResetEmbeddingConfig() {
  embeddingApiKey.value = ''
  embeddingBaseUrl.value = ''
  embeddingModel.value = DEFAULT_EMBEDDING_MODEL
}

async function loadEmailConfig() {
  try {
    const [hostRes, portRes, userRes, passRes, fromRes, sslRes, wsDisconnectRes, cookieExpireRes] = await Promise.all([
      getSetting({ settingKey: EMAIL_SMTP_HOST_KEY }),
      getSetting({ settingKey: EMAIL_SMTP_PORT_KEY }),
      getSetting({ settingKey: EMAIL_SMTP_USERNAME_KEY }),
      getSetting({ settingKey: EMAIL_SMTP_PASSWORD_KEY }),
      getSetting({ settingKey: EMAIL_SMTP_FROM_KEY }),
      getSetting({ settingKey: EMAIL_SMTP_SSL_KEY }),
      getSetting({ settingKey: EMAIL_WS_DISCONNECT_NOTIFY_KEY }),
      getSetting({ settingKey: EMAIL_COOKIE_EXPIRE_NOTIFY_KEY })
    ])

    if (hostRes.code === 200 && hostRes.data) emailSmtpHost.value = hostRes.data.settingValue || ''
    if (portRes.code === 200 && portRes.data && portRes.data.settingValue) emailSmtpPort.value = portRes.data.settingValue
    if (userRes.code === 200 && userRes.data) emailSmtpUsername.value = userRes.data.settingValue || ''
    if (passRes.code === 200 && passRes.data) emailSmtpPassword.value = passRes.data.settingValue || ''
    if (fromRes.code === 200 && fromRes.data) emailSmtpFrom.value = fromRes.data.settingValue || ''
    if (sslRes.code === 200 && sslRes.data && sslRes.data.settingValue !== undefined) {
      emailSmtpSsl.value = sslRes.data.settingValue === '1' || sslRes.data.settingValue === 'true'
    } else {
      emailSmtpSsl.value = true
    }
    if (wsDisconnectRes.code === 200 && wsDisconnectRes.data && wsDisconnectRes.data.settingValue) {
      wsDisconnectNotifyEnabled.value = wsDisconnectRes.data.settingValue === '1' || wsDisconnectRes.data.settingValue === 'true'
    }
    if (cookieExpireRes.code === 200 && cookieExpireRes.data && cookieExpireRes.data.settingValue) {
      cookieExpireNotifyEnabled.value = cookieExpireRes.data.settingValue === '1' || cookieExpireRes.data.settingValue === 'true'
    }

    emailConfigured.value = !!(emailSmtpHost.value && emailSmtpPort.value && emailSmtpUsername.value && emailSmtpPassword.value && emailSmtpFrom.value)
    emailConfigExpanded.value = !emailConfigured.value
  } catch (e) {
    console.error('加载邮箱配置失败:', e)
  }
}

async function handleSaveEmailConfig() {
  emailSaving.value = true
  try {
    const [hostRes, portRes, userRes, passRes, fromRes, sslRes] = await Promise.all([
      saveSetting({ settingKey: EMAIL_SMTP_HOST_KEY, settingValue: emailSmtpHost.value.trim(), settingDesc: 'SMTP服务器地址' }),
      saveSetting({ settingKey: EMAIL_SMTP_PORT_KEY, settingValue: emailSmtpPort.value.trim(), settingDesc: 'SMTP服务器端口' }),
      saveSetting({ settingKey: EMAIL_SMTP_USERNAME_KEY, settingValue: emailSmtpUsername.value.trim(), settingDesc: 'SMTP登录用户名' }),
      saveSetting({ settingKey: EMAIL_SMTP_PASSWORD_KEY, settingValue: emailSmtpPassword.value.trim(), settingDesc: 'SMTP登录密码/授权码' }),
      saveSetting({ settingKey: EMAIL_SMTP_FROM_KEY, settingValue: emailSmtpFrom.value.trim(), settingDesc: '接收通知的收件人邮箱地址' }),
      saveSetting({ settingKey: EMAIL_SMTP_SSL_KEY, settingValue: emailSmtpSsl.value ? '1' : '0', settingDesc: '是否启用SSL（1启用，0关闭）' })
    ])

    if (hostRes.code === 200 && portRes.code === 200 && userRes.code === 200 && passRes.code === 200 && fromRes.code === 200 && sslRes.code === 200) {
      toast.success('邮箱配置保存成功')
      emailConfigured.value = !!(emailSmtpHost.value && emailSmtpPort.value && emailSmtpUsername.value && emailSmtpPassword.value && emailSmtpFrom.value)
      emailConfigExpanded.value = !emailConfigured.value
    }
  } catch (e) {
    console.error('保存邮箱配置失败:', e)
    toast.error('保存邮箱配置失败')
  } finally {
    emailSaving.value = false
  }
}

async function handleSaveWsDisconnectNotify() {
  try {
    const res = await saveSetting({
      settingKey: EMAIL_WS_DISCONNECT_NOTIFY_KEY,
      settingValue: wsDisconnectNotifyEnabled.value ? '1' : '0',
      settingDesc: 'WebSocket断开且无法重连时邮箱通知开关（1启用，0关闭）'
    })
    if (res.code === 200) {
      toast.success(`闲鱼账号消息监听掉线通知已${wsDisconnectNotifyEnabled.value ? '开启' : '关闭'}`)
    }
  } catch (e) {
    console.error('保存通知开关失败:', e)
    toast.error('保存失败')
  }
}

async function handleSaveCookieExpireNotify() {
  try {
    const res = await saveSetting({
      settingKey: EMAIL_COOKIE_EXPIRE_NOTIFY_KEY,
      settingValue: cookieExpireNotifyEnabled.value ? '1' : '0',
      settingDesc: 'Cookie过期且无法续期时邮箱通知开关（1启用，0关闭）'
    })
    if (res.code === 200) {
      toast.success(`Cookie过期通知已${cookieExpireNotifyEnabled.value ? '开启' : '关闭'}`)
    }
  } catch (e) {
    console.error('保存通知开关失败:', e)
    toast.error('保存失败')
  }
}

async function handleTestEmail() {
  emailTesting.value = true
  try {
    const res = await testEmail()
    if (res.code === 200) {
      toast.success('测试邮件发送成功，请检查收件箱')
    } else {
      toast.error(res.msg || '测试邮件发送失败')
    }
  } catch (e: any) {
    console.error('测试邮箱失败:', e)
    toast.error(e.message || '测试邮箱失败')
  } finally {
    emailTesting.value = false
  }
}

// 备份与恢复
const backupModules = ref<BackupModule[]>([])
const backupSelectedModules = ref<string[]>([])
const backupLoaded = ref(false)
const backupExporting = ref(false)
const backupImporting = ref(false)
const backupExportProgress = ref(0)
const backupImportProgress = ref(0)

const logDates = ref<string[]>([])
const logSelectedDate = ref('')
const logDownloading = ref(false)
const logDatesLoaded = ref(false)

async function loadBackupModules() {
  if (backupLoaded.value) return
  try {
    const res = await getBackupModules()
    if (res.code === 200 && res.data) {
      backupModules.value = res.data
      backupSelectedModules.value = res.data.map((m: BackupModule) => m.moduleKey)
      backupLoaded.value = true
    }
  } catch (e) {
    console.error('获取备份模块列表失败:', e)
  }
}

async function loadLogDates() {
  if (logDatesLoaded.value) return
  try {
    const res = await getLogDates()
    if (res.code === 200 && res.data) {
      logDates.value = res.data
      const today = new Date().toISOString().slice(0, 10)
      logSelectedDate.value = res.data.includes(today) ? today : (res.data.length > 0 ? res.data[res.data.length - 1]! : '')
      logDatesLoaded.value = true
    }
  } catch (e) {
    console.error('获取日志日期列表失败:', e)
  }
}

async function handleDownloadLog() {
  if (!logSelectedDate.value) return
  logDownloading.value = true
  try {
    const blob = await downloadLog(logSelectedDate.value)
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `logs-${logSelectedDate.value}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
  } catch (e) {
    console.error('下载日志失败:', e)
    toast.error('下载日志失败')
  } finally {
    setTimeout(() => { logDownloading.value = false }, 2000)
  }
}

function toggleBackupModule(key: string) {
  const idx = backupSelectedModules.value.indexOf(key)
  if (idx >= 0) {
    backupSelectedModules.value.splice(idx, 1)
  } else {
    backupSelectedModules.value.push(key)
  }
}

function toggleAllBackupModules() {
  if (backupSelectedModules.value.length === backupModules.value.length) {
    backupSelectedModules.value = []
  } else {
    backupSelectedModules.value = backupModules.value.map(m => m.moduleKey)
  }
}

async function handleExportBackup() {
  if (backupSelectedModules.value.length === 0) {
    toast.warning('请至少选择一个模块')
    return
  }
  backupExporting.value = true
  backupExportProgress.value = 0
  try {
    const total = backupSelectedModules.value.length
    const progressStep = 100 / total
    for (let i = 0; i < total; i++) {
      backupExportProgress.value = Math.round((i + 1) * progressStep)
      await new Promise(r => setTimeout(r, 100))
    }
    const res = await exportBackup({ modules: backupSelectedModules.value })
    if (res.code === 200 && res.data) {
      backupExportProgress.value = 100
      const jsonStr = res.data.jsonData
      const blob = new Blob([jsonStr], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      const now = new Date()
      const ts = `${now.getFullYear()}${String(now.getMonth()+1).padStart(2,'0')}${String(now.getDate()).padStart(2,'0')}_${String(now.getHours()).padStart(2,'0')}${String(now.getMinutes()).padStart(2,'0')}${String(now.getSeconds()).padStart(2,'0')}`
      a.download = `xianyu_backup_${ts}.json`
      document.body.appendChild(a)
      a.click()
      document.body.removeChild(a)
      URL.revokeObjectURL(url)
      toast.success('备份导出成功')
    } else {
      toast.error(res.msg || '导出失败')
    }
  } catch (e: any) {
    console.error('导出备份失败:', e)
    toast.error(e.message || '导出失败')
  } finally {
    backupExporting.value = false
    backupExportProgress.value = 0
  }
}

const importFileInput = ref<HTMLInputElement | null>(null)
const importJsonData = ref('')
const importFileName = ref('')

function triggerImportFile() {
  importFileInput.value?.click()
}

function handleImportFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return
  importFileName.value = file.name
  const reader = new FileReader()
  reader.onload = (ev) => {
    importJsonData.value = ev.target?.result as string
  }
  reader.readAsText(file)
}

async function handleImportBackup() {
  if (!importJsonData.value) {
    toast.warning('请先选择备份文件')
    return
  }
  if (backupSelectedModules.value.length === 0) {
    toast.warning('请至少选择一个模块')
    return
  }

  try {
    await showConfirm(
      '导入数据将覆盖当前选中模块的已有数据，是否继续？',
      '确认导入'
    )
  } catch {
    return
  }

  backupImporting.value = true
  backupImportProgress.value = 0
  try {
    const total = backupSelectedModules.value.length
    const progressStep = 100 / (total + 1)
    for (let i = 0; i < total; i++) {
      backupImportProgress.value = Math.round((i + 1) * progressStep)
      await new Promise(r => setTimeout(r, 100))
    }
    const res = await importBackup({ jsonData: importJsonData.value, modules: backupSelectedModules.value })
    if (res.code === 200 && res.data) {
      backupImportProgress.value = 100
      const result = res.data
      if (result.failedModules && result.failedModules.length > 0) {
        toast.warning(`导入完成：${result.successCount}/${result.totalCount} 成功，失败模块：${result.failedModules.join(', ')}`)
      } else {
        toast.success(`导入成功：${result.successCount} 个模块`)
      }
      importJsonData.value = ''
      importFileName.value = ''
      if (importFileInput.value) importFileInput.value.value = ''
    } else {
      toast.error(res.msg || '导入失败')
    }
  } catch (e: any) {
    console.error('导入备份失败:', e)
    toast.error(e.message || '导入失败')
  } finally {
    backupImporting.value = false
    backupImportProgress.value = 0
  }
}

function handleBackupMenuEnter() {
  loadBackupModules()
  loadLogDates()
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
          @click="activeMenu = item.key; item.key === 'backup' && handleBackupMenuEnter()"
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

        <!-- 相似度阈值 -->
        <div class="settings__section">
          <div class="settings__section-title">相似度阈值</div>
          <p class="settings__desc">
            配置 RAG 向量搜索的相似度阈值。值越小，匹配越宽松，会返回更多相关度较低的结果；值越大，匹配越严格，只返回高度相关的结果。
          </p>
          <div class="settings__form">
            <div class="settings__field">
              <label class="settings__label">相似度阈值 (0-1)</label>
              <input
                v-model.number="similarityThreshold"
                type="number"
                class="settings__input"
                placeholder="0.1"
                :disabled="similarityThresholdSaving"
                min="0"
                max="1"
                step="0.01"
              />
              <p class="settings__hint">推荐值：0.1（宽松）到 0.5（严格）之间</p>
            </div>
            <div class="settings__actions">
              <button
                class="settings__btn settings__btn--secondary"
                :disabled="similarityThresholdSaving"
                @click="handleResetSimilarityThreshold"
              >
                恢复默认
              </button>
              <button
                class="settings__btn settings__btn--primary"
                :disabled="similarityThresholdSaving"
                @click="handleSaveSimilarityThreshold"
              >
                {{ similarityThresholdSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 邮箱通知 -->
      <div v-if="activeMenu === 'email'" class="settings__panel">
        <div class="settings__panel-title">邮箱通知</div>

        <!-- 邮箱配置 -->
        <div class="settings__section">
          <div class="settings__section-header">
            <div class="settings__section-title">邮箱配置</div>
            <div class="settings__section-header-actions">
              <button
                v-if="emailConfigured"
                class="settings__toggle-btn settings__toggle-btn--test"
                :disabled="emailTesting"
                @click="handleTestEmail"
              >
                {{ emailTesting ? '发送中...' : '发送测试邮件' }}
              </button>
              <span v-if="emailConfigured" class="settings__status-badge" :class="emailConfigured ? 'settings__status-badge--success' : ''">
                {{ emailConfigured ? '已配置' : '未配置' }}
              </span>
              <button
                v-if="emailConfigured"
                class="settings__toggle-btn"
                @click="emailConfigExpanded = !emailConfigExpanded"
              >
                {{ emailConfigExpanded ? '收起' : '展开' }}
              </button>
            </div>
          </div>
          <p class="settings__desc">配置SMTP邮箱服务，用于接收系统通知（如滑块验证提醒）。发件人固定使用SMTP用户名，收件人邮箱用于接收通知。</p>

          <div v-if="emailConfigExpanded || !emailConfigured" class="settings__form">
            <div class="settings__field">
              <label class="settings__label">SMTP 服务器</label>
              <input
                v-model="emailSmtpHost"
                type="text"
                class="settings__input"
                placeholder="如 smtp.qq.com"
                :disabled="emailSaving"
              />
            </div>
            <div class="settings__field">
              <label class="settings__label">SMTP 端口</label>
              <input
                v-model="emailSmtpPort"
                type="text"
                class="settings__input"
                placeholder="465"
                :disabled="emailSaving"
              />
            </div>
            <div class="settings__field">
              <label class="settings__label">用户名</label>
              <input
                v-model="emailSmtpUsername"
                type="text"
                class="settings__input"
                placeholder="邮箱账号"
                :disabled="emailSaving"
              />
            </div>
            <div class="settings__field">
              <label class="settings__label">密码/授权码</label>
              <div class="settings__input-wrap">
                <input
                  v-model="emailSmtpPassword"
                  :type="showEmailPassword ? 'text' : 'password'"
                  class="settings__input"
                  placeholder="邮箱密码或SMTP授权码"
                  :disabled="emailSaving"
                />
                <button class="settings__eye-btn" @click="showEmailPassword = !showEmailPassword" tabindex="-1">
                  {{ showEmailPassword ? '隐藏' : '显示' }}
                </button>
              </div>
              <p class="settings__hint">QQ邮箱需使用授权码，非登录密码</p>
            </div>
            <div class="settings__field">
              <label class="settings__label">收件人邮箱</label>
              <input
                v-model="emailSmtpFrom"
                type="text"
                class="settings__input"
                placeholder="接收通知的邮箱地址"
                :disabled="emailSaving"
              />
            </div>
            <div class="settings__field">
              <label class="settings__label">启用 SSL</label>
              <label class="settings__switch">
                <input type="checkbox" v-model="emailSmtpSsl" :disabled="emailSaving" />
                <span class="settings__switch-track"></span>
                <span class="settings__switch-thumb"></span>
              </label>
            </div>
            <div class="settings__actions">
              <button
                class="settings__btn settings__btn--primary"
                :disabled="emailSaving"
                @click="handleSaveEmailConfig"
              >
                {{ emailSaving ? '保存中...' : '保存' }}
              </button>
            </div>
          </div>
        </div>

        <!-- 通知开关 -->
        <div class="settings__section">
          <div class="settings__section-title">通知开关</div>
          <p class="settings__desc">选择哪些事件需要通过邮件通知您</p>

          <table class="settings__notify-table">
            <thead>
              <tr>
                <th class="settings__notify-th">通知类型</th>
                <th class="settings__notify-th">说明</th>
                <th class="settings__notify-th">状态</th>
              </tr>
            </thead>
            <tbody>
              <tr class="settings__notify-tr">
                <td class="settings__notify-td">闲鱼账号消息监听掉线通知</td>
                <td class="settings__notify-td settings__notify-td--desc">WebSocket连接失败时发送邮件通知</td>
                <td class="settings__notify-td">
                  <label class="settings__switch">
                    <input
                      type="checkbox"
                      v-model="wsDisconnectNotifyEnabled"
                      :disabled="!emailConfigured"
                      @change="handleSaveWsDisconnectNotify"
                    />
                    <span class="settings__switch-track"></span>
                    <span class="settings__switch-thumb"></span>
                  </label>
                </td>
              </tr>
              <tr class="settings__notify-tr">
                <td class="settings__notify-td">Cookie过期通知</td>
                <td class="settings__notify-td settings__notify-td--desc">Cookie过期且无法自动续期时发送邮件通知</td>
                <td class="settings__notify-td">
                  <label class="settings__switch">
                    <input
                      type="checkbox"
                      v-model="cookieExpireNotifyEnabled"
                      :disabled="!emailConfigured"
                      @change="handleSaveCookieExpireNotify"
                    />
                    <span class="settings__switch-track"></span>
                    <span class="settings__switch-thumb"></span>
                  </label>
                </td>
              </tr>
            </tbody>
          </table>
          <p v-if="!emailConfigured" class="settings__hint" style="color:#e6a23c;margin-top:8px;">请先配置邮箱后再开启通知</p>
        </div>
      </div>

      <!-- 备份与恢复 -->
      <div v-if="activeMenu === 'backup'" class="settings__panel">
        <div class="settings__panel-title">备份与恢复</div>

        <div class="settings__section">
          <div class="settings__section-title">选择备份模块</div>
          <p class="settings__desc">选择需要导出或导入的数据模块，默认全部选择</p>
          <div class="settings__backup-modules">
            <div class="settings__backup-module-all">
              <label class="settings__checkbox-label" @click.prevent="toggleAllBackupModules">
                <span class="settings__checkbox" :class="{ 'settings__checkbox--checked': backupSelectedModules.length === backupModules.length && backupModules.length > 0 }">
                  <span v-if="backupSelectedModules.length === backupModules.length && backupModules.length > 0" class="settings__checkbox-tick">✓</span>
                </span>
                全选
              </label>
            </div>
            <div v-for="mod in backupModules" :key="mod.moduleKey" class="settings__backup-module-item">
              <label class="settings__checkbox-label" @click.prevent="toggleBackupModule(mod.moduleKey)">
                <span class="settings__checkbox" :class="{ 'settings__checkbox--checked': backupSelectedModules.includes(mod.moduleKey) }">
                  <span v-if="backupSelectedModules.includes(mod.moduleKey)" class="settings__checkbox-tick">✓</span>
                </span>
                {{ mod.moduleName }}
              </label>
            </div>
          </div>
        </div>

        <div class="settings__section">
          <div class="settings__section-title">导出备份</div>
          <p class="settings__desc">将选中模块的数据导出为 JSON 文件</p>
          <div v-if="backupExporting" class="settings__progress-wrap">
            <div class="settings__progress-bar">
              <div class="settings__progress-fill" :style="{ width: backupExportProgress + '%' }"></div>
            </div>
            <span class="settings__progress-text">{{ backupExportProgress }}%</span>
          </div>
          <div class="settings__actions">
            <button
              class="settings__btn settings__btn--primary"
              :disabled="backupExporting || backupSelectedModules.length === 0"
              @click="handleExportBackup"
            >
              {{ backupExporting ? '导出中...' : '导出备份' }}
            </button>
          </div>
        </div>

        <div class="settings__section">
          <div class="settings__section-title">导入恢复</div>
          <p class="settings__desc">从 JSON 备份文件中恢复数据（将覆盖当前选中模块的已有数据）</p>
          <input
            ref="importFileInput"
            type="file"
            accept=".json"
            class="settings__file-input"
            @change="handleImportFileChange"
          />
          <div class="settings__import-file-row">
            <button
              class="settings__btn settings__btn--secondary"
              :disabled="backupImporting"
              @click="triggerImportFile"
            >
              选择文件
            </button>
            <span v-if="importFileName" class="settings__import-file-name">{{ importFileName }}</span>
            <span v-else class="settings__import-file-hint">未选择文件</span>
          </div>
          <div v-if="backupImporting" class="settings__progress-wrap">
            <div class="settings__progress-bar">
              <div class="settings__progress-fill" :style="{ width: backupImportProgress + '%' }"></div>
            </div>
            <span class="settings__progress-text">{{ backupImportProgress }}%</span>
          </div>
          <div class="settings__actions">
            <button
              class="settings__btn settings__btn--danger"
              :disabled="backupImporting || !importJsonData || backupSelectedModules.length === 0"
              @click="handleImportBackup"
            >
              {{ backupImporting ? '导入中...' : '导入恢复' }}
            </button>
          </div>
        </div>

        <div class="settings__section">
          <div class="settings__section-title">日志打包下载</div>
          <p class="settings__desc">选择日期，将该天的日志文件打包为 ZIP 下载</p>
          <div class="settings__log-pack-row">
            <select v-model="logSelectedDate" class="settings__log-select">
              <option v-for="d in logDates" :key="d" :value="d">{{ d }}</option>
            </select>
            <button
              class="settings__btn settings__btn--primary"
              :disabled="!logSelectedDate || logDownloading"
              @click="handleDownloadLog"
            >
              {{ logDownloading ? '打包中...' : '下载日志' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 关于 -->
      <div v-if="activeMenu === 'about'" class="settings__panel">
        <div class="settings__panel-title">关于</div>

        <!-- 交流群 -->
        <div class="settings__section">
          <div class="settings__section-title">交流群</div>
          <p class="settings__desc">扫描下方二维码添加好友邀请您加入微信交流群</p>
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
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.10), 0 1.5px 4px rgba(0,0,0,0.06);
  padding: 16px;
  display: flex;
  flex-direction: column;
}

.settings__sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1c1c1e;
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
  color: rgba(28,28,30,.55);
}

.settings__menu-item:hover {
  background: rgba(255,255,255,0.38);
}

.settings__menu-item--active {
  background: rgba(60,60,67,.12);
  color: #1c1c1e;
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
  background: rgba(255,255,255,0.55);
  backdrop-filter: blur(28px) saturate(1.8);
  -webkit-backdrop-filter: blur(28px) saturate(1.8);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0,0,0,0.10), 0 1.5px 4px rgba(0,0,0,0.06);
  padding: 24px;
  overflow-y: auto;
  position: relative;
}

.settings__content::before {
  content: '';
  position: absolute;
  top: 0;
  left: 10%;
  right: 10%;
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(255,255,255,0.9) 30%, rgba(255,255,255,0.9) 70%, transparent);
  border-radius: 1px;
  pointer-events: none;
}

.settings__panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings__panel-title {
  font-size: 18px;
  font-weight: 600;
  color: #1c1c1e;
}

.settings__section {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 0.5px solid rgba(60,60,67,.12);
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
  color: #1c1c1e;
  margin-bottom: 0;
}

.settings__toggle-btn {
  height: 28px;
  padding: 0 12px;
  font-size: 12px;
  font-weight: 590;
  color: #0A84FF;
  background: rgba(255,255,255,0.70);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border: 1px solid rgba(255,255,255,0.85);
  border-radius: 100px;
  cursor: pointer;
  transition: opacity .15s, transform .12s, box-shadow .15s;
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 1.5px 4px rgba(0,0,0,0.04);
}

.settings__toggle-btn:hover {
  background: rgba(255,255,255,0.80);
}

.settings__toggle-btn:active { opacity: .80; transform: scale(.96); }

.settings__collapse-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
}

.settings__desc {
  font-size: 13px;
  color: rgba(28,28,30,.55);
  margin: 0;
}

/* Loading */
.settings__loading {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 16px;
  color: rgba(28,28,30,.55);
  font-size: 13px;
}

.settings__spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(0, 0, 0, 0.12);
  border-top-color: #1c1c1e;
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
  color: rgba(28,28,30,.55);
  min-width: 100px;
  flex-shrink: 0;
}

.settings__info-value {
  font-size: 14px;
  color: #1c1c1e;
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
  color: #1c1c1e;
}

.settings__label-hint {
  font-size: 12px;
  font-weight: 400;
  color: rgba(28,28,30,.55);
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
  color: #1c1c1e;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(60,60,67,.12);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.settings__input:focus {
  border-color: #1c1c1e;
  background: rgba(255,255,255,0.55);
}

.settings__input::placeholder {
  color: rgba(28,28,30,.55);
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
  color: rgba(28,28,30,.55);
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 4px;
  transition: color 0.2s;
}

.settings__eye-btn:hover {
  color: #1c1c1e;
}

.settings__textarea {
  width: 100%;
  min-height: 200px;
  padding: 12px 14px;
  font-size: 14px;
  line-height: 1.6;
  color: #1c1c1e;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(60,60,67,.12);
  border-radius: 8px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
  resize: vertical;
  font-family: inherit;
}

.settings__textarea:focus {
  border-color: #1c1c1e;
  background: rgba(255,255,255,0.55);
}

.settings__textarea::placeholder {
  color: rgba(28,28,30,.55);
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
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 36px;
  padding: 0 18px;
  font-size: 13px;
  font-weight: 590;
  border-radius: 100px;
  border: none;
  cursor: pointer;
  transition: opacity .15s, transform .12s, box-shadow .15s;
  white-space: nowrap;
  min-width: 40px;
  -webkit-tap-highlight-color: transparent;
  font-family: inherit;
  user-select: none;
}

.settings__btn--primary {
  color: #fff;
  background: rgba(10,132,255,0.85);
  backdrop-filter: blur(20px) saturate(1.8);
  -webkit-backdrop-filter: blur(20px) saturate(1.8);
  border: 1px solid rgba(255,255,255,0.35);
  box-shadow: 0 4px 16px rgba(10,132,255,0.35), 0 8px 32px rgba(0,0,0,0.08), 0 1.5px 4px rgba(0,0,0,0.04);
}

@media (hover: hover) {
  .settings__btn--primary:hover {
    background: rgba(10,132,255,0.95);
    box-shadow: 0 6px 20px rgba(10,132,255,0.45), 0 8px 32px rgba(0,0,0,0.08), 0 1.5px 4px rgba(0,0,0,0.04);
  }
}

.settings__btn--primary:active { opacity: .80; transform: scale(.96); }

.settings__btn--secondary {
  color: #0A84FF;
  background: rgba(255,255,255,0.70);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border: 1px solid rgba(255,255,255,0.85);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 1.5px 4px rgba(0,0,0,0.04);
}

@media (hover: hover) {
  .settings__btn--secondary:hover {
    background: rgba(255,255,255,0.80);
  }
}

.settings__btn--secondary:active { opacity: .80; transform: scale(.96); }

.settings__btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.settings__btn--danger {
  color: #FF453A;
  background: rgba(255,69,58,0.15);
  backdrop-filter: blur(16px) saturate(1.6);
  -webkit-backdrop-filter: blur(16px) saturate(1.6);
  border: 1px solid rgba(255,69,58,0.2);
  box-shadow: 0 8px 32px rgba(0,0,0,0.08), 0 1.5px 4px rgba(0,0,0,0.04);
}

@media (hover: hover) {
  .settings__btn--danger:hover {
    background: rgba(255,69,58,0.22);
    border-color: rgba(255,69,58,0.35);
  }
}

.settings__btn--danger:active { opacity: .80; transform: scale(.96); }

/* AI Status */
.settings__ai-status {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  background: rgba(255,255,255,0.15);
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
  background: rgba(255,255,255,0.55)1f0;
  color: #ff4d4f;
}

.settings__ai-status-msg {
  font-weight: 400;
  color: rgba(28,28,30,.55);
}

/* Logout */
.settings__logout {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.settings__logout-text {
  font-size: 14px;
  color: rgba(28,28,30,.55);
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
  color: #1c1c1e;
}

.settings__code-block {
  background: #f5f5f7;
  border: 1px solid rgba(60,60,67,.08);
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
  color: #1c1c1e;
  display: block;
}

.settings__step-tip {
  font-size: 12px;
  color: rgba(28,28,30,.55);
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
  background: rgba(255,255,255,0.55)9f0;
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
  color: #1c1c1e;
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
  color: rgba(28,28,30,.55);
}

.settings__warning-content li:last-child {
  margin-bottom: 0;
}

.settings__warning-content code {
  background: rgba(60,60,67,.12);
  padding: 2px 6px;
  border-radius: 3px;
  font-size: 12px;
  font-family: 'SF Mono', 'Monaco', 'Inconsolata', 'Fira Code', monospace;
}

.settings__section-header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.settings__status-badge {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  background: rgba(60,60,67,.12);
  color: rgba(28,28,30,.55);
}

.settings__status-badge--success {
  background: rgba(48,209,88,0.12);
  color: #30D158;
}

.settings__switch {
  position: relative;
  display: inline-flex;
  align-items: center;
  cursor: pointer;
}

.settings__switch input {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
}

.settings__switch-track {
  width: 51px;
  height: 31px;
  background: rgba(120,120,128,.24);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid rgba(255,255,255,.3);
  border-radius: 100px;
  transition: background 0.2s;
  flex-shrink: 0;
}

.settings__switch-thumb {
  position: absolute;
  left: 2px;
  top: 2px;
  width: 27px;
  height: 27px;
  background: linear-gradient(160deg, rgba(255,255,255,1) 0%, rgba(240,240,242,1) 100%);
  border-radius: 50%;
  box-shadow: 0 2px 8px rgba(0,0,0,.22), inset 0 1px 0 rgba(255,255,255,.8);
  transition: transform .22s cubic-bezier(.34,1.56,.64,1);
  pointer-events: none;
}

.settings__switch input:checked + .settings__switch-track {
  background: rgba(48,209,88,0.85);
  border-color: rgba(255,255,255,.4);
}

.settings__switch input:checked + .settings__switch-track + .settings__switch-thumb {
  transform: translateX(20px);
}

.settings__switch input:disabled + .settings__switch-track {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 通知开关表格 */
.settings__notify-table {
  width: 100%;
  border-collapse: collapse;
  border: 1px solid rgba(60,60,67,.12);
  border-radius: 8px;
  overflow: hidden;
  margin-top: 12px;
}

.settings__notify-th {
  background: rgba(255,255,255,0.15);
  padding: 12px 16px;
  font-size: 13px;
  font-weight: 600;
  color: #1c1c1e;
  text-align: left;
  border-bottom: 0.5px solid rgba(60,60,67,.12);
}

.settings__notify-th:last-child {
  text-align: center;
  width: 80px;
}

.settings__notify-tr {
  transition: background 0.2s;
}

.settings__notify-tr:hover {
  background: rgba(255,255,255,0.15);
}

.settings__notify-td {
  padding: 14px 16px;
  font-size: 13px;
  color: #1c1c1e;
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
}

.settings__notify-tr:last-child .settings__notify-td {
  border-bottom: none;
}

.settings__notify-td:last-child {
  text-align: center;
}

.settings__notify-td--desc {
  color: rgba(28,28,30,.55);
  font-size: 12px;
}

/* 备份与恢复 */
.settings__backup-modules {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.settings__backup-module-all {
  width: 100%;
  margin-bottom: 4px;
}

.settings__backup-module-item {
}

.settings__checkbox-label {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 13px;
  color: #1c1c1e;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(60,60,67,.08);
  transition: all 0.2s;
  user-select: none;
}

.settings__checkbox-label:hover {
  background: rgba(255,255,255,0.38);
  border-color: rgba(60,60,67,.12);
}

.settings__checkbox {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 4px;
  border: 1.5px solid rgba(60,60,67,.2);
  flex-shrink: 0;
  transition: all 0.2s;
}

.settings__checkbox--checked {
  background: rgba(10,132,255,0.85);
  border-color: #1c1c1e;
}

.settings__checkbox-tick {
  color: rgba(255,255,255,0.55);
  font-size: 11px;
  line-height: 1;
}

.settings__progress-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.settings__progress-bar {
  flex: 1;
  height: 6px;
  background: rgba(60,60,67,.12);
  border-radius: 3px;
  overflow: hidden;
}

.settings__progress-fill {
  height: 100%;
  background: rgba(10,132,255,0.85);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.settings__progress-text {
  font-size: 12px;
  color: rgba(28,28,30,.55);
  min-width: 36px;
  text-align: right;
}

.settings__file-input {
  display: none;
}

.settings__import-file-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.settings__import-file-name {
  font-size: 13px;
  color: #1c1c1e;
  font-weight: 500;
}

.settings__import-file-hint {
  font-size: 13px;
  color: rgba(28,28,30,.55);
}

.settings__log-pack-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 12px 0;
}

.settings__log-select {
  flex: 1;
  max-width: 200px;
  padding: 8px 12px;
  font-size: 13px;
  border: 1px solid #d2d2d7;
  border-radius: 8px;
  background: rgba(255,255,255,0.55);
  color: #1c1c1e;
  outline: none;
  appearance: none;
  cursor: pointer;
}

.settings__log-select:focus {
  border-color: #0A84FF;
  box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.12);
}

@media (max-width: 768px) {
  .settings__backup-modules {
    flex-direction: column;
  }
}
</style>
