import http, { unwrap } from './http'
import type { ApiEnvelope } from './http'

export interface DocumentVersionDto {
  id: number
  projectId: number
  requirementId: number
  versionNumber: number
  content: string
  changeSummary: string
  userId: number
  userName: string
  createdAt: string
}

export interface CreateVersionRequest {
  projectId: number
  requirementId: number
  content: string
  changeSummary?: string
}

export const versionApi = {
  list: (projectId: number, requirementId: number) =>
    unwrap<DocumentVersionDto[]>(http.get<ApiEnvelope<DocumentVersionDto[]>>(`/versions?projectId=${projectId}&requirementId=${requirementId}`)),
  
  create: (data: CreateVersionRequest) => unwrap<DocumentVersionDto>(http.post<ApiEnvelope<DocumentVersionDto>>('/versions', data)),
  
  get: (id: number) => unwrap<DocumentVersionDto>(http.get<ApiEnvelope<DocumentVersionDto>>(`/versions/${id}`)),
}