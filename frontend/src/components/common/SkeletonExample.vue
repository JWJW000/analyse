<script setup lang="ts">
import { ref } from 'vue'
import { Button } from 'ant-design-vue'
import PageSkeleton from './PageSkeleton.vue'

const showSkeleton = ref(true)
const skeletonType = ref<'list' | 'detail' | 'form' | 'dashboard'>('dashboard')
</script>

<template>
  <div class="skeleton-demo">
    <h3>骨架屏组件示例</h3>
    
    <div class="demo-controls">
      <Button @click="showSkeleton = !showSkeleton">
        {{ showSkeleton ? '隐藏' : '显示' }}骨架屏
      </Button>
      <select v-model="skeletonType" class="type-select">
        <option value="dashboard">仪表盘</option>
        <option value="list">列表页</option>
        <option value="detail">详情页</option>
        <option value="form">表单页</option>
      </select>
    </div>

    <div class="demo-content">
      <PageSkeleton v-if="showSkeleton" :type="skeletonType" />
      <div v-else>
        <!-- 实际内容 -->
        <p>这里是实际内容...</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.skeleton-demo {
  padding: 24px;
}

.demo-controls {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.type-select {
  padding: 4px 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
}

.demo-content {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 16px;
}
</style>
