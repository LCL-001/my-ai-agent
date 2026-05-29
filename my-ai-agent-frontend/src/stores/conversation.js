import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '@/api/client'

export const useConversationStore = defineStore('conversation', () => {
  const list = ref([])
  const loading = ref(false)

  async function fetchList() {
    loading.value = true
    try {
      const res = await api.get('/conversations', { params: { current: 1, pageSize: 50 } })
      list.value = res.data.data?.records || []
    } catch {
      list.value = []
    } finally {
      loading.value = false
    }
  }

  async function create(type = 'manus') {
    try {
      const res = await api.post('/conversations', { type })
      const conv = res.data.data
      list.value.unshift(conv)
      return conv
    } catch (e) {
      const code = e.response?.data?.code
      if (code === 40100) throw new Error('NEED_LOGIN')
      throw e
    }
  }

  async function updateTitle(id, title) {
    await api.put(`/conversations/${id}/title`, { title })
    const item = list.value.find(c => c.id === id)
    if (item) item.title = title
  }

  async function remove(id) {
    await api.delete(`/conversations/${id}`)
    list.value = list.value.filter(c => c.id !== id)
  }

  return { list, loading, fetchList, create, updateTitle, remove }
})
