<script setup>
import { Bot, Cpu, HeartHandshake, MessageSquareText, MoveRight, RadioTower, Terminal } from 'lucide-vue-next'

import { useSeo } from '@/composables/useSeo'

useSeo({
  title: 'My AI Agent | 极客 AI 应用控制台',
  description: 'My AI Agent 提供 AI 恋爱大师和 AI 超级智能体两个 SSE 流式 AI 应用入口，支持实时对话和智能体任务处理。',
  keywords: 'My AI Agent,AI 应用控制台,AI 恋爱大师,AI 超级智能体,SSE 流式对话,Vue3',
})

const apps = [
  {
    title: 'AI 恋爱大师',
    description: '面向情感沟通场景，像真实聊天室一样获取即时回复。',
    to: '/love',
    icon: HeartHandshake,
    meta: 'SSE · chatId 会话',
    tone: 'coral',
    command: 'run love-advisor --stream',
  },
  {
    title: 'AI 超级智能体',
    description: '连接 Manus 智能体能力，实时接收任务拆解和执行反馈。',
    to: '/manus',
    icon: Bot,
    meta: 'SSE · Agent Stream',
    tone: 'cyan',
    command: 'launch manus-agent --live',
  },
]
</script>

<template>
  <main class="home-page">
    <div class="matrix-layer" aria-hidden="true"></div>
    <section class="home-hero">
      <div class="hero-console">
        <div class="brand-mark" aria-hidden="true">
          <Terminal :size="26" />
        </div>
        <p class="eyebrow">
          <RadioTower :size="15" />
          My AI Agent Console
        </p>
        <h1>把 AI 应用开进你的作战终端</h1>
        <p class="hero-copy">
          极客风格实时智能体控制台，一屏切换恋爱顾问与超级智能体，SSE 响应像电流一样持续抵达。
        </p>
        <div class="hero-metrics" aria-label="平台能力">
          <span><strong>02</strong> AI Apps</span>
          <span><strong>SSE</strong> Live Stream</span>
          <span><strong>Vue3</strong> Frontend</span>
        </div>
      </div>

      <div class="signal-panel" aria-hidden="true">
        <div class="signal-panel__bar">
          <span></span>
          <span></span>
          <span></span>
        </div>
        <div class="radar-core">
          <Cpu :size="54" />
        </div>
        <div class="code-feed">
          <span>> boot ai_agent.kernel</span>
          <span>> connect localhost:8123/api</span>
          <span>> stream channel: online</span>
          <span>> choose module: waiting</span>
        </div>
      </div>
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
          <code>{{ app.command }}</code>
        </span>
        <span class="open-indicator" aria-hidden="true">
          <MoveRight :size="22" />
        </span>
      </RouterLink>
    </section>
  </main>
</template>
