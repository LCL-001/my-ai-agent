<script setup>
import { Bot, HeartHandshake, MessageSquareText, MoveRight, Sparkles } from 'lucide-vue-next'

const apps = [
  {
    title: 'AI 恋爱大师',
    description: '面向情感沟通场景，像真实聊天室一样获取即时回复。',
    to: '/love',
    icon: HeartHandshake,
    meta: 'SSE · chatId 会话',
    tone: 'coral',
  },
  {
    title: 'AI 超级智能体',
    description: '连接 Manus 智能体能力，实时接收任务拆解和执行反馈。',
    to: '/manus',
    icon: Bot,
    meta: 'SSE · Agent Stream',
    tone: 'cyan',
  },
]
</script>

<template>
  <main class="home-page">
    <section class="home-hero">
      <div class="brand-mark" aria-hidden="true">
        <Sparkles :size="26" />
      </div>
      <p class="eyebrow">Yu AI Agent Console</p>
      <h1>选择一个 AI 应用，开始实时对话</h1>
      <p class="hero-copy">
        主页用于切换不同应用。每个应用都以聊天室形式承载 SSE 流式响应，适合快速验证后端智能体能力。
      </p>
    </section>

    <section class="app-switcher" aria-label="应用列表">
      <RouterLink v-for="app in apps" :key="app.to" class="app-card" :class="app.tone" :to="app.to">
        <span class="app-icon">
          <component :is="app.icon" :size="30" />
        </span>
        <span class="app-content">
          <span class="app-meta">
            <MessageSquareText :size="15" />
            {{ app.meta }}
          </span>
          <strong>{{ app.title }}</strong>
          <span>{{ app.description }}</span>
        </span>
        <span class="open-indicator" aria-hidden="true">
          <MoveRight :size="22" />
        </span>
      </RouterLink>
    </section>
  </main>
</template>
