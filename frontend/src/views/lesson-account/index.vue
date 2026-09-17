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
  <div>
    <h3>课时账户</h3>

    <el-table :data="accounts" v-loading="loading" border>
      <el-table-column prop="studentName" label="学生" width="110" />
      <el-table-column prop="courseName" label="课程" />
      <el-table-column prop="totalHours" label="已购课时" width="100" />
      <el-table-column prop="consumedHours" label="已消耗" width="100" />
      <el-table-column prop="remainingHours" label="剩余课时" width="100" />
      <el-table-column prop="unitPrice" label="课时单价" width="100" />
      <el-table-column label="操作" width="100">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">流水</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="课时流水" width="680px">
      <p v-if="current">
        {{ current.studentName }} · {{ current.courseName }} · 剩余 {{ current.remainingHours }} 课时
      </p>

      <el-table :data="transactions" border max-height="400">
        <el-table-column label="类型" width="90">
          <template #default="{ row }">{{ TRANSACTION_LABEL[row.type] ?? row.type }}</template>
        </el-table-column>
        <el-table-column prop="hours" label="变动课时" width="100" />
        <el-table-column prop="balanceAfter" label="变动后余额" width="110" />
        <el-table-column prop="remark" label="说明" />
      </el-table>

      <template #footer>
        <el-button type="primary" @click="dialogVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>
