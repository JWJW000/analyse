import http, { unwrap } from './http'
import type { ApiEnvelope } from './http'

export interface ReportRequest {
  projectId: number
  format: 'WORD' | 'PDF'
  content: {
    includeLiterature: boolean
    includeRequirements: boolean
    includeEthicsFusion: boolean
    includeDiagrams: boolean
    includeAppendix: boolean
  }
  literatureIds: number[]
  requirementIds: number[]
  ethicsModuleIds: number[]
}

export interface ReportResponse {
  fileName: string
  downloadUrl: string
  fileSize: number
}

export const reportApi = {
  generate: (data: ReportRequest) => unwrap<ReportResponse>(http.post<ApiEnvelope<ReportResponse>>('/api/reports/generate', data)),
  download: (downloadUrl: string) => http.get(downloadUrl, { responseType: 'blob' }),
}
