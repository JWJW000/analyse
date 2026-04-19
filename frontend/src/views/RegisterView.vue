<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

const formState = reactive({
  username: '',
  password: '',
  displayName: '',
})

const loading = ref(false)

async function onSubmit() {
  loading.value = true
  try {
    const username = formState.username.trim()
    await auth.register(
      username,
      formState.password,
      formState.displayName || undefined,
    )
    await router.push('/app')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '注册失败')
  } finally {
    loading.value = false
  }
}

function onFinishFailed() {
  message.warning('请按要求填写用户名（至少 3 位）和密码（至少 6 位）')
}
</script>

<template>
  <div class="wrap">
    <a-card title="注册（学生）" class="auth-card">
      <a-alert
        message="教师与管理员账号由管理员在后台创建，此处仅开放学生自助注册。"
        type="info"
        show-icon
        style="margin-bottom: 16px"
      />
      <a-form
        :model="formState"
        layout="vertical"
        @finish="onSubmit"
        @finish-failed="onFinishFailed"
      >
        <a-form-item
          label="用户名"
          name="username"
          :rules="[
            { required: true, message: '请输入用户名' },
            { min: 3, message: '用户名至少 3 位' },
          ]"
        >
          <a-input v-model:value="formState.username" allow-clear />
        </a-form-item>
        <a-form-item
          label="密码"
          name="password"
          :rules="[
            { required: true, message: '请输入密码' },
            { min: 6, message: '密码至少 6 位' },
          ]"
        >
          <a-input-password v-model:value="formState.password" />
        </a-form-item>
        <a-form-item label="显示名" name="displayName">
          <a-input v-model:value="formState.displayName" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" html-type="submit" block :loading="loading">注册</a-button>
        </a-form-item>
        <a-form-item>
          <a-button type="link" block @click="router.push({ name: 'login' })">返回登录</a-button>
        </a-form-item>
      </a-form>
    </a-card>
  </div>
</template>

<style scoped>
.wrap {
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f0f2f5;
  padding: 16px;
  padding-left: max(16px, env(safe-area-inset-left));
  padding-right: max(16px, env(safe-area-inset-right));
  padding-bottom: max(16px, env(safe-area-inset-bottom));
}

.auth-card {
  width: 100%;
  max-width: 420px;
}
</style>
