import type { AppRouteRecord } from '@/types/router'

/**
 * AIGC 创作工坊路由模块
 */
export const aigcRoutes: AppRouteRecord = {
  path: '/aigc',
  name: 'AIGC',
  component: '/index/index',
  redirect: '/aigc/studio',
  meta: {
    title: 'AIGC创作工坊',
    icon: 'ri:magic-line',
    order: 1
  },
  children: [
    {
      path: 'studio',
      name: 'AIGCStudio',
      component: '/aigc/studio',
      meta: {
        title: '创作工作台',
        icon: 'ri:edit-line',
        keepAlive: true
      }
    },
    {
      path: 'gallery',
      name: 'AIGCGallery',
      component: '/aigc/gallery',
      meta: {
        title: '灵感广场',
        icon: 'ri:gallery-line',
        keepAlive: true
      }
    },
    {
      path: 'assets',
      name: 'AIGCAssets',
      component: '/aigc/assets',
      meta: {
        title: '我的资产',
        icon: 'ri:folder-5-line',
        keepAlive: true
      }
    }
  ]
}

export default aigcRoutes

