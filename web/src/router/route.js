import homeRouter from './home-router'

const routes = [
  {
    path: '/',
    name: 'Home',
    redirect: '/about',
    component: () => import('@/view/home/home'),
    children: [
      ...homeRouter,
      // JMX Builder route (manually added for reliability)
      {
        path: '/case/jmx-builder/:id?',
        name: 'jmx-builder',
        component: () => import('@/view/case/jmx-builder.vue'),
        meta: {
          title: 'JMX Builder',
          icon: 'iconfont icon-edit',
          permission: ['用例管理'],
          type: 'view',
          keepAlive: false,
        }
      }
    ],
  },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/view/login/login'),
  },
  {
    redirect: '/404',
    path: '/:pathMatch(.*)',
  },
]

export default routes
