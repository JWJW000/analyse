import { ref } from 'vue'
import { Node, Edge } from '@antv/x6'

export type FlowNodeType = 'start' | 'end' | 'process' | 'decision' | 'input' | 'output'

export interface FlowNode {
  id: string
  type: FlowNodeType
  label: string
  description?: string
}

export interface FlowConnection {
  id: string
  source: string
  target: string
  label?: string
}

export interface FlowchartData {
  nodes: FlowNode[]
  connections: FlowConnection[]
}

export function useFlowchart() {
  const nodes = ref<Node[]>([])
  const edges = ref<Edge[]>([])

  const nodeShapes: Record<FlowNodeType, string> = {
    start: 'ellipse',
    end: 'ellipse',
    process: 'rect',
    decision: 'diamond',
    input: 'rect',
    output: 'rect',
  }

  const nodeColors: Record<FlowNodeType, { fill: string; stroke: string }> = {
    start: { fill: '#38a169', stroke: '#2f855a' },
    end: { fill: '#e53e3e', stroke: '#c53030' },
    process: { fill: '#f7fafc', stroke: '#1a365d' },
    decision: { fill: '#fefcbf', stroke: '#d69e2e' },
    input: { fill: '#e9d8fd', stroke: '#805ad5' },
    output: { fill: '#c6f6d5', stroke: '#38a169' },
  }

  function generateFromData(data: FlowchartData) {
    const newNodes: Node[] = []
    const newEdges: Edge[] = []

    const nodeWidth = 140
    const nodeHeight = 50
    const horizontalGap = 80
    const verticalGap = 80
    const startX = 100
    const startY = 50

    const levels = new Map<string, number>()
    function assignLevel(nodeId: string, level: number) {
      if (levels.has(nodeId) && levels.get(nodeId)! <= level) return
      levels.set(nodeId, level)
      const node = data.nodes.find(n => n.id === nodeId)
      if (node) {
        data.connections
          .filter(c => c.source === nodeId)
          .forEach(c => assignLevel(c.target, level + 1))
      }
    }

    if (data.nodes.length > 0) {
      const firstNode = data.nodes[0]
      if (firstNode) {
        assignLevel(firstNode.id, 0)
      }
    }

    const levelMap = new Map<number, FlowNode[]>()
    data.nodes.forEach(node => {
      const level = levels.get(node.id) || 0
      if (!levelMap.has(level)) levelMap.set(level, [])
      levelMap.get(level)!.push(node)
    })

    levelMap.forEach((levelNodes, level) => {
      levelNodes.forEach((node, index) => {
        const x = startX + level * (nodeWidth + horizontalGap)
        const y = startY + index * (nodeHeight + verticalGap)

        newNodes.push({
          id: node.id,
          shape: 'rect',
          position: { x, y },
          size: { width: nodeWidth, height: nodeHeight },
          attrs: {
            body: {
              fill: nodeColors[node.type].fill,
              stroke: nodeColors[node.type].stroke,
              rx: node.type === 'start' || node.type === 'end' ? 25 : 8,
              ry: node.type === 'start' || node.type === 'end' ? 25 : 8,
            },
            label: {
              text: node.label,
              fill: '#2d3748',
            },
          },
          data: { type: node.type, node },
        } as unknown as Node)
      })
    })

    data.connections.forEach(conn => {
      newEdges.push({
        id: conn.id,
        source: conn.source,
        target: conn.target,
        attrs: {
          line: {
            stroke: '#718096',
            strokeWidth: 1.5,
            targetMarker: {
              name: 'classic',
              size: 8,
            },
          },
          label: {
            text: conn.label || '',
            fill: '#718096',
            fontSize: 12,
          },
        },
        labels: conn.label ? [{ attrs: { label: { text: conn.label } } }] : undefined,
      } as unknown as Edge)
    })

    nodes.value = newNodes
    edges.value = newEdges

    return { nodes: newNodes, edges: newEdges }
  }

  function exportToJSON(): string {
    return JSON.stringify({
      nodes: nodes.value.map(n => n.toJSON()),
      edges: edges.value.map(e => e.toJSON()),
    }, null, 2)
  }

  return {
    nodes,
    edges,
    generateFromData,
    exportToJSON,
    nodeShapes,
    nodeColors,
  }
}