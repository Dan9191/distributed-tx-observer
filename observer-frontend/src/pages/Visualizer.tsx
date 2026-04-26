import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import {
  ReactFlow,
  Background,
  Controls,
  useNodesState,
  useEdgesState,
  MarkerType,
  type Node,
  type Edge,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'

import StepNode, { type StepNodeData } from '../components/StepNode'
import GroupNode from '../components/GroupNode'
import StartNode from '../components/StartNode'
import EndNode from '../components/EndNode'
import CustomEdge from '../components/CustomEdge'
import {
  getTemplate, visualize,
  type VisualizationStep, type VisualizationResponse, type LogEntry, type GroupInstance,
} from '../api'
import { generateHtmlReport } from '../lib/reportGenerator'

const nodeTypes = { step: StepNode, group: GroupNode, start: StartNode, end: EndNode }
const edgeTypes = { custom: CustomEdge }

const LOG_LEVEL_COLOR: Record<string, string> = {
  info:  '#639922',
  warn:  '#BA7517',
  error: '#A32D2D',
  none:  '#9ca3af',
}

const LEVEL_BADGE: Record<string, string> = {
  info:  'bg-green-50 text-green-700 border border-green-200',
  warn:  'bg-yellow-50 text-yellow-700 border border-yellow-200',
  error: 'bg-red-50 text-red-700 border border-red-200',
  none:  'bg-gray-50 text-gray-500 border border-gray-200',
}

export default function Visualizer() {
  const { name } = useParams<{ name: string }>()
  const navigate = useNavigate()
  const txName = name ? decodeURIComponent(name) : ''

  const [nodes, setNodes, onNodesChange] = useNodesState<Node>([])
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([])

  const [operationId, setOperationId] = useState('')
  const [vizData, setVizData] = useState<VisualizationResponse | null>(null)
  const [templateGroups, setTemplateGroups] = useState<GroupInstance[]>([])
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null)

  const [templateLoading, setTemplateLoading] = useState(true)
  const [vizLoading, setVizLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  // ── Загрузка шаблона ─────────────────────────────────────────────────────

  useEffect(() => {
    if (!txName) return
    getTemplate(txName)
      .then(data => {
        const groupNodes = data.groups.map(groupToNode)
        const stepNodes = data.instances.map(inst => toNode(inst, undefined))
        setNodes([...groupNodes, ...stepNodes])
        setTemplateGroups(data.groups)
        const initEdges: Edge[] = data.edges.map(e => ({
          id: `${e.fromInstanceId}-${e.toInstanceId}`,
          type: 'custom',
          source: String(e.fromInstanceId),
          target: String(e.toInstanceId),
          markerEnd: { type: MarkerType.ArrowClosed },
          data: { edgeStyle: e.style ?? 'default' },
        }))
        onEdgesChange(initEdges.map(e => ({ type: 'add' as const, item: e })))
      })
      .catch(() => setError('Не удалось загрузить шаблон'))
      .finally(() => setTemplateLoading(false))
  }, [txName])

  // ── Визуализация ─────────────────────────────────────────────────────────

  const handleVisualize = async () => {
    if (!operationId.trim() || !txName) return
    setVizLoading(true)
    setError(null)
    setSelectedNodeId(null)
    try {
      const data = await visualize(operationId.trim(), txName)
      setVizData(data)

      // Определяем активные узлы (logLevel != none)
      const activeIds = new Set(
        data.steps
          .filter(s => s.logLevel !== 'none')
          .map(s => String(s.instanceId)),
      )

      // Раскрашиваем step-узлы
      setNodes(prev => prev.map(node => {
        const step = data.steps.find(s => String(s.instanceId) === node.id)
        if (!step) return node

        // Маркер start: активен если хотя бы один исходящий сосед активен
        // Маркер end:   активен если хотя бы один входящий сосед активен
        if (step.nodeType === 'start') {
          const hasActiveSuccessor = data.edges.some(
            e => String(e.fromInstanceId) === node.id && activeIds.has(String(e.toInstanceId)),
          )
          return {
            ...node,
            data: { ...node.data, color: hasActiveSuccessor ? LOG_LEVEL_COLOR.info : undefined },
          }
        }
        if (step.nodeType === 'end') {
          const hasActivePredecessor = data.edges.some(
            e => String(e.toInstanceId) === node.id && activeIds.has(String(e.fromInstanceId)),
          )
          return {
            ...node,
            data: { ...node.data, color: hasActivePredecessor ? LOG_LEVEL_COLOR.info : undefined },
          }
        }

        // Обычный step
        return {
          ...node,
          data: {
            ...node.data,
            color: LOG_LEVEL_COLOR[step.logLevel],
            logCount: step.logs.length,
          },
        }
      }))

      // Жирные рёбра — оба конца активны
      setEdges(prev => prev.map(edge => ({
        ...edge,
        data: {
          ...edge.data,
          active: activeIds.has(edge.source) && activeIds.has(edge.target),
        },
      })))
    } catch {
      setError('Не удалось получить данные визуализации')
    } finally {
      setVizLoading(false)
    }
  }

  const handleExport = () => {
    if (!vizData) return
    const html = generateHtmlReport(
      txName,
      vizData.operationId,
      vizData.steps,
      templateGroups,
      vizData.edges,
    )
    const blob = new Blob([html], { type: 'text/html;charset=utf-8' })
    const url  = URL.createObjectURL(blob)
    const a    = document.createElement('a')
    a.href     = url
    a.download = `${txName}-${vizData.operationId.slice(0, 8)}.html`
    a.click()
    URL.revokeObjectURL(url)
  }

  const onNodeClick = useCallback((_: React.MouseEvent, node: Node) => {
    // Клик только по step-узлам (маркеры и группы логов не имеют)
    if (node.type !== 'step') return
    setSelectedNodeId(prev => prev === node.id ? null : node.id)
  }, [])

  const selectedStep = vizData?.steps.find(s => String(s.instanceId) === selectedNodeId) ?? null

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
            <p className="text-xs text-gray-400">Визуализация</p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          {error && <p className="text-sm text-red-500">{error}</p>}
          <input
            value={operationId}
            onChange={e => setOperationId(e.target.value)}
            onKeyDown={e => e.key === 'Enter' && handleVisualize()}
            placeholder="operationId (UUID)"
            className="px-3 py-2 text-sm border border-gray-200 rounded-md outline-none
                       focus:border-blue-400 transition-colors w-72 font-mono"
          />
          <button
            onClick={handleVisualize}
            disabled={vizLoading || !operationId.trim()}
            className="px-5 py-2 bg-blue-600 text-white rounded-md text-sm
                       hover:bg-blue-700 disabled:opacity-50 transition-colors"
          >
            {vizLoading ? 'Загрузка...' : 'Визуализировать'}
          </button>
          {vizData && (
            <button
              onClick={handleExport}
              className="px-4 py-2 border border-gray-300 text-gray-600 rounded-md text-sm
                         hover:bg-gray-50 transition-colors"
              title="Выгрузить HTML-отчёт"
            >
              Экспорт HTML
            </button>
          )}
        </div>
      </div>

      {/* Основной layout */}
      <div className="flex flex-1 overflow-hidden">

        {/* Канвас */}
        <div className="flex-1 relative">
          {templateLoading && (
            <div className="absolute inset-0 flex items-center justify-center text-gray-400 z-10 bg-white/80">
              Загрузка шаблона...
            </div>
          )}
          {!templateLoading && nodes.length === 0 && (
            <div className="absolute inset-0 flex items-center justify-center text-gray-400 z-10 bg-white/80">
              <div className="text-center">
                <p>Шаблон не настроен</p>
                <button
                  onClick={() => navigate(`/transactions/${encodeURIComponent(txName)}/template`)}
                  className="text-blue-600 hover:underline text-sm mt-1"
                >
                  Открыть редактор →
                </button>
              </div>
            </div>
          )}
          <ReactFlow
            nodes={nodes}
            edges={edges}
            nodeTypes={nodeTypes}
            edgeTypes={edgeTypes}
            onNodesChange={onNodesChange}
            onEdgesChange={onEdgesChange}
            onNodeClick={onNodeClick}
            nodesDraggable={false}
            nodesConnectable={false}
            elementsSelectable={true}
            deleteKeyCode={null}
            fitView
          >
            <Background />
            <Controls showInteractive={false} />
          </ReactFlow>
        </div>

        {/* Боковая панель логов */}
        {selectedStep && (
          <LogPanel step={selectedStep} onClose={() => setSelectedNodeId(null)} />
        )}
      </div>
    </div>
  )
}

// ── Панель логов ──────────────────────────────────────────────────────────────

function LogPanel({ step, onClose }: { step: VisualizationStep; onClose: () => void }) {
  return (
    <div className="w-80 shrink-0 border-l border-gray-200 bg-white flex flex-col overflow-hidden">

      <div className="flex items-center gap-2 px-4 py-3 border-b border-gray-200 shrink-0">
        <div className="flex-1 min-w-0">
          <p className="font-medium text-sm text-gray-900 truncate">{step.stepName}</p>
          <p className="text-xs text-gray-400">{step.serviceName}</p>
        </div>
        <span className={`px-2 py-0.5 rounded-full text-xs font-medium uppercase ${LEVEL_BADGE[step.logLevel] ?? LEVEL_BADGE.none}`}>
          {step.logLevel}
        </span>
        <button
          onClick={onClose}
          className="text-gray-400 hover:text-gray-600 text-lg leading-none ml-1"
        >
          ×
        </button>
      </div>

      <div className="flex-1 overflow-y-auto">
        {step.logs.length === 0 ? (
          <p className="text-sm text-gray-400 text-center py-8">Логов не найдено</p>
        ) : (
          step.logs.map((entry, i) => (
            <LogEntryRow key={i} entry={entry} />
          ))
        )}
      </div>
    </div>
  )
}

function LogEntryRow({ entry }: { entry: LogEntry }) {
  const extraFields = Object.entries(entry.fields ?? {})

  return (
    <div className="px-4 py-3 border-b border-gray-100 text-sm">
      <div className="flex items-center gap-2 mb-1">
        <span className="text-xs text-gray-400 font-mono tabular-nums">{formatTime(entry.timestamp)}</span>
        <span className={`px-1.5 py-0.5 rounded text-xs font-medium uppercase ${LEVEL_BADGE[entry.level] ?? LEVEL_BADGE.none}`}>
          {entry.level}
        </span>
      </div>
      <p className="text-gray-700 leading-relaxed break-words">{entry.message}</p>
      {extraFields.length > 0 && (
        <div className="mt-2 rounded bg-gray-50 px-2 py-1.5 font-mono text-xs text-gray-500 space-y-0.5">
          {extraFields.map(([k, v]) => (
            <div key={k} className="flex gap-1 min-w-0">
              <span className="text-gray-400 shrink-0">{k}:</span>
              <span className="text-gray-600 break-all">{v}</span>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

// ── Вспомогательные функции ───────────────────────────────────────────────────

function toNode(
  inst: { instanceId: number; stepId: number | null; stepName: string; serviceName: string; x: number; y: number; nodeType: string },
  color: string | undefined,
): Node {
  if (inst.nodeType === 'start' || inst.nodeType === 'end') {
    return {
      id: String(inst.instanceId),
      type: inst.nodeType,
      position: { x: inst.x, y: inst.y },
      data: { color },
    }
  }
  return {
    id: String(inst.instanceId),
    type: 'step',
    position: { x: inst.x, y: inst.y },
    data: { stepId: inst.stepId ?? 0, stepName: inst.stepName, serviceName: inst.serviceName, color } satisfies StepNodeData,
  }
}

function groupToNode(g: GroupInstance): Node {
  return {
    id: `g-${g.groupId}`,
    type: 'group',
    position: { x: g.x, y: g.y },
    style: { width: g.width, height: g.height },
    zIndex: -1,
    data: { label: g.label, color: g.color },
  }
}

function formatTime(iso: string): string {
  try {
    return new Date(iso).toLocaleTimeString('ru-RU', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch {
    return iso
  }
}
