const caseRouter = {
    route: null,
    name: null,
    title: '用例管理',
    type: 'folder', // 类型: folder, tab, view
    icon: 'iconfont icon-caseStore',
    order: 2,
    inNav: true,
    children: [
      {
        route: '/case/list',
        name: 'case-list',
        title: '用例列表',
        type: 'view',
        icon: 'iconfont icon-list',
        filePath: 'view/case/case-list.vue',
        inNav: true,
        permission: ['用例管理'],
        keepAlive: true,
      },
      {
        route: '/case/jmx-builder/:id?',
        name: 'jmx-builder',
        title: 'JMX Builder',
        type: 'view',
        icon: 'iconfont icon-edit',
        filePath: 'view/case/jmx-builder.vue',
        inNav: false, // 不在导航中显示
        permission: ['用例管理'],
        keepAlive: false,
      },
      {
        route: '/case/jmx-assets',
        name: 'jmx-asset-list',
        title: 'JMX资产管理',
        type: 'view',
        icon: 'iconfont icon-caseStore',
        filePath: 'view/case/jmx-asset-list.vue',
        inNav: true,
        permission: ['用例管理'],
        keepAlive: false,
      },
    ],
  }
  
  export default caseRouter
  