<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { PlusOutlined, FileTextOutlined, AppstoreOutlined, HeartOutlined } from '@ant-design/icons-vue'
import { useProjectStore } from '@/stores/project'
import http from '@/api/http'
import { unwrap } from '@/api/http'
import type { CreateProjectRequest } from '@/api/project'

const router = useRouter()
const projectStore = useProjectStore()

const searchKey = ref('')
const filterStatus = ref('')
const showCreateModal = ref(false)
const createForm = ref<CreateProjectRequest>({ name: '', description: '', memberIds: [] })
const courses = ref<any[]>([])
const availableUsers = ref<any[]>([])

const loading = computed(() => projectStore.loading)

const filteredProjects = computed(() => {
  let list = projectStore.projects
  if (filterStatus.value) {
    list = list.filter(p => p.status === filterStatus.value)
  }
  if (searchKey.value) {
    const key = searchKey.value.toLowerCase()
    list = list.filter(p => p.name.toLowerCase().includes(key) || p.description?.toLowerCase().includes(key))
  }
  return list
})

onMounted(async () => {
  await projectStore.fetchProjects()
  try {
    courses.value = await unwrap(http.get('/api/courses/mine'))
  } catch {
    courses.value = []
  }
})

function goToProject(id: number) {
  router.push(`/app/projects/${id}`)
}

async function handleCreate() {
  if (!createForm.value.name) {
    message.error('请输入项目名称')
    return
  }
  await projectStore.createProject(createForm.value)
  showCreateModal.value = false
  createForm.value = { name: '', description: '', memberIds: [] }
  message.success('项目创建成功')
}

function getStatusColor(status: string) {
  return { ACTIVE: 'blue', ARCHIVED: 'default', COMPLETED: 'green' }[status] || 'default'
}

function getStatusText(status: string) {
  return { ACTIVE: '进行中', ARCHIVED: '已归档', COMPLETED: '已完成' }[status] || status
}

function getPhaseColor(phase: string) {
  return { LITERATURE: 'cyan', REQUIREMENTS: 'blue', ETHICS: 'purple', SUBMISSION: 'orange', REVIEW: 'green' }[phase] || 'default'
}

function getPhaseText(phase: string) {
  return { LITERATURE: '文献调研', REQUIREMENTS: '需求分析', ETHICS: '思政融合', SUBMISSION: '作业提交', REVIEW: '审核反馈' }[phase] || phase
}

function formatDate(date: string) {
  return new Date(date).toLocaleDateString('zh-CN')
}
</script>

<template>
  <div class="projects-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">项目工作台</h1>
        <p class="page-subtitle">管理您的课程项目，整合文献、需求和思政内容</p>
      </div>
      <a-button type="primary" @click="showCreateModal = true">
        <template #icon><PlusOutlined /></template>
        新建项目
      </a-button>
    </div>

    <div class="projects-filter">
      <a-input-search v-model:value="searchKey" placeholder="搜索项目..." style="width: 300px" />
      <a-select v-model:value="filterStatus" style="width: 150px" placeholder="状态筛选" allow-clear>
        <a-select-option value="ACTIVE">进行中</a-select-option>
        <a-select-option value="ARCHIVED">已归档</a-select-option>
        <a-select-option value="COMPLETED">已完成</a-select-option>
      </a-select>
    </div>

    <div v-if="loading" class="loading-container">
      <a-spin size="large" />
    </div>

    <div v-else-if="filteredProjects.length === 0" class="empty-state">
      <a-empty description="暂无项目，点击上方按钮创建第一个项目" />
    </div>

    <div v-else class="projects-grid">
      <div v-for="project in filteredProjects" :key="project.id" class="project-card" @click="goToProject(project.id)">
        <div class="project-card-header">
          <h3 class="project-name">{{ project.name }}</h3>
          <a-tag :color="getStatusColor(project.status)">{{ getStatusText(project.status) }}</a-tag>
        </div>

        <p class="project-description">{{ project.description || '暂无描述' }}</p>

        <div class="project-phase">
          <span class="phase-label">当前阶段:</span>
          <a-tag :color="getPhaseColor(project.currentPhase)">{{ getPhaseText(project.currentPhase) }}</a-tag>
        </div>

        <div class="project-stats">
          <div class="stat-item">
            <FileTextOutlined />
            <span>{{ project.literatureCount }} 文献</span>
          </div>
          <div class="stat-item">
            <AppstoreOutlined />
            <span>{{ project.requirementCount }} 需求</span>
          </div>
          <div class="stat-item">
            <HeartOutlined />
            <span>{{ project.ethicsModuleCount }} 思政</span>
          </div>
        </div>

        <div class="project-progress">
          <div class="progress-label">
            <span>进度</span>
            <span>{{ Math.round(project.progress) }}%</span>
          </div>
          <a-progress :percent="project.progress" :show-info="false" size="small" />
        </div>

        <div class="project-footer">
          <div class="project-members">
            <a-avatar v-for="m in project.members.slice(0, 3)" :key="m.id" size="small" :title="m.userName">
              {{ m.userName?.charAt(0) }}
            </a-avatar>
            <span v-if="project.members.length > 3" class="more-members">+{{ project.members.length - 3 }}</span>
          </div>
          <span class="project-updated">更新于 {{ formatDate(project.updatedAt) }}</span>
        </div>
      </div>
    </div>

    <a-modal v-model:open="showCreateModal" title="新建项目" @ok="handleCreate" :width="500">
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="项目名称" required>
          <a-input v-model:value="createForm.name" placeholder="输入项目名称" />
        </a-form-item>
        <a-form-item label="项目描述">
          <a-textarea v-model:value="createForm.description" :rows="3" placeholder="输入项目描述" />
        </a-form-item>
        <a-form-item label="关联课程">
          <a-select v-model:value="createForm.courseId" placeholder="选择关联课程" allow-clear>
            <a-select-option v-for="c in courses" :key="c.id" :value="c.id">{{ c.name }}</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<style scoped>
.projects-view {
  padding: 24px;
  max-width: 1400px;
  margin: 0 auto;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
  margin: 0 0 4px 0;
}

.page-subtitle {
  color: var(--color-text-secondary, #6B7280);
  margin: 0;
}

.projects-filter {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.loading-container {
  display: flex;
  justify-content: center;
  padding: 60px 0;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
}

.projects-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(340px, 1fr));
  gap: 20px;
}

.project-card {
  background: var(--color-surface, #FFFFFF);
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: all 0.2s;
}

.project-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1), 0 4px 6px -2px rgba(0, 0, 0, 0.05);
}

.project-card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.project-name {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
  margin: 0;
  flex: 1;
}

.project-description {
  color: var(--color-text-secondary, #6B7280);
  font-size: 14px;
  margin: 0 0 16px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.project-phase {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.phase-label {
  color: var(--color-text-secondary, #6B7280);
  font-size: 13px;
}

.project-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 16px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-text-secondary, #6B7280);
  font-size: 13px;
}

.project-progress {
  margin-bottom: 16px;
}

.progress-label {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--color-text-secondary, #6B7280);
  margin-bottom: 4px;
}

.project-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid var(--color-border, #E5E7EB);
}

.project-members {
  display: flex;
  align-items: center;
}

.project-members :deep(.ant-avatar) {
  margin-left: -6px;
  border: 2px solid var(--color-surface, #fff);
}

.project-members :deep(.ant-avatar:first-child) {
  margin-left: 0;
}

.more-members {
  margin-left: 8px;
  color: var(--color-text-secondary, #6B7280);
  font-size: 12px;
}

.project-updated {
  color: #9CA3AF;
  font-size: 12px;
}
</style>