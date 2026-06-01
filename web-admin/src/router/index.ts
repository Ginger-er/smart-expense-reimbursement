import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import MainLayout from '@/layout/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: MainLayout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'reimbursement',
        name: 'ReimbursementList',
        component: () => import('@/views/reimbursement/List.vue'),
        meta: { title: '报销管理' }
      },
      {
        path: 'reimbursement/create',
        name: 'ReimbursementCreate',
        component: () => import('@/views/reimbursement/Create.vue'),
        meta: { title: '创建报销单' }
      },
      {
        path: 'reimbursement/detail/:id',
        name: 'ReimbursementDetail',
        component: () => import('@/views/reimbursement/Detail.vue'),
        meta: { title: '报销单详情' }
      },
      {
        path: 'trip',
        name: 'TripList',
        component: () => import('@/views/trip/List.vue'),
        meta: { title: '出差申请' }
      },
      {
        path: 'trip/create',
        name: 'TripCreate',
        component: () => import('@/views/trip/Create.vue'),
        meta: { title: '创建出差申请' }
      },
      {
        path: 'trip/detail/:id',
        name: 'TripDetail',
        component: () => import('@/views/trip/Detail.vue'),
        meta: { title: '出差详情' }
      },
      {
        path: 'invoice',
        name: 'InvoiceList',
        component: () => import('@/views/invoice/List.vue'),
        meta: { title: '发票管理' }
      },
      {
        path: 'invoice/upload',
        name: 'InvoiceUpload',
        component: () => import('@/views/invoice/Upload.vue'),
        meta: { title: '发票上传' }
      },
      {
        path: 'approval',
        name: 'ApprovalList',
        component: () => import('@/views/approval/List.vue'),
        meta: { title: '审批中心' }
      },
      {
        path: 'report',
        name: 'Report',
        component: () => import('@/views/report/Index.vue'),
        meta: { title: '数据报表' }
      },
      {
        path: 'system/user',
        name: 'SystemUser',
        component: () => import('@/views/system/User.vue'),
        meta: { title: '用户管理' }
      },
      {
        path: 'system/dept',
        name: 'SystemDept',
        component: () => import('@/views/system/Dept.vue'),
        meta: { title: '部门管理' }
      },
      {
        path: 'system/operlog',
        name: 'SystemOperLog',
        component: () => import('@/views/system/OperLog.vue'),
        meta: { title: '操作日志' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：未登录跳转 /login；按角色拦截无权限页面
// 角色从 localStorage 读取（登录时写入），避免导航期间调用 store 造成循环依赖/二次导航
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem('token')
  if (to.path === '/login') {
    next()
    return
  }
  if (!token) {
    next('/login')
    return
  }

  const role = Number(localStorage.getItem('role') || 1)

  // 角色权限：审批中心需领导/财务/管理员，系统管理需管理员
  const guards: { prefix: string; roles: number[] }[] = [
    { prefix: '/approval', roles: [2, 3, 4] },
    { prefix: '/system', roles: [4] }
  ]
  for (const g of guards) {
    if (to.path.startsWith(g.prefix) && !g.roles.includes(role)) {
      next('/dashboard')
      return
    }
  }
  next()
})

export default router
