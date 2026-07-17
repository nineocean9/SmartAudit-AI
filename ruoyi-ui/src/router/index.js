import { createWebHistory, createRouter } from 'vue-router'
/* Layout */
import Layout from '@/layout'

/**
 * Note: 路由配置项
 *
 * hidden: true                     // 当设置 true 的时候该路由不会再侧边栏出现 如401，login等页面，或者如一些编辑页面/edit/1
 * alwaysShow: true                 // 当你一个路由下面的 children 声明的路由大于1个时，自动会变成嵌套的模式--如组件页面
 *                                  // 只有一个时，会将那个子路由当做根路由显示在侧边栏--如引导页面
 *                                  // 若你想不管路由下面的 children 声明的个数都显示你的根路由
 *                                  // 你可以设置 alwaysShow: true，这样它就会忽略之前定义的规则，一直显示根路由
 * redirect: noRedirect             // 当设置 noRedirect 的时候该路由在面包屑导航中不可被点击
 * name:'router-name'               // 设定路由的名字，一定要填写不然使用<keep-alive>时会出现各种问题
 * query: '{"id": 1, "name": "ry"}' // 访问路由的默认传递参数
 * roles: ['admin', 'common']       // 访问路由的角色权限
 * permissions: ['a:a:a', 'b:b:b']  // 访问路由的菜单权限
 * meta : {
    noCache: true                   // 如果设置为true，则不会被 <keep-alive> 缓存(默认 false)
    title: 'title'                  // 设置该路由在侧边栏和面包屑中展示的名字
    icon: 'svg-name'                // 设置该路由的图标，对应路径src/assets/icons/svg
    breadcrumb: false               // 如果设置为false，则不会在breadcrumb面包屑中显示
    activeMenu: '/system/user'      // 当路由设置了该属性，则会高亮相对应的侧边栏。
  }
 */

// 公共路由
export const constantRoutes = [
  {
    path: '/redirect',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '/redirect/:path(.*)',
        component: () => import('@/views/redirect/index.vue')
      }
    ]
  },
  {
    path: '/login',
    component: () => import('@/views/login'),
    hidden: true
  },
  {
    path: '/register',
    component: () => import('@/views/register'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/error/404'),
    hidden: true
  },
  {
    path: '/visualization/detail',
    component: Layout,
    hidden: true,
    permissions: ['audit:visual:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/visualization/detail'),
        name: 'VisualizationDetail',
        meta: { title: '数据可视化详情', activeMenu: '/audit-ai-support/visualization/index' }
      }
    ]
  },
  {
    path: '/401',
    component: () => import('@/views/error/401'),
    hidden: true
  },
  {
    path: '',
    component: Layout,
    redirect: '/index',
    children: [
      {
        path: '/index',
        component: () => import('@/views/index'),
        name: 'Index',
        meta: { title: '首页', icon: 'dashboard', affix: true }
      }
    ]
  },
  {
    path: '/lock',
    component: () => import('@/views/lock'),
    hidden: true,
    meta: { title: '锁定屏幕' }
  },
  {
    path: '/user',
    component: Layout,
    hidden: true,
    redirect: 'noredirect',
    children: [
      {
        path: 'profile/:activeTab?',
        component: () => import('@/views/system/user/profile/index'),
        name: 'Profile',
        meta: { title: '个人中心', icon: 'user' }
      }
    ]
  },
  {
    path: '/audit/issue',
    component: Layout,
    hidden: true,
    permissions: ['audit:issue:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/issue'),
        name: 'AuditIssueDirect',
        meta: { title: '审计问题管理', icon: 'bug', activeMenu: '/audit-execution/issue' }
      }
    ]
  },
  {
    path: '/audit/plan',
    component: Layout,
    hidden: true,
    permissions: ['audit:plan:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/plan'),
        name: 'AuditPlanDirect',
        meta: { title: '审计计划', icon: 'date', activeMenu: '/audit-planning/plan' }
      }
    ]
  },
  {
    path: '/audit/projectLib',
    component: Layout,
    hidden: true,
    permissions: ['audit:projectLib:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/projectLib'),
        name: 'AuditProjectLibDirect',
        meta: { title: '项目库', icon: 'documentation', activeMenu: '/audit-planning/projectLib' }
      }
    ]
  },
  {
    path: '/audit/excel-view',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/audit/excelView'),
        name: 'ExcelView',
        meta: { title: 'Excel 查看', icon: 'table' }
      }
    ]
  },
  {
    path: '/audit/doc-preview',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/audit/docPreview'),
        name: 'DocPreview',
        meta: { title: '文档预览', icon: 'document' }
      }
    ]
  },
  {
    path: '/audit/project',
    component: Layout,
    hidden: true,
    permissions: ['audit:project:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/project'),
        name: 'AuditProjectWorkbench',
        meta: { title: '项目工作台', icon: 'project', activeMenu: '/audit-planning/project-workspace' }
      }
    ]
  },
  {
    path: '/audit/progress',
    component: Layout,
    hidden: true,
    permissions: ['audit:progress:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/progress'),
        name: 'AuditProgress',
        meta: { title: '项目进度', icon: 'chart', activeMenu: '/audit-execution/progress' }
      }
    ]
  },
  {
    path: '/audit/report',
    component: Layout,
    hidden: true,
    permissions: ['audit:report:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/report'),
        name: 'AuditReportDirect',
        meta: { title: '审计报告', icon: 'document', activeMenu: '/audit-closeout/report' }
      }
    ]
  },
  {
    path: '/audit/workpaper',
    component: Layout,
    hidden: true,
    permissions: ['audit:workpaper:view'],
    roles: ['audit_project_leader', 'audit_staff', 'intermediary_auditor'],
    accessMode: 'any',
    children: [
      {
        path: '',
        component: () => import('@/views/audit/workpaper'),
        name: 'AuditWorkpaperDirect',
        meta: { title: '底稿管理', icon: 'form', activeMenu: '/audit-execution/workpaper' }
      }
    ]
  },
  {
    path: '/audit/rectification',
    component: Layout,
    hidden: true,
    permissions: ['audit:rectification:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/rectification'),
        name: 'AuditRectificationDirect',
        meta: { title: '整改跟踪', icon: 'edit', activeMenu: '/audit-closeout/rectification' }
      }
    ]
  },
  {
    path: '/audit/archive',
    component: Layout,
    hidden: true,
    permissions: ['audit:archive:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/archive'),
        name: 'AuditArchiveDirect',
        meta: { title: '项目归档', icon: 'folder', activeMenu: '/audit-closeout/archive' }
      }
    ]
  },
  {
    path: '/dashboard/index',
    component: Layout,
    hidden: true,
    permissions: ['audit:visual:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/dataDashboard'),
        name: 'DataDashboardDirect',
        meta: { title: '数据分析仪表盘', icon: 'dashboard', activeMenu: '/audit-ai-support/visualization/index' }
      }
    ]
  },
  {
    path: '/visualization/index',
    component: Layout,
    hidden: true,
    permissions: ['audit:visual:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/visualization/index'),
        name: 'VisualizationListDirect',
        meta: { title: '数据可视化', icon: 'chart', activeMenu: '/audit-ai-support/visualization/index' }
      }
    ]
  },
  {
    path: '/ai/chat',
    component: Layout,
    hidden: true,
    roles: ['school_leader', 'audit_director', 'audit_project_leader', 'audit_staff'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/chat'),
        name: 'AiChatDirect',
        meta: { title: 'AI 审计助手', icon: 'message', activeMenu: '/audit-ai-support/aiChat' }
      }
    ]
  },
  {
    path: '/ai/forensic',
    component: Layout,
    hidden: true,
    permissions: ['ai:forensic:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/forensic'),
        name: 'AiForensicDirect',
        meta: { title: 'AI 取证分析', icon: 'form', activeMenu: '/audit-execution/forensic' }
      }
    ]
  },
  {
    path: '/ai/basis',
    component: Layout,
    hidden: true,
    permissions: ['ai:basis:query', 'audit:basis:query'],
    children: [
      {
        path: '',
        component: () => import('@/views/ai/basis'),
        name: 'AiBasisDirect',
        meta: { title: '审计依据库', icon: 'documentation', activeMenu: '/audit-resource/basis' }
      }
    ]
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import('@/views/error/404'),
    hidden: true
  }
]
export const dynamicRoutes = [
  {
    path: '/system/user-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:user:edit'],
    children: [
      {
        path: 'role/:userId(\\d+)',
        component: () => import('@/views/system/user/authRole'),
        name: 'AuthRole',
        meta: { title: '分配角色', activeMenu: '/system/user' }
      }
    ]
  },
  {
    path: '/system/role-auth',
    component: Layout,
    hidden: true,
    permissions: ['system:role:edit'],
    children: [
      {
        path: 'user/:roleId(\\d+)',
        component: () => import('@/views/system/role/authUser'),
        name: 'AuthUser',
        meta: { title: '分配用户', activeMenu: '/system/role' }
      }
    ]
  },
  {
    path: '/system/dict-data',
    component: Layout,
    hidden: true,
    permissions: ['system:dict:list'],
    children: [
      {
        path: 'index/:dictId(\\d+)',
        component: () => import('@/views/system/dict/data'),
        name: 'Data',
        meta: { title: '字典数据', activeMenu: '/system/dict' }
      }
    ]
  },
  {
    path: '/monitor/job-log',
    component: Layout,
    hidden: true,
    permissions: ['monitor:job:list'],
    children: [
      {
        path: 'index/:jobId(\\d+)',
        component: () => import('@/views/monitor/job/log'),
        name: 'JobLog',
        meta: { title: '调度日志', activeMenu: '/monitor/job' }
      }
    ]
  },
  {
    path: '/tool/gen-edit',
    component: Layout,
    hidden: true,
    permissions: ['tool:gen:edit'],
    children: [
      {
        path: 'index/:tableId(\\d+)',
        component: () => import('@/views/tool/gen/editTable'),
        name: 'GenEdit',
        meta: { title: '修改生成配置', activeMenu: '/tool/gen' }
      }
    ]
  },
  {
    path: '/visualization',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'index',
        component: () => import('@/views/ai/visualization/index'),
        name: 'VisualizationList',
        meta: { title: '数据可视化' }
      },
      {
        path: 'detail',
        component: () => import('@/views/ai/visualization/detail'),
        name: 'VisualizationDetail',
        meta: { title: '数据可视化详情' }
      }
    ]
  },
  {
    path: '/dashboard',
    component: Layout,
    hidden: true,
    meta: { title: '数据分析仪表盘' },
    children: [
      {
        path: 'index',
        component: () => import('@/views/ai/dataDashboard'),
        name: 'DataDashboard',
        meta: { title: '数据分析仪表盘' }
      }
    ]
  },
  {
    path: '/ai',
    component: Layout,
    hidden: true,
    children: [
      {
        path: 'forensic',
        component: () => import('@/views/ai/forensic'),
        name: 'AiForensic',
        meta: { title: 'AI取证分析' }
      },
      {
        path: 'log',
        component: () => import('@/views/ai/aiLog'),
        name: 'AiLog',
        meta: { title: 'AI日志' }
      }
    ]
  },
  {
    path: '/audit/leader',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/audit/leader'),
        name: 'AuditLeader',
        meta: { title: '领导干部库' }
      }
    ]
  },
  {
    path: '/audit/scheme-template',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/audit/schemeTemplate'),
        name: 'SchemeTemplateStandalone',
        meta: { title: '方案模板库' }
      }
    ]
  },
  {
    path: '/audit/prepare',
    component: Layout,
    hidden: true,
    permissions: ['audit:prepare:view'],
    children: [
      {
        path: '',
        component: () => import('@/views/audit/prepare'),
        name: 'AuditPrepare',
        meta: { title: '审计准备', activeMenu: '/audit-prepare-flow/prepare' }
      }
    ]
  },
  {
    path: '/audit/archive',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/audit/archive'),
        name: 'AuditArchive',
        meta: { title: '项目归档' }
      }
    ]
  },
  {
    path: '/audit/temp-auth',
    component: Layout,
    hidden: true,
    children: [
      {
        path: '',
        component: () => import('@/views/audit/tempAuth'),
        name: 'TempAuth',
        meta: { title: '临时授权管理' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: constantRoutes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) {
      return savedPosition
    }
    return { top: 0 }
  },
})

export default router
