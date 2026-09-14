import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import AppLayout from '../layouts/AppLayout.vue'
import Placeholder from '../views/Placeholder.vue'
import { MENUS } from './menus'
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
      component: Placeholder,
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
  return true
})

export default router
