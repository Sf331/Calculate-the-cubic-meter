<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
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
const editingId = ref<number>()
const draftName = ref('')
type ComponentType = 'text' | 'blank' | 'single'
type EditorComponent = { id: string; type: ComponentType; text: string; prompt: string; optionsText: string; answer: string }
type EditorPage = { id: string; title: string; components: EditorComponent[] }
const draftPages = ref<EditorPage[]>([])
const canEdit = computed(() => ['PRINCIPAL', 'ACADEMIC', 'TEACHER'].includes(user.role()))
function newPage(index = 1): EditorPage {
  return {
    id: `page-${Date.now()}-${index}`,
    title: `第${index}页`,
    components: [newComponent('blank', 1)]
  }
}
function newComponent(type: ComponentType, index: number): EditorComponent {
  return {
    id: `component-${Date.now()}-${index}`,
    type,
    text: type === 'text' ? '请输入文本内容' : '',
    prompt: type === 'text' ? '' : '请输入答案',
    optionsText: type === 'single' ? '选项一\n选项二' : '',
    answer: type === 'single' ? '选项一' : ''
  }
}
function resetEditor() {
  editingId.value = undefined
  draftName.value = ''
  draftPages.value = [newPage()]
  editor.value = true
}
function edit(item: Courseware) {
  const raw = JSON.parse(item.schemaJson)
  editingId.value = item.id
  draftName.value = item.name
  draftPages.value = raw.pages.map((page: any, pageIndex: number) => ({
    id: page.id,
    title: page.title || `第${pageIndex + 1}页`,
    components: (page.components || []).map((component: any) => ({
      id: component.id,
      type: component.type === 'title' ? 'text' : component.type,
      text: component.text || component.content || '',
      prompt: component.prompt || '请输入答案',
      optionsText: Array.isArray(component.options) ? component.options.join('\n') : '',
      answer: Array.isArray(component.answer) ? component.answer[0] || '' : component.answer || ''
    }))
  }))
  editor.value = true
}
function serializedSchema() {
  return JSON.stringify({
    version: 1,
    pages: draftPages.value.map((page) => ({
      id: page.id,
      title: page.title,
      components: page.components.map((component) => ({
        id: component.id,
        type: component.type,
        ...(component.type === 'text'
          ? { text: component.text }
          : { prompt: component.prompt, answer: component.answer, ...(component.type === 'single' ? { options: component.optionsText.split('\n').map((item) => item.trim()).filter(Boolean) } : {}) })
      }))
    }))
  })
}
function addPage() { draftPages.value.push(newPage(draftPages.value.length + 1)) }
function removePage(index: number) {
  if (draftPages.value.length === 1) return ElMessage.warning('至少保留一个页面')
  draftPages.value.splice(index, 1)
}
function addComponent(page: EditorPage, type: ComponentType) { page.components.push(newComponent(type, page.components.length + 1)) }
function removeComponent(page: EditorPage, index: number) { page.components.splice(index, 1) }
function typeLabel(type: ComponentType) {
  return type === 'text' ? '文本说明' : type === 'blank' ? '填空题' : '单选题'
}
function validateDraft() {
  let interactionCount = 0
  for (const page of draftPages.value) {
    for (const component of page.components) {
      if (component.type === 'text' && !component.text.trim()) return '请填写文本内容'
      if (component.type !== 'text') interactionCount += 1
      if (component.type !== 'text' && !component.prompt.trim()) return '请填写题目或提示语'
      if (component.type !== 'text' && !component.answer.trim()) return '请填写互动题的正确答案'
      if (component.type === 'single') {
        const options = component.optionsText.split('\n').map((item) => item.trim()).filter(Boolean)
        if (options.length < 2) return '单选题至少需要两个选项'
        if (!options.includes(component.answer.trim())) return '单选题正确答案必须与选项一致'
      }
    }
  }
  if (!interactionCount) return '课件至少需要一个互动题'
  return ''
}

async function choose(item: Courseware) {
  selected.value = item
  schema.value = JSON.parse(item.schemaJson)
  answers.value = {}
  stats.value = canEdit.value ? await coursewareApi.stats(item.id!) : undefined
}
async function submit(componentId: string) {
  if (!answers.value[componentId]?.trim()) return ElMessage.warning('请先填写答案')
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
    if (!draftName.value.trim()) return ElMessage.warning('请输入课件名称')
    const draftError = validateDraft()
    if (draftError) return ElMessage.warning(draftError)
    const data = { name: draftName.value.trim(), schemaJson: serializedSchema() }
    const saved = editingId.value
      ? await coursewareApi.update(editingId.value, data)
      : await coursewareApi.create(data)
    ElMessage.success(editingId.value ? '课件已保存' : '课件已创建')
    editor.value = false
    editingId.value = undefined
    await refresh()
    await choose(saved)
  } catch { /* request helper already displays the validation message */ }
}
async function remove(item: Courseware) {
  await ElMessageBox.confirm(`「${item.name}」删除后不可恢复。`, '删除确认', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
  })
  await coursewareApi.remove(item.id!)
  ElMessage.success('课件已删除')
  if (selected.value?.id === item.id) { selected.value = undefined; schema.value = undefined }
  await refresh()
}
onMounted(refresh)
</script>

<template>
  <div class="k-page k-page--fill">
    <div class="split k-page__fill">
      <!-- 左：课件目录 -->
      <div class="k-card" v-loading="loading">
        <div class="k-toolbar">
          <span class="k-muted">课件目录</span>
          <span class="k-spacer" />
          <el-button v-if="canEdit" type="primary" @click="resetEditor">新建课件</el-button>
        </div>

        <el-form v-if="editor" label-position="top" class="editor-form" @submit.prevent="create">
          <el-form-item label="名称">
            <el-input v-model="draftName" placeholder="课件名称" />
          </el-form-item>
          <div class="editor-heading">
            <span>课件页面</span>
            <el-button text type="primary" @click="addPage">新增页面</el-button>
          </div>
          <div v-for="(page, pageIndex) in draftPages" :key="page.id" class="edit-page">
            <div class="edit-page__heading">
              <el-input v-model="page.title" placeholder="页面标题" />
              <el-button text type="danger" @click="removePage(pageIndex)">删除页面</el-button>
            </div>
            <div v-for="(component, componentIndex) in page.components" :key="component.id" class="edit-component">
              <div class="edit-component__heading">
                <span class="edit-component__type">{{ typeLabel(component.type) }}</span>
                <el-button text type="danger" @click="removeComponent(page, componentIndex)">删除</el-button>
              </div>
              <el-input v-if="component.type === 'text'" v-model="component.text" type="textarea" :rows="2" placeholder="文本内容" />
              <template v-else>
                <label class="edit-field">
                  <span>题目或提示语</span>
                  <el-input v-model="component.prompt" placeholder="例如：1+1等于多少？" />
                </label>
                <label v-if="component.type === 'single'" class="edit-field">
                  <span>选项（每行一个）</span>
                  <el-input v-model="component.optionsText" type="textarea" :rows="3" placeholder="例如：\n1\n2" />
                </label>
                <label class="edit-field">
                  <span>正确答案</span>
                  <el-input v-model="component.answer" placeholder="学生提交后按此答案判定" />
                </label>
              </template>
            </div>
            <div class="component-actions">
              <el-button text @click="addComponent(page, 'text')">+ 文本</el-button>
              <el-button text @click="addComponent(page, 'blank')">+ 填空题</el-button>
              <el-button text @click="addComponent(page, 'single')">+ 单选题</el-button>
            </div>
          </div>
          <el-button type="primary" @click="create">保存课件</el-button>
        </el-form>

        <div v-if="!items.length" class="k-empty">
          <p class="k-empty__text">暂无课件</p>
        </div>
        <ul v-else class="list">
          <li v-for="item in items" :key="item.id">
            <button
              type="button"
              class="list__item"
              :class="{ 'is-active': selected?.id === item.id }"
              :aria-current="selected?.id === item.id ? 'true' : undefined"
              @click="choose(item)"
            >
              {{ item.name }}
            </button>
            <div v-if="canEdit" class="list__actions">
              <el-button text @click.stop="edit(item)">编辑</el-button>
              <el-button text type="danger" @click.stop="remove(item)">删除</el-button>
            </div>
          </li>
        </ul>
      </div>

      <!-- 右：课件内容 -->
      <div class="k-card">
        <template v-if="selected">
          <div class="k-toolbar">
            <span class="list__title">{{ selected.name }}</span>
            <span class="k-spacer" />
            <span v-if="stats" class="k-muted k-num">
              互动 {{ stats.submissions }} 次 · 正确 {{ stats.correct }} 次 · {{ stats.students }} 位学生
            </span>
          </div>

          <div v-if="!schema?.pages?.length" class="k-empty">
            <p class="k-empty__text">课件没有页面</p>
          </div>

          <section v-for="page in schema?.pages" :key="page.id" class="page">
            <h2 class="page__title">{{ page.title || page.id }}</h2>
            <div v-for="component in page.components" :key="component.id" class="component">
              <p v-if="component.type === 'text' || component.type === 'title'">
                {{ component.text || component.content }}
              </p>
              <el-input
                v-else-if="component.type === 'blank'"
                v-model="answers[component.id]"
                :placeholder="component.prompt || '请输入答案'"
                :aria-label="component.prompt || '请输入答案'"
                @keyup.enter="submit(component.id)"
              >
                <template #append>
                  <el-button @click="submit(component.id)">提交</el-button>
                </template>
              </el-input>
              <el-radio-group v-else-if="component.type === 'single'" v-model="answers[component.id]">
                <el-radio v-for="option in component.options" :key="option" :label="option">{{ option }}</el-radio>
                <el-button type="primary" @click="submit(component.id)">提交</el-button>
              </el-radio-group>
            </div>
          </section>
        </template>

        <div v-else class="k-empty">
          <p class="k-empty__text">请选择课件</p>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 目录 + 内容两栏。撑满内容区高度，两栏各自内部滚动。
   原来写的是 align-items: start，两张卡片按内容收着，
   1440×900 下底下空 700px —— 空一大片不叫"简洁"，叫没填满。
   撑满高度的机制在 index.css 的 .k-page--fill / .k-page__fill 里。 */
.split {
  display: grid;
  grid-template-columns: 320px minmax(0, 1fr);
  gap: var(--space-3);
}

/* 卡片自己不设 overflow 的话，子元素会把卡片撑高，flex/grid 的 min-height: 0 就白写了 */
.split > .k-card {
  min-height: 0;
  overflow: auto;
}

/* 窄屏折成上下堆叠，并且不再压高度 —— 堆叠之后是纵向长页，硬塞进一屏会挤成两条缝 */
@media (max-width: 900px) {
  .split {
    grid-template-columns: minmax(0, 1fr);
  }

  .split > .k-card {
    overflow: visible;
  }

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

.list__item:focus-visible {
  outline: 2px solid var(--color-primary);
  outline-offset: -2px;
}

.list li {
  display: flex;
  align-items: center;
}

.list__actions {
  display: flex;
  flex: 0 0 auto;
  gap: var(--space-1);
}

.editor-form {
  margin-top: var(--space-3);
}

.editor-heading,
.edit-page__heading,
.edit-component__heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-2);
}

.editor-heading {
  margin-bottom: var(--space-2);
  font-weight: 600;
}

.edit-page {
  padding: var(--space-2);
  margin-bottom: var(--space-2);
  border: 1px solid var(--separator);
  border-radius: var(--radius-md);
}

.edit-component {
  display: grid;
  gap: var(--space-2);
  padding: var(--space-2) 0;
  border-bottom: 1px solid var(--separator);
}

.edit-component__heading {
  justify-content: flex-start;
}

.edit-component__type {
  font-weight: 600;
  color: var(--label-primary);
}

.edit-field {
  display: grid;
  gap: var(--space-1);
  color: var(--label-secondary);
  font-size: var(--text-xs);
}

.component-actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-1);
  padding-top: var(--space-1);
}

.list__title {
  font-size: var(--text-sm);
  font-weight: 600;
  color: var(--label-primary);
}

.page {
  padding: var(--space-3) 0;
  border-bottom: 1px solid var(--separator);
}

.page:last-child {
  border-bottom: none;
  padding-bottom: 0;
}

/* 区块标题用 --text-md(21px)，不是 --text-sm —— 用正文那一档就没有层级了，
   课件页里这一行是"第几页"的分节，得跟下面的题干拉开。 */
.page__title {
  font-size: var(--text-md);
  font-weight: 600;
  color: var(--label-primary);
  margin-bottom: var(--space-2);
}

.component {
  margin: var(--space-3) 0;
  font-size: var(--text-sm);
  color: var(--label-primary);
}
</style>
