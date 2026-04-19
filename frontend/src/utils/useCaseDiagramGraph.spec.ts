import { describe, expect, it } from 'vitest'
import { diagramGenerationToX6Json } from './useCaseDiagramGraph'

describe('diagramGenerationToX6Json', () => {
  it('converts generated actors, use cases, and relations to X6 graph JSON', () => {
    const json = diagramGenerationToX6Json({
      diagramType: 'usecase',
      nodes: [
        { id: 'actor-1', label: '学生', type: 'actor', description: '参与者' },
        { id: 'uc-1', label: '发布商品', type: 'usecase', description: '用例' },
      ],
      edges: [{ id: 'edge-1', source: 'actor-1', target: 'uc-1', label: '使用' }],
      explanation: '示例',
      recommendations: [],
    })

    const graph = JSON.parse(json) as { cells: Array<{ id: string; shape: string; label?: string }> }

    expect(graph.cells).toHaveLength(3)
    expect(graph.cells).toEqual(
      expect.arrayContaining([
        expect.objectContaining({ id: 'actor-1', shape: 'rect', label: '学生' }),
        expect.objectContaining({ id: 'uc-1', shape: 'ellipse', label: '发布商品' }),
        expect.objectContaining({ id: 'edge-1', shape: 'edge' }),
      ]),
    )
  })
})
