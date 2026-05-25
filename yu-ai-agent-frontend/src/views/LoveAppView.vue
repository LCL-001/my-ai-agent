<script setup>
import ChatRoom from '@/components/ChatRoom.vue'
</script>

<template>
  <ChatRoom
    app-key="love"
    title="AI 恋爱大师"
    subtitle="给出你的聊天困惑，我会用更自然、更有分寸的方式帮你组织回复。"
    endpoint="/ai/love_app/chat/sse"
    accent="coral"
    greeting="你好，我是 AI 恋爱大师。把你的关系处境、对方消息或想表达的话发给我，我们一起把话说漂亮。"
    :with-chat-id="true"
  />
</template>
