import http, { unwrap } from './http'
import type { ApiEnvelope } from './http'

export interface ProjectDto {
  id: number
  name: string
  description: string
  courseId: number
  ownerId: number
  ownerName: string
  status: 'ACTIVE' | 'ARCHIVED' | 'COMPLETED'
  currentPhase: 'LITERATURE' | 'REQUIREMENTS' | 'ETHICS' | 'SUBMISSION' | 'REVIEW'
  createdAt: string
  updatedAt: string
  members: ProjectMemberDto[]
  literatureCount: number
  requirementCount: number
  ethicsModuleCount: number
  progress: number
}

export interface ProjectMemberDto {
  id: number
  userId: number
  userName: string
  role: 'OWNER' | 'EDITOR' | 'MEMBER' | 'VIEWER'
  joinedAt: string
}

export interface CreateProjectRequest {
  name: string
  description?: string
  courseId?: number
  memberIds?: number[]
}

export interface UpdateProjectRequest {
  name?: string
  description?: string
  /** @deprecated 阶段请使用 advance-phase / rollback-phase 接口 */
  currentPhase?: string
  status?: string
}

export interface ProjectPhaseChecklistItem {
  key: string
  label: string
  satisfied: boolean
  hint: string | null
}

export interface ProjectLiteratureDto {
  id: number
  title: string
  author: string
  keywords: string
  createdAt: string
}

export interface ProjectRequirementDto {
  id: number
  title: string
  textContent: string
  createdAt: string
}

export interface ProjectEthicsModuleDto {
  id: number
  title: string
  category: string
  keywords: string
  createdAt: string
}

export const projectApi = {
  list: () => unwrap<ProjectDto[]>(http.get<ApiEnvelope<ProjectDto[]>>('/api/projects')),
  get: (id: number) => unwrap<ProjectDto>(http.get<ApiEnvelope<ProjectDto>>(`/api/projects/${id}`)),
  create: (data: CreateProjectRequest) => unwrap<ProjectDto>(http.post<ApiEnvelope<ProjectDto>>('/api/projects', data)),
  update: (id: number, data: UpdateProjectRequest) => unwrap<ProjectDto>(http.put<ApiEnvelope<ProjectDto>>(`/api/projects/${id}`, data)),
  delete: (id: number) => unwrap(http.delete(`/api/projects/${id}`)),

  addLiterature: (projectId: number, literatureId: number) =>
    unwrap<ProjectDto>(http.post<ApiEnvelope<ProjectDto>>(`/api/projects/${projectId}/literatures/${literatureId}`)),
  removeLiterature: (projectId: number, literatureId: number) =>
    unwrap<ProjectDto>(http.delete<ApiEnvelope<ProjectDto>>(`/api/projects/${projectId}/literatures/${literatureId}`)),

  addRequirement: (projectId: number, requirementId: number) =>
    unwrap<ProjectDto>(http.post<ApiEnvelope<ProjectDto>>(`/api/projects/${projectId}/requirements/${requirementId}`)),
  removeRequirement: (projectId: number, requirementId: number) =>
    unwrap<ProjectDto>(http.delete<ApiEnvelope<ProjectDto>>(`/api/projects/${projectId}/requirements/${requirementId}`)),

  addEthicsModule: (projectId: number, ethicsModuleId: number) =>
    unwrap<ProjectDto>(http.post<ApiEnvelope<ProjectDto>>(`/api/projects/${projectId}/ethics-modules/${ethicsModuleId}`)),
  removeEthicsModule: (projectId: number, ethicsModuleId: number) =>
    unwrap<ProjectDto>(http.delete<ApiEnvelope<ProjectDto>>(`/api/projects/${projectId}/ethics-modules/${ethicsModuleId}`)),

  phaseChecklist: (id: number) =>
    unwrap<ProjectPhaseChecklistItem[]>(http.get<ApiEnvelope<ProjectPhaseChecklistItem[]>>(`/api/projects/${id}/phase-checklist`)),

  getLiteratures: (id: number) =>
    unwrap<ProjectLiteratureDto[]>(http.get<ApiEnvelope<ProjectLiteratureDto[]>>(`/api/projects/${id}/content/literatures`)),

  getRequirements: (id: number) =>
    unwrap<ProjectRequirementDto[]>(http.get<ApiEnvelope<ProjectRequirementDto[]>>(`/api/projects/${id}/content/requirements`)),

  getEthicsModules: (id: number) =>
    unwrap<ProjectEthicsModuleDto[]>(http.get<ApiEnvelope<ProjectEthicsModuleDto[]>>(`/api/projects/${id}/content/ethics-modules`)),

  advancePhase: (id: number) =>
    unwrap<ProjectDto>(http.post<ApiEnvelope<ProjectDto>>(`/api/projects/${id}/advance-phase`)),

  rollbackPhase: (id: number, reason: string) =>
    unwrap<ProjectDto>(http.post<ApiEnvelope<ProjectDto>>(`/api/projects/${id}/rollback-phase`, { reason })),
}
