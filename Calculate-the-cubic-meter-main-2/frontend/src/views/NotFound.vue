<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { menusFor } from '../router/menus'

const router = useRouter()
const userStore = useUserStore()

function goHome() {
  const first = menusFor(userStore.role())[0]
  router.push(first ? first.path : '/login')
}
</script>

<template>
  <div class="k-page">
    <div class="k-card">
      <div class="k-empty">
        <p class="k-empty__title">页面不存在</p>
        <p class="k-empty__text">地址可能敲错了，或者这个模块还没做。</p>
        <el-button type="primary" @click="goHome">回到首页</el-button>
      </div>
    </div>
  </div>
</template>
