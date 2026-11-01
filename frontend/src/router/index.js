import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { requiresAuth: true, title: '数据看板' }
      },
      {
        path: 'task/submit',
        name: 'TaskSubmit',
        component: () => import('@/views/task/TaskSubmit.vue'),
        meta: { requiresAuth: true, title: '提交任务' }
      },
      {
        path: 'task/list',
        name: 'TaskList',
        component: () => import('@/views/task/TaskList.vue'),
        meta: { requiresAuth: true, title: '任务列表' }
      },
      {
        path: 'task/detail/:id',
        name: 'TaskDetail',
        component: () => import('@/views/task/TaskDetail.vue'),
        meta: { requiresAuth: true, title: '任务详情' }
      },
      {
        path: 'stat/basic-metrics',
        name: 'BasicMetrics',
        component: () => import('@/views/stat/BasicMetrics.vue'),
        meta: { requiresAuth: true, title: '基础指标分析' }
      },
      {
        path: 'stat/multi-dimension',
        name: 'MultiDimension',
        component: () => import('@/views/stat/MultiDimension.vue'),
        meta: { requiresAuth: true, title: '多维度分析' }
      },
      {
        path: 'stat/conversion-funnel',
        name: 'ConversionFunnel',
        component: () => import('@/views/stat/ConversionFunnel.vue'),
        meta: { requiresAuth: true, title: '转化漏斗分析' }
      },
      {
        path: 'stat/user-comparison',
        name: 'UserComparison',
        component: () => import('@/views/stat/UserComparison.vue'),
        meta: { requiresAuth: true, title: '新老用户对比' }
      },
      {
        path: 'admin/user-manage',
        name: 'UserManage',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '用户管理' }
      },
      {
        path: 'admin/task-manage',
        name: 'TaskManage',
        component: () => import('@/views/admin/TaskManage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '任务管理' }
      },
      {
        path: 'admin/data-manage',
        name: 'DataManage',
        component: () => import('@/views/admin/DataManage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '数据管理' }
      },
      {
        path: 'admin/cache-manage',
        name: 'CacheManage',
        component: () => import('@/views/admin/CacheManage.vue'),
        meta: { requiresAuth: true, requiresAdmin: true, title: '缓存管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.meta.requiresAdmin && !isAdmin()) {
    next('/dashboard')
  } else {
    next()
  }
})

function isAdmin() {
  const userInfo = JSON.parse(localStorage.getItem('userInfo') || '{}')
  return userInfo.id === 1
    || userInfo.username === 'admin'
    || (userInfo.roles && userInfo.roles.includes('ROLE_ADMIN'))
}

export default router
