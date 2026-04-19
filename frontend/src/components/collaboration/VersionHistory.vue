<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { versionApi, type DocumentVersionDto } from '@/api/version'

const props = defineProps<{
  projectId: number
  requirementId: number
}>()

const emit = defineEmits<{
  restore: [version: DocumentVersionDto]
}>()

const versions = ref<DocumentVersionDto[]>([])
const selectedVersion = ref<DocumentVersionDto | null>(null)
const loading = ref(false)

onMounted(async () => {
  await fetchVersions()
})

async function fetchVersions() {
  loading.value = true
  try {
    versions.value = await versionApi.list(props.projectId, props.requirementId)
  } catch {
    versions.value = []
  } finally {
    loading.value = false
  }
}

function selectVersion(version: DocumentVersionDto) {
  selectedVersion.value = version
}

function handleRestore() {
  if (selectedVersion.value) {
    emit('restore', selectedVersion.value)
  }
}

function formatTime(time: string) {
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="version-history">
    <div class="version-list">
      <div 
        v-for="version in versions" 
        :key="version.id" 
        class="version-item"
        :class="{ selected: selectedVersion?.id === version.id }"
        @click="selectVersion(version)"
      >
        <div class="version-header">
          <span class="version-number">v{{ version.versionNumber }}</span>
          <span class="version-time">{{ formatTime(version.createdAt) }}</span>
        </div>
        <div class="version-author">{{ version.userName }}</div>
        <div v-if="version.changeSummary" class="version-summary">{{ version.changeSummary }}</div>
      </div>
      <a-empty v-if="!versions.length && !loading" description="暂无版本记录" />
    </div>
    
    <div v-if="selectedVersion" class="version-preview">
      <div class="preview-header">
        <h4>v{{ selectedVersion.versionNumber }} 预览</h4>
        <a-button type="primary" size="small" @click="handleRestore">
          恢复此版本
        </a-button>
      </div>
      <div class="preview-content">
        {{ selectedVersion.content || '（无内容）' }}
      </div>
    </div>
  </div>
</template>

<style scoped>
.version-history {
  display: flex;
  gap: 16px;
  height: 100%;
}

.version-list {
  width: 200px;
  flex-shrink: 0;
  overflow-y: auto;
}

.version-item {
  padding: 12px;
  background: var(--color-surface-hover, #F9FAFB);
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.version-item:hover {
  background: var(--color-border, #F3F4F6);
}

.version-item.selected {
  background: rgba(26, 115, 232, 0.1);
  border-left: 3px solid #1A73E8;
}

.version-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 4px;
}

.version-number {
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}

.version-time {
  color: #9CA3AF;
  font-size: 11px;
}

.version-author {
  color: var(--color-text-secondary, #6B7280);
  font-size: 13px;
}

.version-summary {
  margin-top: 4px;
  color: #9CA3AF;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.version-preview {
  flex: 1;
  background: var(--color-surface-hover, #F9FAFB);
  border-radius: 8px;
  padding: 16px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.preview-header h4 {
  margin: 0;
}

.preview-content {
  flex: 1;
  max-height: 400px;
  overflow-y: auto;
  color: var(--color-text-secondary, #374151);
  line-height: 1.6;
  white-space: pre-wrap;
  font-size: 13px;
}
</style>