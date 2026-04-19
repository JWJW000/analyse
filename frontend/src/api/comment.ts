import http, { unwrap } from './http'
import type { ApiEnvelope } from './http'

export interface CommentDto {
  id: number
  projectId: number
  requirementId: number
  userId: number
  userName: string
  content: string
  parentId: number | null
  createdAt: string
  updatedAt: string
}

export interface AddCommentRequest {
  projectId: number
  requirementId: number
  content: string
  parentId?: number
}

export const commentApi = {
  list: (projectId: number, requirementId?: number) => {
    const params = requirementId 
      ? `?projectId=${projectId}&requirementId=${requirementId}`
      : `?projectId=${projectId}`
    return unwrap<CommentDto[]>(http.get<ApiEnvelope<CommentDto[]>>(`/comments${params}`))
  },
  
  add: (data: AddCommentRequest) => unwrap<CommentDto>(http.post<ApiEnvelope<CommentDto>>('/comments', data)),
  
  delete: (id: number) => unwrap(http.delete(`/comments/${id}`)),
}