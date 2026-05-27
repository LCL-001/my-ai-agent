<script setup>
import { computed, nextTick, onBeforeUnmount, ref } from 'vue'
import { ArrowLeft, Bot, Copy, HeartPulse, RefreshCw, Send, Square, UserRound, Wifi, WifiOff } from 'lucide-vue-next'

import { createSseUrl } from '@/api/client'
import { useSeo } from '@/composables/useSeo'

const props = defineProps({
  appKey: {
    type: String,
    required: true,
  },
  title: {
    type: String,
    required: true,
  },
  subtitle: {
    type: String,
    required: true,
  },
  endpoint: {
    type: String,
    required: true,
  },
  accent: {
    type: String,
    default: 'coral',
  },
  greeting: {
    type: String,
    required: true,
  },
  aiName: {
    type: String,
    default: 'AI',
  },
  seoTitle: {
    type: String,
    required: true,
  },
  seoDescription: {
    type: String,
    required: true,
  },
  seoKeywords: {
    type: String,
    required: true,
  },
  withChatId: {
    type: Boolean,
    default: false,
  },
})

useSeo({
  title: props.seoTitle,
  description: props.seoDescription,
  keywords: props.seoKeywords,
})

const messages = ref([
  {
    id: `welcome-${props.appKey}`,
    role: 'assistant',
    content: props.greeting,
  },
])
const input = ref('')
const chatId = ref(createChatId())
const isStreaming = ref(false)
const connectionState = ref('idle')
const errorText = ref('')
const scrollPanel = ref(null)
const TYPE_SPEED_MS = 18
let eventSource = null
let typingTimer = null
let pendingText = ''
let activeAssistantMessageIndex = -1
let streamFinished = false

const canSend = computed(() => input.value.trim().length > 0 && !isStreaming.value)
const stateLabel = computed(() => {
  if (connectionState.value === 'open') return '响应中'
  if (connectionState.value === 'error') return '连接中断'
  return '待命'
})
const aiAvatarIcon = computed(() => (props.accent === 'coral' ? HeartPulse : Bot))

function createChatId() {
  if (typeof crypto !== 'undefined' && crypto.randomUUID) {
    return crypto.randomUUID()
  }
  return `${Date.now()}-${Math.random().toString(16).slice(2)}`
}

async function scrollToBottom() {
  await nextTick()
  if (scrollPanel.value) {
    scrollPanel.value.scrollTop = scrollPanel.value.scrollHeight
  }
}

function normalizeChunk(chunk) {
  if (!chunk) return ''
  if (chunk.trim() === '[DONE]' || chunk.trim() === 'DONE') return ''
  return chunk
}

function closeEventSource() {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

function clearTypewriter() {
  if (typingTimer) {
    window.clearTimeout(typingTimer)
    typingTimer = null
  }
  pendingText = ''
  activeAssistantMessageIndex = -1
  streamFinished = false
}

function finishWhenQueueEmpty() {
  if (!streamFinished || pendingText || typingTimer) return
  activeAssistantMessageIndex = -1
  isStreaming.value = false
  if (connectionState.value !== 'error') {
    connectionState.value = 'idle'
  }
}

function getActiveAssistantMessage() {
  if (activeAssistantMessageIndex < 0) return null
  return messages.value[activeAssistantMessageIndex] || null
}

function runTypewriter() {
  if (typingTimer || !getActiveAssistantMessage()) return

  if (!pendingText) {
    finishWhenQueueEmpty()
    return
  }

  typingTimer = window.setTimeout(async () => {
    typingTimer = null
    const message = getActiveAssistantMessage()
    if (!message) return

    message.content += pendingText.slice(0, 1)
    pendingText = pendingText.slice(1)
    await scrollToBottom()
    runTypewriter()
  }, TYPE_SPEED_MS)
}

function queueAssistantText(text) {
  pendingText += text
  runTypewriter()
}

function finishStream() {
  closeEventSource()
  streamFinished = true
  finishWhenQueueEmpty()
}

function stopStreaming() {
  closeEventSource()
  clearTypewriter()
  isStreaming.value = false
  connectionState.value = 'idle'
}

function resetSession() {
  stopStreaming()
  chatId.value = createChatId()
  connectionState.value = 'idle'
  errorText.value = ''
  messages.value = [
    {
      id: `welcome-${props.appKey}-${chatId.value}`,
      role: 'assistant',
      content: props.greeting,
    },
  ]
  scrollToBottom()
}

async function sendMessage() {
  const text = input.value.trim()
  if (!text || isStreaming.value) return

  stopStreaming()
  errorText.value = ''
  connectionState.value = 'connecting'
  streamFinished = false

  const userMessage = {
    id: `${Date.now()}-user`,
    role: 'user',
    content: text,
  }
  const assistantMessage = {
    id: `${Date.now()}-assistant`,
    role: 'assistant',
    content: '',
  }

  messages.value.push(userMessage, assistantMessage)
  activeAssistantMessageIndex = messages.value.length - 1
  input.value = ''
  isStreaming.value = true
  await scrollToBottom()

  const params = props.withChatId ? { message: text, chatId: chatId.value } : { message: text }
  eventSource = new EventSource(createSseUrl(props.endpoint, params))

  eventSource.onopen = () => {
    connectionState.value = 'open'
  }

  eventSource.onmessage = async (event) => {
    const chunk = normalizeChunk(event.data)
    if (!chunk) {
      if (event.data?.trim() === '[DONE]' || event.data?.trim() === 'DONE') {
        finishStream()
      }
      return
    }
    queueAssistantText(chunk)
  }

  eventSource.onerror = () => {
    const message = getActiveAssistantMessage()
    if (!message?.content && !pendingText) {
      clearTypewriter()
      messages.value[messages.value.length - 1].content = '本次连接没有收到有效内容，请确认后端服务已启动并允许 SSE 访问。'
      errorText.value = 'SSE 连接异常，请检查 http://localhost:8123/api。'
      connectionState.value = 'error'
      closeEventSource()
      isStreaming.value = false
    } else {
      finishStream()
    }
    scrollToBottom()
  }
}

function handleEnter(event) {
  if (event.shiftKey) return
  event.preventDefault()
  sendMessage()
}

async function copyChatId() {
  if (!props.withChatId || !navigator.clipboard) return
  await navigator.clipboard.writeText(chatId.value)
}

onBeforeUnmount(() => {
  stopStreaming()
})
</script>

<template>
  <main class="chat-page" :class="accent">
    <header class="chat-topbar">
      <RouterLink class="icon-button back-button" to="/" aria-label="返回主页" title="返回主页">
        <ArrowLeft :size="22" />
      </RouterLink>

      <div class="chat-heading">
        <p>{{ title }}</p>
        <h1>{{ subtitle }}</h1>
      </div>

      <div class="session-panel">
        <span class="stream-state" :class="connectionState">
          <Wifi v-if="connectionState === 'open'" :size="16" />
          <WifiOff v-else :size="16" />
          {{ stateLabel }}
        </span>
        <button class="icon-button" type="button" aria-label="开启新会话" title="开启新会话" @click="resetSession">
          <RefreshCw :size="19" />
        </button>
      </div>
    </header>

    <section class="chat-layout">
      <aside class="conversation-meta" aria-label="会话信息">
        <span class="meta-label">Session</span>
        <strong>{{ withChatId ? '聊天室 ID' : '智能体通道' }}</strong>
        <code>{{ withChatId ? chatId : 'manus-stream' }}</code>
        <button v-if="withChatId" class="ghost-button" type="button" @click="copyChatId">
          <Copy :size="16" />
          复制 ID
        </button>
      </aside>

      <section class="chat-window" aria-label="聊天窗口">
        <div ref="scrollPanel" class="message-list">
          <article
            v-for="message in messages"
            :key="message.id"
            class="message-row"
            :class="message.role"
          >
            <div class="message-avatar" :class="message.role" aria-hidden="true">
              <component :is="message.role === 'user' ? UserRound : aiAvatarIcon" :size="21" />
            </div>
            <div class="message-stack">
              <span class="speaker">{{ message.role === 'user' ? 'Navigator' : aiName }}</span>
              <div class="bubble">
                <p>{{ message.content || '正在生成...' }}</p>
              </div>
            </div>
          </article>
        </div>

        <p v-if="errorText" class="error-line">{{ errorText }}</p>

        <form class="composer" @submit.prevent="sendMessage">
          <textarea
            v-model="input"
            rows="1"
            placeholder="输入消息，Enter 发送，Shift + Enter 换行"
            :disabled="isStreaming"
            @keydown.enter="handleEnter"
          />
          <button v-if="isStreaming" class="send-button stop" type="button" aria-label="停止生成" @click="stopStreaming">
            <Square :size="18" />
          </button>
          <button v-else class="send-button" type="submit" :disabled="!canSend" aria-label="发送消息">
            <Send :size="18" />
          </button>
        </form>
      </section>
    </section>
  </main>
</template>
