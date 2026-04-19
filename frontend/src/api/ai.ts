import http, { unwrap } from './http'
import type { ApiEnvelope } from './http'

export interface ModuleMatchDto {
  moduleId: number
  score: number
  title: string
  snippet: string
}

export interface AiGeneratedContentDto {
  type: string
  title: string
  description: string
  priority: string
}

export interface UserStoryDto {
  asA: string
  iWant: string
  soThat: string
}

export interface UseCaseDto {
  name: string
  actor: string
  preconditions: string
  mainFlow: string
  postconditions: string
}

export interface EmbedFeedbackDto {
  items: EmbedFeedbackItemDto[]
  alternatives: ModuleMatchDto[]
  summary: string
}

export interface EmbedFeedbackItemDto {
  moduleId: number
  score: number
  weak: boolean
  title: string
  hint: string
}

export interface DiagramGenerationNode {
  id: string
  label: string
  type: string
  description?: string
}

export interface DiagramGenerationEdge {
  id: string
  source: string
  target: string
  label?: string
}

export interface DiagramGenerationDto {
  diagramType: string
  nodes: DiagramGenerationNode[]
  edges: DiagramGenerationEdge[]
  explanation: string
  recommendations: string[]
}

export const aiApi = {
  match: (requirementText: string, requirementId?: number, topK?: number) =>
    unwrap<ModuleMatchDto[]>(http.post<ApiEnvelope<ModuleMatchDto[]>>('/ai/match', { requirementText, requirementId, topK })),

  embedFeedback: (requirementId: number) =>
    unwrap<EmbedFeedbackDto>(http.post<ApiEnvelope<EmbedFeedbackDto>>('/ai/embed-feedback', { requirementId })),

  generateRequirements: (description: string) =>
    unwrap<AiGeneratedContentDto[]>(http.post<ApiEnvelope<AiGeneratedContentDto[]>>('/ai/generate-requirements', { description })),

  generateUserStories: (requirements: string) =>
    unwrap<{ stories: UserStoryDto[] }>(http.post<ApiEnvelope<{ stories: UserStoryDto[] }>>('/ai/generate-user-stories', { requirements })),

  generateUseCases: (requirements: string) =>
    unwrap<{ useCases: UseCaseDto[] }>(http.post<ApiEnvelope<{ useCases: UseCaseDto[] }>>('/ai/generate-use-cases', { requirements })),

  generateUseCaseDiagram: (text: string) =>
    unwrap<DiagramGenerationDto>(http.post<ApiEnvelope<DiagramGenerationDto>>('/api/ai/generate-use-case', { text })),

  generateFusionContent: (text: string, ethicsModuleId: number) =>
    unwrap<{ content: string }>(http.post<ApiEnvelope<{ content: string }>>('/ai/fusion-content', { text, ethicsModuleId })),

  health: () => unwrap<{ status: string }>(http.get<ApiEnvelope<{ status: string }>>('/ai/health')),
}
