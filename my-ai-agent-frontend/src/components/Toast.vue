<script setup>
import { ref } from 'vue'

const toasts = ref([])
let id = 0

function addToast(message, type = 'info', duration = 3000) {
  const tid = ++id
  toasts.value.push({ id: tid, message, type })
  if (duration > 0) setTimeout(() => removeToast(tid), duration)
}

function removeToast(tid) {
  toasts.value = toasts.value.filter(t => t.id !== tid)
}

defineExpose({ add: addToast })
</script>

<template>
  <Teleport to="body">
    <div class="toast-container">
      <TransitionGroup name="slide-up">
        <div v-for="t in toasts" :key="t.id" :class="['toast', `toast-${t.type}`]" @click="removeToast(t.id)">
          <span class="toast-icon">{{ t.type === 'success' ? '✓' : t.type === 'error' ? '✕' : 'ℹ' }}</span>
          {{ t.message }}
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 8px;
  pointer-events: none;
}
.toast {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: var(--radius);
  background: var(--bg-card);
  box-shadow: var(--shadow-lg);
  font-size: 14px;
  font-weight: 500;
  pointer-events: auto;
  cursor: pointer;
  border: 1px solid var(--border);
  animation: toast-in .3s cubic-bezier(.34,1.56,.64,1);
}
.toast-icon { width: 20px; height: 20px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 11px; font-weight: 700; }
.toast-success .toast-icon { background: var(--success); color: #fff; }
.toast-error .toast-icon { background: var(--danger); color: #fff; }
.toast-info .toast-icon { background: var(--accent); color: #fff; }
@keyframes toast-in { from { opacity: 0; transform: translateX(40px) scale(.9); } to { opacity: 1; transform: translateX(0) scale(1); } }
</style>
