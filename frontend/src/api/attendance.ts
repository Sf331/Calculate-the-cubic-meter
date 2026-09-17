import { get, post } from './request'
import type { ScheduleView } from './schedule'

/** 点名页的一行 */
export interface SessionStudent {
  studentId: number
  studentName: string
  /** 已点过名的状态，没点过是 null */
  status: string | null
  consumedHours: number | null
  /** 这门课还剩多少课时。没有账户时为 null */
  remainingHours: number | null
  unitPrice: number | null
}

export interface SessionView {
  scheduleId: number
  classId: number
  className: string
  courseId: number
  courseName: string
  teacherId: number
  teacherName: string
  lessonDate: string
  startTime: string
  endTime: string
  status: string
  students: SessionStudent[]
}

/** 一次点名后"三处联动"的数字 */
export interface ConsumeResult {
  scheduleId: number
  attendanceCount: number
  consumedHours: number
  confirmedAmount: number
  workhourAmount: number
}

export interface AttendanceItem {
  studentId: number
  status: string
}

export const STATUS_OPTIONS = [
  { label: '出勤', value: 'PRESENT' },
  { label: '迟到', value: 'LATE' },
  { label: '早退', value: 'EARLY_LEAVE' },
  { label: '请假', value: 'LEAVE' },
  { label: '旷课', value: 'ABSENT' }
]

export const attendanceApi = {
  /** 可点名的课次。教师只拿得到自己的课，过滤在后端 */
  lessons: (params: { from?: string; to?: string }) =>
    get<ScheduleView[]>('/attendance/lessons', params),
  session: (scheduleId: number) => get<SessionView>(`/attendance/${scheduleId}`),
  submit: (scheduleId: number, items: AttendanceItem[]) =>
    post<ConsumeResult>(`/attendance/${scheduleId}`, { items })
}

export interface LessonAccountView {
  id: number
  studentId: number
  studentName: string
  courseId: number
  courseName: string
  totalHours: number
  consumedHours: number
  remainingHours: number
  /** 课时单价 = 已收费总额 / 已购课时数 */
  unitPrice: number
}

export interface LessonTransaction {
  id: number
  /** RECHARGE / CONSUME / REFUND / GIFT / ADJUST */
  type: string
  /** 带正负号 */
  hours: number
  balanceAfter: number
  refId: number | null
  remark: string
  createdAt: string
}

export const TRANSACTION_LABEL: Record<string, string> = {
  RECHARGE: '充值',
  CONSUME: '消耗',
  REFUND: '退费',
  GIFT: '赠送',
  ADJUST: '调整'
}

export const lessonAccountApi = {
  /** 不传 studentId 就是"我能看的全部"：学生是本人，家长是孩子，校长教务是全部 */
  list: (studentId?: number) =>
    get<LessonAccountView[]>('/lesson-account', studentId ? { studentId } : undefined),
  transactions: (id: number) => get<LessonTransaction[]>(`/lesson-account/${id}/transaction`)
}
