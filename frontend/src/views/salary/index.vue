<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { monthRange, salaryApi } from '../../api/salary'
import type { PayslipDetail, PayslipView, WorkhourView } from '../../api/salary'
import { attendanceApi, STATUS_OPTIONS } from '../../api/attendance'
import type { SessionView } from '../../api/attendance'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()
const isPrincipal = computed(() => userStore.role() === 'PRINCIPAL')

const thisMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const period = ref(thisMonth())
const tab = ref('payslip')
const loading = ref(false)
const generating = ref(false)

const slips = ref<PayslipView[]>([])
const workhours = ref<WorkhourView[]>([])

const detailVisible = ref(false)
const detail = ref<PayslipDetail>()

const sessionVisible = ref(false)
const session = ref<SessionView>()

const hm = (time: string) => time.slice(0, 5)
const statusLabel = (value: string | null) =>
  STATUS_OPTIONS.find((o) => o.value === value)?.label ?? '未点名'

async function load() {
  loading.value = true
  try {
    const [from, to] = monthRange(period.value)
    slips.value = await salaryApi.payslips({ period: period.value })
    workhours.value = await salaryApi.workhours({ from, to })
  } finally {
    loading.value = false
  }
}

async function generate() {
  generating.value = true
  try {
    const rows = await salaryApi.generate(period.value)
    ElMessage.success(`已生成 ${rows.length} 张工资单`)
    await load()
  } finally {
    generating.value = false
  }
}

async function openDetail(row: PayslipView) {
  detail.value = await salaryApi.detail(row.id)
  detailVisible.value = true
}

/** 工资单项 → 产生这笔钱的那节课的签到名单。验收标准第 5 条就是这条链路。 */
async function openSession(scheduleId: number) {
  session.value = await attendanceApi.session(scheduleId)
  sessionVisible.value = true
}

onMounted(load)
</script>

<template>
  <div>
    <h3>工时薪酬</h3>

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
      <el-form-item v-if="isPrincipal">
        <el-button type="primary" :loading="generating" @click="generate">生成工资单</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="tab">
      <el-tab-pane label="工资单" name="payslip">
        <el-table :data="slips" v-loading="loading" border>
          <el-table-column prop="teacherName" label="教师" width="120" />
          <el-table-column prop="period" label="月份" width="100" />
          <el-table-column prop="lessonCount" label="课时数" width="100" />
          <el-table-column prop="totalAmount" label="工资金额" width="120" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openDetail(row)">明细</el-button>
            </template>
          </el-table-column>
        </el-table>
        <p v-if="!slips.length">
          这个月还没有工资单。{{ isPrincipal ? '点上面的「生成工资单」。' : '' }}
        </p>
      </el-tab-pane>

      <el-tab-pane label="工时明细" name="workhour">
        <el-table :data="workhours" v-loading="loading" border>
          <el-table-column prop="workDate" label="上课日期" width="120" />
          <el-table-column prop="className" label="班级" width="140" />
          <el-table-column prop="courseName" label="课程" />
          <el-table-column prop="studentCount" label="出勤人数" width="100" />
          <el-table-column prop="rate" label="课时单价" width="110" />
          <el-table-column prop="amount" label="金额" width="100" />
          <el-table-column label="操作" width="100">
            <template #default="{ row }">
              <el-button link type="primary" @click="openSession(row.scheduleId)">签到</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="工资单明细" width="820px">
      <p v-if="detail">
        {{ detail.teacherName }} · {{ detail.period }} · 合计 {{ detail.totalAmount }} 元
        （共 {{ detail.items.length }} 节课）
      </p>
      <el-table :data="detail?.items ?? []" border max-height="420">
        <el-table-column prop="workDate" label="上课日期" width="120" />
        <el-table-column prop="className" label="班级" width="140" />
        <el-table-column prop="courseName" label="课程" />
        <el-table-column prop="studentCount" label="出勤人数" width="100" />
        <el-table-column prop="rate" label="单价" width="90" />
        <el-table-column prop="amount" label="金额" width="90" />
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button link type="primary" @click="openSession(row.scheduleId)">看签到</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="sessionVisible" title="这节课的签到" width="640px">
      <p v-if="session">
        {{ session.lessonDate }} {{ hm(session.startTime) }}-{{ hm(session.endTime) }} ·
        {{ session.className }} · {{ session.courseName }}
      </p>
      <el-table :data="session?.students ?? []" border max-height="400">
        <el-table-column prop="studentName" label="学生" width="120" />
        <el-table-column label="签到状态" width="120">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="扣课时" width="100">
          <template #default="{ row }">{{ row.consumedHours ?? '' }}</template>
        </el-table-column>
        <el-table-column label="剩余课时">
          <template #default="{ row }">
            {{ row.remainingHours === null ? '无账户' : row.remainingHours }}
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="sessionVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>
