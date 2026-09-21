import { del, get, post } from './request'

/** 站内通知。后端存在内存里，重启即清空。 */
export interface Notice {
  id?: number
  title: string
  content?: string
  /** ALL=全体（学生 + 家长） / STUDENT=仅学生 / PARENT=仅家长 */
  audience: string
  /** 发布者，由后端按登录态填，前端不传 */
  publisherName?: string
  publisherRole?: string
  createdAt?: string
}

export const AUDIENCE_OPTIONS = [
  { label: '全体', value: 'ALL' },
  { label: '仅学生', value: 'STUDENT' },
  { label: '仅家长', value: 'PARENT' }
]

export const AUDIENCE_LABEL: Record<string, string> = {
  ALL: '全体',
  STUDENT: '仅学生',
  PARENT: '仅家长'
}

export const noticeApi = {
  /** 发布方拿到全部通知，学生 / 家长只拿到发给自己的 */
  list: () => get<Notice[]>('/notice/list'),
  publish: (data: Notice) => post<Notice>('/notice', data),
  remove: (id: number) => del<void>(`/notice/${id}`)
}
