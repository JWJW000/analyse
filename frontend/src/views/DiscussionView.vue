<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'
import { RobotOutlined } from '@ant-design/icons-vue'

type Post = {
  id: number
  courseId: number
  authorId: number
  authorDisplayName: string
  title: string
  content: string
  visible: boolean
  createdAt: string | null
}

type AiAnswer = {
  answer: string
  confidence: number
  sources: string[]
}

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()

const courseId = computed(() => route.params.courseId as string)
const posts = ref<Post[]>([])
const loading = ref(false)
const submitting = ref(false)
const newTitle = ref('')
const newContent = ref('')
const aiAnswerLoading = ref(false)
const aiAnswerResult = ref<AiAnswer | null>(null)
const showAiAnswerModal = ref(false)

const canModerate = computed(() => auth.role === 'TEACHER' || auth.role === 'ADMIN')

async function load() {
  loading.value = true
  try {
    posts.value = (await unwrap(http.get(`/api/courses/${courseId.value}/discussion/posts`))) as Post[]
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
    posts.value = []
  } finally {
    loading.value = false
  }
}

async function submit() {
  const t = newTitle.value.trim()
  const c = newContent.value.trim()
  if (!t || !c) {
    message.warning('请填写标题与正文')
    return
  }
  submitting.value = true
  try {
    await unwrap(
      http.post(`/api/courses/${courseId.value}/discussion/posts`, {
        title: t,
        content: c,
      }),
    )
    message.success('已发布')
    newTitle.value = ''
    newContent.value = ''
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '发布失败')
  } finally {
    submitting.value = false
  }
}

async function setVisible(p: Post, visible: boolean) {
  try {
    await unwrap(
      http.patch(`/api/courses/${courseId.value}/discussion/posts/${p.id}`, { visible }),
    )
    message.success(visible ? '已显示' : '已隐藏')
    await load()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function generateAiAnswer() {
  const question = newContent.value.trim()
  if (!question) {
    message.warning('请先填写要提问的内容')
    return
  }
  aiAnswerLoading.value = true
  aiAnswerResult.value = null
  showAiAnswerModal.value = true
  try {
    aiAnswerResult.value = await unwrap<AiAnswer>(
      http.post<{ ok: boolean; data: AiAnswer }>(`/api/courses/${courseId.value}/discussion/ai-answer`, { question })
    )
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : 'AI回答生成失败')
    showAiAnswerModal.value = false
  } finally {
    aiAnswerLoading.value = false
  }
}

watch(courseId, () => load())
onMounted(load)
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-space wrap>
      <a-button type="link" @click="router.push({ name: 'courses' })">返回课程任务</a-button>
      <a-typography-text type="secondary">课程 ID {{ courseId }} · 讨论区</a-typography-text>
    </a-space>

    <a-card title="发帖" size="small">
      <a-space direction="vertical" style="width: 100%">
        <a-input v-model:value="newTitle" placeholder="标题" :maxlength="255" show-count />
        <a-textarea v-model:value="newContent" placeholder="正文（请文明讨论，勿上传违规内容）" :rows="5" :maxlength="20000" show-count />
        <a-space>
          <a-button type="primary" :loading="submitting" @click="submit">发布</a-button>
          <a-button :loading="aiAnswerLoading" @click="generateAiAnswer">
            <template #icon><RobotOutlined /></template>
            AI智能回答
          </a-button>
        </a-space>
      </a-space>
    </a-card>

    <a-typography-paragraph type="secondary">
      学生与教师均可发帖；教师/管理员可隐藏不当内容。学生端仅可见已展示的帖子。
    </a-typography-paragraph>

    <a-spin :spinning="loading">
      <a-list v-if="posts.length" :data-source="posts" item-layout="vertical">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta :title="item.title" :description="item.content">
              <template #avatar>
                <a-tag v-if="canModerate && !item.visible" color="red">已隐藏</a-tag>
              </template>
            </a-list-item-meta>
            <template #actions>
              <span>{{ item.authorDisplayName }} · {{ item.createdAt }}</span>
              <template v-if="canModerate">
                <a-button v-if="item.visible" type="link" danger size="small" @click="setVisible(item, false)">
                  隐藏
                </a-button>
                <a-button v-else type="link" size="small" @click="setVisible(item, true)">显示</a-button>
              </template>
            </template>
          </a-list-item>
        </template>
      </a-list>
      <a-empty v-else-if="!loading" description="暂无帖子" />
    </a-spin>

    <a-modal
      v-model:open="showAiAnswerModal"
      title="AI智能回答"
      :footer="null"
      width="600px"
    >
      <a-spin :spinning="aiAnswerLoading">
        <template v-if="aiAnswerResult">
          <a-typography-paragraph strong>回答内容：</a-typography-paragraph>
          <a-typography-paragraph class="ai-answer-content">
            {{ aiAnswerResult.answer }}
          </a-typography-paragraph>
          <a-divider />
          <a-typography-paragraph type="secondary">
            <div>置信度：{{ (aiAnswerResult.confidence * 100).toFixed(1) }}%</div>
            <div v-if="aiAnswerResult.sources && aiAnswerResult.sources.length">
              参考来源：{{ aiAnswerResult.sources.join('；') }}
            </div>
          </a-typography-paragraph>
        </template>
        <a-empty v-else description="正在生成回答..." />
      </a-spin>
    </a-modal>
  </a-space>
</template>

<style scoped>
.ai-answer-content {
  background: var(--color-background);
  padding: 16px;
  border-radius: 8px;
  font-size: 14px;
  line-height: 1.8;
}
</style>
