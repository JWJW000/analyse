<script setup lang="ts">
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { Graph } from '@antv/x6'

const props = defineProps<{
  modelValue: string | null
}>()
const emit = defineEmits<{
  'update:modelValue': [v: string]
}>()

const wrap = ref<HTMLElement | null>(null)
let graph: Graph | null = null

function emitJson() {
  if (!graph) return
  emit('update:modelValue', JSON.stringify(graph.toJSON()))
}

function addActor() {
  if (!graph) return
  graph.addNode({
    x: 40 + Math.random() * 40,
    y: 40 + Math.random() * 40,
    width: 100,
    height: 40,
    label: '参与者',
    attrs: {
      body: { stroke: '#5F95FF', fill: '#EFF4FF', rx: 6, ry: 6 },
      label: { fill: '#333' },
    },
  })
  emitJson()
}

function addUseCase() {
  if (!graph) return
  graph.addNode({
    shape: 'ellipse',
    x: 200 + Math.random() * 40,
    y: 40 + Math.random() * 40,
    width: 140,
    height: 64,
    label: '用例',
    attrs: {
      body: { stroke: '#73d13d', fill: '#f6ffed' },
      label: { fill: '#333' },
    },
  })
  emitJson()
}

onMounted(() => {
  if (!wrap.value) return
  graph = new Graph({
    container: wrap.value,
    autoResize: true,
    grid: true,
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },
    connecting: {
      router: 'manhattan',
      connector: 'rounded',
    },
  })
  graph.on('node:change:position', emitJson)
  graph.on('edge:connected', emitJson)
  graph.on('node:removed', emitJson)
  graph.on('edge:removed', emitJson)
  if (props.modelValue) {
    try {
      graph.fromJSON(JSON.parse(props.modelValue))
    } catch {
      /* empty */
    }
  }
})

onUnmounted(() => {
  graph?.dispose()
  graph = null
})

watch(
  () => props.modelValue,
  (v) => {
    if (!graph || !v) return
    try {
      const cur = JSON.stringify(graph.toJSON())
      if (cur !== v) graph.fromJSON(JSON.parse(v))
    } catch {
      /* ignore */
    }
  },
)
</script>

<template>
  <div>
    <a-space style="margin-bottom: 8px">
      <a-button size="small" @click="addActor">添加参与者</a-button>
      <a-button size="small" @click="addUseCase">添加用例</a-button>
    </a-space>
    <div ref="wrap" style="height: 420px; border: 1px solid #f0f0f0" />
  </div>
</template>
