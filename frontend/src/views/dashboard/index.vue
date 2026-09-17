<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { financeApi } from '../../api/finance'
import type { DashboardView } from '../../api/finance'

const thisMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}

const period = ref(thisMonth())
const data = ref<DashboardView>()
const loading = ref(false)

const chartEl = ref<HTMLDivElement>()
let chart: echarts.ECharts | undefined

function renderChart() {
  if (!chart || !data.value) return
  const daily = data.value.daily
  chart.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['确认收入', '课时消耗'] },
    grid: { left: 60, right: 60, top: 40, bottom: 40 },
    xAxis: { type: 'category', data: daily.map((p) => p.date.slice(5)) },
    yAxis: [
      { type: 'value', name: '收入(元)' },
      { type: 'value', name: '课时' }
    ],
    series: [
      { name: '确认收入', type: 'line', data: daily.map((p) => p.revenue) },
      { name: '课时消耗', type: 'line', yAxisIndex: 1, data: daily.map((p) => p.hours) }
    ]
  })
}

async function load() {
  loading.value = true
  try {
    data.value = await financeApi.dashboard(period.value)
    renderChart()
  } finally {
    loading.value = false
  }
}

const onResize = () => chart?.resize()

onMounted(async () => {
  await load()
  if (chartEl.value) {
    chart = echarts.init(chartEl.value)
    renderChart()
  }
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  chart?.dispose()
})
</script>

<template>
  <div v-loading="loading">
    <h3>经营看板</h3>

    <el-form inline>
      <el-form-item label="月份">
        <el-date-picker
          v-model="period"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          @change="load"
        />
      </el-form-item>
      <el-form-item>
        <span>点一次签到，这里的数字当场就变。</span>
      </el-form-item>
    </el-form>

    <el-descriptions :column="4" border>
      <el-descriptions-item label="确认收入（元）">
        {{ data?.confirmedRevenue ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="课时消耗">
        {{ data?.consumedHours ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="教师成本（元）">
        {{ data?.teacherCost ?? '-' }}
      </el-descriptions-item>
      <el-descriptions-item label="预收余额（元）">
        {{ data?.preReceiveBalance ?? '-' }}
      </el-descriptions-item>

      <el-descriptions-item label="已上课次">{{ data?.lessonCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="出勤人次">{{ data?.presentCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="在读学员">{{ data?.studentCount ?? '-' }}</el-descriptions-item>
      <el-descriptions-item label="教师 / 班级">
        {{ data?.teacherCount ?? '-' }} / {{ data?.classCount ?? '-' }}
      </el-descriptions-item>
    </el-descriptions>

    <div ref="chartEl" style="width: 100%; height: 360px; margin-top: 16px"></div>
  </div>
</template>
