import { del, get, post, put } from './request'

export interface Courseware {
  id?: number
  name: string
  schemaJson: string
  createdAt?: string
}
export interface CoursewareAnswer {
  scheduleId?: number
  componentId: string
  answer?: string
}
export interface CoursewareStats {
  submissions: number
  correct: number
  students: number
}
export const coursewareApi = {
  list: () => get<Courseware[]>('/courseware'),
  get: (id: number) => get<Courseware>(`/courseware/${id}`),
  create: (data: Courseware) => post<Courseware>('/courseware', data),
  update: (id: number, data: Courseware) => put<Courseware>(`/courseware/${id}`, data),
  remove: (id: number) => del<void>(`/courseware/${id}`),
  answer: (id: number, data: CoursewareAnswer) => post('/courseware/' + id + '/answers', data),
  stats: (id: number) => get<CoursewareStats>(`/courseware/${id}/stats`)
}
