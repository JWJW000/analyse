import { defineStore } from 'pinia'
import { ref } from 'vue'
import { projectApi, type ProjectDto, type CreateProjectRequest, type UpdateProjectRequest } from '@/api/project'

export const useProjectStore = defineStore('project', () => {
  const projects = ref<ProjectDto[]>([])
  const currentProject = ref<ProjectDto | null>(null)
  const loading = ref(false)

  async function fetchProjects() {
    loading.value = true
    try {
      projects.value = await projectApi.list()
    } finally {
      loading.value = false
    }
  }

  async function fetchProject(id: number) {
    loading.value = true
    try {
      currentProject.value = await projectApi.get(id)
    } finally {
      loading.value = false
    }
  }

  async function createProject(data: CreateProjectRequest) {
    const project = await projectApi.create(data)
    projects.value.unshift(project)
    return project
  }

  async function updateProject(id: number, data: UpdateProjectRequest) {
    const updated = await projectApi.update(id, data)
    const idx = projects.value.findIndex(p => p.id === id)
    if (idx !== -1) projects.value[idx] = updated
    if (currentProject.value?.id === id) currentProject.value = updated
    return updated
  }

  async function deleteProject(id: number) {
    await projectApi.delete(id)
    projects.value = projects.value.filter(p => p.id !== id)
    if (currentProject.value?.id === id) currentProject.value = null
  }

  async function addLiterature(projectId: number, literatureId: number) {
    const updated = await projectApi.addLiterature(projectId, literatureId)
    updateCurrentProject(updated)
    return updated
  }

  async function removeLiterature(projectId: number, literatureId: number) {
    const updated = await projectApi.removeLiterature(projectId, literatureId)
    updateCurrentProject(updated)
    return updated
  }

  async function addRequirement(projectId: number, requirementId: number) {
    const updated = await projectApi.addRequirement(projectId, requirementId)
    updateCurrentProject(updated)
    return updated
  }

  async function removeRequirement(projectId: number, requirementId: number) {
    const updated = await projectApi.removeRequirement(projectId, requirementId)
    updateCurrentProject(updated)
    return updated
  }

  async function addEthicsModule(projectId: number, ethicsModuleId: number) {
    const updated = await projectApi.addEthicsModule(projectId, ethicsModuleId)
    updateCurrentProject(updated)
    return updated
  }

  async function removeEthicsModule(projectId: number, ethicsModuleId: number) {
    const updated = await projectApi.removeEthicsModule(projectId, ethicsModuleId)
    updateCurrentProject(updated)
    return updated
  }

  function updateCurrentProject(updated: ProjectDto) {
    const idx = projects.value.findIndex(p => p.id === updated.id)
    if (idx !== -1) projects.value[idx] = updated
    if (currentProject.value?.id === updated.id) currentProject.value = updated
  }

  return {
    projects,
    currentProject,
    loading,
    fetchProjects,
    fetchProject,
    createProject,
    updateProject,
    deleteProject,
    addLiterature,
    removeLiterature,
    addRequirement,
    removeRequirement,
    addEthicsModule,
    removeEthicsModule,
    /** 用接口返回的 ProjectDto 同步列表与当前详情（如阶段推进） */
    updateCurrentProject,
  }
})