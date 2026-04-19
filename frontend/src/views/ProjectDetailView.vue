<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  SettingOutlined,
  TeamOutlined,
  FileTextOutlined,
  AppstoreOutlined,
  HeartOutlined,
  PlusOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  DeleteOutlined,
} from '@ant-design/icons-vue'
import { useProjectStore } from '@/stores/project'
import { useAuthStore } from '@/stores/auth'
import { projectApi, type ProjectPhaseChecklistItem } from '@/api/project'
import { useProjectContent } from '@/composables/useProjectContent'
import MentorSidebar from '@/components/mentor/MentorSidebar.vue'

const route = useRoute()
const router = useRouter()
const projectStore = useProjectStore()
const auth = useAuthStore()

const activeTab = ref('overview')
const showSettings = ref(false)
const showShare = ref(false)

// 项目内容数据
const projectContent = ref<ReturnType<typeof useProjectContent> | null>(null)
const contentLoading = ref(false)

const checklist = ref<ProjectPhaseChecklistItem[]>([])
const checklistLoading = ref(false)
const phaseActionLoading = ref(false)
const rollbackOpen = ref(false)
const rollbackReason = ref('')

const project = computed(() => projectStore.currentProject)
const loading = computed(() => projectStore.loading)

const currentPhaseIndex = computed(() => {
  if (!project.value) return 0
  const phases = ['LITERATURE', 'REQUIREMENTS', 'ETHICS', 'SUBMISSION', 'REVIEW']
  return phases.indexOf(project.value.currentPhase)
})

const phaseItems = computed(() => [
  { title: '文献调研' },
  { title: '需求分析' },
  { title: '思政融合' },
  { title: '作业提交' },
  { title: '审核反馈' },
])

const projectProgress = computed(() => {
  if (!project.value) return 0
  const satisfiedCount = checklist.value.filter(c => c.satisfied).length
  if (checklist.value.length === 0) return 0
  return Math.round((satisfiedCount / checklist.value.length) * 100)
})

const canEditPhase = computed(() => {
  if (!project.value || auth.userId == null) return false
  if (project.value.ownerId === auth.userId) return true
  const m = project.value.members.find((x) => x.userId === auth.userId)
  return m?.role === 'OWNER' || m?.role === 'EDITOR'
})

const showRollback = computed(() => {
  if (!project.value || auth.userId == null) return false
  if (project.value.ownerId === auth.userId) return true
  const r = auth.role
  return r === 'TEACHER' || r === 'ADMIN' || r === 'TA'
})

const showAdvanceButton = computed(
  () => canEditPhase.value && project.value?.currentPhase !== 'REVIEW',
)

const canAdvancePhase = computed(() => {
  if (!project.value || project.value.currentPhase === 'REVIEW') return false
  if (checklist.value.length === 0) return false
  return checklist.value.every((c) => c.satisfied)
})

async function loadChecklist() {
  if (!project.value) return
  checklistLoading.value = true
  try {
    checklist.value = await projectApi.phaseChecklist(project.value.id)
  } catch (e: unknown) {
    const err = e as Error
    message.error(err.message || '加载阶段条件失败')
    checklist.value = []
  } finally {
    checklistLoading.value = false
  }
}

async function loadProject(id: number) {
  await projectStore.fetchProject(id)
}

onMounted(() => {
  const id = Number(route.params.id)
  if (id) {
    void loadProject(id)
  }
})

watch(
  () => [
    project.value?.id,
    project.value?.literatureCount,
    project.value?.requirementCount,
    project.value?.ethicsModuleCount,
    project.value?.currentPhase,
  ],
  () => {
    if (project.value?.id) void loadChecklist()
  },
)

watch(
  () => route.params.id,
  (id) => {
    const n = Number(id)
    if (n) void loadProject(n)
  },
)

watch(
  () => [project.value?.id, activeTab.value],
  async ([newId, tab]) => {
    if (!newId) return
    projectContent.value = useProjectContent(newId)
    if (tab === 'literature') {
      contentLoading.value = true
      await projectContent.value.loadLiteratures()
      contentLoading.value = false
    } else if (tab === 'requirements') {
      contentLoading.value = true
      await projectContent.value.loadRequirements()
      contentLoading.value = false
    } else if (tab === 'ethics') {
      contentLoading.value = true
      await projectContent.value.loadEthicsModules()
      contentLoading.value = false
    }
  },
)

async function handleAdvancePhase() {
  if (!project.value || !canAdvancePhase.value) return
  phaseActionLoading.value = true
  try {
    const updated = await projectApi.advancePhase(project.value.id)
    projectStore.updateCurrentProject(updated)
    message.success('已进入下一阶段')
  } catch (e: unknown) {
    const err = e as Error
    message.error(err.message || '推进失败')
  } finally {
    phaseActionLoading.value = false
  }
}

/** @returns false 时 Modal 保持打开（Ant Design Vue 4 onOk 约定） */
async function handleRollbackModalOk(): Promise<boolean> {
  if (!project.value) return false
  const reason = rollbackReason.value.trim()
  if (reason.length < 2) {
    message.warning('请填写至少 2 个字的回退原因')
    return false
  }
  phaseActionLoading.value = true
  try {
    const updated = await projectApi.rollbackPhase(project.value.id, reason)
    projectStore.updateCurrentProject(updated)
    rollbackOpen.value = false
    rollbackReason.value = ''
    message.success('已回退到上一阶段')
    return true
  } catch (e: unknown) {
    const err = e as Error
    message.error(err.message || '回退失败')
    return false
  } finally {
    phaseActionLoading.value = false
  }
}

function getStatusColor(status: string) {
  return { ACTIVE: 'blue', ARCHIVED: 'default', COMPLETED: 'green' }[status] || 'default'
}

function getStatusText(status: string) {
  return { ACTIVE: '进行中', ARCHIVED: '已归档', COMPLETED: '已完成' }[status] || status
}

function getRoleText(role: string) {
  return { OWNER: '负责人', EDITOR: '编辑', MEMBER: '成员', VIEWER: '观察者' }[role] || role
}

function getPhaseColor(phase: string) {
  return { LITERATURE: 'cyan', REQUIREMENTS: 'blue', ETHICS: 'purple', SUBMISSION: 'orange', REVIEW: 'green' }[phase] || 'default'
}

function getPhaseText(phase: string) {
  return { LITERATURE: '文献调研', REQUIREMENTS: '需求分析', ETHICS: '思政融合', SUBMISSION: '作业提交', REVIEW: '审核反馈' }[phase] || phase
}

async function removeLiterature(literatureId: number) {
  if (!project.value) return
  await projectStore.removeLiterature(project.value.id, literatureId)
  message.success('已移除')
}

async function removeRequirement(requirementId: number) {
  if (!project.value) return
  await projectStore.removeRequirement(project.value.id, requirementId)
  message.success('已移除')
}

async function removeEthicsModule(ethicsModuleId: number) {
  if (!project.value) return
  await projectStore.removeEthicsModule(project.value.id, ethicsModuleId)
  message.success('已移除')
  if (projectContent.value) {
    await projectContent.value.loadEthicsModules()
  }
}

async function handleRemoveLiterature(literatureId: number) {
  await removeLiterature(literatureId)
}

async function handleRemoveRequirement(requirementId: number) {
  await removeRequirement(requirementId)
}

async function handleRemoveEthicsModule(ethicsModuleId: number) {
  await removeEthicsModule(ethicsModuleId)
}

function goToLiterature() {
  router.push('/app/literature')
}

function goToRequirements() {
  router.push('/app/requirements')
}

function goToEthics() {
  router.push('/app/ethics')
}
</script>

<template>
  <div class="project-detail" v-if="project">
    <div class="project-header">
      <div class="header-left">
        <a-breadcrumb>
          <a-breadcrumb-item>
            <router-link to="/app/projects">项目工作台</router-link>
          </a-breadcrumb-item>
          <a-breadcrumb-item>{{ project.name }}</a-breadcrumb-item>
        </a-breadcrumb>
        <div class="project-title-row">
          <h1 class="project-title">{{ project.name }}</h1>
          <a-tag :color="getStatusColor(project.status)">{{ getStatusText(project.status) }}</a-tag>
          <a-tag :color="getPhaseColor(project.currentPhase)">{{ getPhaseText(project.currentPhase) }}</a-tag>
        </div>
        <p class="project-desc">{{ project.description || '暂无描述' }}</p>
      </div>
      <div class="header-actions">
        <a-button @click="showSettings = true">
          <template #icon><SettingOutlined /></template>
          设置
        </a-button>
        <a-button type="primary" @click="showShare = true">
          <template #icon><TeamOutlined /></template>
          分享
        </a-button>
      </div>
    </div>

    <a-tabs v-model:activeKey="activeTab" class="project-tabs">
      <a-tab-pane key="overview" tab="总览">
        <div class="overview-grid">
          <div class="overview-card">
            <div class="overview-icon" style="background: rgba(26, 115, 232, 0.1); color: #1A73E8;">
              <FileTextOutlined />
            </div>
            <div class="overview-info">
              <span class="overview-num">{{ project.literatureCount }}</span>
              <span class="overview-label">文献</span>
            </div>
          </div>
          <div class="overview-card">
            <div class="overview-icon" style="background: rgba(16, 185, 129, 0.1); color: #10B981;">
              <AppstoreOutlined />
            </div>
            <div class="overview-info">
              <span class="overview-num">{{ project.requirementCount }}</span>
              <span class="overview-label">需求</span>
            </div>
          </div>
          <div class="overview-card">
            <div class="overview-icon" style="background: rgba(139, 92, 246, 0.1); color: #8B5CF6;">
              <HeartOutlined />
            </div>
            <div class="overview-info">
              <span class="overview-num">{{ project.ethicsModuleCount }}</span>
              <span class="overview-label">思政</span>
            </div>
          </div>
        </div>

        <div class="phase-progress">
          <h3>项目阶段</h3>
          <a-steps :current="currentPhaseIndex" :items="phaseItems" />
          <div v-if="showAdvanceButton || showRollback" class="phase-actions">
            <a-space wrap>
              <a-button
                v-if="showAdvanceButton"
                type="primary"
                :disabled="!canAdvancePhase"
                :loading="phaseActionLoading"
                @click="handleAdvancePhase"
              >
                进入下一阶段
              </a-button>
              <a-button
                v-if="showRollback && project.currentPhase !== 'LITERATURE'"
                danger
                ghost
                :loading="phaseActionLoading"
                @click="rollbackOpen = true"
              >
                回退阶段
              </a-button>
            </a-space>
            <p v-if="showAdvanceButton && !canAdvancePhase" class="phase-hint">
              请完成上方「阶段完成条件」后再推进。
            </p>
          </div>
          <div v-if="checklist.length" class="phase-checklist">
            <h4>阶段完成条件</h4>
            <a-spin :spinning="checklistLoading">
              <ul class="checklist-ul">
                <li v-for="item in checklist" :key="item.key" class="checklist-row">
                  <CheckCircleOutlined v-if="item.satisfied" class="icon ok" />
                  <CloseCircleOutlined v-else class="icon no" />
                  <span class="checklist-label">{{ item.label }}</span>
                  <span v-if="!item.satisfied && item.hint" class="checklist-hint">{{ item.hint }}</span>
                </li>
              </ul>
            </a-spin>
          </div>
        </div>

        <div class="members-section">
          <h3>项目成员</h3>
          <div class="members-list">
            <div v-for="m in project.members" :key="m.id" class="member-item">
              <a-avatar>{{ m.userName?.charAt(0) }}</a-avatar>
              <span class="member-name">{{ m.userName }}</span>
              <a-tag :color="m.role === 'OWNER' ? 'blue' : 'default'">{{ getRoleText(m.role) }}</a-tag>
            </div>
          </div>
        </div>
      </a-tab-pane>

      <a-tab-pane key="literature" tab="文献调研">
        <div class="tab-header">
          <h3>关联文献</h3>
          <a-button type="link" @click="goToLiterature">
            <PlusOutlined /> 添加文献
          </a-button>
        </div>
        <a-spin v-if="contentLoading && activeTab === 'literature'" />
        <div v-else-if="projectContent?.literatures.value?.length" class="content-list">
          <div v-for="item in projectContent?.literatures.value" :key="item.id" class="content-item">
            <FileTextOutlined style="font-size: 20px; color: #1A73E8;" />
            <div class="content-info">
              <span class="content-title">{{ item.title }}</span>
              <span class="content-meta">{{ item.author }} | {{ item.keywords || '无关键词' }}</span>
            </div>
            <a-button type="text" danger size="small" @click="handleRemoveLiterature(item.id)">
              <DeleteOutlined />
            </a-button>
          </div>
        </div>
        <a-empty v-else description="暂无关联文献" />
      </a-tab-pane>

      <a-tab-pane key="requirements" tab="需求分析">
        <div class="tab-header">
          <h3>关联需求</h3>
          <a-button type="link" @click="goToRequirements">
            <PlusOutlined /> 添加需求
          </a-button>
        </div>
        <a-spin v-if="contentLoading && activeTab === 'requirements'" />
        <div v-else-if="projectContent?.requirements.value?.length" class="content-list">
          <div v-for="item in projectContent?.requirements.value" :key="item.id" class="content-item">
            <AppstoreOutlined style="font-size: 20px; color: #10B981;" />
            <div class="content-info">
              <span class="content-title">{{ item.title }}</span>
              <span class="content-meta">{{ item.textContent?.substring(0, 50) || '无内容' }}...</span>
            </div>
            <a-button type="text" danger size="small" @click="handleRemoveRequirement(item.id)">
              <DeleteOutlined />
            </a-button>
          </div>
        </div>
        <a-empty v-else description="暂无关联需求" />
      </a-tab-pane>

      <a-tab-pane key="ethics" tab="思政融合">
        <div class="tab-header">
          <h3>思政模块</h3>
          <a-button type="link" @click="goToEthics">
            <PlusOutlined /> 添加思政
          </a-button>
        </div>
        <a-spin v-if="contentLoading && activeTab === 'ethics'" />
        <div v-else-if="projectContent?.ethicsModules.value?.length" class="content-list">
          <div v-for="item in projectContent?.ethicsModules.value" :key="item.id" class="content-item">
            <HeartOutlined style="font-size: 20px; color: #8B5CF6;" />
            <div class="content-info">
              <span class="content-title">{{ item.title }}</span>
              <span class="content-meta">{{ item.category }} | {{ item.keywords || '无关键词' }}</span>
            </div>
            <a-button type="text" danger size="small" @click="handleRemoveEthicsModule(item.id)">
              <DeleteOutlined />
            </a-button>
          </div>
        </div>
        <a-empty v-else description="暂无关联思政模块" />
      </a-tab-pane>
    </a-tabs>

    <a-modal
      v-model:open="rollbackOpen"
      title="回退到上一阶段"
      ok-text="确认回退"
      cancel-text="取消"
      :confirm-loading="phaseActionLoading"
      @ok="handleRollbackModalOk"
    >
      <p class="rollback-tip">请说明回退原因，将写入审计日志。</p>
      <a-textarea v-model:value="rollbackReason" :rows="4" placeholder="至少 2 个字，例如：需补充文献后再进入需求分析" />
    </a-modal>
  </div>

  <div v-else-if="loading" class="loading-container">
    <a-spin size="large" />
  </div>

  <div v-else class="not-found">
    <a-empty description="项目不存在或您没有权限访问" />
  </div>

  <!-- AI 导师侧边栏 -->
  <MentorSidebar
    v-if="project"
    :project-id="project.id"
    :project-name="project.name"
    :phase="project.currentPhase"
    :progress="projectProgress"
  />
</template>

<style scoped>
.project-detail {
  padding: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

.project-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--color-border, #E5E7EB);
}

.header-left {
  flex: 1;
}

.project-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 8px 0;
}

.project-title {
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
  margin: 0;
}

.project-desc {
  color: var(--color-text-secondary, #6B7280);
  margin: 0;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.project-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 0;
}

.tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.tab-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary, #111827);
}

.content-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.content-placeholder {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 24px;
  background: var(--color-surface-hover, #F9FAFB);
  border-radius: 8px;
  color: var(--color-text-secondary, #6B7280);
}

.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
  margin-bottom: 32px;
}

.overview-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  background: var(--color-surface, #FFFFFF);
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.overview-icon {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}

.overview-info {
  display: flex;
  flex-direction: column;
}

.overview-num {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary, #111827);
}

.overview-label {
  font-size: 14px;
  color: var(--color-text-secondary, #6B7280);
}

.phase-progress {
  margin-bottom: 32px;
}

.phase-progress h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.phase-progress h4 {
  margin: 16px 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-text-primary, #374151);
}

.phase-actions {
  margin-top: 16px;
}

.phase-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}

.phase-checklist {
  margin-top: 8px;
}

.checklist-ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.checklist-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--color-border, #e5e7eb);
}

.checklist-row:last-child {
  border-bottom: none;
}

.checklist-row .icon {
  margin-top: 2px;
  font-size: 16px;
}

.checklist-row .icon.ok {
  color: #52c41a;
}

.checklist-row .icon.no {
  color: #faad14;
}

.checklist-label {
  flex: 1;
  min-width: 120px;
  color: var(--color-text-primary, #111827);
}

.checklist-hint {
  width: 100%;
  flex-basis: 100%;
  padding-left: 24px;
  font-size: 13px;
  color: var(--color-text-secondary, #6b7280);
}

.rollback-tip {
  margin-bottom: 8px;
  color: var(--color-text-secondary, #6b7280);
  font-size: 13px;
}

.members-section h3 {
  margin: 0 0 16px 0;
  font-size: 16px;
  font-weight: 600;
}

.members-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.member-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  background: var(--color-surface-hover, #F9FAFB);
  border-radius: 8px;
}

.member-name {
  flex: 1;
  color: var(--color-text-primary, #374151);
}

.loading-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.not-found {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 400px;
}

.content-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-surface, #FFFFFF);
  border-radius: 8px;
  border: 1px solid var(--color-border, #E5E7EB);
  margin-bottom: 8px;
  transition: all 0.2s;
}

.content-item:hover {
  border-color: var(--color-primary, #667eea);
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.1);
}

.content-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow: hidden;
}

.content-title {
  font-weight: 600;
  color: var(--color-text-primary, #111827);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.content-meta {
  font-size: 12px;
  color: var(--color-text-secondary, #6B7280);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>