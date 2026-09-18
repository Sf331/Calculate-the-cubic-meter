<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { coursewareApi, type Courseware, type CoursewareStats } from '../../api/courseware'
import { useUserStore } from '../../stores/user'

const user = useUserStore()
const items = ref<Courseware[]>([])
const selected = ref<Courseware>()
const schema = ref<any>()
const answers = ref<Record<string, string>>({})
const stats = ref<CoursewareStats>()
const loading = ref(false)
const editor = ref(false)
const draftName = ref('')
const draftJson = ref('{"version":1,"pages":[{"id":"page-1","title":"第一页","components":[{"id":"q-1","type":"blank","prompt":"请输入答案","answer":""}]}]}')
const canEdit = computed(() => ['PRINCIPAL', 'ACADEMIC', 'TEACHER'].includes(user.role()))

async function choose(item: Courseware) {
  selected.value = item
  schema.value = JSON.parse(item.schemaJson)
  answers.value = {}
  stats.value = canEdit.value ? await coursewareApi.stats(item.id!) : undefined
}
async function submit(componentId: string) {
  await coursewareApi.answer(selected.value!.id!, { componentId, answer: answers.value[componentId] })
  ElMessage.success('答案已提交')
}
async function refresh() {
  loading.value = true
  try { items.value = await coursewareApi.list(); if (items.value.length && !selected.value) await choose(items.value[0]) }
  finally { loading.value = false }
}
async function create() {
  try {
    const created = await coursewareApi.create({ name: draftName.value, schemaJson: draftJson.value })
    ElMessage.success('课件已创建')
    editor.value = false
    draftName.value = ''
    await refresh()
    await choose(created)
  } catch { /* request helper already displays the validation message */ }
}
onMounted(refresh)
</script>

<template>
  <div class="courseware">
    <h3>交互课件</h3>
    <el-row :gutter="16">
      <el-col :span="7">
        <el-card v-loading="loading">
          <template #header><span>课件目录</span><el-button v-if="canEdit" link type="primary" @click="editor = !editor">新建</el-button></template>
          <el-form v-if="editor" label-position="top" @submit.prevent="create">
            <el-form-item label="名称"><el-input v-model="draftName" placeholder="课件名称" /></el-form-item>
            <el-form-item label="JSON"><el-input v-model="draftJson" type="textarea" :rows="7" /></el-form-item>
            <el-button type="primary" @click="create">保存课件</el-button>
          </el-form>
          <el-empty v-if="!items.length" description="暂无课件" />
          <el-menu v-else :default-active="String(selected?.id)" @select="id => choose(items.find(i => String(i.id) === id)!)">
            <el-menu-item v-for="item in items" :key="item.id" :index="String(item.id)">{{ item.name }}</el-menu-item>
          </el-menu>
        </el-card>
      </el-col>
      <el-col :span="17">
        <el-card v-if="selected">
          <template #header><b>{{ selected.name }}</b><span v-if="stats" class="stats">互动 {{ stats.submissions }} 次 · 正确 {{ stats.correct }} 次 · {{ stats.students }} 位学生</span></template>
          <el-empty v-if="!schema?.pages?.length" description="课件没有页面" />
          <section v-for="page in schema?.pages" :key="page.id" class="page">
            <h4>{{ page.title || page.id }}</h4>
            <div v-for="component in page.components" :key="component.id" class="component">
              <p v-if="component.type === 'text' || component.type === 'title'">{{ component.text || component.content }}</p>
              <el-input v-else v-model="answers[component.id]" :placeholder="component.prompt || '请输入答案'" @keyup.enter="submit(component.id)">
                <template #append><el-button @click="submit(component.id)">提交</el-button></template>
              </el-input>
            </div>
          </section>
        </el-card>
        <el-empty v-else description="请选择课件" />
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page { padding: 12px 0; border-bottom: 1px solid var(--el-border-color-lighter); }
.component { margin: 12px 0; }
.stats { float: right; color: var(--el-text-color-secondary); font-size: 13px; font-weight: normal; }
</style>
