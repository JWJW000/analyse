<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http, { unwrap } from '@/api/http'
import { message } from 'ant-design-vue'

const logs = ref<string[]>([])
const backups = ref<{ id: number; filePath: string; status: string; createdAt: string }[]>([])
const configText = ref('')

async function loadLogs() {
  const res = await unwrap(http.get('/api/admin/logs', { params: { lines: 200 } }))
  logs.value = res as string[]
}

async function loadBackups() {
  const res = await unwrap(http.get('/api/admin/backups'))
  backups.value = res as typeof backups.value
}

async function loadConfig() {
  const res = await unwrap(http.get('/api/admin/config'))
  configText.value = JSON.stringify(res, null, 2)
}

async function saveConfig() {
  const obj = JSON.parse(configText.value)
  await unwrap(http.put('/api/admin/config', obj))
  message.success('配置已保存')
}

async function runBackup() {
  try {
    await unwrap(http.post('/api/admin/backup'))
    message.success('备份已触发')
    await loadBackups()
  } catch (e: unknown) {
    message.error(e instanceof Error ? e.message : '备份失败（需本机 mysqldump）')
  }
}

onMounted(async () => {
  await loadLogs()
  await loadBackups()
  await loadConfig()
})
</script>

<template>
  <a-space direction="vertical" style="width: 100%">
    <a-card title="数据库备份">
      <a-button type="primary" @click="runBackup">执行 mysqldump</a-button>
      <div class="responsive-table-wrap">
        <a-table
          :data-source="backups"
          :pagination="false"
          row-key="id"
          style="margin-top: 12px"
          :scroll="{ x: 'max-content' }"
        >
        <a-table-column title="文件" data-index="filePath" />
        <a-table-column title="状态" data-index="status" />
      </a-table>
      </div>
    </a-card>
    <a-card title="系统配置 (JSON)">
      <a-textarea v-model:value="configText" :rows="8" />
      <a-button style="margin-top: 8px" @click="saveConfig">保存</a-button>
    </a-card>
    <a-card title="最近日志">
      <a-button size="small" @click="loadLogs">刷新</a-button>
      <pre style="max-height: 320px; overflow: auto; background: #fafafa; padding: 8px">{{
        logs.join('\n')
      }}</pre>
    </a-card>
  </a-space>
</template>
