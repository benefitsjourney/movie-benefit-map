import { createRouter, createWebHistory } from 'vue-router'
import FrontLayout from '@/layouts/FrontLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AdminCinemaView from '@/views/AdminCinemaView.vue'
import AdminCinemaForm from '@/views/AdminCinemaForm.vue'
import AdminCrawlerView from '@/views/AdminCrawlerView.vue'
import CinemaView from '@/views/CinemaView.vue'
import MapView from '@/views/MapView.vue'
import MovieView from '@/views/MovieView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: FrontLayout,
      children: [
        {
          path: '',
          redirect: '/map',
        },
        {
          path: 'movies',
          name: 'movies',
          component: MovieView,
        },
        {
          path: 'cinemas',
          name: 'cinemas',
          component: CinemaView,
        },
        {
          path: 'map',
          name: 'map',
          component: MapView,
        },
      ],
    },
    {
      path: '/admin',
      component: AdminLayout,
      redirect: '/admin/crawler',
      children: [
        {
          path: 'crawler',
          name: 'admin-crawler',
          component: AdminCrawlerView,
        },
        {
          path: 'cinemas',
          name: 'admin-cinemas',
          component: AdminCinemaView,
        },
        {
          path: 'cinemas/new',
          name: 'admin-cinema-create',
          component: AdminCinemaForm,
        },
        {
          path: 'cinemas/:id/edit',
          name: 'admin-cinema-edit',
          component: AdminCinemaForm,
        },
      ],
    },
  ],
})

export default router
