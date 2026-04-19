<script setup lang="ts">
import { ref, computed } from 'vue'
import { Card, Button, Form, FormItem, Input, Select, SelectOption, Space, Divider, Tag, Modal } from 'ant-design-vue'
import { useUseCaseDiagram, type UseCaseActor, type UseCase, type UseCaseDiagramData } from '@/composables/useUseCaseDiagram'

const currentStep = ref(0)
const diagramData = ref<UseCaseDiagramData>({
  actors: [],
  useCases: [],
  systemBoundary: {
    name: 'System',
    width: 400,
    height: 300,
  },
})

const newActor = ref<Partial<UseCaseActor>>({ name: '', type: 'primary' })
const newUseCase = ref<Partial<UseCase>>({ name: '', actors: [], description: '' })

const { generateFromData, exportToJSON } = useUseCaseDiagram()

function nextStep() { currentStep.value++ }
function prevStep() { currentStep.value-- }

function addActor() {
  if (!newActor.value.name) return
  const actor: UseCaseActor = {
    id: `actor-${Date.now()}`,
    name: newActor.value.name,
    type: newActor.value.type || 'primary',
  }
  diagramData.value.actors.push(actor)
  newActor.value = { name: '', type: 'primary' }
}

function removeActor(id: string) {
  diagramData.value.actors = diagramData.value.actors.filter(a => a.id !== id)
  diagramData.value.useCases.forEach(uc => {
    uc.actors = uc.actors.filter(aId => aId !== id)
  })
}

function addUseCase() {
  if (!newUseCase.value.name) return
  const useCase: UseCase = {
    id: `usecase-${Date.now()}`,
    name: newUseCase.value.name,
    description: newUseCase.value.description,
    actors: [],
  }
  diagramData.value.useCases.push(useCase)
  newUseCase.value = { name: '', actors: [], description: '' }
}

function removeUseCase(id: string) {
  diagramData.value.useCases = diagramData.value.useCases.filter(uc => uc.id !== id)
}

function toggleActorForUseCase(useCaseId: string, actorId: string) {
  const useCase = diagramData.value.useCases.find(uc => uc.id === useCaseId)
  if (!useCase) return
  const index = useCase.actors.indexOf(actorId)
  if (index === -1) {
    useCase.actors.push(actorId)
  } else {
    useCase.actors.splice(index, 1)
  }
}

function isActorSelected(useCaseId: string, actorId: string): boolean {
  const useCase = diagramData.value.useCases.find(uc => uc.id === useCaseId)
  return useCase ? useCase.actors.includes(actorId) : false
}

function generate() {
  generateFromData(diagramData.value)
  Modal.success({
    title: '生成成功',
    content: '用例图数据已生成，可以在控制台查看输出',
  })
}

const generatedJson = computed(() => exportToJSON())

const stepTitles = ['参与者', '用例', '关联', '生成']
</script>

<template>
  <Card title="用例图生成向导" class="wizard-card">
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
        <h4>添加参与者</h4>
        <Form layout="inline" style="margin-bottom: 16px">
          <FormItem label="名称">
            <Input v-model:value="newActor.name" placeholder="参与者名称" style="width: 150px" />
          </FormItem>
          <FormItem label="类型">
            <Select v-model:value="newActor.type" style="width: 120px">
              <SelectOption value="primary">主要参与者</SelectOption>
              <SelectOption value="secondary">次要参与者</SelectOption>
              <SelectOption value="system">系统参与者</SelectOption>
            </Select>
          </FormItem>
          <FormItem>
            <Button type="primary" @click="addActor">添加</Button>
          </FormItem>
        </Form>
        <div class="item-list">
          <div v-for="actor in diagramData.actors" :key="actor.id" class="item-row">
            <span>{{ actor.name }}</span>
            <Tag :color="actor.type === 'primary' ? 'blue' : actor.type === 'system' ? 'purple' : 'default'">
              {{ actor.type === 'primary' ? '主要' : actor.type === 'system' ? '系统' : '次要' }}
            </Tag>
            <Button type="text" size="small" class="delete-btn" @click="removeActor(actor.id)">删除</Button>
          </div>
          <div v-if="diagramData.actors.length === 0" class="empty-tip">暂无参与者，请添加</div>
        </div>
      </div>

      <div v-if="currentStep === 1" class="step-content">
        <h4>添加用例</h4>
        <Form layout="inline" style="margin-bottom: 16px">
          <FormItem label="名称">
            <Input v-model:value="newUseCase.name" placeholder="用例名称" style="width: 150px" />
          </FormItem>
          <FormItem label="描述">
            <Input v-model:value="newUseCase.description" placeholder="用例描述" style="width: 200px" />
          </FormItem>
          <FormItem>
            <Button type="primary" @click="addUseCase">添加</Button>
          </FormItem>
        </Form>
        <div class="item-list">
          <div v-for="uc in diagramData.useCases" :key="uc.id" class="item-row">
            <span>{{ uc.name }}</span>
            <span class="text-gray">{{ uc.description }}</span>
            <Button type="text" size="small" class="delete-btn" @click="removeUseCase(uc.id)">删除</Button>
          </div>
          <div v-if="diagramData.useCases.length === 0" class="empty-tip">暂无用例，请添加</div>
        </div>
      </div>

      <div v-if="currentStep === 2" class="step-content">
        <h4>建立关联</h4>
        <p style="margin-bottom: 16px">选择每个用例关联的参与者：</p>
        <div v-for="uc in diagramData.useCases" :key="uc.id" style="margin-bottom: 16px">
          <strong>{{ uc.name }}</strong>
          <Space style="margin-top: 8px">
            <Tag
              v-for="actor in diagramData.actors"
              :key="actor.id"
              :color="isActorSelected(uc.id, actor.id) ? 'blue' : 'default'"
              :checkable="true"
              :checked="isActorSelected(uc.id, actor.id)"
              @change="() => toggleActorForUseCase(uc.id, actor.id)"
            >
              {{ actor.name }}
            </Tag>
          </Space>
        </div>
        <div v-if="diagramData.useCases.length === 0 || diagramData.actors.length === 0" class="empty-tip">
          请先添加参与者和用例
        </div>
      </div>

      <div v-if="currentStep === 3" class="step-content">
        <h4>预览结果</h4>
        <Divider>生成的图形数据</Divider>
        <pre class="json-preview">{{ generatedJson }}</pre>
        <Button type="primary" @click="generate">生成用例图</Button>
      </div>
    </div>

    <div class="wizard-footer">
      <Space>
        <Button v-if="currentStep > 0" @click="prevStep">上一步</Button>
        <Button v-if="currentStep < 3" type="primary" @click="nextStep">下一步</Button>
        <Button v-if="currentStep === 3" type="primary" @click="generate">生成用例图</Button>
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

.text-gray {
  color: var(--color-text-secondary);
  font-size: 12px;
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
