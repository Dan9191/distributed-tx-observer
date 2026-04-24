import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ReactFlow,
  Background,
  Controls,
  addEdge,
  useNodesState,
  useEdgesState,
  type Node,
  type Edge,
  type Connection,
  type ReactFlowInstance,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import StepNode, { type StepNodeData } from '../components/StepNode'
import { getTemplate, saveTemplate, type TemplateStep } from '../api'

const nodeTypes = { step: StepNode }

/**
 * Редактор шаблона транзакции.
 * Слева — палитра незакреплённых шагов. Справа — канвас React Flow.
 */
export default function TemplateEditor() {
  const { name } = useParams<{ name: string }>()
  const navigate = useNavigate()
  const txName = name ? decodeURIComponent(name) : ''

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [palette, setPalette] = useState<TemplateStep[]>([])
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [saveOk, setSaveOk] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const rfInstance = useRef<ReactFlowInstance | null>(null)
  const canvasRef = useRef<HTMLDivElement | null>(null)

  // ── Загрузка шаблона ─────────────────────────────────────────────────────

  useEffect(() => {
    if (!txName) return
    getTemplate(txName)
      .then(data => {
        const positioned = data.steps.filter((s): s is TemplateStep & { x: number; y: number } => s.x !== null)
        setNodes(positioned.map(stepToNode))
        setPalette(data.steps.filter(s => s.x === null))
        setEdges(data.edges.map(e => ({
          id: `${e.fromStepId}-${e.toStepId}`,
          source: String(e.fromStepId),
          target: String(e.toStepId),
        })))
      })
      .catch(() => setError('Не удалось загрузить шаблон'))
      .finally(() => setLoading(false))
  }, [txName])

  // ── Соединение узлов ─────────────────────────────────────────────────────

  const onConnect = useCallback(
    (connection: Connection) => setEdges(eds => addEdge(connection, eds)),
    [setEdges],
  )

  // ── Drag & drop из палитры ───────────────────────────────────────────────

  const onDragStart = (e: React.DragEvent, step: TemplateStep) => {
    e.dataTransfer.setData('application/step', JSON.stringify(step))
    e.dataTransfer.effectAllowed = 'move'
  }

  const onDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'move'
  }

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      if (!rfInstance.current || !canvasRef.current) return
      const raw = e.dataTransfer.getData('application/step')
      if (!raw) return
      const step: TemplateStep = JSON.parse(raw)
      const bounds = canvasRef.current.getBoundingClientRect()
      const position = rfInstance.current.screenToFlowPosition({
        x: e.clientX - bounds.left,
        y: e.clientY - bounds.top,
      })
      setNodes(nds => [...nds, stepToNode({ ...step, x: position.x, y: position.y })])
      setPalette(prev => prev.filter(s => s.stepId !== step.stepId))
    },
    [setNodes],
  )

  // ── Удаление узла: возвращаем шаг в палитру ──────────────────────────────

  const onNodesDelete = useCallback((deleted: Node[]) => {
    const restored: TemplateStep[] = deleted.map(n => {
      const d = n.data as StepNodeData
      return { stepId: d.stepId, stepName: d.stepName, serviceName: d.serviceName, x: null, y: null }
    })
    setPalette(prev => [...prev, ...restored])
  }, [])

  // ── Сохранение ───────────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!txName) return
    setSaving(true)
    setError(null)
    try {
      await saveTemplate(txName, {
        steps: nodes.map(n => ({
          stepId: (n.data as StepNodeData).stepId,
          x: n.position.x,
          y: n.position.y,
        })),
        edges: edges.map(e => ({
          fromStepId: Number(e.source),
          toStepId: Number(e.target),
        })),
      })
      setSaveOk(true)
      setTimeout(() => setSaveOk(false), 2000)
    } catch {
      setError('Ошибка сохранения')
    } finally {
      setSaving(false)
    }
  }

  // ── Рендер ───────────────────────────────────────────────────────────────

  return (
    <div className="h-screen flex flex-col">

      {/* Заголовок */}
      <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200 bg-white shrink-0">
        <div className="flex items-center gap-4">
          <button
            onClick={() => navigate('/transactions')}
            className="text-sm text-gray-400 hover:text-gray-600 transition-colors"
          >
            ← Назад
          </button>
          <div>
            <h1 className="text-lg font-semibold text-gray-900">{txName}</h1>
            <p className="text-xs text-gray-400">Редактор шаблона</p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          {error  && <p className="text-sm text-red-500">{error}</p>}
          {saveOk && <p className="text-sm text-green-500">Сохранено ✓</p>}
          <button
            onClick={handleSave}
            disabled={saving}
            className="px-5 py-2 bg-blue-600 text-white rounded-md text-sm
                       hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {saving ? 'Сохранение...' : 'Сохранить'}
          </button>
        </div>
      </div>

      {/* Основной layout */}
      <div className="flex flex-1 overflow-hidden">

        {/* Палитра */}
        <div className="w-64 shrink-0 border-r border-gray-200 bg-gray-50 overflow-y-auto p-4">
          <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-3">
            Доступные шаги
          </p>
          {loading && (
            <p className="text-xs text-gray-400 text-center py-6">Загрузка...</p>
          )}
          {!loading && palette.length === 0 && nodes.length === 0 && (
            <p className="text-xs text-gray-400 text-center py-6">Нет зарегистрированных шагов</p>
          )}
          {!loading && palette.length === 0 && nodes.length > 0 && (
            <p className="text-xs text-gray-400 text-center py-6">Все шаги на канвасе</p>
          )}
          <div className="space-y-2">
            {palette.map(step => (
              <div
                key={step.stepId}
                draggable
                onDragStart={e => onDragStart(e, step)}
                className="bg-white border border-dashed border-gray-300 rounded-lg p-3
                           cursor-grab active:cursor-grabbing hover:border-blue-400
                           hover:shadow-sm transition-all select-none"
              >
                <p className="text-sm font-medium text-gray-800">{step.stepName}</p>
                <p className="text-xs text-gray-400 mt-0.5">{step.serviceName}</p>
              </div>
            ))}
          </div>
        </div>

        {/* Канвас */}
        <div ref={canvasRef} className="flex-1" onDragOver={onDragOver} onDrop={onDrop}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodesDelete={onNodesDelete}
            onInit={inst => { rfInstance.current = inst }}
            deleteKeyCode="Delete"
            fitView
          >
            <Background />
            <Controls />
          </ReactFlow>
        </div>
      </div>
    </div>
  )
}

function stepToNode(step: TemplateStep & { x: number; y: number }): Node {
  return {
    id: String(step.stepId),
    type: 'step',
    position: { x: step.x, y: step.y },
    data: { stepId: step.stepId, stepName: step.stepName, serviceName: step.serviceName },
  }
}
