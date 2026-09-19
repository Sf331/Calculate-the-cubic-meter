<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { scheduleApi } from '../../api/schedule'
import type { GenerateResult, ReschedulePlan, ScheduleView } from '../../api/schedule'
import { clazzApi, classroomApi, studentApi, teacherApi } from '../../api/basedata'
import { canEditSchedule } from '../../router/menus'
import { useUserStore } from '../../stores/user'

const userStore = useUserStore()

/**
 * 教师/学生/家长只看课表：不显示智能排课与清空课表，课程卡也不可点。
 * 注意这只是界面层的收敛 —— 后端现在只校验登录、不校验角色，
 * 真要拦住得在 ScheduleController 上加校验，见 CLAUDE.md「角色与权限」。
 */
const canEdit = computed(() => canEditSchedule(userStore.role()))

type ViewMode = 'class' | 'teacher' | 'classroom' | 'student'

const MODES: { label: string; value: ViewMode }[] = [
  { label: '班级', value: 'class' },
  { label: '教师', value: 'teacher' },
  { label: '教室', value: 'classroom' },
  { label: '学生', value: 'student' }
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
/** 预览出来的调整方案。确认后才落库 */
const plan = ref<ReschedulePlan>()
const planning = ref(false)
const applying = ref(false)

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
const viewLabel = computed(
  () => MODES.find((m) => m.value === mode.value)?.label ?? ''
)

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
  // 破坏性操作写清楚后果，按钮用动词
  await ElMessageBox.confirm('所有未锁定的课都会被删除，此操作不可恢复。', '清空课表', {
    type: 'warning',
    confirmButtonText: '清空',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger'
  })
  await scheduleApi.clear()
  ElMessage.success('已清空')
  await load()
}

/** 只读角色点课程卡不该有任何反应 —— 拦在这里，模板里就不用给每张卡挂条件 */
function onLessonClick(lesson: ScheduleView) {
  if (!canEdit.value) return
  openMove(lesson)
}

function openMove(lesson: ScheduleView) {
  moveTitle.value = `${lesson.className} · ${lesson.courseName} · ${lesson.teacherName}`
  moveForm.value = {
    id: lesson.id,
    lessonDate: lesson.lessonDate,
    startTime: hm(lesson.startTime),
    classroomId: lesson.classroomId
  }
  plan.value = undefined
  moveVisible.value = true
}

const rescheduleReq = (dryRun: boolean) => ({
  lessonDate: moveForm.value.lessonDate,
  startTime: moveForm.value.startTime,
  classroomId: moveForm.value.classroomId,
  dryRun
})

/** 先出方案给教务看，不落库 */
async function previewMove() {
  planning.value = true
  try {
    plan.value = await scheduleApi.reschedule(moveForm.value.id, rescheduleReq(true))
  } finally {
    planning.value = false
  }
}

async function confirmMove() {
  applying.value = true
  try {
    await scheduleApi.reschedule(moveForm.value.id, rescheduleReq(false))
    ElMessage.success('课表已调整')
    moveVisible.value = false
    await load()
  } finally {
    applying.value = false
  }
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div class="k-page k-page--fill">
    <!-- 操作条 -->
    <div class="k-card">
      <div class="k-toolbar">
        <span class="k-muted">视角</span>
        <el-radio-group v-model="mode" aria-label="课表视角" @change="onModeChange">
          <el-radio-button v-for="m in MODES" :key="m.value" :value="m.value">
            {{ m.label }}
          </el-radio-button>
        </el-radio-group>

        <el-select
          v-model="targetId"
          style="width: 180px"
          clearable
          placeholder="全部"
          :aria-label="`选择${viewLabel}`"
          @change="load"
        >
          <el-option v-for="o in options" :key="o.value" :label="o.label" :value="o.value" />
        </el-select>

        <span class="k-spacer" />

        <div class="weeknav">
          <button type="button" class="weeknav__btn" aria-label="上一周" @click="shiftWeek(-1)">
            ‹
          </button>
          <span class="weeknav__label k-num">{{ weekLabel }}</span>
          <button type="button" class="weeknav__btn" aria-label="下一周" @click="shiftWeek(1)">
            ›
          </button>
        </div>

        <template v-if="canEdit">
          <el-button type="primary" @click="genVisible = true">智能排课</el-button>
          <el-button class="btn-destructive" @click="doClear">清空课表</el-button>
        </template>
      </div>
    </div>

    <!-- 周视图：横向滚动容器，窄屏不挤压列宽 -->
    <div class="week-scroll k-page__fill">
      <div class="week" role="table" :aria-label="`${viewLabel}周课表 ${weekLabel}`">
        <div v-for="(day, i) in weekDays" :key="i" class="day" role="row">
          <div class="day__head">
            <span class="day__name">{{ WEEK_LABELS[i] }}</span>
            <span class="day__date k-num">{{ day.getMonth() + 1 }}/{{ day.getDate() }}</span>
          </div>

          <div class="day__body">
            <!-- 可改课表时是按钮，否则是纯展示的 div —— 别给只读角色留一个点得动的焦点位 -->
            <component
              :is="canEdit ? 'button' : 'div'"
              v-for="lesson in lessonsOf(day)"
              :key="lesson.id"
              :type="canEdit ? 'button' : undefined"
              class="lesson"
              :class="{ 'lesson--static': !canEdit }"
              :aria-label="
                canEdit
                  ? `${lesson.courseName} ${hm(lesson.startTime)} 到 ${hm(lesson.endTime)}，点击调整`
                  : `${lesson.courseName} ${hm(lesson.startTime)} 到 ${hm(lesson.endTime)}`
              "
              @click="onLessonClick(lesson)"
            >
              <span class="lesson__time k-num">
                {{ hm(lesson.startTime) }}–{{ hm(lesson.endTime) }}
              </span>
              <span class="lesson__course">{{ lesson.courseName }}</span>
              <span class="lesson__meta">{{ secondary(lesson) }}</span>
            </component>

            <p v-if="!lessonsOf(day).length" class="day__empty">无课</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 智能排课 -->
    <el-dialog v-model="genVisible" title="智能排课" width="400px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="起始日期">
          <el-date-picker
            v-model="genForm.startDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="排几周">
          <el-input-number v-model="genForm.weeks" :min="1" :max="30" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="genVisible = false">取消</el-button>
        <el-button type="primary" @click="doGenerate">开始排课</el-button>
      </template>
    </el-dialog>

    <!-- 排课结果 -->
    <el-dialog v-model="resultVisible" title="排课结果" width="560px">
      <p class="summary">共生成 <strong>{{ result?.placedCount ?? 0 }}</strong> 节课。</p>

      <template v-if="result && result.conflicts.length">
        <p class="summary summary--warn">以下班级有课排不进去：</p>
        <el-table :data="result.conflicts" stripe>
          <el-table-column prop="className" label="班级" width="150" />
          <el-table-column prop="sessionCount" label="未排节数" width="90" />
          <el-table-column label="原因">
            <template #default="{ row }">{{ row.reasons.join('；') }}</template>
          </el-table-column>
        </el-table>
      </template>
      <p v-else class="summary k-muted">没有冲突，全部排下。</p>

      <template #footer>
        <el-button type="primary" @click="resultVisible = false">完成</el-button>
      </template>
    </el-dialog>

    <!-- 调整这节课 -->
    <el-dialog
      v-model="moveVisible"
      title="调整这节课"
      width="640px"
      :close-on-click-modal="false"
    >
      <p class="move-title">{{ moveTitle }}</p>

      <el-form label-position="top" class="move-form">
        <el-form-item label="日期">
          <el-date-picker
            v-model="moveForm.lessonDate"
            type="date"
            value-format="YYYY-MM-DD"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-time-select
            v-model="moveForm.startTime"
            start="09:00"
            step="00:30"
            end="20:30"
            style="width: 100%"
          />
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

      <template v-if="plan">
        <template v-if="!plan.feasible">
          <div class="verdict verdict--bad">
            <p class="verdict__title">这个时段排不了</p>
            <ul class="verdict__list">
              <li v-for="(reason, i) in plan.reasons" :key="i">{{ reason }}</li>
            </ul>
          </div>
        </template>

        <template v-else>
          <p class="summary">
            需要调整 <strong>{{ plan.moves.length }}</strong> 节课，影响
            <strong>{{ plan.affectedClasses.length }}</strong> 个班、
            <strong>{{ plan.affectedTeachers.length }}</strong> 位教师、
            <strong>{{ plan.affectedStudents }}</strong> 名学生。
          </p>
          <el-table :data="plan.moves" stripe>
            <el-table-column prop="className" label="班级" width="130" />
            <el-table-column label="原时间" width="180">
              <template #default="{ row }">
                {{ row.fromDate }} {{ row.fromStart.slice(0, 5) }}-{{ row.fromEnd.slice(0, 5) }}
              </template>
            </el-table-column>
            <el-table-column prop="fromClassroomName" label="原教室" width="90" />
            <el-table-column label="新时间" width="180">
              <template #default="{ row }">
                {{ row.toDate }} {{ row.toStart.slice(0, 5) }}-{{ row.toEnd.slice(0, 5) }}
              </template>
            </el-table-column>
            <el-table-column prop="toClassroomName" label="新教室" width="90" />
            <el-table-column prop="teacherName" label="教师" />
          </el-table>
        </template>
      </template>

      <template #footer>
        <el-button @click="moveVisible = false">取消</el-button>
        <el-button :loading="planning" @click="previewMove">预览方案</el-button>
        <el-button v-if="plan?.feasible" type="primary" :loading="applying" @click="confirmMove">
          确认调整
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* ---------- 周切换 ---------- */
.weeknav {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

/* 细描边小按钮，跟工具栏其他控件同高；::after 在触屏下把可点区补到 44 */
.weeknav__btn {
  position: relative;
  width: var(--control-h);
  height: var(--control-h);
  display: grid;
  place-items: center;
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
  background: var(--bg-card);
  color: var(--label-primary);
  font-family: inherit;
  font-size: var(--text-sm);
  line-height: 1;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.weeknav__btn::after {
  content: '';
  position: absolute;
  inset: calc((var(--touch-min) - var(--control-h)) / -2);
}

.weeknav__btn:hover {
  background: var(--bg-hover);
  border-color: var(--separator-strong);
}

.weeknav__btn:active {
  transform: scale(0.95);
}

.weeknav__btn:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: 2px;
}

.weeknav__label {
  font-size: var(--text-xs);
  color: var(--label-secondary);
  font-variant-numeric: tabular-nums;
  min-width: 200px;
  text-align: center;
}

/* ---------- 周视图 ----------
   窄屏不做折叠：七列始终是七列，横过来看跟桌面端是同一张表，
   不用重新学一遍版式。
   撑满高度的规则（.k-page--fill）在 index.css 里 —— 图表页也要用同一套，
   写两份会漂。这里只管日历自己。 */
.week-scroll {
  margin-top: var(--space-3);
  overflow: auto;
}

.week {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 1px;
  min-width: 1120px;
  height: 100%;
  background: var(--separator);
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
  overflow: hidden;
}

/* 七列共用一张表：靠 1px 间隙露出底色当分隔线，不再各自浮一张卡 */
.day {
  display: flex;
  flex-direction: column;
  min-height: 220px;
  background: var(--bg-card);
}

.day__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-2);
  padding: var(--space-1) var(--space-2);
  border-bottom: 1px solid var(--separator);
  background: var(--bg-subtle);
}

.day__name {
  font-size: var(--text-xs);
  font-weight: 500;
  color: var(--label-primary);
}

.day__date {
  font-size: var(--text-xs);
  color: var(--label-secondary);
  font-variant-numeric: tabular-nums;
}

.day__body {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding: var(--space-1);
}

/* "无课"是内容不是装饰，用 secondary 而不是 tertiary ——
   后者对白底只有 2.56:1，达不到 AA。

   纵向顶部对齐，不用 margin: auto 居中：一列有课、隔壁没课时，
   居中的两个字会飘在三四百像素高的格子正中间，跟隔壁从顶上排下来的课程块
   不在同一条视线上，扫过去像个渲染错位。横向仍居中 —— 它是占位，不是内容。 */
.day__empty {
  margin: var(--space-2) auto 0;
  font-size: var(--text-xs);
  color: var(--label-secondary);
}

/* 课程块：时长 / 课名 / 另一半信息三层，没有额外装饰 */
.lesson {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: var(--space-1) var(--space-2);
  border: 1px solid var(--separator);
  border-radius: var(--radius-sm);
  background: var(--bg-card);
  font-family: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.lesson:hover {
  background: var(--bg-hover);
  border-color: var(--separator-strong);
}

.lesson:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -1px;
}

/* 只读角色：不响应 hover、不显示手型 */
.lesson--static,
.lesson--static:hover {
  background: var(--bg-card);
  border-color: var(--separator);
  cursor: default;
}

.lesson__time {
  font-size: var(--text-xs);
  color: var(--label-secondary);
  font-variant-numeric: tabular-nums;
}

.lesson__course {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
}

.lesson__meta {
  font-size: var(--text-xs);
  color: var(--label-secondary);
}

/* ---------- 弹窗内的说明与结论 ---------- */
.summary {
  font-size: var(--text-sm);
  color: var(--label-primary);
  margin-bottom: var(--space-2);
}

.summary--warn {
  color: var(--color-warning);
}

.verdict {
  border-radius: var(--radius-md);
  padding: var(--space-3);
}

.verdict--bad {
  background: var(--tint-danger);
}

.verdict__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--color-danger);
  margin-bottom: var(--space-2);
}

.verdict__list {
  margin: 0;
  padding-left: var(--space-4);
  font-size: var(--text-xs);
  color: var(--label-primary);
}

.move-title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
  margin-bottom: var(--space-3);
}

.move-form {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(190px, 1fr));
  gap: 0 var(--space-3);
  align-items: end;
}
</style>
