<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { financeApi, FUND_TYPE_LABEL } from '../../api/finance'
import type {
  ClassProfitRow,
  FundTransaction,
  LessonCarryRow,
  PreReceiveRow,
  TeacherCostRow
} from '../../api/finance'

const thisMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const period = ref(thisMonth())
const tab = ref('transaction')
const loading = ref(false)

const transactions = ref<FundTransaction[]>([])
const preReceive = ref<PreReceiveRow[]>([])
const lessonCarry = ref<LessonCarryRow[]>([])
const teacherCost = ref<TeacherCostRow[]>([])
const classProfit = ref<ClassProfitRow[]>([])

async function load() {
  loading.value = true
  try {
    transactions.value = await financeApi.transactions({})
    preReceive.value = await financeApi.preReceive(period.value)
    lessonCarry.value = await financeApi.lessonCarry(period.value)
    teacherCost.value = await financeApi.teacherCost(period.value)
    classProfit.value = await financeApi.classProfit(period.value)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <h3>财会报表</h3>

    <el-form inline>
      <el-form-item label="月份">
        <el-date-picker
          v-model="period"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          @change="load"
        />
      </el-form-item>
    </el-form>

    <el-tabs v-model="tab" v-loading="loading">
      <el-tab-pane label="收支明细" name="transaction">
        <el-table :data="transactions" border max-height="560">
          <el-table-column prop="occurDate" label="日期" width="120" />
          <el-table-column prop="studentName" label="学生" width="110" />
          <el-table-column label="类型" width="110">
            <template #default="{ row }">{{ FUND_TYPE_LABEL[row.type] ?? row.type }}</template>
          </el-table-column>
          <el-table-column label="方向" width="70">
            <template #default="{ row }">{{ row.direction === 'IN' ? '收' : '支' }}</template>
          </el-table-column>
          <el-table-column prop="amount" label="金额" width="110" />
          <el-table-column prop="remark" label="说明" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="预收台账" name="preReceive">
        <el-table :data="preReceive" border max-height="560">
          <el-table-column prop="studentName" label="学生" width="120" />
          <el-table-column prop="received" label="累计预收" width="130" />
          <el-table-column prop="confirmed" label="已确认收入" width="130" />
          <el-table-column prop="balance" label="预收余额" width="130" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="课时结转" name="lessonCarry">
        <el-table :data="lessonCarry" border max-height="560">
          <el-table-column prop="studentName" label="学生" width="110" />
          <el-table-column prop="courseName" label="课程" />
          <el-table-column prop="opening" label="期初" width="90" />
          <el-table-column prop="recharged" label="充值" width="90" />
          <el-table-column prop="consumed" label="消耗" width="90" />
          <el-table-column prop="closing" label="期末" width="90" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="教师成本" name="teacherCost">
        <el-table :data="teacherCost" border max-height="560">
          <el-table-column prop="teacherName" label="教师" width="130" />
          <el-table-column prop="lessonCount" label="课时数" width="100" />
          <el-table-column prop="studentCount" label="出勤人次" width="110" />
          <el-table-column prop="amount" label="成本金额" width="130" />
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="班级毛利" name="classProfit">
        <el-table :data="classProfit" border max-height="560">
          <el-table-column prop="className" label="班级" width="150" />
          <el-table-column prop="courseName" label="课程" />
          <el-table-column prop="revenue" label="确认收入" width="120" />
          <el-table-column prop="cost" label="教师成本" width="120" />
          <el-table-column prop="profit" label="毛利" width="120" />
        </el-table>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
