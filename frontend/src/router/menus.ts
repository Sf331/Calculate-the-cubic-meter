/** 菜单与路由的唯一来源。改这里，菜单和路由同时变。 */

export const ROLE_LABEL: Record<string, string> = {
  PRINCIPAL: '校长',
  ACADEMIC: '教务',
  TEACHER: '教师',
  STUDENT: '学生',
  PARENT: '家长'
}

export interface MenuItem {
  path: string
  title: string
  /** 可见角色。空数组表示所有角色可见。 */
  roles: string[]
  /** 已实现的模块指向真实页面；不填则用占位页 */
  component?: () => Promise<unknown>
}

export const MENUS: MenuItem[] = [
  {
    path: '/dashboard',
    title: '经营看板',
    roles: ['PRINCIPAL'],
    component: () => import('../views/dashboard/index.vue')
  },
  {
    path: '/schedule',
    title: '排课与课表',
    roles: [],
    component: () => import('../views/schedule/index.vue')
  },
  {
    path: '/students',
    title: '学员管理',
    roles: ['PRINCIPAL', 'ACADEMIC'],
    component: () => import('../views/students/index.vue')
  },
  {
    path: '/attendance',
    title: '签到管理',
    roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER'],
    component: () => import('../views/attendance/index.vue')
  },
  {
    path: '/salary',
    title: '工时薪酬',
    roles: ['PRINCIPAL', 'TEACHER'],
    component: () => import('../views/salary/index.vue')
  },
  {
    path: '/finance',
    title: '财会报表',
    roles: ['PRINCIPAL'],
    component: () => import('../views/finance/index.vue')
  },
  {
    path: '/basedata',
    title: '基础数据',
    roles: ['PRINCIPAL', 'ACADEMIC'],
    component: () => import('../views/basedata/index.vue')
  },
  {
    path: '/content',
    title: '内容托管',
    roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER'],
    component: () => import('../views/content/index.vue')
  },
  { path: '/courseware', title: '交互课件', roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER'] },
  { path: '/homework', title: '电子作业', roles: [] },
  { path: '/wrong-book', title: '错题本', roles: ['STUDENT'] },
  { path: '/report', title: '学情报告', roles: ['PRINCIPAL', 'STUDENT', 'PARENT'] },
  {
    path: '/lesson-account',
    title: '课时账户',
    roles: ['STUDENT', 'PARENT'],
    component: () => import('../views/lesson-account/index.vue')
  },
  { path: '/notice', title: '通知', roles: ['STUDENT', 'PARENT'] }
]

export const menusFor = (role: string) =>
  MENUS.filter((m) => m.roles.length === 0 || m.roles.includes(role))
