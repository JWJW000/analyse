<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const profileForm = reactive({
  displayName: '',
})

const pwdForm = reactive({
  oldPassword: '',
  newPassword: '',
  newPassword2: '',
})

const saving = ref(false)
const pwdLoading = ref(false)

type AuditRow = {
  id: number
  action: string
  entityType: string | null
  entityId: number | null
  createdAt: string | null
}

const activity = ref<AuditRow[]>([])
const actPage = ref(0)
const actTotal = ref(0)
const actPageSize = 15
const actLoading = ref(false)

async function loadProfile() {
  await auth.fetchMe()
  profileForm.displayName = auth.displayName || ''
}

async function saveProfile() {
  saving.value = true
  try {
    await unwrap(http.patch('/api/me', { displayName: profileForm.displayName || null }))
    await auth.fetchMe()
    message.success('资料已更新')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function changePassword() {
  if (pwdForm.newPassword !== pwdForm.newPassword2) {
    message.warning('两次新密码不一致')
    return
  }
  if (pwdForm.newPassword.length < 6) {
    message.warning('新密码至少 6 位')
    return
  }
  pwdLoading.value = true
  try {
    await unwrap(
      http.post('/api/me/password', {
        oldPassword: pwdForm.oldPassword,
        newPassword: pwdForm.newPassword,
      }),
    )
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.newPassword2 = ''
    message.success('密码已修改')
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '修改失败')
  } finally {
    pwdLoading.value = false
  }
}

async function loadActivity(page = 0) {
  actLoading.value = true
  try {
    const res = (await unwrap(
      http.get('/api/me/activity', { params: { page, size: actPageSize } }),
    )) as {
      content: AuditRow[]
      totalElements: number
      number: number
    }
    activity.value = res.content || []
    actTotal.value = res.totalElements ?? 0
    actPage.value = res.number ?? page
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '加载失败')
  } finally {
    actLoading.value = false
  }
}

function onActPage(page: number) {
  loadActivity(page - 1)
}

onMounted(async () => {
  await loadProfile()
  await loadActivity(0)
})
</script>

<template>
  <a-space direction="vertical" size="large" style="width: 100%">
    <a-typography-title :level="4">个人中心</a-typography-title>

    <a-card title="基本资料" size="small">
      <a-form layout="vertical" style="max-width: 420px">
        <a-form-item label="用户名">
          <a-input :value="auth.username || ''" disabled />
        </a-form-item>
        <a-form-item label="角色">
          <a-tag>{{ auth.role }}</a-tag>
        </a-form-item>
        <a-form-item label="显示名">
          <a-input v-model:value="profileForm.displayName" placeholder="姓名或昵称" allow-clear />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="saving" @click="saveProfile">保存资料</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="修改密码" size="small">
      <a-form layout="vertical" style="max-width: 420px">
        <a-form-item label="当前密码">
          <a-input-password v-model:value="pwdForm.oldPassword" autocomplete="current-password" />
        </a-form-item>
        <a-form-item label="新密码">
          <a-input-password v-model:value="pwdForm.newPassword" autocomplete="new-password" />
        </a-form-item>
        <a-form-item label="确认新密码">
          <a-input-password v-model:value="pwdForm.newPassword2" autocomplete="new-password" />
        </a-form-item>
        <a-form-item>
          <a-button type="primary" :loading="pwdLoading" @click="changePassword">更新密码</a-button>
        </a-form-item>
      </a-form>
    </a-card>

    <a-card title="操作记录" size="small">
      <a-table
        :data-source="activity"
        :loading="actLoading"
        :pagination="{
          current: actPage + 1,
          pageSize: actPageSize,
          total: actTotal,
          showSizeChanger: false,
          onChange: (p: number) => onActPage(p),
        }"
        row-key="id"
        size="small"
        :scroll="{ x: 'max-content' }"
      >
        <a-table-column title="时间" data-index="createdAt" width="200" />
        <a-table-column title="动作" data-index="action" />
        <a-table-column title="对象" data-index="entityType" />
        <a-table-column title="对象 ID" data-index="entityId" />
      </a-table>
    </a-card>
  </a-space>
</template>
