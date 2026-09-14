<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '../stores/user'
import DesktopLayout from './DesktopLayout.vue'
import MobileLayout from './MobileLayout.vue'

const userStore = useUserStore()

// 学生和家长用手机版布局，其余角色用桌面版。路由和菜单是同一套，只有外壳不同。
const useMobile = computed(() => ['STUDENT', 'PARENT'].includes(userStore.role()))
</script>

<template>
  <component :is="useMobile ? MobileLayout : DesktopLayout">
    <router-view />
  </component>
</template>
