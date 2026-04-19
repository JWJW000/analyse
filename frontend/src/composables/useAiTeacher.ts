import { ref } from 'vue'
import http, { unwrap } from '@/api/http'

export function useAiTeacher() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function generateFeedback(
    requirementId: number,
    requirementTitle: string,
    requirementContent: string,
    studentName: string,
    courseName: string
  ): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ feedback: string }>('/api/ai/mentor/teacher/generate-feedback', {
        requirementId,
        requirementTitle,
        requirementContent,
        studentName,
        courseName
      }))
      return response.feedback
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 生成反馈失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function analyzeSubmissionQuality(
    submissionId: number,
    submissionContent: string,
    studentName: string,
    projectName: string
  ): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ analysis: string }>('/api/ai/mentor/teacher/analyze-quality', {
        submissionId,
        submissionContent,
        studentName,
        projectName
      }))
      return response.analysis
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 分析失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  async function suggestImprovements(
    requirementId: number,
    requirementTitle: string,
    requirementContent: string,
    currentFeedback: string
  ): Promise<string> {
    loading.value = true
    error.value = null
    try {
      const response = await unwrap(http.post<{ suggestions: string }>('/api/ai/mentor/student/suggest-improvements', {
        requirementId,
        requirementTitle,
        requirementContent,
        currentFeedback
      }))
      return response.suggestions
    } catch (e) {
      error.value = e instanceof Error ? e.message : 'AI 建议失败'
      throw e
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    generateFeedback,
    analyzeSubmissionQuality,
    suggestImprovements
  }
}
