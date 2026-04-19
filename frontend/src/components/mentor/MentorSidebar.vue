<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useMentorStore, type MentorSuggestion } from '@/stores/mentor'

interface Props {
  projectId: number
  projectName: string
  phase: string
  progress: number
}

const props = defineProps<Props>()
const mentorStore = useMentorStore()

const activeTab = ref<'suggestions' | 'chat'>('suggestions')
const userInput = ref('')

const contextInfo = computed(() => [
  { label: '项目', value: props.projectName },
  { label: '阶段', value: phaseLabel.value },
  { label: '进度', value: `${props.progress}%` }
])

const phaseLabel = computed(() => {
  const labels: Record<string, string> = {
    LITERATURE: '文献调研',
    REQUIREMENTS: '需求分析',
    ETHICS: '思政融合',
    SUBMISSION: '提交',
    REVIEW: '评审'
  }
  return labels[props.phase] || props.phase
})

const suggestionIcon = (type: string) => {
  const icons: Record<string, string> = {
    action: '💡',
    warning: '⚠️',
    tip: '💬'
  }
  return icons[type] || '💡'
}

const suggestionClass = (type: string) => {
  const classes: Record<string, string> = {
    action: 'suggestion-action',
    warning: 'suggestion-warning',
    tip: 'suggestion-tip'
  }
  return classes[type] || 'suggestion-tip'
}

async function handleSendMessage() {
  if (!userInput.value.trim()) return
  const message = userInput.value
  userInput.value = ''
  await mentorStore.sendMessage(props.projectId, message)
}

watch(() => mentorStore.isOpen, (isOpen) => {
  if (isOpen && props.projectId) {
    mentorStore.loadSuggestions(props.projectId, mentorStore.currentPage)
  }
})

if (mentorStore.isOpen && props.projectId) {
  mentorStore.loadSuggestions(props.projectId, mentorStore.currentPage)
}
</script>

<template>
  <Transition name="slide">
    <aside v-if="mentorStore.isOpen" class="mentor-sidebar">
      <div class="sidebar-header">
        <div class="header-title">
          <span class="mentor-avatar">🤖</span>
          <span class="mentor-name">AI 学习导师</span>
        </div>
        <button class="close-btn" @click="mentorStore.closeSidebar">×</button>
      </div>

      <div class="context-card">
        <div v-for="item in contextInfo" :key="item.label" class="context-item">
          <span class="context-label">{{ item.label }}</span>
          <span class="context-value">{{ item.value }}</span>
        </div>
      </div>

      <div class="tabs">
        <button
          :class="['tab', { active: activeTab === 'suggestions' }]"
          @click="activeTab = 'suggestions'"
        >
          💡 建议
        </button>
        <button
          :class="['tab', { active: activeTab === 'chat' }]"
          @click="activeTab = 'chat'"
        >
          💬 对话
        </button>
      </div>

      <div class="tab-content">
        <div v-if="activeTab === 'suggestions'" class="suggestions-panel">
          <div v-if="mentorStore.isLoading" class="loading">
            <span class="loading-spinner"></span>
            加载中...
          </div>
          <div v-else-if="mentorStore.suggestions.length === 0" class="empty-state">
            <span class="empty-icon">📭</span>
            <p>暂无建议</p>
            <p class="empty-hint">试试切换到对话模式提问</p>
          </div>
          <div v-else class="suggestions-list">
            <div
              v-for="(suggestion, index) in mentorStore.suggestions"
              :key="index"
              :class="['suggestion-item', suggestionClass(suggestion.type)]"
            >
              <div class="suggestion-header">
                <span class="suggestion-icon">{{ suggestionIcon(suggestion.type) }}</span>
                <span class="suggestion-type">
                  {{ suggestion.type === 'action' ? '操作建议' : suggestion.type === 'warning' ? '提醒' : '技巧' }}
                </span>
              </div>
              <p class="suggestion-content">{{ suggestion.content }}</p>
            </div>
          </div>
        </div>

        <div v-else class="chat-panel">
          <div class="chat-messages">
            <div v-if="mentorStore.chatMessages.length === 0" class="empty-state">
              <span class="empty-icon">💬</span>
              <p>开始和 AI 导师对话吧</p>
              <p class="empty-hint">询问任何关于项目的问题</p>
            </div>
            <div
              v-for="(msg, index) in mentorStore.chatMessages"
              :key="index"
              :class="['chat-message', msg.role]"
            >
              <div class="message-avatar">
                {{ msg.role === 'user' ? '👤' : '🤖' }}
              </div>
              <div class="message-content">
                <p>{{ msg.content }}</p>
              </div>
            </div>
          </div>

          <div class="chat-input">
            <input
              v-model="userInput"
              type="text"
              placeholder="输入您的问题..."
              :disabled="mentorStore.isLoading"
              @keyup.enter="handleSendMessage"
            />
            <button
              :disabled="!userInput.trim() || mentorStore.isLoading"
              @click="handleSendMessage"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </aside>
  </Transition>

  <button v-if="!mentorStore.isOpen" class="mentor-toggle" @click="mentorStore.openSidebar">
    <span class="toggle-icon">🤖</span>
    <span class="toggle-text">AI 导师</span>
  </button>
</template>

<style scoped>
.mentor-sidebar {
  position: fixed;
  top: 64px;
  right: 0;
  width: 360px;
  height: calc(100vh - 64px);
  background: #fff;
  border-left: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  z-index: 100;
  box-shadow: -4px 0 20px rgba(0, 0, 0, 0.05);
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #e5e7eb;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.header-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.mentor-avatar {
  font-size: 24px;
}

.mentor-name {
  font-weight: 600;
  font-size: 16px;
}

.close-btn {
  background: rgba(255, 255, 255, 0.2);
  border: none;
  color: white;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  cursor: pointer;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}

.close-btn:hover {
  background: rgba(255, 255, 255, 0.3);
}

.context-card {
  padding: 12px 16px;
  background: #f8fafc;
  border-bottom: 1px solid #e5e7eb;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
}

.context-item {
  display: flex;
  flex-direction: column;
  text-align: center;
}

.context-label {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 2px;
}

.context-value {
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
}

.tabs {
  display: flex;
  border-bottom: 1px solid #e5e7eb;
}

.tab {
  flex: 1;
  padding: 12px;
  background: none;
  border: none;
  cursor: pointer;
  font-size: 14px;
  color: #6b7280;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.tab.active {
  color: #667eea;
  border-bottom-color: #667eea;
  font-weight: 600;
}

.tab:hover:not(.active) {
  background: #f3f4f6;
}

.tab-content {
  flex: 1;
  overflow-y: auto;
}

.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 40px;
  color: #6b7280;
}

.loading-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #e5e7eb;
  border-top-color: #667eea;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-state {
  text-align: center;
  padding: 40px 20px;
  color: #6b7280;
}

.empty-icon {
  font-size: 48px;
  margin-bottom: 12px;
  display: block;
}

.empty-state p {
  margin: 4px 0;
}

.empty-hint {
  font-size: 12px;
  color: #9ca3af;
}

.suggestions-list {
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggestion-item {
  padding: 12px;
  border-radius: 10px;
  background: #f9fafb;
}

.suggestion-action {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.08) 0%, rgba(118, 75, 162, 0.08) 100%);
  border-left: 3px solid #667eea;
}

.suggestion-warning {
  background: linear-gradient(135deg, rgba(245, 158, 11, 0.08) 0%, rgba(234, 88, 12, 0.08) 100%);
  border-left: 3px solid #f59e0b;
}

.suggestion-tip {
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.08) 0%, rgba(5, 150, 105, 0.08) 100%);
  border-left: 3px solid #10b981;
}

.suggestion-header {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 6px;
}

.suggestion-icon {
  font-size: 14px;
}

.suggestion-type {
  font-size: 12px;
  font-weight: 600;
  color: #6b7280;
}

.suggestion-content {
  font-size: 13px;
  line-height: 1.5;
  color: #374151;
  margin: 0;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.chat-message {
  display: flex;
  gap: 8px;
  max-width: 85%;
}

.chat-message.user {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.chat-message.assistant {
  align-self: flex-start;
}

.message-avatar {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  flex-shrink: 0;
}

.chat-message.user .message-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.message-content {
  padding: 10px 14px;
  border-radius: 12px;
  background: #f3f4f6;
}

.chat-message.user .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.message-content p {
  margin: 0;
  font-size: 13px;
  line-height: 1.5;
  white-space: pre-wrap;
}

.chat-input {
  padding: 12px;
  border-top: 1px solid #e5e7eb;
  display: flex;
  gap: 8px;
}

.chat-input input {
  flex: 1;
  padding: 10px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 20px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.2s;
}

.chat-input input:focus {
  border-color: #667eea;
}

.chat-input button {
  padding: 10px 16px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  border-radius: 20px;
  font-size: 13px;
  cursor: pointer;
  transition: opacity 0.2s;
}

.chat-input button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-input button:not(:disabled):hover {
  opacity: 0.9;
}

.mentor-toggle {
  position: fixed;
  bottom: 80px;
  right: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border: none;
  padding: 12px 20px;
  border-radius: 24px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.4);
  transition: all 0.3s;
  z-index: 100;
}

.mentor-toggle:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 24px rgba(102, 126, 234, 0.5);
}

.toggle-icon {
  font-size: 18px;
}

.toggle-text {
  font-weight: 600;
  font-size: 14px;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%);
}
</style>
