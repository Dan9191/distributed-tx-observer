import { Handle, Position, type NodeProps } from '@xyflow/react'

export interface StartNodeData extends Record<string, unknown> {
  color?: string
}

export default function StartNode({ data }: NodeProps) {
  const d = data as StartNodeData
  const borderColor = d.color ?? '#9ca3af'
  const bgColor = d.color ? `${d.color}18` : '#f9fafb'

  return (
    <div
      style={{ borderColor, backgroundColor: bgColor }}
      className="border-2 rounded-full px-5 py-2 shadow-sm min-w-[80px] text-center transition-colors"
    >
      <Handle type="source" position={Position.Right} className="!bg-gray-400" />
      <Handle type="source" position={Position.Bottom} className="!bg-gray-400" />
      <span className="text-xs font-bold tracking-widest uppercase" style={{ color: borderColor }}>
        СТАРТ
      </span>
    </div>
  )
}
