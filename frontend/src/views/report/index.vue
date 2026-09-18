<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { homeworkApi } from '../../api/homework'
const report = ref<Record<string, any>>({})
onMounted(async () => { report.value = await homeworkApi.report() })
</script>
<template><div><h3>学情报告</h3><el-row :gutter="16"><el-col :span="6"><el-statistic title="已提交作业" :value="report.submitted || 0" /></el-col><el-col :span="6"><el-statistic title="已批改" :value="report.graded || 0" /></el-col><el-col :span="6"><el-statistic title="累计得分" :value="report.score || 0" /></el-col><el-col :span="6"><el-statistic title="错题数" :value="report.wrongCount || 0" /></el-col></el-row></div></template>
