import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import AppLayout from '@/components/AppLayout.vue'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/',
    redirect: () => {
      const authStore = useAuthStore()
      return authStore.isAdmin ? '/dashboard' : '/itineraries'
    },
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true, title: '登录' },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true, title: '注册' },
  },
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '系统数据看板', requiresAdmin: true },
      },
      {
        path: 'attractions',
        name: 'attractions',
        component: () => import('@/views/AttractionPlazaView.vue'),
        meta: { title: '热门景点广场' },
      },
      {
        path: 'attractions/edit/:id?',
        name: 'attraction-edit',
        component: () => import('@/views/AttractionEditView.vue'),
        meta: { title: '基础图鉴管理', requiresAdmin: true },
      },
      {
        path: 'planner',
        name: 'planner',
        component: () => import('@/views/PlannerView.vue'),
        meta: { title: '智能规划', userOnly: true },
      },
      {
        path: 'planner/draft',
        name: 'planner-draft',
        component: () => import('@/views/PlanDraftView.vue'),
        meta: { title: '智能规划', userOnly: true },
      },
      {
        path: 'itineraries',
        name: 'itineraries',
        component: () => import('@/views/ItineraryListView.vue'),
        meta: { title: '我的行程', userOnly: true },
      },
      {
        path: 'itineraries/:id',
        name: 'itinerary-detail',
        component: () => import('@/views/ItineraryDetailView.vue'),
        meta: { title: '行程详情', userOnly: true },
      },
      {
        path: 'profile',
        name: 'profile',
        component: () => import('@/views/ProfileView.vue'),
        meta: { title: '我的资料' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to) => {
  const authStore = useAuthStore()
  if (!to.meta.public && !authStore.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.public && authStore.isLoggedIn) {
    return { name: 'dashboard' }
  }
  if (to.name === 'dashboard' && authStore.isLoggedIn && !authStore.isAdmin) {
    return { name: 'itineraries' }
  }
  if (to.meta.requiresAdmin && !authStore.isAdmin) {
    ElMessage.warning('您无权访问管理员控制台')
    return { name: 'dashboard' }
  }
  if (to.meta.userOnly && authStore.isAdmin) {
    ElMessage.warning('管理员账号不进入用户行程功能')
    return { name: 'dashboard' }
  }
  return true
})

export default router
