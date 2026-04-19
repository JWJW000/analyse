<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { FileTextOutlined, DownloadOutlined } from '@ant-design/icons-vue'
import { reportApi, type ReportRequest, type ReportResponse } from '@/api/report'
import { projectApi, type ProjectDto } from '@/api/project'
import http, { unwrap, type ApiEnvelope } from '@/api/http'

const generating = ref(false)
const showResult = ref(false)
const reportResult = ref<ReportResponse | null>(null)
const projects = ref<ProjectDto[]>([])
const selectedProject = ref<ProjectDto | null>(null)
const requirements = ref<RequirementOption[]>([])
const loadingOptions = ref(false)

type ReportOption = {
  id: number
  name: string
  description?: string
  kind: 'project' | 'requirements'
}

type RequirementOption = {
  id: number
  title: string
  embeddedModules?: string | null
}

const form = reactive<ReportRequest>({
  projectId: 0,
  format: 'WORD',
  content: {
    includeLiterature: true,
    includeRequirements: true,
    includeEthicsFusion: true,
    includeDiagrams: false,
    includeAppendix: false,
  },
  literatureIds: [],
  requirementIds: [],
  ethicsModuleIds: [],
})

const reportOptions = computed<ReportOption[]>(() => [
  ...projects.value.map((p) => ({
    id: p.id,
    name: p.name,
    description: p.description,
    kind: 'project' as const,
  })),
  {
    id: 0,
    name: requirements.value.length ? `我的需求报告（${requirements.value.length} 条需求）` : '我的需求报告',
    description: '不依赖项目，直接汇总当前账号下的需求文档',
    kind: 'requirements' as const,
  },
])

const isFormValid = computed(() => form.projectId >= 0 && (form.projectId > 0 || form.requirementIds.length > 0))

onMounted(async () => {
  loadingOptions.value = true
  try {
    const [projectList, requirementList] = await Promise.all([
      projectApi.list().catch(() => [] as ProjectDto[]),
      loadMyRequirements().catch(() => [] as RequirementOption[]),
    ])
    projects.value = projectList
    requirements.value = requirementList
    if (projects.value.length === 0 && requirements.value.length > 0) {
      await handleProjectChange(0)
    }
  } finally {
    loadingOptions.value = false
  }
})

async function handleProjectChange(projectId: number) {
  selectedProject.value = projects.value.find(p => p.id === projectId) || null
  form.literatureIds = []
  form.requirementIds = []
  form.ethicsModuleIds = []
  if (projectId === 0) {
    form.requirementIds = requirements.value.map((r) => r.id)
    form.ethicsModuleIds = extractEthicsIds(requirements.value.map((r) => r.embeddedModules).join(','))
    return
  }
  if (projectId > 0) {
    try {
      const [literatures, projectRequirements, ethicsModules] = await Promise.all([
        projectApi.getLiteratures(projectId).catch(() => []),
        projectApi.getRequirements(projectId).catch(() => []),
        projectApi.getEthicsModules(projectId).catch(() => []),
      ])
      form.literatureIds = literatures.map((x) => x.id)
      form.requirementIds = projectRequirements.map((x) => x.id)
      form.ethicsModuleIds = ethicsModules.map((x) => x.id)
    } catch (e: any) {
      message.warning(e?.message || '项目内容加载失败，可稍后重试')
    }
  }
}

async function handleGenerate() {
  if (!isFormValid.value) {
    message.error(projects.value.length ? '请选择项目或需求报告' : '当前账号暂无可生成报告的需求')
    return
  }

  generating.value = true
  try {
    reportResult.value = await reportApi.generate(form)
    showResult.value = true
  } catch (e: any) {
    message.error(e.message || '报告生成失败')
  } finally {
    generating.value = false
  }
}

async function handleDownload() {
  if (!reportResult.value?.downloadUrl) return
  try {
    const res = await reportApi.download(reportResult.value.downloadUrl)
    const name =
      parseAttachmentFilename(res.headers['content-disposition']) ||
      reportResult.value.fileName ||
      '报告.docx'
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '下载报告失败')
  }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

function parseAttachmentFilename(cd: string | undefined): string | null {
  if (!cd) return null
  const star = cd.match(/filename\*=UTF-8''([^;\s]+)/i)
  if (star && star[1]) {
    try {
      return decodeURIComponent(star[1])
    } catch {
      return star[1]
    }
  }
  const plain = cd.match(/filename="([^"]+)"/i)
  return plain && plain[1] ? plain[1] : null
}

async function loadMyRequirements() {
  return unwrap<RequirementOption[]>(http.get<ApiEnvelope<RequirementOption[]>>('/api/requirements/mine'))
}

function extractEthicsIds(value: string) {
  return Array.from(
    new Set(
      value
        .split(',')
        .map((x) => Number(x.trim()))
        .filter((x) => Number.isFinite(x) && x > 0),
    ),
  )
}
</script>

<template>
  <div class="report-view">
    <div class="page-header">
      <div>
        <h1 class="page-title">报告生成</h1>
        <p class="page-subtitle">一键生成包含文献综述、需求分析、思政融合的专业报告</p>
      </div>
    </div>

    <div class="report-config">
      <a-form layout="vertical">
        <a-form-item label="报告格式">
          <a-radio-group v-model:value="form.format">
            <a-radio-button value="WORD">Word 文档</a-radio-button>
            <a-radio-button value="PDF">PDF 文件</a-radio-button>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="选择项目" required>
          <a-select 
            v-model:value="form.projectId" 
            placeholder="请选择项目；没有项目时可选择我的需求报告"
            :loading="loadingOptions"
            @change="handleProjectChange"
          >
            <a-select-option v-for="p in reportOptions" :key="p.kind + '-' + p.id" :value="p.id">
              {{ p.name }}
            </a-select-option>
          </a-select>
          <div class="select-hint">
            <template v-if="projects.length === 0">
              未找到项目时，系统会使用“我的需求报告”汇总当前账号下的需求、用例图和思政映射。
            </template>
            <template v-else-if="selectedProject">
              已自动载入该项目关联内容：文献 {{ form.literatureIds.length }} 篇、需求
              {{ form.requirementIds.length }} 条、思政模块 {{ form.ethicsModuleIds.length }} 个。
            </template>
          </div>
        </a-form-item>

        <a-divider>包含内容</a-divider>

        <a-form-item>
          <a-space direction="vertical" style="width: 100%">
            <a-checkbox v-model:checked="form.content.includeLiterature">
              <span>文献综述</span>
              <span class="checkbox-hint">包含关联文献的标题、作者、摘要和关键词</span>
            </a-checkbox>
            <a-checkbox v-model:checked="form.content.includeRequirements">
              <span>需求分析</span>
              <span class="checkbox-hint">包含需求文档的完整内容和状态</span>
            </a-checkbox>
            <a-checkbox v-model:checked="form.content.includeEthicsFusion">
              <span>思政融合章节</span>
              <span class="checkbox-hint">包含思政模块的案例和融合说明</span>
            </a-checkbox>
            <a-checkbox v-model:checked="form.content.includeDiagrams">
              <span>图表/流程图</span>
              <span class="checkbox-hint">包含需求图表等可视化内容</span>
            </a-checkbox>
            <a-checkbox v-model:checked="form.content.includeAppendix">
              <span>附录</span>
              <span class="checkbox-hint">包含报告生成信息</span>
            </a-checkbox>
          </a-space>
        </a-form-item>

        <a-form-item style="margin-top: 24px">
          <a-button 
            type="primary" 
            size="large" 
            :loading="generating" 
            :disabled="!isFormValid"
            @click="handleGenerate"
          >
            <template #icon><FileTextOutlined /></template>
            生成报告
          </a-button>
        </a-form-item>
      </a-form>
    </div>

    <a-modal 
      v-model:open="showResult" 
      title="报告生成成功" 
        :footer="null"
      width="400px"
    >
      <div v-if="reportResult" class="result-content">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="文件名">{{ reportResult.fileName }}</a-descriptions-item>
          <a-descriptions-item label="文件大小">{{ formatSize(reportResult.fileSize) }}</a-descriptions-item>
        </a-descriptions>
        <div class="result-actions">
          <a-button type="primary" @click="handleDownload">
            <template #icon><DownloadOutlined /></template>
            下载报告
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<style scoped>
.report-view {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 32px;
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

.report-config {
  background: var(--color-surface, #FFFFFF);
  padding: 24px;
  border-radius: 12px;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);
}

.checkbox-hint {
  display: block;
  font-size: 12px;
  color: var(--color-text-tertiary, #9CA3AF);
  margin-left: 0;
}

.select-hint {
  margin-top: 8px;
  color: var(--color-text-secondary, #6B7280);
  font-size: 12px;
}

.result-content {
  text-align: center;
}

.result-actions {
  margin-top: 24px;
}
</style>
