<script setup lang="ts">
/**
 * 桌面端外壳：侧边栏 + 工具栏 + 内容区。
 * 页面标题由工具栏统一渲染（取当前路由的菜单名），页面里不再重复写 H2。
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { menusFor, ROLE_LABEL } from '../router/menus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => menusFor(userStore.role()))
const roleLabel = computed(() => ROLE_LABEL[userStore.role()] ?? '')
const currentTitle = computed(
  () => menus.value.find((m) => m.path === route.path)?.title ?? '课立方'
)
/** 头像占位用姓氏首字，不引入图片资源 */
const initial = computed(() => userStore.user?.realName?.slice(0, 1) ?? '课')

async function onLogout() {
  await userStore.signOut()
  router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand__mark" aria-hidden="true">课</div>
        <div class="brand__text">
          <div class="brand__name">课立方</div>
          <div class="brand__sub">机构运营台</div>
        </div>
      </div>

      <nav class="nav" aria-label="主导航">
        <router-link
          v-for="m in menus"
          :key="m.path"
          :to="m.path"
          class="nav__item"
          :class="{ 'is-active': route.path === m.path }"
          :aria-current="route.path === m.path ? 'page' : undefined"
        >
          {{ m.title }}
        </router-link>
      </nav>
    </aside>

    <div class="main">
      <header class="topbar">
        <h1 class="topbar__title">{{ currentTitle }}</h1>

        <div class="topbar__user">
          <div class="avatar" aria-hidden="true">{{ initial }}</div>
          <div class="who">
            <span class="who__name">{{ userStore.user?.realName }}</span>
            <span class="who__role">{{ roleLabel }}</span>
          </div>
          <el-button @click="onLogout">退出登录</el-button>
        </div>
      </header>

      <main class="content">
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  height: 100%;
}

/* ---------- 侧边栏 ---------- */
.sidebar {
  width: var(--sidebar-width);
  flex: none;
  display: flex;
  flex-direction: column;
  background: var(--bg-card);
  border-right: 1px solid var(--separator);
}

.brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  height: var(--header-height);
  flex: none;
  padding: 0 var(--space-2);
  border-bottom: 1px solid var(--separator);
}

.brand__mark {
  width: 28px;
  height: 28px;
  flex: none;
  border-radius: var(--radius-sm);
  background: var(--color-primary);
  color: var(--label-inverse);
  display: grid;
  place-items: center;
  font-size: var(--text-2xs);
  font-weight: 600;
}

.brand__name {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
}

.brand__sub {
  font-size: var(--text-xs);
  color: var(--label-secondary);
}

.nav {
  flex: 1;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: var(--space-1);
}

/* 38px 一行，14 个菜单项一屏放得下 —— 侧边栏是导航不是内容，不需要留白 */
.nav__item {
  display: flex;
  align-items: center;
  height: 38px;
  padding: 0 var(--space-2);
  border-radius: var(--radius-md);
  font-size: var(--text-xs);
  color: var(--label-primary);
  text-decoration: none;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.nav__item:hover {
  background: var(--bg-hover);
}

/* 选中态：**实心主色块 + 白字**，参考站就是这么做的。
   上一版是"主色淡底 + 主色文字"，怕 14 个菜单项里有一块实心蓝太抢眼 ——
   但正因为侧栏通体是白的，这一块实心蓝反而成了全站唯一明确的位置指示，
   比淡底清楚得多。文字用 --color-primary-on 这个 token，不写死白字。 */
.nav__item.is-active {
  background: var(--color-primary);
  color: var(--color-primary-on);
  font-weight: 500;
}

.nav__item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

/* ---------- 主区 ---------- */
.main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.topbar {
  flex: none;
  height: var(--header-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 0 var(--space-3);
  background: var(--bg-card);
  border-bottom: 1px solid var(--separator);
}

.topbar__title {
  font-size: var(--text-md);
  font-weight: 600;
  color: var(--label-primary);
}

.topbar__user {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.avatar {
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  background: var(--bg-subtle);
  color: var(--label-secondary);
  display: grid;
  place-items: center;
  font-size: var(--text-2xs);
  font-weight: 600;
}

.who {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.who__name {
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--label-primary);
}

.who__role {
  font-size: var(--text-2xs);
  color: var(--label-secondary);
}

.content {
  flex: 1;
  overflow-y: auto;
  background: var(--bg-canvas);
}

/* 窗口不够宽时侧栏收窄，再窄就折成顶部横向菜单 —— 演示时全屏用不到，但别让它撑破 */
@media (max-width: 1100px) {
  .sidebar {
    width: 186px;
  }
}

@media (max-width: 820px) {
  .shell {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
    border-right: none;
    border-bottom: 1px solid var(--separator);
  }

  .brand {
    display: none;
  }

  .nav {
    flex-direction: row;
    flex-wrap: wrap;
    padding: var(--space-2) var(--space-3);
  }

  .nav__item {
    height: var(--control-h);
    padding: 0 var(--space-3);
  }

  .who {
    display: none;
  }
}
</style>
