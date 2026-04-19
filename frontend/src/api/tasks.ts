import http, { unwrap } from './http'

export type TaskRequirementProgress = {
  requirementId: number
  title: string
  status: string
  referenceCount: number
  ethicsCount: number
}

export type TaskWorkspace = {
  assignmentId: number
  courseId: number
  assignmentTitle: string
  assignmentDescription: string | null
  dueAt: string | null
  requirementCount: number
  referenceLinkCount: number
  ethicsLinkCount: number
  readyForSubmission: boolean
  blockingIssues: string[]
  requirements: TaskRequirementProgress[]
}

export type RequirementReferenceLink = {
  id: number
  requirementId: number
  referenceId: number
  evidenceNote: string
  confidence: number | null
  createdAt: string | null
}

export type TaskChecks = {
  readyForSubmission: boolean
  blockingIssues: string[]
}

export const tasksApi = {
  workspace: (assignmentId: number) =>
    unwrap<TaskWorkspace>(http.get(`/api/tasks/${assignmentId}/workspace`)),

  checks: (assignmentId: number) =>
    unwrap<TaskChecks>(http.get(`/api/tasks/${assignmentId}/checks`)),

  listReferenceLinks: (requirementId: number) =>
    unwrap<RequirementReferenceLink[]>(http.get(`/api/requirements/${requirementId}/reference-links`)),

  createReferenceLink: (
    requirementId: number,
    body: { referenceId: number; evidenceNote: string; confidence?: number | null },
  ) => unwrap<RequirementReferenceLink>(http.post(`/api/requirements/${requirementId}/reference-links`, body)),

  deleteReferenceLink: (linkId: number) => unwrap<void>(http.delete(`/api/reference-links/${linkId}`)),
}
