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
  <div class="k-page k-page--fill">
    <div class="split k-page__fill">
      <!-- 左：课件目录 -->
      <div class="k-card" v-loading="loading">
        <div class="k-toolbar">
          <span class="k-muted">课件目录</span>
          <span class="k-spacer" />
          <el-button v-if="canEdit" @click="editor = !editor">
            {{ editor ? '收起' : '新建' }}
          </el-button>
        </div>

        <el-form v-if="editor" label-position="top" style="margin-top: var(--space-3)" @submit.prevent="create">
          <el-form-item label="名称">
            <el-input v-model="draftName" placeholder="课件名称" />
          </el-form-item>
          <el-form-item label="JSON">
            <el-input v-model="draftJson" type="textarea" :rows="7" />
          </el-form-item>
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
                v-else
                v-model="answers[component.id]"
                :placeholder="component.prompt || '请输入答案'"
                :aria-label="component.prompt || '请输入答案'"
                @keyup.enter="submit(component.id)"
              >
                <template #append>
                  <el-button @click="submit(component.id)">提交</el-button>
                </template>
              </el-input>
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
