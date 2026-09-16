import axios from 'axios'
import { ElMessage } from 'element-plus'

/** 后端统一响应体，见 backend 的 com.kelifang.common.Result */
interface Result<T> {
  code: number
  msg: string
  data: T
}

const instance = axios.create({
  // 与后端同源，靠 vite.config.ts 的 proxy 转发到 8080。
  // 没有需要按环境改的理由，所以不走环境变量。
  baseURL: '/api',
  timeout: 30000
})

function unwrap<T>(body: Result<T>): T {
  if (body.code === 0) {
    return body.data
  }
  if (body.code === 401) {
    // 直接跳转，不用 router，避免和 router → views → api 形成循环依赖
    window.location.href = '/login'
    return Promise.reject(new Error('未登录')) as never
  }
  ElMessage.error(body.msg)
  return Promise.reject(new Error(body.msg)) as never
}

export const get = <T>(url: string, params?: object): Promise<T> =>
  instance.get<Result<T>>(url, { params }).then((r) => unwrap(r.data))

export const post = <T>(url: string, data?: object): Promise<T> =>
  instance.post<Result<T>>(url, data).then((r) => unwrap(r.data))

export const put = <T>(url: string, data?: object): Promise<T> =>
  instance.put<Result<T>>(url, data).then((r) => unwrap(r.data))

export const del = <T>(url: string): Promise<T> =>
  instance.delete<Result<T>>(url).then((r) => unwrap(r.data))

/** 文件上传用，返回后端传来的相对路径 */
export const upload = <T>(url: string, file: File): Promise<T> => {
  const form = new FormData()
  form.append('file', file)
  return instance
    .post<Result<T>>(url, form, { headers: { 'Content-Type': 'multipart/form-data' } })
    .then((r) => unwrap(r.data))
}
