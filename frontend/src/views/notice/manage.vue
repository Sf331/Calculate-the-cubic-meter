<script setup lang="ts">
/**
 * 校长 / 教务 / 教师端的「发布通知」：上面填写并发布，下面看已发布的全部通知。
 * 通知存在后端内存里，重启即清空 —— 页面不提示这个，demo 里没必要。
 */
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { AUDIENCE_LABEL, AUDIENCE_OPTIONS, noticeApi } from '../../api/notice'
import type { Notice } from '../../api/notice'

const notices = ref<Notice[]>([])
const loading = ref(false)
const publishing = ref(false)

/** 发布表单。发布者和发布时间由后端填，这里不传。 */
const draft = ref<Notice>({ title: '', content: '', audience: 'ALL' })

const timeText = (time?: string) => (time ? time.replace('T', ' ').slice(0, 16) : '')

async function load() {
  loading.value = true
  try {
    notices.value = await noticeApi.list()
  } finally {
    loading.value = false
  }
}

async function publish() {
  if (!draft.value.title.trim()) {
    ElMessage.warning('请填写通知标题')
    return
  }

  publishing.value = true
  try {
    await noticeApi.publish({
      title: draft.value.title.trim(),
      content: draft.value.content?.trim() ?? '',
      audience: draft.value.audience
    })
    ElMessage.success('通知已发布')
    draft.value = { title: '', content: '', audience: 'ALL' }
    await load()
  } finally {
    publishing.value = false
  }
}

async function remove(row: Notice) {
  await ElMessageBox.confirm(`「${row.title}」撤回后学生和家长就看不到了。`, '撤回确认', {
    confirmButtonText: '撤回',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await noticeApi.remove(row.id!)
  ElMessage.success('通知已撤回')
  await load()
}

onMounted(load)
</script>

<template>
  <div class="k-page">
    <div class="k-card">
      <div class="k-toolbar">
        <span class="k-muted">发布后，学生和家长在手机端「通知」里就能看到。</span>
      </div>

      <el-form label-position="top" style="margin-top: var(--space-3)" @submit.prevent>
        <el-form-item label="标题" required>
          <el-input v-model="draft.title" maxlength="200" placeholder="例如：国庆假期停课安排" />
        </el-form-item>

        <el-form-item label="内容">
          <el-input
            v-model="draft.content"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="写清楚时间、地点和要带的东西"
          />
        </el-form-item>

        <el-form-item label="面向">
          <el-radio-group v-model="draft.audience">
            <el-radio-button v-for="option in AUDIENCE_OPTIONS" :key="option.value" :value="option.value">
              {{ option.label }}
            </el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-button type="primary" :loading="publishing" @click="publish">发布通知</el-button>
      </el-form>
    </div>

    <div class="k-card k-card--table">
      <div class="k-toolbar">
        <span class="k-muted">已发布 <span class="k-num">{{ notices.length }}</span> 条</span>
        <span class="k-spacer" />
      </div>

      <el-table :data="notices" v-loading="loading" stripe style="margin-top: var(--space-2)">
        <!-- min-width 加起来是预算：1440 屏下内容区约 1186px，超了就会横向滚动 -->
        <el-table-column prop="title" label="标题" min-width="220" />
        <el-table-column prop="content" label="内容" min-width="320" />
        <el-table-column label="面向" min-width="120">
          <template #default="{ row }">{{ AUDIENCE_LABEL[row.audience] ?? row.audience }}</template>
        </el-table-column>
        <el-table-column prop="publisherName" label="发布人" min-width="120" />
        <el-table-column label="发布时间" min-width="160">
          <template #default="{ row }"><span class="k-num">{{ timeText(row.createdAt) }}</span></template>
        </el-table-column>
        <el-table-column label="操作" min-width="110" align="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="remove(row)">撤回</el-button>
          </template>
        </el-table-column>

        <template #empty>
          <div class="k-empty">
            <p class="k-empty__text">暂无通知</p>
          </div>
        </template>
      </el-table>
    </div>
  </div>
</template>
