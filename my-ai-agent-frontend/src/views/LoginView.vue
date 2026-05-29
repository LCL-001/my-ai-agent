<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { Sparkles, User, Lock, ArrowRight, RefreshCw } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()

const isLogin = ref(true)
const username = ref('')
const password = ref('')
const checkPassword = ref('')
const errorMsg = ref('')
const busy = ref(false)
const focusUser = ref(false)
const focusPwd = ref(false)
const focusCheck = ref(false)

const title = computed(() => isLogin.value ? '欢迎回来' : '创建账号')
const subtitle = computed(() => isLogin.value ? '登录以同步你的会话记录' : '注册后即可使用全部功能')

async function submit() {
  errorMsg.value = ''
  busy.value = true
  let result
  if (isLogin.value) {
    result = await userStore.login(username.value, password.value)
  } else {
    if (password.value !== checkPassword.value) {
      errorMsg.value = '两次密码不一致'
      busy.value = false
      return
    }
    result = await userStore.register(username.value, password.value, checkPassword.value)
    if (result.success) {
      result = await userStore.login(username.value, password.value)
    }
  }
  busy.value = false
  if (result.success) {
    router.replace('/chat')
  } else {
    errorMsg.value = result.message
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 装饰背景 -->
    <div class="login-bg">
      <div class="orb orb-1" />
      <div class="orb orb-2" />
      <div class="orb orb-3" />
    </div>

    <!-- 卡片 -->
    <div class="login-card">
      <div class="login-header">
        <div class="login-brand">
          <Sparkles :size="28" stroke-width="1.5" />
        </div>
        <h1>{{ title }}</h1>
        <p>{{ subtitle }}</p>
      </div>

      <form class="login-form" @submit.prevent="submit">
        <div class="input-group">
          <User :size="18" class="input-icon" />
          <input v-model="username"
            :placeholder="focusUser ? '不少于2位' : '用户名'"
            @focus="focusUser = true" @blur="focusUser = false"
            autocomplete="username" required />
        </div>

        <div class="input-group">
          <Lock :size="18" class="input-icon" />
          <input v-model="password" type="password"
            :placeholder="focusPwd ? '不少于6位' : '密码'"
            @focus="focusPwd = true" @blur="focusPwd = false"
            autocomplete="current-password" required />
        </div>

        <Transition name="fade">
          <div v-if="!isLogin" class="input-group">
            <Lock :size="18" class="input-icon" />
            <input v-model="checkPassword" type="password"
              :placeholder="focusCheck ? '与密码保持一致' : '确认密码'"
              @focus="focusCheck = true" @blur="focusCheck = false"
              required />
          </div>
        </Transition>

        <Transition name="fade">
          <p v-if="errorMsg" class="login-error">{{ errorMsg }}</p>
        </Transition>

        <button type="submit" class="login-btn" :disabled="busy">
          <RefreshCw v-if="busy" :size="18" class="spin" />
          <span v-else>{{ isLogin ? '登录' : '注册' }}</span>
          <ArrowRight v-if="!busy" :size="18" />
        </button>
      </form>

      <p class="login-switch">
        {{ isLogin ? '还没有账号？' : '已有账号？' }}
        <button @click="isLogin = !isLogin; errorMsg = ''">
          {{ isLogin ? '立即注册' : '去登录' }}
        </button>
      </p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f0f4ff 0%, #fdf2f8 50%, #ecfeff 100%);
  position: relative;
  overflow: hidden;
}
.login-bg { position: absolute; inset: 0; overflow: hidden; }
.orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: .4;
  animation: float 8s ease-in-out infinite;
}
.orb-1 { width: 400px; height: 400px; background: var(--accent-light); top: -100px; left: -100px; }
.orb-2 { width: 300px; height: 300px; background: var(--coral-light); bottom: -80px; right: -60px; animation-delay: -4s; }
.orb-3 { width: 250px; height: 250px; background: var(--cyan-light); top: 50%; left: 50%; animation-delay: -2s; }
@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.05); }
  66% { transform: translate(-20px, 20px) scale(.95); }
}
.login-card {
  position: relative;
  z-index: 1;
  width: 400px;
  padding: 40px;
  background: rgba(255,255,255,.85);
  backdrop-filter: blur(20px);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  border: 1px solid rgba(255,255,255,.6);
}
.login-header { text-align: center; margin-bottom: 32px; }
.login-brand {
  width: 56px; height: 56px; margin: 0 auto 16px;
  background: linear-gradient(135deg, var(--accent), #a855f7);
  border-radius: var(--radius);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.login-header h1 { font-size: 24px; font-weight: 700; margin-bottom: 6px; }
.login-header p { font-size: 14px; color: var(--text-secondary); }
.login-form { display: flex; flex-direction: column; gap: 14px; }
.input-group {
  display: flex; align-items: center; gap: 10px;
  padding: 12px 16px;
  background: var(--bg-input);
  border-radius: var(--radius-sm);
  border: 2px solid transparent;
  transition: border var(--transition), box-shadow var(--transition);
}
.input-group:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-light);
  background: #fff;
}
.input-icon { color: var(--text-muted); flex-shrink: 0; }
.input-group input { flex: 1; font-size: 15px; color: var(--text); background: transparent; }
.login-error { font-size: 13px; color: var(--danger); text-align: center; }
.login-btn {
  display: flex; align-items: center; justify-content: center; gap: 8px;
  width: 100%; padding: 14px;
  background: linear-gradient(135deg, var(--accent), #7c3aed);
  color: #fff; font-size: 16px; font-weight: 600;
  border-radius: var(--radius-sm);
  transition: transform var(--transition-spring), box-shadow var(--transition);
}
.login-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(99,102,241,.35);
}
.login-btn:disabled { opacity: .7; }
.login-switch { text-align: center; margin-top: 24px; font-size: 14px; color: var(--text-secondary); }
.login-switch button { color: var(--accent); font-weight: 600; }
.login-switch button:hover { text-decoration: underline; }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
</style>
