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

const fields: CrudField[] = [
  { prop: 'name', label: '名称', required: true, width: 180 },
  {
    prop: 'type',
    label: '类型',
    type: 'select',
    options: ASSET_TYPE_OPTIONS,
    required: true,
    defaultValue: 'HANDOUT',
    width: 100
  },
  {
    prop: 'scope',
    label: '可见范围',
    type: 'select',
    options: ASSET_SCOPE_OPTIONS,
    required: true,
    defaultValue: 'ALL',
    width: 120
  },
  { prop: 'subject', label: '科目', width: 100 },
  { prop: 'grade', label: '年级', type: 'select', options: GRADE_OPTIONS, width: 100 },
  { prop: 'courseId', label: '课程', type: 'select', loadOptions: courseOptions, width: 150 },
  { prop: 'knowledgePoint', label: '知识点' },
  // 版本由后端读库自算，前端改不了 —— 标成只读，免得有人改了发现没反应
  { prop: 'versionNo', label: '版本', type: 'number', readonly: true, width: 80 },
  { prop: 'filePath', label: '文件', type: 'file', uploadUrl: '/content/upload' }
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
  <div>
    <h3>内容托管</h3>

    <el-form inline>
      <el-form-item label="课次">
        <el-select
          v-model="lessonId"
          clearable
          placeholder="不选就是我能看到的全部内容"
          style="width: 360px"
          @change="onLesson"
        >
          <el-option
            v-for="lesson in lessons"
            :key="lesson.id"
            :label="`${lesson.lessonDate} ${lesson.className} · ${lesson.courseName}`"
            :value="lesson.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <span v-if="!lessons.length">还没有课表，先到「排课与课表」排一次课</span>
      </el-form-item>
    </el-form>

    <!-- 换课次就换 key，表格重挂后用新的 courseId 重新拉（页码回到 1，符合预期） -->
    <CrudTable :key="courseId ?? 'all'" :fields="fields" :api="api" :action-width="180">
      <template #actions="{ row }">
        <el-button v-if="row.filePath" link type="primary" @click="openFile(row)">下载</el-button>
      </template>
    </CrudTable>
  </div>
</template>
