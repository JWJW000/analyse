<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { listProgressLabel } from '@/composables/useRequirementWorkflow'

const auth = useAuthStore()

type Req = {
  id: number
  title: string | null
  status: string
  updatedAt: string | null
  teacherComment: string | null
  textContent?: string | null
  embeddedModules?: string | null
  diagramJson?: string | null
  courseId?: number | null
  assignmentId?: number | null
}

const router = useRouter()
const list = ref<Req[]>([])

async function load() {
  const res = await unwrap(http.get('/api/requirements/mine'))
  list.value = res as Req[]
}

async function create() {
  const r = (await unwrap(
    http.post('/api/requirements', { title: '未命名需求' }),
  )) as Req
  router.push({ name: 'requirement-edit', params: { id: String(r.id) } })
}

function edit(row: Req) {
  router.push({ name: 'requirement-edit', params: { id: String(row.id) } })
}

async function submitRow(row: Req) {
  try {
    await unwrap(http.post(`/api/requirements/${row.id}/submit`))
    message.success('已提交')
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '提交失败')
  }
}

function progressFor(row: Req) {
  return listProgressLabel({
    title: row.title,
    textContent: row.textContent ?? null,
    embeddedModules: row.embeddedModules ?? null,
    diagramJson: row.diagramJson ?? null,
    status: row.status,
    courseId: row.courseId ?? null,
    assignmentId: row.assignmentId ?? null,
  })
}

onMounted(load)
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-space wrap align="start">
      <a-button type="primary" @click="create">新建需求文档</a-button>
      <a-typography-text type="secondary">
        学生可关联课程作业后点「提交」；教师从课程页进入批改。首次写作可打开文档按顶部「写作进度」分步完成。
      </a-typography-text>
    </a-space>
    <div class="responsive-table-wrap">
      <a-table :data-source="list" :pagination="false" row-key="id" :scroll="{ x: 'max-content' }">
        <a-table-column title="标题" data-index="title" ellipsis />
        <a-table-column title="进度" width="120">
          <template #default="{ record }">
            <a-tag :color="progressFor(record).color">{{ progressFor(record).label }}</a-tag>
          </template>
        </a-table-column>
        <a-table-column title="状态" data-index="status" width="110" />
        <a-table-column title="教师批注" data-index="teacherComment" ellipsis />
        <a-table-column title="更新" data-index="updatedAt" width="180" />
        <a-table-column title="操作" width="300">
          <template #default="{ record }">
            <a-button type="link" size="small" @click="edit(record)">编辑</a-button>
            <a-button
              v-if="record.assignmentId"
              type="link"
              size="small"
              @click="router.push({ name: 'task-workspace', params: { assignmentId: String(record.assignmentId) } })"
            >
              任务工作台
            </a-button>
            <a-button
              v-if="auth.role === 'STUDENT' && record.status === 'DRAFT'"
              type="link"
              size="small"
              @click="submitRow(record)"
            >
              提交
            </a-button>
          </template>
        </a-table-column>
      </a-table>
    </div>
  </a-space>
</template>
