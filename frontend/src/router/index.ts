import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import Placeholder from '../views/Placeholder.vue'
import { MENUS, menusFor } from './menus'
import { useUserStore } from '../stores/user'

const routes: RouteRecordRaw[] = [
  { path: '/login', name: 'login', component: () => import('../views/login/index.vue') },
  {
    path: '/',
    component: AppLayout,
    redirect: '/dashboard',
    children: MENUS.map((m) => ({
      path: m.path.slice(1),
      name: m.path,
      // 菜单里配了 component 就用真实页面，否则落到占位页
      component: m.component ?? Placeholder,
      meta: { title: m.title }
    }))
  },
  { path: '/:pathMatch(.*)*', component: () => import('../views/NotFound.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  if (to.path === '/login') return true

  const userStore = useUserStore()
  if (!userStore.user) {
    await userStore.restore()
  }
  if (!userStore.user) {
    return { path: '/login' }
  }

  // 默认落点是经营看板，但那是校长专属。别的角色进来会被后端 403，
  // 所以在这里换到他菜单里的第一项。
  const menus = menusFor(userStore.role())
  if (to.path === '/dashboard' && !menus.some((m) => m.path === '/dashboard')) {
    return menus.length ? { path: menus[0].path } : false
  }
  return true
})

export default router
