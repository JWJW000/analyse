<script setup lang="ts">
import { ref, onMounted } from 'vue'

const progress = ref(0)
const loadingText = ref('初始化中...')

onMounted(() => {
  const steps = [
    { progress: 30, text: '加载资源...', delay: 200 },
    { progress: 60, text: '渲染界面...', delay: 300 },
    { progress: 90, text: '完成...', delay: 200 },
    { progress: 100, text: '', delay: 100 },
  ]

  let totalDelay = 0
  steps.forEach(step => {
    totalDelay += step.delay
    setTimeout(() => {
      progress.value = step.progress
      if (step.text) loadingText.value = step.text
    }, totalDelay)
  })
})
</script>

<template>
  <div class="initial-loader">
    <div class="loader-content">
      <div class="loader-logo">
        <span class="logo-text">伦理思政</span>
        <span class="logo-subtitle">需求分析辅助工具</span>
      </div>
      <div class="loader-progress">
        <div class="progress-bar">
          <div class="progress-fill" :style="{ width: `${progress}%` }"></div>
        </div>
        <div class="progress-text">{{ loadingText }}</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.initial-loader {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a365d 0%, #2c5282 100%);
  z-index: 9999;
  transition: opacity 0.5s ease, visibility 0.5s ease;
}

.initial-loader.fade-out {
  opacity: 0;
  visibility: hidden;
}

.loader-content {
  text-align: center;
  color: white;
}

.loader-logo {
  margin-bottom: 48px;
}

.logo-text {
  display: block;
  font-size: 36px;
  font-weight: 600;
  margin-bottom: 8px;
  letter-spacing: 2px;
}

.logo-subtitle {
  display: block;
  font-size: 14px;
  opacity: 0.8;
  letter-spacing: 4px;
}

.loader-progress {
  width: 280px;
  margin: 0 auto;
}

.progress-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #d69e2e;
  border-radius: 2px;
  transition: width 0.3s ease;
}

.progress-text {
  margin-top: 16px;
  font-size: 12px;
  opacity: 0.7;
}
</style>
