import { ref } from 'vue'
import { projectApi, type ProjectLiteratureDto, type ProjectRequirementDto, type ProjectEthicsModuleDto } from '@/api/project'

export function useProjectContent(projectId: number) {
  const literatures = ref<ProjectLiteratureDto[]>([])
  const requirements = ref<ProjectRequirementDto[]>([])
  const ethicsModules = ref<ProjectEthicsModuleDto[]>([])
  const loading = ref(false)
  const error = ref<string | null>(null)

  async function loadLiteratures() {
    try {
      literatures.value = await projectApi.getLiteratures(projectId)
    } catch (e) {
      console.error('Failed to load literatures:', e)
      error.value = '加载文献失败'
    }
  }

  async function loadRequirements() {
    try {
      requirements.value = await projectApi.getRequirements(projectId)
    } catch (e) {
      console.error('Failed to load requirements:', e)
      error.value = '加载需求失败'
    }
  }

  async function loadEthicsModules() {
    try {
      ethicsModules.value = await projectApi.getEthicsModules(projectId)
    } catch (e) {
      console.error('Failed to load ethics modules:', e)
      error.value = '加载思政模块失败'
    }
  }

  async function loadAll() {
    loading.value = true
    error.value = null
    try {
      await Promise.all([
        loadLiteratures(),
        loadRequirements(),
        loadEthicsModules()
      ])
    } finally {
      loading.value = false
    }
  }

  return {
    literatures,
    requirements,
    ethicsModules,
    loading,
    error,
    loadLiteratures,
    loadRequirements,
    loadEthicsModules,
    loadAll
  }
}
