import { createRouter, createWebHistory } from 'vue-router'
import FrontLayout from '@/layouts/FrontLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AdminCinemaMovieCrawlerView from '@/views/admin/AdminCinemaMovieCrawlerView.vue'
import AdminCinemaView from '@/views/admin/cinema-movie/AdminCinemaView.vue'
import AdminCinemaDetail from '@/views/admin/cinema-movie/AdminCinemaDetail.vue'
import AdminCinemaForm from '@/views/admin/cinema-movie/AdminCinemaForm.vue'
import AdminMovieView from '@/views/admin/cinema-movie/AdminMovieView.vue'
import AdminMovieDetail from '@/views/admin/cinema-movie/AdminMovieDetail.vue'
import AdminMovieForm from '@/views/admin/cinema-movie/AdminMovieForm.vue'
import AdminCrawlerReviewView from '@/views/admin/cinema-movie/AdminCrawlerReviewView.vue'
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
          component: AdminCinemaMovieCrawlerView,
        },
        {
          path: 'crawler-reviews',
          name: 'admin-crawler-reviews',
          component: AdminCrawlerReviewView,
        },
        {
          path: 'crawler-reviews/:id',
          redirect: { name: 'admin-crawler-reviews' },
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
          path: 'cinemas/:id',
          name: 'admin-cinema-detail',
          component: AdminCinemaDetail,
        },
        {
          path: 'cinemas/:id/edit',
          redirect: (to) => ({ name: 'admin-cinema-detail', params: { id: to.params.id } }),
        },
        {
          path: 'movies',
          name: 'admin-movies',
          component: AdminMovieView,
        },
        {
          path: 'movies/new',
          name: 'admin-movie-create',
          component: AdminMovieForm,
        },
        {
          path: 'movies/:id',
          name: 'admin-movie-detail',
          component: AdminMovieDetail,
        },
        {
          path: 'movies/:id/edit',
          redirect: (to) => ({ name: 'admin-movie-detail', params: { id: to.params.id } }),
        },
      ],
    },
  ],
})

export default router
