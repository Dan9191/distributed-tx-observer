import { Handle, Position, type NodeProps } from '@xyflow/react'

export interface EndNodeData extends Record<string, unknown> {
  color?: string
}

export default function EndNode({ data }: NodeProps) {
  const d = data as EndNodeData
  const borderColor = d.color ?? '#9ca3af'
  const bgColor = d.color ? `${d.color}18` : '#f9fafb'

  return (
    <div
      style={{ borderColor, backgroundColor: bgColor }}
      className="border-2 rounded-full px-5 py-2 shadow-sm min-w-[80px] text-center transition-colors"
    >
      <Handle type="target" position={Position.Left} className="!bg-gray-400" />
      <Handle type="target" position={Position.Top} className="!bg-gray-400" />
      <span className="text-xs font-bold tracking-widest uppercase" style={{ color: borderColor }}>
        СТОП
      </span>
    </div>
  )
}
