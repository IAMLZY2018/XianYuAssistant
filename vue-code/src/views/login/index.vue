<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { checkUserExists, login, register } from '@/api/auth'
import { setAuthToken, isLoggedIn } from '@/utils/request'

// 'checking' -> 'login' -> 'register'
const mode = ref<'checking' | 'login' | 'register'>('checking')
const loading = ref(false)

const username = ref('')
const password = ref('')
const confirmPassword = ref('')

const showPassword = ref(false)
const showConfirmPassword = ref(false)

onMounted(async () => {
  // 已登录则跳转首页
  if (isLoggedIn()) {
    window.location.href = '/dashboard'
    return
  }
  // 检查是否有用户，决定显示登录还是注册
  try {
    const res = await checkUserExists()
    console.log('[Login] checkUserExists response:', JSON.stringify(res))
    if (res.code === 200 && res.data) {
      // exists=true -> 有用户 -> 登录; exists=false -> 无用户 -> 注册
      mode.value = res.data.exists ? 'login' : 'register'
      console.log('[Login] mode set to:', mode.value)
    } else {
      mode.value = 'login'
    }
  } catch (e) {
    console.error('[Login] checkUserExists failed:', e)
    mode.value = 'login'
  }
})

async function handleLogin() {
  if (!username.value.trim()) return
  if (!password.value) return
  loading.value = true
  try {
    const res = await login({ username: username.value.trim(), password: password.value })
    if (res.code === 200 && res.data && res.data.token) {
      setAuthToken(res.data.token, res.data.username)
      window.location.href = '/dashboard'
    } else {
      console.error('[Login] login response invalid:', res)
    }
  } catch (e) {
    console.error('[Login] login failed:', e)
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  if (!username.value.trim()) return
  if (!password.value) return
  if (password.value !== confirmPassword.value) return
  if (username.value.trim().length < 3) return
  if (password.value.length < 6) return
  loading.value = true
  try {
    const res = await register({
      username: username.value.trim(),
      password: password.value,
      confirmPassword: confirmPassword.value
    })
    if (res.code === 200 && res.data && res.data.token) {
      setAuthToken(res.data.token, res.data.username)
      window.location.href = '/dashboard'
    } else {
      console.error('[Login] register response invalid:', res)
    }
  } catch (e) {
    console.error('[Login] register failed:', e)
  } finally {
    loading.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !loading.value) {
    if (mode.value === 'login') handleLogin()
    else if (mode.value === 'register') handleRegister()
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- Logo -->
      <div class="login-logo">
        <div class="login-logo-icon">闲</div>
        <div class="login-logo-text">自动化管理</div>
      </div>

      <!-- Loading -->
      <div v-if="mode === 'checking'" class="login-loading">
        <div class="login-spinner"></div>
      </div>

      <!-- Login Form -->
      <div v-else-if="mode === 'login'" class="login-form">
        <h2 class="login-title">登录</h2>
        <p class="login-subtitle">请输入账号密码登录</p>

        <div class="login-field">
          <label class="login-label">账号</label>
          <div class="login-input-wrap">
            <input
              v-model="username"
              type="text"
              class="login-input"
              placeholder="请输入账号"
              autocomplete="username"
              :disabled="loading"
              @keydown="handleKeydown"
            />
          </div>
        </div>

        <div class="login-field">
          <label class="login-label">密码</label>
          <div class="login-input-wrap">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="login-input"
              placeholder="请输入密码"
              autocomplete="current-password"
              :disabled="loading"
              @keydown="handleKeydown"
            />
            <button class="login-eye-btn" @click="showPassword = !showPassword" tabindex="-1">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <button class="login-btn" :disabled="loading" @click="handleLogin">
          <span v-if="loading" class="login-btn-spinner"></span>
          {{ loading ? '请稍候...' : '登录' }}
        </button>
      </div>

      <!-- Register Form -->
      <div v-else-if="mode === 'register'" class="login-form">
        <h2 class="login-title">创建账号</h2>
        <p class="login-subtitle">首次使用，请创建管理员账号</p>

        <div class="login-field">
          <label class="login-label">账号</label>
          <div class="login-input-wrap">
            <input
              v-model="username"
              type="text"
              class="login-input"
              placeholder="请输入账号"
              autocomplete="username"
              :disabled="loading"
              @keydown="handleKeydown"
            />
          </div>
        </div>

        <div class="login-field">
          <label class="login-label">密码</label>
          <div class="login-input-wrap">
            <input
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="login-input"
              placeholder="请输入密码"
              autocomplete="new-password"
              :disabled="loading"
              @keydown="handleKeydown"
            />
            <button class="login-eye-btn" @click="showPassword = !showPassword" tabindex="-1">
              {{ showPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <div class="login-field">
          <label class="login-label">确认密码</label>
          <div class="login-input-wrap">
            <input
              v-model="confirmPassword"
              :type="showConfirmPassword ? 'text' : 'password'"
              class="login-input"
              placeholder="请再次输入密码"
              autocomplete="new-password"
              :disabled="loading"
              @keydown="handleKeydown"
            />
            <button class="login-eye-btn" @click="showConfirmPassword = !showConfirmPassword" tabindex="-1">
              {{ showConfirmPassword ? '隐藏' : '显示' }}
            </button>
          </div>
        </div>

        <button class="login-btn" :disabled="loading" @click="handleRegister">
          <span v-if="loading" class="login-btn-spinner"></span>
          {{ loading ? '请稍候...' : '创建账号' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #e8e8e8;
  padding: 16px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: rgba(255,255,255,0.72);
  backdrop-filter: blur(40px) saturate(2);
  -webkit-backdrop-filter: blur(40px) saturate(2);
  border: 1px solid rgba(255,255,255,0.75);
  border-radius: 20px;
  box-shadow: 0 16px 48px rgba(0,0,0,0.16), 0 2px 8px rgba(0,0,0,0.08);
  padding: 40px 32px;
  position: relative;
  overflow: hidden;
}

.login-card::before {
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

/* Logo */
.login-logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  margin-bottom: 32px;
}

.login-logo-icon {
  width: 40px;
  height: 40px;
  background: rgba(10,132,255,0.75);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 22px;
  font-weight: bold;
}

.login-logo-text {
  font-size: 20px;
  font-weight: 600;
  color: #1c1c1e;
}

/* Loading */
.login-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
}

.login-spinner {
  width: 24px;
  height: 24px;
  border: 2px solid #d4d4d4;
  border-top-color: #1c1c1e;
  border-radius: 50%;
  animation: login-spin 0.6s linear infinite;
}

@keyframes login-spin {
  to { transform: rotate(360deg); }
}

/* Form */
.login-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.login-title {
  font-size: 22px;
  font-weight: 600;
  color: #1c1c1e;
  margin: 0;
  text-align: center;
}

.login-subtitle {
  font-size: 14px;
  color: rgba(28,28,30,.55);
  margin: -12px 0 0;
  text-align: center;
}

.login-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.login-label {
  font-size: 13px;
  font-weight: 500;
  color: #1c1c1e;
}

.login-input-wrap {
  position: relative;
  display: flex;
  align-items: center;
}

.login-input {
  width: 100%;
  height: 44px;
  padding: 0 14px;
  font-size: 15px;
  color: #1c1c1e;
  background: rgba(255,255,255,0.15);
  border: 1px solid transparent;
  border-radius: 10px;
  outline: none;
  transition: all 0.2s;
  box-sizing: border-box;
}

.login-input:focus {
  border-color: #1c1c1e;
  background: rgba(255,255,255,0.55);
}

.login-input::placeholder {
  color: rgba(28,28,30,.55);
}

.login-input:disabled {
  opacity: 0.5;
}

.login-eye-btn {
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

.login-eye-btn:hover {
  color: #1c1c1e;
}

/* Submit Button */
.login-btn {
  width: 100%;
  height: 48px;
  background: rgba(10,132,255,0.85);
  color: #fff;
  border: none;
  border-radius: 10px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 4px;
}

.login-btn:hover {
  background: rgba(10,132,255,0.75);
}

.login-btn:active {
  transform: scale(0.98);
}

.login-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.login-btn-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: login-spin 0.6s linear infinite;
}

/* Responsive: Tablet */
@media (max-width: 768px) {
  .login-card {
    padding: 32px 24px;
  }

  .login-title {
    font-size: 20px;
  }
}

/* Responsive: Small phone */
@media (max-width: 480px) {
  .login-card {
    padding: 24px 20px;
    border-radius: 12px;
  }

  .login-logo-icon {
    width: 36px;
    height: 36px;
    font-size: 20px;
  }

  .login-logo-text {
    font-size: 18px;
  }

  .login-title {
    font-size: 18px;
  }

  .login-input {
    height: 42px;
    font-size: 14px;
  }

  .login-btn {
    height: 44px;
    font-size: 15px;
  }
}
</style>
