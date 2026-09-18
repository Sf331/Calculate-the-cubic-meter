<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { homeworkApi } from '../../api/homework'
const rows = ref<any[]>([])
onMounted(async () => { rows.value = await homeworkApi.wrongBook() })
</script>
<template><div><h3>错题本</h3><el-card><el-table :data="rows" stripe><el-table-column prop="questionId" label="题目 ID" /><el-table-column prop="knowledgePoint" label="知识点" /><el-table-column prop="wrongCount" label="错误次数" /><el-table-column prop="lastWrongTime" label="最近错误" /></el-table><el-empty v-if="!rows.length" description="还没有错题，继续保持" /></el-card></div></template>
