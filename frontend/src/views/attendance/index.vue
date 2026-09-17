<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { attendanceApi, STATUS_OPTIONS } from '../../api/attendance'
import type { ConsumeResult, SessionStudent, SessionView } from '../../api/attendance'
import type { ScheduleView } from '../../api/schedule'

const STATUS_TEXT: Record<string, string> = {
  PLANNED: '待点名',
  DONE: '已完成',
  CANCELLED: '已取消'
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
  <div>
    <h3>签到管理</h3>

    <el-form inline>
      <el-form-item label="日期范围">
        <el-date-picker
          v-model="range"
          type="daterange"
          value-format="YYYY-MM-DD"
          start-placeholder="开始"
          end-placeholder="结束"
          @change="load"
        />
      </el-form-item>
      <el-form-item>
        <el-button @click="load">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="lessons" v-loading="loading" border style="margin-top: 12px">
      <el-table-column prop="lessonDate" label="日期" width="120" />
      <el-table-column label="时间" width="120">
        <template #default="{ row }">{{ hm(row.startTime) }}-{{ hm(row.endTime) }}</template>
      </el-table-column>
      <el-table-column prop="className" label="班级" width="140" />
      <el-table-column prop="courseName" label="课程" />
      <el-table-column prop="teacherName" label="教师" width="100" />
      <el-table-column prop="classroomName" label="教室" width="100" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">{{ STATUS_TEXT[row.status] ?? row.status }}</template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button link type="primary" @click="open(row)">点名</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" title="点名" width="720px">
      <p v-if="session">
        {{ session.lessonDate }} {{ hm(session.startTime) }}-{{ hm(session.endTime) }} ·
        {{ session.className }} · {{ session.courseName }} · {{ session.teacherName }}
      </p>

      <el-table :data="rows" border max-height="420">
        <el-table-column prop="studentName" label="学生" width="110" />
        <el-table-column label="剩余课时" width="100">
          <template #default="{ row }">
            {{ row.remainingHours === null ? '无账户' : row.remainingHours }}
          </template>
        </el-table-column>
        <el-table-column label="状态">
          <template #default="{ row }">
            <el-radio-group
              v-model="row.status"
              size="small"
              :disabled="signed.has(row.studentId)"
            >
              <el-radio-button
                v-for="o in STATUS_OPTIONS"
                :key="o.value"
                :value="o.value"
              >
                {{ o.label }}
              </el-radio-button>
            </el-radio-group>
            <span v-if="signed.has(row.studentId)" style="margin-left: 8px">已点名</span>
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
      <p>课时余额、教师工时、财务报表三处都已同步。</p>

      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>
