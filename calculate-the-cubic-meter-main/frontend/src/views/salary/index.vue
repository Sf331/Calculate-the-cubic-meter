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
        <el-button v-if="isPrincipal" type="primary" :loading="generating" @click="generate">
          生成工资单
        </el-button>
      </div>
    </div>

    <div class="k-card k-card--table" v-loading="loading">
      <el-tabs v-model="tab">
        <el-tab-pane label="工资单" name="payslip">
          <el-table :data="slips" stripe>
            <el-table-column prop="teacherName" label="教师" min-width="160" />
            <el-table-column prop="period" label="月份" min-width="130">
              <template #default="{ row }"><span class="k-num">{{ row.period }}</span></template>
            </el-table-column>
            <el-table-column prop="lessonCount" label="课时数" min-width="130" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.lessonCount }}</span></template>
            </el-table-column>
            <el-table-column prop="totalAmount" label="工资金额" min-width="150" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.totalAmount }}</span></template>
            </el-table-column>
            <el-table-column label="操作" min-width="130" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">明细</el-button>
              </template>
            </el-table-column>

            <template #empty>
              <div class="k-empty">
                <p class="k-empty__text">
                  这个月还没有工资单。{{ isPrincipal ? '点上面的「生成工资单」。' : '' }}
                </p>
              </div>
            </template>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="工时明细" name="workhour">
          <el-table :data="workhours" stripe>
            <el-table-column prop="workDate" label="上课日期" min-width="160">
              <template #default="{ row }"><span class="k-num">{{ row.workDate }}</span></template>
            </el-table-column>
            <el-table-column prop="className" label="班级" min-width="180" />
            <el-table-column prop="courseName" label="课程" min-width="240" />
            <el-table-column prop="studentCount" label="出勤人数" min-width="140" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.studentCount }}</span></template>
            </el-table-column>
            <el-table-column prop="rate" label="课时单价" min-width="150" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.rate }}</span></template>
            </el-table-column>
            <el-table-column prop="amount" label="金额" min-width="140" align="right">
              <template #default="{ row }"><span class="k-num">{{ row.amount }}</span></template>
            </el-table-column>
            <el-table-column label="操作" min-width="130" align="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openSession(row.scheduleId)">签到</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </div>

    <el-dialog v-model="detailVisible" title="工资单明细" width="680px">
      <p v-if="detail" class="dialog-note">
        {{ detail.teacherName }} · {{ detail.period }} · 合计
        <strong class="k-num">{{ detail.totalAmount }}</strong> 元（共
        <span class="k-num">{{ detail.items.length }}</span> 节课）
      </p>
      <el-table :data="detail?.items ?? []" stripe max-height="420">
        <el-table-column prop="workDate" label="上课日期" min-width="150" />
        <el-table-column prop="className" label="班级" min-width="180" />
        <el-table-column prop="courseName" label="课程" min-width="220" />
        <el-table-column prop="studentCount" label="出勤人数" min-width="130" align="right" />
        <el-table-column prop="rate" label="单价" min-width="120" align="right" />
        <el-table-column prop="amount" label="金额" min-width="120" align="right" />
        <el-table-column label="操作" min-width="130" align="right">
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
      <p v-if="session" class="dialog-note">
        <span class="k-num">
          {{ session.lessonDate }} {{ hm(session.startTime) }}-{{ hm(session.endTime) }}
        </span>
        · {{ session.className }} · {{ session.courseName }}
      </p>
      <el-table :data="session?.students ?? []" stripe max-height="400">
        <el-table-column prop="studentName" label="学生" min-width="150" />
        <el-table-column label="签到状态" min-width="150">
          <template #default="{ row }">{{ statusLabel(row.status) }}</template>
        </el-table-column>
        <el-table-column label="扣课时" min-width="130" align="right">
          <template #default="{ row }">
            <span class="k-num">{{ row.consumedHours ?? '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="剩余课时" min-width="140" align="right">
          <template #default="{ row }">
            <span v-if="row.remainingHours === null" class="k-muted">无账户</span>
            <span v-else class="k-num">{{ row.remainingHours }}</span>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button type="primary" @click="sessionVisible = false">关闭</el-button>
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
