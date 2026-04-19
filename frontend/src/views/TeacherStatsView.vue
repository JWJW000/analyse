<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http, { unwrap } from '@/api/http'
import * as echarts from 'echarts'
import { useTheme } from '@/composables/useTheme'
import { useEchartsTheme } from '@/composables/useEchartsTheme'
import { useEchartsThemeDark } from '@/composables/useEchartsThemeDark'
import { message } from 'ant-design-vue'

const { isDark } = useTheme()
const { getOption: getLightOption } = useEchartsTheme()
const { getOption: getDarkOption } = useEchartsThemeDark()

function getOption(base?: echarts.EChartsOption) {
  return isDark.value ? getDarkOption(base) : getLightOption(base)
}

const route = useRoute()
const courseId = Number(route.params.courseId)
const data = ref<Record<string, unknown> | null>(null)
const loading = ref(false)
const loadError = ref('')
const chartRef = ref<HTMLElement | null>(null)
const statusChartRef = ref<HTMLElement | null>(null)
let embedChart: echarts.ECharts | null = null
let statusChart: echarts.ECharts | null = null

type StudentProgress = {
  studentId: number
  studentName: string
  requirementCount: number
  withEmbeddingCount: number
  submittedOrApprovedCount: number
}

const studentProgress = ref<StudentProgress[]>([])

function renderCharts(res: Record<string, unknown>) {
  const freq = (res.embeddedModuleFrequency || {}) as Record<string, number>
  const entries = Object.entries(freq).map(([k, v]) => ({ name: '模块' + k, value: v }))
  if (chartRef.value) {
    if (!embedChart) embedChart = echarts.init(chartRef.value)
    embedChart.setOption(getOption({
      title: { text: '思政模块嵌入频次' },
      tooltip: {},
      xAxis: { type: 'category', data: entries.length ? entries.map((e) => e.name) : ['暂无'] },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: entries.length ? entries.map((e) => e.value) : [0] }],
    }))
  }
  const statusCounts = (res.requirementStatusCounts || {}) as Record<string, number>
  const statusEntries = Object.entries(statusCounts).filter(([, v]) => v > 0)
  if (statusChartRef.value) {
    if (!statusChart) statusChart = echarts.init(statusChartRef.value)
    statusChart.setOption(getOption({
      title: { text: '本课程需求状态分布' },
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: '62%',
          data: statusEntries.length
            ? statusEntries.map(([name, value]) => ({ name, value }))
            : [{ name: '暂无', value: 1 }],
        },
      ],
    }))
  }
}

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    const res = (await unwrap(http.get(`/api/stats/class/${courseId}`))) as Record<string, unknown>
    data.value = res
    studentProgress.value = Array.isArray(res.studentProgress)
      ? (res.studentProgress as StudentProgress[])
      : []
    await nextTick()
    renderCharts(res)
  } catch (e: unknown) {
    loadError.value = e instanceof Error ? e.message : '统计加载失败'
    message.error(loadError.value)
  } finally {
    loading.value = false
  }
}

function onResize() {
  embedChart?.resize()
  statusChart?.resize()
}

function exportCsv() {
  if (!data.value) return
  const rows: string[][] = [
    ['指标', '值'],
    ['学生数', String(data.value.students)],
    ['需求数', String(data.value.requirements)],
    ['已提交或通过', String(data.value.submittedOrApproved)],
    ['AI 匹配次数', String(data.value.matchEvents)],
  ]
  const sc = data.value.requirementStatusCounts as Record<string, number> | undefined
  if (sc) {
    rows.push(['---', '---'])
    rows.push(...Object.entries(sc).map(([k, v]) => ['状态_' + k, String(v)]))
  }
  rows.push(['---', '---'])
  rows.push(['学生', '需求数', '已嵌入思政的需求数', '已提交或通过数'])
  for (const s of studentProgress.value) {
    rows.push([
      s.studentName,
      String(s.requirementCount),
      String(s.withEmbeddingCount),
      String(s.submittedOrApprovedCount),
    ])
  }
  const blob = new Blob([rows.map((r) => r.join(',')).join('\n')], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = `course-${courseId}-stats.csv`
  a.click()
}

onMounted(() => {
  load()
  window.addEventListener('resize', onResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', onResize)
  embedChart?.dispose()
  statusChart?.dispose()
  embedChart = null
  statusChart = null
})
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-alert v-if="loadError" type="warning" show-icon :message="loadError" />
    <a-spin :spinning="loading">
    <a-typography-title :level="5">班级统计 — 课程 {{ data?.courseName }}</a-typography-title>
    <a-descriptions bordered size="small" v-if="data">
      <a-descriptions-item label="学生数">{{ data.students }}</a-descriptions-item>
      <a-descriptions-item label="需求文档数">{{ data.requirements }}</a-descriptions-item>
      <a-descriptions-item label="已提交/通过">{{ data.submittedOrApproved }}</a-descriptions-item>
      <a-descriptions-item label="AI 匹配次数">{{ data.matchEvents }}</a-descriptions-item>
    </a-descriptions>

    <a-typography-text type="secondary">
      下表为每名学生在课程下的需求数、至少嵌入过一条思政模块的需求数、以及已提交或通过数，便于对照班级进度。
    </a-typography-text>
    <a-table
      v-if="studentProgress.length"
      :data-source="studentProgress"
      :pagination="false"
      row-key="studentId"
      size="small"
      :scroll="{ x: 'max-content' }"
    >
      <a-table-column title="学生" data-index="studentName" />
      <a-table-column title="需求数" data-index="requirementCount" width="90" />
      <a-table-column title="已嵌入思政的需求数" data-index="withEmbeddingCount" width="160" />
      <a-table-column title="已提交或通过" data-index="submittedOrApprovedCount" width="130" />
    </a-table>
    <a-empty v-else-if="data" description="暂无选课学生或暂无进度数据" />

    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :lg="12">
        <div ref="chartRef" class="stats-chart" />
      </a-col>
      <a-col :xs="24" :lg="12">
        <div ref="statusChartRef" class="stats-chart" />
      </a-col>
    </a-row>

    <a-button @click="exportCsv">导出 CSV（含学生明细与状态）</a-button>
    </a-spin>
  </a-space>
</template>

<style scoped>
.stats-chart {
  width: 100%;
  height: min(360px, 55vh);
  min-height: 220px;
}
</style>
