import { createRouter, createWebHistory } from 'vue-router'
import CinemaListView from '@/views/CinemaListView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      redirect: '/cinemas',
    },
    {
      path: '/cinemas',
      name: 'cinemas',
      component: CinemaListView,
    },
  ],
})

export default router
