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

/**
 * 从 token 里读色值，而不是在图表里写死十六进制 ——
 * 全站只有两支颜色，图表也只有两条线：主色蓝 + 正文墨。
 * 再加第三种色相就破了"双色"这个前提。
 */
function palette() {
  const css = getComputedStyle(document.documentElement)
  const read = (name: string, fallback: string) => css.getPropertyValue(name).trim() || fallback
  return {
    primary: read('--color-primary', '#3b82f6'),
    ink: read('--label-primary', '#1e293b'),
    secondary: read('--label-secondary', '#64748b'),
    separator: read('--separator', '#e2e8f0'),
    // 轴标签用表头那一档（14px），不是 ECharts 默认的 12px ——
    // 12px 摆在 17px 的数据旁边会明显小一号，像没跟着主题走
    labelSize: parseInt(read('--text-2xs', '14px'), 10) || 14
  }
}

function renderChart() {
  if (!chart || !data.value) return
  const daily = data.value.daily
  const c = palette()
  chart.setOption({
    // 全局兜底，免得漏掉哪处又退回 12px
    textStyle: { fontSize: c.labelSize },
    tooltip: { trigger: 'axis', textStyle: { fontSize: c.labelSize } },
    legend: { data: ['确认收入', '课时消耗'], textStyle: { color: c.secondary, fontSize: c.labelSize } },
    // 左边距跟着轴标签字号走，写死 60 的话字号一调大，数字就被切掉半截
    grid: { left: c.labelSize * 5, right: 60, top: 44, bottom: 40 },
    xAxis: {
      type: 'category',
      data: daily.map((p) => p.date.slice(5)),
      axisLine: { lineStyle: { color: c.separator } },
      axisLabel: { color: c.secondary, fontSize: c.labelSize }
    },
    yAxis: [
      {
        type: 'value',
        name: '收入(元)',
        nameTextStyle: { color: c.secondary, fontSize: c.labelSize },
        axisLabel: { color: c.secondary, fontSize: c.labelSize },
        splitLine: { lineStyle: { color: c.separator } }
      },
      {
        type: 'value',
        name: '课时',
        nameTextStyle: { color: c.secondary, fontSize: c.labelSize },
        axisLabel: { color: c.secondary, fontSize: c.labelSize },
        splitLine: { show: false }
      }
    ],
    series: [
      { name: '确认收入', type: 'line', data: daily.map((p) => p.revenue), itemStyle: { color: c.primary } },
      {
        name: '课时消耗',
        type: 'line',
        yAxisIndex: 1,
        data: daily.map((p) => p.hours),
        itemStyle: { color: c.ink }
      }
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
  <div class="k-page k-page--fill">
    <div class="k-card">
      <div class="k-toolbar">
        <span class="k-muted">月份</span>
        <el-date-picker
          v-model="period"
          type="month"
          value-format="YYYY-MM"
          :clearable="false"
          aria-label="月份"
          @change="load"
        />
        <span class="k-spacer" />
        <span class="k-muted">点一次签到，这里的数字当场就变。</span>
      </div>
    </div>

    <!-- 数字墙自己就是卡片，不要再套 .k-card（那是卡片套卡片） -->
    <div v-loading="loading" class="k-kpi">
      <div class="k-kpi__item">
        <span class="k-kpi__label">确认收入（元）</span>
        <span class="k-kpi__value k-num">{{ data?.confirmedRevenue ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">课时消耗</span>
        <span class="k-kpi__value k-num">{{ data?.consumedHours ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">教师成本（元）</span>
        <span class="k-kpi__value k-num">{{ data?.teacherCost ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">预收余额（元）</span>
        <span class="k-kpi__value k-num">{{ data?.preReceiveBalance ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">已上课次</span>
        <span class="k-kpi__value k-num">{{ data?.lessonCount ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">出勤人次</span>
        <span class="k-kpi__value k-num">{{ data?.presentCount ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">在读学员</span>
        <span class="k-kpi__value k-num">{{ data?.studentCount ?? '-' }}</span>
      </div>
      <div class="k-kpi__item">
        <span class="k-kpi__label">教师 / 班级</span>
        <span class="k-kpi__value k-num">
          {{ data?.teacherCount ?? '-' }} / {{ data?.classCount ?? '-' }}
        </span>
      </div>
    </div>

    <div class="k-card k-page__fill chart-card">
      <div ref="chartEl" class="chart"></div>
    </div>
  </div>
</template>

<style scoped>
/* 撑满高度的规则在 index.css 的 .k-page--fill 里，这里只管图表自己。
   原来图表写死 360px，1440×900 下卡片底下空 180px —— 空一大片不叫"简洁"，叫没填满。 */

/* 卡片自己不变成 flex 列的话，里面的 canvas 拿不到可分配的高度 */
.chart-card {
  display: flex;
  flex-direction: column;
}

.chart {
  width: 100%;
  flex: 1;
  /* 再矮也要留出画折线的地方，窄屏下不至于被压没 */
  min-height: 280px;
}
</style>
