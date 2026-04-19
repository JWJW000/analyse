<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { commentApi, type CommentDto } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  projectId: number
  requirementId?: number
}>()

const auth = useAuthStore()
const comments = ref<CommentDto[]>([])
const newComment = ref('')
const submitting = ref(false)
const loading = ref(false)

onMounted(async () => {
  await fetchComments()
})

async function fetchComments() {
  loading.value = true
  try {
    comments.value = await commentApi.list(props.projectId, props.requirementId)
  } catch {
    comments.value = []
  } finally {
    loading.value = false
  }
}

async function handleSubmit() {
  if (!newComment.value.trim()) return
  submitting.value = true
  try {
    await commentApi.add({
      projectId: props.projectId,
      requirementId: props.requirementId || 0,
      content: newComment.value
    })
    newComment.value = ''
    await fetchComments()
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  await commentApi.delete(id)
  await fetchComments()
}

function formatTime(time: string) {
  return new Date(time).toLocaleString('zh-CN')
}
</script>

<template>
  <div class="comment-panel">
    <div class="comment-list">
      <div v-for="comment in comments" :key="comment.id" class="comment-item">
        <div class="comment-header">
          <a-avatar size="small">{{ comment.userName?.charAt(0) }}</a-avatar>
          <span class="comment-author">{{ comment.userName }}</span>
          <span class="comment-time">{{ formatTime(comment.createdAt) }}</span>
          <a-button 
            v-if="comment.userId === auth.userId" 
            type="link" 
            size="small" 
            danger 
            @click="handleDelete(comment.id)"
          >
            删除
          </a-button>
        </div>
        <div class="comment-content">{{ comment.content }}</div>
      </div>
      <a-empty v-if="!comments.length && !loading" description="暂无评论" />
    </div>
    
    <div class="comment-input">
      <a-textarea 
        v-model:value="newComment" 
        placeholder="添加评论..." 
        :rows="2"
      />
      <a-button 
        type="primary" 
        @click="handleSubmit" 
        :loading="submitting" 
        style="margin-top: 8px"
      >
        发送
      </a-button>
    </div>
  </div>
</template>

<style scoped>
.comment-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.comment-list {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 16px;
}

.comment-item {
  padding: 12px;
  background: var(--color-surface-hover, #F9FAFB);
  border-radius: 8px;
  margin-bottom: 8px;
}

.comment-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.comment-author {
  font-weight: 500;
  color: var(--color-text-primary, #111827);
}

.comment-time {
  color: #9CA3AF;
  font-size: 12px;
}

.comment-content {
  color: var(--color-text-secondary, #374151);
  line-height: 1.5;
  white-space: pre-wrap;
}

.comment-input {
  border-top: 1px solid var(--color-border, #E5E7EB);
  padding-top: 16px;
}
</style>