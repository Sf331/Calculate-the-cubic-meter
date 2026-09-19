<script setup lang="ts">
import { onMounted, ref } from 'vue'
import CrudTable from '../../components/CrudTable.vue'
import type { CrudField } from '../../components/crud'
import { GRADE_OPTIONS, courseApi } from '../../api/basedata'
import { attendanceApi } from '../../api/attendance'
import type { ScheduleView } from '../../api/schedule'
import { ASSET_SCOPE_OPTIONS, ASSET_TYPE_OPTIONS, contentApi } from '../../api/content'

const lessons = ref<ScheduleView[]>([])
const lessonId = ref<number>()
/** 当前选中课次对应的课程。空表示不筛，看我能看到的全部 */
const courseId = ref<number>()

const courseOptions = async () =>
  (await courseApi.page(1, 200)).records.map((c) => ({ label: c.name, value: c.id }))

/**
 * 九列 + 操作，是本站最宽的一张表。
 * min-width 是**预算**不是各自的偏好：全部加起来必须 ≤ 内容区宽度（1440 屏下 1186），
 * 否则 el-table 会横向滚动，把「操作」列推出可视区 —— 页面上看不出异常，
 * 只有量 body-wrapper 的 scrollWidth 才会发现。改这里的任何一列，都要重新算总和。
 */
const fields: CrudField[] = [
  { prop: 'name', label: '名称', required: true, minWidth: 150 },
  {
    prop: 'type',
    label: '类型',
    type: 'select',
    options: ASSET_TYPE_OPTIONS,
    required: true,
    defaultValue: 'HANDOUT',
    minWidth: 90
  },
  {
    prop: 'scope',
    label: '可见范围',
    type: 'select',
    options: ASSET_SCOPE_OPTIONS,
    required: true,
    defaultValue: 'ALL',
    minWidth: 100
  },
  { prop: 'subject', label: '科目', minWidth: 85 },
  { prop: 'grade', label: '年级', type: 'select', options: GRADE_OPTIONS, minWidth: 85 },
  { prop: 'courseId', label: '课程', type: 'select', loadOptions: courseOptions, minWidth: 145 },
  { prop: 'knowledgePoint', label: '知识点', minWidth: 120 },
  // 版本由后端读库自算，前端改不了 —— 标成只读，免得有人改了发现没反应。
  // 内容长度恒定（就是个数字），这一列用固定宽度而不是最小宽度。
  { prop: 'versionNo', label: '版本', type: 'number', readonly: true, width: 80 },
  { prop: 'filePath', label: '文件', type: 'file', uploadUrl: '/content/upload', minWidth: 110 }
]

/** 带上当前选中的课程。切课次时靠 :key 重挂表格，所以这里读到的一定是新值 */
const api = {
  page: (page: number, size: number) => contentApi.page(page, size, courseId.value),
  create: contentApi.create,
  update: contentApi.update,
  remove: contentApi.remove
}

/** 选完课次反查它的课程 —— 这就是"按课表推送本节内容" */
function onLesson(id?: number) {
  courseId.value = lessons.value.find((lesson) => lesson.id === id)?.courseId
}

function openFile(row: any) {
  window.open(`/uploads/${row.filePath}`, '_blank')
}

onMounted(async () => {
  lessons.value = await attendanceApi.lessons({})
})
</script>

<template>
  <div class="k-page">
    <div class="k-card">
      <div class="k-toolbar">
        <span class="k-muted">课次</span>
        <el-select
          v-model="lessonId"
          clearable
          placeholder="不选就是我能看到的全部内容"
          style="width: 360px"
          aria-label="选择课次"
          @change="onLesson"
        >
          <el-option
            v-for="lesson in lessons"
            :key="lesson.id"
            :label="`${lesson.lessonDate} ${lesson.className} · ${lesson.courseName}`"
            :value="lesson.id"
          />
        </el-select>
        <span class="k-spacer" />
        <span v-if="!lessons.length" class="k-muted">
          还没有课表，先到「排课与课表」排一次课
        </span>
      </div>
    </div>

    <!-- 换课次就换 key，表格重挂后用新的 courseId 重新拉（页码回到 1，符合预期） -->
    <CrudTable
      :key="courseId ?? 'all'"
      :fields="fields"
      :api="api"
      title="内容"
      :action-width="160"
    >
      <template #actions="{ row }">
        <el-button v-if="row.filePath" link type="primary" @click="openFile(row)">下载</el-button>
      </template>
    </CrudTable>
  </div>
</template>
