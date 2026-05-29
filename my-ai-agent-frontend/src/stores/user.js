import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import api from '@/api/client'

export const useUserStore = defineStore('user', () => {
  const user = ref(null)
  const loading = ref(false)

  const isLoggedIn = computed(() => user.value !== null)

  async function fetchCurrent() {
    try {
      const res = await api.get('/user/current')
      user.value = res.data.data
    } catch {
      user.value = null
    }
  }

  async function login(username, password) {
    loading.value = true
    try {
      const res = await api.post('/user/login', { username, password })
      user.value = res.data.data
      return { success: true }
    } catch (e) {
      return { success: false, message: e.response?.data?.message || '登录失败' }
    } finally {
      loading.value = false
    }
  }

  async function register(username, password, checkPassword) {
    loading.value = true
    try {
      await api.post('/user/register', { username, password, checkPassword })
      return { success: true }
    } catch (e) {
      return { success: false, message: e.response?.data?.message || '注册失败' }
    } finally {
      loading.value = false
    }
  }

  async function logout() {
    try { await api.post('/user/logout') } catch {}
    user.value = null
  }

  return { user, loading, isLoggedIn, fetchCurrent, login, register, logout }
})
