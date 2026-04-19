/**
 * 生成 AntV X6 Graph JSON 初稿（与 RequirementEditor 中 UseCaseDiagram 的节点风格一致）。
 */
export function buildMinimalUseCaseGraphJson(title: string): string {
  const uc = (title.trim() || '核心用例').slice(0, 48)
  const data = {
    cells: [
      {
        shape: 'rect',
        id: 'draft-actor',
        x: 48,
        y: 100,
        width: 100,
        height: 40,
        label: '用户',
        attrs: {
          body: { stroke: '#5F95FF', fill: '#EFF4FF', rx: 6, ry: 6 },
          label: { fill: '#333' },
        },
      },
      {
        shape: 'ellipse',
        id: 'draft-uc',
        x: 240,
        y: 92,
        width: 168,
        height: 64,
        label: uc,
        attrs: {
          body: { stroke: '#73d13d', fill: '#f6ffed' },
          label: { fill: '#333' },
        },
      },
      {
        shape: 'edge',
        id: 'draft-edge',
        source: { cell: 'draft-actor' },
        target: { cell: 'draft-uc' },
        attrs: {
          line: { stroke: '#A2B1C3', strokeWidth: 2 },
        },
      },
    ],
  }
  return JSON.stringify(data)
}
