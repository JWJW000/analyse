<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http, { unwrap } from '@/api/http'
import * as echarts from 'echarts'
import { useTheme } from '@/composables/useTheme'
import { useEchartsTheme } from '@/composables/useEchartsTheme'
import { useEchartsThemeDark } from '@/composables/useEchartsThemeDark'

const { isDark } = useTheme()
const { getOption: getLightOption } = useEchartsTheme()
const { getOption: getDarkOption } = useEchartsThemeDark()

const stats = ref<Record<string, number> | null>(null)
const chartRef = ref<HTMLElement | null>(null)

function getOption(base?: echarts.EChartsOption) {
  return isDark.value ? getDarkOption(base) : getLightOption(base)
}

async function load() {
  const res = await unwrap(http.get('/api/stats/global'))
  stats.value = res as Record<string, number>
  if (chartRef.value && stats.value) {
    const ch = echarts.init(chartRef.value)
    ch.setOption(getOption({
      title: { text: '全站概览' },
      tooltip: {},
      xAxis: { type: 'category', data: ['用户', '需求', '思政模块', '匹配事件'] },
      yAxis: { type: 'value' },
      series: [
        {
          type: 'bar',
          data: [stats.value.users, stats.value.requirements, stats.value.ethicsModules, stats.value.matchEvents],
        },
      ],
    }))
  }
}

function exportCsv() {
  if (!stats.value) return
  const rows = [
    ['users', String(stats.value.users)],
    ['requirements', String(stats.value.requirements)],
    ['ethicsModules', String(stats.value.ethicsModules)],
    ['matchEvents', String(stats.value.matchEvents)],
  ]
  const blob = new Blob([rows.map((r) => r.join(',')).join('\n')], { type: 'text/csv;charset=utf-8;' })
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'global-stats.csv'
  a.click()
}

onMounted(load)
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-descriptions v-if="stats" bordered size="small">
      <a-descriptions-item label="用户数">{{ stats.users }}</a-descriptions-item>
      <a-descriptions-item label="需求数">{{ stats.requirements }}</a-descriptions-item>
      <a-descriptions-item label="思政模块">{{ stats.ethicsModules }}</a-descriptions-item>
      <a-descriptions-item label="匹配事件">{{ stats.matchEvents }}</a-descriptions-item>
    </a-descriptions>
    <div ref="chartRef" class="stats-chart" />
    <a-button @click="exportCsv">导出 CSV</a-button>
  </a-space>
</template>

<style scoped>
.stats-chart {
  width: 100%;
  height: min(360px, 55vh);
  min-height: 220px;
}
</style>
