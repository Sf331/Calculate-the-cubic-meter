<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import CrudTable from '../../components/CrudTable.vue'
import type { CrudField } from '../../components/crud'
import {
  GRADE_OPTIONS,
  campusApi,
  clazzApi,
  clazzStudentApi,
  courseApi,
  studentApi
} from '../../api/basedata'
import { lessonAccountApi } from '../../api/attendance'
import { fetchUsersByRole } from '../../api/auth'

const activeTab = ref('student')

const campusOptions = async () =>
  (await campusApi.page(1, 200)).records.map((c) => ({ label: c.name, value: c.id }))

/** 家长账号下拉。账号列表在 auth 模块，走 AuthService 拿，不跨包查表。 */
const parentOptions = async () =>
  (await fetchUsersByRole('PARENT')).map((u) => ({
    label: `${u.realName}（${u.username}）`,
    value: u.id
  }))

const studentFields: CrudField[] = [
  { prop: 'name', label: '姓名', required: true },
  { prop: 'grade', label: '年级', type: 'select', options: GRADE_OPTIONS, required: true, minWidth: 110 },
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions, minWidth: 180 },
  { prop: 'parentUserId', label: '家长账号', type: 'select', loadOptions: parentOptions, minWidth: 210 }
]

// ---- 收费开课 ----
const rechargeVisible = ref(false)
const rechargeTarget = ref<{ id?: number; name?: string }>({})
const rechargeForm = ref({
  courseId: undefined as number | undefined,
  hours: 40,
  amount: 0,
  remark: ''
})
const courseOptions = ref<{ label: string; value: number }[]>([])
const recharging = ref(false)

/** 新建的学生没有课时账户，只能从这里收费开课，之后才点得了名 */
async function openRecharge(row: any) {
  rechargeTarget.value = row
  rechargeForm.value = { courseId: undefined, hours: 40, amount: 0, remark: '' }
  courseOptions.value = (await courseApi.page(1, 200)).records.map((c) => ({
    label: c.name,
    value: c.id!
  }))
  rechargeVisible.value = true
}

async function submitRecharge() {
  const { courseId, hours, amount, remark } = rechargeForm.value
  if (!courseId) {
    ElMessage.warning('请选择课程')
    return
  }
  if (amount <= 0) {
    ElMessage.warning('收费金额必须大于 0')
    return
  }

  recharging.value = true
  try {
    const account = await lessonAccountApi.recharge({
      studentId: rechargeTarget.value.id!,
      courseId,
      hours,
      amount,
      remark: remark || undefined
    })
    ElMessage.success(
      `${account.studentName} 的「${account.courseName}」已充 ${hours} 课时，剩余 ${account.remainingHours}，单价 ${account.unitPrice}`
    )
    rechargeVisible.value = false
  } finally {
    recharging.value = false
  }
}

// ---- 班级名单 ----
const classes = ref<{ label: string; value: number }[]>([])
const allStudents = ref<{ key: number; label: string }[]>([])
const selectedClassId = ref<number>()
const roster = ref<number[]>([])
const saving = ref(false)

async function loadRefs() {
  classes.value = (await clazzApi.page(1, 200)).records.map((c) => ({
    label: c.name,
    value: c.id!
  }))
  allStudents.value = (await studentApi.page(1, 500)).records.map((s) => ({
    key: s.id!,
    label: `${s.name}（${s.grade}）`
  }))
}

async function loadRoster() {
  roster.value = selectedClassId.value
    ? (await clazzStudentApi.list(selectedClassId.value)).map((s) => s.id!)
    : []
}

async function saveRoster() {
  if (!selectedClassId.value) return
  saving.value = true
  try {
    await clazzStudentApi.set(selectedClassId.value, roster.value)
    ElMessage.success('名单已保存')
  } finally {
    saving.value = false
  }
}

/** 切 tab 时重拉，否则刚在"学生"tab 建的学生在这边选不到 */
async function onTabChange(name: string | number) {
  if (name !== 'clazz') return
  await loadRefs()
  await loadRoster()
}

onMounted(async () => {
  await loadRefs()
  await loadRoster()
})
</script>

<template>
  <div class="k-page">
    <el-tabs v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="学生" name="student">
        <CrudTable :fields="studentFields" :api="studentApi" title="学生" :action-width="220">
          <template #actions="{ row }">
            <el-button link type="primary" @click="openRecharge(row)">收费开课</el-button>
          </template>
        </CrudTable>
      </el-tab-pane>

      <el-tab-pane label="班级名单" name="clazz">
        <div class="k-card">
          <div class="k-toolbar">
            <span class="k-muted">班级</span>
            <el-select
              v-model="selectedClassId"
              style="width: 220px"
              placeholder="先选一个班级"
              aria-label="选择班级"
              @change="loadRoster"
            >
              <el-option v-for="c in classes" :key="c.value" :label="c.label" :value="c.value" />
            </el-select>
            <span class="k-spacer" />
            <el-button type="primary" :disabled="!selectedClassId" :loading="saving" @click="saveRoster">
              保存名单
            </el-button>
          </div>

          <div v-if="!selectedClassId" class="k-empty">
            <p class="k-empty__text">先选一个班级，再左右穿梭调整名单</p>
          </div>

          <el-transfer
            v-else
            v-model="roster"
            class="roster"
            :data="allStudents"
            :titles="['全部学生', '本班学生']"
            filterable
            filter-placeholder="搜索学生"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="rechargeVisible" title="收费开课" width="420px" :close-on-click-modal="false">
      <p class="dialog-note">
        <strong>{{ rechargeTarget.name }}</strong>
      </p>
      <p class="dialog-note k-muted">课程要和该学生所在班级上的一致，否则点名时还是扣不动课时。</p>

      <el-form label-position="top" style="margin-top: var(--space-3)">
        <el-form-item label="课程" required>
          <el-select v-model="rechargeForm.courseId" style="width: 100%" placeholder="选一门课">
            <el-option v-for="c in courseOptions" :key="c.value" :label="c.label" :value="c.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="充值课时">
          <el-input-number
            v-model="rechargeForm.hours"
            :min="0.5"
            :step="0.5"
            :precision="1"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="收费金额">
          <el-input-number
            v-model="rechargeForm.amount"
            :min="0"
            :step="100"
            :precision="2"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="rechargeForm.remark" placeholder="不填就是「收费开课：课程名」" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="rechargeVisible = false">取消</el-button>
        <el-button type="primary" :loading="recharging" @click="submitRecharge">确认收费</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.dialog-note {
  font-size: var(--text-sm);
  color: var(--label-primary);
  margin-bottom: var(--space-1);
}

.roster {
  margin-top: var(--space-3);
}

/* 穿梭框面板改成淡底色块，按钮留在中间，整体读起来是"两边列表 + 中间操作" */
.roster :deep(.el-transfer-panel) {
  width: 300px;
  border: none;
  border-radius: var(--radius-md);
  background: var(--bg-subtle);
}

.roster :deep(.el-transfer-panel__header) {
  background: transparent;
  border-bottom: 1px solid var(--separator);
}

.roster :deep(.el-transfer-panel__body) {
  background: transparent;
}

.roster :deep(.el-transfer__buttons) {
  padding: 0 var(--space-2);
}

/* 窄屏放不下两栏并排，改成上下堆叠 */
@media (max-width: 720px) {
  .roster :deep(.el-transfer) {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: var(--space-3);
  }

  .roster :deep(.el-transfer-panel) {
    width: 100%;
    max-width: 440px;
  }
}
</style>
