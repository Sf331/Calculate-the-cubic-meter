<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { homeworkApi, type Homework, type HomeworkDetail, type Question } from '../../api/homework'
import { useUserStore } from '../../stores/user'
import { clazzApi } from '../../api/basedata'
const user = useUserStore()
const items = ref<Homework[]>([])
const selected = ref<HomeworkDetail>()
const answers = ref<Record<number, string>>({})
const loading = ref(false)
const canEdit = ['PRINCIPAL', 'ACADEMIC', 'TEACHER'].includes(user.role())
const editor = ref(false)
const classes = ref<{ id?: number; name: string }[]>([])
const questions = ref<Question[]>([])
const draft = ref({ classId: undefined as number | undefined, name: '', questionIds: [] as number[] })
const questionEditor = ref(false)
const questionDraft = ref<Question>({ stem: '', type: 'SINGLE', options: '', answer: '', score: 10 })
async function choose(h: Homework) { selected.value = await homeworkApi.detail(h.id!); answers.value = {} }
async function submit() {
  if (!selected.value) return
  await homeworkApi.submit(selected.value.homework.id!, selected.value.questions.map(q => ({ questionId: q.id!, answer: answers.value[q.id!] || '' })))
  ElMessage.success('作业已提交'); await choose(selected.value.homework)
}
async function refresh() { loading.value = true; try { items.value = await homeworkApi.list(); if (items.value.length) await choose(items.value[0]) } finally { loading.value = false } }
async function openEditor() {
  classes.value = (await clazzApi.page(1, 200)).records
  questions.value = await homeworkApi.questions()
  editor.value = true
}
async function createHomework() {
  if (!draft.value.classId || !draft.value.name.trim() || !draft.value.questionIds.length) {
    ElMessage.warning('请填写班级、作业名称并至少选择一道题')
    return
  }
  await homeworkApi.create({ classId: draft.value.classId, name: draft.value.name, questionIds: draft.value.questionIds })
  ElMessage.success('作业已布置')
  editor.value = false
  draft.value = { classId: undefined, name: '', questionIds: [] }
  await refresh()
}
async function createQuestion() {
  if (!questionDraft.value.stem.trim() || !questionDraft.value.answer?.trim()) {
    ElMessage.warning('请填写题干和答案')
    return
  }
  await homeworkApi.createQuestion(questionDraft.value)
  ElMessage.success('题目已加入题库')
  questionEditor.value = false
  questionDraft.value = { stem: '', type: 'SINGLE', options: '', answer: '', score: 10 }
  questions.value = await homeworkApi.questions()
}
onMounted(refresh)
</script>
<template>
  <div><h3>电子作业</h3><el-alert v-if="canEdit" title="教师端可布置作业；学生端可提交并查看自动判分结果。" type="info" :closable="false" style="margin-bottom: 12px" /><el-button v-if="canEdit" type="primary" @click="openEditor">布置作业</el-button>
    <el-dialog v-model="editor" title="布置作业" width="620px"><el-form label-width="90px"><el-form-item label="班级"><el-select v-model="draft.classId" style="width: 100%"><el-option v-for="item in classes" :key="item.id" :label="item.name" :value="item.id" /></el-select></el-form-item><el-form-item label="名称"><el-input v-model="draft.name" /></el-form-item><el-form-item label="题目"><div style="width: 100%"><el-checkbox-group v-model="draft.questionIds"><el-checkbox v-for="question in questions" :key="question.id" :value="question.id">{{ question.id }}. {{ question.stem }}（{{ question.score }}分）</el-checkbox></el-checkbox-group><el-empty v-if="!questions.length" description="题库为空，请先新增题目" /><el-button link type="primary" @click="questionEditor = true">新增题目</el-button></div></el-form-item></el-form><template #footer><el-button @click="editor = false">取消</el-button><el-button type="primary" @click="createHomework">保存</el-button></template></el-dialog>
    <el-dialog v-model="questionEditor" title="新增题目" width="520px"><el-form label-width="80px"><el-form-item label="题型"><el-select v-model="questionDraft.type"><el-option label="单选" value="SINGLE" /><el-option label="多选" value="MULTI" /><el-option label="填空" value="BLANK" /><el-option label="主观题" value="SUBJECTIVE" /></el-select></el-form-item><el-form-item label="题干"><el-input v-model="questionDraft.stem" type="textarea" /></el-form-item><el-form-item label="选项"><el-input v-model="questionDraft.options" placeholder="JSON 或逗号分隔，可留空" /></el-form-item><el-form-item label="答案"><el-input v-model="questionDraft.answer" placeholder="多选可用 JSON 数组" /></el-form-item><el-form-item label="分值"><el-input-number v-model="questionDraft.score" :min="0" /></el-form-item></el-form><template #footer><el-button @click="questionEditor = false">取消</el-button><el-button type="primary" @click="createQuestion">保存题目</el-button></template></el-dialog>
    <el-row :gutter="16" style="margin-top: 12px"><el-col :span="8"><el-card v-loading="loading"><template #header>作业列表</template><el-empty v-if="!items.length" description="暂无作业" /><el-menu v-else :default-active="String(items[0].id)" @select="id => choose(items.find(x => String(x.id) === id)!)"><el-menu-item v-for="h in items" :key="h.id" :index="String(h.id)">{{ h.name }}<small>（{{ h.totalScore }}分）</small></el-menu-item></el-menu></el-card></el-col>
    <el-col :span="16"><el-card v-if="selected"><template #header><b>{{ selected.homework.name }}</b><el-tag v-if="selected.submission" type="success" style="float:right">{{ selected.submission.status }}</el-tag></template><el-form label-position="top"><el-form-item v-for="(q, i) in selected.questions" :key="q.id" :label="`${i + 1}. ${q.stem}`"><el-input v-if="q.type !== 'MULTI'" v-model="answers[q.id!]" :disabled="!!selected.submission" /><el-input v-else v-model="answers[q.id!]" placeholder="多个选项用逗号分隔" :disabled="!!selected.submission" /></el-form-item><el-button v-if="!selected.submission && !canEdit" type="primary" @click="submit">提交作业</el-button></el-form></el-card><el-empty v-else description="请选择作业" /></el-col></el-row></div>
</template>
