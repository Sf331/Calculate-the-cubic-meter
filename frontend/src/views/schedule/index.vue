<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleApi } from '../../api/schedule'
import type { GenerateResult, ScheduleView } from '../../api/schedule'
import { clazzApi, classroomApi, studentApi, teacherApi } from '../../api/basedata'

type ViewMode = 'class' | 'teacher' | 'classroom' | 'student'

const MODES: { label: string; value: ViewMode }[] = [
  { label: '班级课表', value: 'class' },
  { label: '教师课表', value: 'teacher' },
  { label: '教室课表', value: 'classroom' },
  { label: '学生课表', value: 'student' }
]

/** 视角 → 查询参数名。四类课表在后端就是同一个接口的四种筛选。 */
const PARAM: Record<ViewMode, string> = {
  class: 'classId',
  teacher: 'teacherId',
  classroom: 'classroomId',
  student: 'studentId'
}

const WEEK_LABELS = ['周一', '周二', '周三', '周四', '周五', '周六', '周日']

const mode = ref<ViewMode>('class')
const targetId = ref<number>()
const weekStart = ref(mondayOf(new Date()))
const lessons = ref<ScheduleView[]>([])

const classOptions = ref<{ label: string; value: number }[]>([])
const teacherOptions = ref<{ label: string; value: number }[]>([])
const classroomOptions = ref<{ label: string; value: number }[]>([])
const studentOptions = ref<{ label: string; value: number }[]>([])

const genVisible = ref(false)
const genForm = ref({ startDate: fmt(mondayOf(new Date())), weeks: 4 })
const resultVisible = ref(false)
const result = ref<GenerateResult>()

const moveVisible = ref(false)
const moveTitle = ref('')
const moveForm = ref({ id: 0, lessonDate: '', startTime: '16:00', classroomId: undefined as
  | number
  | undefined })
/** 挪课被拒时的原因。空数组表示成功 */
const moveReasons = ref<string[]>([])
const moving = ref(false)

// ---- 日期工具 ----
function mondayOf(date: Date) {
  const copy = new Date(date)
  const weekday = copy.getDay() || 7 // 周日是 0，按 7 算
  copy.setDate(copy.getDate() - weekday + 1)
  return copy
}

function fmt(date: Date) {
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${m}-${d}`
}

function addDays(date: Date, days: number) {
  const copy = new Date(date)
  copy.setDate(copy.getDate() + days)
  return copy
}

/** "16:00:00" → "16:00" */
const hm = (time: string) => time.slice(0, 5)

const weekDays = computed(() => Array.from({ length: 7 }, (_, i) => addDays(weekStart.value, i)))
const weekLabel = computed(() => `${fmt(weekStart.value)} ~ ${fmt(addDays(weekStart.value, 6))}`)

const options = computed(() => {
  switch (mode.value) {
    case 'teacher':
      return teacherOptions.value
    case 'classroom':
      return classroomOptions.value
    case 'student':
      return studentOptions.value
    default:
      return classOptions.value
  }
})

function lessonsOf(day: Date) {
  const key = fmt(day)
  return lessons.value.filter((l) => l.lessonDate === key)
}

/** 单元格里第二行显示什么，取决于当前视角 —— 看课表的人关心的是"还缺哪一半信息" */
function secondary(lesson: ScheduleView) {
  switch (mode.value) {
    case 'teacher':
      return `${lesson.className} · ${lesson.classroomName}`
    case 'classroom':
      return `${lesson.className} · ${lesson.teacherName}`
    case 'student':
      return `${lesson.className} · ${lesson.teacherName}`
    default:
      return `${lesson.teacherName} · ${lesson.classroomName}`
  }
}

// ---- 数据 ----
async function load() {
  const params: Record<string, unknown> = {
    from: fmt(weekStart.value),
    to: fmt(addDays(weekStart.value, 6))
  }
  if (targetId.value) {
    params[PARAM[mode.value]] = targetId.value
  }
  lessons.value = await scheduleApi.list(params)
}

async function loadOptions() {
  classOptions.value = (await clazzApi.page(1, 200)).records.map((c) => ({
    label: c.name,
    value: c.id!
  }))
  teacherOptions.value = (await teacherApi.page(1, 200)).records.map((t) => ({
    label: `${t.name}（${t.subject}）`,
    value: t.id!
  }))
  classroomOptions.value = (await classroomApi.page(1, 200)).records.map((c) => ({
    label: c.name,
    value: c.id!
  }))
  studentOptions.value = (await studentApi.page(1, 500)).records.map((s) => ({
    label: `${s.name}（${s.grade}）`,
    value: s.id!
  }))
}

function onModeChange() {
  targetId.value = undefined
  load()
}

function shiftWeek(weeks: number) {
  weekStart.value = addDays(weekStart.value, weeks * 7)
  load()
}

async function doGenerate() {
  genVisible.value = false
  result.value = await scheduleApi.generate(genForm.value.startDate, genForm.value.weeks)
  resultVisible.value = true
  // 排完把视图跳到起始日期那一周，否则用户看到的还是空白的一周
  weekStart.value = mondayOf(new Date(genForm.value.startDate))
  await load()
}

async function doClear() {
  await ElMessageBox.confirm('确定清空所有未锁定的课表吗？', '确认', { type: 'warning' })
  await scheduleApi.clear()
  ElMessage.success('已清空')
  await load()
}

function openMove(lesson: ScheduleView) {
  moveTitle.value = `${lesson.className} · ${lesson.courseName} · ${lesson.teacherName}`
  moveForm.value = {
    id: lesson.id,
    lessonDate: lesson.lessonDate,
    startTime: hm(lesson.startTime),
    classroomId: lesson.classroomId
  }
  moveReasons.value = []
  moveVisible.value = true
}

async function submitMove() {
  moving.value = true
  try {
    moveReasons.value = await scheduleApi.move(moveForm.value.id, {
      lessonDate: moveForm.value.lessonDate,
      startTime: moveForm.value.startTime,
      classroomId: moveForm.value.classroomId
    })
    if (moveReasons.value.length === 0) {
      ElMessage.success('已调整')
      moveVisible.value = false
      await load()
    }
  } finally {
    moving.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div>
    <h3>排课与课表</h3>

    <el-form inline>
      <el-form-item label="视角">
        <el-radio-group v-model="mode" @change="onModeChange">
          <el-radio-button v-for="m in MODES" :key="m.value" :value="m.value">
            {{ m.label }}
          </el-radio-button>
        </el-radio-group>
      </el-form-item>

      <el-form-item label="对象">
        <el-select
          v-model="targetId"
          style="width: 200px"
          clearable
          placeholder="全部"
          @change="load"
        >
          <el-option v-for="o in options" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>
      </el-form-item>

      <el-form-item>
        <el-button @click="shiftWeek(-1)">上一周</el-button>
        <span style="margin: 0 12px">{{ weekLabel }}</span>
        <el-button @click="shiftWeek(1)">下一周</el-button>
      </el-form-item>

      <el-form-item>
        <el-button type="primary" @click="genVisible = true">智能排课</el-button>
        <el-button @click="doClear">清空课表</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="[{}]" border style="margin-top: 12px">
      <el-table-column
        v-for="(day, i) in weekDays"
        :key="i"
        :label="`${WEEK_LABELS[i]} ${day.getMonth() + 1}/${day.getDate()}`"
      >
        <template #default>
          <div
            v-for="lesson in lessonsOf(day)"
            :key="lesson.id"
            class="lesson"
            @click="openMove(lesson)"
          >
            <div>{{ hm(lesson.startTime) }}-{{ hm(lesson.endTime) }}</div>
            <div>{{ lesson.courseName }}</div>
            <div>{{ secondary(lesson) }}</div>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="genVisible" title="智能排课" width="420px">
      <el-form label-width="90px">
        <el-form-item label="起始日期">
          <el-date-picker v-model="genForm.startDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="排几周">
          <el-input-number v-model="genForm.weeks" :min="1" :max="30" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" @click="doGenerate">开始排课</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resultVisible" title="排课结果" width="640px">
      <p>共生成 {{ result?.placedCount ?? 0 }} 节课。</p>

      <template v-if="result && result.conflicts.length">
        <p>以下班级有课排不进去：</p>
        <el-table :data="result.conflicts" border>
          <el-table-column prop="className" label="班级" width="150" />
          <el-table-column prop="sessionCount" label="未排节数" width="90" />
          <el-table-column label="原因">
            <template #default="{ row }">{{ row.reasons.join('；') }}</template>
          </el-table-column>
        </el-table>
      </template>
      <p v-else>没有冲突，全部排下。</p>

      <template #footer>
        <el-button type="primary" @click="resultVisible = false">知道了</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="moveVisible" title="调整这节课" width="480px">
      <p>{{ moveTitle }}</p>

      <el-form label-width="90px">
        <el-form-item label="日期">
          <el-date-picker v-model="moveForm.lessonDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select v-model="moveForm.startTime" start="09:00" step="00:30" end="20:30" />
        </el-form-item>
        <el-form-item label="教室">
          <el-select v-model="moveForm.classroomId" style="width: 100%">
            <el-option
              v-for="o in classroomOptions"
              :key="o.value"
              :label="o.label"
              :value="o.value"
            />
          </el-select>
        </el-form-item>
      </el-form>

      <div v-if="moveReasons.length">
        <p>这个时段排不了，原因：</p>
        <ul>
          <li v-for="(reason, i) in moveReasons" :key="i">{{ reason }}</li>
        </ul>
      </div>

      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button type="primary" :loading="moving" @click="submitMove">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.lesson {
  padding: 4px 0;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
}

.lesson:last-child {
  border-bottom: none;
}
</style>
