import { ref } from 'vue'

export interface UseCaseActor {
  id: string
  name: string
  type: 'primary' | 'secondary' | 'system'
}

export interface UseCase {
  id: string
  name: string
  description?: string
  actors: string[]
  extends?: string[]
  includes?: string[]
}

export interface UseCaseDiagramData {
  actors: UseCaseActor[]
  useCases: UseCase[]
  systemBoundary?: {
    name: string
    width: number
    height: number
  }
}

interface DiagramNode {
  id: string
  shape: string
  x: number
  y: number
  width: number
  height: number
  attrs?: Record<string, unknown>
  data?: Record<string, unknown>
  zIndex?: number
}

interface DiagramEdge {
  id: string
  source: string
  target: string
  attrs?: Record<string, unknown>
}

export function useUseCaseDiagram() {
  const nodes = ref<DiagramNode[]>([])
  const edges = ref<DiagramEdge[]>([])

  function generateFromData(data: UseCaseDiagramData) {
    const newNodes: DiagramNode[] = []
    const newEdges: DiagramEdge[] = []

    const actorWidth = 60
    const actorHeight = 80
    const useCaseWidth = 160
    const useCaseHeight = 60
    const systemPadding = 40

    const actorY = 100
    data.actors.filter(a => a.type === 'primary').forEach((actor, index) => {
      newNodes.push({
        id: actor.id,
        shape: 'image',
        x: 50,
        y: actorY + index * 120,
        width: actorWidth,
        height: actorHeight,
        attrs: {
          label: { text: actor.name },
        },
        data: { type: 'actor', actor },
      })
    })

    const systemActorY = 100
    data.actors.filter(a => a.type === 'system').forEach((actor, index) => {
      newNodes.push({
        id: actor.id,
        shape: 'image',
        x: 800,
        y: systemActorY + index * 120,
        width: actorWidth,
        height: actorHeight,
        attrs: {
          label: { text: actor.name },
        },
        data: { type: 'actor', actor },
      })
    })

    const useCaseStartX = 300
    const useCaseY = 80
    data.useCases.forEach((useCase, index) => {
      newNodes.push({
        id: useCase.id,
        shape: 'rect',
        x: useCaseStartX,
        y: useCaseY + index * 90,
        width: useCaseWidth,
        height: useCaseHeight,
        attrs: {
          body: {
            fill: '#f7fafc',
            stroke: '#1a365d',
            rx: 8,
            ry: 8,
          },
          label: { text: useCase.name },
        },
        data: { type: 'useCase', useCase },
      })

      useCase.actors.forEach(actorId => {
        const actorNode = newNodes.find(n => n.id === actorId)
        if (actorNode) {
          newEdges.push({
            id: `edge-${actorId}-${useCase.id}`,
            source: actorId,
            target: useCase.id,
            attrs: {
              line: {
                stroke: '#718096',
                strokeWidth: 1,
              },
            },
          })
        }
      })
    })

    if (data.systemBoundary) {
      const maxY = Math.max(
        ...newNodes.filter(n => n.data?.type === 'useCase').map(n => n.y + useCaseHeight),
        useCaseY + data.useCases.length * 90
      )

      newNodes.push({
        id: 'system-boundary',
        shape: 'rect',
        x: useCaseStartX - systemPadding,
        y: useCaseY - systemPadding,
        width: useCaseWidth + systemPadding * 2,
        height: maxY - useCaseY + systemPadding * 2,
        attrs: {
          body: {
            fill: 'transparent',
            stroke: '#1a365d',
            strokeWidth: 2,
            strokeDasharray: '5,5',
            rx: 4,
            ry: 4,
          },
          label: { text: data.systemBoundary.name },
        },
        zIndex: -1,
        data: { type: 'boundary' },
      })
    }

    nodes.value = newNodes
    edges.value = newEdges

    return { nodes: newNodes, edges: newEdges }
  }

  function exportToJSON(): string {
    return JSON.stringify({
      nodes: nodes.value,
      edges: edges.value,
    }, null, 2)
  }

  return {
    nodes,
    edges,
    generateFromData,
    exportToJSON,
  }
}
