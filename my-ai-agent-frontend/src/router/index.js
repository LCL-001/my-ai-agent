import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
import ChatView from '@/views/ChatView.vue'
import LoginView from '@/views/LoginView.vue'
import LoveAppView from '@/views/LoveAppView.vue'
import ManusAppView from '@/views/ManusAppView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/chat', name: 'chat', component: ChatView },
    { path: '/chat/:id', name: 'chat-detail', component: ChatView },
    { path: '/login', name: 'login', component: LoginView },
    { path: '/love', name: 'love-app', component: LoveAppView },
    { path: '/manus', name: 'manus-app', component: ManusAppView },
  ],
})

export default router
