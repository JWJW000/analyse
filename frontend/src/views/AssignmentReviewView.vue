<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()

const courseId = computed(() => route.params.courseId as string)
const assignmentId = computed(() => route.params.assignmentId as string)

type Row = {
  submissionId: number
  studentId: number
  studentName: string
  requirementId: number
  requirementTitle: string
  status: string
  submittedAt: string | null
  teacherComment: string | null
}

const rows = ref<Row[]>([])
const loading = ref(false)
const selectedReqIds = ref<number[]>([])
const batchComment = ref('')
const reviewComment = ref<Record<number, string>>({})

async function load() {
  loading.value = true
  try {
    const res = (await unwrap(
      http.get(`/api/courses/${courseId.value}/assignments/${assignmentId.value}/submissions`),
    )) as Row[]
    rows.value = res
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    loading.value = false
  }
}

function goEdit(reqId: number) {
  router.push({ name: 'requirement-edit', params: { id: String(reqId) } })
}

async function reviewOne(reqId: number, status: 'APPROVED' | 'REJECTED') {
  const c = (reviewComment.value[reqId] ?? '').trim() || batchComment.value.trim()
  try {
    await unwrap(
      http.post(`/api/requirements/${reqId}/review`, {
        status,
        comment: c || undefined,
      }),
    )
    message.success('已审核')
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '审核失败')
  }
}

async function batchReview(status: 'APPROVED' | 'REJECTED') {
  if (selectedReqIds.value.length === 0) {
    message.warning('请勾选要处理的需求')
    return
  }
  const commentsByRequirementId: Record<string, string> = {}
  for (const id of selectedReqIds.value) {
    const line = (reviewComment.value[id] ?? '').trim()
    if (line) commentsByRequirementId[String(id)] = line
  }
  try {
    await unwrap(
      http.post('/api/requirements/batch-review', {
        requirementIds: selectedReqIds.value,
        status,
        comment: batchComment.value || undefined,
        commentsByRequirementId:
          Object.keys(commentsByRequirementId).length > 0 ? commentsByRequirementId : undefined,
      }),
    )
    message.success('批量审核完成')
    selectedReqIds.value = []
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '批量审核失败')
  }
}

function onSelectChange(keys: number[]) {
  selectedReqIds.value = keys
}

const rowSelection = computed(() => ({
  selectedRowKeys: selectedReqIds.value,
  onChange: onSelectChange,
}))

onMounted(load)
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-space>
      <a-button type="link" @click="router.push({ name: 'courses' })">返回课程任务</a-button>
      <a-typography-text type="secondary">课程 {{ courseId }} · 作业 {{ assignmentId }}</a-typography-text>
    </a-space>

    <a-card title="作业提交与批改" size="small">
      <a-space direction="vertical" style="width: 100%">
        <a-space wrap>
          <a-input
            v-model:value="batchComment"
            placeholder="批量默认批注（可选）；表格内可填单条批注，批量时单条优先"
            style="max-width: 420px"
          />
          <a-button type="primary" :disabled="!selectedReqIds.length" @click="batchReview('APPROVED')">
            批量通过
          </a-button>
          <a-button danger :disabled="!selectedReqIds.length" @click="batchReview('REJECTED')">
            批量退回
          </a-button>
        </a-space>

        <a-table
          :data-source="rows"
          :loading="loading"
          :row-selection="rowSelection"
          :pagination="false"
          row-key="requirementId"
          size="small"
          :scroll="{ x: 'max-content' }"
        >
          <a-table-column title="学生" data-index="studentName" />
          <a-table-column title="需求标题" data-index="requirementTitle" />
          <a-table-column title="状态" data-index="status" width="100" />
          <a-table-column title="提交时间" data-index="submittedAt" width="180" />
          <a-table-column title="批注">
            <template #default="{ record }">
              <a-input
                v-model:value="reviewComment[record.requirementId]"
                size="small"
                placeholder="单条批注"
                style="min-width: 140px"
              />
            </template>
          </a-table-column>
          <a-table-column title="操作" width="280">
            <template #default="{ record }">
              <a-button type="link" size="small" @click="goEdit(record.requirementId)">打开文档</a-button>
              <a-button type="link" size="small" @click="reviewOne(record.requirementId, 'APPROVED')">通过</a-button>
              <a-button type="link" size="small" danger @click="reviewOne(record.requirementId, 'REJECTED')">退回</a-button>
            </template>
          </a-table-column>
        </a-table>
      </a-space>
    </a-card>
  </a-space>
</template>
