import { Handle, Position, type NodeProps } from '@xyflow/react'

export interface StepNodeData extends Record<string, unknown> {
  stepId: number
  stepName: string
  serviceName: string
  /** Цвет рамки — задаётся страницей визуализации по уровню лога. */
  color?: string
  /** Количество лог-записей шага. Бейдж показывается если > 1. */
  logCount?: number
}

/**
 * Кастомный узел React Flow для шага транзакции.
 * Цвет рамки — по уровню лога. Бейдж — количество запусков (log-записей).
 */
export default function StepNode({ data }: NodeProps) {
  const d = data as StepNodeData
  const borderColor = d.color ?? '#d1d5db'
  const showBadge = (d.logCount ?? 0) > 1

  return (
    <div
      style={{ borderColor }}
      className="relative border-2 rounded-lg px-4 py-3 bg-white shadow-sm min-w-[160px] transition-colors"
    >
      <Handle type="target" position={Position.Left} className="!bg-gray-400" />

      {showBadge && (
        <div
          title={`${d.logCount} лог-записей`}
          style={{ backgroundColor: borderColor }}
          className="absolute -top-2.5 -right-2.5 min-w-[20px] h-5 px-1
                     flex items-center justify-center
                     rounded-full text-white text-xs font-bold leading-none select-none"
        >
          ×{d.logCount}
        </div>
      )}

      <p className="font-medium text-gray-900 text-sm leading-tight">{d.stepName}</p>
      <p className="text-xs text-gray-400 mt-0.5">{d.serviceName}</p>

      <Handle type="source" position={Position.Right} className="!bg-gray-400" />
    </div>
  )
}
