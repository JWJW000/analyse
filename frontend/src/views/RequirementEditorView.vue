<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { aiApi } from '@/api/ai'
import { message } from 'ant-design-vue'
const UseCaseDiagram = defineAsyncComponent(() => import('@/components/UseCaseDiagram.vue'))
import { Document, Packer, Paragraph, TextRun } from 'docx'
import { saveAs } from 'file-saver'
import { useAuthStore } from '@/stores/auth'
import { buildMinimalUseCaseGraphJson } from '@/utils/useCaseDraft'
import { diagramGenerationToX6Json } from '@/utils/useCaseDiagramGraph'
import {
  WORKFLOW_STEPS,
  type WorkflowTabKey,
  advanceBlockReason,
  buildWorkflowInputFromEditor,
  firstIncompleteStepIndex,
  isStepFinished,
} from '@/composables/useRequirementWorkflow'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const idParam = route.params.id as string

const title = ref('未命名需求')
const textContent = ref('')
const diagramJson = ref<string | null>(null)
const specWizard = ref<{ background?: string; goals?: string; ethics?: string }>({})
const embedded = ref('')
const matchLoading = ref(false)
const continueLoading = ref(false)
const matches = ref<{ moduleId: number; score: number; title: string; snippet: string }[]>([])
const activeTab = ref('text')
const status = ref('')

const courseId = ref<number | null>(null)
const assignmentId = ref<number | null>(null)
type CourseOpt = { id: number; name: string; code: string | null }
type AssignOpt = { id: number; title: string; courseId: number }
const myCourses = ref<CourseOpt[]>([])
const courseAssignments = ref<AssignOpt[]>([])

type ReqTemplate = {
  id: string
  title: string
  description: string
  scenarioTags: string[]
  starterText: string
  specBackgroundHint: string
  specGoalsHint: string
  specEthicsHint: string
}
const templates = ref<ReqTemplate[]>([])
const integrityLoading = ref(false)
const integrityResult = ref<{
  score: number
  summary: string
  items: { id: string; ok: boolean; label: string; hint: string }[]
} | null>(null)

const embedFeedbackLoading = ref(false)
type EmbedFeedbackRow = {
  moduleId: number
  score: number
  weak: boolean
  title: string
  hint: string
}
const embedFeedbackResult = ref<{
  items: EmbedFeedbackRow[]
  suggestedAlternatives: { moduleId: number; score: number; title: string; snippet: string }[]
  summary: string
} | null>(null)

const draftSpecLoading = ref(false)
const diagramLoading = ref(false)

const docAnalysisLoading = ref(false)
type DocAnalysis = {
  language: {
    primaryLanguage: string
    zhScriptRatio: number
    latinScriptRatio: number
    hints: string[]
  }
  logicalConsistency: {
    allSatisfied: boolean
    summary: string
    items: {
      id: string
      formalRule: string
      label: string
      satisfied: boolean
      hint: string
    }[]
  }
}
const docAnalysisResult = ref<DocAnalysis | null>(null)

/** 工作流：是否已运行过各类检查（用于阶段判定） */
const hasRunIntegrity = ref(false)
const hasRunDocAnalysis = ref(false)
const hasRunEmbedFeedback = ref(false)
/** 可选：跳过用例步骤（sessionStorage 按需求 id） */
const skippedDiagram = ref(false)
const guideDismissed = ref(false)
const GUIDE_LS = 'req-editor-guide-v1'

/** 学生分层模式：当前所在层 0..4，一次只展示一层 */
const currentLayer = ref(0)
const layerShowPreview = ref(false)

type ReqDto = {
  userId?: number
  title: string | null
  textContent: string | null
  diagramJson: string | null
  embeddedModules: string | null
  specWizardJson: string | null
  courseId: number | null
  assignmentId: number | null
  status: string | null
  createdAt?: string | null
  updatedAt?: string | null
}

/** 教师/管理员打开他人需求：只读 + 定时同步学生保存（P4 轻量协作） */
const requirementOwnerId = ref<number | null>(null)
const lastSyncedUpdatedAt = ref<string | null>(null)

const readOnly = computed(() => {
  if (auth.role !== 'TEACHER' && auth.role !== 'ADMIN') return false
  if (auth.userId == null || requirementOwnerId.value == null) return false
  return requirementOwnerId.value !== auth.userId
})

const showStudentWorkflow = computed(
  () => auth.role === 'STUDENT' && !readOnly.value,
)

const workflowInput = computed(() =>
  buildWorkflowInputFromEditor({
    title: title.value,
    textContent: textContent.value,
    diagramJson: diagramJson.value,
    embedded: embedded.value,
    specWizard: specWizard.value,
    status: status.value,
    courseId: courseId.value,
    assignmentId: assignmentId.value,
    hasRunIntegrity: hasRunIntegrity.value,
    hasRunDocAnalysis: hasRunDocAnalysis.value,
    hasRunEmbedFeedback: hasRunEmbedFeedback.value,
    skippedDiagram: skippedDiagram.value,
  }),
)

const firstIncompleteIdx = computed(() => firstIncompleteStepIndex(workflowInput.value))

const allWorkflowDone = computed(() => firstIncompleteIdx.value >= WORKFLOW_STEPS.length)

/** 可进入的最大层索引（未完成时不可预览更深层内容） */
const maxReachableLayer = computed(() => {
  if (allWorkflowDone.value) return 4
  return Math.min(firstIncompleteIdx.value, 4)
})

function stepItemStatus(i: number): 'wait' | 'process' | 'finish' | 'error' {
  const w = workflowInput.value
  if (allWorkflowDone.value) return 'finish'
  if (isStepFinished(i, w)) return 'finish'
  if (showStudentWorkflow.value) {
    if (i > maxReachableLayer.value) return 'wait'
    if (i === currentLayer.value) return 'process'
    return 'wait'
  }
  if (i === firstIncompleteIdx.value) return 'process'
  return 'wait'
}

function goWorkflowStepLegacy(stepIndex: number) {
  const s = WORKFLOW_STEPS[stepIndex]
  if (!s) return
  activeTab.value = s.tabKeys[0] as WorkflowTabKey
  if (stepIndex === 4) {
    setTimeout(() => {
      document.getElementById('req-course-card')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    }, 100)
  }
}

function onStepClick(stepIndex: number) {
  if (showStudentWorkflow.value) {
    goToLayer(stepIndex)
  } else {
    goWorkflowStepLegacy(stepIndex)
  }
}

function goToLayer(i: number) {
  if (i < 0 || i > maxReachableLayer.value) {
    message.warning('请先完成当前层要求，再解锁下一层')
    return
  }
  currentLayer.value = i
  if (i === 4) {
    nextTick(() => {
      document.getElementById('req-course-card')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
    })
  }
}

function goNextLayer() {
  const w = workflowInput.value
  const L = currentLayer.value
  if (!isStepFinished(L, w)) {
    message.warning(advanceBlockReason(L, w))
    return
  }
  if (L >= 4) {
    message.info('已是最后一层，请保存并在本页提交教师审核')
    return
  }
  currentLayer.value = L + 1
}

function goPrevLayer() {
  if (currentLayer.value > 0) currentLayer.value--
}

function goNextWorkflowStep() {
  if (showStudentWorkflow.value) {
    goNextLayer()
  } else {
    const next = firstIncompleteIdx.value
    if (next >= WORKFLOW_STEPS.length) {
      message.info('当前步骤已完成，可保存并提交教师审核')
      goWorkflowStepLegacy(WORKFLOW_STEPS.length - 1)
      return
    }
    goWorkflowStepLegacy(next)
  }
}

function syncLayerToProgress() {
  if (!showStudentWorkflow.value) return
  currentLayer.value = Math.min(firstIncompleteIdx.value, 4)
}

function skipDiagramStep() {
  skippedDiagram.value = true
  try {
    sessionStorage.setItem(`req-${idParam}-skip-diagram`, '1')
  } catch {
    /* ignore */
  }
  message.success('已标记跳过用例步骤，可随时回来补画')
}

function dismissGuide() {
  guideDismissed.value = true
  try {
    localStorage.setItem(GUIDE_LS, '1')
  } catch {
    /* ignore */
  }
}

function embeddedPreview(): string {
  const ids = embedded.value
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
  if (!ids.length) return ''
  return ids.slice(0, 6).join('、') + (ids.length > 6 ? '…' : '')
}

let pollTimer: ReturnType<typeof setInterval> | null = null

function stopPoll() {
  if (pollTimer != null) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

function startPoll() {
  stopPoll()
  if (!readOnly.value) return
  pollTimer = setInterval(async () => {
    if (typeof document !== 'undefined' && document.visibilityState !== 'visible') return
    try {
      const r = (await unwrap(http.get(`/api/requirements/${idParam}`))) as ReqDto
      const u = r.updatedAt
      if (u && lastSyncedUpdatedAt.value != null && u !== lastSyncedUpdatedAt.value) {
        await load()
        message.info('学生已保存，已同步最新内容')
      }
    } catch {
      /* 忽略轮询错误 */
    }
  }, 12000)
}

watch(readOnly, (v) => {
  if (v) startPoll()
  else stopPoll()
})

async function loadCourses() {
  if (auth.role !== 'STUDENT') return
  try {
    const res = (await unwrap(http.get('/api/courses/mine'))) as CourseOpt[]
    myCourses.value = res
  } catch {
    myCourses.value = []
  }
}

async function onCoursePicked(cid: number | null) {
  assignmentId.value = null
  courseAssignments.value = []
  if (cid == null) return
  try {
    const res = (await unwrap(http.get(`/api/courses/${cid}/assignments`))) as AssignOpt[]
    courseAssignments.value = res
  } catch {
    courseAssignments.value = []
  }
}

async function loadTemplates() {
  try {
    templates.value = (await unwrap(http.get('/api/meta/requirement-templates'))) as ReqTemplate[]
  } catch {
    templates.value = []
  }
}

function applyTemplate(t: ReqTemplate) {
  title.value = t.title
  textContent.value = t.starterText
  specWizard.value = {
    background: t.specBackgroundHint,
    goals: t.specGoalsHint,
    ethics: t.specEthicsHint,
  }
  message.success('已套用模板，请继续按场景润色')
}

async function runIntegrity() {
  integrityLoading.value = true
  integrityResult.value = null
  try {
    const body = {
      title: title.value,
      textContent: textContent.value,
      specWizardJson: JSON.stringify(specWizard.value),
      diagramJson: diagramJson.value,
      embeddedModules: embedded.value,
    }
    integrityResult.value = (await unwrap(http.post('/api/requirements/check-integrity', body))) as typeof integrityResult.value
    hasRunIntegrity.value = true
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '检查失败')
  } finally {
    integrityLoading.value = false
  }
}

async function runDocAnalysis() {
  docAnalysisLoading.value = true
  docAnalysisResult.value = null
  try {
    const body = {
      title: title.value,
      textContent: textContent.value,
      specWizardJson: JSON.stringify(specWizard.value),
      diagramJson: diagramJson.value,
      embeddedModules: embedded.value,
    }
    docAnalysisResult.value = (await unwrap(http.post('/api/requirements/analyze-document', body))) as DocAnalysis
    hasRunDocAnalysis.value = true
    message.success('多语言与逻辑分析完成')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '分析失败')
  } finally {
    docAnalysisLoading.value = false
  }
}

async function load() {
  const r = (await unwrap(http.get(`/api/requirements/${idParam}`))) as ReqDto
  requirementOwnerId.value = r.userId ?? null
  lastSyncedUpdatedAt.value = r.updatedAt ?? null
  title.value = r.title || ''
  textContent.value = r.textContent || ''
  diagramJson.value = r.diagramJson || null
  embedded.value = r.embeddedModules || ''
  courseId.value = r.courseId ?? null
  assignmentId.value = r.assignmentId ?? null
  status.value = r.status || ''
  if (r.specWizardJson) {
    try {
      specWizard.value = JSON.parse(r.specWizardJson)
    } catch {
      specWizard.value = {}
    }
  }
  if (auth.role === 'STUDENT' && courseId.value) {
    await onCoursePicked(courseId.value)
  }
}

async function save() {
  const body = {
    title: title.value,
    textContent: textContent.value,
    diagramJson: diagramJson.value,
    specWizardJson: JSON.stringify(specWizard.value),
    embeddedModules: embedded.value,
    matchingScore: null as number | null,
    courseId: courseId.value,
    assignmentId: assignmentId.value,
  }
  await unwrap(http.put(`/api/requirements/${idParam}`, body))
  message.success('已保存')
}

async function submitReq() {
  await save()
  try {
    await unwrap(http.post(`/api/requirements/${idParam}/submit`))
    message.success('已提交教师审核')
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '提交失败')
  }
}

async function continueRequirementText() {
  if (readOnly.value) return
  continueLoading.value = true
  try {
    const res = (await unwrap(
      http.post('/api/requirements/assist/continue-text', {
        title: title.value,
        textContent: textContent.value,
        specWizardJson: JSON.stringify(specWizard.value),
      }),
    )) as { continuedText: string; source: 'llm' | 'rule' }
    const addition = res.continuedText?.trim()
    if (!addition) {
      message.warning('暂未生成可续写内容')
      return
    }
    textContent.value = textContent.value?.trim()
      ? `${textContent.value.trim()}\n\n${addition}`
      : addition
    message.success(res.source === 'llm' ? 'AI 已续写需求正文' : '已生成续写建议')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '续写失败')
  } finally {
    continueLoading.value = false
  }
}

function goBack() {
  if (assignmentId.value != null) {
    router.push({ name: 'task-workspace', params: { assignmentId: String(assignmentId.value) } })
    return
  }
  router.push({ name: 'requirements' })
}

async function runMatch() {
  matchLoading.value = true
  try {
    const rid = Number(idParam)
    const res = await unwrap(
      http.post('/api/ai/match', {
        requirementText: textContent.value || title.value,
        requirementId: rid,
        topK: 8,
      }),
    )
    matches.value = res as typeof matches.value
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '匹配失败')
  } finally {
    matchLoading.value = false
  }
}

function pickModule(m: { moduleId: number }) {
  if (readOnly.value) return
  const cur = embedded.value ? embedded.value.split(',').filter(Boolean) : []
  if (!cur.includes(String(m.moduleId))) cur.push(String(m.moduleId))
  embedded.value = cur.join(',')
}

async function runEmbedFeedback() {
  embedFeedbackLoading.value = true
  embedFeedbackResult.value = null
  try {
    const res = (await unwrap(
      http.post('/api/ai/embed-feedback', { requirementId: Number(idParam) }),
    )) as {
      items: EmbedFeedbackRow[]
      suggestedAlternatives: { moduleId: number; score: number; title: string; snippet: string }[]
      summary: string
    }
    embedFeedbackResult.value = res
    hasRunEmbedFeedback.value = true
    message.success('嵌入反馈已生成')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '分析失败')
  } finally {
    embedFeedbackLoading.value = false
  }
}

async function applyDraftSpec() {
  draftSpecLoading.value = true
  try {
    const res = (await unwrap(
      http.post('/api/requirements/assist/draft-spec', {
        title: title.value,
        textContent: textContent.value,
      }),
    )) as { keywords: string[]; background: string; goals: string; ethics: string }
    specWizard.value = {
      background: res.background,
      goals: res.goals,
      ethics: res.ethics,
    }
    const kw = res.keywords?.length ? res.keywords.join('、') : '（无）'
    message.success('已填入规格向导初稿；提取关键词：' + kw)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '生成失败')
  } finally {
    draftSpecLoading.value = false
  }
}

async function generateDiagramDraft() {
  if (readOnly.value) return
  diagramLoading.value = true
  try {
    const sourceText = [title.value, textContent.value].filter(Boolean).join('\n')
    const diagram = await aiApi.generateUseCaseDiagram(sourceText)
    diagramJson.value = diagramGenerationToX6Json(diagram)
    if (showStudentWorkflow.value) {
      currentLayer.value = 2
    } else {
      activeTab.value = 'diagram'
    }
    const actorCount = diagram.nodes.filter((n) => n.type === 'actor').length
    const useCaseCount = diagram.nodes.length - actorCount
    message.success(`已根据正文生成用例图：${actorCount} 个参与者、${useCaseCount} 个用例`)
  } catch (e: unknown) {
    diagramJson.value = buildMinimalUseCaseGraphJson(title.value)
    if (showStudentWorkflow.value) {
      currentLayer.value = 2
    } else {
      activeTab.value = 'diagram'
    }
    message.warning(e instanceof Error ? `接口生成失败，已生成本地初稿：${e.message}` : '接口生成失败，已生成本地初稿')
  } finally {
    diagramLoading.value = false
  }
}

async function exportDocx() {
  const doc = new Document({
    sections: [
      {
        children: [
          new Paragraph({
            children: [new TextRun({ text: title.value, bold: true, size: 32 })],
          }),
          new Paragraph({ children: [new TextRun('1. 项目背景')] }),
          new Paragraph(specWizard.value.background || ''),
          new Paragraph({ children: [new TextRun('2. 目标与范围')] }),
          new Paragraph(specWizard.value.goals || ''),
          new Paragraph({ children: [new TextRun('3. 工程伦理与思政嵌入')] }),
          new Paragraph(specWizard.value.ethics || ''),
          new Paragraph({ children: [new TextRun('4. 需求正文')] }),
          new Paragraph(textContent.value || ''),
        ],
      },
    ],
  })
  const blob = await Packer.toBlob(doc)
  saveAs(blob, `${title.value || '需求规格'}.docx`)
}

watch(diagramJson, () => {
  /* sync */
})

onMounted(async () => {
  if (auth.userId == null) {
    try {
      await auth.fetchMe()
    } catch {
      /* ignore */
    }
  }
  await loadTemplates()
  await loadCourses()
  await load()
  try {
    guideDismissed.value = localStorage.getItem(GUIDE_LS) === '1'
    skippedDiagram.value = sessionStorage.getItem(`req-${idParam}-skip-diagram`) === '1'
  } catch {
    /* ignore */
  }
  await nextTick()
  syncLayerToProgress()
})

watch(firstIncompleteIdx, () => {
  if (!showStudentWorkflow.value) return
  if (currentLayer.value > maxReachableLayer.value) {
    currentLayer.value = maxReachableLayer.value
  }
})

onUnmounted(() => {
  stopPoll()
})
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-button type="link" @click="goBack">返回</a-button>
    <a-alert
      v-if="readOnly"
      type="info"
      show-icon
      message="只读：查看学生需求文档"
      :description="'最近保存时间（服务器）：' + (lastSyncedUpdatedAt || '—') + '。系统将每约 12 秒检查更新并同步正文。'"
    />
    <a-space wrap class="req-title-row">
      <a-input
        v-model:value="title"
        placeholder="标题"
        class="req-title-input"
        :disabled="readOnly"
      />
      <a-tag v-if="status">{{ status }}</a-tag>
    </a-space>

    <a-alert
      v-if="showStudentWorkflow && !guideDismissed"
      type="info"
      show-icon
      closable
      class="req-guide-alert"
      message="第一次写需求？按这五步完成"
      @close="dismissGuide"
    >
      <template #description>
        <ol class="req-guide-list">
          <li>第 1 层：改标题或套用模板</li>
          <li>第 2 层：写满正文（≥200 字）后可进入下一层</li>
          <li>第 3 层：用例图（可跳过）</li>
          <li>第 4 层：思政推荐与质量检查</li>
          <li>第 5 层：规格向导、关联课程、保存并提交</li>
        </ol>
        <a-button type="primary" size="small" @click="goNextWorkflowStep">按步骤继续（下一步）</a-button>
      </template>
    </a-alert>

    <div class="req-workflow-wrap">
      <a-typography-text type="secondary" class="req-workflow-label">写作进度</a-typography-text>
      <a-steps
        size="small"
        class="req-workflow-steps"
        :current="
          showStudentWorkflow
            ? currentLayer
            : allWorkflowDone
              ? WORKFLOW_STEPS.length - 1
              : Math.min(firstIncompleteIdx, WORKFLOW_STEPS.length - 1)
        "
      >
        <a-step
          v-for="(st, i) in WORKFLOW_STEPS"
          :key="st.key"
          :title="st.optional ? st.title + '（选做）' : st.title"
          :description="st.description"
          :status="stepItemStatus(i)"
          class="req-workflow-step"
          @click="onStepClick(i)"
        />
      </a-steps>
      <p v-if="showStudentWorkflow" class="req-workflow-hint">
        一次只显示一层内容；完成当前层要求后，点底部「进入下一层」。已完成层可随时点上方步骤回看。
      </p>
      <a-space v-else wrap class="req-workflow-actions">
        <a-button size="small" type="primary" @click="goNextWorkflowStep">跳到下一未完成步骤</a-button>
      </a-space>
    </div>

    <!-- 学生：分层推进，一次一层 -->
    <div v-if="showStudentWorkflow" class="req-layer-root">
      <div class="req-layer-panel" v-show="currentLayer === 0">
        <a-typography-title :level="5">第 1 层 · 选题与模板</a-typography-title>
        <a-typography-text type="secondary">先确定标题或从典型场景套用模板，再进入下一层写正文。</a-typography-text>
        <a-alert
          message="从典型场景起步"
          description="下列模板附带 starter 文本与规格向导提示，点击套用后再按你的课题修改。"
          type="info"
          show-icon
          style="margin: 12px 0"
        />
        <a-list :data-source="templates" row-key="id" item-layout="vertical">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta :title="item.title" :description="item.description" />
              <div>
                <a-tag v-for="tag in item.scenarioTags" :key="tag" style="margin-right: 4px">{{ tag }}</a-tag>
              </div>
              <template #actions>
                <a-button type="primary" size="small" @click="applyTemplate(item)">套用此模板</a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </div>

      <div class="req-layer-panel" v-show="currentLayer === 1">
        <a-typography-title :level="5">第 2 层 · 需求正文</a-typography-title>
        <a-space wrap align="center">
          <a-typography-text type="secondary">建议不少于 200 字，完成后可进入下一层。</a-typography-text>
          <a-button size="small" type="primary" :loading="continueLoading" @click="continueRequirementText">
            AI 一键续写
          </a-button>
        </a-space>
        <a-alert
          v-if="embeddedPreview()"
          type="success"
          show-icon
          class="req-embed-hint"
          style="margin-top: 12px"
          :message="'已选思政模块 ID：' + embeddedPreview()"
        />
        <a-textarea
          v-model:value="textContent"
          :rows="16"
          placeholder="在此编写需求描述（建议不少于 200 字）..."
          style="margin-top: 12px"
        />
        <a-space style="margin-top: 12px">
          <a-button size="small" @click="layerShowPreview = !layerShowPreview">
            {{ layerShowPreview ? '收起预览' : '阅读预览' }}
          </a-button>
        </a-space>
        <div v-show="layerShowPreview" class="preview-pane" style="margin-top: 12px">{{ textContent || '（暂无正文）' }}</div>
      </div>

      <div class="req-layer-panel" v-show="currentLayer === 2">
        <a-typography-title :level="5">第 3 层 · 用例建模（选做）</a-typography-title>
        <a-space wrap align="center" style="margin-bottom: 8px">
          <a-button size="small" type="primary" :loading="diagramLoading" @click="generateDiagramDraft">
            根据正文生成用例图
          </a-button>
          <a-button size="small" @click="skipDiagramStep">跳过本层</a-button>
        </a-space>
        <a-typography-text type="secondary">可跳过直接进入思政与质量层。</a-typography-text>
        <div style="margin-top: 12px">
          <UseCaseDiagram v-if="currentLayer === 2" v-model="diagramJson" />
        </div>
      </div>

      <div class="req-layer-panel" v-show="currentLayer === 3">
        <a-typography-title :level="5">第 4 层 · 思政与质量</a-typography-title>
        <a-divider orientation="left">AI 思政推荐</a-divider>
        <a-space direction="vertical" style="width: 100%">
          <a-button type="primary" :loading="matchLoading" @click="runMatch">匹配思政模块</a-button>
          <a-typography-text type="secondary">
            智能嵌入反馈：对已加入「嵌入列表」的模块与需求正文做语义对照；偏弱项会推荐替代模块。
          </a-typography-text>
          <a-button :loading="embedFeedbackLoading" @click="runEmbedFeedback">分析嵌入质量</a-button>
          <a-alert v-if="embedFeedbackResult" :message="embedFeedbackResult.summary" type="info" show-icon />
          <div v-if="embedFeedbackResult?.items?.length" class="responsive-table-wrap">
            <a-table
              :data-source="embedFeedbackResult.items"
              :pagination="false"
              row-key="moduleId"
              size="small"
              :scroll="{ x: 'max-content' }"
            >
              <a-table-column title="模块" data-index="title" />
              <a-table-column title="相似度" data-index="score" width="100" />
              <a-table-column title="评估" width="90">
                <template #default="{ record }">
                  <a-tag :color="record.weak ? 'warning' : 'success'">{{ record.weak ? '偏弱' : '较好' }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column title="说明" data-index="hint" />
            </a-table>
          </div>
          <div v-if="embedFeedbackResult?.suggestedAlternatives?.length" class="responsive-table-wrap">
            <a-typography-text strong>可参考的替代推荐</a-typography-text>
            <a-table
              :data-source="embedFeedbackResult.suggestedAlternatives"
              :pagination="false"
              row-key="moduleId"
              size="small"
              :scroll="{ x: 'max-content' }"
            >
              <a-table-column title="模块" data-index="title" />
              <a-table-column title="得分" data-index="score" width="100" />
              <a-table-column title="操作">
                <template #default="{ record }">
                  <a-button type="link" size="small" @click="pickModule(record)">加入嵌入</a-button>
                </template>
              </a-table-column>
            </a-table>
          </div>
          <div v-if="matches.length" class="responsive-table-wrap">
            <a-table
              :data-source="matches"
              :pagination="false"
              row-key="moduleId"
              size="small"
              :scroll="{ x: 'max-content' }"
            >
              <a-table-column title="模块" data-index="title" />
              <a-table-column title="得分" data-index="score" />
              <a-table-column title="操作">
                <template #default="{ record }">
                  <a-button type="link" size="small" @click="pickModule(record)">加入嵌入列表</a-button>
                </template>
              </a-table-column>
            </a-table>
          </div>
          <a-alert v-if="embedded" :message="'已选模块 ID: ' + embedded" type="info" />
        </a-space>
        <a-divider orientation="left">完整性检查</a-divider>
        <a-space direction="vertical" style="width: 100%">
          <a-space wrap>
            <a-button type="primary" :loading="integrityLoading" @click="runIntegrity">规则完整性评分</a-button>
            <a-button :loading="docAnalysisLoading" @click="runDocAnalysis">多语言与逻辑一致性分析</a-button>
          </a-space>
          <a-typography-text type="secondary">
            「逻辑一致性」基于可解释的蕴含规则（非自动定理证明）；主语言由中英字符占比启发式判定。
          </a-typography-text>
          <a-alert v-if="docAnalysisResult" :type="docAnalysisResult.logicalConsistency.allSatisfied ? 'success' : 'warning'" show-icon>
            <template #message>
              主语言：{{ docAnalysisResult.language.primaryLanguage }} · 中文脚本占比
              {{ (docAnalysisResult.language.zhScriptRatio * 100).toFixed(1) }}% · 拉丁字母占比
              {{ (docAnalysisResult.language.latinScriptRatio * 100).toFixed(1) }}%
            </template>
            <template #description>
              <div v-for="(h, i) in docAnalysisResult.language.hints" :key="'lh' + i">{{ h }}</div>
              <a-divider style="margin: 8px 0" />
              <strong>{{ docAnalysisResult.logicalConsistency.summary }}</strong>
            </template>
          </a-alert>
          <a-list
            v-if="docAnalysisResult?.logicalConsistency?.items?.length"
            :data-source="docAnalysisResult.logicalConsistency.items"
            size="small"
            row-key="id"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.label" :description="item.hint">
                  <template #avatar>
                    <span :style="{ color: item.satisfied ? '#52c41a' : '#fa8c16' }">{{ item.satisfied ? '✓' : '✗' }}</span>
                  </template>
                </a-list-item-meta>
                <div class="formal-rule">{{ item.formalRule }}</div>
              </a-list-item>
            </template>
          </a-list>
          <a-alert v-if="integrityResult" :message="'得分 ' + integrityResult.score + ' — ' + integrityResult.summary" type="info" />
          <a-list v-if="integrityResult" :data-source="integrityResult.items" row-key="id">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.label" :description="item.hint">
                  <template #avatar>
                    <span :style="{ color: item.ok ? '#52c41a' : '#faad14' }">{{ item.ok ? '✓' : '!' }}</span>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-space>
      </div>

      <div class="req-layer-panel" v-show="currentLayer === 4">
        <a-typography-title :level="5">第 5 层 · 规格与提交</a-typography-title>
        <a-typography-text type="secondary">完善规格说明、关联课程作业（如需），保存并提交教师审核。</a-typography-text>
        <a-space wrap style="margin: 12px 0">
          <a-button type="primary" :loading="draftSpecLoading" @click="applyDraftSpec">根据标题与正文生成向导初稿</a-button>
        </a-space>
        <a-form layout="vertical">
          <a-form-item label="项目背景">
            <a-textarea v-model:value="specWizard.background" :rows="3" />
          </a-form-item>
          <a-form-item label="目标与范围">
            <a-textarea v-model:value="specWizard.goals" :rows="3" />
          </a-form-item>
          <a-form-item label="伦理与思政嵌入说明">
            <a-textarea v-model:value="specWizard.ethics" :rows="3" />
          </a-form-item>
          <a-button type="primary" @click="exportDocx">导出 Word</a-button>
        </a-form>

        <a-card id="req-course-card" size="small" title="关联课程作业（可选）" style="margin-top: 20px">
          <a-typography-text type="secondary">
            选择后保存，即可在对应作业下提交；教师将在「课程任务 → 批改」中查看。
          </a-typography-text>
          <a-row :gutter="16" style="margin-top: 8px">
            <a-col :xs="24" :md="12">
              <a-form-item label="课程" style="margin-bottom: 0">
                <a-select
                  v-model:value="courseId"
                  allow-clear
                  placeholder="不关联则仅为个人草稿"
                  style="width: 100%"
                  :options="myCourses.map((c) => ({ label: c.name + (c.code ? ' (' + c.code + ')' : ''), value: c.id }))"
                  @change="(v: number | null) => onCoursePicked(v)"
                />
              </a-form-item>
            </a-col>
            <a-col :xs="24" :md="12">
              <a-form-item label="作业" style="margin-bottom: 0">
                <a-select
                  v-model:value="assignmentId"
                  allow-clear
                  placeholder="先选课程"
                  style="width: 100%"
                  :disabled="!courseId"
                  :options="courseAssignments.map((a) => ({ label: a.title, value: a.id }))"
                />
              </a-form-item>
            </a-col>
          </a-row>
        </a-card>
      </div>

      <div class="req-layer-nav">
        <a-button :disabled="currentLayer <= 0" @click="goPrevLayer">上一层</a-button>
        <a-space>
          <a-button type="primary" @click="save">保存</a-button>
          <a-button
            v-if="auth.role === 'STUDENT' && (status === 'DRAFT' || !status)"
            @click="submitReq"
          >
            提交教师审核
          </a-button>
          <a-button type="primary" :disabled="currentLayer >= 4" @click="goNextLayer">进入下一层</a-button>
        </a-space>
      </div>
    </div>

    <!-- 教师/管理员：保留 Tab 全貌 -->
    <template v-else>
    <a-tabs v-model:active-key="activeTab" class="req-editor-tabs" size="small">
      <a-tab-pane key="text" tab="需求正文">
        <a-space v-if="!readOnly" wrap style="margin-bottom: 10px">
          <a-button type="primary" :loading="continueLoading" @click="continueRequirementText">
            AI 一键续写
          </a-button>
        </a-space>
        <a-alert
          v-if="embeddedPreview()"
          type="success"
          show-icon
          class="req-embed-hint"
          :message="'已选思政模块 ID：' + embeddedPreview()"
        />
        <a-textarea
          v-model:value="textContent"
          :rows="16"
          placeholder="在此编写需求描述（建议不少于 200 字）..."
          :disabled="readOnly"
        />
      </a-tab-pane>
      <a-tab-pane key="preview" tab="阅读预览">
        <div class="preview-pane">{{ textContent || '（暂无正文）' }}</div>
      </a-tab-pane>
      <a-tab-pane key="tpl" tab="模板与场景">
        <a-alert
          message="从典型场景起步"
          description="下列模板附带 starter 文本与规格向导提示，点击套用后再按你的课题修改。"
          type="info"
          show-icon
          style="margin-bottom: 12px"
        />
        <a-list :data-source="templates" row-key="id" item-layout="vertical">
          <template #renderItem="{ item }">
            <a-list-item>
              <a-list-item-meta :title="item.title" :description="item.description" />
              <div>
                <a-tag v-for="tag in item.scenarioTags" :key="tag" style="margin-right: 4px">{{ tag }}</a-tag>
              </div>
              <template #actions>
                <a-button type="primary" size="small" :disabled="readOnly" @click="applyTemplate(item)">
                  套用此模板
                </a-button>
              </template>
            </a-list-item>
          </template>
        </a-list>
      </a-tab-pane>
      <a-tab-pane key="diagram" tab="用例图">
        <a-space direction="vertical" style="width: 100%">
          <a-space v-if="!readOnly" wrap align="center">
            <a-button size="small" type="primary" :loading="diagramLoading" @click="generateDiagramDraft">
              根据正文生成用例图
            </a-button>
            <a-button size="small" @click="skipDiagramStep">跳过用例步骤（可选）</a-button>
            <a-typography-text type="secondary">基于当前标题与正文生成参与者、用例和关系，将覆盖画布内容</a-typography-text>
          </a-space>
          <a-space v-else wrap align="center">
            <a-typography-text type="secondary">基于当前标题与正文生成参与者、用例和关系</a-typography-text>
          </a-space>
          <div :class="{ 'diagram-readonly': readOnly }">
            <UseCaseDiagram v-if="activeTab === 'diagram'" v-model="diagramJson" />
          </div>
        </a-space>
      </a-tab-pane>
      <a-tab-pane key="check" tab="完整性检查">
        <a-space direction="vertical" style="width: 100%">
          <a-space wrap>
            <a-button type="primary" :loading="integrityLoading" @click="runIntegrity">规则完整性评分</a-button>
            <a-button :loading="docAnalysisLoading" @click="runDocAnalysis">多语言与逻辑一致性分析</a-button>
          </a-space>
          <a-typography-text type="secondary">
            「逻辑一致性」基于可解释的蕴含规则（非自动定理证明）；主语言由中英字符占比启发式判定。
          </a-typography-text>
          <a-alert v-if="docAnalysisResult" :type="docAnalysisResult.logicalConsistency.allSatisfied ? 'success' : 'warning'" show-icon>
            <template #message>
              主语言：{{ docAnalysisResult.language.primaryLanguage }} · 中文脚本占比
              {{ (docAnalysisResult.language.zhScriptRatio * 100).toFixed(1) }}% · 拉丁字母占比
              {{ (docAnalysisResult.language.latinScriptRatio * 100).toFixed(1) }}%
            </template>
            <template #description>
              <div v-for="(h, i) in docAnalysisResult.language.hints" :key="'lh' + i">{{ h }}</div>
              <a-divider style="margin: 8px 0" />
              <strong>{{ docAnalysisResult.logicalConsistency.summary }}</strong>
            </template>
          </a-alert>
          <a-list
            v-if="docAnalysisResult?.logicalConsistency?.items?.length"
            :data-source="docAnalysisResult.logicalConsistency.items"
            size="small"
            row-key="id"
          >
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.label" :description="item.hint">
                  <template #avatar>
                    <span :style="{ color: item.satisfied ? '#52c41a' : '#fa8c16' }">{{ item.satisfied ? '✓' : '✗' }}</span>
                  </template>
                </a-list-item-meta>
                <div class="formal-rule">{{ item.formalRule }}</div>
              </a-list-item>
            </template>
          </a-list>
          <a-alert v-if="integrityResult" :message="'得分 ' + integrityResult.score + ' — ' + integrityResult.summary" type="info" />
          <a-list v-if="integrityResult" :data-source="integrityResult.items" row-key="id">
            <template #renderItem="{ item }">
              <a-list-item>
                <a-list-item-meta :title="item.label" :description="item.hint">
                  <template #avatar>
                    <span :style="{ color: item.ok ? '#52c41a' : '#faad14' }">{{ item.ok ? '✓' : '!' }}</span>
                  </template>
                </a-list-item-meta>
              </a-list-item>
            </template>
          </a-list>
        </a-space>
      </a-tab-pane>
      <a-tab-pane key="ai" tab="AI 思政推荐">
        <a-space direction="vertical" style="width: 100%">
          <a-button type="primary" :loading="matchLoading" @click="runMatch">匹配思政模块</a-button>
          <a-divider style="margin: 8px 0" />
          <a-typography-text type="secondary">
            智能嵌入反馈：对已加入「嵌入列表」的模块与需求正文做语义对照；偏弱项会推荐替代模块。
          </a-typography-text>
          <a-button :loading="embedFeedbackLoading" @click="runEmbedFeedback">分析嵌入质量</a-button>
          <a-alert v-if="embedFeedbackResult" :message="embedFeedbackResult.summary" type="info" show-icon />
          <div v-if="embedFeedbackResult?.items?.length" class="responsive-table-wrap">
            <a-table
              :data-source="embedFeedbackResult.items"
              :pagination="false"
              row-key="moduleId"
              size="small"
              :scroll="{ x: 'max-content' }"
            >
              <a-table-column title="模块" data-index="title" />
              <a-table-column title="相似度" data-index="score" width="100" />
              <a-table-column title="评估" width="90">
                <template #default="{ record }">
                  <a-tag :color="record.weak ? 'warning' : 'success'">{{ record.weak ? '偏弱' : '较好' }}</a-tag>
                </template>
              </a-table-column>
              <a-table-column title="说明" data-index="hint" />
            </a-table>
          </div>
          <div v-if="embedFeedbackResult?.suggestedAlternatives?.length" class="responsive-table-wrap">
            <a-typography-text strong>可参考的替代推荐</a-typography-text>
            <a-table
              :data-source="embedFeedbackResult.suggestedAlternatives"
              :pagination="false"
              row-key="moduleId"
              size="small"
              :scroll="{ x: 'max-content' }"
            >
              <a-table-column title="模块" data-index="title" />
              <a-table-column title="得分" data-index="score" width="100" />
              <a-table-column title="操作">
                <template #default="{ record }">
                  <a-button type="link" size="small" :disabled="readOnly" @click="pickModule(record)">
                    加入嵌入
                  </a-button>
                </template>
              </a-table-column>
            </a-table>
          </div>
          <div v-if="matches.length" class="responsive-table-wrap">
            <a-table
              :data-source="matches"
              :pagination="false"
              row-key="moduleId"
              size="small"
              :scroll="{ x: 'max-content' }"
            >
              <a-table-column title="模块" data-index="title" />
              <a-table-column title="得分" data-index="score" />
              <a-table-column title="操作">
                <template #default="{ record }">
                  <a-button type="link" size="small" :disabled="readOnly" @click="pickModule(record)">
                    加入嵌入列表
                  </a-button>
                </template>
              </a-table-column>
            </a-table>
          </div>
          <a-alert v-if="embedded" :message="'已选模块 ID: ' + embedded" type="info" />
        </a-space>
      </a-tab-pane>
      <a-tab-pane key="spec" tab="规格说明书向导">
        <a-space wrap style="margin-bottom: 12px">
          <a-button type="primary" :loading="draftSpecLoading" :disabled="readOnly" @click="applyDraftSpec">
            根据标题与正文生成向导初稿
          </a-button>
          <a-typography-text type="secondary">规则抽取关键词并填充各栏，请再人工润色</a-typography-text>
        </a-space>
        <a-form layout="vertical">
          <a-form-item label="项目背景">
            <a-textarea v-model:value="specWizard.background" :rows="3" :disabled="readOnly" />
          </a-form-item>
          <a-form-item label="目标与范围">
            <a-textarea v-model:value="specWizard.goals" :rows="3" :disabled="readOnly" />
          </a-form-item>
          <a-form-item label="伦理与思政嵌入说明">
            <a-textarea v-model:value="specWizard.ethics" :rows="3" :disabled="readOnly" />
          </a-form-item>
          <a-button type="primary" @click="exportDocx">导出 Word</a-button>
        </a-form>
      </a-tab-pane>
    </a-tabs>
    </template>

    <a-space v-if="!readOnly && !showStudentWorkflow" wrap>
      <a-button type="primary" @click="save">保存</a-button>
      <a-button
        v-if="auth.role === 'STUDENT' && (status === 'DRAFT' || !status)"
        @click="submitReq"
      >
        提交教师审核
      </a-button>
    </a-space>
  </a-space>
</template>

<style scoped>
.req-title-input {
  max-width: min(100%, 420px);
  flex: 1;
  min-width: 0;
}
.req-title-row {
  width: 100%;
}
.req-editor-tabs :deep(.ant-tabs-nav) {
  margin-bottom: 12px;
}
.req-editor-tabs :deep(.ant-tabs-nav-wrap) {
  overflow-x: auto;
}
.req-editor-tabs :deep(.ant-tabs-nav-list) {
  flex-wrap: nowrap;
}
.preview-pane {
  white-space: pre-wrap;
  word-break: break-word;
  padding: 12px 14px;
  background: #fafafa;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  min-height: min(52vh, 420px);
  font-size: 14px;
  line-height: 1.7;
  color: rgba(0, 0, 0, 0.85);
}
.diagram-readonly {
  pointer-events: none;
  opacity: 0.96;
}
.formal-rule {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  font-family: ui-monospace, monospace;
  margin-top: 4px;
}

.req-guide-alert {
  width: 100%;
}
.req-guide-list {
  margin: 0 0 12px;
  padding-left: 20px;
  line-height: 1.7;
  color: var(--color-text-secondary, rgba(0, 0, 0, 0.65));
}
.req-workflow-wrap {
  width: 100%;
  padding: 12px 14px;
  background: var(--color-background, #f4f7f9);
  border: 1px solid var(--color-border, #e8ecf0);
  border-radius: var(--radius-lg, 12px);
  overflow-x: auto;
}
.req-workflow-label {
  display: block;
  margin-bottom: 8px;
  font-size: 13px;
}
.req-workflow-steps {
  width: 100%;
}
.req-workflow-steps :deep(.ant-steps-item) {
  cursor: pointer;
}
.req-workflow-steps :deep(.ant-steps-item-description) {
  max-width: 140px;
  font-size: 11px;
  line-height: 1.35;
}
.req-workflow-step {
  min-height: auto;
}
.req-workflow-actions {
  margin-top: 12px;
}
.req-workflow-hint {
  margin: 8px 0 0;
  font-size: 13px;
  color: var(--color-text-secondary, rgba(0, 0, 0, 0.55));
  line-height: 1.5;
}
.req-embed-hint {
  margin-bottom: 10px;
}
.req-layer-root {
  width: 100%;
}
.req-layer-panel {
  min-height: 48vh;
  padding-bottom: 8px;
}
.req-layer-nav {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  position: sticky;
  bottom: 0;
  z-index: 15;
  margin-top: 20px;
  padding: 14px 16px;
  background: var(--color-surface, #fff);
  border: 1px solid var(--color-border, #e8ecf0);
  border-radius: var(--radius-lg, 12px);
  box-shadow: 0 -4px 16px rgba(15, 23, 42, 0.06);
}
@media (max-width: 991px) {
  .req-workflow-steps :deep(.ant-steps-item-description) {
    display: none;
  }
}
</style>
