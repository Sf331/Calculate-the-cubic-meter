<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { menusFor, ROLE_LABEL } from '../router/menus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => menusFor(userStore.role()))
const roleLabel = computed(() => ROLE_LABEL[userStore.role()] ?? '')

async function onLogout() {
  await userStore.signOut()
  router.push('/login')
}
</script>

<template>
  <el-container class="layout">
    <el-aside width="180px">
      <div class="brand">课立方</div>
      <el-menu :default-active="route.path" router>
        <el-menu-item v-for="m in menus" :key="m.path" :index="m.path">
          {{ m.title }}
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <span>{{ userStore.user?.realName }}（{{ roleLabel }}）</span>
        <el-button link type="primary" @click="onLogout">退出登录</el-button>
      </el-header>
      <el-main>
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout {
  height: 100%;
}

.brand {
  padding: 16px;
  font-size: 18px;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #dcdfe6;
}
</style>
