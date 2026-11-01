import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    redirect: '/home',
    children: [
      { path: 'home',       name: 'Home',       component: () => import('@/views/Home.vue'),       meta: { title: '数据总览' } },
      { path: 'trend',      name: 'Trend',      component: () => import('@/views/Trend.vue'),      meta: { title: '访问趋势' } },
      { path: 'funnel',     name: 'Funnel',     component: () => import('@/views/Funnel.vue'),     meta: { title: '转化漏斗' } },
      { path: 'dimension',  name: 'Dimension',  component: () => import('@/views/Dimension.vue'),  meta: { title: '多维分析' } },
      { path: 'comparison', name: 'Comparison', component: () => import('@/views/Comparison.vue'), meta: { title: '用户对比' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => {
  const store = useUserStore()
  if (!to.meta.public && !store.isLoggedIn()) return '/login'
})

export default router
