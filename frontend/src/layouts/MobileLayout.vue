<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { menusFor } from '../router/menus'

const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => menusFor(userStore.role()))

async function onLogout() {
  await userStore.signOut()
  router.push('/login')
}
</script>

<template>
  <div class="mobile">
    <div class="bar">
      <span>{{ userStore.user?.realName }}</span>
      <el-button link type="primary" @click="onLogout">退出</el-button>
    </div>

    <!-- 手机端用平铺入口代替侧边菜单 -->
    <div class="nav">
      <el-button
        v-for="m in menus"
        :key="m.path"
        class="nav-item"
        @click="router.push(m.path)"
      >
        {{ m.title }}
      </el-button>
    </div>

    <div class="body">
      <slot />
    </div>
  </div>
</template>

<style scoped>
.mobile {
  height: 100%;
}

.bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  border-bottom: 1px solid #dcdfe6;
}

.nav {
  padding: 8px;
}

.nav-item {
  margin: 0 8px 8px 0;
}

.body {
  padding: 8px 12px;
}
</style>
