<script setup lang="ts">
import { watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import CodeBlockLowlight from '@tiptap/extension-code-block-lowlight'
import Image from '@tiptap/extension-image'
import { Table } from '@tiptap/extension-table'
import { TableRow } from '@tiptap/extension-table-row'
import { TableCell } from '@tiptap/extension-table-cell'
import { TableHeader } from '@tiptap/extension-table-header'
import Placeholder from '@tiptap/extension-placeholder'
import { createLowlight } from 'lowlight'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import python from 'highlight.js/lib/languages/python'
import java from 'highlight.js/lib/languages/java'

const lowlight = createLowlight()
lowlight.register('javascript', javascript)
lowlight.register('typescript', typescript)
lowlight.register('python', python)
lowlight.register('java', java)

const props = defineProps<{
  modelValue: string
  placeholder?: string
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const editor = useEditor({
  content: props.modelValue,
  extensions: [
    StarterKit.configure({
      codeBlock: false,
    }),
    CodeBlockLowlight.configure({
      lowlight,
    }),
    Image.configure({
      inline: true,
    }),
    Table.configure({
      resizable: true,
    }),
    TableRow,
    TableCell,
    TableHeader,
    Placeholder.configure({
      placeholder: props.placeholder || '开始输入内容...',
    }),
  ],
  onUpdate: ({ editor }) => {
    emit('update:modelValue', editor.getHTML())
  },
})

watch(() => props.modelValue, (value) => {
  if (editor.value && editor.value.getHTML() !== value) {
    editor.value.commands.setContent(value, { emitUpdate: false })
  }
})

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<template>
  <div class="rich-editor">
    <div v-if="editor" class="editor-toolbar">
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('bold') }"
        @click="editor.chain().focus().toggleBold().run()"
      >
        <strong>B</strong>
      </button>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('italic') }"
        @click="editor.chain().focus().toggleItalic().run()"
      >
        <em>I</em>
      </button>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('strike') }"
        @click="editor.chain().focus().toggleStrike().run()"
      >
        <s>S</s>
      </button>
      <span class="toolbar-divider"></span>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('heading', { level: 1 }) }"
        @click="editor.chain().focus().toggleHeading({ level: 1 }).run()"
      >
        H1
      </button>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('heading', { level: 2 }) }"
        @click="editor.chain().focus().toggleHeading({ level: 2 }).run()"
      >
        H2
      </button>
      <span class="toolbar-divider"></span>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('bulletList') }"
        @click="editor.chain().focus().toggleBulletList().run()"
      >
        •
      </button>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('orderedList') }"
        @click="editor.chain().focus().toggleOrderedList().run()"
      >
        1.
      </button>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('blockquote') }"
        @click="editor.chain().focus().toggleBlockquote().run()"
      >
        "
      </button>
      <button
        type="button"
        class="toolbar-btn"
        :class="{ active: editor.isActive('codeBlock') }"
        @click="editor.chain().focus().toggleCodeBlock().run()"
      >
        &lt;&gt;
      </button>
      <span class="toolbar-divider"></span>
      <button
        type="button"
        class="toolbar-btn"
        @click="editor.chain().focus().setHorizontalRule().run()"
      >
        —
      </button>
    </div>
    <EditorContent :editor="editor" class="editor-content" />
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.editor-toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  padding: 8px;
  background: var(--color-background);
  border-bottom: 1px solid var(--color-border);
}

.toolbar-btn {
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-sm);
  background: var(--color-bg-surface);
  cursor: pointer;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.toolbar-btn:hover {
  background: var(--color-background);
  border-color: var(--color-primary);
}

.toolbar-btn.active {
  background: var(--color-primary);
  color: white;
  border-color: var(--color-primary);
}

.toolbar-divider {
  width: 1px;
  height: 24px;
  background: var(--color-border);
  margin: 0 4px;
  align-self: center;
}

.editor-content {
  min-height: 200px;
  padding: 16px;
  background: var(--color-surface);
}

.editor-content :deep(.ProseMirror) {
  outline: none;
  min-height: 180px;
}

.editor-content :deep(.ProseMirror p.is-editor-empty:first-child::before) {
  content: attr(data-placeholder);
  float: left;
  color: var(--color-text-secondary);
  pointer-events: none;
  height: 0;
}

.editor-content :deep(pre) {
  background: var(--color-background);
  border-radius: var(--radius-md);
  padding: 12px;
  overflow-x: auto;
}

.editor-content :deep(code) {
  background: var(--color-background);
  padding: 2px 6px;
  border-radius: var(--radius-sm);
  font-family: var(--font-mono);
}
</style>
