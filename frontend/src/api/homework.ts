import { get, post, put } from './request'
export interface Question { id?: number; stem: string; type: string; options?: string; answer?: string; score?: number; knowledgePoint?: string }
export interface Homework { id?: number; classId: number; name: string; questionIds: string; totalScore?: number; dueTime?: string }
export interface HomeworkDetail { homework: Homework; questions: Question[]; submission?: any; answers?: any[] }
export const homeworkApi = {
  list: () => get<Homework[]>('/homework/list'),
  detail: (id: number) => get<HomeworkDetail>(`/homework/${id}`),
  questions: () => get<Question[]>('/homework/questions'),
  createQuestion: (q: Question) => post<Question>('/homework/questions', q),
  create: (h: { classId: number; name: string; questionIds: number[]; dueTime?: string }) => post<Homework>('/homework', h),
  submit: (id: number, answers: { questionId: number; answer: string }[]) => post(`/homework/${id}/submit`, { answers }),
  wrongBook: () => get<any[]>('/homework/wrong-book'),
  report: () => get<Record<string, any>>('/homework/report')
}
