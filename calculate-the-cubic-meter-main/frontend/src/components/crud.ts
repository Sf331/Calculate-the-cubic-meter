/** CrudTable.vue 的配置类型。抽出来是因为 <script setup> 里不能写 ES 模块导出。 */

/** 状态标签的配色，对应 index.css 的 .k-tag--* */
export type TagTone = 'info' | 'success' | 'warning' | 'danger' | 'neutral'

export interface CrudOption {
  label: string
  value: any
  /**
   * 只在这个字段用 asTag 展示时生效。颜色是给"要一眼扫出来的业务状态"用的
   * （试听课、待补课…），纯粹的分类值不要填 —— 那是装饰，不是信息。
   */
  tone?: TagTone
}

export interface CrudField {
  prop: string
  label: string
  /** 不填按 text 处理 */
  type?: 'text' | 'textarea' | 'number' | 'select' | 'multiselect' | 'file'
  /** 静态选项 */
  options?: CrudOption[]
  /** 远程选项，例如"所属校区"下拉。挂载时加载一次 */
  loadOptions?: () => Promise<CrudOption[]>
  /** 表格里渲染成状态标签而不是纯文本。multiselect 会渲染成一排标签 */
  asTag?: boolean
  required?: boolean
  /**
   * 列的最小宽度。**优先用这个，不要用固定宽度** —— 固定宽度会让 Element Plus 硬裁，
   * 字号一调大表头就折行（「单次时长(分)」被挤成两行就是这么来的）。
   * 给最小宽度，列自己会撑开，调字号时不用回来改一遍。
   */
  minWidth?: number
  /**
   * 固定列宽。只在"这一列内容长度恒定、且就是要窄"时才用（比如只放一个版本号）。
   * 其余情况一律用 minWidth。
   */
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
