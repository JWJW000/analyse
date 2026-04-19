<script setup lang="ts">
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { Input, ListItemMeta, Tag } from 'ant-design-vue'

const props = defineProps<{
  open: boolean
}>()

const emit = defineEmits<{
  (e: 'update:open', value: boolean): void
}>()

const router = useRouter()
const searchQuery = ref('')

const selectedIndex = ref(0)

const searchResults = ref([
  { type: 'requirement', title: '需求分析文档1', path: '/app/requirements/1', snippet: '软件需求规格说明书...' },
  { type: 'literature', title: '文献资料', path: '/app/literature', snippet: '相关研究文献...' },
  { type: 'ethics', title: '工程伦理思政库', path: '/app/ethics', snippet: '知识点、案例与规范…' },
])

const searchHistory = ref<string[]>([])

const filteredResults = computed(() => {
  if (!searchQuery.value) return []
  const q = searchQuery.value.toLowerCase()
  return searchResults.value.filter(r => 
    r.title.toLowerCase().includes(q) || 
    r.snippet.toLowerCase().includes(q)
  )
})

function handleSearch() {
  if (searchQuery.value && !searchHistory.value.includes(searchQuery.value)) {
    searchHistory.value.unshift(searchQuery.value)
    if (searchHistory.value.length > 10) {
      searchHistory.value.pop()
    }
  }
}

function handleSelect(item: typeof searchResults.value[0]) {
  emit('update:open', false)
  searchQuery.value = ''
  router.push(item.path)
}

function handleKeyDown(e: KeyboardEvent) {
  if (e.key === 'ArrowDown') {
    e.preventDefault()
    selectedIndex.value = Math.min(selectedIndex.value + 1, filteredResults.value.length - 1)
  } else if (e.key === 'ArrowUp') {
    e.preventDefault()
    selectedIndex.value = Math.max(selectedIndex.value - 1, 0)
  } else if (e.key === 'Enter' && filteredResults.value.length > 0) {
    const item = filteredResults.value[selectedIndex.value]
    if (item) handleSelect(item)
  }
}

function onGlobalKeydown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key === 'k') {
    e.preventDefault()
    emit('update:open', true)
  }
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeydown)
})

onBeforeUnmount(() => {
  window.removeEventListener('keydown', onGlobalKeydown)
})

watch(() => props.open, (isOpen) => {
  if (isOpen) {
    searchQuery.value = ''
    selectedIndex.value = 0
  }
})

function getTypeColor(type: string) {
  const map: Record<string, string> = {
    requirement: 'blue',
    literature: 'green',
    ethics: 'orange',
  }
  return map[type] || 'default'
}
</script>

<template>
  <a-modal
    :open="open"
    :footer="null"
    :closable="false"
    :width="560"
    centered
    @update:open="emit('update:open', $event)"
  >
    <template #title>
      <div class="search-modal-title">
        <Input
          v-model:value="searchQuery"
          placeholder="搜索需求、文献、模块..."
          size="large"
          autofocus
          @pressEnter="handleSearch"
          @keydown="handleKeyDown"
        >
          <template #prefix>
            <span>🔍</span>
          </template>
        </Input>
      </div>
    </template>
    
    <div class="search-content">
      <template v-if="!searchQuery">
        <div class="search-section">
          <div class="section-title">搜索历史</div>
          <div v-if="searchHistory.length === 0" class="empty-tip">暂无搜索历史</div>
          <div v-else class="history-list">
            <Tag
              v-for="history in searchHistory"
              :key="history"
              class="history-tag"
              @click="searchQuery = history"
            >
              {{ history }}
            </Tag>
          </div>
        </div>
      </template>
      
      <template v-else>
        <div class="search-section">
          <div class="section-title">搜索结果</div>
          <a-list
            v-if="filteredResults.length > 0"
            :data-source="filteredResults"
            size="small"
          >
            <template #renderItem="{ item, index }">
              <a-list-item 
                class="result-item"
                :class="{ selected: index === selectedIndex }"
                @click="handleSelect(item)"
              >
                <ListItemMeta :title="item.title" :description="item.snippet">
                  <template #avatar>
                    <Tag :color="getTypeColor(item.type)">{{ item.type }}</Tag>
                  </template>
                </ListItemMeta>
              </a-list-item>
            </template>
          </a-list>
          <div v-else class="empty-tip">未找到相关结果</div>
        </div>
      </template>
      
      <div class="search-tips">
        <span><kbd>↑</kbd><kbd>↓</kbd> 导航</span>
        <span><kbd>Enter</kbd> 选择</span>
        <span><kbd>Esc</kbd> 关闭</span>
      </div>
    </div>
  </a-modal>
</template>

<style scoped>
.search-modal-title :deep(.ant-input-affix-wrapper) {
  border: none;
  background: var(--color-bg-base);
}

.search-modal-title :deep(.ant-input) {
  font-size: 16px;
}

.search-content {
  max-height: 400px;
  overflow-y: auto;
}

.search-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 12px;
  color: var(--color-text-secondary);
  margin-bottom: 8px;
  font-weight: 600;
}

.empty-tip {
  color: var(--color-text-tertiary);
  font-size: 13px;
  padding: 8px 0;
}

.history-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.history-tag {
  cursor: pointer;
  padding: 4px 12px;
}

.history-tag:hover {
  opacity: 0.8;
}

.result-item {
  cursor: pointer;
  padding: 8px 12px;
  border-radius: var(--radius-md);
  transition: background var(--transition-fast);
}

.result-item:hover,
.result-item.selected {
  background: var(--color-bg-base);
}

.search-tips {
  display: flex;
  gap: 16px;
  justify-content: center;
  padding: 12px 0;
  border-top: 1px solid var(--color-border);
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.search-tips kbd {
  background: var(--color-bg-base);
  border: 1px solid var(--color-border);
  border-radius: 3px;
  padding: 2px 6px;
  font-family: inherit;
  font-size: 11px;
}
</style>
