import { createRouter, createWebHistory } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import FrontLayout from '@/layouts/FrontLayout.vue'
import AdminLayout from '@/layouts/AdminLayout.vue'
import AdminCrawlerView from '@/views/admin/crawler/AdminCrawlerView.vue'
import AdminCrawlerLogView from '@/views/admin/crawler/AdminCrawlerLogView.vue'
import AdminCinemaView from '@/views/admin/cinema-movie/AdminCinemaView.vue'
import AdminCinemaDetail from '@/views/admin/cinema-movie/AdminCinemaDetail.vue'
import AdminCinemaForm from '@/views/admin/cinema-movie/AdminCinemaForm.vue'
import AdminMovieView from '@/views/admin/cinema-movie/AdminMovieView.vue'
import AdminMovieDetail from '@/views/admin/cinema-movie/AdminMovieDetail.vue'
import AdminMovieForm from '@/views/admin/cinema-movie/AdminMovieForm.vue'
import AdminCrawlerReviewView from '@/views/admin/cinema-movie/AdminCrawlerReviewView.vue'
import AdminBenefitReportView from '@/views/admin/AdminBenefitReportView.vue'
import AdminBenefitView from '@/views/admin/AdminBenefitView.vue'
import AdminInventoryReportView from '@/views/admin/AdminInventoryReportView.vue'
import AdminExcelExportView from '@/views/admin/AdminExcelExportView.vue'
import BenefitView from '@/views/BenefitView.vue'
import BenefitDetailView from '@/views/BenefitDetailView.vue'
import CollectionView from '@/views/CollectionView.vue'
import MyReportView from '@/views/MyReportView.vue'
import BenefitReportFormView from '@/views/BenefitReportFormView.vue'
import CalendarView from '@/views/CalendarView.vue'
import ReminderView from '@/views/ReminderView.vue'
import RankingView from '@/views/RankingView.vue'
import CinemaView from '@/views/CinemaView.vue'
import MapView from '@/views/MapView.vue'
import MovieView from '@/views/MovieView.vue'
import LoginView from '@/views/LoginView.vue'
import RegisterView from '@/views/RegisterView.vue'
import ForgotPasswordView from '@/views/ForgotPasswordView.vue'
import ResetPasswordView from '@/views/ResetPasswordView.vue'
import AccountLayout from '@/layouts/AccountLayout.vue'
import AccountHomeView from '@/account/AccountHomeView.vue'
import AccountProfileView from '@/account/AccountProfileView.vue'
import AccountListingsView from '@/account/AccountListingsView.vue'
import AccountFavoritesView from '@/account/AccountFavoritesView.vue'
import AccountBenefitsView from '@/account/AccountBenefitsView.vue'
import AccountMessagesView from '@/account/AccountMessagesView.vue'
import MessageInboxView from '@/messages/MessageInboxView.vue'
import MessageConversationView from '@/messages/MessageConversationView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 會員認證：獨立頁面，不套用 FrontLayout 或 AdminLayout
    {
      path: '/login',
      name: 'login',
      component: LoginView,
    },
    {
      path: '/register',
      name: 'register',
      component: RegisterView,
    },
    {
      path: '/forgot-password',
      name: 'forgot-password',
      component: ForgotPasswordView,
    },
    {
      path: '/reset-password',
      name: 'reset-password',
      component: ResetPasswordView,
    },
    {
      path: '/account',
      component: AccountLayout,
      meta: { requiresAuth: true },
      children: [
        {
          path: '',
          name: 'account-home',
          component: AccountHomeView,
        },
        {
          path: 'profile',
          name: 'account-profile',
          component: AccountProfileView,
        },
        {
          path: 'listings',
          name: 'account-listings',
          component: AccountListingsView,
        },
        {
          path: 'favorites',
          name: 'account-favorites',
          component: AccountFavoritesView,
        },
        {
          path: 'benefits',
          name: 'account-benefits',
          component: AccountBenefitsView,
        },
        {
          path: 'messages',
          name: 'account-messages',
          component: MessageInboxView,
        },
        {
          path: 'messages/:conversationId',
          name: 'message-conversation',
          component: MessageConversationView,
        },
      ],
    },
    {
      path: '/',
      component: FrontLayout,
      children: [
        {
          path: '',
          redirect: '/map',
        },
        {
          path: 'benefits',
          name: 'benefits',
          component: BenefitView,
        },
        {
          path: 'benefits/:id',
          name: 'benefit-detail',
          component: BenefitDetailView,
        },
        {
          path: 'collection',
          name: 'collection',
          component: CollectionView
        },
        {
          path: 'my-reports',
          name: 'my-reports',
          component: MyReportView,
          meta: { requiresAuth: true },
        },
        {
          path: 'report-benefit',
          name: 'report-benefit',
          component: BenefitReportFormView,
          meta: { requiresAuth: true },
        },
        {
          path: 'calendar',
          name: 'calendar',
          component: CalendarView,
        },
        {
          path: 'reminders',
          name: 'reminders',
          component: ReminderView,
          meta: { requiresAuth: true },
        },
        {
          path: 'ranking',
          name: 'ranking',
          component: RankingView,
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
          path: 'benefit-reports',
          name: 'admin-benefit-reports',
          component: AdminBenefitReportView
        },
        {
          path: 'benefits',
          name: 'admin-benefits',
          component: AdminBenefitView
        },
        {
          path: 'inventory-reports',
          name: 'admin-inventory-reports',
          component: AdminInventoryReportView
        },
        {
          path: 'crawler',
          name: 'admin-crawler',
          component: AdminCrawlerView,
        },
        {
          path: 'crawler-logs',
          name: 'admin-crawler-logs',
          component: AdminCrawlerLogView,
        },
        {
          path: 'cinemas-movies/reviews',
          name: 'admin-reviews',
          component: AdminCrawlerReviewView,
        },
        {
          path: 'cinemas-movies/reviews/:id',
          redirect: { name: 'admin-reviews' },
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
        {
          path: 'excel',
          name: 'admin-excel',
          component: AdminExcelExportView,
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuth()

  // 每次切換路由時，重新讀取最新的登入資訊
  auth.refreshAuth()

  // 管理後台：必須登入，而且是啟用中的管理員
  if (to.matched.some((record) => record.path === '/admin')) {
    if (!auth.isLoggedIn.value) {
      return {
        name: 'login',
        query: { redirect: to.fullPath },
      }
    }

    if (!auth.isAdmin.value) {
      return { name: 'map' }
    }
  }

  // 其他需要登入的頁面
  if (to.meta.requiresAuth && !auth.isLoggedIn.value) {
    return {
      name: 'login',
      query: { redirect: to.fullPath },
    }
  }

  return true
})

export default router
