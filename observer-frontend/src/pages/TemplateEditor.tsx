import { useCallback, useEffect, useRef, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ReactFlow,
  Background,
  Controls,
  addEdge,
  useNodesState,
  useEdgesState,
  MarkerType,
  type Node,
  type Edge,
  type Connection,
  type ReactFlowInstance,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import StepNode, { type StepNodeData } from '../components/StepNode'
import GroupNode, { type GroupNodeData } from '../components/GroupNode'
import StartNode from '../components/StartNode'
import EndNode from '../components/EndNode'
import CustomEdge from '../components/CustomEdge'
import {
  getTemplate, saveTemplate, deleteTransaction, deleteStep,
  type StepDef, type StepInstance, type GroupInstance,
} from '../api'
import { absolutePosition, insideGroup, attachToGroups } from '../lib/canvasUtils'

const nodeTypes = { step: StepNode, group: GroupNode, start: StartNode, end: EndNode }
const edgeTypes = { custom: CustomEdge }

const DEFAULT_GROUP_COLOR = '#6366f1'

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
  const [confirmDelete, setConfirmDelete] = useState(false)

  const rfInstance = useRef<ReactFlowInstance | null>(null)
  const canvasRef = useRef<HTMLDivElement | null>(null)

  // ── Загрузка шаблона ─────────────────────────────────────────────────────

  useEffect(() => {
    if (!txName) return
    getTemplate(txName)
      .then(data => {
        const groupNodes = data.groups.map(groupToNode)
        const stepNodes = attachToGroups(
          data.instances.map(inst => instanceToNode(inst)),
          groupNodes,
        )
        setNodes([...groupNodes, ...stepNodes])
        setPalette(data.steps)
        setEdges(data.edges.map(e => ({
          id: `${e.fromInstanceId}-${e.toInstanceId}`,
          type: 'custom',
          source: String(e.fromInstanceId),
          target: String(e.toInstanceId),
          markerEnd: { type: MarkerType.ArrowClosed },
          data: { edgeStyle: e.style ?? 'default' },
        })))
      })
      .catch(() => setError('Не удалось загрузить шаблон'))
      .finally(() => setLoading(false))
  }, [txName])

  // ── Соединение узлов ─────────────────────────────────────────────────────

  const onConnect = useCallback(
    (connection: Connection) => setEdges(eds => addEdge({
      ...connection,
      type: 'custom',
      markerEnd: { type: MarkerType.ArrowClosed },
      data: { edgeStyle: 'default' },
    }, eds)),
    [setEdges],
  )

  // ── Drag & drop из палитры ───────────────────────────────────────────────

  const onDragStart = (e: React.DragEvent, step: StepDef) => {
    e.dataTransfer.setData('application/step', JSON.stringify(step))
    e.dataTransfer.effectAllowed = 'copy'
  }

  const onDragStartMarker = (e: React.DragEvent, markerType: 'start' | 'end') => {
    e.dataTransfer.setData('application/marker', markerType)
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
      const bounds = canvasRef.current.getBoundingClientRect()
      const position = rfInstance.current.screenToFlowPosition({
        x: e.clientX - bounds.left,
        y: e.clientY - bounds.top,
      })

      const markerType = e.dataTransfer.getData('application/marker') as 'start' | 'end' | ''
      if (markerType === 'start' || markerType === 'end') {
        setNodes(nds => [...nds, {
          id: crypto.randomUUID(),
          type: markerType,
          position,
          data: {},
        }])
        return
      }

      const raw = e.dataTransfer.getData('application/step')
      if (!raw) return
      const step: StepDef = JSON.parse(raw)

      setNodes(nds => {
        const groupNodes = nds.filter(n => n.type === 'group')
        const parent = groupNodes.find(g => insideGroup(g, position))
        const newNode: Node = {
          id: crypto.randomUUID(),
          type: 'step',
          position: parent
            ? { x: position.x - parent.position.x, y: position.y - parent.position.y }
            : position,
          parentId: parent?.id,
          data: { stepId: step.stepId, stepName: step.stepName, serviceName: step.serviceName },
        }
        return [...nds, newNode]
      })
    },
    [setNodes],
  )

  // ── Авто-прикрепление при перетаскивании ─────────────────────────────────

  const onNodeDragStop = useCallback(
    (_: React.MouseEvent, draggedNode: Node, currentNodes: Node[]) => {
      if (draggedNode.type !== 'step') return

      const absPos = absolutePosition(draggedNode, currentNodes)

      const groups = currentNodes.filter(n => n.type === 'group')
      const targetGroup = groups.find(g => insideGroup(g, absPos))
      const newParentId = targetGroup?.id

      if (newParentId === (draggedNode.parentId ?? undefined)) return

      setNodes(nds =>
        nds.map(n => {
          if (n.id !== draggedNode.id) return n
          if (targetGroup) {
            return {
              ...n,
              parentId: targetGroup.id,
              position: {
                x: absPos.x - targetGroup.position.x,
                y: absPos.y - targetGroup.position.y,
              },
            }
          }
          return { ...n, parentId: undefined, position: absPos }
        }),
      )
    },
    [setNodes],
  )

  // ── Добавить группу ──────────────────────────────────────────────────────

  const addGroup = useCallback(() => {
    const canvas = canvasRef.current?.getBoundingClientRect()
    let position = { x: 100, y: 100 }
    if (rfInstance.current && canvas) {
      position = rfInstance.current.screenToFlowPosition({
        x: canvas.left + canvas.width / 2 - 100,
        y: canvas.top + canvas.height / 2 - 75,
      })
    }
    setNodes(nds => [...nds, {
      id: crypto.randomUUID(),
      type: 'group',
      position,
      style: { width: 200, height: 150 },
      zIndex: -1,
      data: { label: 'Новая группа', color: DEFAULT_GROUP_COLOR } satisfies GroupNodeData,
    }])
  }, [setNodes])

  // ── Удалить шаг из палитры ───────────────────────────────────────────────

  const handleDeleteStep = async (step: StepDef) => {
    try {
      await deleteStep(step.stepId)
      setPalette(prev => prev.filter(s => s.stepId !== step.stepId))
      // Убираем с канваса все экземпляры удалённого шага
      setNodes(nds => nds.filter(
        n => !(n.type === 'step' && (n.data as StepNodeData).stepId === step.stepId),
      ))
    } catch {
      setError('Не удалось удалить шаг')
    }
  }

  // ── Удалить шаблон (транзакцию) ──────────────────────────────────────────

  const handleDeleteTransaction = async () => {
    try {
      await deleteTransaction(txName)
      navigate('/transactions')
    } catch {
      setError('Не удалось удалить шаблон')
      setConfirmDelete(false)
    }
  }

  // ── Сохранение ───────────────────────────────────────────────────────────

  const handleSave = async () => {
    if (!txName) return
    setSaving(true)
    setError(null)
    try {
      const stepNodes  = nodes.filter(n => n.type === 'step')
      const markerNodes = nodes.filter(n => n.type === 'start' || n.type === 'end')
      const groupNodes = nodes.filter(n => n.type === 'group')

      await saveTemplate(txName, {
        instances: [
          ...stepNodes.map(n => {
            const pos = absolutePosition(n, nodes)
            return {
              nodeId: n.id,
              stepId: (n.data as StepNodeData).stepId,
              x: pos.x,
              y: pos.y,
              nodeType: 'step',
            }
          }),
          ...markerNodes.map(n => ({
            nodeId: n.id,
            stepId: null,
            x: n.position.x,
            y: n.position.y,
            nodeType: n.type as 'start' | 'end',
          })),
        ],
        groups: groupNodes.map(n => ({
          nodeId: n.id,
          label: (n.data as GroupNodeData).label,
          color: (n.data as GroupNodeData).color,
          x: n.position.x,
          y: n.position.y,
          width: (n.measured?.width ?? n.width ?? 200) as number,
          height: (n.measured?.height ?? n.height ?? 150) as number,
        })),
        edges: edges.map(e => ({
          fromNodeId: e.source,
          toNodeId: e.target,
          style: (e.data as { edgeStyle?: string })?.edgeStyle ?? 'default',
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
          {confirmDelete ? (
            <>
              <p className="text-sm text-red-500">Удалить транзакцию?</p>
              <button
                onClick={handleDeleteTransaction}
                className="px-4 py-2 bg-red-600 text-white rounded-md text-sm hover:bg-red-700 transition-colors"
              >
                Да, удалить
              </button>
              <button
                onClick={() => setConfirmDelete(false)}
                className="px-4 py-2 border border-gray-300 text-gray-600 rounded-md text-sm hover:bg-gray-50 transition-colors"
              >
                Отмена
              </button>
            </>
          ) : (
            <button
              onClick={() => setConfirmDelete(true)}
              className="px-4 py-2 border border-red-300 text-red-500 rounded-md text-sm hover:bg-red-50 transition-colors"
            >
              Удалить
            </button>
          )}
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

      <div className="flex flex-1 overflow-hidden">

        {/* Палитра */}
        <div className="w-64 shrink-0 border-r border-gray-200 bg-gray-50 overflow-y-auto p-4 flex flex-col gap-4">

          <div>
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Маркеры</p>
            <div className="space-y-2">
              <div
                draggable
                onDragStart={e => onDragStartMarker(e, 'start')}
                className="bg-white border border-dashed border-gray-300 rounded-lg p-3
                           cursor-grab active:cursor-grabbing hover:border-blue-400
                           hover:shadow-sm transition-all select-none text-center"
              >
                <p className="text-xs font-bold tracking-widest uppercase text-gray-500">СТАРТ</p>
              </div>
              <div
                draggable
                onDragStart={e => onDragStartMarker(e, 'end')}
                className="bg-white border border-dashed border-gray-300 rounded-lg p-3
                           cursor-grab active:cursor-grabbing hover:border-blue-400
                           hover:shadow-sm transition-all select-none text-center"
              >
                <p className="text-xs font-bold tracking-widest uppercase text-gray-500">СТОП</p>
              </div>
            </div>
          </div>

          <div>
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Группы</p>
            <button
              onClick={addGroup}
              className="w-full text-left bg-white border border-dashed border-indigo-300 rounded-lg p-3
                         hover:border-indigo-500 hover:shadow-sm transition-all text-sm text-indigo-600 font-medium"
            >
              + Добавить группу
            </button>
          </div>

          <div className="flex-1">
            <p className="text-xs font-medium text-gray-500 uppercase tracking-wide mb-2">Шаги</p>
            {loading && <p className="text-xs text-gray-400 text-center py-6">Загрузка...</p>}
            {!loading && palette.length === 0 && (
              <p className="text-xs text-gray-400 text-center py-6">Нет зарегистрированных шагов</p>
            )}
            <div className="space-y-2">
              {palette.map(step => (
                <div
                  key={step.stepId}
                  draggable
                  onDragStart={e => onDragStart(e, step)}
                  className="group relative bg-white border border-dashed border-gray-300 rounded-lg p-3
                             cursor-grab active:cursor-grabbing hover:border-blue-400
                             hover:shadow-sm transition-all select-none"
                >
                  <p className="text-sm font-medium text-gray-800 pr-5">{step.stepName}</p>
                  <p className="text-xs text-gray-400 mt-0.5">{step.serviceName}</p>
                  <button
                    onMouseDown={e => e.stopPropagation()}
                    onClick={e => { e.stopPropagation(); handleDeleteStep(step) }}
                    title="Удалить шаг"
                    className="absolute top-2 right-2 opacity-0 group-hover:opacity-100
                               text-gray-300 hover:text-red-500 transition-all text-base leading-none"
                  >
                    ×
                  </button>
                </div>
              ))}
            </div>
          </div>
        </div>

        {/* Канвас */}
        <div ref={canvasRef} className="flex-1" onDragOver={onDragOver} onDrop={onDrop}>
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onConnect={onConnect}
            onNodeDragStop={onNodeDragStop}
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
  if (inst.nodeType === 'start' || inst.nodeType === 'end') {
    return {
      id: String(inst.instanceId),
      type: inst.nodeType,
      position: { x: inst.x, y: inst.y },
      data: {},
    }
  }
  return {
    id: String(inst.instanceId),
    type: 'step',
    position: { x: inst.x, y: inst.y },
    data: { stepId: inst.stepId, stepName: inst.stepName, serviceName: inst.serviceName },
  }
}

function groupToNode(g: GroupInstance): Node {
  return {
    id: `g-${g.groupId}`,
    type: 'group',
    position: { x: g.x, y: g.y },
    style: { width: g.width, height: g.height },
    zIndex: -1,
    data: { label: g.label, color: g.color } satisfies GroupNodeData,
  }
}
