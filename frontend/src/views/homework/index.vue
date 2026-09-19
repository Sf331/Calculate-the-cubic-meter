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

async function choose(h: Homework) {
  selected.value = await homeworkApi.detail(h.id!)
  answers.value = {}
}

async function submit() {
  if (!selected.value) return
  await homeworkApi.submit(
    selected.value.homework.id!,
    selected.value.questions.map((q) => ({ questionId: q.id!, answer: answers.value[q.id!] || '' }))
  )
  ElMessage.success('作业已提交')
  await choose(selected.value.homework)
}

async function refresh() {
  loading.value = true
  try {
    items.value = await homeworkApi.list()
    if (items.value.length) await choose(items.value[0])
  } finally {
    loading.value = false
  }
}

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
  await homeworkApi.create({
    classId: draft.value.classId,
    name: draft.value.name,
    questionIds: draft.value.questionIds
  })
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
  <div class="k-page k-page--fill">
    <div v-if="canEdit" class="k-card">
      <div class="k-toolbar">
        <span class="k-muted">教师端可布置作业；学生端可提交并查看自动判分结果。</span>
        <span class="k-spacer" />
        <el-button type="primary" @click="openEditor">布置作业</el-button>
      </div>
    </div>

    <div class="split k-page__fill">
      <!-- 左：作业列表 -->
      <div class="k-card" v-loading="loading">
        <div class="k-toolbar">
          <span class="k-muted">作业列表</span>
        </div>

        <div v-if="!items.length" class="k-empty">
          <p class="k-empty__text">暂无作业</p>
        </div>
        <ul v-else class="list">
          <li v-for="h in items" :key="h.id">
            <button
              type="button"
              class="list__item"
              :class="{ 'is-active': selected?.homework.id === h.id }"
              :aria-current="selected?.homework.id === h.id ? 'true' : undefined"
              @click="choose(h)"
            >
              <span>{{ h.name }}</span>
              <span class="k-num k-muted">{{ h.totalScore }}分</span>
            </button>
          </li>
        </ul>
      </div>

      <!-- 右：作业详情 -->
      <div class="k-card">
        <template v-if="selected">
          <div class="k-toolbar">
            <span class="list__title">{{ selected.homework.name }}</span>
            <span class="k-spacer" />
            <span v-if="selected.submission" class="k-tag k-tag--success">
              {{ selected.submission.status }}
            </span>
          </div>

          <el-form label-position="top" style="margin-top: var(--space-3)">
            <el-form-item v-for="(q, i) in selected.questions" :key="q.id" :label="`${i + 1}. ${q.stem}`">
              <el-input
                v-model="answers[q.id!]"
                :placeholder="q.type === 'MULTI' ? '多个选项用逗号分隔' : ''"
                :disabled="!!selected.submission"
              />
            </el-form-item>
          </el-form>

          <el-button
            v-if="!selected.submission && !canEdit"
            type="primary"
            @click="submit"
          >
            提交作业
          </el-button>
        </template>

        <div v-else class="k-empty">
          <p class="k-empty__text">请选择作业</p>
        </div>
      </div>
    </div>

    <!-- 布置作业 -->
    <el-dialog v-model="editor" title="布置作业" width="560px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="班级" required>
          <el-select v-model="draft.classId" style="width: 100%">
            <el-option v-for="item in classes" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="draft.name" />
        </el-form-item>
        <el-form-item label="题目" required>
          <el-checkbox-group v-model="draft.questionIds">
            <el-checkbox v-for="question in questions" :key="question.id" :value="question.id">
              {{ question.id }}. {{ question.stem }}（{{ question.score }}分）
            </el-checkbox>
          </el-checkbox-group>
          <p v-if="!questions.length" class="k-muted">题库为空，请先新增题目</p>
          <el-button link type="primary" @click="questionEditor = true">新增题目</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editor = false">取消</el-button>
        <el-button type="primary" @click="createHomework">保存</el-button>
      </template>
    </el-dialog>

    <!-- 新增题目 -->
    <el-dialog v-model="questionEditor" title="新增题目" width="480px" :close-on-click-modal="false">
      <el-form label-position="top">
        <el-form-item label="题型">
          <el-select v-model="questionDraft.type" style="width: 100%">
            <el-option label="单选" value="SINGLE" />
            <el-option label="多选" value="MULTI" />
            <el-option label="填空" value="BLANK" />
            <el-option label="主观题" value="SUBJECTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="questionDraft.stem" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="选项">
          <el-input v-model="questionDraft.options" placeholder="JSON 或逗号分隔，可留空" />
        </el-form-item>
        <el-form-item label="答案" required>
          <el-input v-model="questionDraft.answer" placeholder="多选可用 JSON 数组" />
        </el-form-item>
        <el-form-item label="分值">
          <el-input-number v-model="questionDraft.score" :min="0" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="questionEditor = false">取消</el-button>
        <el-button type="primary" @click="createQuestion">保存题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
/* 列表 + 详情两栏，撑满内容区高度，两栏各自内部滚动。窄屏（学生端手机）折成上下堆叠。
   原来写的是 align-items: start，两张卡片按内容收着，
   1440×900 下底下空 700px —— 空一大片不叫"简洁"，叫没填满。
   撑满高度的机制在 index.css 的 .k-page--fill / .k-page__fill 里。 */
.split {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-3);
  margin-top: var(--space-3);
}

/* 卡片自己不设 overflow 的话，子元素会把卡片撑高，flex/grid 的 min-height: 0 就白写了 */
.split > .k-card {
  min-height: 0;
  overflow: auto;
}

@media (max-width: 900px) {
  .split {
    grid-template-columns: minmax(0, 1fr);
  }

  .split > .k-card {
    overflow: visible;
  }

  /* 堆叠之后是纵向长页，硬塞进一屏会挤成两条缝 */
  .k-page--fill {
    height: auto;
  }
}

.list {
  list-style: none;
  margin: var(--space-2) 0 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.list__item {
  width: 100%;
  min-height: var(--control-h);
  padding: 0 var(--space-2);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
  border: none;
  border-radius: var(--radius-md);
  background: none;
  font-family: inherit;
  font-size: var(--text-sm);
  color: var(--label-primary);
  text-align: left;
  cursor: pointer;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.list__item:hover {
  background: var(--bg-hover);
}

/* 选中态跟侧栏一致：实心主色块 + 面上的字 */
.list__item.is-active {
  background: var(--color-primary);
  color: var(--color-primary-on);
  font-weight: 500;
}

/* 选中块里的次要说明得跟着一起转白，否则灰字压蓝底看不清 */
.list__item.is-active .k-muted {
  color: var(--color-primary-on);
}

.list__item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.list__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
}
</style>
