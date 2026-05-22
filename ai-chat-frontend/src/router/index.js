import { createRouter, createWebHashHistory } from 'vue-router'

// 路由配置
const routes = [
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('../views/Dashboard.vue')
  },
  {
    // 登录页
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue')
  },
  {
    // 注册页
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue')
  },
  {
    // 聊天页（需要登录）
    path: '/chat',
    name: 'Chat',
    component: () => import('../views/Chat.vue')
  },
  {
    // 历史记录页（需要登录）
    path: '/history',
    name: 'History',
    component: () => import('../views/History.vue')
  },
  {
    // 知识库（需要登录）
    path: '/knowledge',
    name: 'Knowledge',
    component: () => import('../views/Knowledge.vue')
  },
  {
    path: '/daily-review',
    name: 'DailyReview',
    component: () => import('../views/DailyReview.vue')
  },
  {
    path: '/learning',
    name: 'Learning',
    component: () => import('../views/Learning.vue')
  },
  {
    path: '/expression',
    name: 'Expression',
    component: () => import('../views/Expression.vue')
  },
  {
    path: '/project',
    name: 'Project',
    component: () => import('../views/Project.vue')
  }
]

const router = createRouter({
  // 使用 hash 模式（兼容性好，不需要服务端配置）
  history: createWebHashHistory(),
  routes
})

// 需要登录才能访问的路由列表
const authRoutes = ['/chat', '/history', '/knowledge', '/daily-review', '/learning', '/expression', '/project', '/dashboard']

// 路由守卫：检查登录状态
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')

  // 如果访问需要登录的页面但没有登录，跳转到登录页
  if (authRoutes.includes(to.path) && !token) {
    next('/login')
  } else {
    next()
  }
})

export default router
