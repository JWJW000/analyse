import axios from 'axios'
import router from '@/router'
import { useAuthStore } from '@/stores/auth'

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '',
  timeout: 120000,
})

export function toApiError(error: unknown): Error {
  if (axios.isAxiosError(error)) {
    const msg = error.response?.data?.message
    if (typeof msg === 'string' && msg.trim().length > 0) {
      return new Error(msg)
    }
    return new Error(error.message || '请求失败')
  }
  if (error instanceof Error) {
    return error
  }
  return new Error('请求失败')
}

http.interceptors.request.use((config) => {
  const t = localStorage.getItem('token')
  if (t) {
    config.headers.Authorization = `Bearer ${t}`
  }
  return config
})

http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error)) {
      const status = error.response?.status
      if (status === 401) {
        const auth = useAuthStore()
        auth.clear()
        const currentRoute = router.currentRoute.value
        if (currentRoute.name !== 'login') {
          router.push({ name: 'login', query: { redirect: currentRoute.fullPath } })
        }
      }
    }
    return Promise.reject(toApiError(error))
  },
)

export type ApiEnvelope<T> = { ok: boolean; data: T; message?: string }

export async function unwrap<T>(p: Promise<{ data: ApiEnvelope<T> }>): Promise<T> {
  const { data } = await p
  if (!data.ok) {
    throw new Error(data.message || '请求失败')
  }
  return data.data
}

export default http
