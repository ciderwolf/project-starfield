import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '@/views/HomeView.vue'
import LoginView from '@/views/LoginView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: HomeView
    },
    {
      path: '/login',
      name: 'login',
      component: LoginView
    },
    {
      path: '/create-account',
      name: 'create-account',
      component: () => import('../views/CreateAccountView.vue')
    },
    {
      path: '/lobby/:id',
      name: 'lobby',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/LobbyView.vue')
    },
    {
      path: '/game/:id',
      name: 'game',
      // route level code-splitting
      // this generates a separate chunk (About.[hash].js) for this route
      // which is lazy-loaded when the route is visited.
      component: () => import('../views/GameView.vue')
    },
    {
      path: '/deckbuilder/:id',
      name: 'deckbuilder',
      component: () => import('../views/DecklistView.vue')
    },
    {
      path: '/cube/:id',
      name: 'cube',
      component: () => import('../views/CubeView.vue')
    },
    {
      path: '/draft/:id',
      name: 'draft',
      component: () => import('../views/DraftView.vue')
    }
  ]
})

export default router
