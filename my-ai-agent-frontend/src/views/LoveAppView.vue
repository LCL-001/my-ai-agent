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
    ai-name="Love OS"
    seo-title="AI 恋爱大师 | My AI Agent"
    seo-description="AI 恋爱大师提供情感沟通、聊天回复建议和关系场景分析，支持 SSE 流式实时对话。"
    seo-keywords="AI 恋爱大师,情感聊天助手,恋爱回复,SSE 流式对话,My AI Agent"
    :with-chat-id="true"
  />
</template>
