<script setup lang="ts">
import SkeletonComponents from './SkeletonComponents.vue'

defineProps<{
  type: 'list' | 'detail' | 'form' | 'dashboard'
}>()
</script>

<template>
  <div class="page-skeleton">
    <!-- 列表页骨架 -->
    <template v-if="type === 'list'">
      <div class="skeleton-header">
        <SkeletonComponents type="title" width="200px" />
        <div class="skeleton-actions">
          <SkeletonComponents type="button" width="80px" />
          <SkeletonComponents type="button" width="100px" />
        </div>
      </div>
      <SkeletonComponents type="table" :rows="8" />
    </template>

    <!-- 详情页骨架 -->
    <template v-else-if="type === 'detail'">
      <SkeletonComponents type="title" width="300px" />
      <div class="skeleton-content">
        <SkeletonComponents type="card" />
        <SkeletonComponents type="card" />
      </div>
      <SkeletonComponents type="text" :rows="6" />
    </template>

    <!-- 表单页骨架 -->
    <template v-else-if="type === 'form'">
      <SkeletonComponents type="title" width="150px" />
      <div class="skeleton-form">
        <div class="form-row">
          <SkeletonComponents type="text" :rows="1" />
        </div>
        <div class="form-row">
          <SkeletonComponents type="text" :rows="1" />
        </div>
        <SkeletonComponents type="text" :rows="4" />
      </div>
      <div class="skeleton-form-actions">
        <SkeletonComponents type="button" width="80px" />
        <SkeletonComponents type="button" width="80px" />
      </div>
    </template>

    <!-- 仪表盘骨架 -->
    <template v-else-if="type === 'dashboard'">
      <div class="skeleton-stats">
        <SkeletonComponents type="card" />
        <SkeletonComponents type="card" />
        <SkeletonComponents type="card" />
        <SkeletonComponents type="card" />
      </div>
      <SkeletonComponents type="card" />
    </template>
  </div>
</template>

<style scoped>
.page-skeleton {
  padding: 16px;
}

.skeleton-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.skeleton-actions {
  display: flex;
  gap: 8px;
}

.skeleton-content {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
  margin: 24px 0;
}

.skeleton-form {
  margin: 24px 0;
}

.form-row {
  margin-bottom: 16px;
  padding: 16px;
  background: var(--color-bg-surface);
  border-radius: var(--radius-lg);
}

.skeleton-form-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
}

.skeleton-stats {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

@media (max-width: 768px) {
  .skeleton-content,
  .skeleton-stats {
    grid-template-columns: 1fr;
  }
}
</style>
