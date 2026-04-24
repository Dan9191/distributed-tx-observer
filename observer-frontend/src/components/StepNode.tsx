import { Handle, Position, type NodeProps } from '@xyflow/react'

/** Данные узла канваса. */
export interface StepNodeData extends Record<string, unknown> {
  stepId: number
  stepName: string
  serviceName: string
  /** Цвет рамки — задаётся страницей визуализации по уровню лога. */
  color?: string
}

/**
 * Кастомный узел React Flow для шага транзакции.
 * Цвет рамки управляется через {@link StepNodeData.color} (inline style).
 */
export default function StepNode({ data }: NodeProps) {
  const d = data as StepNodeData
  const borderColor = d.color ?? '#d1d5db'

  return (
    <div
      style={{ borderColor }}
      className="border-2 rounded-lg px-4 py-3 bg-white shadow-sm min-w-[160px] transition-colors"
    >
      <Handle type="target" position={Position.Left} className="!bg-gray-400" />
      <p className="font-medium text-gray-900 text-sm leading-tight">{d.stepName}</p>
      <p className="text-xs text-gray-400 mt-0.5">{d.serviceName}</p>
      <Handle type="source" position={Position.Right} className="!bg-gray-400" />
    </div>
  )
}
