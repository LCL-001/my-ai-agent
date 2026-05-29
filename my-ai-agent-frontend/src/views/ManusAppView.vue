<script setup>
import AppLayout from '@/components/AppLayout.vue'
import ChatRoom from '@/components/ChatRoom.vue'
</script>

<template>
  <AppLayout>
    <ChatRoom
      endpoint="/ai/manus/chat"
      greeting="你好，我是 AI 超级智能体。告诉我你的目标，我会边思考边把处理过程同步给你。"
      ai-name="Manus Core"
    />
  </AppLayout>
</template>
