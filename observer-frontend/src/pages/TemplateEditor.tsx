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
import { getTemplate, saveTemplate, type StepDef, type StepInstance } from '../api'

const nodeTypes = { step: StepNode }

/**
 * Редактор шаблона транзакции.
 * Слева — палитра всех зарегистрированных шагов (шаг можно перетащить несколько раз).
 * Справа — канвас React Flow с экземплярами шагов.
 */
export default function TemplateEditor() {
  const { name } = useParams<{ name: string }>()
  const navigate = useNavigate()
  const txName = name ? decodeURIComponent(name) : ''

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])
  const [palette, setPalette] = useState<StepDef[]>([])
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
        setNodes(data.instances.map(instanceToNode))
        setPalette(data.steps)
        setEdges(data.edges.map(e => ({
          id: `${e.fromInstanceId}-${e.toInstanceId}`,
          source: String(e.fromInstanceId),
          target: String(e.toInstanceId),
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

  const onDragStart = (e: React.DragEvent, step: StepDef) => {
    e.dataTransfer.setData('application/step', JSON.stringify(step))
    e.dataTransfer.effectAllowed = 'copy'
  }

  const onDragOver = (e: React.DragEvent) => {
    e.preventDefault()
    e.dataTransfer.dropEffect = 'copy'
  }

  const onDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      if (!rfInstance.current || !canvasRef.current) return
      const raw = e.dataTransfer.getData('application/step')
      if (!raw) return
      const step: StepDef = JSON.parse(raw)
      const bounds = canvasRef.current.getBoundingClientRect()
      const position = rfInstance.current.screenToFlowPosition({
        x: e.clientX - bounds.left,
        y: e.clientY - bounds.top,
      })
      // Каждый drop создаёт новый экземпляр с уникальным ID
      setNodes(nds => [...nds, {
        id: crypto.randomUUID(),
        type: 'step',
        position,
        data: { stepId: step.stepId, stepName: step.stepName, serviceName: step.serviceName },
      }])
      // Палитра не изменяется — шаг всегда доступен для повторного drop
    },
    [setNodes],
  )

  // ── Сохранение ───────────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!txName) return
    setSaving(true)
    setError(null)
    try {
      await saveTemplate(txName, {
        instances: nodes.map(n => ({
          nodeId: n.id,
          stepId: (n.data as StepNodeData).stepId,
          x: n.position.x,
          y: n.position.y,
        })),
        edges: edges.map(e => ({
          fromNodeId: e.source,
          toNodeId: e.target,
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
            Шаги
          </p>
          {loading && (
            <p className="text-xs text-gray-400 text-center py-6">Загрузка...</p>
          )}
          {!loading && palette.length === 0 && (
            <p className="text-xs text-gray-400 text-center py-6">Нет зарегистрированных шагов</p>
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

function instanceToNode(inst: StepInstance): Node {
  return {
    id: String(inst.instanceId),
    type: 'step',
    position: { x: inst.x, y: inst.y },
    data: { stepId: inst.stepId, stepName: inst.stepName, serviceName: inst.serviceName },
  }
}
