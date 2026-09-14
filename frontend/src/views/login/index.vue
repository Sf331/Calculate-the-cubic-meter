<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { menusFor } from '../../router/menus'

const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', password: '' })

/** 演示用的快捷登录，省得评委看演示时手敲账号 */
const QUICK_LOGINS = [
  { username: 'principal', label: '校长' },
  { username: 'academic', label: '教务' },
  { username: 'teacher01', label: '教师' },
  { username: 'student01', label: '学生' },
  { username: 'parent01', label: '家长' }
]

async function submit(username: string, password: string) {
  try {
    await userStore.signIn(username, password)
    // 每个角色能看的第一个菜单不一样，登完直接落到自己能看到的第一页
    const first = menusFor(userStore.role())[0]
    router.push(first ? first.path : '/')
  } catch {
    // 错误提示已由 api/request.ts 统一弹出
  }
}

async function quick(username: string) {
  form.value = { username, password: '123456' }
  await submit(username, '123456')
}
</script>

<template>
  <div class="page">
    <h2>课立方</h2>

    <el-form label-width="60px" @submit.prevent>
      <el-form-item label="账号">
        <el-input v-model="form.username" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="form.password" type="password" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="submit(form.username, form.password)">登录</el-button>
      </el-form-item>
    </el-form>

    <p>演示账号（密码均为 123456）：</p>
    <el-button v-for="q in QUICK_LOGINS" :key="q.username" class="quick" @click="quick(q.username)">
      {{ q.label }}
    </el-button>
  </div>
</template>

<style scoped>
.page {
  width: 320px;
  margin: 80px auto;
}

.quick {
  margin: 0 8px 8px 0;
}
</style>
