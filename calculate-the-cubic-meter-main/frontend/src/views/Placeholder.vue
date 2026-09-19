<script setup lang="ts">
/** 未实现模块的占位页。空态三件套：一个说明 + 一句补充 + 一个动作。 */
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { menusFor } from '../router/menus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function goDone() {
  const first = menusFor(userStore.role())[0]
  if (first) router.push(first.path)
}
</script>

<template>
  <div class="k-page">
    <div class="k-card">
      <div class="k-empty">
        <p class="k-empty__title">{{ route.meta.title }}尚未实现</p>
        <p class="k-empty__text">开发顺序见《编码计划书》第八节的批次计划。</p>
        <el-button type="primary" @click="goDone">回到已完成的模块</el-button>
      </div>
    </div>
  </div>
</template>
