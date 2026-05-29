<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useConversationStore } from '@/stores/conversation'
import { useUserStore } from '@/stores/user'
import { useLoginPrompt } from '@/composables/useLoginPrompt'
import AppLayout from '@/components/AppLayout.vue'
import ChatRoom from '@/components/ChatRoom.vue'
import { Sparkles, Bot, MessageSquare, Zap, FileText, Search, Wand2, ArrowRight } from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const convStore = useConversationStore()
const userStore = useUserStore()

const currentChatId = ref(route.params.id || null)
const currentType = ref('manus')

const chatConfig = computed(() => {
  if (currentType.value === 'love') {
    return {
      endpoint: '/ai/love_app/chat/sse',
      greeting: '你好，我是 AI 恋爱大师。把你的关系处境、对方消息或想表达的话发给我，我们一起把话说漂亮。',
      aiName: 'Love OS',
    }
  }
  return {
    endpoint: '/ai/manus/chat',
    greeting: '你好，我是 AI 超级智能体。告诉我你的目标，我会边思考边把处理过程同步给你。',
    aiName: 'Manus Core',
  }
})

function syncType(chatId) {
  if (!chatId) { currentType.value = 'manus'; return }
  const conv = convStore.list.find(c => c.id === chatId)
  if (conv) currentType.value = conv.type || 'manus'
}

watch(() => route.params.id, (id) => {
  currentChatId.value = id || null
  syncType(currentChatId.value)
})

const { show: showLoginPrompt } = useLoginPrompt()

async function startChat(type) {
  try {
    const conv = await convStore.create(type)
    router.push(`/chat/${conv.id}`)
  } catch (e) {
    if (e.message === 'NEED_LOGIN') showLoginPrompt()
  }
}

onMounted(() => {
  if (userStore.isLoggedIn) {
    convStore.fetchList().then(() => syncType(currentChatId.value))
  }
})
</script>

<template>
  <!-- 未登录：全屏欢迎页，无侧边栏 -->
  <div v-if="!userStore.isLoggedIn && !currentChatId" class="landing">
    <div class="bg-layer">
      <div class="bg-orb o1" />
      <div class="bg-orb o2" />
      <div class="bg-orb o3" />
      <div class="bg-orb o4" />
      <div class="bg-grid" />
    </div>

    <div class="landing-hero">
      <div class="hero-icon-wrap">
        <div class="hero-glow" />
        <div class="hero-icon">
          <Sparkles :size="36" stroke-width="1.5" />
        </div>
      </div>

      <h1 class="hero-title">
        <span class="gradient-text">YuLin AI Agent</span>
      </h1>
      <p class="hero-subtitle">多智能体协作平台，让 AI 为你完成复杂任务</p>

      <div class="feature-tags">
        <span><Zap :size="14" /> 工具调用</span>
        <span><Search :size="14" /> 联网搜索</span>
        <span><FileText :size="14" /> PDF 生成</span>
        <span><Wand2 :size="14" /> 多轮对话</span>
      </div>

      <div class="hero-cards">
        <button class="hero-card manus" @click="showLoginPrompt()">
          <div class="hc-shine" />
          <div class="hc-icon-wrap"><Bot :size="28" stroke-width="1.5" /></div>
          <div class="hc-body">
            <strong>超级智能体</strong>
            <p>多工具协作，复杂任务自动拆解与执行</p>
          </div>
          <div class="hc-arrow">&#8594;</div>
        </button>
        <button class="hero-card love" @click="showLoginPrompt()">
          <div class="hc-shine" />
          <div class="hc-icon-wrap"><MessageSquare :size="28" stroke-width="1.5" /></div>
          <div class="hc-body">
            <strong>恋爱大师</strong>
            <p>情感对话专家，给你温暖专业的建议</p>
          </div>
          <div class="hc-arrow">&#8594;</div>
        </button>
      </div>

      <button class="cta-btn" @click="router.push('/login')">
        <span>即刻出发</span>
        <ArrowRight :size="20" />
      </button>
    </div>
  </div>

  <!-- 已登录或在对话中：侧边栏 + 主区域 -->
  <AppLayout v-else>
    <div v-if="!currentChatId" class="welcome">
      <div class="bg-layer">
        <div class="bg-orb o1" />
        <div class="bg-orb o2" />
        <div class="bg-orb o3" />
        <div class="bg-orb o4" />
        <div class="bg-grid" />
      </div>

      <div class="welcome-hero">
        <div class="hero-icon-wrap">
          <div class="hero-glow" />
          <div class="hero-icon">
            <Sparkles :size="36" stroke-width="1.5" />
          </div>
        </div>

        <h1 class="hero-title">
          <span class="gradient-text">YuLin AI Agent</span>
        </h1>
        <p class="hero-subtitle">多智能体协作平台，让 AI 为你完成复杂任务</p>

        <div class="feature-tags">
          <span><Zap :size="14" /> 工具调用</span>
          <span><Search :size="14" /> 联网搜索</span>
          <span><FileText :size="14" /> PDF 生成</span>
          <span><Wand2 :size="14" /> 多轮对话</span>
        </div>

        <div class="hero-cards">
          <button class="hero-card manus" @click="startChat('manus')">
            <div class="hc-shine" />
            <div class="hc-icon-wrap"><Bot :size="28" stroke-width="1.5" /></div>
            <div class="hc-body">
              <strong>超级智能体</strong>
              <p>多工具协作，复杂任务自动拆解与执行</p>
            </div>
            <div class="hc-arrow">&#8594;</div>
          </button>
          <button class="hero-card love" @click="startChat('love')">
            <div class="hc-shine" />
            <div class="hc-icon-wrap"><MessageSquare :size="28" stroke-width="1.5" /></div>
            <div class="hc-body">
              <strong>恋爱大师</strong>
              <p>情感对话专家，给你温暖专业的建议</p>
            </div>
            <div class="hc-arrow">&#8594;</div>
          </button>
        </div>
      </div>
    </div>

    <ChatRoom
      v-else
      :endpoint="chatConfig.endpoint"
      :greeting="chatConfig.greeting"
      :ai-name="chatConfig.aiName"
      :chat-id="currentChatId"
    />
  </AppLayout>
</template>

<style scoped>
/* ===== 共享背景 ===== */
.bg-layer { position: absolute; inset: 0; overflow: hidden; }
.bg-orb {
  position: absolute; border-radius: 50%; filter: blur(100px); opacity: .25;
  animation: orb-drift 12s ease-in-out infinite;
}
.o1 { width: 500px; height: 500px; background: #6366f1; top: -150px; left: -100px; }
.o2 { width: 400px; height: 400px; background: #a855f7; bottom: -120px; right: -80px; animation-delay: -4s; }
.o3 { width: 300px; height: 300px; background: #06b6d4; top: 40%; left: 50%; animation-delay: -8s; }
.o4 { width: 350px; height: 350px; background: #f43f5e; top: 10%; right: 15%; animation-delay: -2s; }
@keyframes orb-drift {
  0%,100% { transform: translate(0,0) scale(1); }
  25% { transform: translate(60px,-40px) scale(1.1); }
  50% { transform: translate(-30px,50px) scale(.9); }
  75% { transform: translate(-50px,-30px) scale(1.05); }
}
.bg-grid {
  position: absolute; inset: 0;
  background-image:
    linear-gradient(rgba(99,102,241,.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(99,102,241,.04) 1px, transparent 1px);
  background-size: 60px 60px;
}

/* ===== 未登录落地页 ===== */
.landing {
  height: 100%;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  background: #0a0a1a;
}
.landing-hero {
  position: relative; z-index: 1;
  text-align: center; max-width: 560px; padding: 40px 20px;
}

/* ===== 已登录欢迎页（在侧边栏内） ===== */
.welcome {
  flex: 1;
  display: flex; align-items: center; justify-content: center;
  position: relative; overflow: hidden;
  background: #0a0a1a;
}

/* ===== 共享组件 ===== */
.welcome-hero, .landing-hero {
  position: relative; z-index: 1; text-align: center; max-width: 560px; padding: 40px 20px;
}
.hero-icon-wrap {
  position: relative; width: 100px; height: 100px; margin: 0 auto 32px;
}
.hero-glow {
  position: absolute; inset: -15px; border-radius: 50%;
  background: conic-gradient(from 0deg, #6366f1, #a855f7, #06b6d4, #6366f1);
  animation: glow-spin 4s linear infinite; opacity: .6; filter: blur(15px);
}
@keyframes glow-spin { to { transform: rotate(360deg); } }
.hero-icon {
  position: relative; z-index: 1; width: 100%; height: 100%;
  border-radius: 24px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  display: flex; align-items: center; justify-content: center;
  color: #fff;
  box-shadow: 0 0 60px rgba(99,102,241,.4);
  animation: float-icon 4s ease-in-out infinite;
}
@keyframes float-icon {
  0%,100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}
.gradient-text {
  background: linear-gradient(135deg, #a5b4fc, #c4b5fd, #67e8f9);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-title { font-size: 40px; font-weight: 900; letter-spacing: -1px; margin-bottom: 10px; }
.hero-subtitle { font-size: 16px; color: rgba(255,255,255,.55); margin-bottom: 28px; }
.feature-tags {
  display: flex; justify-content: center; gap: 10px; flex-wrap: wrap; margin-bottom: 36px;
}
.feature-tags span {
  display: flex; align-items: center; gap: 5px;
  padding: 6px 14px; border-radius: 20px;
  background: rgba(255,255,255,.06); border: 1px solid rgba(255,255,255,.08);
  color: rgba(255,255,255,.65); font-size: 12px; font-weight: 500;
}

/* ===== 即刻出发按钮 ===== */
.cta-btn {
  display: inline-flex; align-items: center; gap: 10px;
  margin-top: 28px;
  padding: 16px 40px;
  border-radius: 50px;
  background: linear-gradient(135deg, #6366f1, #a855f7);
  color: #fff;
  font-size: 18px; font-weight: 700;
  box-shadow: 0 8px 32px rgba(99,102,241,.35);
  transition: all .3s cubic-bezier(.34,1.56,.64,1);
}
.cta-btn:hover {
  transform: translateY(-2px) scale(1.05);
  box-shadow: 0 12px 40px rgba(99,102,241,.5);
}

/* ===== 选择卡片 ===== */
.hero-cards { display: flex; flex-direction: column; gap: 14px; }
.hero-card {
  position: relative; display: flex; align-items: center; gap: 16px;
  padding: 20px 24px; border-radius: 16px;
  text-align: left; cursor: pointer; overflow: hidden;
  transition: all .4s cubic-bezier(.34,1.56,.64,1);
}
.hero-card.manus {
  background: linear-gradient(135deg, rgba(99,102,241,.15), rgba(139,92,246,.1));
  border: 1px solid rgba(99,102,241,.25);
}
.hero-card.love {
  background: linear-gradient(135deg, rgba(244,63,94,.15), rgba(225,29,72,.1));
  border: 1px solid rgba(244,63,94,.25);
}
.hero-card:hover { transform: translateY(-3px) scale(1.02); }
.hero-card.manus:hover {
  border-color: rgba(99,102,241,.6);
  box-shadow: 0 0 40px rgba(99,102,241,.2), 0 8px 32px rgba(0,0,0,.3);
}
.hero-card.love:hover {
  border-color: rgba(244,63,94,.6);
  box-shadow: 0 0 40px rgba(244,63,94,.2), 0 8px 32px rgba(0,0,0,.3);
}
.hc-shine {
  position: absolute; inset: 0;
  background: linear-gradient(105deg, transparent 40%, rgba(255,255,255,.06) 45%, rgba(255,255,255,.12) 50%, rgba(255,255,255,.06) 55%, transparent 60%);
  transform: translateX(-100%); transition: transform .6s;
}
.hero-card:hover .hc-shine { transform: translateX(100%); }
.hc-icon-wrap {
  width: 56px; height: 56px; border-radius: 14px;
  display: flex; align-items: center; justify-content: center;
  flex-shrink: 0; color: #fff;
}
.hero-card.manus .hc-icon-wrap { background: linear-gradient(135deg, #6366f1, #8b5cf6); }
.hero-card.love .hc-icon-wrap { background: linear-gradient(135deg, #f43f5e, #e11d48); }
.hc-body { flex: 1; min-width: 0; }
.hc-body strong { display: block; font-size: 17px; font-weight: 700; color: #fff; margin-bottom: 4px; }
.hc-body p { font-size: 13px; color: rgba(255,255,255,.5); }
.hc-arrow {
  font-size: 22px; color: rgba(255,255,255,.2); transition: all .3s;
}
.hero-card:hover .hc-arrow { color: #fff; transform: translateX(4px); }
</style>
