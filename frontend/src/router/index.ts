import { createRouter, createWebHistory } from 'vue-router'
import { defineAsyncComponent } from 'vue'
import { useAuthStore } from '@/stores/auth'

const lazy = (path: string) => defineAsyncComponent(() => import(/* @vite-ignore */ `${path}`))

function prefetchRoute(path: string) {
  if ('requestIdleCallback' in window) {
    (window as Window & typeof globalThis & { requestIdleCallback?: (cb: () => void) => void }).requestIdleCallback(() => {
      import(/* @vite-ignore */ `${path}`)
    })
  }
}

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/', redirect: '/app' },
    {
      path: '/login',
      name: 'login',
      component: lazy('/src/views/LoginView.vue'),
    },
    {
      path: '/register',
      name: 'register',
      component: lazy('/src/views/RegisterView.vue'),
    },
    {
      path: '/app',
      component: lazy('/src/layouts/MainLayout.vue'),
      meta: { requiresAuth: true },
      children: [
        { path: '', name: 'home', component: lazy('/src/views/HomeView.vue') },
        { path: 'search', name: 'search', component: lazy('/src/views/SearchView.vue') },
        { path: 'profile', name: 'profile', component: lazy('/src/views/ProfileView.vue') },
        { path: 'literature', name: 'literature', component: lazy('/src/views/LiteratureView.vue') },
        { path: 'ethics', name: 'ethics', component: lazy('/src/views/EthicsModulesView.vue') },
        { path: 'requirements', name: 'requirements', component: lazy('/src/views/RequirementsView.vue') },
        {
          path: 'tasks/:assignmentId',
          name: 'task-workspace',
          component: lazy('/src/views/TaskWorkspaceView.vue'),
        },
        {
          path: 'requirements/:id',
          name: 'requirement-edit',
          component: lazy('/src/views/RequirementEditorView.vue'),
        },
        { path: 'projects', name: 'projects', component: lazy('/src/views/ProjectsView.vue') },
        { path: 'projects/:id', name: 'project-detail', component: lazy('/src/views/ProjectDetailView.vue') },
        { path: 'courses', name: 'courses', component: lazy('/src/views/CoursesView.vue') },
        {
          path: 'courses/:courseId/discussion',
          name: 'course-discussion',
          component: lazy('/src/views/DiscussionView.vue'),
        },
        {
          path: 'courses/:courseId/assignments/:assignmentId/review',
          name: 'assignment-review',
          component: lazy('/src/views/AssignmentReviewView.vue'),
        },
        {
          path: 'stats/course/:courseId',
          name: 'stats-course',
          component: lazy('/src/views/TeacherStatsView.vue'),
        },
        {
          path: 'stats/course/:courseId/student/:studentId',
          name: 'student-profile',
          component: lazy('/src/views/StudentProfileView.vue'),
        },
        { path: 'stats/global', name: 'stats-global', component: lazy('/src/views/AdminStatsView.vue') },
        { path: 'reports', name: 'report-generate', component: lazy('/src/views/ReportGenerateView.vue') },
        { path: 'admin/users', name: 'admin-users', component: lazy('/src/views/AdminUsersView.vue') },
        { path: 'admin/ops', name: 'admin-ops', component: lazy('/src/views/AdminOpsView.vue') },
      ],
    },
  ],
})

router.beforeEach(async (to) => {
  if (to.name === 'login') {
    prefetchRoute('/src/views/HomeView.vue')
  }
  if (to.name === 'home') {
    prefetchRoute('/src/views/RequirementsView.vue')
    prefetchRoute('/src/views/LiteratureView.vue')
    prefetchRoute('/src/views/EthicsModulesView.vue')
  }

  const auth = useAuthStore()
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (auth.isLoggedIn && !auth.role) {
    try {
      await auth.fetchMe()
    } catch {
      auth.logout()
      return { name: 'login' }
    }
  }
  const adminOnly = ['stats-global', 'admin-users', 'admin-ops']
  if (adminOnly.includes(String(to.name)) && auth.role !== 'ADMIN') {
    return { name: 'home' }
  }
  return true
})

export default router
