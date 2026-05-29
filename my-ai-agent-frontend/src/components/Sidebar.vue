<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { useConversationStore } from '@/stores/conversation'
import { useLoginPrompt } from '@/composables/useLoginPrompt'
import {
  Plus, MessageSquare, Search, Trash2, Edit3, Check, X,
  User, LogIn, LogOut, Sparkles, Bot, ChevronDown
} from 'lucide-vue-next'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const convStore = useConversationStore()

const searchQuery = ref('')
const editingId = ref(null)
const editingTitle = ref('')
const contextMenuId = ref(null)

const filteredList = computed(() => {
  if (!searchQuery.value) return convStore.list
  const q = searchQuery.value.toLowerCase()
  return convStore.list.filter(c => c.title.toLowerCase().includes(q))
})

const activeId = computed(() => route.params.id || null)
const newChatOpen = ref(false)

const { show: showLoginPrompt } = useLoginPrompt()

async function newChat(type = 'manus') {
  newChatOpen.value = false
  try {
    const conv = await convStore.create(type)
    router.push(`/chat/${conv.id}`)
  } catch (e) {
    if (e.message === 'NEED_LOGIN') showLoginPrompt()
  }
}

function selectChat(id) {
  router.push(`/chat/${id}`)
}

function startEdit(conv) {
  editingId.value = conv.id
  editingTitle.value = conv.title
}

function saveEdit(id) {
  convStore.updateTitle(id, editingTitle.value)
  editingId.value = null
}

function cancelEdit() {
  editingId.value = null
}

function deleteChat(id) {
  convStore.remove(id)
  if (activeId.value === id) {
    router.push('/chat')
  }
}

async function handleLogin() {
  if (userStore.isLoggedIn) {
    await userStore.logout()
  }
  router.push('/login')
}
</script>

<template>
  <aside class="sidebar">
    <!-- Logo -->
    <div class="sidebar-brand" @click="router.push('/chat')">
      <Sparkles :size="22" stroke-width="1.8" />
      <span>My AI</span>
    </div>

    <!-- 新建对话 -->
    <div class="new-chat-wrap">
      <button class="new-chat-btn" @click="newChat()">
        <Plus :size="18" />
        <span>新对话</span>
      </button>
      <button class="new-chat-arrow" @click.stop="newChatOpen = !newChatOpen">
        <ChevronDown :size="14" :class="{ open: newChatOpen }" />
      </button>
      <Transition name="fade">
        <div v-if="newChatOpen" class="new-chat-menu" @click.stop>
          <button @click="newChat('manus')"><Bot :size="16" /> 超级智能体</button>
          <button @click="newChat('love')"><MessageSquare :size="16" /> 恋爱大师</button>
        </div>
      </Transition>
    </div>

    <!-- 搜索 -->
    <div class="sidebar-search">
      <Search :size="15" />
      <input v-model="searchQuery" placeholder="搜索会话..." />
    </div>

    <!-- 会话列表 -->
    <div class="sidebar-list">
      <TransitionGroup name="fade">
        <div
          v-for="conv in filteredList"
          :key="conv.id"
          :class="['sidebar-item', { active: activeId === conv.id }]"
          @click="selectChat(conv.id)"
        >
          <Bot v-if="conv.type !== 'love'" :size="16" class="sidebar-item-icon" />
          <MessageSquare v-else :size="16" class="sidebar-item-icon" />
          <div class="sidebar-item-content">
            <input
              v-if="editingId === conv.id"
              v-model="editingTitle"
              class="edit-input"
              @click.stop
              @keyup.enter="saveEdit(conv.id)"
              @keyup.escape="cancelEdit"
              @blur="saveEdit(conv.id)"
              autofocus
            />
            <span v-else class="sidebar-item-title">{{ conv.title }}</span>
            <span class="sidebar-item-time">{{ conv.messageCount }} 条消息</span>
          </div>
          <div class="sidebar-item-actions" @click.stop>
            <button v-if="editingId === conv.id" @click="saveEdit(conv.id)" class="action-btn"><Check :size="14" /></button>
            <button v-if="editingId === conv.id" @click="cancelEdit" class="action-btn"><X :size="14" /></button>
            <template v-else>
              <button @click="startEdit(conv)" class="action-btn"><Edit3 :size="14" /></button>
              <button @click="deleteChat(conv.id)" class="action-btn danger"><Trash2 :size="14" /></button>
            </template>
          </div>
        </div>
      </TransitionGroup>

      <p v-if="filteredList.length === 0 && convStore.list.length > 0" class="sidebar-empty">
        未找到匹配会话
      </p>
    </div>

    <!-- 底部用户区 -->
    <div class="sidebar-footer">
      <template v-if="userStore.isLoggedIn">
        <div class="user-info">
          <div class="user-avatar">{{ userStore.user?.username?.charAt(0).toUpperCase() }}</div>
          <span class="user-name">{{ userStore.user?.username }}</span>
        </div>
        <button @click="handleLogin" class="user-logout" title="退出登录"><LogOut :size="17" /></button>
      </template>
      <button v-else class="login-prompt" @click="handleLogin">
        <LogIn :size="17" />
        <span>登录</span>
      </button>
    </div>
  </aside>
</template>

<style scoped>
.sidebar {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border-right: 1px solid var(--border);
  flex-shrink: 0;
}
.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 8px;
  font-size: 18px;
  font-weight: 700;
  color: var(--accent);
  cursor: pointer;
  user-select: none;
}
.new-chat-wrap { position: relative; margin: 8px 16px; display: flex; }
.new-chat-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-sm) 0 0 var(--radius-sm);
  transition: all var(--transition-spring);
}
.new-chat-arrow {
  padding: 10px 10px;
  background: var(--accent-soft);
  color: var(--accent);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  border-left: 1px solid var(--accent-light);
  transition: all var(--transition-spring);
}
.new-chat-arrow .open { transform: rotate(180deg); }
.new-chat-btn:hover, .new-chat-wrap:hover .new-chat-btn,
.new-chat-wrap:hover .new-chat-arrow {
  background: var(--accent);
  color: #fff;
}
.new-chat-btn:hover { transform: translateY(-1px); }
.new-chat-arrow:hover { transform: translateY(-1px); }
.new-chat-wrap:hover { box-shadow: 0 4px 12px rgba(99,102,241,.25); border-radius: var(--radius-sm); }
.new-chat-menu {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 4px;
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  box-shadow: var(--shadow-lg);
  z-index: 100;
  overflow: hidden;
}
.new-chat-menu button {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 14px;
  font-size: 13px;
  font-weight: 500;
  transition: background .15s;
}
.new-chat-menu button:hover { background: var(--bg-hover); }
.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 4px 16px 8px;
  padding: 8px 12px;
  background: var(--bg-input);
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  border: 2px solid transparent;
  transition: border var(--transition), box-shadow var(--transition);
}
.sidebar-search:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-light);
  background: #fff;
}
.sidebar-search input { flex: 1; font-size: 13px; }
.sidebar-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 12px;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all .15s;
  margin-bottom: 2px;
}
.sidebar-item:hover { background: var(--bg-hover); }
.sidebar-item.active { background: var(--accent-soft); }
.sidebar-item-icon { color: var(--text-muted); flex-shrink: 0; }
.sidebar-item.active .sidebar-item-icon { color: var(--accent); }
.sidebar-item-content { flex: 1; min-width: 0; }
.sidebar-item-title {
  display: block;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.sidebar-item-time { font-size: 11px; color: var(--text-muted); }
.sidebar-item-actions { display: flex; gap: 2px; opacity: 0; transition: opacity .15s; }
.sidebar-item:hover .sidebar-item-actions { opacity: 1; }
.edit-input {
  font-size: 13px;
  padding: 2px 6px;
  border: 2px solid var(--accent);
  border-radius: 4px;
  width: 100%;
  background: #fff;
}
.action-btn {
  padding: 4px;
  border-radius: 4px;
  color: var(--text-muted);
  transition: all .15s;
}
.action-btn:hover { background: var(--bg-hover); color: var(--text); }
.action-btn.danger:hover { color: var(--danger); }
.sidebar-empty { font-size: 13px; color: var(--text-muted); text-align: center; padding: 20px; }
.sidebar-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-top: 1px solid var(--border);
}
.user-info { display: flex; align-items: center; gap: 10px; }
.user-avatar {
  width: 32px; height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--accent), #a855f7);
  color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 13px;
  font-weight: 700;
}
.user-name { font-size: 13px; font-weight: 500; }
.user-logout { padding: 6px; border-radius: var(--radius-sm); color: var(--text-muted); transition: all .15s; }
.user-logout:hover { background: var(--bg-hover); color: var(--danger); }
.login-prompt {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 14px;
  background: var(--accent);
  color: #fff;
  font-size: 14px;
  font-weight: 600;
  border-radius: var(--radius-sm);
  transition: all var(--transition-spring);
  width: 100%;
  justify-content: center;
}
.login-prompt:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(99,102,241,.3);
}
</style>
