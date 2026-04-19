<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  MenuOutlined,
  HomeOutlined,
  SearchOutlined,
  BookOutlined,
  AimOutlined,
  FileTextOutlined,
  ThunderboltOutlined,
  UserOutlined,
  SettingOutlined,
  BarChartOutlined,
  TeamOutlined,
  ToolOutlined,
  BulbOutlined,
  BulbFilled,
} from '@ant-design/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useTheme } from '@/composables/useTheme'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()
const { isDark, toggleTheme } = useTheme()

const mobileDrawerOpen = ref(false)
const headerSearchQ = ref('')
const isMobile = ref(false)
const windowWidth = ref(typeof window !== 'undefined' ? window.innerWidth : 1200)

const MQ = '(max-width: 1199px)'

function updateIsMobile() {
  windowWidth.value = window.innerWidth
  isMobile.value = window.matchMedia(MQ).matches
}

onMounted(() => {
  updateIsMobile()
  window.addEventListener('resize', updateIsMobile)
})

onUnmounted(() => {
  window.removeEventListener('resize', updateIsMobile)
})

watch(isMobile, (m) => {
  if (!m) mobileDrawerOpen.value = false
})

const selected = computed(() => {
  const n = route.name as string
  if (n === 'requirement-edit') return auth.role === 'STUDENT' ? ['courses'] : ['requirements']
  if (n === 'assignment-review') return ['courses']
  if (n === 'task-workspace') return ['courses']
  if (n === 'course-discussion') return ['courses']
  if (n === 'stats-course') return ['courses']
  if (n === 'search') return ['search']
  if (n === 'projects' || n === 'project-detail') return ['projects']
  return [n || 'home']
})

const pageTitle = computed(() => {
  const titles: Record<string, string> = {
    home: '首页',
    search: '全局搜索',
    projects: '项目工作台',
    'project-detail': '项目详情',
    literature: '文献调研',
    ethics: '工程伦理思政库',
    requirements: '需求分析',
    'requirement-edit': '需求编辑',
    courses: '课程任务',
    'course-discussion': '课程讨论',
    'assignment-review': '作业批改',
    'task-workspace': '任务工作台',
    'stats-course': '班级统计',
    'stats-global': '全站统计',
    'student-profile': '学生画像',
    'report-generate': '报告生成',
    'admin-users': '用户管理',
    'admin-ops': '系统运维',
    profile: '个人中心',
  }
  return titles[String(route.name)] || ''
})

// 菜单配置
const menuItems = computed(() => {
  const items = [
    { key: 'home', title: '首页', icon: HomeOutlined },
    { key: 'courses', title: '课程任务', icon: ThunderboltOutlined },
  ]

  if (auth.role === 'STUDENT') {
    items.push(
      { key: 'requirements', title: '我的需求', icon: FileTextOutlined },
      { key: 'literature', title: '文献证据', icon: BookOutlined },
      { key: 'ethics', title: '伦理素材', icon: AimOutlined },
    )
  }
  
  if (auth.role === 'TEACHER' || auth.role === 'ADMIN') {
    items.push(
      { key: 'search', title: '全局搜索', icon: SearchOutlined },
      { key: 'requirements', title: '需求分析维护', icon: FileTextOutlined },
      { key: 'literature', title: '文献资源库', icon: BookOutlined },
      { key: 'ethics', title: '伦理思政库', icon: AimOutlined },
      { key: 'report-generate', title: '报告生成', icon: FileTextOutlined },
    )
  }
  
  if (auth.role === 'ADMIN') {
    items.push(
      { key: 'stats-global', title: '全站统计', icon: BarChartOutlined },
      { key: 'admin-users', title: '用户管理', icon: TeamOutlined },
      { key: 'admin-ops', title: '系统运维', icon: ToolOutlined },
      { key: 'profile', title: '个人中心', icon: UserOutlined },
    )
  } else if (auth.role === 'TEACHER') {
    items.push({ key: 'profile', title: '个人中心', icon: UserOutlined })
  } else {
    items.push({ key: 'profile', title: '个人中心', icon: UserOutlined })
  }
  
  return items
})

function go(name: string) {
  mobileDrawerOpen.value = false
  router.push({ name })
}

function logout() {
  mobileDrawerOpen.value = false
  auth.logout()
  router.push({ name: 'login' })
}

function onHeaderSearch() {
  const term = headerSearchQ.value.trim()
  if (!term) return
  router.push({ name: 'search', query: { q: term } })
  headerSearchQ.value = ''
}
</script>

<template>
  <a-layout class="app-shell">
    <!-- 桌面端侧栏 -->
    <a-layout-sider 
      v-if="!isMobile" 
      class="app-sider" 
      :width="240"
      :collapsed-width="72"
      breakpoint="lg"
      collapsible
      :default-collapsed="false"
    >
      <div class="sider-header">
        <div class="sider-logo">
          <svg viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="32" height="32" rx="8" fill="white" fill-opacity="0.15"/>
            <path d="M16 6L24 10V22L16 26L8 22V10L16 6Z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M16 12V20" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M12 16H20" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span class="sider-logo-text">伦理思政</span>
        </div>
      </div>

      <a-menu 
        theme="light" 
        mode="inline" 
        :selected-keys="selected" 
        class="sider-menu"
      >
        <a-menu-item 
          v-for="item in menuItems" 
          :key="item.key"
          @click="go(item.key)"
          class="menu-item"
        >
          <template #icon>
            <component :is="item.icon" class="menu-icon" />
          </template>
          <span class="menu-title">{{ item.title }}</span>
        </a-menu-item>
      </a-menu>

      <div class="sider-footer"></div>
    </a-layout-sider>

    <a-layout class="main-layout">
      <!-- Header -->
      <a-layout-header class="app-header">
        <div class="header-left">
          <a-button 
            v-if="isMobile" 
            type="text" 
            class="menu-trigger" 
            @click="mobileDrawerOpen = true"
          >
            <template #icon><MenuOutlined /></template>
          </a-button>
          
          <div class="header-title">
            <h1>{{ pageTitle }}</h1>
          </div>
        </div>

        <div class="header-right">
          <!-- 全局搜索 -->
          <a-input-search
            v-if="!isMobile"
            v-model:value="headerSearchQ"
            placeholder="搜索需求、文献、模块... (Ctrl+K)"
            class="header-search"
            @search="onHeaderSearch"
          />

          <!-- 主题切换 -->
          <a-tooltip :title="isDark ? '切换到亮色模式' : '切换到暗色模式'">
            <a-button 
              type="text" 
              class="theme-toggle"
              @click="toggleTheme"
            >
              <template #icon>
                <BulbOutlined v-if="!isDark" />
                <BulbFilled v-else />
              </template>
            </a-button>
          </a-tooltip>

          <!-- 用户菜单 -->
          <a-dropdown placement="bottomRight">
            <div class="header-user">
              <div class="user-avatar-small">
                <UserOutlined />
              </div>
              <span class="user-name-small">{{ auth.displayName || auth.username }}</span>
            </div>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile" @click="go('profile')">
                  <UserOutlined /> 个人中心
                </a-menu-item>
                <a-menu-divider />
                <a-menu-item key="logout" @click="logout" danger>
                  <SettingOutlined /> 退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <!-- 内容区 -->
      <a-layout-content class="app-content">
        <router-view />
      </a-layout-content>
    </a-layout>

    <!-- 移动端抽屉导航 -->
    <a-drawer
      v-model:open="mobileDrawerOpen"
      placement="left"
      :width="280"
      :closable="true"
      :body-style="{ padding: 0 }"
      class="mobile-drawer"
    >
      <div class="drawer-header">
        <div class="drawer-logo">
          <svg viewBox="0 0 32 32" fill="none">
            <rect width="32" height="32" rx="8" fill="white" fill-opacity="0.15"/>
            <path d="M16 6L24 10V22L16 26L8 22V10L16 6Z" stroke="white" stroke-width="1.5" stroke-linejoin="round"/>
            <path d="M16 12V20" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
            <path d="M12 16H20" stroke="white" stroke-width="1.5" stroke-linecap="round"/>
          </svg>
          <span>伦理思政</span>
        </div>
      </div>

      <a-menu mode="inline" :selected-keys="selected" class="drawer-menu">
        <a-menu-item 
          v-for="item in menuItems" 
          :key="item.key"
          @click="go(item.key)"
        >
          <template #icon>
            <component :is="item.icon" />
          </template>
          {{ item.title }}
        </a-menu-item>
      </a-menu>

      <div class="drawer-footer">
        <div class="drawer-user">
          <UserOutlined />
          <span>{{ auth.displayName || auth.username }}</span>
          <a-tag size="small" :color="auth.role === 'ADMIN' ? 'red' : 'blue'">
            {{ auth.role }}
          </a-tag>
        </div>
        <a-button type="text" danger @click="logout">退出登录</a-button>
      </div>
    </a-drawer>
  </a-layout>
</template>

<style scoped>
.app-shell {
  min-height: 100vh;
}

/* === 侧边栏 === */
.app-sider {
  background: var(--color-surface) !important;
  border-right: 1px solid var(--color-border) !important;
  display: flex;
  flex-direction: column;
  position: fixed !important;
  left: 0;
  top: 0;
  bottom: 0;
  z-index: var(--z-sticky);
  box-shadow: 2px 0 12px rgba(15, 23, 42, 0.04);
}

.sider-header {
  height: var(--header-height);
  display: flex;
  align-items: center;
  padding: 0 16px;
  border-bottom: 1px solid var(--color-border);
}

.sider-logo {
  display: flex;
  align-items: center;
  gap: 12px;
}

.sider-logo svg {
  width: 36px;
  height: 36px;
  flex-shrink: 0;
}

.sider-logo-text {
  font-size: 16px;
  font-weight: 700;
  color: var(--color-text-primary);
  letter-spacing: 1px;
}

.sider-menu {
  flex: 1;
  padding: 12px 8px;
  border-right: none !important;
}

.sider-menu :deep(.ant-menu-item) {
  height: 44px;
  line-height: 44px;
  margin: 2px 0;
  padding: 0 12px !important;
  border-radius: var(--radius-lg) !important;
  transition: all 150ms ease;
}

.sider-menu :deep(.ant-menu-item:hover) {
  background: var(--color-surface-hover) !important;
}

.sider-menu :deep(.ant-menu-item-selected) {
  background: var(--color-primary-light) !important;
  font-weight: 600;
}

.sider-menu :deep(.ant-menu-item-selected)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 4px;
  height: 24px;
  background: var(--color-primary);
  border-radius: 0 4px 4px 0;
}

.menu-icon {
  font-size: 18px;
}

.menu-title {
  margin-left: 8px;
}

.sider-footer {
  padding: 16px;
  border-top: 1px solid var(--color-border);
}

/* === 主布局 === */
.main-layout {
  margin-left: 240px;
  background: var(--color-background);
  min-height: 100vh;
  transition: margin-left 200ms ease;
}

.app-sider.ant-layout-sider-collapsed + .main-layout {
  margin-left: 72px;
}

/* === Header === */
.app-header {
  background: var(--color-surface) !important;
  height: var(--header-height) !important;
  padding: 0 24px !important;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(15, 23, 42, 0.04);
  position: sticky;
  top: 0;
  z-index: var(--z-sticky);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-title h1 {
  font-size: 18px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin: 0;
}

.menu-trigger {
  font-size: 18px;
  width: 40px;
  height: 40px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-search {
  width: 280px;
}

.header-search :deep(.ant-input-affix-wrapper) {
  border-radius: 20px;
  padding: 8px 16px;
  background: var(--color-background);
  border-color: transparent;
}

.header-search :deep(.ant-input-affix-wrapper:focus),
.header-search :deep(.ant-input-affix-wrapper-focused) {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px rgba(66, 99, 235, 0.1);
}

.theme-toggle {
  width: 40px;
  height: 40px;
  font-size: 18px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.theme-toggle:hover {
  background: var(--color-surface-hover);
}

.header-user {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: var(--radius-lg);
  cursor: pointer;
  transition: background 150ms ease;
}

.header-user:hover {
  background: var(--color-surface-hover);
}

.user-avatar-small {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--color-primary-light);
  color: var(--color-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.user-name-small {
  font-size: 14px;
  color: var(--color-text-primary);
  font-weight: 500;
}

/* === 内容区 === */
.app-content {
  padding: 24px;
  min-height: calc(100vh - var(--header-height));
}

/* === 移动端抽屉 === */
.mobile-drawer :deep(.ant-drawer-header) {
  display: none;
}

.drawer-header {
  height: var(--header-height);
  padding: 0 20px;
  display: flex;
  align-items: center;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-hover) 100%);
}

.drawer-logo {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  font-size: 16px;
  font-weight: 600;
}

.drawer-logo svg {
  width: 32px;
  height: 32px;
}

.drawer-menu {
  padding: 12px 8px;
}

.drawer-menu :deep(.ant-menu-item) {
  height: 48px;
  line-height: 48px;
  margin: 2px 0;
  border-radius: var(--radius-lg);
}

.drawer-footer {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.drawer-user {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-primary);
}

/* === 响应式 === */
@media (max-width: 1199px) {
  .app-sider {
    display: none !important;
  }

  .main-layout {
    margin-left: 0 !important;
  }

  .header-search {
    display: none;
  }

  .user-name-small {
    display: none;
  }
}

@media (max-width: 767px) {
  .app-header {
    padding: 0 16px !important;
  }

  .app-content {
    padding: 16px;
  }

  .header-title h1 {
    font-size: 16px;
  }
}
</style>
