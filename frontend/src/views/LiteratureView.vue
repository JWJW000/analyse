<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http, { unwrap } from '@/api/http'
import type { ApiEnvelope } from '@/api/http'
import { message } from 'ant-design-vue'

type Lit = {
  id: number
  title: string
  author: string | null
  source: string | null
  abstractText: string | null
  keywords: string | null
  filePath: string | null
  publicationYear: number | null
  doi: string | null
  url: string | null
  literatureType: string | null
  researchMethod: string | null
  applicableTopic: string | null
  keyFindings: string | null
  evidenceValue: string | null
}

type LiteratureAnalysis = {
  title: string
  author: string | null
  abstractText: string | null
  keywords: string[]
  researchDirection: string
  summary: string
}

const list = ref<Lit[]>([])
const q = ref('')
const modalOpen = ref(false)
const editingId = ref<number | null>(null)
const emptyForm = () => ({
  title: '',
  author: '',
  source: '',
  abstractText: '',
  keywords: '',
  publicationYear: undefined as number | undefined,
  doi: '',
  url: '',
  literatureType: '',
  researchMethod: '',
  applicableTopic: '',
  keyFindings: '',
  evidenceValue: '',
})
const form = ref(emptyForm())
const detailOpen = ref(false)
const detail = ref<Lit | null>(null)
const analyzingId = ref<number | null>(null)
const analysisResult = ref<LiteratureAnalysis | null>(null)
const analysisLoading = ref(false)

async function load() {
  const res = await unwrap(http.get('/api/literature', { params: { q: q.value || undefined } }))
  list.value = res as Lit[]
}

function openCreate() {
  editingId.value = null
  form.value = emptyForm()
  modalOpen.value = true
}

function openEdit(row: Lit) {
  editingId.value = row.id
  form.value = {
    title: row.title,
    author: row.author || '',
    source: row.source || '',
    abstractText: row.abstractText || '',
    keywords: row.keywords || '',
    publicationYear: row.publicationYear || undefined,
    doi: row.doi || '',
    url: row.url || '',
    literatureType: row.literatureType || '',
    researchMethod: row.researchMethod || '',
    applicableTopic: row.applicableTopic || '',
    keyFindings: row.keyFindings || '',
    evidenceValue: row.evidenceValue || '',
  }
  modalOpen.value = true
}

async function save() {
  if (!form.value.title.trim()) {
    message.warning('请填写标题')
    return
  }
  if (editingId.value == null) {
    await unwrap(http.post('/api/literature', form.value))
    message.success('已创建')
  } else {
    await unwrap(http.put(`/api/literature/${editingId.value}`, form.value))
    message.success('已更新')
  }
  modalOpen.value = false
  await load()
}

async function showDetail(row: Lit) {
  try {
    detail.value = (await unwrap(http.get(`/api/literature/${row.id}`))) as Lit
    detailOpen.value = true
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  }
}

async function remove(row: Lit) {
  try {
    await unwrap(http.delete(`/api/literature/${row.id}`))
    message.success('已删除')
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '删除失败')
  }
}

async function onUploadFile(record: Lit, file: File) {
  try {
    const fd = new FormData()
    fd.append('file', file)
    await unwrap(http.post(`/api/literature/${record.id}/file`, fd))
    message.success('附件已上传')
    await load()
    if (detail.value?.id === record.id) {
      detail.value = (await unwrap(http.get(`/api/literature/${record.id}`))) as Lit
    }
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '上传失败')
  }
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

async function downloadAttachment(row: Lit) {
  if (!row.filePath) {
    message.warning('无附件')
    return
  }
  try {
    const res = await http.get(`/api/literature/${row.id}/file`, { responseType: 'blob' })
    const name =
      parseAttachmentFilename(res.headers['content-disposition']) ||
      `${row.title.replace(/[/\\?%*:|"<>]/g, '_')}_附件`
    const url = URL.createObjectURL(res.data)
    const a = document.createElement('a')
    a.href = url
    a.download = name
    a.click()
    URL.revokeObjectURL(url)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '下载失败')
  }
}

async function analyzeLiterature(row: Lit) {
  analyzingId.value = row.id
  analysisLoading.value = true
  analysisResult.value = null
  try {
    analysisResult.value = await unwrap<LiteratureAnalysis>(http.post<ApiEnvelope<LiteratureAnalysis>>(`/api/literature/${row.id}/analyze`))
    message.success('分析完成')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '分析失败')
  } finally {
    analysisLoading.value = false
    analyzingId.value = null
  }
}

onMounted(load)
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-space wrap>
      <a-input v-model:value="q" placeholder="标题 / 作者 / 关键词 / 主题 / DOI" style="width: 300px" @press-enter="load" />
      <a-button type="primary" @click="load">搜索</a-button>
      <a-button @click="openCreate">新建文献</a-button>
    </a-space>
    <div class="responsive-table-wrap">
      <a-table :data-source="list" :pagination="false" row-key="id" :scroll="{ x: 'max-content' }">
        <a-table-column title="标题" data-index="title" ellipsis />
        <a-table-column title="作者" data-index="author" width="120" />
        <a-table-column title="年份" data-index="publicationYear" width="90" />
        <a-table-column title="类型" data-index="literatureType" width="110" />
        <a-table-column title="适用主题" data-index="applicableTopic" ellipsis />
        <a-table-column title="关键词" data-index="keywords" ellipsis />
        <a-table-column title="附件" width="120">
          <template #default="{ record }">
            <a-space v-if="record.filePath" :size="4">
              <a-tag color="blue">有</a-tag>
              <a-button type="link" size="small" @click="downloadAttachment(record)">下载</a-button>
            </a-space>
            <span v-else>—</span>
          </template>
        </a-table-column>
        <a-table-column title="操作" width="340">
          <template #default="{ record }">
            <a-button type="link" size="small" @click="showDetail(record)">查看</a-button>
            <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
            <a-button type="link" size="small" @click="analyzeLiterature(record)" :loading="analyzingId === record.id">AI分析</a-button>
            <a-upload
              :before-upload="(f: File) => { onUploadFile(record, f); return false }"
              :show-upload-list="false"
            >
              <a-button type="link" size="small">上传附件</a-button>
            </a-upload>
            <a-popconfirm title="确定删除？" @confirm="remove(record)">
              <a-button type="text" size="small" class="delete-btn">删除</a-button>
            </a-popconfirm>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <a-modal
      v-model:open="modalOpen"
      :title="editingId == null ? '新建文献' : '编辑文献'"
      ok-text="保存"
      @ok="save"
    >
      <a-form layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" />
        </a-form-item>
        <a-form-item label="作者">
          <a-input v-model:value="form.author" />
        </a-form-item>
        <a-form-item label="来源">
          <a-input v-model:value="form.source" />
        </a-form-item>
        <a-form-item label="发表年份">
          <a-input-number v-model:value="form.publicationYear" :min="1900" :max="2100" style="width: 100%" />
        </a-form-item>
        <a-form-item label="DOI">
          <a-input v-model:value="form.doi" placeholder="如：10.1000/example" />
        </a-form-item>
        <a-form-item label="链接">
          <a-input v-model:value="form.url" placeholder="论文、标准或课程资料链接" />
        </a-form-item>
        <a-form-item label="文献类型">
          <a-input v-model:value="form.literatureType" placeholder="如：期刊论文、会议论文、教学案例、标准规范" />
        </a-form-item>
        <a-form-item label="研究方法">
          <a-input v-model:value="form.researchMethod" placeholder="如：案例研究、问卷调查、实验研究、规范分析" />
        </a-form-item>
        <a-form-item label="适用主题">
          <a-input v-model:value="form.applicableTopic" placeholder="可支撑的课程主题或需求场景" />
        </a-form-item>
        <a-form-item label="摘要">
          <a-textarea v-model:value="form.abstractText" :rows="4" />
        </a-form-item>
        <a-form-item label="核心结论">
          <a-textarea v-model:value="form.keyFindings" :rows="3" placeholder="提炼与项目需求、工程伦理或课程教学相关的结论" />
        </a-form-item>
        <a-form-item label="证据价值">
          <a-textarea v-model:value="form.evidenceValue" :rows="3" placeholder="说明该文献可支撑哪些需求、约束或验收标准" />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input v-model:value="form.keywords" />
        </a-form-item>
      </a-form>
    </a-modal>

    <a-drawer v-model:open="detailOpen" title="文献详情" width="500" @close="detail = null">
      <template v-if="detail">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="标题">{{ detail.title }}</a-descriptions-item>
          <a-descriptions-item label="作者">{{ detail.author || '—' }}</a-descriptions-item>
          <a-descriptions-item label="来源">{{ detail.source || '—' }}</a-descriptions-item>
          <a-descriptions-item label="发表年份">{{ detail.publicationYear || '—' }}</a-descriptions-item>
          <a-descriptions-item label="DOI">{{ detail.doi || '—' }}</a-descriptions-item>
          <a-descriptions-item label="链接">
            <a v-if="detail.url" :href="detail.url" target="_blank" rel="noreferrer">{{ detail.url }}</a>
            <span v-else>—</span>
          </a-descriptions-item>
          <a-descriptions-item label="文献类型">{{ detail.literatureType || '—' }}</a-descriptions-item>
          <a-descriptions-item label="研究方法">{{ detail.researchMethod || '—' }}</a-descriptions-item>
          <a-descriptions-item label="适用主题">{{ detail.applicableTopic || '—' }}</a-descriptions-item>
          <a-descriptions-item label="关键词">{{ detail.keywords || '—' }}</a-descriptions-item>
          <a-descriptions-item label="摘要">{{ detail.abstractText || '—' }}</a-descriptions-item>
          <a-descriptions-item label="核心结论">{{ detail.keyFindings || '—' }}</a-descriptions-item>
          <a-descriptions-item label="证据价值">{{ detail.evidenceValue || '—' }}</a-descriptions-item>
          <a-descriptions-item label="附件">
            <template v-if="detail.filePath">
              <a-space>
                <span class="file-ref">{{ detail.filePath }}</span>
                <a-button type="link" size="small" @click="downloadAttachment(detail)">下载</a-button>
              </a-space>
            </template>
            <span v-else>无</span>
          </a-descriptions-item>
        </a-descriptions>

        <a-divider>AI分析结果</a-divider>

        <a-spin :spinning="analysisLoading">
          <template v-if="analysisResult">
            <a-descriptions :column="1" bordered size="small">
              <a-descriptions-item label="研究领域">{{ analysisResult.researchDirection }}</a-descriptions-item>
              <a-descriptions-item label="提取关键词">
                <a-tag v-for="kw in analysisResult.keywords" :key="kw" color="blue">{{ kw }}</a-tag>
              </a-descriptions-item>
              <a-descriptions-item label="内容摘要">{{ analysisResult.summary }}</a-descriptions-item>
            </a-descriptions>
          </template>
          <a-empty v-else description="点击「AI分析」按钮生成文献分析" />
        </a-spin>

        <div style="margin-top: 16px">
          <a-button type="primary" :loading="analysisLoading" :disabled="!detail" @click="analyzeLiterature(detail!)">
            AI分析
          </a-button>
        </div>
      </template>
    </a-drawer>
  </a-space>
</template>

<style scoped>
.file-ref {
  word-break: break-all;
  font-size: 12px;
  color: var(--color-text-tertiary, #999);
}
.delete-btn {
  color: var(--color-text-secondary);
  padding: 0 4px;
  height: auto;
}
.delete-btn:hover {
  color: #ff4d4f;
}
</style>
