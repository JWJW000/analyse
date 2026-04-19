<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import dayjs from 'dayjs'
import {
  BookOutlined,
  FileTextOutlined,
  CommentOutlined,
  BarChartOutlined,
  PlusOutlined,
  UserAddOutlined,
} from '@ant-design/icons-vue'

type Course = { id: number; name: string; code: string | null; teacherId: number; createdAt: string }
type Assignment = {
  id: number
  courseId: number
  title: string
  description: string | null
  dueAt: string | null
  createdBy: number | null
  createdAt: string
}
type StudentOption = {
  id: number
  username: string
  displayName: string | null
  role: string
}

const auth = useAuthStore()
const router = useRouter()
const courses = ref<Course[]>([])
const assignments = ref<Assignment[]>([])
const selectedCourse = ref<Course | null>(null)

const studentOptions = ref<StudentOption[]>([])
const selectedStudentIds = ref<number[]>([])
const studentSearchLoading = ref(false)
const createStudentLoading = ref(false)
const newStudent = ref({ username: '', password: '123456', displayName: '' })
const newCourse = ref({ name: '', code: '' })
const newAssign = ref({ title: '', description: '', dueAt: '' })

const isTeacher = computed(() => auth.role === 'TEACHER' || auth.role === 'ADMIN')

async function load() {
  const res = await unwrap(http.get('/api/courses/mine'))
  courses.value = res as Course[]
}

async function selectCourse(course: Course) {
  selectedCourse.value = course
  const res = await unwrap(http.get(`/api/courses/${course.id}/assignments`))
  assignments.value = res as Assignment[]
  await searchStudents('')
}

async function createCourse() {
  if (!newCourse.value.name.trim()) {
    message.warning('请输入课程名称')
    return
  }
  await unwrap(http.post('/api/courses', newCourse.value))
  message.success('课程已创建')
  newCourse.value = { name: '', code: '' }
  await load()
}

async function searchStudents(q: string) {
  if (!isTeacher.value) return
  studentSearchLoading.value = true
  try {
    studentOptions.value = (await unwrap(
      http.get('/api/users/students', { params: { q: q || undefined } }),
    )) as StudentOption[]
  } catch {
    studentOptions.value = []
  } finally {
    studentSearchLoading.value = false
  }
}

async function enroll() {
  if (!selectedCourse.value || !selectedStudentIds.value.length) {
    message.warning('请选择学生')
    return
  }
  await Promise.all(
    selectedStudentIds.value.map((studentId) =>
      unwrap(http.post(`/api/courses/${selectedCourse.value!.id}/enroll`, { studentId })),
    ),
  )
  message.success(`已加入 ${selectedStudentIds.value.length} 名学生`)
  selectedStudentIds.value = []
}

async function createStudentAndEnroll() {
  if (!selectedCourse.value) return
  if (!newStudent.value.username.trim()) {
    message.warning('请输入学生用户名')
    return
  }
  if (newStudent.value.password.length < 6) {
    message.warning('密码至少 6 位')
    return
  }
  createStudentLoading.value = true
  try {
    const created = (await unwrap(
      http.post(`/api/courses/${selectedCourse.value.id}/students`, {
        username: newStudent.value.username.trim(),
        password: newStudent.value.password,
        displayName: newStudent.value.displayName || undefined,
      }),
    )) as StudentOption
    message.success('学生已创建并加入课程')
    newStudent.value = { username: '', password: '123456', displayName: '' }
    await searchStudents('')
    selectedStudentIds.value = [created.id]
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '创建学生失败')
  } finally {
    createStudentLoading.value = false
  }
}

function studentLabel(s: StudentOption) {
  const name = s.displayName || s.username
  return `${name}（${s.username} · #${s.id}）`
}

async function createAssign() {
  if (!selectedCourse.value || !newAssign.value.title.trim()) {
    message.warning('请输入作业标题')
    return
  }
  await unwrap(
    http.post(`/api/courses/${selectedCourse.value.id}/assignments`, {
      title: newAssign.value.title,
      description: newAssign.value.description || undefined,
      dueAt: newAssign.value.dueAt || undefined,
    }),
  )
  message.success('作业已发布')
  newAssign.value = { title: '', description: '', dueAt: '' }
  await selectCourse(selectedCourse.value)
}

function goReview(a: Assignment) {
  if (!selectedCourse.value) return
  router.push({
    name: 'assignment-review',
    params: { courseId: String(selectedCourse.value.id), assignmentId: String(a.id) },
  })
}

function goStats(course = selectedCourse.value) {
  if (!course) return
  router.push({ name: 'stats-course', params: { courseId: String(course.id) } })
}

function goDiscussion() {
  if (!selectedCourse.value) return
  router.push({ name: 'course-discussion', params: { courseId: String(selectedCourse.value.id) } })
}

function goTaskWorkspace(a: Assignment) {
  router.push({ name: 'task-workspace', params: { assignmentId: String(a.id) } })
}

function formatDue(dueAt: string | null): { text: string; status: 'success' | 'warning' | 'error' | 'default' } {
  if (!dueAt) return { text: '无截止日期', status: 'default' }
  const due = dayjs(dueAt)
  const now = dayjs()
  if (due.isBefore(now)) return { text: `已截止 · ${due.format('MM-DD HH:mm')}`, status: 'error' }
  if (due.diff(now, 'hour') < 24) return { text: `即将截止 · ${due.format('MM-DD HH:mm')}`, status: 'warning' }
  return { text: `截止 ${due.format('MM-DD HH:mm')}`, status: 'success' }
}

onMounted(load)
</script>

<template>
  <div class="courses-page">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-content">
        <h2>课程任务</h2>
        <p class="subtitle">{{ isTeacher ? '管理课程、发布作业、批改学生提交' : '查看课程作业、提交需求文档' }}</p>
      </div>
    </div>

    <a-row :gutter="[24, 24]" class="courses-layout">
      <!-- 左侧：课程列表 -->
      <a-col :xs="24" :lg="selectedCourse ? 8 : 24">
        <div class="section">
          <div class="section-header">
            <span class="section-title">
              <BookOutlined /> 我的课程
            </span>
            <span class="course-count">{{ courses.length }} 个课程</span>
          </div>

          <!-- 课程卡片列表 -->
          <div v-if="courses.length" class="course-cards">
            <div
              v-for="(course, index) in courses"
              :key="course.id"
              class="course-card"
              :class="{ 'course-card--selected': selectedCourse?.id === course.id }"
              :style="{ animationDelay: `${index * 80}ms` }"
              @click="selectCourse(course)"
            >
              <div class="course-card-header">
                <div class="course-icon">
                  <BookOutlined />
                </div>
                <div class="course-info">
                  <h4 class="course-name">{{ course.name }}</h4>
                  <div class="course-meta">
                    <a-tag v-if="course.code" color="blue" size="small">{{ course.code }}</a-tag>
                    <span class="assignment-count">{{ assignments.filter(a => a.courseId === course.id).length || '—' }} 个作业</span>
                  </div>
                </div>
              </div>
              
              <div v-if="isTeacher" class="course-actions" @click.stop>
                <a-space size="small">
                  <a-button type="text" size="small" @click="goDiscussion">
                    <CommentOutlined /> 讨论
                  </a-button>
                  <a-button type="text" size="small" @click="goStats(course)">
                    <BarChartOutlined /> 统计
                  </a-button>
                </a-space>
              </div>
            </div>
          </div>

          <a-empty v-else description="暂无课程" class="empty-state">
            <template #image>
              <div class="empty-illustration">
                <BookOutlined />
              </div>
            </template>
          </a-empty>

          <!-- 创建课程（教师/管理员） -->
          <div v-if="isTeacher" class="create-section">
            <a-collapse :bordered="false" class="create-collapse">
              <a-collapse-panel key="create" :header="'创建新课程'">
                <a-form layout="vertical" class="create-form">
                  <a-form-item label="课程名称" required>
                    <a-input v-model:value="newCourse.name" placeholder="如：软件工程伦理与思政" />
                  </a-form-item>
                  <a-form-item label="课程代码">
                    <a-input v-model:value="newCourse.code" placeholder="如：SE-ETHICS-2024" />
                  </a-form-item>
                  <a-button type="primary" block @click="createCourse">
                    <PlusOutlined /> 创建课程
                  </a-button>
                </a-form>
              </a-collapse-panel>
            </a-collapse>
          </div>
        </div>
      </a-col>

      <!-- 右侧：作业详情 -->
      <a-col :xs="24" :lg="16" v-if="selectedCourse">
        <div class="section">
          <div class="section-header">
            <span class="section-title">
              <FileTextOutlined /> {{ selectedCourse.name }}
            </span>
          </div>

          <!-- 教师操作栏 -->
          <div v-if="isTeacher" class="teacher-toolbar">
            <a-space wrap>
              <a-button type="primary" @click="goStats">
                <BarChartOutlined /> 班级统计
              </a-button>
              <a-button @click="goDiscussion">
                <CommentOutlined /> 讨论区
              </a-button>
            </a-space>
          </div>

          <!-- 作业列表 -->
          <div v-if="assignments.length" class="assignments-list">
            <div
              v-for="(assign, index) in assignments"
              :key="assign.id"
              class="assignment-card"
              :style="{ animationDelay: `${index * 80}ms` }"
            >
              <div class="assignment-header">
                <div class="assignment-info">
                  <h4 class="assignment-title">{{ assign.title }}</h4>
                  <div class="assignment-meta">
                    <a-tag 
                      :color="formatDue(assign.dueAt).status === 'error' ? 'red' : formatDue(assign.dueAt).status === 'warning' ? 'orange' : 'green'"
                      size="small"
                    >
                      {{ formatDue(assign.dueAt).text }}
                    </a-tag>
                    <span class="assignment-date">发布于 {{ dayjs(assign.createdAt).format('YYYY-MM-DD') }}</span>
                  </div>
                </div>
                <div class="assignment-actions">
                  <a-button v-if="!isTeacher" type="primary" @click="goTaskWorkspace(assign)">
                    进入任务工作台
                  </a-button>
                  <a-space v-else>
                    <a-button @click="goTaskWorkspace(assign)">工作台</a-button>
                    <a-button type="primary" @click="goReview(assign)">
                      <FileTextOutlined /> 批改
                    </a-button>
                  </a-space>
                </div>
              </div>
              <p v-if="assign.description" class="assignment-desc">{{ assign.description }}</p>
            </div>
          </div>

          <a-empty v-else description="暂无作业">
            <template #image>
              <div class="empty-illustration">
                <FileTextOutlined />
              </div>
            </template>
          </a-empty>

          <!-- 教师操作区 -->
          <div v-if="isTeacher" class="teacher-actions">
            <a-tabs>
              <a-tab-pane key="assign" tab="发布作业">
                <a-form layout="vertical" class="action-form">
                  <a-form-item label="作业标题" required>
                    <a-input v-model:value="newAssign.title" placeholder="如：第二次需求分析报告" />
                  </a-form-item>
                  <a-form-item label="作业说明">
                    <a-textarea v-model:value="newAssign.description" placeholder="详细描述作业要求..." :rows="3" />
                  </a-form-item>
                  <a-form-item label="截止时间">
                    <a-input v-model:value="newAssign.dueAt" placeholder="格式：2024-12-31T23:59" />
                    <template #extra>使用 ISO 格式，如 2024-12-31T23:59</template>
                  </a-form-item>
                  <a-button type="primary" @click="createAssign">
                    <PlusOutlined /> 发布作业
                  </a-button>
                </a-form>
              </a-tab-pane>

              <a-tab-pane key="enroll" tab="加入学生">
                <a-form layout="vertical" class="action-form">
                  <a-form-item label="选择学生" required>
                    <a-select
                      v-model:value="selectedStudentIds"
                      mode="multiple"
                      show-search
                      :filter-option="false"
                      :loading="studentSearchLoading"
                      placeholder="输入姓名或用户名搜索学生"
                      style="width: 100%"
                      :options="studentOptions.map((s) => ({ label: studentLabel(s), value: s.id }))"
                      :not-found-content="studentSearchLoading ? '搜索中...' : '暂无学生，可在下方新建'"
                      @search="searchStudents"
                    />
                  </a-form-item>
                  <a-space wrap>
                    <a-button @click="searchStudents('')">刷新学生</a-button>
                    <a-button type="primary" @click="enroll">
                      <UserAddOutlined /> 加入课程
                    </a-button>
                  </a-space>
                  <a-divider orientation="left">没有学生账号时</a-divider>
                  <a-row :gutter="12">
                    <a-col :xs="24" :md="8">
                      <a-form-item label="用户名" required>
                        <a-input v-model:value="newStudent.username" placeholder="如 student01" />
                      </a-form-item>
                    </a-col>
                    <a-col :xs="24" :md="8">
                      <a-form-item label="初始密码" required>
                        <a-input-password v-model:value="newStudent.password" />
                      </a-form-item>
                    </a-col>
                    <a-col :xs="24" :md="8">
                      <a-form-item label="显示名">
                        <a-input v-model:value="newStudent.displayName" placeholder="如 张三" />
                      </a-form-item>
                    </a-col>
                  </a-row>
                  <a-button :loading="createStudentLoading" @click="createStudentAndEnroll">
                    创建学生并加入课程
                  </a-button>
                </a-form>
              </a-tab-pane>
            </a-tabs>
          </div>
        </div>
      </a-col>

      <!-- 未选择课程时的占位 -->
      <a-col :xs="24" :lg="16" v-else>
        <div class="section placeholder-section">
          <div class="placeholder-content">
            <div class="placeholder-icon">📚</div>
            <h3>选择一个课程</h3>
            <p>点击左侧课程卡片查看作业列表和详情</p>
          </div>
        </div>
      </a-col>
    </a-row>
  </div>
</template>

<style scoped>
.courses-page {
  max-width: 1400px;
  animation: fadeIn 300ms ease;
}

/* === 页面标题 === */
.page-header {
  margin-bottom: 24px;
}

.header-content h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px;
}

.subtitle {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* === 布局 === */
.courses-layout {
  width: 100%;
}

/* === 通用区块 === */
.section {
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  padding: 24px;
  border: 1px solid var(--color-border);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--color-text-primary);
  display: flex;
  align-items: center;
  gap: 8px;
}

.course-count {
  font-size: 13px;
  color: var(--color-text-tertiary);
}

/* === 课程卡片 === */
.course-cards {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.course-card {
  padding: 16px;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: all 200ms ease;
  animation: fadeInUp 300ms ease forwards;
  opacity: 0;
}

.course-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
  border-color: var(--color-primary);
}

.course-card--selected {
  border-color: var(--color-primary);
  background: var(--color-primary-light);
}

.course-card-header {
  display: flex;
  gap: 12px;
}

.course-icon {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-lg);
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.course-info {
  flex: 1;
  min-width: 0;
}

.course-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 6px;
}

.course-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.assignment-count {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.course-actions {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--color-border);
}

/* === 创建课程 === */
.create-section {
  margin-top: 20px;
}

.create-collapse {
  background: var(--color-background);
  border-radius: var(--radius-lg) !important;
}

.create-collapse :deep(.ant-collapse-header) {
  font-weight: 600;
  padding: 12px 16px !important;
}

.create-form {
  margin-top: 16px;
}

/* === 教师工具栏 === */
.teacher-toolbar {
  margin-bottom: 20px;
  padding: 16px;
  background: var(--color-background);
  border-radius: var(--radius-lg);
}

/* === 作业列表 === */
.assignments-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.assignment-card {
  padding: 20px;
  background: var(--color-background);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  transition: all 200ms ease;
  animation: fadeInUp 300ms ease forwards;
  opacity: 0;
}

.assignment-card:hover {
  border-color: var(--color-border-hover);
}

.assignment-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
}

.assignment-info {
  flex: 1;
  min-width: 0;
}

.assignment-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.assignment-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.assignment-date {
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.assignment-actions {
  flex-shrink: 0;
}

.assignment-desc {
  margin: 12px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

/* === 教师操作 === */
.teacher-actions {
  margin-top: 24px;
  padding-top: 24px;
  border-top: 1px solid var(--color-border);
}

.action-form {
  max-width: 480px;
}

/* === 空状态 === */
.empty-state {
  padding: 40px 0;
}

.empty-illustration {
  font-size: 48px;
  color: var(--color-text-tertiary);
}

/* === 占位区 === */
.placeholder-section {
  min-height: 400px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.placeholder-content {
  text-align: center;
}

.placeholder-icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.placeholder-content h3 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.placeholder-content p {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin: 0;
}

/* === 动画 === */
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* === 响应式 === */
@media (max-width: 991px) {
  .course-actions {
    display: none;
  }

  .assignment-header {
    flex-direction: column;
    gap: 12px;
  }

  .assignment-actions {
    width: 100%;
  }

  .assignment-actions :deep(.ant-btn) {
    width: 100%;
  }
}
</style>
