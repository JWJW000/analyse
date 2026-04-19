import http, { unwrap } from './http'
import type { ApiEnvelope } from './http'

export interface StudentProfileDto {
  studentId: number
  studentName: string
  avgScore: number
  ethicsScore: number
  commonMistakes: CommonMistakeDto[]
  abilities: Record<string, number>
  totalSubmissions: number
  submittedCount: number
}

export interface CommonMistakeDto {
  type: string
  description: string
  suggestion: string
  count: number
}

export const statsApi = {
  me: () => unwrap<any>(http.get<ApiEnvelope<any>>('/stats/me')),
  
  classStats: (courseId: number) => unwrap<any>(http.get<ApiEnvelope<any>>(`/stats/class/${courseId}`)),
  
  global: () => unwrap<any>(http.get<ApiEnvelope<any>>('/stats/global')),
  
  courseStudentProfiles: (courseId: number) => 
    unwrap<StudentProfileDto[]>(http.get<ApiEnvelope<StudentProfileDto[]>>(`/stats/course/${courseId}/students`)),
  
  studentProfile: (courseId: number, studentId: number) =>
    unwrap<StudentProfileDto>(http.get<ApiEnvelope<StudentProfileDto>>(`/stats/course/${courseId}/student/${studentId}`)),
  
  courseAnalytics: (courseId: number) =>
    unwrap<any>(http.get<ApiEnvelope<any>>(`/stats/course/${courseId}/analytics`)),
}