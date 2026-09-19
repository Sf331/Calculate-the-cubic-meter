<script setup lang="ts">
/** 通用 CRUD 表格，字段配置驱动。后续几个基础数据实体都复用它。 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CrudApi, CrudField, CrudOption, TagTone } from './crud'

const props = defineProps<{ fields: CrudField[]; api: CrudApi; title: string }>()

const rows = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<Record<string, any>>({})

/** select 字段的选项，key 是字段名 */
const optionsMap = ref<Record<string, CrudOption[]>>({})

const optionsOf = (field: CrudField) => optionsMap.value[field.prop] ?? field.options ?? []

/** 新增/编辑弹窗的标题，带上实体名，用户一眼知道在改什么 */
const dialogTitle = () => `${editingId.value === null ? '新增' : '编辑'}${props.title}`

async function load() {
  loading.value = true
  try {
    const res = await props.api.page(query.page, query.size)
    rows.value = res.records
    total.value = res.total
  } finally {
    loading.value = false
  }
}

async function loadOptions() {
  for (const field of props.fields) {
    if (field.loadOptions) {
      optionsMap.value[field.prop] = await field.loadOptions()
    }
  }
}

/** 表格里展示 select 字段时显示 label 而不是原始 id */
function display(field: CrudField, row: any) {
  const value = row[field.prop]
  if (value === null || value === undefined || value === '') return ''
  if (field.type === 'select') {
    return optionsOf(field).find((o) => o.value === value)?.label ?? value
  }
  if (field.type === 'multiselect') {
    if (!Array.isArray(value) || value.length === 0) return ''
    return value.map((v) => optionsOf(field).find((o) => o.value === v)?.label ?? v).join('、')
  }
  return value
}

/** asTag 字段：把值（或值数组）翻成一组带配色的标签 */
function tagsOf(field: CrudField, row: any): { label: string; tone: TagTone }[] {
  const value = row[field.prop]
  const values = Array.isArray(value)
    ? value
    : value === null || value === undefined || value === ''
      ? []
      : [value]
  return values.map((v) => {
    const option = optionsOf(field).find((o) => o.value === v)
    return { label: option?.label ?? String(v), tone: option?.tone ?? 'neutral' }
  })
}

function openCreate() {
  editingId.value = null
  form.value = {}
  for (const field of props.fields) {
    if (field.defaultValue !== undefined) form.value[field.prop] = field.defaultValue
    else if (field.type === 'number') form.value[field.prop] = 0
    else if (field.type === 'multiselect') form.value[field.prop] = []
    else form.value[field.prop] = undefined
  }
  openDialog()
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = { ...row }
  openDialog()
}

/** 每次开弹窗都重拉选项，否则在别的 tab 新建的下拉项这里选不到 */
async function openDialog() {
  await loadOptions()
  dialogVisible.value = true
}

async function submit() {
  for (const field of props.fields) {
    // 注意空数组是 truthy，不能只用 !value 判断
    const value = form.value[field.prop]
    const empty =
      value === undefined || value === null || value === '' || (Array.isArray(value) && !value.length)
    if (field.required && empty) {
      ElMessage.warning(`${field.label}不能为空`)
      return
    }
  }

  if (editingId.value === null) {
    await props.api.create(form.value)
    ElMessage.success('新增成功')
  } else {
    await props.api.update(editingId.value, form.value)
    ElMessage.success('修改成功')
  }
  dialogVisible.value = false
  await load()
}

async function remove(row: any) {
  // 破坏性操作的按钮写动词，不写"确定" —— 让人看清点了会发生什么
  await ElMessageBox.confirm(`「${row[props.fields[0].prop]}」删除后不可恢复。`, '删除确认', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    confirmButtonClass: 'el-button--danger'
  })
  await props.api.remove(row.id)
  ElMessage.success('已删除')
  await load()
}

onMounted(async () => {
  await loadOptions()
  await load()
})
</script>

<template>
  <div class="k-card k-card--table">
    <div class="k-toolbar">
      <span class="k-muted">共 <span class="k-num">{{ total }}</span> 条</span>
      <span class="k-spacer" />
      <el-button type="primary" @click="openCreate">新增{{ title }}</el-button>
    </div>

    <el-table
      :data="rows"
      v-loading="loading"
      stripe
      style="margin-top: var(--space-2)"
    >
      <!-- min-width 而不是 width：固定宽度会被硬裁，字号一调大表头就折行。
           注意 el-table 是按各列 min-width 的**比例**分配剩余空间的，
           所以第一列（主标识列，比如"课程名称"）必须给得比别的列大，
           否则会被挤到折行 —— 它没写 minWidth 时用下面的默认值兜底。 -->
      <el-table-column
        v-for="(field, index) in fields"
        :key="field.prop"
        :label="field.label"
        :min-width="field.minWidth ?? (index === 0 ? 240 : 140)"
        :align="field.type === 'number' ? 'right' : undefined"
      >
        <template #default="{ row }">
          <template v-if="field.asTag">
            <span
              v-for="tag in tagsOf(field, row)"
              :key="tag.label"
              class="k-tag"
              :class="`k-tag--${tag.tone}`"
            >
              {{ tag.label }}
            </span>
          </template>
          <!-- 数字、编号用等宽字体，列才对得齐 -->
          <span v-else :class="{ 'k-num': field.type === 'number' }">
            {{ display(field, row) }}
          </span>
        </template>
      </el-table-column>

      <el-table-column label="操作" min-width="160" align="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>

      <template #empty>
        <div class="k-empty">
          <p class="k-empty__text">还没有{{ title }}</p>
          <el-button type="primary" @click="openCreate">新增{{ title }}</el-button>
        </div>
      </template>
    </el-table>

    <el-pagination
      v-if="total > query.size"
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="prev, pager, next"
      style="margin-top: var(--space-2)"
      @current-change="load"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle()"
      width="400px"
      :close-on-click-modal="false"
    >
      <!-- 标签放上方：中文标签长短不一，顶部对齐比左右对齐整齐 -->
      <el-form label-position="top">
        <el-form-item v-for="field in fields" :key="field.prop" :label="field.label">
          <el-input-number
            v-if="field.type === 'number'"
            v-model="form[field.prop]"
            :min="0"
            :precision="field.precision"
            :step="field.precision ? 0.1 : 1"
            style="width: 100%"
          />
          <el-select
            v-else-if="field.type === 'select' || field.type === 'multiselect'"
            v-model="form[field.prop]"
            :multiple="field.type === 'multiselect'"
            style="width: 100%"
          >
            <el-option
              v-for="option in optionsOf(field)"
              :key="option.value"
              :label="option.label"
              :value="option.value"
            />
          </el-select>
          <el-input
            v-else
            v-model="form[field.prop]"
            :type="field.type === 'textarea' ? 'textarea' : 'text'"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>
