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
  /** 手机端底部标签栏用的短名（标签栏必须有文字，且越短越好）。不填就用 title。 */
  short?: string
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
    short: '课表',
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
    short: '签到',
    roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER'],
    component: () => import('../views/attendance/index.vue')
  },
  {
    path: '/salary',
    title: '工时薪酬',
    short: '薪酬',
    roles: ['PRINCIPAL', 'TEACHER'],
    component: () => import('../views/salary/index.vue')
  },
  {
    path: '/finance',
    title: '财会报表',
    short: '财务',
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
    short: '内容',
    roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER'],
    component: () => import('../views/content/index.vue')
  },
  {
    path: '/courseware',
    title: '交互课件',
    short: '课件',
    roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER', 'STUDENT'],
    component: () => import('../views/courseware/index.vue')
  },
  {
    path: '/homework',
    title: '电子作业',
    short: '作业',
    roles: [],
    component: () => import('../views/homework/index.vue')
  },
  {
    path: '/wrong-book',
    title: '错题本',
    short: '错题',
    roles: ['STUDENT'],
    component: () => import('../views/wrong-book/index.vue')
  },
  {
    path: '/report',
    title: '学情报告',
    short: '学情',
    roles: ['PRINCIPAL', 'STUDENT', 'PARENT'],
    component: () => import('../views/report/index.vue')
  },
  {
    path: '/lesson-account',
    title: '课时账户',
    short: '课时',
    roles: ['STUDENT', 'PARENT'],
    component: () => import('../views/lesson-account/index.vue')
  },
  {
    path: '/notice',
    title: '通知',
    short: '通知',
    roles: ['STUDENT', 'PARENT'],
    component: () => import('../views/notice/index.vue')
  },
  {
    path: '/notice-publish',
    title: '发布通知',
    short: '通知',
    roles: ['PRINCIPAL', 'ACADEMIC', 'TEACHER'],
    component: () => import('../views/notice/manage.vue')
  }
]

export const menusFor = (role: string) =>
  MENUS.filter((m) => m.roles.length === 0 || m.roles.includes(role))

/**
 * 课表是所有人都能看的，但**改课表**是教务线的操作。
 * 排课、清空课表、调课这三件事只有校长和教务发起，教师/学生/家长只读。
 */
export const SCHEDULE_EDITOR_ROLES = ['PRINCIPAL', 'ACADEMIC']

export const canEditSchedule = (role: string) => SCHEDULE_EDITOR_ROLES.includes(role)
