import { get, post } from './request'

export interface WorkhourView {
  id: number
  teacherId: number
  teacherName: string
  scheduleId: number
  className: string
  courseName: string
  workDate: string
  studentCount: number
  rate: number
  amount: number
  /** 点进签到记录的入口 */
  attendanceId: number | null
}

export interface PayslipView {
  id: number
  teacherId: number
  teacherName: string
  /** yyyy-MM */
  period: string
  totalAmount: number
  lessonCount: number
}

/** 工资单生成时冻下来的快照，之后改单价不影响它 */
export interface PayslipItem {
  scheduleId: number
  workDate: string
  className: string
  courseName: string
  studentCount: number
  rate: number
  amount: number
  attendanceId: number | null
}

export interface PayslipDetail {
  id: number
  teacherId: number
  teacherName: string
  period: string
  totalAmount: number
  items: PayslipItem[]
}

export const salaryApi = {
  /** 不传 teacherId 就是"我能看的"：教师是本人，校长是全部 */
  workhours: (params: { teacherId?: number; from?: string; to?: string }) =>
    get<WorkhourView[]>('/salary/workhour', params),
  payslips: (params: { period?: string; teacherId?: number }) =>
    get<PayslipView[]>('/salary/payslip', params),
  generate: (period: string) =>
    post<PayslipView[]>('/salary/payslip/generate', { period }),
  detail: (id: number) => get<PayslipDetail>(`/salary/payslip/${id}`)
}

/** yyyy-MM 的月初到月末，工时明细按这个区间查 */
export const monthRange = (period: string): [string, string] => {
  const [year, month] = period.split('-').map(Number)
  const last = new Date(year, month, 0).getDate()
  return [`${period}-01`, `${period}-${String(last).padStart(2, '0')}`]
}
