<script setup lang="ts">
import { ref, onMounted, computed, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import * as echarts from 'echarts'
import { statsApi, type StudentProfileDto } from '@/api/stats'

const route = useRoute()
const profile = ref<StudentProfileDto | null>(null)
const radarChart = ref<HTMLDivElement | null>(null)
const loading = ref(false)

const courseId = computed(() => Number(route.params.courseId))
const studentId = computed(() => Number(route.params.studentId))

onMounted(async () => {
  await loadProfile()
})

async function loadProfile() {
  loading.value = true
  try {
    profile.value = await statsApi.studentProfile(courseId.value, studentId.value)
    await nextTick()
    initRadarChart()
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

function initRadarChart() {
  if (!radarChart.value || !profile.value) return

  const chart = echarts.init(radarChart.value)
  const abilities = profile.value.abilities

  const indicator = Object.entries(abilities).map(([name, value]) => ({
    name,
    max: 100
  }))

  chart.setOption({
    tooltip: {},
    radar: {
      indicator,
      shape: 'polygon',
      axisName: {
        color: '#6B7280'
      }
    },
    series: [{
      type: 'radar',
      data: [{
        value: Object.values(abilities),
        name: '能力值',
        areaStyle: {
          color: 'rgba(26, 115, 232, 0.2)'
        },
        lineStyle: {
          color: '#1A73E8'
        },
        itemStyle: {
          color: '#1A73E8'
        }
      }]
    }]
  })
}
</script>

<template>
  <div class="student-profile" v-if="profile">
    <div class="profile-header">
      <a-avatar size="large" style="background: #1A73E8;">
        {{ profile.studentName?.charAt(0) }}
      </a-avatar>
      <div class="profile-info">
        <h2>{{ profile.studentName }}</h2>
        <div class="profile-scores">
          <span>平均分: <strong>{{ profile.avgScore.toFixed(1) }}</strong></span>
          <span>思政分: <strong>{{ profile.ethicsScore.toFixed(1) }}</strong></span>
          <span>提交率: <strong>{{ profile.totalSubmissions > 0 ? (profile.submittedCount / profile.totalSubmissions * 100).toFixed(0) : 0 }}%</strong></span>
        </div>
      </div>
    </div>

    <div class="profile-content">
      <div class="ability-section">
        <h3>能力画像</h3>
        <div ref="radarChart" style="width: 100%; height: 350px;"></div>
      </div>

      <div class="mistakes-section" v-if="profile.commonMistakes.length">
        <h3>常见问题分析</h3>
        <div v-for="mistake in profile.commonMistakes" :key="mistake.type" class="mistake-card">
          <div class="mistake-header">
            <a-tag color="orange">{{ mistake.type }}</a-tag>
            <span class="mistake-count">{{ mistake.count }}次</span>
          </div>
          <p class="mistake-desc">{{ mistake.description }}</p>
          <p class="mistake-suggestion"><strong>建议:</strong> {{ mistake.suggestion }}</p>
        </div>
      </div>
    </div>
  </div>

  <a-spin v-else-if="loading" size="large" />

  <a-empty v-else description="暂无数据" />
</template>

<style scoped>
.student-profile {
  padding: 24px;
  max-width: 1000px;
  margin: 0 auto;
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 24px;
  background: var(--color-surface, #FFFFFF);
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
  margin-bottom: 24px;
}

.profile-info h2 {
  margin: 0 0 8px 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}

.profile-scores {
  display: flex;
  gap: 24px;
  color: var(--color-text-secondary, #6B7280);
}

.profile-scores strong {
  color: var(--color-text-primary, #111827);
}

.profile-content {
  display: grid;
  gap: 24px;
}

.ability-section,
.mistakes-section {
  background: var(--color-surface, #FFFFFF);
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.ability-section h3,
.mistakes-section h3 {
  margin: 0 0 20px 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}

.mistake-card {
  padding: 16px;
  background: var(--color-surface-hover, #F9FAFB);
  border-radius: 8px;
  margin-bottom: 12px;
}

.mistake-card:last-child {
  margin-bottom: 0;
}

.mistake-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.mistake-count {
  color: #9CA3AF;
  font-size: 13px;
}

.mistake-desc {
  color: var(--color-text-secondary, #374151);
  margin: 0 0 8px 0;
  font-size: 14px;
}

.mistake-suggestion {
  color: var(--color-text-secondary, #6B7280);
  margin: 0;
  font-size: 13px;
}
</style>