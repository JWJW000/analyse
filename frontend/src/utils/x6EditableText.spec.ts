import { describe, expect, it } from 'vitest'
import { getCellEditableText, setCellEditableText } from './x6EditableText'

describe('x6EditableText', () => {
  it('reads and writes node labels stored in label and attrs.label.text', () => {
    const node = {
      label: '原参与者',
      attrs: { label: { text: '原参与者', fill: '#333' } },
    }

    expect(getCellEditableText(node)).toBe('原参与者')

    setCellEditableText(node, '新参与者')

    expect(node.label).toBe('新参与者')
    expect(node.attrs.label.text).toBe('新参与者')
    expect(node.attrs.label.fill).toBe('#333')
  })

  it('reads and writes edge labels without dropping existing attrs', () => {
    const edge = {
      shape: 'edge',
      labels: [{ attrs: { label: { text: '使用', fill: '#666' } } }],
    }

    expect(getCellEditableText(edge)).toBe('使用')

    setCellEditableText(edge, '提交')

    expect(edge.labels[0].attrs.label.text).toBe('提交')
    expect(edge.labels[0].attrs.label.fill).toBe('#666')
  })

  it('creates an edge label when the edge has no labels yet', () => {
    const edge = { shape: 'edge' }

    expect(getCellEditableText(edge)).toBe('')

    setCellEditableText(edge, '关联')

    expect(edge.labels).toEqual([{ attrs: { label: { text: '关联' } } }])
  })
})
