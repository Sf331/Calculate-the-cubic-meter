<script setup lang="ts">
/**
 * 学生 / 家长端的通知页：只读列表，走手机端外壳（MobileLayout）。
 * 没有通知时就是一个「无通知」空态 —— 不放假数据。
 */
import { onMounted, ref } from 'vue'
import { noticeApi } from '../../api/notice'
import type { Notice } from '../../api/notice'

const notices = ref<Notice[]>([])
const loading = ref(false)

/** 后端给的是 ISO 时间（2026-09-21T10:30:00），页面上只显示到分钟 */
const timeText = (time?: string) => (time ? time.replace('T', ' ').slice(0, 16) : '')

onMounted(async () => {
  loading.value = true
  try {
    notices.value = await noticeApi.list()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="k-page">
    <div class="k-card" v-loading="loading">
      <div class="k-toolbar">
        <span class="k-muted">共 <span class="k-num">{{ notices.length }}</span> 条通知</span>
      </div>

      <div v-if="!notices.length" class="k-empty">
        <p class="k-empty__title">无通知</p>
        <p class="k-empty__text">老师发布通知后会显示在这里。</p>
      </div>

      <ul v-else class="notices">
        <li v-for="notice in notices" :key="notice.id" class="notice">
          <p class="notice__title">{{ notice.title }}</p>
          <p v-if="notice.content" class="notice__content">{{ notice.content }}</p>
          <p class="notice__meta k-muted">
            <span>{{ notice.publisherName }}</span>
            <span class="k-num">{{ timeText(notice.createdAt) }}</span>
          </p>
        </li>
      </ul>
    </div>
  </div>
</template>

<style scoped>
.notices {
  list-style: none;
  margin: var(--space-2) 0 0;
  padding: 0;
}

/* 一条通知一块，靠 1px 细线分隔，不给每条单独描边（卡片套卡片） */
.notice {
  padding: var(--space-3) 0;
  border-top: 1px solid var(--separator);
}

.notice:first-child {
  border-top: none;
  padding-top: 0;
}

.notice:last-child {
  padding-bottom: 0;
}

.notice__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
}

.notice__content {
  margin-top: var(--space-1);
  font-size: var(--text-sm);
  color: var(--label-primary);
  /* 发布时敲的换行照原样显示 */
  white-space: pre-wrap;
}

.notice__meta {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-top: var(--space-2);
}
</style>
