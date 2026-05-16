import { mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { nextTick } from 'vue'
import UseCaseDiagram from '../UseCaseDiagram.vue'

type GraphHandler = (payload: Record<string, unknown>) => void

const graphInstances = vi.hoisted(() => [] as Array<{
  container: HTMLElement
  handlers: Record<string, GraphHandler>
  addEdgeMock: ReturnType<typeof vi.fn>
  removeCellMock: ReturnType<typeof vi.fn>
  dispose: () => void
  on: (event: string, handler: GraphHandler) => void
  toJSON: () => Record<string, unknown>
  fromJSON: (value: unknown) => void
  addNode: (value: unknown) => void
  addEdge: (value: unknown) => void
  removeCell: (value: unknown) => void
}>)

vi.mock('@antv/x6', () => {
  class Graph {
    container: HTMLElement
    handlers: Record<string, GraphHandler> = {}
    addEdgeMock = vi.fn()
    removeCellMock = vi.fn()

    constructor(options: { container: HTMLElement }) {
      this.container = options.container
      graphInstances.push(this)
    }

    on(event: string, handler: GraphHandler) {
      this.handlers[event] = handler
    }

    toJSON() {
      return { cells: [] }
    }

    fromJSON() {}

    addNode() {}

    addEdge(value: unknown) {
      this.addEdgeMock(value)
    }

    removeCell(value: unknown) {
      this.removeCellMock(value)
    }

    dispose() {}
  }

  return { Graph }
})

function mountDiagram() {
  return mount(UseCaseDiagram, {
    props: { modelValue: null },
    global: {
      stubs: {
        'a-space': { template: '<div><slot /></div>' },
        'a-button': { template: '<button @click="$emit(\'click\')"><slot /></button>' },
      },
    },
  })
}

describe('UseCaseDiagram', () => {
  beforeEach(() => {
    graphInstances.length = 0
  })

  it('keeps the inline editor outside the X6-managed graph container', () => {
    const wrapper = mountDiagram()
    const graphContainer = wrapper.find('.use-case-graph')

    expect(graphContainer.exists()).toBe(true)
    expect(graphInstances[0]?.container).toBe(graphContainer.element)
  })

  it('opens an inline input on node double click', async () => {
    const wrapper = mountDiagram()
    const graph = graphInstances[0]
    const editableCell = {
      toJSON: () => ({ label: '参与者' }),
      prop: vi.fn(),
    }

    graph?.handlers['node:dblclick']?.({
      node: editableCell,
      e: { clientX: 80, clientY: 120 },
    })
    await nextTick()

    const editor = wrapper.find('.inline-label-editor')
    expect(editor.exists()).toBe(true)
    expect((editor.element as HTMLInputElement).value).toBe('参与者')
  })

  it('opens an inline input on edge double click', async () => {
    const wrapper = mountDiagram()
    const graph = graphInstances[0]
    const editableCell = {
      toJSON: () => ({ shape: 'edge', labels: [{ attrs: { label: { text: '包含' } } }] }),
      prop: vi.fn(),
    }

    graph?.handlers['edge:dblclick']?.({
      edge: editableCell,
      e: { clientX: 160, clientY: 180 },
    })
    await nextTick()

    const editor = wrapper.find('.inline-label-editor')
    expect(editor.exists()).toBe(true)
    expect((editor.element as HTMLInputElement).value).toBe('包含')
  })

  it('adds an association edge after selecting two nodes in connection mode', async () => {
    const wrapper = mountDiagram()
    const graph = graphInstances[0]
    const source = { id: 'actor-1', toJSON: () => ({ id: 'actor-1' }) }
    const target = { id: 'usecase-1', toJSON: () => ({ id: 'usecase-1' }) }

    await wrapper.findAll('button')[2]?.trigger('click')
    graph?.handlers['node:click']?.({ node: source })
    graph?.handlers['node:click']?.({ node: target })

    expect(graph?.addEdgeMock).toHaveBeenCalledWith(expect.objectContaining({
      source: { cell: 'actor-1' },
      target: { cell: 'usecase-1' },
    }))
  })

  it('adds a labeled dashed edge for include relationships', async () => {
    const wrapper = mountDiagram()
    const graph = graphInstances[0]
    const source = { id: 'usecase-1', toJSON: () => ({ id: 'usecase-1' }) }
    const target = { id: 'usecase-2', toJSON: () => ({ id: 'usecase-2' }) }

    await wrapper.findAll('button')[3]?.trigger('click')
    graph?.handlers['node:click']?.({ node: source })
    graph?.handlers['node:click']?.({ node: target })

    expect(graph?.addEdgeMock).toHaveBeenCalledWith(expect.objectContaining({
      source: { cell: 'usecase-1' },
      target: { cell: 'usecase-2' },
      labels: [{ attrs: { label: { text: '<<include>>' } } }],
      attrs: expect.objectContaining({
        line: expect.objectContaining({ strokeDasharray: '5 5' }),
      }),
    }))
  })

  it('removes the selected cell from the toolbar delete action', async () => {
    const wrapper = mountDiagram()
    const graph = graphInstances[0]
    const cell = { id: 'actor-1', toJSON: () => ({ id: 'actor-1' }) }

    graph?.handlers['cell:click']?.({ cell })
    await wrapper.findAll('button')[5]?.trigger('click')

    expect(graph?.removeCellMock).toHaveBeenCalledWith(cell)
  })

  it('removes the selected cell with the Delete key', async () => {
    mountDiagram()
    const graph = graphInstances[0]
    const cell = { id: 'edge-1', toJSON: () => ({ id: 'edge-1', shape: 'edge' }) }

    graph?.handlers['cell:click']?.({ cell })
    window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Delete' }))

    expect(graph?.removeCellMock).toHaveBeenCalledWith(cell)
  })
})
