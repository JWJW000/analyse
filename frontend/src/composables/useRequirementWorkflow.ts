/** 正文最少字数（达到即视为「正文撰写」完成） */
export const MIN_BODY_CHARS = 200
/** 视为已开始写作的字数下限 */
export const MIN_START_CHARS = 80
const DEFAULT_TITLE = '未命名需求'

export type WorkflowTabKey =
  | 'text'
  | 'preview'
  | 'tpl'
  | 'diagram'
  | 'check'
  | 'ai'
  | 'spec'

export type WorkflowStepDef = {
  key: string
  title: string
  description: string
  tabKeys: WorkflowTabKey[]
  optional?: boolean
}

export const WORKFLOW_STEPS: WorkflowStepDef[] = [
  {
    key: 'pick',
    title: '选题与模板',
    description: '修改标题或套用模板，开始你的课题',
    tabKeys: ['tpl', 'text'],
  },
  {
    key: 'body',
    title: '需求正文',
    description: '撰写不少于 ' + MIN_BODY_CHARS + ' 字的需求描述',
    tabKeys: ['text', 'preview'],
  },
  {
    key: 'diagram',
    title: '用例建模',
    description: '可选：用用例图梳理角色与功能',
    tabKeys: ['diagram'],
    optional: true,
  },
  {
    key: 'ethics',
    title: '思政与质量',
    description: '匹配思政模块、运行完整性或文档分析',
    tabKeys: ['ai', 'check'],
  },
  {
    key: 'deliver',
    title: '规格与提交',
    description: '完善规格向导、导出 Word，并关联作业后提交',
    tabKeys: ['spec'],
  },
]

export type WorkflowInput = {
  title: string
  textContent: string
  diagramJson: string | null
  embeddedModules: string
  specWizard: { background?: string; goals?: string; ethics?: string }
  status: string
  courseId: number | null
  assignmentId: number | null
  /** 编辑页：是否已运行过完整性检查 */
  hasRunIntegrity: boolean
  hasRunDocAnalysis: boolean
  hasRunEmbedFeedback: boolean
  /** 可选：用户跳过用例步骤（sessionStorage 按需求 id） */
  skippedDiagram: boolean
}

function hasDiagram(json: string | null): boolean {
  if (json == null || json.trim() === '') return false
  try {
    const o = JSON.parse(json) as { cells?: unknown[] }
    return Array.isArray(o.cells) && o.cells.length > 0
  } catch {
    return json.length > 20
  }
}

function hasSpecContent(w: WorkflowInput['specWizard']): boolean {
  const b = (w.background || '').trim()
  const g = (w.goals || '').trim()
  const e = (w.ethics || '').trim()
  return b.length > 0 || g.length > 0 || e.length > 0
}

function embeddedIds(s: string): string[] {
  return s
    .split(',')
    .map((x) => x.trim())
    .filter(Boolean)
}

export function stepPickDone(w: WorkflowInput): boolean {
  const t = w.title.trim()
  if (t && t !== DEFAULT_TITLE) return true
  return w.textContent.trim().length >= MIN_START_CHARS
}

export function stepBodyDone(w: WorkflowInput): boolean {
  return w.textContent.trim().length >= MIN_BODY_CHARS
}

export function stepDiagramDone(w: WorkflowInput): boolean {
  if (w.skippedDiagram) return true
  return hasDiagram(w.diagramJson)
}

export function stepEthicsDone(w: WorkflowInput): boolean {
  if (embeddedIds(w.embeddedModules).length > 0) return true
  return w.hasRunIntegrity || w.hasRunDocAnalysis || w.hasRunEmbedFeedback
}

export function stepDeliverDone(w: WorkflowInput): boolean {
  if (w.status !== 'DRAFT' && w.status !== '') return true
  return hasSpecContent(w.specWizard)
}

/** 第几步（0-based）已完成：该步及之前都视为已达成 */
export function isStepFinished(stepIndex: number, w: WorkflowInput): boolean {
  switch (stepIndex) {
    case 0:
      return stepPickDone(w)
    case 1:
      return stepBodyDone(w)
    case 2:
      return stepDiagramDone(w)
    case 3:
      return stepEthicsDone(w)
    case 4:
      return stepDeliverDone(w)
    default:
      return false
  }
}

/** 第一个未完成的步骤索引；若全部完成则为 steps.length */
export function firstIncompleteStepIndex(w: WorkflowInput): number {
  for (let i = 0; i < WORKFLOW_STEPS.length; i++) {
    if (!isStepFinished(i, w)) return i
  }
  return WORKFLOW_STEPS.length
}

/** 无法进入「下一层」时的说明（layerIndex 0..4） */
export function advanceBlockReason(layerIndex: number, w: WorkflowInput): string {
  if (isStepFinished(layerIndex, w)) return ''
  switch (layerIndex) {
    case 0:
      return '请先完成本层：修改默认标题、或套用模板、或撰写至少 ' + MIN_START_CHARS + ' 字正文'
    case 1:
      return '请先完成本层：正文不少于 ' + MIN_BODY_CHARS + ' 字'
    case 2:
      return '请先完成本层：绘制用例图，或点击「跳过用例步骤」'
    case 3:
      return '请先完成本层：嵌入至少一个思政模块，或运行「完整性评分 / 文档分析 / 嵌入反馈」之一'
    case 4:
      return '请先完成本层：在规格向导中填写「背景 / 目标 / 伦理」至少一栏，或提交文档'
    default:
      return '请完成当前层要求后再继续'
  }
}

export function buildWorkflowInputFromEditor(p: {
  title: string
  textContent: string
  diagramJson: string | null
  embedded: string
  specWizard: { background?: string; goals?: string; ethics?: string }
  status: string
  courseId: number | null
  assignmentId: number | null
  hasRunIntegrity: boolean
  hasRunDocAnalysis: boolean
  hasRunEmbedFeedback: boolean
  skippedDiagram: boolean
}): WorkflowInput {
  return {
    title: p.title,
    textContent: p.textContent,
    diagramJson: p.diagramJson,
    embeddedModules: p.embedded,
    specWizard: p.specWizard,
    status: p.status,
    courseId: p.courseId,
    assignmentId: p.assignmentId,
    hasRunIntegrity: p.hasRunIntegrity,
    hasRunDocAnalysis: p.hasRunDocAnalysis,
    hasRunEmbedFeedback: p.hasRunEmbedFeedback,
    skippedDiagram: p.skippedDiagram,
  }
}

/** 列表行：无「是否跑过检查」信息时的进度文案 */
export function listProgressLabel(r: {
  title: string | null
  textContent: string | null
  embeddedModules: string | null
  diagramJson: string | null
  status: string
  courseId: number | null
  assignmentId: number | null
}): { label: string; color: string } {
  const st = r.status || 'DRAFT'
  if (st === 'SUBMITTED') return { label: '待审核', color: 'processing' }
  if (st === 'APPROVED') return { label: '已通过', color: 'success' }
  if (st === 'REJECTED') return { label: '已退回', color: 'error' }

  const title = (r.title || '').trim()
  const text = (r.textContent || '').trim()
  const w: WorkflowInput = {
    title: title || DEFAULT_TITLE,
    textContent: text,
    diagramJson: r.diagramJson,
    embeddedModules: r.embeddedModules || '',
    specWizard: {},
    status: 'DRAFT',
    courseId: r.courseId,
    assignmentId: r.assignmentId,
    hasRunIntegrity: false,
    hasRunDocAnalysis: false,
    hasRunEmbedFeedback: false,
    skippedDiagram: false,
  }

  if (!stepPickDone(w)) return { label: '刚开始', color: 'default' }
  if (!stepBodyDone(w)) return { label: '写作中', color: 'blue' }
  if (!stepEthicsDone(w)) return { label: '待思政/检查', color: 'orange' }
  if (r.courseId != null && r.courseId !== undefined && !r.assignmentId) {
    return { label: '待选作业', color: 'gold' }
  }
  return { label: '可提交', color: 'cyan' }
}
