import { createRouter, createWebHistory } from 'vue-router'

import HomeView from '@/views/HomeView.vue'
const ChatView = () => import('@/views/ChatView.vue')
const LoginView = () => import('@/views/LoginView.vue')
const LoveAppView = () => import('@/views/LoveAppView.vue')
const ManusAppView = () => import('@/views/ManusAppView.vue')

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
