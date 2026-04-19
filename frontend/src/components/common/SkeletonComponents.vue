<script setup lang="ts">
defineProps<{
  type?: 'text' | 'title' | 'avatar' | 'image' | 'button' | 'card' | 'table'
  width?: string
  height?: string
  rows?: number
}>()
</script>

<template>
  <!-- 文本骨架 -->
  <template v-if="type === 'text'">
    <div class="skeleton-text" :style="{ width: width || '100%' }">
      <div v-for="i in (rows || 3)" :key="i" class="skeleton-line" :style="{ width: i === (rows || 3) ? '60%' : '100%' }"></div>
    </div>
  </template>

  <!-- 标题骨架 -->
  <template v-else-if="type === 'title'">
    <div class="skeleton-title" :style="{ width: width || '50%', height: height || '24px' }"></div>
  </template>

  <!-- 头像骨架 -->
  <template v-else-if="type === 'avatar'">
    <div class="skeleton-avatar" :style="{ width: width || '40px', height: height || '40px' }"></div>
  </template>

  <!-- 图片骨架 -->
  <template v-else-if="type === 'image'">
    <div class="skeleton-image" :style="{ width: width || '100px', height: height || '100px' }"></div>
  </template>

  <!-- 按钮骨架 -->
  <template v-else-if="type === 'button'">
    <div class="skeleton-button" :style="{ width: width || '80px', height: height || '32px' }"></div>
  </template>

  <!-- 卡片骨架 -->
  <template v-else-if="type === 'card'">
    <div class="skeleton-card">
      <div class="skeleton-card-header">
        <div class="skeleton-avatar" style="width: 40px; height: 40px;"></div>
        <div class="skeleton-card-title">
          <div class="skeleton-line" style="width: 120px; height: 16px;"></div>
          <div class="skeleton-line" style="width: 80px; height: 12px; margin-top: 8px;"></div>
        </div>
      </div>
      <div class="skeleton-card-body">
        <div v-for="i in 4" :key="i" class="skeleton-line" :style="{ width: i === 4 ? '60%' : '100%' }"></div>
      </div>
    </div>
  </template>

  <!-- 表格骨架 -->
  <template v-else-if="type === 'table'">
    <div class="skeleton-table">
      <div class="skeleton-table-header">
        <div v-for="i in 5" :key="i" class="skeleton-line" :style="{ width: `${100 / 5}%` }"></div>
      </div>
      <div v-for="row in (rows || 5)" :key="row" class="skeleton-table-row">
        <div v-for="i in 5" :key="i" class="skeleton-line" :style="{ width: `${100 / 5}%` }"></div>
      </div>
    </div>
  </template>
</template>

<style scoped>
/* 基础骨架线 */
.skeleton-line {
  height: 14px;
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--color-bg-base) 25%,
    var(--color-border) 50%,
    var(--color-bg-base) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
  margin-bottom: 8px;
}

/* 文本区块 */
.skeleton-text {
  display: flex;
  flex-direction: column;
}

/* 标题 */
.skeleton-title {
  border-radius: 4px;
  background: linear-gradient(
    90deg,
    var(--color-bg-base) 25%,
    var(--color-border) 50%,
    var(--color-bg-base) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

/* 头像 */
.skeleton-avatar {
  border-radius: 50%;
  background: linear-gradient(
    90deg,
    var(--color-bg-base) 25%,
    var(--color-border) 50%,
    var(--color-bg-base) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

/* 图片 */
.skeleton-image {
  border-radius: var(--radius-lg);
  background: linear-gradient(
    90deg,
    var(--color-bg-base) 25%,
    var(--color-border) 50%,
    var(--color-bg-base) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

/* 按钮 */
.skeleton-button {
  border-radius: var(--radius-md);
  background: linear-gradient(
    90deg,
    var(--color-bg-base) 25%,
    var(--color-border) 50%,
    var(--color-bg-base) 75%
  );
  background-size: 200% 100%;
  animation: skeleton-pulse 1.5s ease-in-out infinite;
}

/* 卡片 */
.skeleton-card {
  padding: 16px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.skeleton-card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.skeleton-card-title {
  flex: 1;
}

.skeleton-card-body {
  display: flex;
  flex-direction: column;
}

/* 表格 */
.skeleton-table {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.skeleton-table-header {
  display: flex;
  background: var(--color-bg-base);
  padding: 12px 16px;
  gap: 16px;
}

.skeleton-table-row {
  display: flex;
  padding: 12px 16px;
  gap: 16px;
  border-top: 1px solid var(--color-border);
}

@keyframes skeleton-pulse {
  0% {
    background-position: 200% 0;
  }
  100% {
    background-position: -200% 0;
  }
}
</style>
