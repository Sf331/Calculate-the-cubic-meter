<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { attendanceApi, STATUS_OPTIONS } from '../../api/attendance'
import type { ConsumeResult, SessionStudent, SessionView } from '../../api/attendance'
import type { ScheduleView } from '../../api/schedule'

/** 课次状态 → 文案 + 标签配色。只有承担业务含义的值才上色。 */
const STATUS: Record<string, { text: string; tone: string }> = {
  PLANNED: { text: '待点名', tone: 'warning' },
  DONE: { text: '已完成', tone: 'success' },
  CANCELLED: { text: '已取消', tone: 'neutral' }
}

const fmt = (date: Date) => {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

const addDays = (days: number) => {
  const copy = new Date()
  copy.setDate(copy.getDate() + days)
  return fmt(copy)
}

const range = ref<[string, string]>([addDays(-7), addDays(30)])
const lessons = ref<ScheduleView[]>([])
const loading = ref(false)

const dialogVisible = ref(false)
const session = ref<SessionView>()
/** 名单的本地副本，直接绑在单选框上 */
const rows = ref<SessionStudent[]>([])
/** 打开时就已点过名的学生，这些行不允许改 */
const signed = ref<Set<number>>(new Set())
const submitting = ref(false)
const result = ref<ConsumeResult>()
const resultVisible = ref(false)

const hm = (time: string) => time.slice(0, 5)

const changed = computed(() =>
  rows.value.filter(
    (row) => row.status && !signed.value.has(row.studentId)
  )
)

async function load() {
  loading.value = true
  try {
    lessons.value = await attendanceApi.lessons({ from: range.value[0], to: range.value[1] })
  } finally {
    loading.value = false
  }
}

async function open(schedule: ScheduleView) {
  session.value = await attendanceApi.session(schedule.id)
  signed.value = new Set(
    session.value.students.filter((s) => s.status).map((s) => s.studentId)
  )
  // 默认全按出勤，老师只需要改动少数几个 —— 演示时少点几下。
  // 但没课时账户的学生不给默认值：他被带上整批就会被回滚，
  // 名单上「剩余课时」那列会显示"无账户"，由老师自己决定怎么处理。
  rows.value = session.value.students.map((s) => ({
    ...s,
    status: s.status ?? (s.remainingHours === null ? null : 'PRESENT')
  }))
  result.value = undefined
  dialogVisible.value = true
}

async function submit() {
  if (!changed.value.length) {
    ElMessage.warning('请先给至少一个学生选状态')
    return
  }
  submitting.value = true
  try {
    result.value = await attendanceApi.submit(
      session.value!.scheduleId,
      changed.value.map((row) => ({ studentId: row.studentId, status: row.status! }))
    )
    dialogVisible.value = false
    resultVisible.value = true
    await load()
  } finally {
    submitting.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="k-page">
    <div class="k-card k-card--table">
      <div class="k-toolbar">
        <span class="k-muted">日期范围</span>
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          aria-label="日期范围"
          @change="load"
        />
        <span class="k-spacer" />
        <el-button @click="load">查询</el-button>
      </div>

      <el-table :data="lessons" v-loading="loading" stripe style="margin-top: var(--space-2)">
        <el-table-column prop="lessonDate" label="日期" min-width="140">
          <template #default="{ row }">
            <span class="k-num">{{ row.lessonDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" min-width="140">
          <template #default="{ row }">
            <span class="k-num">{{ hm(row.startTime) }}-{{ hm(row.endTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="className" label="班级" min-width="150" />
        <el-table-column prop="courseName" label="课程" min-width="180" />
        <el-table-column prop="teacherName" label="教师" min-width="110" />
        <el-table-column prop="classroomName" label="教室" min-width="100" />
        <el-table-column label="状态" min-width="110">
          <template #default="{ row }">
            <span class="k-tag" :class="`k-tag--${STATUS[row.status]?.tone ?? 'neutral'}`">
              {{ STATUS[row.status]?.text ?? row.status }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="120" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="open(row)">点名</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="k-empty">
            <p class="k-empty__text">这个日期范围里还没有课次，先到「排课与课表」排一次课</p>
          </div>
        </template>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" title="点名" width="640px" :close-on-click-modal="false">
      <p v-if="session" class="dialog-note">
        <span class="k-num">
          {{ session.lessonDate }} {{ hm(session.startTime) }}-{{ hm(session.endTime) }}
        </span>
        · {{ session.className }} · {{ session.courseName }} · {{ session.teacherName }}
      </p>

      <el-table :data="rows" stripe max-height="420">
        <el-table-column prop="studentName" label="学生" min-width="130" />
        <el-table-column label="剩余课时" min-width="140">
          <template #default="{ row }">
            <span v-if="row.remainingHours === null" class="k-muted">无账户</span>
            <span v-else class="k-num">{{ row.remainingHours }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-radio-group
              v-model="row.status"
              :disabled="signed.has(row.studentId)"
              :aria-label="`${row.studentName}的签到状态`"
            >
              <el-radio-button
                v-for="o in STATUS_OPTIONS"
                :key="o.value"
                :value="o.value"
              >
                {{ o.label }}
              </el-radio-button>
            </el-radio-group>
            <span v-if="signed.has(row.studentId)" class="k-tag k-tag--neutral">已点名</span>
          </template>
        </el-table-column>
      </el-table>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submit">
          提交 {{ changed.length }} 人
        </el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="点名完成" width="420px">
      <el-descriptions :column="1" border>
        <el-descriptions-item label="本次点名">{{ result?.attendanceCount }} 人</el-descriptions-item>
        <el-descriptions-item label="消耗课时">{{ result?.consumedHours }}</el-descriptions-item>
        <el-descriptions-item label="确认收入">{{ result?.confirmedAmount }} 元</el-descriptions-item>
        <el-descriptions-item label="教师工时">
          {{ result?.workhourAmount }} 元
        </el-descriptions-item>
      </el-descriptions>
      <p class="dialog-note k-muted">课时余额、教师工时、财务报表三处都已同步。</p>

      <template #footer>
        <el-button type="primary" @click="resultVisible = false">完成</el-button>
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

.k-muted + .dialog-note {
  margin-top: var(--space-2);
  margin-bottom: 0;
}
</style>
