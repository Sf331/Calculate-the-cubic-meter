<script setup lang="ts">
/**
 * 手机端外壳（学生 / 家长）：导航栏 + 内容 + 底部标签栏。
 *
 * 这是全项目唯一一处"把操作收进弹层"的地方（风格里一般禁止）——
 * 手机上 5 个以上菜单横排会挤成一个个点按不准的窄条，收进「更多」是唯一解法。
 * 标签栏只放最高频的 4 项，且必须有文字标签，不画图标。
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../stores/user'
import { menusFor } from '../router/menus'

/** HIG：标签栏最多 5 项，「更多」占一格，所以前面留 4 格 */
const MAX_TABS = 4

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const menus = computed(() => menusFor(userStore.role()))
const tabs = computed(() => menus.value.slice(0, MAX_TABS))
const overflow = computed(() => menus.value.slice(MAX_TABS))

const moreOpen = ref(false)
const moreActive = computed(() => overflow.value.some((m) => m.path === route.path))
const currentTitle = computed(
  () => menus.value.find((m) => m.path === route.path)?.title ?? '课立方'
)

function go(path: string) {
  moreOpen.value = false
  router.push(path)
}

async function onLogout() {
  await userStore.signOut()
  router.push('/login')
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') moreOpen.value = false
}

onMounted(() => window.addEventListener('keydown', onKeydown))
onUnmounted(() => window.removeEventListener('keydown', onKeydown))
</script>

<template>
  <div class="mobile">
    <header class="navbar">
      <h1 class="navbar__title">{{ currentTitle }}</h1>
      <button class="navbar__action" type="button" @click="onLogout">退出</button>
    </header>

    <main class="body">
      <slot />
    </main>

    <nav class="tabbar" aria-label="主导航">
      <button
        v-for="m in tabs"
        :key="m.path"
        type="button"
        class="tab"
        :class="{ 'is-active': route.path === m.path }"
        :aria-current="route.path === m.path ? 'page' : undefined"
        @click="go(m.path)"
      >
        {{ m.short ?? m.title }}
      </button>

      <button
        v-if="overflow.length"
        type="button"
        class="tab"
        :class="{ 'is-active': moreActive }"
        @click="moreOpen = true"
      >
        更多
      </button>
    </nav>

    <!-- 底部弹层：抓手 + 列表项，遮罩统一 --mask -->
    <Teleport to="body">
      <div v-if="moreOpen" class="sheet-mask" @click.self="moreOpen = false">
        <div class="sheet" role="dialog" aria-modal="true" aria-label="更多菜单">
          <div class="sheet__grabber" aria-hidden="true"></div>
          <button
            v-for="m in overflow"
            :key="m.path"
            type="button"
            class="sheet__item"
            :class="{ 'is-active': route.path === m.path }"
            @click="go(m.path)"
          >
            <span>{{ m.title }}</span>
            <span class="sheet__chevron" aria-hidden="true">›</span>
          </button>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.mobile {
  height: 100%;
  display: flex;
  flex-direction: column;
  background: var(--bg-canvas);
}

/* ---------- 导航栏 ---------- */
.navbar {
  flex: none;
  min-height: var(--navbar-height);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding: 0 var(--space-3);
  padding-top: env(safe-area-inset-top, 0px);
  background: var(--bg-card);
  border-bottom: 1px solid var(--separator);
}

.navbar__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
}

.navbar__action {
  min-height: var(--touch-min);
  padding: 0 var(--space-2);
  border: none;
  background: none;
  font-family: inherit;
  font-size: var(--text-sm);
  color: var(--color-primary);
  cursor: pointer;
}

/* ---------- 内容 ---------- */
.body {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
}

/* ---------- 标签栏 ---------- */
.tabbar {
  flex: none;
  display: flex;
  align-items: stretch;
  min-height: var(--tabbar-height);
  padding-bottom: env(safe-area-inset-bottom, 0px);
  background: var(--bg-card);
  border-top: 1px solid var(--separator);
}

.tab {
  flex: 1 1 0;
  min-height: var(--touch-min);
  display: grid;
  place-items: center;
  border: none;
  background: none;
  font-family: inherit;
  font-size: var(--text-xs);
  color: var(--label-secondary);
  cursor: pointer;
  transition: color var(--duration-fast) var(--ease-out);
}

.tab.is-active {
  color: var(--color-primary);
  font-weight: 600;
}

.tab:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -3px;
}

/* ---------- 更多弹层 ---------- */
.sheet-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: flex-end;
  background: var(--mask);
}

.sheet {
  width: 100%;
  background: var(--bg-card);
  border-radius: var(--radius-lg) var(--radius-lg) 0 0;
  padding: var(--space-2) var(--space-3)
    calc(var(--space-3) + env(safe-area-inset-bottom, 0px));
  box-shadow: var(--shadow-pop);
  animation: sheet-up var(--duration-normal) var(--ease-out);
}

@keyframes sheet-up {
  from {
    transform: translateY(100%);
  }
  to {
    transform: translateY(0);
  }
}

/* 抓手 36×5，居中 */
.sheet__grabber {
  width: 36px;
  height: 5px;
  margin: var(--space-1) auto var(--space-3);
  border-radius: var(--radius-full);
  background: var(--label-tertiary);
}

.sheet__item {
  width: 100%;
  min-height: var(--touch-min);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 var(--space-2);
  border: none;
  border-bottom: 1px solid var(--separator);
  background: none;
  font-family: inherit;
  font-size: var(--text-sm);
  color: var(--label-primary);
  text-align: left;
  cursor: pointer;
}

.sheet__item:last-child {
  border-bottom: none;
}

.sheet__item.is-active {
  color: var(--color-primary);
  font-weight: 600;
}

.sheet__chevron {
  color: var(--label-tertiary);
  font-size: var(--text-sm);
}
</style>
