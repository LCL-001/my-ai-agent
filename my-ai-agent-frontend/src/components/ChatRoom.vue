<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { Bot, ChevronDown, Copy, Download, Loader2, RefreshCw, Send, Square, UserRound, Wrench } from 'lucide-vue-next'
import { marked } from 'marked'
import DOMPurify from 'dompurify'
import { createSseUrl } from '@/api/client'
import api from '@/api/client'

// 工具/思考步骤明细用 markdown 渲染（配最大高度滚动），v-html 前必须过 DOMPurify
function renderMd(text) {
  try {
    return DOMPurify.sanitize(marked.parse(text || '', { breaks: true }))
  } catch {
    return text
  }
}

const props = defineProps({
  endpoint: { type: String, required: true },
  greeting: { type: String, required: true },
  aiName: { type: String, default: 'AI' },
  chatId: { type: String, default: null },
})

const localChatId = crypto.randomUUID ? crypto.randomUUID() : Date.now().toString(36)
const effectiveChatId = computed(() => props.chatId || localChatId)

const messages = ref([])
const input = ref('')
const isStreaming = ref(false)
const connectionState = ref('idle')
const pendingText = ref('')
const typingTimer = ref(null)
const drainTimer = ref(null)
const activeMsgId = ref(null)
const scrollPanel = ref(null)
const textareaRef = ref(null)
const eventSource = ref(null)
const TYPE_SPEED_MS = 16

/* ---- 历史消息加载 ---- */
async function loadHistory() {
  if (!props.chatId) {
    messages.value = [{ id: 'welcome', role: 'assistant', content: props.greeting }]
    return
  }
  try {
    const res = await api.get(`/conversations/${props.chatId}/messages`)
    const list = res.data.data || []
    if (list.length > 0) {
      messages.value = list.map(m => ({
        id: m.id,
        role: m.role,
        content: m.content,
        // 历史步骤默认收起（无用时统计）
        steps: (m.steps || []).map(s => ({ name: s.name, content: s.content })),
        stepsCollapsed: true,
        stepsRunning: false,
      }))
    } else {
      messages.value = [{ id: 'welcome', role: 'assistant', content: props.greeting }]
    }
  } catch {
    messages.value = [{ id: 'welcome', role: 'assistant', content: props.greeting }]
  }
  nextTick(() => scrollToBottom())
}

onMounted(() => loadHistory())
watch(() => props.chatId, () => loadHistory())

/* ---- 打字机 ---- */
function runTypewriter(msg) {
  if (pendingText.value.length === 0) {
    typingTimer.value = null
    return
  }
  const char = pendingText.value.charAt(0)
  pendingText.value = pendingText.value.slice(1)
  const m = messages.value.find(x => x.id === msg.id)
  if (m) m.content += char
  typingTimer.value = setTimeout(() => runTypewriter(msg), TYPE_SPEED_MS)
}

function clearTypewriter() {
  if (typingTimer.value) { clearTimeout(typingTimer.value); typingTimer.value = null }
  if (drainTimer.value) { clearTimeout(drainTimer.value); drainTimer.value = null }
  pendingText.value = ''
}

function finishWhenQueueEmpty() {
  if (pendingText.value.length > 0) {
    drainTimer.value = setTimeout(finishWhenQueueEmpty, 50)
    return
  }
  isStreaming.value = false
  connectionState.value = 'idle'
  activeMsgId.value = null
}

/* ---- SSE ---- */

/** 把工具/思考事件追加到当前回答的折叠区（首次创建时展开并标记执行中） */
function pushStep(evt) {
  const m = messages.value.find(x => x.id === activeMsgId.value)
  if (!m) return
  if (!m.steps) {
    m.steps = []
    m.stepsCollapsed = false   // 执行中展开，让用户看到"正在干活"
    m.stepsRunning = true
    m.turnStartTs = Date.now()
  }
  m.steps.push({ name: evt.name || (evt.kind === 'think' ? '思考' : '步骤'), content: evt.content || '' })
  scrollToBottom()
}

/** 流结束（完成/停止）时收起折叠区并计算用时 */
function collapseSteps() {
  const m = messages.value.find(x => x.id === activeMsgId.value)
  if (m && m.steps && m.steps.length) {
    m.stepsRunning = false
    m.stepsCollapsed = true
    m.stepsDuration = m.turnStartTs ? Math.max(1, Math.round((Date.now() - m.turnStartTs) / 1000)) : null
  }
}

function sendMessage() {
  const text = input.value.trim()
  if (!text || isStreaming.value) return
  input.value = ''
  composerHeight.value = 80
  autoResize()

  const userMsg = { id: `u-${Date.now()}`, role: 'user', content: text }
  const aiMsg = { id: `a-${Date.now()}`, role: 'assistant', content: '' }
  messages.value.push(userMsg, aiMsg)
  activeMsgId.value = aiMsg.id
  isStreaming.value = true
  connectionState.value = 'connecting'

  nextTick(() => scrollToBottom())

  const params = { message: text }
  params.chatId = effectiveChatId.value

  const url = createSseUrl(props.endpoint, params)
  let retryCount = 0

  function connect() {
    const es = new EventSource(url)
    eventSource.value = es

    es.onopen = () => {
      retryCount = 0
      connectionState.value = 'open'
    }
    es.onmessage = (e) => {
      if (e.data === '[DONE]' || e.data === 'DONE') { collapseSteps(); finishStream(); return }
      let evt = null
      try { evt = JSON.parse(e.data) } catch { evt = null }
      if (evt && evt.event === 'step') { pushStep(evt); return }
      if (evt && evt.event === 'error') {
        const m = messages.value.find(x => x.id === activeMsgId.value)
        if (m) m.content += (m.content ? '\n' : '') + (evt.content || '执行出错')
        collapseSteps()
        finishStream()
        return
      }
      if (evt && evt.event === 'answer') {
        pendingText.value += evt.content || ''
        if (!typingTimer.value) runTypewriter(aiMsg)
        scrollToBottom()
        return
      }
      // 旧协议兜底：未改造的端点（如 LoveApp）发纯文本，按回答处理
      pendingText.value += typeof e.data === 'string' ? e.data : ''
      if (!typingTimer.value) runTypewriter(aiMsg)
      scrollToBottom()
    }
    es.onerror = () => {
      es.close()
      if (retryCount < 2 && aiMsg.content.length === 0) {
        retryCount++
        setTimeout(connect, 2000)
        return
      }
      if (aiMsg.content.length === 0) {
        aiMsg.content = '连接中断，请重试'
      }
      finishStream()
    }
  }
  connect()
}

function finishStream() {
  if (eventSource.value) { eventSource.value.close(); eventSource.value = null }
  collapseSteps()
  finishWhenQueueEmpty()
}

function stopStreaming() {
  if (eventSource.value) { eventSource.value.close(); eventSource.value = null }
  clearTypewriter()
  collapseSteps()
  isStreaming.value = false
  connectionState.value = 'idle'
  activeMsgId.value = null
}

/* ---- 工具 ---- */
function scrollToBottom() {
  nextTick(() => {
    if (scrollPanel.value) scrollPanel.value.scrollTop = scrollPanel.value.scrollHeight
  })
}

function handleEnter(e) {
  if (e.key !== 'Enter') return
  if (e.shiftKey) return
  e.preventDefault()
  sendMessage()
}

const composerHeight = ref(80)
const isDragging = ref(false)
const dragStartY = ref(0)
const dragStartHeight = ref(0)

function autoResize(forcedHeight) {
  nextTick(() => {
    const el = textareaRef.value
    if (!el) return
    el.style.height = 'auto'
    const h = forcedHeight || Math.min(el.scrollHeight, composerHeight.value)
    el.style.height = h + 'px'
  })
}

function onDragStart(e) {
  isDragging.value = true
  dragStartY.value = e.clientY
  dragStartHeight.value = composerHeight.value
  document.addEventListener('mousemove', onDragMove)
  document.addEventListener('mouseup', onDragEnd)
  e.preventDefault()
}

function onDragMove(e) {
  if (!isDragging.value) return
  const delta = dragStartY.value - e.clientY
  composerHeight.value = Math.max(40, Math.min(dragStartHeight.value + delta, 400))
  autoResize(composerHeight.value)
}

function onDragEnd() {
  isDragging.value = false
  document.removeEventListener('mousemove', onDragMove)
  document.removeEventListener('mouseup', onDragEnd)
}

function handleInput() {
  autoResize()
  scrollToBottom()
}

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/api'

function extractPdfName(content) {
  const m = content.match(/([\w一-鿿\-\ _]+\.pdf)/i)
  return m ? m[1].trim() : null
}

function downloadPdf(name) {
  window.open(`${API_BASE}/files/download?name=${encodeURIComponent(name)}`, '_blank')
}

function copyMessage(content) {
  navigator.clipboard?.writeText(content)
}

/* ---- 生命周期 ---- */
onBeforeUnmount(() => {
  if (eventSource.value) eventSource.value.close()
  clearTypewriter()
})
</script>

<template>
  <div class="chatroom">
    <!-- 头部 -->
    <header class="chat-header">
      <div class="chat-header-info">
        <div class="chat-avatar">
          <Bot :size="20" stroke-width="1.8" />
        </div>
        <div>
          <h2>{{ aiName }}</h2>
          <span :class="['status-dot', connectionState]" />
          <span class="status-text">
            {{ connectionState === 'open' ? '在线' : connectionState === 'connecting' ? '连接中' : '就绪' }}
          </span>
        </div>
      </div>
    </header>

    <!-- 消息列表 -->
    <div ref="scrollPanel" class="chat-messages">
      <TransitionGroup name="fade">
        <div
          v-for="msg in messages"
          v-show="msg.content || (msg.steps && msg.steps.length)"
          :key="msg.id"
          :class="['message', msg.role]"
        >
          <div class="message-avatar">
            <UserRound v-if="msg.role === 'user'" :size="16" />
            <Bot v-else :size="16" />
          </div>
          <div class="message-main">
            <!-- 工具/思考折叠条：执行中展开，结束后自动收起 -->
            <div v-if="msg.steps && msg.steps.length" class="steps-collapse">
              <div class="steps-header" @click="msg.stepsCollapsed = !msg.stepsCollapsed">
                <Loader2 v-if="msg.stepsRunning" :size="13" class="spin" />
                <Wrench v-else :size="13" />
                <span v-if="msg.stepsRunning">
                  正在执行：{{ msg.steps[msg.steps.length - 1].name }}…
                </span>
                <span v-else>
                  已执行 {{ msg.steps.length }} 步<template v-if="msg.stepsDuration">（用时 {{ msg.stepsDuration }} 秒）</template>
                </span>
                <ChevronDown :size="14" :class="['chevron', { open: !msg.stepsCollapsed }]" />
              </div>
              <div v-show="!msg.stepsCollapsed" class="steps-body">
                <div v-for="(s, i) in msg.steps" :key="i" class="step-item">
                  <div class="step-name">{{ i + 1 }}. {{ s.name }}</div>
                  <div class="step-content" v-html="renderMd(s.content)" />
                </div>
              </div>
            </div>
            <div v-show="msg.content" class="message-bubble">
              <div class="message-text">{{ msg.content }}</div>
              <!-- 流式加载动画 -->
              <span v-if="msg.id === activeMsgId && isStreaming" class="typing-cursor">|</span>
              <button
                v-if="msg.role === 'assistant' && extractPdfName(msg.content)"
                class="pdf-btn"
                @click="downloadPdf(extractPdfName(msg.content))"
                title="下载 PDF"
              >
                <Download :size="14" />
              </button>
              <button
                v-if="msg.role === 'assistant' && msg.content && msg.id !== activeMsgId"
                class="copy-btn"
                @click="copyMessage(msg.content)"
                title="复制"
              >
                <Copy :size="13" />
              </button>
            </div>
          </div>
        </div>
      </TransitionGroup>
    </div>

    <!-- 输入区 -->
    <div class="chat-composer" :style="{ '--composer-h': composerHeight + 'px' }">
      <div
        class="composer-handle"
        @mousedown="onDragStart"
        :class="{ dragging: isDragging }"
      />
      <div class="composer-inner">
        <textarea
          ref="textareaRef"
          v-model="input"
          :placeholder="isStreaming ? 'AI 正在回复...' : '输入消息，Enter 发送，Shift+Enter 换行'"
          :disabled="isStreaming"
          rows="1"
          @keydown="handleEnter"
          @input="handleInput"
        />
        <button
          v-if="isStreaming"
          class="send-btn stop"
          @click="stopStreaming"
          title="停止生成"
        >
          <Square :size="16" fill="#fff" />
        </button>
        <button
          v-else
          class="send-btn"
          :disabled="!input.trim()"
          @click="sendMessage"
          title="发送"
        >
          <Send :size="17" />
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.chatroom {
  display: flex;
  flex-direction: column;
  height: 100%;
  background: var(--bg);
}

/* ---- 头部 ---- */
.chat-header {
  flex-shrink: 0;
  padding: 14px 24px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
}
.chat-header-info { display: flex; align-items: center; gap: 12px; }
.chat-avatar {
  width: 38px; height: 38px;
  background: linear-gradient(135deg, var(--accent), #a855f7);
  border-radius: var(--radius-sm);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
}
.chat-header-info h2 { font-size: 16px; font-weight: 700; }
.status-dot {
  display: inline-block;
  width: 7px; height: 7px;
  border-radius: 50%;
  margin-right: 4px; vertical-align: middle;
  background: var(--text-muted);
}
.status-dot.open { background: var(--success); animation: pulse 2s infinite; }
.status-dot.connecting { background: var(--warning); animation: pulse 1s infinite; }
.status-text { font-size: 12px; color: var(--text-muted); vertical-align: middle; }

@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: .4; } }

/* ---- 消息列表 ---- */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
}
.message {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  animation: msg-in .35s cubic-bezier(.34,1.56,.64,1);
}
@keyframes msg-in { from { opacity: 0; transform: translateY(12px) scale(.97); } }

.message.user { flex-direction: row-reverse; }
.message-avatar {
  width: 32px; height: 32px; border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  background: var(--bg-hover);
  color: var(--text-muted);
}
.message.user .message-avatar { background: var(--accent); color: #fff; }
/* 头像右侧的主列：折叠条在上、气泡在下 */
.message-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-width: 70%;
  min-width: 0;
}
.message.user .message-main { align-items: flex-end; }
.message-bubble {
  max-width: 100%;
  padding: 12px 16px;
  border-radius: var(--radius);
  position: relative;
  transition: box-shadow .2s;
}

/* ---- 工具/思考折叠条 ---- */
.steps-collapse {
  width: 100%;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm, 8px);
  background: var(--bg-hover);
  overflow: hidden;
}
.steps-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  font-size: 12.5px;
  color: var(--text-muted);
  cursor: pointer;
  user-select: none;
}
.steps-header:hover { color: var(--text); }
.chevron { margin-left: auto; transition: transform .2s; }
.chevron.open { transform: rotate(180deg); }
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }
.steps-body {
  max-height: 320px;
  overflow-y: auto;
  padding: 4px 12px 10px;
  border-top: 1px dashed var(--border);
}
.step-item { margin-top: 8px; }
.step-name { font-size: 12px; font-weight: 600; color: var(--text); margin-bottom: 4px; }
.step-content { font-size: 12.5px; line-height: 1.6; color: var(--text-muted); word-break: break-word; }
.step-content :deep(p) { margin: 0 0 6px; }
.step-content :deep(pre) { background: var(--bg); padding: 8px; border-radius: 6px; overflow-x: auto; }
.step-content :deep(code) { font-size: 12px; }
.message.assistant .message-bubble {
  background: var(--bg-card);
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  box-shadow: var(--shadow-sm);
}
.message.user .message-bubble {
  background: linear-gradient(135deg, var(--accent), #7c3aed);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.message-text { font-size: 14px; line-height: 1.65; white-space: pre-wrap; word-break: break-word; }
.typing-cursor { display: inline; animation: blink .8s infinite; font-weight: 300; color: var(--accent); }
@keyframes blink { 0%,100% { opacity: 1; } 50% { opacity: 0; } }
.copy-btn {
  position: absolute; bottom: -20px; right: 0;
  padding: 2px 6px; color: var(--text-muted);
  font-size: 11px; opacity: 0; transition: opacity .15s;
}
.message-bubble:hover .copy-btn { opacity: 1; }
.copy-btn:hover { color: var(--accent); }
.pdf-btn {
  position: absolute; bottom: -22px; right: 28px;
  padding: 2px 6px; color: var(--accent);
  font-size: 11px; opacity: 0; transition: opacity .15s;
}
.message-bubble:hover .pdf-btn { opacity: 1; }
.pdf-btn:hover { color: var(--accent-hover); }

/* ---- 输入区 ---- */
.chat-composer {
  flex-shrink: 0;
  padding: 4px 24px 20px;
  background: var(--bg-card);
  border-top: 1px solid var(--border);
  user-select: none;
}
.composer-handle {
  height: 16px;
  cursor: ns-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 4px;
}
.composer-handle::after {
  content: '';
  width: 32px;
  height: 4px;
  border-radius: 2px;
  background: var(--border);
  pointer-events: none;
  transition: background .15s, width .15s;
}
.composer-handle:hover::after,
.composer-handle.dragging::after {
  background: var(--accent);
  width: 48px;
}
.composer-inner {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 8px 12px;
  background: var(--bg-input);
  border-radius: var(--radius);
  border: 2px solid transparent;
  transition: border var(--transition), box-shadow var(--transition);
}
.composer-inner:focus-within {
  border-color: var(--accent);
  box-shadow: 0 0 0 3px var(--accent-light);
  background: #fff;
}
.composer-inner textarea {
  flex: 1;
  font-size: 14px;
  line-height: 1.5;
  min-height: 24px;
  background: transparent;
  color: var(--text);
  transition: height .15s ease;
}
.composer-inner textarea::placeholder { color: var(--text-muted); }
.send-btn {
  width: 38px; height: 38px;
  border-radius: 50%;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0;
  background: var(--accent);
  color: #fff;
  transition: all var(--transition-spring);
}
.send-btn:hover:not(:disabled) { transform: scale(1.1); background: var(--accent-hover); }
.send-btn:disabled { opacity: .4; cursor: default; }
.send-btn.stop { background: var(--danger); }
.send-btn.stop:hover { background: var(--danger); transform: scale(1.1); }
</style>
