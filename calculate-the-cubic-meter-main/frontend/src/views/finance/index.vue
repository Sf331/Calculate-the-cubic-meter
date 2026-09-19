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
  <div class="k-page">
    <div class="k-card">
      <div class="k-toolbar">
        <span class="k-muted">月份</span>
        <el-date-picker
          v-model="period"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          aria-label="月份"
          @change="load"
        />
        <span class="k-spacer" />
      </div>
    </div>

    <div class="k-card" v-loading="loading">
      <el-tabs v-model="tab">
        <el-tab-pane label="收支明细" name="transaction">
          <el-table :data="transactions" stripe max-height="560">
            <el-table-column prop="occurDate" label="日期" min-width="150">
              <template #default="{ row }"><span class="k-num">{{ row.occurDate }}</span></template>
            </el-table-column>
            <el-table-column prop="studentName" label="学生" min-width="140" />
            <el-table-column label="类型" min-width="140">
              <template #default="{ row }">{{ FUND_TYPE_LABEL[row.type] ?? row.type }}</template>
            </el-table-column>
            <el-table-column label="方向" min-width="100">
              <template #default="{ row }">
                <span class="k-tag" :class="row.direction === 'IN' ? 'k-tag--success' : 'k-tag--neutral'">
                  {{ row.direction === 'IN' ? '收' : '支' }}
                </span>
              </template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" min-width="140" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.amount }}</span></template>
            </el-table-column>
            <el-table-column prop="remark" label="说明" min-width="220" />
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="预收台账" name="preReceive">
          <el-table :data="preReceive" stripe max-height="560">
            <el-table-column prop="studentName" label="学生" min-width="150" />
            <el-table-column prop="received" label="累计预收" min-width="160" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.received }}</span></template>
            </el-table-column>
            <el-table-column prop="confirmed" label="已确认收入" min-width="160" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.confirmed }}</span></template>
            </el-table-column>
            <el-table-column prop="balance" label="预收余额" min-width="160" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.balance }}</span></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="课时结转" name="lessonCarry">
          <el-table :data="lessonCarry" stripe max-height="560">
            <el-table-column prop="studentName" label="学生" min-width="140" />
            <el-table-column prop="courseName" label="课程" min-width="220" />
            <el-table-column prop="opening" label="期初" min-width="120" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.opening }}</span></template>
            </el-table-column>
            <el-table-column prop="recharged" label="充值" min-width="120" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.recharged }}</span></template>
            </el-table-column>
            <el-table-column prop="consumed" label="消耗" min-width="120" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.consumed }}</span></template>
            </el-table-column>
            <el-table-column prop="closing" label="期末" min-width="120" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.closing }}</span></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="教师成本" name="teacherCost">
          <el-table :data="teacherCost" stripe max-height="560">
            <el-table-column prop="teacherName" label="教师" min-width="160" />
            <el-table-column prop="lessonCount" label="课时数" min-width="130" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.lessonCount }}</span></template>
            </el-table-column>
            <el-table-column prop="studentCount" label="出勤人次" min-width="140" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.studentCount }}</span></template>
            </el-table-column>
            <el-table-column prop="amount" label="成本金额" min-width="160" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.amount }}</span></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="班级毛利" name="classProfit">
          <el-table :data="classProfit" stripe max-height="560">
            <el-table-column prop="className" label="班级" min-width="190" />
            <el-table-column prop="courseName" label="课程" min-width="220" />
            <el-table-column prop="revenue" label="确认收入" min-width="150" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.revenue }}</span></template>
            </el-table-column>
            <el-table-column prop="cost" label="教师成本" min-width="150" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.cost }}</span></template>
            </el-table-column>
            <el-table-column prop="profit" label="毛利" min-width="150" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.profit }}</span></template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>
