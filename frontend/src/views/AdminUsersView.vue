<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'

type U = { id: number; username: string; role: string; displayName: string | null }

const list = ref<U[]>([])
const form = ref({
  username: '',
  password: '',
  role: 'TEACHER' as 'STUDENT' | 'TEACHER' | 'ADMIN',
  displayName: '',
})
const reset = ref({ userId: undefined as number | undefined, newPassword: '' })

async function load() {
  const res = await unwrap(http.get('/api/admin/users'))
  list.value = res as U[]
}

async function createUser() {
  await unwrap(http.post('/api/admin/users', form.value))
  message.success('已创建')
  await load()
}

async function doReset() {
  if (!reset.value.userId) return
  await unwrap(
    http.post(`/api/admin/users/${reset.value.userId}/reset-password`, {
      newPassword: reset.value.newPassword,
    }),
  )
  message.success('密码已重置')
}

onMounted(load)
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-card title="新建用户">
      <a-space wrap>
        <a-input v-model:value="form.username" placeholder="用户名" style="width: 140px" />
        <a-input-password v-model:value="form.password" placeholder="密码" style="width: 140px" />
        <a-select v-model:value="form.role" style="width: 120px">
          <a-select-option value="STUDENT">学生</a-select-option>
          <a-select-option value="TEACHER">教师</a-select-option>
          <a-select-option value="ADMIN">管理员</a-select-option>
        </a-select>
        <a-input v-model:value="form.displayName" placeholder="显示名" style="width: 140px" />
        <a-button type="primary" @click="createUser">创建</a-button>
      </a-space>
    </a-card>
    <a-card title="重置密码">
      <a-space>
        <a-input-number v-model:value="reset.userId" placeholder="用户 ID" />
        <a-input-password v-model:value="reset.newPassword" placeholder="新密码" />
        <a-button @click="doReset">重置</a-button>
      </a-space>
    </a-card>
    <div class="responsive-table-wrap">
      <a-table :data-source="list" :pagination="false" row-key="id" :scroll="{ x: 'max-content' }">
      <a-table-column title="ID" data-index="id" />
      <a-table-column title="用户名" data-index="username" />
      <a-table-column title="角色" data-index="role" />
      <a-table-column title="显示名" data-index="displayName" />
    </a-table>
    </div>
  </a-space>
</template>
