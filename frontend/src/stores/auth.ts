import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import http, { unwrap } from '@/api/http'

export type Role = 'STUDENT' | 'TEACHER' | 'TA' | 'ADMIN'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const userId = ref<number | null>(null)
  const username = ref<string | null>(null)
  const role = ref<Role | null>(null)
  const displayName = ref<string | null>(null)

  const isLoggedIn = computed(() => !!token.value)

  function setSession(t: string, uid: number, u: string, r: Role, dn: string | null) {
    token.value = t
    userId.value = uid
    username.value = u
    role.value = r
    displayName.value = dn
    localStorage.setItem('token', t)
  }

  function clear() {
    token.value = null
    userId.value = null
    username.value = null
    role.value = null
    displayName.value = null
    localStorage.removeItem('token')
  }

  type AuthPayload = {
    token: string
    userId: number
    username: string
    role: Role
    displayName: string | null
  }

  async function login(user: string, pass: string) {
    const res = (await unwrap(
      http.post('/api/auth/login', { username: user, password: pass }),
    )) as AuthPayload
    setSession(res.token, res.userId, res.username, res.role, res.displayName)
  }

  async function register(user: string, pass: string, dn?: string) {
    const res = (await unwrap(
      http.post('/api/auth/register', { username: user, password: pass, displayName: dn }),
    )) as AuthPayload
    setSession(res.token, res.userId, res.username, res.role, res.displayName)
  }

  async function fetchMe() {
    const me = (await unwrap(http.get('/api/me'))) as {
      id: number
      username: string
      role: Role
      displayName: string | null
    }
    userId.value = me.id
    username.value = me.username
    role.value = me.role
    displayName.value = me.displayName
  }

  function logout() {
    clear()
  }

  return {
    token,
    userId,
    username,
    role,
    displayName,
    isLoggedIn,
    login,
    register,
    fetchMe,
    logout,
    setSession,
    clear,
  }
})
