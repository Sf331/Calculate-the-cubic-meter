<script setup lang="ts">
import { ref } from 'vue'
import CrudTable from '../../components/CrudTable.vue'
import type { CrudField } from '../../components/crud'
import {
  COURSE_TAG_OPTIONS,
  GRADE_OPTIONS,
  SALARY_TYPE_OPTIONS,
  campusApi,
  clazzApi,
  classroomApi,
  courseApi,
  teacherApi
} from '../../api/basedata'

const activeTab = ref('campus')

/** 各下拉的选项来源。每次打开弹窗会重拉，所以在别的 tab 新建的项这里能马上选到。 */
const campusOptions = async () =>
  (await campusApi.page(1, 200)).records.map((c) => ({ label: c.name, value: c.id }))

const teacherOptions = async () =>
  (await teacherApi.page(1, 200)).records.map((t) => ({ label: t.name, value: t.id }))

const classroomOptions = async () =>
  (await classroomApi.page(1, 200)).records.map((c) => ({ label: c.name, value: c.id }))

const courseOptions = async () =>
  (await courseApi.page(1, 200)).records.map((c) => ({ label: c.name, value: c.id }))

const campusFields: CrudField[] = [
  { prop: 'name', label: '校区名称', required: true },
  { prop: 'address', label: '地址' }
]

const classroomFields: CrudField[] = [
  { prop: 'name', label: '教室名称', required: true },
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions, minWidth: 180 },
  { prop: 'capacity', label: '容量', type: 'number', required: true, minWidth: 110 }
]

const teacherFields: CrudField[] = [
  { prop: 'name', label: '姓名', required: true },
  { prop: 'subject', label: '所授科目', required: true, minWidth: 150 },
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions, minWidth: 180 },
  {
    prop: 'salaryType',
    label: '计薪方式',
    type: 'select',
    options: SALARY_TYPE_OPTIONS,
    required: true,
    defaultValue: 'HOURLY',
    minWidth: 160
  },
  { prop: 'baseSalary', label: '底薪', type: 'number', precision: 2, minWidth: 140 },
  { prop: 'hourlyRate', label: '课时单价', type: 'number', precision: 2, defaultValue: 0, minWidth: 150 }
]

const courseFields: CrudField[] = [
  { prop: 'name', label: '课程名称', required: true },
  { prop: 'subject', label: '科目', required: true, minWidth: 120 },
  { prop: 'grade', label: '年级', type: 'select', options: GRADE_OPTIONS, minWidth: 110 },
  {
    prop: 'durationMinutes',
    label: '单次时长(分)',
    type: 'number',
    required: true,
    defaultValue: 45,
    minWidth: 140
  },
  {
    prop: 'weeklyTimes',
    label: '周频次',
    type: 'number',
    required: true,
    defaultValue: 1,
    minWidth: 110
  },
  {
    prop: 'tags',
    label: '课型标记',
    type: 'multiselect',
    options: COURSE_TAG_OPTIONS,
    asTag: true,
    minWidth: 130
  }
]

// 六列 + 操作，min-width 合计要压在内容区宽度以内，否则「操作」列会被横向滚动推出去
const clazzFields: CrudField[] = [
  { prop: 'name', label: '班级名称', required: true, minWidth: 200 },
  {
    prop: 'courseId',
    label: '所属课程',
    type: 'select',
    loadOptions: courseOptions,
    required: true,
    minWidth: 165
  },
  {
    prop: 'teacherId',
    label: '授课教师',
    type: 'select',
    loadOptions: teacherOptions,
    required: true,
    minWidth: 165
  },
  { prop: 'classroomId', label: '教室', type: 'select', loadOptions: classroomOptions, minWidth: 120 },
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions, minWidth: 150 },
  { prop: 'capacity', label: '班级容量', type: 'number', required: true, minWidth: 100 }
]
</script>

<template>
  <div class="k-page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="校区" name="campus">
        <CrudTable :fields="campusFields" :api="campusApi" title="校区" />
      </el-tab-pane>
      <el-tab-pane label="教室" name="classroom">
        <CrudTable :fields="classroomFields" :api="classroomApi" title="教室" />
      </el-tab-pane>
      <el-tab-pane label="教师" name="teacher">
        <CrudTable :fields="teacherFields" :api="teacherApi" title="教师" />
      </el-tab-pane>
      <el-tab-pane label="课程" name="course">
        <CrudTable :fields="courseFields" :api="courseApi" title="课程" />
      </el-tab-pane>
      <el-tab-pane label="班级" name="clazz">
        <CrudTable :fields="clazzFields" :api="clazzApi" title="班级" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
