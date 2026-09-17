import { del, get, post, put, upload } from './request'
import type { PageResult } from './basedata'

/** 内容资产。文件存在服务端，前端只拿 filePath 拼 /uploads/ 访问。 */
export interface ContentAsset {
  id?: number
  name: string
  subject?: string
  grade?: string
  /** 按课表推送时按它匹配 */
  courseId?: number
  knowledgePoint?: string
  /** COURSEWARE / HANDOUT / PAPER / MEDIA */
  type: string
  /** 落盘后的相对文件名，没传文件时为空 */
  filePath?: string
  /** 改一次加一，只增不减 */
  versionNo?: number
  /** ALL / PRIVATE */
  scope: string
  /** 上传者的用户 id，由后端填，前端不传 */
  ownerId?: number
}

export const ASSET_TYPE_OPTIONS = [
  { label: '课件', value: 'COURSEWARE' },
  { label: '讲义', value: 'HANDOUT' },
  { label: '试卷', value: 'PAPER' },
  { label: '音视频', value: 'MEDIA' }
]

export const ASSET_SCOPE_OPTIONS = [
  { label: '所有人可见', value: 'ALL' },
  { label: '仅本人可见', value: 'PRIVATE' }
]

export const contentApi = {
  /** 不传 courseId 就是"我能看到的全部"，传了就是某节课用的内容 */
  page: (page = 1, size = 20, courseId?: number) =>
    get<PageResult<ContentAsset>>('/content/list', courseId ? { page, size, courseId } : { page, size }),
  create: (data: ContentAsset) => post<ContentAsset>('/content', data),
  update: (id: number, data: ContentAsset) => put<ContentAsset>(`/content/${id}`, data),
  remove: (id: number) => del<void>(`/content/${id}`),
  /** 上传文件，返回落盘后的相对文件名 */
  upload: (file: File) => upload<string>('/content/upload', file)
}
