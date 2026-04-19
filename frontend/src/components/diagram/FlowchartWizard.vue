<script setup lang="ts">
import { ref } from 'vue'
import { Card, Button, Form, FormItem, Input, Select, SelectOption, Space, Divider, Tag, Modal } from 'ant-design-vue'
import { useFlowchart, type FlowNode, type FlowConnection, type FlowNodeType } from '@/composables/useFlowchart'

const currentStep = ref(0)
const nodes = ref<FlowNode[]>([])
const connections = ref<FlowConnection[]>([])
const nextNodeId = ref(1)
const nextConnId = ref(1)

const newNode = ref<Partial<FlowNode>>({ type: 'process', label: '' })
const newConn = ref({ source: '', target: '', label: '' })

const { generateFromData, exportToJSON, nodeColors } = useFlowchart()

const nodeTypes: { value: FlowNodeType; label: string }[] = [
  { value: 'start', label: '开始' },
  { value: 'end', label: '结束' },
  { value: 'process', label: '处理' },
  { value: 'decision', label: '判断' },
  { value: 'input', label: '输入' },
  { value: 'output', label: '输出' },
]

function addNode() {
  if (!newNode.value.label) return
  nodes.value.push({
    id: `node-${nextNodeId.value++}`,
    type: newNode.value.type || 'process',
    label: newNode.value.label,
    description: newNode.value.description,
  })
  newNode.value = { type: 'process', label: '' }
}

function removeNode(id: string) {
  nodes.value = nodes.value.filter(n => n.id !== id)
  connections.value = connections.value.filter(c => c.source !== id && c.target !== id)
}

function addConnection() {
  if (!newConn.value.source || !newConn.value.target) return
  if (newConn.value.source === newConn.value.target) return
  connections.value.push({
    id: `conn-${nextConnId.value++}`,
    source: newConn.value.source,
    target: newConn.value.target,
    label: newConn.value.label,
  })
  newConn.value = { source: '', target: '', label: '' }
}

function generate() {
  generateFromData({ nodes: nodes.value, connections: connections.value })
  Modal.success({
    title: '生成成功',
    content: '流程图数据已生成，可以在控制台查看输出',
  })
}

const stepTitles = ['节点', '连接', '生成']
</script>

<template>
  <Card title="流程图生成向导" class="wizard-card">
    <div class="wizard-steps">
      <div
        v-for="(title, index) in stepTitles"
        :key="title"
        class="step-item"
        :class="{ active: currentStep === index, completed: currentStep > index }"
      >
        <div class="step-number">{{ index + 1 }}</div>
        <div class="step-title">{{ title }}</div>
      </div>
    </div>

    <div class="wizard-content">
      <div v-if="currentStep === 0" class="step-content">
        <h4>添加流程节点</h4>
        <Form layout="inline" style="margin-bottom: 16px">
          <FormItem label="类型">
            <Select v-model:value="newNode.type" style="width: 120px">
              <SelectOption v-for="t in nodeTypes" :key="t.value" :value="t.value">
                {{ t.label }}
              </SelectOption>
            </Select>
          </FormItem>
          <FormItem label="名称">
            <Input v-model:value="newNode.label" placeholder="节点名称" style="width: 200px" />
          </FormItem>
          <FormItem>
            <Button type="primary" @click="addNode">添加</Button>
          </FormItem>
        </Form>
        <div class="item-list">
          <div v-for="node in nodes" :key="node.id" class="item-row">
            <Tag :color="nodeColors[node.type].fill.replace('#', '')">{{ node.type }}</Tag>
            <span>{{ node.label }}</span>
            <Button type="text" size="small" class="delete-btn" @click="removeNode(node.id)">删除</Button>
          </div>
          <div v-if="nodes.length === 0" class="empty-tip">暂无节点，请添加</div>
        </div>
      </div>

      <div v-if="currentStep === 1" class="step-content">
        <h4>建立流程连接</h4>
        <Form layout="inline" style="margin-bottom: 16px">
          <FormItem label="从">
            <Select v-model:value="newConn.source" placeholder="源节点" style="width: 150px">
              <SelectOption v-for="n in nodes" :key="n.id" :value="n.id">{{ n.label }}</SelectOption>
            </Select>
          </FormItem>
          <FormItem label="到">
            <Select v-model:value="newConn.target" placeholder="目标节点" style="width: 150px">
              <SelectOption v-for="n in nodes" :key="n.id" :value="n.id">{{ n.label }}</SelectOption>
            </Select>
          </FormItem>
          <FormItem label="标签">
            <Input v-model:value="newConn.label" placeholder="连接标签" style="width: 120px" />
          </FormItem>
          <FormItem>
            <Button type="primary" @click="addConnection">添加</Button>
          </FormItem>
        </Form>
        <div class="item-list">
          <div v-for="conn in connections" :key="conn.id" class="item-row">
            <span>{{ nodes.find(n => n.id === conn.source)?.label }}</span>
            <span class="arrow">→</span>
            <span>{{ nodes.find(n => n.id === conn.target)?.label }}</span>
            <Tag v-if="conn.label">{{ conn.label }}</Tag>
            <Button type="text" size="small" class="delete-btn" @click="connections = connections.filter(c => c.id !== conn.id)">删除</Button>
          </div>
          <div v-if="connections.length === 0" class="empty-tip">暂无连接，请添加</div>
        </div>
      </div>

      <div v-if="currentStep === 2" class="step-content">
        <h4>预览结果</h4>
        <Divider>生成的图形数据</Divider>
        <pre class="json-preview">{{ exportToJSON() }}</pre>
        <Button type="primary" @click="generate">生成流程图</Button>
      </div>
    </div>

    <div class="wizard-footer">
      <Space>
        <Button v-if="currentStep > 0" @click="currentStep--">上一步</Button>
        <Button v-if="currentStep < 2" type="primary" @click="currentStep++">下一步</Button>
        <Button v-if="currentStep === 2" type="primary" @click="generate">生成流程图</Button>
      </Space>
    </div>
  </Card>
</template>

<style scoped>
.wizard-card {
  max-width: 800px;
  margin: 0 auto;
}

.wizard-steps {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24px;
  padding: 0 20px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--color-text-secondary);
}

.step-item.active {
  color: var(--color-primary);
}

.step-item.completed {
  color: var(--color-success);
}

.step-number {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: currentColor;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.step-item.active .step-number {
  background: var(--color-primary);
}

.step-item.completed .step-number {
  background: var(--color-success);
}

.step-title {
  font-size: 14px;
}

.wizard-content {
  min-height: 300px;
  padding: 24px 0;
}

.step-content h4 {
  margin-bottom: 16px;
  color: var(--color-text);
}

.item-list {
  border: 1px solid var(--color-border);
  border-radius: 4px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px;
  border-bottom: 1px solid var(--color-border);
}

.item-row:last-child {
  border-bottom: none;
}

.arrow {
  color: var(--color-text-secondary);
}

.empty-tip {
  padding: 24px;
  text-align: center;
  color: var(--color-text-secondary);
}

.wizard-footer {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.json-preview {
  background: var(--color-bg-container);
  padding: 16px;
  border-radius: 4px;
  overflow: auto;
  max-height: 300px;
  font-size: 12px;
  margin-bottom: 16px;
}
.delete-btn {
  color: var(--color-text-secondary);
  padding: 0 4px;
  height: auto;
}
.delete-btn:hover {
  color: #ff4d4f;
}
</style>