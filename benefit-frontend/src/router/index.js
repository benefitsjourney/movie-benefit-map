import { createRouter, createWebHistory } from 'vue-router'
import CinemaView from '@/views/CinemaView.vue'
import MapView from '@/views/MapView.vue'

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
      component: CinemaView,
    },
    {
      path: '/map',
      name: 'map',
      component: MapView,
    },
  ],
})

export default router
