import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http from '@/api/http'

export interface MentorSuggestion {
  type: 'action' | 'warning' | 'tip'
  content: string
}

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  timestamp?: Date
}

export interface MentorContext {
  projectId: number
  projectName: string
  phase: string
  progress: number
  literatureCount: number
  requirementCount: number
  ethicsCount: number
}

export const useMentorStore = defineStore('mentor', () => {
  const isOpen = ref(false)
  const isLoading = ref(false)
  const suggestions = ref<MentorSuggestion[]>([])
  const chatMessages = ref<ChatMessage[]>([])
  const context = ref<MentorContext | null>(null)
  const currentPage = ref('dashboard')

  const hasSuggestions = computed(() => suggestions.value.length > 0)
  const hasChatHistory = computed(() => chatMessages.value.length > 0)

  async function loadSuggestions(projectId: number, page: string = 'dashboard') {
    isLoading.value = true
    currentPage.value = page
    try {
      const response = await http.get<MentorSuggestion[]>(`/api/ai/mentor/suggestions`, {
        params: { projectId, page }
      })
      suggestions.value = response.data.data || []
    } catch (error) {
      console.error('Failed to load suggestions:', error)
      suggestions.value = []
    } finally {
      isLoading.value = false
    }
  }

  async function sendMessage(projectId: number, message: string) {
    chatMessages.value.push({ role: 'user', content: message, timestamp: new Date() })

    isLoading.value = true
    try {
      const history = chatMessages.value.map(m => ({
        role: m.role,
        content: m.content
      }))

      const response = await http.post<{ response: string }>(`/api/ai/mentor/chat`, {
        message,
        history
      }, {
        params: { projectId }
      })

      chatMessages.value.push({
        role: 'assistant',
        content: response.data.data?.response || '抱歉，我没有理解您的问题。',
        timestamp: new Date()
      })
    } catch (error) {
      console.error('Failed to send message:', error)
      chatMessages.value.push({
        role: 'assistant',
        content: '抱歉，AI 导师暂时不可用，请稍后再试。',
        timestamp: new Date()
      })
    } finally {
      isLoading.value = false
    }
  }

  function setContext(ctx: MentorContext) {
    context.value = ctx
  }

  function toggleSidebar() {
    isOpen.value = !isOpen.value
  }

  function openSidebar() {
    isOpen.value = true
  }

  function closeSidebar() {
    isOpen.value = false
  }

  function clearChat() {
    chatMessages.value = []
  }

  return {
    isOpen,
    isLoading,
    suggestions,
    chatMessages,
    context,
    currentPage,
    hasSuggestions,
    hasChatHistory,
    loadSuggestions,
    sendMessage,
    setContext,
    toggleSidebar,
    openSidebar,
    closeSidebar,
    clearChat
  }
})
