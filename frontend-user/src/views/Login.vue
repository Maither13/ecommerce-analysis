<template>
  <div class="login-page">
    <!-- 背景粒子效果 -->
    <div class="bg-grid"></div>
    <div class="bg-glow glow-1"></div>
    <div class="bg-glow glow-2"></div>

    <div class="login-box">
      <!-- Logo -->
      <div class="brand">
        <div class="brand-icon">📊</div>
        <h1 class="brand-title gradient-text">电商用户行为分析</h1>
        <p class="brand-sub">基于 MapReduce 的大数据分析平台</p>
      </div>

      <!-- 表单 -->
      <form class="form" @submit.prevent="handleLogin">
        <div class="field" :class="{ error: errors.username }">
          <span class="field-icon">👤</span>
          <input
            v-model="form.username"
            type="text"
            placeholder="请输入用户名"
            autocomplete="username"
            @input="errors.username = ''"
          />
        </div>
        <p v-if="errors.username" class="err-msg">{{ errors.username }}</p>

        <div class="field" :class="{ error: errors.password }">
          <span class="field-icon">🔒</span>
          <input
            v-model="form.password"
            :type="showPwd ? 'text' : 'password'"
            placeholder="请输入密码"
            autocomplete="current-password"
            @input="errors.password = ''"
          />
          <span class="toggle-pwd" @click="showPwd = !showPwd">
            {{ showPwd ? '🙈' : '👁️' }}
          </span>
        </div>
        <p v-if="errors.password" class="err-msg">{{ errors.password }}</p>

        <p v-if="errorMsg" class="err-global">{{ errorMsg }}</p>

        <button type="submit" class="btn-primary" :disabled="loading">
          <span v-if="loading" class="spinner" style="width:18px;height:18px;margin:0"></span>
          <span>{{ loading ? '登录中...' : '立即登录' }}</span>
        </button>
      </form>

      <p class="hint">测试账号：user001 / user123</p>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/index.js'

const router = useRouter()
const store = useUserStore()

const form = reactive({ username: '', password: '' })
const errors = reactive({ username: '', password: '' })
const errorMsg = ref('')
const loading = ref(false)
const showPwd = ref(false)

async function handleLogin() {
  errors.username = form.username ? '' : '请输入用户名'
  errors.password = form.password ? '' : '请输入密码'
  if (errors.username || errors.password) return

  loading.value = true
  errorMsg.value = ''
  try {
    const data = await login({ username: form.username, password: form.password })
    store.setLogin(data)
    router.push('/home')
  } catch (e) {
    errorMsg.value = e.message || '登录失败，请检查账号密码'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: var(--bg-base);
}

/* 背景装饰 */
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(var(--border) 1px, transparent 1px),
    linear-gradient(90deg, var(--border) 1px, transparent 1px);
  background-size: 40px 40px;
  opacity: .3;
}
.bg-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  pointer-events: none;
}
.glow-1 { width: 400px; height: 400px; background: #00d4ff18; top: -100px; left: -100px; }
.glow-2 { width: 300px; height: 300px; background: #7c3aed18; bottom: -80px; right: -80px; }

/* 登录框 */
.login-box {
  position: relative;
  width: min(380px, 92vw);
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: 24px;
  padding: 36px 28px;
  box-shadow: 0 0 40px #00d4ff18, 0 20px 60px #00000066;
}

/* 品牌 */
.brand { text-align: center; margin-bottom: 32px; }
.brand-icon { font-size: 48px; margin-bottom: 12px; }
.brand-title { font-size: 22px; font-weight: 800; margin-bottom: 6px; }
.brand-sub { font-size: 12px; color: var(--text-sub); }

/* 表单 */
.form { display: flex; flex-direction: column; gap: 4px; }
.field {
  display: flex;
  align-items: center;
  background: var(--bg-card2);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 0 14px;
  height: 52px;
  transition: border-color .2s, box-shadow .2s;
  margin-bottom: 6px;
}
.field:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 2px #00d4ff22;
}
.field.error { border-color: var(--danger); }
.field-icon { font-size: 16px; margin-right: 10px; flex-shrink: 0; }
.field input {
  flex: 1;
  background: none;
  border: none;
  outline: none;
  color: var(--text-main);
  font-size: 15px;
}
.field input::placeholder { color: var(--text-muted); }
.toggle-pwd { font-size: 16px; cursor: pointer; margin-left: 8px; }
.err-msg { font-size: 12px; color: var(--danger); margin-top: -2px; margin-bottom: 4px; padding-left: 4px; }
.err-global {
  padding: 10px 14px;
  background: #ef444411;
  border: 1px solid #ef444433;
  border-radius: 8px;
  font-size: 13px;
  color: var(--danger);
  text-align: center;
}

.btn-primary { margin-top: 12px; }

.hint {
  margin-top: 20px;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}
</style>
