<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { BookOutlined, FileTextOutlined, ReadOutlined } from '@ant-design/icons-vue'

type Mod = {
  id: number
  title: string
  category: string | null
  keywords: string | null
  description: string | null
  caseText: string | null
  reference: string | null
}

const auth = useAuthStore()
const route = useRoute()
const list = ref<Mod[]>([])
const q = ref('')
const open = ref(false)
const editing = ref<Mod | null>(null)
const form = ref({
  title: '',
  category: '',
  keywords: '',
  description: '',
  caseText: '',
  reference: '',
})

/** 列表筛选：整合展示中的三个维度 */
const dimFilter = ref<'all' | 'knowledge' | 'case' | 'norm'>('all')

const filteredList = computed(() => {
  const rows = list.value
  if (dimFilter.value === 'all') return rows
  return rows.filter((r) => {
    const hasK = !!(r.description && r.description.trim())
    const hasC = !!(r.caseText && r.caseText.trim())
    const hasR = !!(r.reference && r.reference.trim())
    if (dimFilter.value === 'knowledge') return hasK
    if (dimFilter.value === 'case') return hasC
    if (dimFilter.value === 'norm') return hasR
    return true
  })
})

const detailOpen = ref(false)
const detail = ref<Mod | null>(null)

async function load() {
  const res = await unwrap(http.get('/api/ethics-modules', { params: { q: q.value || undefined } }))
  list.value = res as Mod[]
}

function openCreate() {
  editing.value = null
  form.value = { title: '', category: '', keywords: '', description: '', caseText: '', reference: '' }
  open.value = true
}

async function openEdit(row: Mod) {
  const full = (await unwrap(http.get(`/api/ethics-modules/${row.id}`))) as Mod
  editing.value = row
  form.value = {
    title: full.title,
    category: full.category || '',
    keywords: full.keywords || '',
    description: full.description || '',
    caseText: full.caseText || '',
    reference: full.reference || '',
  }
  open.value = true
}

async function save() {
  if (editing.value) {
    await unwrap(http.put(`/api/ethics-modules/${editing.value.id}`, form.value))
  } else {
    await unwrap(http.post('/api/ethics-modules', form.value))
  }
  message.success('已保存')
  open.value = false
  await load()
}

async function remove(row: Mod) {
  await unwrap(http.delete(`/api/ethics-modules/${row.id}`))
  message.success('已删除')
  await load()
}

async function openDetail(row: Mod) {
  const full = (await unwrap(http.get(`/api/ethics-modules/${row.id}`))) as Mod
  detail.value = full
  detailOpen.value = true
}

function hasKnowledge(m: Mod) {
  return !!(m.description && m.description.trim())
}
function hasCase(m: Mod) {
  return !!(m.caseText && m.caseText.trim())
}
function hasNorm(m: Mod) {
  return !!(m.reference && m.reference.trim())
}

onMounted(async () => {
  const qq = route.query.q
  if (typeof qq === 'string' && qq) {
    q.value = qq
  }
  await load()
})
</script>

<template>
  <a-space direction="vertical" style="width: 100%" size="middle">
    <a-card class="ethics-hero" :bordered="false">
      <div class="ethics-hero-inner">
        <div>
          <h1 class="ethics-title">工程伦理思政模块库</h1>
          <p class="ethics-sub">
            整合<strong>工程伦理与思政</strong>相关的<strong>知识点</strong>、<strong>典型案例</strong>与<strong>规范/标准来源</strong>，供需求撰写与课程教学引用。每条模块可包含其中一项或多项内容。
          </p>
        </div>
        <div class="ethics-legend">
          <span class="ethics-legend-item"><FileTextOutlined /> 知识点</span>
          <span class="ethics-legend-item"><ReadOutlined /> 案例</span>
          <span class="ethics-legend-item"><BookOutlined /> 规范</span>
        </div>
      </div>
    </a-card>

    <a-space wrap>
      <a-input
        v-model:value="q"
        placeholder="搜索标题、类别、关键词或正文…"
        style="width: min(100%, 280px)"
        @press-enter="load"
      />
      <a-button type="primary" @click="load">搜索</a-button>
      <a-divider type="vertical" />
      <span class="filter-label">内容维度：</span>
      <a-radio-group v-model:value="dimFilter" button-style="solid" size="small">
        <a-radio-button value="all">全部</a-radio-button>
        <a-radio-button value="knowledge">含知识点</a-radio-button>
        <a-radio-button value="case">含案例</a-radio-button>
        <a-radio-button value="norm">含规范</a-radio-button>
      </a-radio-group>
      <a-button v-if="auth.role === 'ADMIN'" type="primary" @click="openCreate">新建模块</a-button>
    </a-space>

    <div class="responsive-table-wrap">
      <a-table
        :data-source="filteredList"
        :pagination="false"
        row-key="id"
        :scroll="{ x: 'max-content' }"
      >
        <a-table-column title="标题" data-index="title" ellipsis />
        <a-table-column title="类别" data-index="category" width="100" />
        <a-table-column title="构成" width="120">
          <template #default="{ record }">
            <a-space :size="4" wrap>
              <a-tag v-if="hasKnowledge(record)" color="blue">知识点</a-tag>
              <a-tag v-if="hasCase(record)" color="green">案例</a-tag>
              <a-tag v-if="hasNorm(record)" color="purple">规范</a-tag>
              <span v-if="!hasKnowledge(record) && !hasCase(record) && !hasNorm(record)" class="text-muted">—</span>
            </a-space>
          </template>
        </a-table-column>
        <a-table-column title="关键词" data-index="keywords" ellipsis />
        <a-table-column title="操作" :width="auth.role === 'ADMIN' ? 200 : 100" fixed="right">
          <template #default="{ record }">
            <a-button type="link" size="small" @click="openDetail(record)">查看详情</a-button>
            <template v-if="auth.role === 'ADMIN'">
              <a-button type="link" size="small" @click="openEdit(record)">编辑</a-button>
              <a-popconfirm title="确认删除？" @confirm="remove(record)">
                <a-button type="text" size="small" class="delete-btn">删除</a-button>
              </a-popconfirm>
            </template>
          </template>
        </a-table-column>
      </a-table>
    </div>

    <a-drawer v-model:open="detailOpen" :title="detail?.title || '模块详情'" width="min(92vw, 520px)" @close="detail = null">
      <template v-if="detail">
        <a-descriptions :column="1" bordered size="small">
          <a-descriptions-item label="类别">{{ detail.category || '—' }}</a-descriptions-item>
          <a-descriptions-item label="关键词">{{ detail.keywords || '—' }}</a-descriptions-item>
        </a-descriptions>
        <a-divider orientation="left">知识点</a-divider>
        <div class="ethics-block">{{ detail.description || '（未录入）' }}</div>
        <a-divider orientation="left">案例</a-divider>
        <div class="ethics-block">{{ detail.caseText || '（未录入）' }}</div>
        <a-divider orientation="left">规范 / 标准来源</a-divider>
        <div class="ethics-block ethics-block-mono">{{ detail.reference || '（未录入）' }}</div>
      </template>
    </a-drawer>

    <a-modal v-model:open="open" :title="editing ? '编辑模块' : '新建模块'" @ok="save" width="680px">
      <a-form layout="vertical">
        <a-form-item label="标题" required>
          <a-input v-model:value="form.title" placeholder="简明概括本条模块主题" />
        </a-form-item>
        <a-form-item label="类别">
          <a-input
            v-model:value="form.category"
            placeholder="如：综合、知识点、案例、规范、课程思政（可自由填写）"
          />
        </a-form-item>
        <a-form-item label="关键词">
          <a-input v-model:value="form.keywords" placeholder="便于检索，多个用顿号或逗号分隔" />
        </a-form-item>
        <a-form-item label="知识点（伦理与思政要点）">
          <a-textarea
            v-model:value="form.description"
            :rows="5"
            placeholder="核心概念、教学要点、与工程实践的关联等"
          />
        </a-form-item>
        <a-form-item label="案例（典型情境与讨论）">
          <a-textarea v-model:value="form.caseText" :rows="4" placeholder="可含情境描述、讨论问题等" />
        </a-form-item>
        <a-form-item label="规范 / 标准来源">
          <a-input
            v-model:value="form.reference"
            placeholder="如规范名称、标准编号、文件来源、链接说明等"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </a-space>
</template>

<style scoped>
.ethics-hero {
  background: linear-gradient(135deg, rgba(22, 119, 255, 0.06) 0%, #fff 48%, rgba(114, 46, 209, 0.05) 100%);
  border-radius: var(--radius-lg, 12px);
  border: 1px solid var(--color-border, #e8ecf0);
}
.ethics-hero-inner {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.ethics-title {
  margin: 0 0 8px;
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary, #1f1f1f);
}
.ethics-sub {
  margin: 0;
  max-width: 720px;
  font-size: 14px;
  line-height: 1.65;
  color: var(--color-text-secondary, #666);
}
.ethics-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: var(--color-text-tertiary, #999);
}
.ethics-legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.filter-label {
  font-size: 13px;
  color: var(--color-text-secondary, #666);
}
.text-muted {
  color: var(--color-text-tertiary, #999);
  font-size: 13px;
}
.ethics-block {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 14px;
  line-height: 1.65;
  color: rgba(0, 0, 0, 0.85);
}
.ethics-block-mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
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
