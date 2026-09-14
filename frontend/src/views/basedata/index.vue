<script setup lang="ts">
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
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions },
  { prop: 'capacity', label: '容量', type: 'number', required: true }
]

const teacherFields: CrudField[] = [
  { prop: 'name', label: '姓名', required: true },
  { prop: 'subject', label: '所授科目', required: true },
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions },
  {
    prop: 'salaryType',
    label: '计薪方式',
    type: 'select',
    options: SALARY_TYPE_OPTIONS,
    required: true,
    defaultValue: 'HOURLY'
  },
  { prop: 'baseSalary', label: '底薪', type: 'number', precision: 2 }
]

const courseFields: CrudField[] = [
  { prop: 'name', label: '课程名称', required: true },
  { prop: 'subject', label: '科目', required: true },
  { prop: 'grade', label: '年级', type: 'select', options: GRADE_OPTIONS },
  {
    prop: 'durationMinutes',
    label: '单次时长(分)',
    type: 'number',
    required: true,
    defaultValue: 45
  },
  { prop: 'weeklyTimes', label: '周频次', type: 'number', required: true, defaultValue: 1 },
  { prop: 'tags', label: '课型标记', type: 'multiselect', options: COURSE_TAG_OPTIONS }
]

const clazzFields: CrudField[] = [
  { prop: 'name', label: '班级名称', required: true },
  {
    prop: 'courseId',
    label: '所属课程',
    type: 'select',
    loadOptions: courseOptions,
    required: true
  },
  {
    prop: 'teacherId',
    label: '授课教师',
    type: 'select',
    loadOptions: teacherOptions,
    required: true
  },
  { prop: 'classroomId', label: '教室', type: 'select', loadOptions: classroomOptions },
  { prop: 'campusId', label: '所属校区', type: 'select', loadOptions: campusOptions },
  { prop: 'capacity', label: '班级容量', type: 'number', required: true }
]
</script>

<template>
  <div>
    <h3>基础数据</h3>

    <el-tabs>
      <el-tab-pane label="校区">
        <CrudTable :fields="campusFields" :api="campusApi" />
      </el-tab-pane>
      <el-tab-pane label="教室">
        <CrudTable :fields="classroomFields" :api="classroomApi" />
      </el-tab-pane>
      <el-tab-pane label="教师">
        <CrudTable :fields="teacherFields" :api="teacherApi" />
      </el-tab-pane>
      <el-tab-pane label="课程">
        <CrudTable :fields="courseFields" :api="courseApi" />
      </el-tab-pane>
      <el-tab-pane label="班级">
        <CrudTable :fields="clazzFields" :api="clazzApi" />
      </el-tab-pane>
    </el-tabs>
  </div>
</template>
