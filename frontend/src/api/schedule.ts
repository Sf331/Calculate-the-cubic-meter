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

export const scheduleApi = {
  list: (params: ScheduleQuery) => get<ScheduleView[]>('/schedule', params),
  generate: (startDate: string, weeks: number) =>
    post<GenerateResult>('/schedule/generate', { startDate, weeks }),
  clear: () => del<void>('/schedule'),
  /** 返回空数组表示挪成功，非空表示被拒且每条就是原因 */
  move: (id: number, data: MoveRequest) => put<string[]>(`/schedule/${id}/move`, data)
}
