<script setup>
import ChatRoom from '@/components/ChatRoom.vue'
</script>

<template>
  <ChatRoom
    app-key="manus"
    title="AI 超级智能体"
    subtitle="描述目标或任务，我会实时输出智能体的分析、计划与执行过程。"
    endpoint="/ai/manus/chat"
    accent="cyan"
    greeting="你好，我是 AI 超级智能体。告诉我你的目标，我会边思考边把处理过程同步给你。"
    ai-name="Manus Core"
    seo-title="AI 超级智能体 | My AI Agent"
    seo-description="AI 超级智能体支持任务理解、计划拆解和执行过程流式输出，适合智能体能力验证。"
    seo-keywords="AI 超级智能体,Manus,智能体,SSE,任务拆解,My AI Agent"
    :with-chat-id="false"
  />
</template>
