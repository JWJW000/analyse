<script setup lang="ts">
import { nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { Graph } from '@antv/x6'
import { getCellEditableText, setCellEditableText, type EditableX6Cell } from '@/utils/x6EditableText'
import { getInlineEditorPosition, type PointerLike } from '@/utils/inlineCellEditor'

const props = defineProps<{
  modelValue: string | null
}>()
const emit = defineEmits<{
  'update:modelValue': [v: string]
}>()

const frame = ref<HTMLElement | null>(null)
const graphWrap = ref<HTMLElement | null>(null)
const editorInput = ref<HTMLInputElement | null>(null)
let graph: Graph | null = null
let lastEditAt = 0
let selectedCell: EditableGraphCell | null = null

type RelationKind = 'association' | 'include' | 'extend'

const editor = reactive<{
  visible: boolean
  left: number
  top: number
  value: string
  cell: EditableGraphCell | null
}>({
  visible: false,
  left: 0,
  top: 0,
  value: '',
  cell: null,
})

const connectionMode = reactive<{
  kind: RelationKind | null
  source: EditableGraphCell | null
}>({
  kind: null,
  source: null,
})

type EditableGraphCell = {
  id?: string
  toJSON: () => EditableX6Cell
  prop: (pathOrProps: string | Record<string, unknown>, value?: unknown) => void
}

type ConnectableGraph = Graph & {
  getNodes: () => EditableGraphCell[]
  removeCell: (cell: EditableGraphCell) => void
}

function emitJson() {
  if (!graph) return
  emit('update:modelValue', JSON.stringify(graph.toJSON()))
}

function makeNodeConnectable(node: EditableGraphCell) {
  const json = node.toJSON()
  if (json.shape === 'edge') return
  node.prop({
    attrs: {
      ...json.attrs,
      body: {
        ...(typeof json.attrs?.body === 'object' && json.attrs.body ? json.attrs.body : {}),
        magnet: true,
      },
    },
  })
}

function makeExistingNodesConnectable() {
  if (!graph) return
  ;(graph as ConnectableGraph).getNodes().forEach(makeNodeConnectable)
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
      body: { stroke: '#5F95FF', fill: '#EFF4FF', rx: 6, ry: 6, magnet: true },
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
      body: { stroke: '#73d13d', fill: '#f6ffed', magnet: true },
      label: { fill: '#333' },
    },
  })
  emitJson()
}

function startConnection(kind: RelationKind) {
  closeInlineEdit()
  connectionMode.kind = kind
  connectionMode.source = null
}

function getCellId(cell: EditableGraphCell) {
  return String(cell.id ?? cell.toJSON().id ?? '')
}

function buildRelationEdge(sourceId: string, targetId: string, kind: RelationKind) {
  const isDashed = kind !== 'association'
  const label = kind === 'include' ? '<<include>>' : kind === 'extend' ? '<<extend>>' : ''
  return {
    source: { cell: sourceId },
    target: { cell: targetId },
    attrs: {
      line: {
        stroke: '#A2B1C3',
        strokeWidth: 2,
        targetMarker: {
          name: 'block',
          width: 8,
          height: 6,
        },
        ...(isDashed ? { strokeDasharray: '5 5' } : {}),
      },
    },
    ...(label ? { labels: [{ attrs: { label: { text: label } } }] } : {}),
  }
}

function handleConnectionNodeClick(node: EditableGraphCell) {
  if (!graph || !connectionMode.kind) return false
  if (!connectionMode.source) {
    connectionMode.source = node
    return true
  }

  const sourceId = getCellId(connectionMode.source)
  const targetId = getCellId(node)
  if (sourceId && targetId && sourceId !== targetId) {
    graph.addEdge(buildRelationEdge(sourceId, targetId, connectionMode.kind))
    emitJson()
  }
  connectionMode.kind = null
  connectionMode.source = null
  return true
}

function selectCell(cell: EditableGraphCell) {
  selectedCell = cell
}

function clearSelection() {
  selectedCell = null
}

function deleteSelectedCell() {
  if (!graph || !selectedCell) return
  ;(graph as ConnectableGraph).removeCell(selectedCell)
  selectedCell = null
  closeInlineEdit()
  connectionMode.kind = null
  connectionMode.source = null
  emitJson()
}

function handleDeleteKey(event: KeyboardEvent) {
  const target = event.target as HTMLElement | null
  if (target?.tagName === 'INPUT' || target?.tagName === 'TEXTAREA' || target?.isContentEditable) return
  if (event.key !== 'Delete' && event.key !== 'Backspace') return
  if (!selectedCell) return
  event.preventDefault()
  deleteSelectedCell()
}

function applyCellText(cell: EditableGraphCell, text: string) {
  const json = cell.toJSON()
  setCellEditableText(json, text)
  if (json.shape === 'edge') {
    cell.prop('labels', json.labels ?? [])
  } else {
    cell.prop({
      label: json.label,
      attrs: json.attrs ?? {},
    })
  }
  emitJson()
}

function startInlineEdit(cell: EditableGraphCell, pointer: PointerLike) {
  const now = Date.now()
  if (now - lastEditAt < 200) return
  lastEditAt = now

  const json = cell.toJSON()
  const rect = frame.value?.getBoundingClientRect()
  if (!rect) return
  const position = getInlineEditorPosition(pointer, rect)

  editor.visible = true
  editor.left = position.left
  editor.top = position.top
  editor.value = getCellEditableText(json)
  editor.cell = cell

  nextTick(() => {
    editorInput.value?.focus()
    editorInput.value?.select()
  })
}

function commitInlineEdit() {
  if (!editor.visible || !editor.cell) return
  const next = editor.value.trim()
  if (next) {
    applyCellText(editor.cell, next)
  }
  closeInlineEdit()
}

function closeInlineEdit() {
  editor.visible = false
  editor.value = ''
  editor.cell = null
}

onMounted(() => {
  if (!graphWrap.value) return
  graph = new Graph({
    container: graphWrap.value,
    autoResize: true,
    grid: true,
    panning: true,
    mousewheel: { enabled: true, modifiers: ['ctrl', 'meta'] },
    connecting: {
      router: 'manhattan',
      connector: 'rounded',
      snap: true,
      allowBlank: false,
      allowLoop: false,
    },
  })
  graph.on('node:change:position', emitJson)
  graph.on('edge:connected', emitJson)
  graph.on('node:removed', emitJson)
  graph.on('edge:removed', emitJson)
  graph.on('node:click', ({ node }) => handleConnectionNodeClick(node as EditableGraphCell))
  graph.on('cell:click', ({ cell }) => selectCell(cell as EditableGraphCell))
  graph.on('blank:click', clearSelection)
  graph.on('cell:dblclick', ({ cell, e }) => startInlineEdit(cell as EditableGraphCell, e))
  graph.on('node:dblclick', ({ node, e }) => startInlineEdit(node as EditableGraphCell, e))
  graph.on('edge:dblclick', ({ edge, e }) => startInlineEdit(edge as EditableGraphCell, e))
  if (props.modelValue) {
    try {
      graph.fromJSON(JSON.parse(props.modelValue))
      makeExistingNodesConnectable()
    } catch {
      /* empty */
    }
  }
  window.addEventListener('keydown', handleDeleteKey)
})

onUnmounted(() => {
  window.removeEventListener('keydown', handleDeleteKey)
  graph?.dispose()
  graph = null
})

watch(
  () => props.modelValue,
  (v) => {
    if (!graph || !v) return
    try {
      const cur = JSON.stringify(graph.toJSON())
      if (cur !== v) {
        graph.fromJSON(JSON.parse(v))
        makeExistingNodesConnectable()
      }
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
      <a-button size="small" :type="connectionMode.kind === 'association' ? 'primary' : 'default'" @click="startConnection('association')">关联连线</a-button>
      <a-button size="small" :type="connectionMode.kind === 'include' ? 'primary' : 'default'" @click="startConnection('include')">包含连线</a-button>
      <a-button size="small" :type="connectionMode.kind === 'extend' ? 'primary' : 'default'" @click="startConnection('extend')">扩展连线</a-button>
      <a-button size="small" danger @click="deleteSelectedCell">删除</a-button>
    </a-space>
    <div ref="frame" class="use-case-canvas">
      <div ref="graphWrap" class="use-case-graph" />
      <input
        v-if="editor.visible"
        ref="editorInput"
        v-model="editor.value"
        class="inline-label-editor"
        :style="{ left: `${editor.left}px`, top: `${editor.top}px` }"
        @blur="commitInlineEdit"
        @keydown.enter.prevent="commitInlineEdit"
        @keydown.esc.prevent="closeInlineEdit"
        @mousedown.stop
        @dblclick.stop
      />
    </div>
  </div>
</template>

<style scoped>
.use-case-canvas {
  position: relative;
  height: 420px;
  border: 1px solid #f0f0f0;
  overflow: hidden;
}

.use-case-graph {
  position: absolute;
  inset: 0;
  z-index: 1;
}

.inline-label-editor {
  position: absolute;
  z-index: 2;
  width: 140px;
  height: 32px;
  padding: 4px 8px;
  border: 1px solid #1677ff;
  border-radius: 4px;
  background: #fff;
  color: rgba(0, 0, 0, 0.88);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  outline: none;
}
</style>
