import { get } from './request'

export interface FundTransaction {
  id: number
  studentId: number | null
  studentName: string | null
  /** PRE_RECEIVE / RECEIVE_CONFIRM / REFUND / EXPENSE */
  type: string
  amount: number
  /** IN / OUT */
  direction: string
  refId: number | null
  occurDate: string
  remark: string
}

export const FUND_TYPE_LABEL: Record<string, string> = {
  PRE_RECEIVE: '预收款',
  RECEIVE_CONFIRM: '确认收入',
  REFUND: '退费',
  EXPENSE: '支出'
}

/** 预收台账：收了 / 确认了 / 还挂着 */
export interface PreReceiveRow {
  studentId: number
  studentName: string
  received: number
  confirmed: number
  balance: number
}

/** 课时结转：期初 + 充值 − 消耗 = 期末 */
export interface LessonCarryRow {
  studentId: number
  studentName: string
  courseId: number
  courseName: string
  period: string
  opening: number
  recharged: number
  consumed: number
  closing: number
}

export interface TeacherCostRow {
  teacherId: number
  teacherName: string
  lessonCount: number
  studentCount: number
  amount: number
}

export interface ClassProfitRow {
  classId: number
  className: string
  courseName: string
  revenue: number
  cost: number
  profit: number
}

export interface DailyPoint {
  date: string
  revenue: number
  hours: number
}

export interface DashboardView {
  period: string
  confirmedRevenue: number
  consumedHours: number
  preReceiveBalance: number
  teacherCost: number
  lessonCount: number
  presentCount: number
  studentCount: number
  teacherCount: number
  classCount: number
  daily: DailyPoint[]
}

export const financeApi = {
  transactions: (params: { studentId?: number; from?: string; to?: string; type?: string }) =>
    get<FundTransaction[]>('/finance/transaction', params),
  preReceive: (period: string) =>
    get<PreReceiveRow[]>('/finance/report/pre-receive', { period }),
  lessonCarry: (period: string) =>
    get<LessonCarryRow[]>('/finance/report/lesson-carry', { period }),
  teacherCost: (period: string) =>
    get<TeacherCostRow[]>('/finance/report/teacher-cost', { period }),
  classProfit: (period: string) =>
    get<ClassProfitRow[]>('/finance/report/class-profit', { period }),
  dashboard: (period: string) => get<DashboardView>('/dashboard', { period })
}
