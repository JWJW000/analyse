import { ref } from 'vue'
import http, { unwrap } from '@/api/http'

export function useAiMentor() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function analyzeRequirement(projectId: number, title: string, content: string): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ analysis: string }>('/api/ai/mentor/analyze-requirement', {
        title,
        content
      }, {
        params: { projectId }
      }))
      return response.analysis
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 分析失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function generateUseCases(projectId: number, content: string): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ useCases: string }>('/api/ai/mentor/generate-use-cases', {
        content
      }, {
        params: { projectId }
      }))
      return response.useCases
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 生成用例失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function generateRequirementDraft(projectId: number, description: string): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ draft: string }>('/api/ai/mentor/generate-requirement-draft', {
        description
      }, {
        params: { projectId }
      }))
      return response.draft
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 生成需求草稿失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function generateFusion(projectId: number, requirementText: string, ethicsModuleId: number): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ fusion: string }>('/api/ai/mentor/generate-fusion', {
        requirementText,
        ethicsModuleId
      }, {
        params: { projectId }
      }))
      return response.fusion
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 生成思政融合内容失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    analyzeRequirement,
    generateUseCases,
    generateRequirementDraft,
    generateFusion
  }
}
