import { useRef, useState } from 'react'
import { NodeResizer, useReactFlow, type NodeProps } from '@xyflow/react'

export interface GroupNodeData extends Record<string, unknown> {
  label: string
  color: string
}

const PRESET_COLORS = [
  '#6366f1', '#3b82f6', '#10b981', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#64748b',
]

/**
 * Кастомный узел React Flow — визуальная группа с изменяемым размером.
 * Двойной клик на заголовке — редактирование названия.
 * При выборе показывается палитра цветов и маркеры ресайза.
 */
export default function GroupNode({ id, data, selected }: NodeProps) {
  const d = data as GroupNodeData
  const { updateNodeData } = useReactFlow()
  const [editing, setEditing] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  const bg = hexToRgba(d.color, 0.07)

  return (
    <div
      style={{ backgroundColor: bg, borderColor: d.color, width: '100%', height: '100%' }}
      className="border-2 rounded-lg relative overflow-visible select-none"
    >
      <NodeResizer
        isVisible={selected}
        minWidth={120}
        minHeight={60}
        lineStyle={{ borderColor: d.color, borderWidth: 1.5 }}
        handleStyle={{ borderColor: d.color, backgroundColor: 'white', width: 10, height: 10 }}
      />

      {/* Заголовок */}
      <div className="px-3 pt-2 pb-1">
        {editing ? (
          <input
            ref={inputRef}
            autoFocus
            value={d.label}
            onChange={e => updateNodeData(id, { label: e.target.value })}
            onBlur={() => setEditing(false)}
            onKeyDown={e => { if (e.key === 'Enter' || e.key === 'Escape') setEditing(false) }}
            className="text-sm font-semibold bg-transparent outline-none border-b w-full"
            style={{ borderColor: d.color, color: d.color }}
          />
        ) : (
          <p
            className="text-sm font-semibold cursor-text"
            style={{ color: d.color }}
            onDoubleClick={() => setEditing(true)}
          >
            {d.label || 'Группа'}
          </p>
        )}
      </div>

      {/* Палитра цветов — показывается только при выборе */}
      {selected && (
        <div className="absolute bottom-2 right-2 flex gap-1 z-10">
          {PRESET_COLORS.map(c => (
            <button
              key={c}
              title={c}
              onClick={() => updateNodeData(id, { color: c })}
              className="w-4 h-4 rounded-full transition-transform hover:scale-125"
              style={{
                backgroundColor: c,
                outline: c === d.color ? `2px solid ${c}` : 'none',
                outlineOffset: '2px',
              }}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function hexToRgba(hex: string, alpha: number): string {
  const n = parseInt(hex.replace('#', ''), 16)
  return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alpha})`
}
