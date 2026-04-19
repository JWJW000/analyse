<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import dayjs from 'dayjs'
import http, { unwrap } from '@/api/http'
import { tasksApi, type RequirementReferenceLink, type TaskWorkspace } from '@/api/tasks'
import { useAuthStore } from '@/stores/auth'

type RequirementDto = {
  id: number
  userId?: number
  title: string | null
  textContent: string | null
  embeddedModules: string | null
  matchingScore: number | null
  diagramJson: string | null
  specWizardJson: string | null
  courseId: number | null
  assignmentId: number | null
  status: string | null
  teacherComment: string | null
}

type LiteratureItem = {
  id: number
  title: string
  author: string | null
  source: string | null
  keywords: string | null
}

type EthicsModule = {
  id: number
  title: string
  category: string | null
  keywords: string | null
  description: string | null
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const assignmentId = computed(() => Number(route.params.assignmentId))
const workspace = ref<TaskWorkspace | null>(null)
const loading = ref(false)

const selectedRequirementId = ref<number | null>(null)
const selectedRequirement = ref<RequirementDto | null>(null)
const requirementLoading = ref(false)

const referenceLinks = ref<RequirementReferenceLink[]>([])
const referenceLoading = ref(false)
const literature = ref<LiteratureItem[]>([])
const literatureQ = ref('')
const addReferenceForm = ref({ referenceId: undefined as number | undefined, evidenceNote: '', confidence: 0.7 })
const submittingReference = ref(false)

const ethicsModules = ref<EthicsModule[]>([])
const ethicsQ = ref('')
const savingEthics = ref(false)

const createLoading = ref(false)
const submitLoading = ref(false)

const canEditCurrent = computed(() => {
  if (!selectedRequirement.value) return false
  if (auth.role === 'ADMIN') return true
  return selectedRequirement.value.userId === auth.userId
})

const embeddedIds = computed<number[]>(() => {
  const raw = selectedRequirement.value?.embeddedModules || ''
  return raw
    .split(',')
    .map((x) => Number(x.trim()))
    .filter((x) => Number.isFinite(x) && x > 0)
})

const embeddedSet = computed(() => new Set(embeddedIds.value))

const filteredEthicsModules = computed(() => {
  const q = ethicsQ.value.trim().toLowerCase()
  if (!q) return ethicsModules.value
  return ethicsModules.value.filter((m) =>
    [m.title, m.category, m.keywords, m.description]
      .filter(Boolean)
      .some((v) => String(v).toLowerCase().includes(q)),
  )
})

function dueText() {
  if (!workspace.value?.dueAt) return '无截止时间'
  return dayjs(workspace.value.dueAt).format('YYYY-MM-DD HH:mm')
}

async function loadWorkspace(pickFirst = false) {
  if (!Number.isFinite(assignmentId.value) || assignmentId.value <= 0) return
  loading.value = true
  try {
    workspace.value = await tasksApi.workspace(assignmentId.value)
    if (pickFirst && workspace.value.requirements.length) {
      selectedRequirementId.value = workspace.value.requirements[0].requirementId
    }
    if (
      selectedRequirementId.value != null &&
      !workspace.value.requirements.some((r) => r.requirementId === selectedRequirementId.value)
    ) {
      selectedRequirementId.value = workspace.value.requirements[0]?.requirementId ?? null
    }
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载任务工作台失败')
  } finally {
    loading.value = false
  }
}

async function loadRequirement(requirementId: number | null) {
  if (requirementId == null) {
    selectedRequirement.value = null
    referenceLinks.value = []
    return
  }
  requirementLoading.value = true
  try {
    selectedRequirement.value = (await unwrap(http.get(`/api/requirements/${requirementId}`))) as RequirementDto
    await loadReferenceLinks(requirementId)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载需求失败')
  } finally {
    requirementLoading.value = false
  }
}

async function loadReferenceLinks(requirementId: number) {
  referenceLoading.value = true
  try {
    referenceLinks.value = await tasksApi.listReferenceLinks(requirementId)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载文献映射失败')
  } finally {
    referenceLoading.value = false
  }
}

async function loadLiterature() {
  try {
    literature.value = (await unwrap(
      http.get('/api/literature', { params: { q: literatureQ.value || undefined } }),
    )) as LiteratureItem[]
  } catch {
    literature.value = []
  }
}

async function loadEthicsModules() {
  try {
    ethicsModules.value = (await unwrap(
      http.get('/api/ethics-modules', { params: { q: ethicsQ.value || undefined } }),
    )) as EthicsModule[]
  } catch {
    ethicsModules.value = []
  }
}

async function createRequirement() {
  if (!workspace.value) return
  createLoading.value = true
  try {
    const req = (await unwrap(
      http.post('/api/requirements', {
        title: `任务需求 ${workspace.value.requirementCount + 1}`,
        textContent: '',
        courseId: workspace.value.courseId,
        assignmentId: workspace.value.assignmentId,
      }),
    )) as RequirementDto
    message.success('已创建任务需求')
    await loadWorkspace()
    selectedRequirementId.value = req.id
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '创建需求失败')
  } finally {
    createLoading.value = false
  }
}

async function addReferenceLink() {
  if (!selectedRequirementId.value) return
  if (!addReferenceForm.value.referenceId) {
    message.warning('请选择文献')
    return
  }
  if (!addReferenceForm.value.evidenceNote.trim()) {
    message.warning('请填写证据说明')
    return
  }
  submittingReference.value = true
  try {
    await tasksApi.createReferenceLink(selectedRequirementId.value, {
      referenceId: addReferenceForm.value.referenceId,
      evidenceNote: addReferenceForm.value.evidenceNote.trim(),
      confidence: addReferenceForm.value.confidence,
    })
    addReferenceForm.value.evidenceNote = ''
    message.success('已关联文献证据')
    await loadReferenceLinks(selectedRequirementId.value)
    await loadWorkspace()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '关联失败')
  } finally {
    submittingReference.value = false
  }
}

async function removeReferenceLink(linkId: number) {
  try {
    await tasksApi.deleteReferenceLink(linkId)
    message.success('已移除映射')
    if (selectedRequirementId.value) {
      await loadReferenceLinks(selectedRequirementId.value)
    }
    await loadWorkspace()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '移除失败')
  }
}

async function persistEmbedded(ids: number[]) {
  if (!selectedRequirement.value) return
  const payload = {
    title: selectedRequirement.value.title,
    textContent: selectedRequirement.value.textContent,
    diagramJson: selectedRequirement.value.diagramJson,
    specWizardJson: selectedRequirement.value.specWizardJson,
    embeddedModules: ids.join(','),
    matchingScore: selectedRequirement.value.matchingScore,
    courseId: selectedRequirement.value.courseId,
    assignmentId: selectedRequirement.value.assignmentId,
  }
  savingEthics.value = true
  try {
    const updated = (await unwrap(
      http.put(`/api/requirements/${selectedRequirement.value.id}`, payload),
    )) as RequirementDto
    selectedRequirement.value = updated
    await loadWorkspace()
  } finally {
    savingEthics.value = false
  }
}

async function addEthicsModule(id: number) {
  if (!canEditCurrent.value) return
  if (embeddedSet.value.has(id)) return
  try {
    await persistEmbedded([...embeddedIds.value, id])
    message.success('已关联伦理模块')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '关联失败')
  }
}

async function removeEthicsModule(id: number) {
  if (!canEditCurrent.value) return
  try {
    await persistEmbedded(embeddedIds.value.filter((x) => x !== id))
    message.success('已移除伦理映射')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '移除失败')
  }
}

async function submitCurrentRequirement() {
  if (!selectedRequirementId.value) return
  submitLoading.value = true
  try {
    await unwrap(http.post(`/api/requirements/${selectedRequirementId.value}/submit`))
    message.success('已提交教师审核')
    await loadWorkspace()
    await loadRequirement(selectedRequirementId.value)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '提交失败')
  } finally {
    submitLoading.value = false
  }
}

function goEditRequirement() {
  if (!selectedRequirementId.value) return
  router.push({ name: 'requirement-edit', params: { id: String(selectedRequirementId.value) } })
}

watch(selectedRequirementId, (id) => {
  void loadRequirement(id)
})

onMounted(async () => {
  await Promise.all([loadWorkspace(true), loadLiterature(), loadEthicsModules()])
})
</script>

<template>
  <a-space direction="vertical" style="width: 100%" :size="16">
    <a-card v-if="workspace" :loading="loading" size="small">
      <a-space direction="vertical" style="width: 100%" :size="8">
        <a-space wrap align="center" style="justify-content: space-between; width: 100%">
          <div>
            <a-typography-title :level="4" style="margin: 0">{{ workspace.assignmentTitle }}</a-typography-title>
            <a-typography-text type="secondary">截止时间：{{ dueText() }}</a-typography-text>
          </div>
          <a-space>
            <a-tag :color="workspace.readyForSubmission ? 'success' : 'warning'">
              {{ workspace.readyForSubmission ? '可提交' : '未就绪' }}
            </a-tag>
            <a-button type="primary" :loading="createLoading" @click="createRequirement">新建任务需求</a-button>
          </a-space>
        </a-space>
        <a-space wrap>
          <a-tag color="blue">需求 {{ workspace.requirementCount }}</a-tag>
          <a-tag color="geekblue">文献映射 {{ workspace.referenceLinkCount }}</a-tag>
          <a-tag color="purple">伦理映射 {{ workspace.ethicsLinkCount }}</a-tag>
        </a-space>
        <a-alert
          v-if="workspace.blockingIssues.length"
          type="warning"
          show-icon
          message="提交阻塞项"
          :description="workspace.blockingIssues.join('；')"
        />
      </a-space>
    </a-card>

    <a-row :gutter="16">
      <a-col :xs="24" :lg="9">
        <a-card title="任务需求列表" size="small" :loading="loading">
          <a-table
            :data-source="workspace?.requirements || []"
            row-key="requirementId"
            :pagination="false"
            size="small"
            :custom-row="(record) => ({ onClick: () => (selectedRequirementId = record.requirementId) })"
          >
            <a-table-column title="需求" data-index="title" ellipsis />
            <a-table-column title="状态" data-index="status" width="90" />
            <a-table-column title="文献" data-index="referenceCount" width="70" />
            <a-table-column title="伦理" data-index="ethicsCount" width="70" />
          </a-table>
        </a-card>
      </a-col>

      <a-col :xs="24" :lg="15">
        <a-card :loading="requirementLoading" size="small">
          <template v-if="selectedRequirement">
            <a-space direction="vertical" style="width: 100%" :size="12">
              <a-space wrap style="justify-content: space-between; width: 100%">
                <a-typography-title :level="5" style="margin: 0">
                  {{ selectedRequirement.title || `需求 #${selectedRequirement.id}` }}
                </a-typography-title>
                <a-space>
                  <a-button @click="goEditRequirement">打开完整编辑器</a-button>
                  <a-button
                    v-if="canEditCurrent && (selectedRequirement.status === 'DRAFT' || !selectedRequirement.status)"
                    type="primary"
                    :loading="submitLoading"
                    @click="submitCurrentRequirement"
                  >
                    提交
                  </a-button>
                </a-space>
              </a-space>

              <a-tabs>
                <a-tab-pane key="references" tab="文献证据映射">
                  <a-space direction="vertical" style="width: 100%" :size="10">
                    <a-table
                      :data-source="referenceLinks"
                      row-key="id"
                      size="small"
                      :loading="referenceLoading"
                      :pagination="false"
                    >
                      <a-table-column title="文献ID" data-index="referenceId" width="90" />
                      <a-table-column title="证据说明" data-index="evidenceNote" ellipsis />
                      <a-table-column title="置信度" data-index="confidence" width="90" />
                      <a-table-column title="操作" width="80">
                        <template #default="{ record }">
                          <a-button
                            type="link"
                            size="small"
                            :disabled="!canEditCurrent"
                            @click="removeReferenceLink(record.id)"
                          >
                            移除
                          </a-button>
                        </template>
                      </a-table-column>
                    </a-table>

                    <a-divider style="margin: 8px 0" />
                    <a-space wrap>
                      <a-input
                        v-model:value="literatureQ"
                        placeholder="搜索文献"
                        style="width: 220px"
                        @press-enter="loadLiterature"
                      />
                      <a-button @click="loadLiterature">刷新文献</a-button>
                    </a-space>
                    <a-space wrap style="width: 100%">
                      <a-select
                        v-model:value="addReferenceForm.referenceId"
                        style="min-width: 260px"
                        show-search
                        :filter-option="false"
                        placeholder="选择文献"
                        :options="literature.map((l) => ({ value: l.id, label: `${l.title}（#${l.id}）` }))"
                      />
                      <a-input
                        v-model:value="addReferenceForm.evidenceNote"
                        placeholder="证据说明（该文献如何支撑需求）"
                        style="min-width: 280px"
                      />
                      <a-input-number
                        v-model:value="addReferenceForm.confidence"
                        :min="0"
                        :max="1"
                        :step="0.1"
                        style="width: 100px"
                      />
                      <a-button
                        type="primary"
                        :disabled="!canEditCurrent"
                        :loading="submittingReference"
                        @click="addReferenceLink"
                      >
                        添加映射
                      </a-button>
                    </a-space>
                  </a-space>
                </a-tab-pane>

                <a-tab-pane key="ethics" tab="伦理模块映射">
                  <a-space direction="vertical" style="width: 100%" :size="10">
                    <a-space wrap>
                      <a-tag
                        v-for="id in embeddedIds"
                        :key="id"
                        color="purple"
                        :closable="canEditCurrent"
                        @close.prevent="removeEthicsModule(id)"
                      >
                        模块 #{{ id }}
                      </a-tag>
                      <span v-if="!embeddedIds.length" class="text-secondary">暂无伦理映射</span>
                    </a-space>

                    <a-space wrap>
                      <a-input
                        v-model:value="ethicsQ"
                        placeholder="搜索伦理模块"
                        style="width: 220px"
                        @press-enter="loadEthicsModules"
                      />
                      <a-button @click="loadEthicsModules">刷新模块</a-button>
                    </a-space>

                    <a-table
                      :data-source="filteredEthicsModules"
                      row-key="id"
                      size="small"
                      :pagination="{ pageSize: 6 }"
                    >
                      <a-table-column title="模块" data-index="title" ellipsis />
                      <a-table-column title="类别" data-index="category" width="100" />
                      <a-table-column title="关键词" data-index="keywords" ellipsis />
                      <a-table-column title="操作" width="90">
                        <template #default="{ record }">
                          <a-button
                            type="link"
                            size="small"
                            :disabled="savingEthics || !canEditCurrent || embeddedSet.has(record.id)"
                            @click="addEthicsModule(record.id)"
                          >
                            {{ embeddedSet.has(record.id) ? '已关联' : '关联' }}
                          </a-button>
                        </template>
                      </a-table-column>
                    </a-table>
                  </a-space>
                </a-tab-pane>
              </a-tabs>
            </a-space>
          </template>
          <a-empty v-else description="请选择左侧需求" />
        </a-card>
      </a-col>
    </a-row>
  </a-space>
</template>

<style scoped>
.text-secondary {
  color: rgba(0, 0, 0, 0.45);
}
</style>
