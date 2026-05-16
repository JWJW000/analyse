import { describe, expect, it } from 'vitest'
import { getInlineEditorPosition } from './inlineCellEditor'

describe('inlineCellEditor', () => {
  it('places the editor at the pointer relative to the graph container', () => {
    const pos = getInlineEditorPosition(
      { clientX: 320, clientY: 240 },
      { left: 120, top: 90, width: 500, height: 300 },
    )

    expect(pos).toEqual({ left: 200, top: 150 })
  })

  it('keeps the editor inside the graph container bounds', () => {
    const pos = getInlineEditorPosition(
      { clientX: 650, clientY: 420 },
      { left: 120, top: 90, width: 500, height: 300 },
    )

    expect(pos.left).toBeLessThanOrEqual(360)
    expect(pos.top).toBeLessThanOrEqual(264)
  })
})
