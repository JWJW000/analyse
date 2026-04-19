<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  FileTextOutlined,
  ReadOutlined,
  AimOutlined,
  ThunderboltOutlined,
  BookOutlined,
  RobotOutlined,
  BulbOutlined,
  ArrowRightOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  ExclamationCircleOutlined,
  ProjectOutlined,
} from '@ant-design/icons-vue'
import http, { unwrap } from '@/api/http'
import type { ApiEnvelope } from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()

type MeStats = {
  requirementsTotal: number
  requirementsDraft: number
  requirementsSubmitted: number
  requirementsApproved: number
  matchEvents: number
  myLiterature: number
}

type RecMod = {
  moduleId: number
  title: string
  snippet: string
  reason: string
  score: number | null
}

type Project = {
  id: number
  name: string
  currentPhase: string
  progress: number
  updatedAt: string
}

type TodayTask = {
  id: number
  type: 'submission' | 'review' | 'literature' | 'requirement'
  title: string
  dueDate?: string
  status: 'pending' | 'overdue' | 'completed'
  projectId?: number
}

type AiSuggestion = {
  id: string
  type: 'analyze' | 'match' | 'review' | 'create'
  title: string
  description: string
  priority: 'high' | 'medium' | 'low'
  actionPath?: string
}

const stats = ref<MeStats | null>(null)
const loading = ref(true)
const recLoading = ref(true)
const recommendations = ref<RecMod[]>([])
const projects = ref<Project[]>([])
const todayTasks = ref<TodayTask[]>([])
const aiSuggestions = ref<AiSuggestion[]>([])
const projectsLoading = ref(true)
const showResourceMaintenance = computed(() => auth.role === 'TEACHER' || auth.role === 'ADMIN')

// 动态问候语
const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '早上好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const greetingIcon = computed(() => {
  const hour = new Date().getHours()
  if (hour < 12) return '🌤️'
  if (hour < 18) return '🌤️'
  return '🌙'
})

// 统计数据
const statCards = computed(() => [
  {
    key: 'requirements',
    title: '需求文档',
    value: stats.value?.requirementsTotal ?? 0,
    sub: `${stats.value?.requirementsDraft ?? 0} 草稿 · ${stats.value?.requirementsSubmitted ?? 0} 已提交`,
    icon: FileTextOutlined,
    color: 'primary',
    path: 'requirements',
  },
  {
    key: 'match',
    title: 'AI 匹配',
    value: stats.value?.matchEvents ?? 0,
    sub: '智能思政模块匹配次数',
    icon: RobotOutlined,
    color: 'purple',
    path: null,
  },
  {
    key: 'literature',
    title: '文献资料',
    value: stats.value?.myLiterature ?? 0,
    sub: '已创建的文献条目',
    icon: BookOutlined,
    color: 'green',
    path: 'literature',
  },
  {
    key: 'ethics',
    title: '任务闭环',
    value: auth.role === 'STUDENT' ? 4 : 1,
    sub: auth.role === 'STUDENT' ? '任务 · 需求 · 文献 · 伦理' : '课程任务统一入口',
    icon: ThunderboltOutlined,
    color: 'accent',
    path: null,
  },
])

// 快捷操作
const quickActions = computed(() => {
  const courseAction = {
    key: 'courses',
    title: '课程任务',
    desc: auth.role === 'STUDENT' ? '从任务进入需求、文献与伦理映射' : '发布任务、查看工作台与批改',
    icon: ThunderboltOutlined,
    color: '#E8A838',
    path: '/app/courses',
  }
  if (auth.role === 'STUDENT') {
    return [
      courseAction,
      {
        key: 'requirements',
        title: '我的需求',
        desc: '查看任务下沉淀的需求草稿与提交状态',
        icon: FileTextOutlined,
        color: '#5C7CFA',
        path: '/app/requirements',
      },
      {
        key: 'literature',
        title: '文献证据',
        desc: '上传和整理可关联到需求的文献',
        icon: ReadOutlined,
        color: '#51B786',
        path: '/app/literature',
      },
      {
        key: 'ethics',
        title: '伦理素材',
        desc: '检索可映射到需求的伦理思政模块',
        icon: AimOutlined,
        color: '#8B7FE8',
        path: '/app/ethics',
      },
    ]
  }
  return [
    courseAction,
    {
      key: 'requirements',
      title: '需求分析维护',
      desc: '查看和管理需求文档',
      icon: FileTextOutlined,
      color: '#5C7CFA',
      path: '/app/requirements',
    },
    {
      key: 'literature',
      title: '文献资源库',
      desc: '维护可映射到需求的文献资料',
      icon: ReadOutlined,
      color: '#51B786',
      path: '/app/literature',
    },
    {
      key: 'ethics',
      title: '伦理思政库',
      desc: '维护课程任务可引用的模块',
      icon: AimOutlined,
      color: '#8B7FE8',
      path: '/app/ethics',
    },
  ]
})

async function load() {
  loading.value = true
  try {
    stats.value = (await unwrap(http.get('/api/stats/me'))) as MeStats
  } catch {
    stats.value = null
  } finally {
    loading.value = false
  }
}

async function loadRecommendations() {
  recLoading.value = true
  try {
    recommendations.value = (await unwrap(
      http.get('/api/recommend/ethics-modules', { params: { limit: 6 } }),
    )) as RecMod[]
  } catch {
    recommendations.value = []
  } finally {
    recLoading.value = false
  }
}

async function loadProjects() {
  projectsLoading.value = true
  try {
    const projectList = await unwrap<Project[]>(http.get<ApiEnvelope<Project[]>>('/api/projects'))
    projects.value = projectList.slice(0, 3)
    generateTodayTasks(projectList)
    generateAiSuggestions(projectList)
  } catch {
    projects.value = []
  } finally {
    projectsLoading.value = false
  }
}

function generateTodayTasks(projectList: Project[]) {
  const tasks: TodayTask[] = []
  
  projectList.forEach(p => {
    if (p.currentPhase === 'LITERATURE' && p.progress < 100) {
      tasks.push({
        id: p.id,
        type: 'literature',
        title: `项目「${p.name}」文献调研`,
        status: 'pending',
        projectId: p.id
      })
    }
    if (p.currentPhase === 'REQUIREMENTS' && p.progress < 100) {
      tasks.push({
        id: p.id + 1000,
        type: 'requirement',
        title: `项目「${p.name}」需求分析`,
        status: 'pending',
        projectId: p.id
      })
    }
    if (p.currentPhase === 'SUBMISSION') {
      tasks.push({
        id: p.id + 2000,
        type: 'submission',
        title: `项目「${p.name}」待提交`,
        status: 'pending',
        projectId: p.id
      })
    }
  })

  if (stats.value?.requirementsDraft && stats.value.requirementsDraft > 0) {
    tasks.unshift({
      id: 99999,
      type: 'requirement',
      title: `您有 ${stats.value.requirementsDraft} 个需求文档草稿未提交`,
      status: 'pending'
    })
  }

  todayTasks.value = tasks.slice(0, 5)
}

function generateAiSuggestions(projectList: Project[]) {
  const suggestions: AiSuggestion[] = []

  projectList.forEach(p => {
    if (p.currentPhase === 'LITERATURE') {
      suggestions.push({
        id: `ai-lit-${p.id}`,
        type: 'analyze',
        title: '建议进行文献AI分析',
        description: `项目「${p.name}」处于文献调研阶段，可以使用AI分析已有文献`,
        priority: 'medium',
        actionPath: `/app/literature`
      })
    }
    if (p.currentPhase === 'REQUIREMENTS') {
      suggestions.push({
        id: `ai-req-${p.id}`,
        type: 'create',
        title: 'AI生成需求文档',
        description: `项目「${p.name}」需要需求文档，尝试使用AI辅助生成`,
        priority: 'high',
        actionPath: `/app/requirements`
      })
    }
    if (p.currentPhase === 'ETHICS') {
      suggestions.push({
        id: `ai-eth-${p.id}`,
        type: 'match',
        title: '思政元素融合',
        description: `项目「${p.name}」可以尝试AI思政匹配功能`,
        priority: 'medium',
        actionPath: `/app/ethics`
      })
    }
  })

  if (projects.value.length === 0) {
    suggestions.push({
      id: 'ai-create-project',
      type: 'create',
      title: '创建新项目',
      description: '您还没有进行中的项目，创建项目开始您的工程教育之旅',
      priority: 'high',
      actionPath: '/app/projects'
    })
  }

  aiSuggestions.value = suggestions.slice(0, 3)
}

function getTaskIcon(type: string) {
  switch (type) {
    case 'submission': return CheckCircleOutlined
    case 'review': return ClockCircleOutlined
    case 'literature': return ReadOutlined
    case 'requirement': return FileTextOutlined
    default: return ExclamationCircleOutlined
  }
}

function getTaskStatusColor(status: string) {
  switch (status) {
    case 'completed': return 'green'
    case 'overdue': return 'red'
    default: return 'blue'
  }
}

function getSuggestionPriorityColor(priority: string) {
  switch (priority) {
    case 'high': return 'red'
    case 'medium': return 'orange'
    default: return 'blue'
  }
}

function getPhaseLabel(phase: string) {
  const phaseMap: Record<string, string> = {
    LITERATURE: '文献调研',
    REQUIREMENTS: '需求分析',
    ETHICS: '思政融合',
    SUBMISSION: '提交审核',
    REVIEW: '评审完成'
  }
  return phaseMap[phase] || phase
}

function reasonLabel(r: string) {
  if (r === 'CONTENT_SIMILARITY') return '内容相似'
  if (r === 'POPULAR') return '热门推荐'
  return r
}

function getTagColor(reason: string) {
  return reason === 'POPULAR' ? 'blue' : 'green'
}

function goTo(path: string) {
  router.push(path)
}

onMounted(() => {
  load()
  loadRecommendations()
  loadProjects()
})
</script>

<template>
  <div class="home-page">
    <!-- 欢迎区域 -->
    <div class="welcome-section">
      <div class="welcome-content">
        <div class="welcome-text">
          <h1 class="welcome-title">
            {{ greetingIcon }} {{ greeting }}，{{ auth.displayName || auth.username }}
          </h1>
          <p class="welcome-subtitle">
            <a-tag color="gold" size="small">{{ auth.role }}</a-tag>
            继续您的工作吧
          </p>
        </div>
        <div class="welcome-date">
          {{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' }) }}
        </div>
      </div>
    </div>

    <!-- 统计卡片区域 -->
    <div class="stats-section">
      <a-row :gutter="[20, 20]">
        <a-col :xs="12" :sm="12" :lg="6" v-for="(stat, index) in statCards" :key="stat.key">
          <a-card 
            class="stat-card" 
            :class="`stat-card--${stat.color}`"
            :style="{ animationDelay: `${index * 100}ms` }"
            hoverable
            @click="stat.path && goTo(stat.path)"
          >
            <div class="stat-inner">
              <div class="stat-icon-wrap">
                <component :is="stat.icon" />
              </div>
              <div class="stat-info">
                <div class="stat-title">{{ stat.title }}</div>
                <a-statistic :value="stat.value" class="stat-value" />
                <div class="stat-sub">{{ stat.sub }}</div>
              </div>
            </div>
            <div class="stat-decoration">
              <component :is="stat.icon" />
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 主内容区域 -->
    <a-row :gutter="[24, 20]" class="main-content">
      <!-- 左侧主内容 -->
      <a-col :xs="24" :lg="16">
        <!-- 今日任务 -->
        <a-card v-if="showResourceMaintenance" class="tasks-card" :bordered="false">
          <template #title>
            <div class="section-header">
              <ClockCircleOutlined /> 今日任务
              <a-badge :count="todayTasks.length" :number-style="{ backgroundColor: '#1890ff' }" style="margin-left: 8px" />
            </div>
          </template>
          <a-spin :spinning="projectsLoading">
            <div v-if="todayTasks.length" class="tasks-list">
              <div 
                v-for="task in todayTasks" 
                :key="task.id"
                class="task-item"
                @click="task.projectId && goTo(`/app/projects/${task.projectId}`)"
              >
                <div class="task-icon" :class="`task-icon--${task.status}`">
                  <component :is="getTaskIcon(task.type)" />
                </div>
                <div class="task-content">
                  <div class="task-title">{{ task.title }}</div>
                  <div class="task-meta">
                    <a-tag :color="getTaskStatusColor(task.status)" size="small">
                      {{ task.status === 'pending' ? '待处理' : task.status === 'overdue' ? '已逾期' : '已完成' }}
                    </a-tag>
                  </div>
                </div>
                <ArrowRightOutlined class="task-arrow" />
              </div>
            </div>
            <a-empty v-else description="暂无待处理任务" />
          </a-spin>
        </a-card>

        <!-- AI智能建议 -->
        <a-card v-if="showResourceMaintenance" class="ai-suggestions-card" :bordered="false">
          <template #title>
            <div class="section-header">
              <RobotOutlined /> AI智能建议
            </div>
          </template>
          <a-spin :spinning="projectsLoading">
            <div v-if="aiSuggestions.length" class="suggestions-list">
              <div 
                v-for="suggestion in aiSuggestions" 
                :key="suggestion.id"
                class="suggestion-item"
                @click="suggestion.actionPath && goTo(suggestion.actionPath)"
              >
                <div class="suggestion-header">
                  <div class="suggestion-title">{{ suggestion.title }}</div>
                  <a-tag :color="getSuggestionPriorityColor(suggestion.priority)" size="small">
                    {{ suggestion.priority === 'high' ? '高优' : suggestion.priority === 'medium' ? '中优' : '低优' }}
                  </a-tag>
                </div>
                <div class="suggestion-desc">{{ suggestion.description }}</div>
                <div v-if="suggestion.actionPath" class="suggestion-action">
                  立即处理 <ArrowRightOutlined />
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无AI建议" />
          </a-spin>
        </a-card>

        <!-- 项目进度 -->
        <a-card v-if="showResourceMaintenance" class="projects-progress-card" :bordered="false">
          <template #title>
            <div class="section-header">
              <ProjectOutlined /> 项目进度
              <a-button type="link" size="small" @click="goTo('/app/projects')">
                查看全部 <ArrowRightOutlined />
              </a-button>
            </div>
          </template>
          <a-spin :spinning="projectsLoading">
            <div v-if="projects.length" class="projects-list">
              <div 
                v-for="project in projects" 
                :key="project.id"
                class="project-item"
                @click="goTo(`/app/projects/${project.id}`)"
              >
                <div class="project-info">
                  <div class="project-name">{{ project.name }}</div>
                  <div class="project-phase">{{ getPhaseLabel(project.currentPhase) }}</div>
                </div>
                <div class="project-progress">
                  <a-progress 
                    :percent="project.progress" 
                    :stroke-color="project.progress === 100 ? '#52c41a' : '#1890ff'"
                    size="small"
                    :show-info="false"
                  />
                  <span class="progress-text">{{ project.progress }}%</span>
                </div>
              </div>
            </div>
            <a-empty v-else description="暂无进行中的项目" />
          </a-spin>
        </a-card>

        <!-- 快捷操作 -->
        <a-card class="quick-actions-card" :bordered="false">
          <template #title>
            <div class="section-header">
              <span>快捷操作</span>
            </div>
          </template>
          <a-row :gutter="[12, 12]">
            <a-col :xs="12" :sm="12" :lg="6" v-for="action in quickActions" :key="action.key">
              <div 
                class="quick-action-item" 
                @click="goTo(action.path)"
              >
                <div class="quick-action-icon" :style="{ backgroundColor: action.color + '12', color: action.color }">
                  <component :is="action.icon" />
                </div>
                <div class="quick-action-info">
                  <div class="quick-action-title">{{ action.title }}</div>
                  <div class="quick-action-desc">{{ action.desc }}</div>
                </div>
              </div>
            </a-col>
          </a-row>
        </a-card>

        <!-- 思政推荐 - 有数据时显示 -->
        <a-card v-if="showResourceMaintenance && (recommendations.length || recLoading)" class="recommend-card" :bordered="false">
          <template #title>
            <div class="section-header">
              <span>工程伦理思政模块推荐</span>
              <a-button type="link" size="small" @click="goTo('/app/ethics')">
                查看全部 <ArrowRightOutlined />
              </a-button>
            </div>
          </template>
          
          <a-spin :spinning="recLoading">
            <div v-if="recommendations.length" class="recommend-list">
              <div 
                v-for="(item, index) in recommendations" 
                :key="item.moduleId"
                class="recommend-item"
                :style="{ animationDelay: `${index * 80}ms` }"
              >
                <div class="recommend-item-content">
                  <div class="recommend-item-title">{{ item.title }}</div>
                  <div class="recommend-item-snippet">{{ (item.snippet || '').slice(0, 60) }}...</div>
                </div>
                <div class="recommend-item-action">
                  <a-tag :color="getTagColor(item.reason)">{{ reasonLabel(item.reason) }}</a-tag>
                  <a-button type="link" size="small" @click="goTo(`/app/ethics?q=${encodeURIComponent(item.title.slice(0, 20))}`)">
                    查看
                  </a-button>
                </div>
              </div>
            </div>
          </a-spin>
        </a-card>
      </a-col>

      <!-- 右侧边栏 -->
      <a-col :xs="24" :lg="8">
        <div class="sidebar-area">
          <!-- 使用提示 -->
          <a-card class="tips-card" :bordered="false">
            <template #title>
              <div class="section-header">
                <BulbOutlined /> 使用提示
              </div>
            </template>
            <div class="tips-content">
              <div class="tip-item">
                <span class="tip-num">1</span>
                <div class="tip-text">
                  <strong>学生账号</strong>可自助注册，教师和管理员账号由管理员创建
                </div>
              </div>
              <div class="tip-item">
                <span class="tip-num">2</span>
                <div class="tip-text">
                  从<strong>课程任务</strong>进入工作台，补齐需求、文献证据和伦理映射
                </div>
              </div>
              <div class="tip-item">
                <span class="tip-num">3</span>
                <div class="tip-text">
                  关联<strong>课程作业</strong>后可提交给教师审核
                </div>
              </div>
            </div>
          </a-card>

        
         
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.home-page {
  max-width: 1400px;
  animation: fadeIn 300ms ease;
}

/* === 欢迎区域 - 毛玻璃紧凑设计 === */
.welcome-section {
  background: linear-gradient(135deg, rgba(66, 99, 235, 0.85) 0%, rgba(92, 124, 250, 0.85) 100%);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: var(--radius-lg);
  padding: 20px 28px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.welcome-section::before {
  content: '';
  position: absolute;
  top: -60%;
  right: -5%;
  width: 200px;
  height: 200px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 50%;
}

.welcome-section::after {
  content: '';
  position: absolute;
  bottom: -40%;
  right: 15%;
  width: 120px;
  height: 120px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 50%;
}

.welcome-content {
  position: relative;
  z-index: 1;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text {
  display: flex;
  align-items: center;
  gap: 12px;
}

.welcome-title {
  font-size: 18px;
  font-weight: 600;
  color: white;
  margin: 0;
}

.welcome-subtitle {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.8);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.welcome-date {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

/* === 统计卡片 === */
.stats-section {
  margin-bottom: 20px;
}

.stats-section :deep(.ant-row) {
  margin: 0 !important;
}

.stats-section :deep(.ant-col) {
  padding: 0 10px !important;
}

.stats-section :deep(.ant-col:first-child) {
  padding-left: 0 !important;
}

.stats-section :deep(.ant-col:last-child) {
  padding-right: 0 !important;
}

.stat-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-md) !important;
  animation: fadeInUp 400ms ease forwards;
  opacity: 0;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04), 0 4px 12px rgba(0, 0, 0, 0.03);
  transition: all 200ms ease;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.stat-inner {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  position: relative;
  z-index: 1;
}

.stat-icon-wrap {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  flex-shrink: 0;
}

.stat-card--primary .stat-icon-wrap {
  background: rgba(66, 99, 235, 0.1);
  color: #4263EB;
}

.stat-card--purple .stat-icon-wrap {
  background: rgba(112, 72, 232, 0.1);
  color: #7048E8;
}

.stat-card--green .stat-icon-wrap {
  background: rgba(55, 178, 77, 0.1);
  color: #37B24D;
}

.stat-card--accent .stat-icon-wrap {
  background: rgba(246, 103, 7, 0.1);
  color: #F76707;
}

.stat-info {
  flex: 1;
  min-width: 0;
}

.stat-title {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 2px;
}

.stat-value {
  margin: 0 !important;
}

.stat-value :deep(.ant-statistic-content-value) {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.stat-sub {
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.stat-decoration {
  position: absolute;
  right: -8px;
  bottom: -8px;
  font-size: 56px;
  opacity: 0.05;
  transform: rotate(-15deg);
}

/* === 快捷操作 === */
.quick-actions-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md) !important;
}

.quick-actions-card :deep(.ant-card-body) {
  padding: 20px;
}

.quick-actions-card :deep(.ant-row) {
  margin: 0 !important;
}

.quick-actions-card :deep(.ant-col) {
  padding: 0 8px !important;
}

.quick-actions-card :deep(.ant-col:first-child) {
  padding-left: 0 !important;
}

.quick-actions-card :deep(.ant-col:last-child) {
  padding-right: 0 !important;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 14px;
}

.quick-action-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  padding: 16px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 200ms ease;
  background: var(--color-background);
  border: 1px solid var(--color-border);
}

.quick-action-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border-color: transparent;
  background: var(--color-surface);
}

.quick-action-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-bottom: 10px;
}

.quick-action-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 3px;
}

.quick-action-desc {
  font-size: 11px;
  color: var(--color-text-tertiary);
  line-height: 1.3;
}

/* === 推荐列表 - 横向流式布局 === */
.recommend-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md) !important;
}

.recommend-card :deep(.ant-card-body) {
  padding: 20px;
}

.recommend-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 12px;
}

.recommend-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 14px 16px;
  background: var(--color-background);
  border-radius: var(--radius-md);
  transition: all 200ms ease;
  animation: fadeInUp 300ms ease forwards;
  opacity: 0;
  border: 1px solid var(--color-border);
}

.recommend-item:hover {
  background: var(--color-surface);
  border-color: transparent;
}

.recommend-item-content {
  flex: 1;
  min-width: 0;
}

.recommend-item-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-item-snippet {
  font-size: 12px;
  color: var(--color-text-secondary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recommend-item-action {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
  margin-left: 12px;
}

/* === 空状态 === */
.empty-illustration {
  width: 100px;
  margin: 0 auto 12px;
}

.empty-hint {
  color: var(--color-text-tertiary);
  font-size: 12px;
  margin-bottom: 12px;
}

/* === 侧边栏区域 === */
.sidebar-area {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* === 今日任务卡片 === */
.tasks-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md) !important;
}

.tasks-card :deep(.ant-card-body) {
  padding: 16px 20px;
}

.tasks-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--color-background);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 200ms ease;
  border: 1px solid var(--color-border);
}

.task-item:hover {
  background: var(--color-surface);
  border-color: transparent;
  transform: translateX(4px);
}

.task-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.task-icon--pending {
  background: rgba(24, 144, 255, 0.1);
  color: #1890ff;
}

.task-icon--overdue {
  background: rgba(255, 77, 79, 0.1);
  color: #ff4d4f;
}

.task-icon--completed {
  background: rgba(82, 196, 26, 0.1);
  color: #52c41a;
}

.task-content {
  flex: 1;
  min-width: 0;
}

.task-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.task-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.task-arrow {
  color: var(--color-text-tertiary);
  font-size: 12px;
}

/* === AI智能建议卡片 === */
.ai-suggestions-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md) !important;
  background: linear-gradient(135deg, rgba(112, 72, 232, 0.04) 0%, var(--color-surface) 100%);
  border: 1px solid var(--color-border);
}

.ai-suggestions-card :deep(.ant-card-body) {
  padding: 16px 20px;
}

.suggestions-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.suggestion-item {
  padding: 14px 16px;
  background: var(--color-background);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 200ms ease;
  border: 1px solid var(--color-border);
}

.suggestion-item:hover {
  background: rgba(112, 72, 232, 0.04);
  border-color: rgba(112, 72, 232, 0.3);
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}

.suggestion-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.suggestion-desc {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
  margin-bottom: 8px;
}

.suggestion-action {
  font-size: 12px;
  color: #7048e8;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* === 项目进度卡片 === */
.projects-progress-card {
  margin-bottom: 20px;
  border-radius: var(--radius-md) !important;
}

.projects-progress-card :deep(.ant-card-body) {
  padding: 16px 20px;
}

.projects-list {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.project-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background: var(--color-background);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all 200ms ease;
  border: 1px solid var(--color-border);
}

.project-item:hover {
  background: var(--color-surface);
  border-color: transparent;
  transform: translateX(4px);
}

.project-info {
  flex: 1;
  min-width: 0;
}

.project-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 4px;
}

.project-phase {
  font-size: 12px;
  color: var(--color-text-secondary);
}

.project-progress {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 140px;
}

.progress-text {
  font-size: 12px;
  font-weight: 500;
  color: var(--color-text-secondary);
  width: 32px;
  text-align: right;
}

/* === 提示卡片 - 无边框设计 === */
.tips-card {
  margin-bottom: 0;
  border-radius: var(--radius-md) !important;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.tips-card :deep(.ant-card-body) {
  padding: 16px;
}

.tips-content {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.tip-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.tip-num {
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: var(--color-primary);
  color: white;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.tip-text {
  font-size: 12px;
  color: var(--color-text-secondary);
  line-height: 1.5;
}

.tip-text strong {
  color: var(--color-text-primary);
}

/* === 角色信息 - 渐变背景 === */
.role-card {
  border-radius: var(--radius-md) !important;
  background: linear-gradient(135deg, rgba(66, 99, 235, 0.08) 0%, var(--color-surface) 100%);
  border: 1px solid var(--color-border);
}

.role-card :deep(.ant-card-body) {
  padding: 16px;
}

.role-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 10px;
}

.role-item {
  display: flex;
  align-items: center;
  gap: 10px;
}

.role-label {
  font-size: 12px;
  color: var(--color-text-secondary);
  width: 36px;
}

.role-value {
  font-family: var(--font-mono);
  font-size: 13px;
  background: var(--color-background);
  padding: 3px 10px;
  border-radius: var(--radius-sm);
  color: var(--color-primary);
  border: 1px solid var(--color-border);
}

.role-tip {
  font-size: 11px;
  color: var(--color-text-tertiary);
}

/* === 动画 === */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(16px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* === 响应式 === */
@media (max-width: 767px) {
  .welcome-section {
    padding: 16px 20px;
  }

  .welcome-title {
    font-size: 16px;
  }

  .welcome-subtitle {
    display: none;
  }

  .welcome-date {
    display: none;
  }

  .stat-card {
    margin-bottom: 0;
  }

  .stats-section :deep(.ant-col) {
    padding: 0 5px !important;
  }

  .quick-actions-card :deep(.ant-col) {
    padding: 0 5px !important;
  }
}
</style>
