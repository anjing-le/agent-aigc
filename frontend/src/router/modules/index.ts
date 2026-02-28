import { AppRouteRecord } from '@/types/router'
import { dashboardRoutes } from './dashboard'
import { systemRoutes } from './system'
import { resultRoutes } from './result'
import { exceptionRoutes } from './exception'
import { themeRoutes } from './theme'
import { aigcRoutes } from './aigc'

/**
 * 导出所有模块化路由
 */
export const routeModules: AppRouteRecord[] = [
  aigcRoutes,
  dashboardRoutes,
  systemRoutes,
  resultRoutes,
  exceptionRoutes,
  themeRoutes
]
