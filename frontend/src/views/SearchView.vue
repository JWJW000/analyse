<script setup lang="ts">
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'

type Hit = {
  type: string
  id: number
  title: string
  snippet: string
}

const route = useRoute()
const router = useRouter()
const q = ref('')
const hits = ref<Hit[]>([])
const loading = ref(false)

async function runSearch() {
  const term = q.value.trim()
  if (!term) {
    hits.value = []
    return
  }
  loading.value = true
  try {
    const res = (await unwrap(http.get('/api/search', { params: { q: term, limit: 40 } }))) as {
      hits: Hit[]
    }
    hits.value = res.hits || []
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '搜索失败')
    hits.value = []
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.q,
  (v) => {
    q.value = typeof v === 'string' ? v : ''
    runSearch()
  },
  { immediate: true },
)

function submit() {
  const term = q.value.trim()
  if (!term) {
    message.warning('请输入关键词')
    return
  }
  router.replace({ name: 'search', query: { q: term } })
}

function labelType(t: string) {
  switch (t) {
    case 'ETHICS_MODULE':
      return '思政模块'
    case 'LITERATURE':
      return '文献'
    case 'REQUIREMENT':
      return '需求'
    default:
      return t
  }
}

function goHit(h: Hit) {
  if (h.type === 'REQUIREMENT') {
    router.push({ name: 'requirement-edit', params: { id: String(h.id) } })
    return
  }
  if (h.type === 'LITERATURE') {
    router.push({ name: 'literature', query: { q: h.title.slice(0, 32) } })
    return
  }
  if (h.type === 'ETHICS_MODULE') {
    router.push({ name: 'ethics', query: { q: h.title.slice(0, 40) } })
  }
}
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-typography-title :level="5">全局搜索</a-typography-title>
    <a-input-search
  v-model:value="q"
  placeholder="搜索思政模块、文献、需求（标题与正文）"
  enter-button="搜索"
  size="large"
  style="max-width: 560px"
  :loading="loading"
  @search="submit"
/>
    <a-typography-text v-if="route.query.q" type="secondary">关键词：{{ route.query.q }}</a-typography-text>

    <a-spin :spinning="loading">
      <a-list v-if="hits.length" :data-source="hits" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.title" :description="item.snippet">
              <template #avatar>
                <a-tag>{{ labelType(item.type) }}</a-tag>
              </template>
            </a-list-item-meta>
            <template #actions>
              <a-button type="link" size="small" @click="goHit(item)">打开</a-button>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else-if="!loading && route.query.q" description="无匹配结果" />
      <a-empty v-else-if="!loading && !route.query.q" description="输入关键词后回车搜索" />
    </a-spin>
  </a-space>
</template>
