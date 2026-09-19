<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { homeworkApi } from '../../api/homework'

/** 后端学情报告的返回结构。api/homework 没导出这个类型，就近声明，避免用 any。 */
interface StudyReport {
  submitted?: number
  graded?: number
  score?: number
  wrongCount?: number
}

const report = ref<StudyReport>({})
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    report.value = await homeworkApi.report()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="k-page">
    <!-- 数字墙自己就是卡片，不要再套 .k-card —— 套上去就是两层白框叠在一起 -->
    <div class="k-kpi" v-loading="loading">
      <div class="k-kpi__item">
        <span class="k-kpi__label">已提交作业</span>
        <span class="k-kpi__value k-num">{{ report.submitted || 0 }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">已批改</span>
        <span class="k-kpi__value k-num">{{ report.graded || 0 }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">累计得分</span>
        <span class="k-kpi__value k-num">{{ report.score || 0 }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">错题数</span>
        <span class="k-kpi__value k-num">{{ report.wrongCount || 0 }}</span>
      </div>
    </div>
  </div>
</template>
