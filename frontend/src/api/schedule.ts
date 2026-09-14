import { del, get, post, put } from './request'

export interface ScheduleView {
  id: number
  classId: number
  className: string
  teacherId: number
  teacherName: string
  classroomId: number
  classroomName: string
  courseId: number
  courseName: string
  subject: string
  /** yyyy-MM-dd */
  lessonDate: string
  /** HH:mm:ss */
  startTime: string
  endTime: string
  locked: number
  status: string
}

/** 排不进去的班级与原因。sessionCount 是落在同一原因上的课时数。 */
export interface Conflict {
  classId: number
  className: string
  reasons: string[]
  sessionCount: number
}

export interface GenerateResult {
  placedCount: number
  conflictCount: number
  conflicts: Conflict[]
}

export interface ScheduleQuery {
  from?: string
  to?: string
  classId?: number
  teacherId?: number
  classroomId?: number
  studentId?: number
}

export interface MoveRequest {
  lessonDate: string
  /** HH:mm，结束时间由后端按课程时长算 */
  startTime: string
  classroomId?: number
}

export interface RescheduleMove {
  scheduleId: number
  className: string
  courseName: string
  teacherName: string
  fromDate: string
  fromStart: string
  fromEnd: string
  fromClassroomName: string
  toDate: string
  toStart: string
  toEnd: string
  toClassroomName: string
}

/** 一次调课的完整方案与影响面。feasible=false 时 moves 为空、reasons 说明原因。 */
export interface ReschedulePlan {
  feasible: boolean
  moves: RescheduleMove[]
  affectedClasses: string[]
  affectedTeachers: string[]
  affectedStudents: number
  reasons: string[]
}

export const scheduleApi = {
  list: (params: ScheduleQuery) => get<ScheduleView[]>('/schedule', params),
  generate: (startDate: string, weeks: number) =>
    post<GenerateResult>('/schedule/generate', { startDate, weeks }),
  clear: () => del<void>('/schedule'),
  /** 严格模式：撞到别的课就拒绝，返回被违反的约束（空数组 = 挪成功） */
  move: (id: number, data: MoveRequest) => put<string[]>(`/schedule/${id}/move`, data),
  /** dryRun=true 只出方案不落库，false 才真正落库。两次跑同一算法，方案一致 */
  reschedule: (
    id: number,
    data: MoveRequest & { dryRun: boolean }
  ) => post<ReschedulePlan>(`/schedule/${id}/reschedule`, data)
}
