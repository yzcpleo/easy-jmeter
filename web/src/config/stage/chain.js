// 链路延时数据收集模块配置
export default {
  title: '链路延时监控',
  type: 'folder',
  name: Symbol('chain-monitor'),
  route: '/chain',
  filePath: 'view/chain/chain.vue',
  inNav: true, // 启用菜单显示
  icon: 'iconfont icon-clock',
  order: 7,
  permission: ['链路监控查看'],
  children: [
    {
      title: '实时监控',
      type: 'view',
      name: 'chain-monitoring',
      route: '/chain/monitoring',
      filePath: 'view/chain/chain-monitoring.vue',
      inNav: true,
      icon: 'iconfont icon-loading',
      order: 1,
      permission: ['链路监控查看']
    },
    {
      title: '链路管理',
      type: 'view',
      name: 'chain-list',
      route: '/chain/list',
      filePath: 'view/chain/chain-list.vue',
      inNav: true,
      icon: 'iconfont icon-tushuguanli',
      order: 2,
      permission: ['链路配置查看']
    },
    {
      title: '节点管理',
      type: 'view',
      name: 'chain-nodes',
      route: '/chain/nodes',
      filePath: 'view/chain/chain-nodes.vue',
      inNav: false, // 不在菜单显示，只能通过链路管理页面跳转
      icon: 'iconfont icon-erjituandui',
      order: 3,
      permission: ['链路配置查看']
    },
    {
      title: '数据收集',
      type: 'view',
      name: 'chain-collection',
      route: '/chain/collection',
      filePath: 'view/chain/chain-collection.vue',
      inNav: true,
      icon: 'iconfont icon-erjizhibiao',
      order: 4,
      permission: ['链路数据收集']
    },
    {
      title: '性能分析',
      type: 'view',
      name: 'chain-analysis',
      route: '/chain/analysis',
      filePath: 'view/chain/chain-analysis.vue',
      inNav: true,
      icon: 'iconfont icon-tushuguanli',
      order: 5,
      permission: ['链路数据查看']
    }
  ]
}