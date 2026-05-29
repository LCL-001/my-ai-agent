<script setup>
import AppLayout from '@/components/AppLayout.vue'
import ChatRoom from '@/components/ChatRoom.vue'
</script>

<template>
  <AppLayout>
    <ChatRoom
      endpoint="/ai/love_app/chat/sse"
      greeting="你好，我是 AI 恋爱大师。把你的关系处境、对方消息或想表达的话发给我，我们一起把话说漂亮。"
      ai-name="Love OS"
    />
  </AppLayout>
</template>
