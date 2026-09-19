<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { menusFor } from '../../router/menus'

const router = useRouter()
const userStore = useUserStore()

const form = ref({ username: '', password: '' })
const loading = ref(false)

/** 演示用的快捷登录，省得评委看演示时手敲账号 */
const QUICK_LOGINS = [
  { username: 'principal', label: '校长' },
  { username: 'academic', label: '教务' },
  { username: 'teacher01', label: '教师' },
  { username: 'student01', label: '学生' },
  { username: 'parent01', label: '家长' }
]

async function submit(username: string, password: string) {
  if (!username || !password) return
  loading.value = true
  try {
    await userStore.signIn(username, password)
    // 每个角色能看的第一个菜单不一样，登完直接落到自己能看到的第一页
    const first = menusFor(userStore.role())[0]
    router.push(first ? first.path : '/')
  } catch {
    // 错误提示已由 api/request.ts 统一弹出
  } finally {
    loading.value = false
  }
}

async function quick(username: string) {
  form.value = { username, password: '123456' }
  await submit(username, '123456')
}
</script>

<template>
  <div class="login">
    <div class="login__card">
      <div class="login__brand">
        <div class="login__mark" aria-hidden="true">课</div>
        <h1 class="login__title">课立方</h1>
        <p class="login__desc">面向中小型辅导机构的一站式智能运营 SaaS</p>
      </div>

      <form class="login__form" @submit.prevent="submit(form.username, form.password)">
        <div class="field">
          <span class="field__label">账号</span>
          <el-input
            v-model="form.username"
            size="large"
            placeholder="请输入账号"
            aria-label="账号"
          />
        </div>

        <div class="field">
          <span class="field__label">密码</span>
          <el-input
            v-model="form.password"
            type="password"
            size="large"
            show-password
            placeholder="请输入密码"
            aria-label="密码"
          />
        </div>

        <el-button
          class="login__submit"
          type="primary"
          size="large"
          native-type="submit"
          :loading="loading"
        >
          登录
        </el-button>
      </form>

      <div class="login__quick">
        <p class="login__hint">演示账号（密码均为 123456），点一下直接进</p>
        <div class="login__roles">
          <button
            v-for="q in QUICK_LOGINS"
            :key="q.username"
            type="button"
            class="role"
            :title="q.username"
            :aria-label="`以${q.label}身份登录（${q.username}）`"
            @click="quick(q.username)"
          >
            {{ q.label }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login {
  min-height: 100%;
  display: grid;
  place-items: center;
  padding: var(--space-4);
  background: var(--bg-canvas);
}

.login__card {
  width: 100%;
  max-width: 440px;
  background: var(--bg-card);
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
  padding: var(--space-4);
}

.login__brand {
  margin-bottom: var(--space-4);
}

.login__mark {
  width: 32px;
  height: 32px;
  margin-bottom: var(--space-2);
  border-radius: var(--radius-md);
  background: var(--color-primary);
  color: var(--label-inverse);
  display: grid;
  place-items: center;
  font-size: var(--text-sm);
  font-weight: 600;
}

.login__title {
  font-size: var(--text-lg);
  font-weight: 600;
  color: var(--label-primary);
}

.login__desc {
  margin-top: var(--space-1);
  font-size: var(--text-xs);
  color: var(--label-secondary);
}

.login__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.field {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.field__label {
  font-size: var(--text-xs);
  color: var(--label-secondary);
}

.login__submit {
  width: 100%;
  margin-top: var(--space-2);
}

.login__quick {
  margin-top: var(--space-4);
  padding-top: var(--space-3);
  border-top: 1px solid var(--separator);
}

.login__hint {
  font-size: var(--text-xs);
  color: var(--label-secondary);
  margin-bottom: var(--space-2);
}

/* 固定 5 等分，正好一行放下 5 个角色；用 flex-wrap 会变成 4+1，最后一个被拉满整行 */
.login__roles {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: var(--space-1);
}

/* 演示入口，用中性描边按钮，不跟上面的主操作抢蓝色 */
.role {
  height: var(--control-h);
  padding: 0;
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--label-primary);
  font-family: inherit;
  font-size: var(--text-xs);
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.role:hover {
  background: var(--bg-hover);
  border-color: var(--separator-strong);
}

.role:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 1px;
}

/* 手机端：卡片铺满，去掉圆角和描边 */
@media (max-width: 520px) {
  .login {
    padding: 0;
    place-items: stretch;
  }

  .login__card {
    max-width: none;
    border: none;
    border-radius: 0;
    padding: calc(var(--space-5) + env(safe-area-inset-top, 0px)) var(--space-4)
      var(--space-4);
  }
}
</style>
