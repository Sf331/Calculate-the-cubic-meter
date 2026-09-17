/** CrudTable.vue 的配置类型。抽出来是因为 <script setup> 里不能写 ES 模块导出。 */

export interface CrudField {
  prop: string
  label: string
  /** 不填按 text 处理 */
  type?: 'text' | 'textarea' | 'number' | 'select' | 'multiselect' | 'file'
  /** 静态选项 */
  options?: { label: string; value: any }[]
  /** 远程选项，例如"所属校区"下拉。挂载时加载一次 */
  loadOptions?: () => Promise<{ label: string; value: any }[]>
  required?: boolean
  width?: number
  /** number 类型专用：小数位数。金额填 2，人数不填 */
  precision?: number
  /** 新增时的初始值。不填则 number 为 0、multiselect 为 []、其余为空 */
  defaultValue?: any
  /** file 类型专用：上传接口路径，例如 '/content/upload' */
  uploadUrl?: string
  /** 只读。用于后端自己算出来的字段（例如版本号），让它在弹窗里看得见但改不了 */
  readonly?: boolean
}

export interface CrudApi {
  page: (page: number, size: number) => Promise<{ records: any[]; total: number }>
  create: (data: any) => Promise<any>
  update: (id: number, data: any) => Promise<any>
  remove: (id: number) => Promise<void>
}
