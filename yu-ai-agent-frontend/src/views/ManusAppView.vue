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
    :with-chat-id="false"
  />
</template>
