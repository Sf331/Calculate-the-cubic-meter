<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { lessonAccountApi, TRANSACTION_LABEL } from '../../api/attendance'
import type { LessonAccountView, LessonTransaction } from '../../api/attendance'

const accounts = ref<LessonAccountView[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const current = ref<LessonAccountView>()
const transactions = ref<LessonTransaction[]>([])

async function load() {
  loading.value = true
  try {
    accounts.value = await lessonAccountApi.list()
  } finally {
    loading.value = false
  }
}

async function openDetail(account: LessonAccountView) {
  current.value = account
  transactions.value = await lessonAccountApi.transactions(account.id)
  dialogVisible.value = true
}

onMounted(load)
</script>

<template>
  <div class="k-page">
    <div class="k-card k-card--table">
      <div class="k-toolbar">
        <span class="k-muted">共 <span class="k-num">{{ accounts.length }}</span> 个账户</span>
        <span class="k-spacer" />
      </div>

      <el-table :data="accounts" v-loading="loading" stripe style="margin-top: var(--space-2)">
        <el-table-column prop="studentName" label="学生" min-width="150" />
        <el-table-column prop="courseName" label="课程" min-width="240" />
        <el-table-column prop="totalHours" label="已购课时" min-width="140" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.totalHours }}</span></template>
        </el-table-column>
        <el-table-column prop="consumedHours" label="已消耗" min-width="130" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.consumedHours }}</span></template>
        </el-table-column>
        <el-table-column prop="remainingHours" label="剩余课时" min-width="140" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.remainingHours }}</span></template>
        </el-table-column>
        <el-table-column prop="unitPrice" label="课时单价" min-width="140" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.unitPrice }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="130" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">流水</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="k-empty">
            <p class="k-empty__text">还没有课时账户</p>
          </div>
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="课时流水" width="620px">
      <p v-if="current" class="dialog-note">
        {{ current.studentName }} · {{ current.courseName }} · 剩余
        <strong class="k-num">{{ current.remainingHours }}</strong> 课时
      </p>

      <el-table :data="transactions" stripe max-height="400">
        <el-table-column label="类型" min-width="130">
          <template #default="{ row }">{{ TRANSACTION_LABEL[row.type] ?? row.type }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="变动课时" min-width="140" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.hours }}</span></template>
        </el-table-column>
        <el-table-column prop="balanceAfter" label="变动后余额" min-width="150" align="right">
          <template #default="{ row }"><span class="k-num">{{ row.balanceAfter }}</span></template>
        </el-table-column>
        <el-table-column prop="remark" label="说明" min-width="200" />
      </el-table>

      <template #footer>
        <el-button type="primary" @click="dialogVisible = false">完成</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dialog-note {
  font-size: var(--text-sm);
  color: var(--label-primary);
  margin-bottom: var(--space-2);
}
</style>
