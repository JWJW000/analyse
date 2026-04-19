import { ref } from 'vue'
import { aiApi, type AiGeneratedContentDto, type UserStoryDto, type UseCaseDto, type ModuleMatchDto } from '@/api/ai'

export function useAiAssistant() {
  const generating = ref(false)
  const suggestions = ref<AiGeneratedContentDto[]>([])
  const userStories = ref<UserStoryDto[]>([])
  const useCases = ref<UseCaseDto[]>([])
  const matchResults = ref<ModuleMatchDto[]>([])
  const fusionContent = ref('')

  async function generateRequirements(projectDescription: string) {
    generating.value = true
    suggestions.value = []
    try {
      suggestions.value = await aiApi.generateRequirements(projectDescription)
    } finally {
      generating.value = false
    }
  }

  async function generateUserStories(requirements: string) {
    generating.value = true
    userStories.value = []
    try {
      const result = await aiApi.generateUserStories(requirements)
      userStories.value = result.stories
    } finally {
      generating.value = false
    }
  }

  async function generateUseCases(requirements: string) {
    generating.value = true
    useCases.value = []
    try {
      const result = await aiApi.generateUseCases(requirements)
      useCases.value = result.useCases
    } finally {
      generating.value = false
    }
  }

  async function matchEthicsModules(requirementText: string) {
    generating.value = true
    matchResults.value = []
    try {
      matchResults.value = await aiApi.match(requirementText)
    } finally {
      generating.value = false
    }
  }

  async function generateFusionContent(requirementText: string, ethicsModuleId: number) {
    generating.value = true
    fusionContent.value = ''
    try {
      const result = await aiApi.generateFusionContent(requirementText, ethicsModuleId)
      fusionContent.value = result.content
    } finally {
      generating.value = false
    }
  }

  return {
    generating,
    suggestions,
    userStories,
    useCases,
    matchResults,
    fusionContent,
    generateRequirements,
    generateUserStories,
    generateUseCases,
    matchEthicsModules,
    generateFusionContent,
  }
}