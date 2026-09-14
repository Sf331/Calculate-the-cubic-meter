<script setup lang="ts">
/** 通用 CRUD 表格，字段配置驱动。后续几个基础数据实体都复用它。 */
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CrudApi, CrudField } from './crud'

const props = defineProps<{ fields: CrudField[]; api: CrudApi }>()

const rows = ref<any[]>([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })
const loading = ref(false)

const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref<Record<string, any>>({})

/** select 字段的选项，key 是字段名 */
const optionsMap = ref<Record<string, { label: string; value: any }[]>>({})

const optionsOf = (field: CrudField) => optionsMap.value[field.prop] ?? field.options ?? []

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
  await ElMessageBox.confirm(`确定删除「${row[props.fields[0].prop]}」吗？`, '确认', {
    type: 'warning'
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
  <div>
    <el-button type="primary" @click="openCreate">新增</el-button>

    <el-table :data="rows" v-loading="loading" border style="margin-top: 12px">
      <el-table-column
        v-for="field in fields"
        :key="field.prop"
        :label="field.label"
        :width="field.width"
      >
        <template #default="{ row }">{{ display(field, row) }}</template>
      </el-table-column>

      <el-table-column label="操作" width="140">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="primary" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="load"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="editingId === null ? '新增' : '编辑'"
      width="480px"
    >
      <el-form label-width="90px">
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
        <el-button type="primary" @click="submit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
