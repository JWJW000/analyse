<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const formState = reactive({
  username: '',
  password: '',
})

const loading = ref(false)

async function onSubmit() {
  if (!formState.username.trim() || !formState.password) {
    message.warning('请输入用户名和密码')
    return
  }
  loading.value = true
  try {
    await auth.login(formState.username, formState.password)
    const red = (route.query.redirect as string) || '/app'
    await router.push(red)
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区域 -->
    <div class="login-brand">
      <div class="brand-content">
        <div class="brand-logo">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="white" fill-opacity="0.15"/>
            <path d="M24 8L36 16V32L24 40L12 32V16L24 8Z" stroke="white" stroke-width="2" stroke-linejoin="round"/>
            <path d="M24 20V28" stroke="white" stroke-width="2" stroke-linecap="round"/>
            <path d="M18 24H30" stroke="white" stroke-width="2" stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="brand-title">伦理思政</h1>
        <p class="brand-subtitle">需求分析辅助工具</p>
        
        <div class="brand-features">
          <div class="feature-item">
            <span class="feature-icon">📚</span>
            <span>丰富的思政模块库</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">🤖</span>
            <span>AI 智能匹配推荐</span>
          </div>
          <div class="feature-item">
            <span class="feature-icon">📊</span>
            <span>可视化数据分析</span>
          </div>
        </div>
      </div>
      
      <div class="brand-decoration">
        <div class="decoration-circle decoration-circle-1"></div>
        <div class="decoration-circle decoration-circle-2"></div>
        <div class="decoration-circle decoration-circle-3"></div>
      </div>
    </div>

    <!-- 右侧登录表单区域 -->
    <div class="login-form-area">
      <div class="login-form-container">
        <div class="login-form-header">
          <h2>欢迎回来</h2>
          <p>请登录您的账号继续使用</p>
        </div>

        <a-form
          :model="formState"
          layout="vertical"
          autocomplete="on"
          @finish="onSubmit"
          class="login-form"
        >
          <a-form-item 
            label="用户名" 
            name="username"
            :rules="[{ required: true, message: '请输入用户名' }]"
          >
            <a-input 
              v-model:value="formState.username" 
              placeholder="请输入用户名"
              size="large"
              autocomplete="username"
            >
              <template #prefix>
                <span class="input-prefix-icon">👤</span>
              </template>
            </a-input>
          </a-form-item>

          <a-form-item 
            label="密码" 
            name="password"
            :rules="[{ required: true, message: '请输入密码' }]"
          >
            <a-input-password 
              v-model:value="formState.password" 
              placeholder="请输入密码"
              size="large"
              autocomplete="current-password"
            >
              <template #prefix>
                <span class="input-prefix-icon">🔒</span>
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <a-button 
              type="primary" 
              html-type="submit" 
              block 
              size="large"
              :loading="loading"
              class="login-btn"
            >
              {{ loading ? '登录中...' : '登 录' }}
            </a-button>
          </a-form-item>

          <div class="login-form-footer">
            <span>还没有账号？</span>
            <a href="#" @click.prevent="router.push({ name: 'register' })">注册学生账号</a>
          </div>
        </a-form>

        <div class="login-tips">
          <div class="tip-item">
            <span class="tip-icon">💡</span>
            <span>提示：默认管理员账号 admin / admin123</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
}

/* === 左侧品牌区域 === */
.login-brand {
  flex: 1;
  background: linear-gradient(135deg, #4263EB 0%, #5C7CFA 50%, #748FFC 100%);
  padding: 60px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  position: relative;
  overflow: hidden;
}

.brand-content {
  position: relative;
  z-index: 1;
  max-width: 400px;
}

.brand-logo {
  width: 72px;
  height: 72px;
  margin-bottom: 24px;
  animation: fadeInUp 600ms ease forwards;
}

.brand-logo svg {
  width: 100%;
  height: 100%;
}

.brand-title {
  font-size: 36px;
  font-weight: 700;
  color: white;
  margin: 0 0 8px;
  letter-spacing: 2px;
  animation: fadeInUp 600ms ease 100ms forwards;
  opacity: 0;
}

.brand-subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.85);
  margin: 0 0 48px;
  animation: fadeInUp 600ms ease 200ms forwards;
  opacity: 0;
}

.brand-features {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: fadeInUp 600ms ease 300ms forwards;
  opacity: 0;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 12px;
  color: white;
  font-size: 15px;
}

.feature-icon {
  font-size: 20px;
}

/* 装饰圆形 */
.brand-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.decoration-circle {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.08);
}

.decoration-circle-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  animation: float 8s ease-in-out infinite;
}

.decoration-circle-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  left: -50px;
  animation: float 6s ease-in-out infinite reverse;
}

.decoration-circle-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  right: 20%;
  animation: float 5s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-20px);
  }
}

@keyframes fadeInUp {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* === 右侧表单区域 === */
.login-form-area {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 40px;
  background: var(--color-background);
}

.login-form-container {
  width: 100%;
  max-width: 400px;
  animation: fadeIn 400ms ease;
}

.login-form-header {
  text-align: center;
  margin-bottom: 40px;
}

.login-form-header h2 {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 8px;
}

.login-form-header p {
  font-size: 14px;
  color: var(--color-text-secondary);
  margin: 0;
}

/* 表单样式 */
.login-form :deep(.ant-form-item-label > label) {
  font-weight: 500;
  color: var(--color-text-primary);
}

.login-form :deep(.ant-input),
.login-form :deep(.ant-input-affix-wrapper) {
  font-size: 15px;
  height: 48px;
  border-radius: var(--radius-lg);
}

.login-form :deep(.ant-input-affix-wrapper) {
  padding: 0 16px;
}

.login-form :deep(.ant-input-affix-wrapper .ant-input) {
  padding: 12px 0;
  height: 46px;
}

.login-form :deep(.ant-input-password .ant-input) {
  padding: 12px 0;
  height: 46px;
}

.login-form :deep(.ant-input-affix-wrapper:hover),
.login-form :deep(.ant-input-affix-wrapper-focused),
.login-form :deep(.ant-input:focus) {
  border-color: var(--color-primary);
}

.input-prefix-icon {
  font-size: 16px;
  margin-right: 8px;
}

/* 登录按钮 */
.login-btn {
  height: 48px !important;
  font-size: 16px !important;
  font-weight: 600 !important;
  border-radius: var(--radius-lg) !important;
  margin-top: 8px;
  box-shadow: 0 4px 14px rgba(66, 99, 235, 0.35) !important;
  transition: all 200ms ease !important;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(66, 99, 235, 0.45) !important;
}

.login-btn:active {
  transform: translateY(0);
}

/* 表单底部 */
.login-form-footer {
  text-align: center;
  margin-top: 24px;
  font-size: 14px;
  color: var(--color-text-secondary);
}

.login-form-footer a {
  color: var(--color-primary);
  font-weight: 500;
  margin-left: 4px;
}

.login-form-footer a:hover {
  color: var(--color-primary-hover);
}

/* 提示信息 */
.login-tips {
  margin-top: 32px;
  padding: 16px;
  background: var(--color-surface);
  border-radius: var(--radius-lg);
  border: 1px solid var(--color-border);
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
}

.tip-icon {
  font-size: 14px;
}

/* === 响应式 === */
@media (max-width: 991px) {
  .login-page {
    flex-direction: column;
  }

  .login-brand {
    padding: 40px 24px;
    min-height: auto;
  }

  .brand-features {
    display: none;
  }

  .brand-title {
    font-size: 28px;
  }

  .brand-logo {
    width: 56px;
    height: 56px;
  }

  .login-form-area {
    padding: 40px 24px;
  }
}

/* 动画 */
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
