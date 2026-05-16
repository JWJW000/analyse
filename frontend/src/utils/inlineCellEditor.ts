export type PointerLike = {
  clientX: number
  clientY: number
}

export type RectLike = {
  left: number
  top: number
  width: number
  height: number
}

export function getInlineEditorPosition(pointer: PointerLike, container: RectLike) {
  const editorWidth = 140
  const editorHeight = 36
  const padding = 4

  const left = Math.min(
    Math.max(pointer.clientX - container.left, padding),
    Math.max(container.width - editorWidth, padding),
  )
  const top = Math.min(
    Math.max(pointer.clientY - container.top, padding),
    Math.max(container.height - editorHeight, padding),
  )

  return { left, top }
}
