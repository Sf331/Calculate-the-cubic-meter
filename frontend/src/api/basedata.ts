import { del, get, post, put } from './request'

/** 后端 MyBatis-Plus 的 Page 序列化结果，前端只用 records 和 total */
export interface PageResult<T> {
  records: T[]
  total: number
}

export interface Campus {
  id?: number
  name: string
  address?: string
}

export interface Classroom {
  id?: number
  campusId?: number
  name: string
  capacity: number
}

export interface Teacher {
  id?: number
  userId?: number
  name: string
  subject: string
  campusId?: number
  salaryType: string
  baseSalary: number
}

/** 计薪方式的选项。薪酬模块也会用到，所以放在这里而不是页面里。 */
export const SALARY_TYPE_OPTIONS = [
  { label: '纯课时费', value: 'HOURLY' },
  { label: '底薪+课时费', value: 'BASE_HOURLY' },
  { label: '纯月薪', value: 'MONTHLY' }
]

export const GRADE_OPTIONS = [
  '一年级', '二年级', '三年级', '四年级', '五年级', '六年级',
  '初一', '初二', '初三',
  '高一', '高二', '高三'
].map((g) => ({ label: g, value: g }))

export const COURSE_TAG_OPTIONS = [
  { label: '试听课', value: 'TRIAL' },
  { label: '补课', value: 'MAKEUP' },
  { label: '代课', value: 'SUBSTITUTE' },
  { label: '跨校区课', value: 'CROSS_CAMPUS' }
]

export interface Course {
  id?: number
  name: string
  subject: string
  grade?: string
  durationMinutes: number
  weeklyTimes: number
  tags?: string[]
}

export interface Clazz {
  id?: number
  name: string
  courseId?: number
  teacherId?: number
  classroomId?: number
  campusId?: number
  capacity: number
}

export interface Student {
  id?: number
  userId?: number
  parentUserId?: number
  name: string
  grade: string
  campusId?: number
}

export const campusApi = {
  page: (page = 1, size = 20) => get<PageResult<Campus>>('/basedata/campus', { page, size }),
  create: (data: Campus) => post<Campus>('/basedata/campus', data),
  update: (id: number, data: Campus) => put<Campus>(`/basedata/campus/${id}`, data),
  remove: (id: number) => del<void>(`/basedata/campus/${id}`)
}

export const classroomApi = {
  page: (page = 1, size = 20) => get<PageResult<Classroom>>('/basedata/classroom', { page, size }),
  create: (data: Classroom) => post<Classroom>('/basedata/classroom', data),
  update: (id: number, data: Classroom) => put<Classroom>(`/basedata/classroom/${id}`, data),
  remove: (id: number) => del<void>(`/basedata/classroom/${id}`)
}

export const teacherApi = {
  page: (page = 1, size = 20) => get<PageResult<Teacher>>('/basedata/teacher', { page, size }),
  create: (data: Teacher) => post<Teacher>('/basedata/teacher', data),
  update: (id: number, data: Teacher) => put<Teacher>(`/basedata/teacher/${id}`, data),
  remove: (id: number) => del<void>(`/basedata/teacher/${id}`)
}

export const courseApi = {
  page: (page = 1, size = 20) => get<PageResult<Course>>('/basedata/course', { page, size }),
  create: (data: Course) => post<Course>('/basedata/course', data),
  update: (id: number, data: Course) => put<Course>(`/basedata/course/${id}`, data),
  remove: (id: number) => del<void>(`/basedata/course/${id}`)
}

export const clazzApi = {
  page: (page = 1, size = 20) => get<PageResult<Clazz>>('/basedata/clazz', { page, size }),
  create: (data: Clazz) => post<Clazz>('/basedata/clazz', data),
  update: (id: number, data: Clazz) => put<Clazz>(`/basedata/clazz/${id}`, data),
  remove: (id: number) => del<void>(`/basedata/clazz/${id}`)
}

export const studentApi = {
  page: (page = 1, size = 20) => get<PageResult<Student>>('/basedata/student', { page, size }),
  create: (data: Student) => post<Student>('/basedata/student', data),
  update: (id: number, data: Student) => put<Student>(`/basedata/student/${id}`, data),
  remove: (id: number) => del<void>(`/basedata/student/${id}`)
}

/** 班级学生名单。提交的是整个名单，不是逐个增删。 */
export const clazzStudentApi = {
  list: (classId: number) => get<Student[]>(`/basedata/clazz/${classId}/student`),
  set: (classId: number, studentIds: number[]) =>
    put<void>(`/basedata/clazz/${classId}/student`, studentIds)
}
