type LabelAttrs = {
  attrs?: {
    body?: {
      [key: string]: unknown
    }
    label?: {
      text?: string
      [key: string]: unknown
    }
    [key: string]: unknown
  }
  [key: string]: unknown
}

export type EditableX6Cell = {
  shape?: string
  label?: string
  attrs?: {
    label?: {
      text?: string
      [key: string]: unknown
    }
    [key: string]: unknown
  }
  labels?: LabelAttrs[]
  [key: string]: unknown
}

export function getCellEditableText(cell: EditableX6Cell): string {
  if (cell.shape === 'edge') {
    return String(cell.labels?.[0]?.attrs?.label?.text ?? cell.label ?? '')
  }
  return String(cell.attrs?.label?.text ?? cell.label ?? '')
}

export function setCellEditableText(cell: EditableX6Cell, text: string) {
  if (cell.shape === 'edge') {
    const firstLabel = cell.labels?.[0] ?? {}
    cell.labels = [
      {
        ...firstLabel,
        attrs: {
          ...firstLabel.attrs,
          label: {
            ...firstLabel.attrs?.label,
            text,
          },
        },
      },
      ...(cell.labels ?? []).slice(1),
    ]
    return
  }

  cell.label = text
  cell.attrs = {
    ...cell.attrs,
    label: {
      ...cell.attrs?.label,
      text,
    },
  }
}
