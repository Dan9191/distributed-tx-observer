import {
  BaseEdge,
  EdgeLabelRenderer,
  getBezierPath,
  getStraightPath,
  getSmoothStepPath,
  useReactFlow,
  type EdgeProps,
} from '@xyflow/react'

export interface CustomEdgeData extends Record<string, unknown> {
  edgeStyle?: string
  /** Устанавливается визуализатором: оба конца ребра активны. */
  active?: boolean
}

type StyleKey = 'default' | 'straight' | 'smoothstep' | 'dashed' | 'dotted' | 'dashed-step'

const STYLE_OPTIONS: { key: StyleKey; label: string; title: string }[] = [
  { key: 'default',     label: '⌒',    title: 'Bezier' },
  { key: 'straight',    label: '—',    title: 'Прямая' },
  { key: 'smoothstep',  label: '⌐',    title: 'Ступени' },
  { key: 'dashed',      label: '╌',    title: 'Пунктир' },
  { key: 'dotted',      label: '·····', title: 'Точки' },
  { key: 'dashed-step', label: '┐╌',   title: 'Пунктир с углами' },
]

const DASH: Record<StyleKey, string | undefined> = {
  default:     undefined,
  straight:    undefined,
  smoothstep:  undefined,
  dashed:      '7 4',
  dotted:      '2 5',
  'dashed-step': '7 4',
}

/**
 * Кастомное ребро React Flow с выбором стиля линии.
 * При выборе ребра показывается мини-тулбар в середине линии.
 * Если data.active === true — ребро рисуется жирным синим (визуализатор).
 */
export default function CustomEdge({
  id, selected, data,
  sourceX, sourceY, targetX, targetY,
  sourcePosition, targetPosition,
  markerEnd,
}: EdgeProps) {
  const { setEdges } = useReactFlow()
  const edgeStyle = ((data as CustomEdgeData)?.edgeStyle ?? 'default') as StyleKey
  const active    = !!(data as CustomEdgeData)?.active

  let edgePath = ''
  let labelX = 0
  let labelY = 0

  const isStep = edgeStyle === 'smoothstep' || edgeStyle === 'dashed-step'

  if (edgeStyle === 'straight') {
    ;[edgePath, labelX, labelY] = getStraightPath({ sourceX, sourceY, targetX, targetY })
  } else if (isStep) {
    ;[edgePath, labelX, labelY] = getSmoothStepPath({
      sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition,
    })
  } else {
    ;[edgePath, labelX, labelY] = getBezierPath({
      sourceX, sourceY, sourcePosition, targetX, targetY, targetPosition,
    })
  }

  const updateStyle = (newStyle: StyleKey) => {
    setEdges(eds =>
      eds.map(e => e.id === id ? { ...e, data: { ...e.data, edgeStyle: newStyle } } : e),
    )
  }

  const strokeColor  = active ? '#3b82f6' : selected ? '#3b82f6' : '#94a3b8'
  const strokeWidth  = active ? 3 : selected ? 2.5 : 1.5

  return (
    <>
      <BaseEdge
        path={edgePath}
        markerEnd={markerEnd}
        style={{
          strokeDasharray: DASH[edgeStyle],
          strokeWidth,
          stroke: strokeColor,
        }}
      />

      {selected && (
        <EdgeLabelRenderer>
          <div
            style={{ transform: `translate(-50%, -50%) translate(${labelX}px,${labelY}px)` }}
            className="absolute pointer-events-auto nodrag nopan z-50"
          >
            <div className="flex gap-0.5 bg-white border border-gray-200 rounded-lg shadow-lg p-1">
              {STYLE_OPTIONS.map(opt => (
                <button
                  key={opt.key}
                  title={opt.title}
                  onClick={() => updateStyle(opt.key)}
                  className={`px-2 py-1 text-xs rounded transition-colors font-mono ${
                    edgeStyle === opt.key
                      ? 'bg-blue-100 text-blue-700 font-bold'
                      : 'hover:bg-gray-100 text-gray-500'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>
        </EdgeLabelRenderer>
      )}
    </>
  )
}
