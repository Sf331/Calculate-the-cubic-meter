import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as authApi from '../api/auth'
import type { CurrentUser } from '../api/auth'

export const useUserStore = defineStore('user', () => {
  const user = ref<CurrentUser | null>(null)

  async function signIn(username: string, password: string) {
    user.value = await authApi.login(username, password)
  }

  async function signOut() {
    await authApi.logout()
    user.value = null
  }

  /** 刷新页面后从 session 恢复登录态，失败就当作未登录。 */
  async function restore() {
    try {
      user.value = await authApi.fetchMe()
    } catch {
      user.value = null
    }
  }

  const role = () => user.value?.role ?? ''

  return { user, signIn, signOut, restore, role }
})
