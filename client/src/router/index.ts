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
      path: '/cubes',
      name: 'cubes',
      component: () => import('../views/CubesView.vue')
    },
    {
      path: '/cubes/:id',
      name: 'cube',
      component: () => import('../views/CubeView.vue')
    },
    {
      path: '/draft/:id',
      name: 'draft',
      component: () => import('../views/DraftView.vue')
    },
    {
      path: '/sets',
      name: 'sets',
      component: () => import('../views/SetsView.vue')
    },
    {
      path: '/sets/:set/cards',
      name: 'card-search',
      component: () => import('../views/CardSearchView.vue')
    },
    {
      path: '/sets/:set/advanced-search',
      name: 'advanced-search',
      component: () => import('../views/AdvancedSearchView.vue')
    },
    {
      path: '/sets/:set/cards/:id',
      name: 'card-details',
      component: () => import('../views/CardDetailView.vue')
    }
  ]
})

export default router
