<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useLoginPrompt } from '@/composables/useLoginPrompt'
import Toast from '@/components/Toast.vue'
import { LogIn, X } from 'lucide-vue-next'

const router = useRouter()
const userStore = useUserStore()
const { visible, hide } = useLoginPrompt()
const appReady = ref(false)

onMounted(async () => {
  await userStore.fetchCurrent()
  appReady.value = true
})

function goLogin() {
  hide()
  router.push('/login')
}
</script>

<template>
  <div v-if="appReady" class="app-shell">
    <RouterView />
  </div>
  <div v-else class="app-loading">
    <div class="app-loading-spinner" />
  </div>

  <!-- 登录提示弹窗 -->
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="visible" class="modal-overlay" @click.self="hide">
        <div class="modal-card">
          <button class="modal-close" @click="hide"><X :size="18" /></button>
          <div class="modal-icon">
            <LogIn :size="28" stroke-width="1.5" />
          </div>
          <h3>需要登录</h3>
          <p>登录后才能创建和管理会话记录</p>
          <div class="modal-actions">
            <button class="modal-btn cancel" @click="hide">稍后再说</button>
            <button class="modal-btn primary" @click="goLogin">去登录</button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>

  <Toast ref="toast" />
</template>

<style scoped>
.app-shell {
  height: 100%;
}
.app-loading {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.app-loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--border);
  border-top-color: var(--accent);
  border-radius: 50%;
  animation: spin .6s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.modal-overlay {
  position: fixed; inset: 0; z-index: 9999;
  background: rgba(0,0,0,.5);
  backdrop-filter: blur(4px);
  display: flex; align-items: center; justify-content: center;
}
.modal-card {
  position: relative;
  width: 380px; max-width: 90vw;
  background: #fff;
  border-radius: var(--radius-lg);
  padding: 32px 28px 24px;
  text-align: center;
  box-shadow: 0 20px 60px rgba(0,0,0,.2);
  animation: modal-in .3s cubic-bezier(.34,1.56,.64,1);
}
@keyframes modal-in { from { opacity: 0; transform: scale(.9) translateY(10px); } }
.modal-close {
  position: absolute; top: 12px; right: 12px;
  padding: 6px; border-radius: 50%; color: var(--text-muted);
}
.modal-close:hover { background: var(--bg-hover); }
.modal-icon {
  width: 56px; height: 56px; margin: 0 auto 16px;
  background: var(--accent-soft);
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  color: var(--accent);
}
.modal-card h3 { font-size: 18px; font-weight: 700; margin-bottom: 6px; }
.modal-card p { font-size: 14px; color: var(--text-secondary); margin-bottom: 24px; }
.modal-actions { display: flex; gap: 10px; }
.modal-btn {
  flex: 1; padding: 10px;
  border-radius: var(--radius-sm);
  font-size: 14px; font-weight: 600;
  transition: all .15s;
}
.modal-btn.cancel {
  background: var(--bg-input); color: var(--text-secondary);
}
.modal-btn.cancel:hover { background: var(--bg-hover); }
.modal-btn.primary {
  background: var(--accent); color: #fff;
}
.modal-btn.primary:hover { background: var(--accent-hover); }
</style>
