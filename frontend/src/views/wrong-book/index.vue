<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { homeworkApi } from '../../api/homework'

/** 后端错题本的返回行。api/homework 没导出这个类型，就近声明，避免用 any。 */
interface WrongQuestion {
  questionId: number
  knowledgePoint?: string
  wrongCount: number
  lastWrongTime?: string
}

const rows = ref<WrongQuestion[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    rows.value = await homeworkApi.wrongBook()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="k-page">
    <div class="k-card k-card--table">
      <div class="k-toolbar">
        <span class="k-muted">共 <span class="k-num">{{ rows.length }}</span> 道错题</span>
        <span class="k-spacer" />
      </div>

      <el-table :data="rows" v-loading="loading" stripe style="margin-top: var(--space-2)">
        <el-table-column prop="questionId" label="题目 ID" min-width="130" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.questionId }}</span></template>
        </el-table-column>
        <el-table-column prop="knowledgePoint" label="知识点" min-width="240" />
        <el-table-column prop="wrongCount" label="错误次数" min-width="140" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.wrongCount }}</span></template>
        </el-table-column>
        <el-table-column prop="lastWrongTime" label="最近错误" min-width="190">
          <template #default="{ row }"><span class="k-num">{{ row.lastWrongTime }}</span></template>
        </el-table-column>

        <template #empty>
          <div class="k-empty">
            <p class="k-empty__text">还没有错题，继续保持</p>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>
