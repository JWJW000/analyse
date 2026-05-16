export type DiagramGenerationNode = {
  id: string
  label: string
  type: string
  description?: string
}

export type DiagramGenerationEdge = {
  id: string
  source: string
  target: string
  label?: string
}

export type DiagramGenerationDto = {
  diagramType: string
  nodes: DiagramGenerationNode[]
  edges: DiagramGenerationEdge[]
  explanation: string
  recommendations: string[]
}

export function diagramGenerationToX6Json(diagram: DiagramGenerationDto): string {
  const actors = diagram.nodes.filter((n) => n.type === 'actor')
  const useCases = diagram.nodes.filter((n) => n.type !== 'actor')
  const actorCount = Math.max(actors.length, 1)
  const useCaseCount = Math.max(useCases.length, 1)

  const actorCells = actors.map((node, index) => ({
    shape: 'rect',
    id: node.id,
    x: 48,
    y: 48 + index * Math.max(72, 320 / actorCount),
    width: 108,
    height: 42,
    label: node.label,
    attrs: {
      body: { stroke: '#5F95FF', fill: '#EFF4FF', rx: 8, ry: 8, magnet: true },
      label: { fill: '#1f2d3d', fontWeight: 600 },
    },
    data: { description: node.description },
  }))

  const useCaseCells = useCases.map((node, index) => ({
    shape: 'ellipse',
    id: node.id,
    x: 260,
    y: 36 + index * Math.max(78, 360 / useCaseCount),
    width: Math.max(150, Math.min(230, node.label.length * 16 + 56)),
    height: 64,
    label: node.label,
    attrs: {
      body: { stroke: '#73d13d', fill: '#f6ffed', magnet: true },
      label: { fill: '#244f1d', fontWeight: 600 },
    },
    data: { description: node.description },
  }))

  const edgeCells = diagram.edges.map((edge) => ({
    shape: 'edge',
    id: edge.id,
    source: { cell: edge.source },
    target: { cell: edge.target },
    labels: edge.label ? [{ attrs: { label: { text: edge.label } } }] : [],
    attrs: {
      line: { stroke: '#A2B1C3', strokeWidth: 2, targetMarker: 'classic' },
    },
  }))

  return JSON.stringify({
    cells: [...actorCells, ...useCaseCells, ...edgeCells],
  })
}
