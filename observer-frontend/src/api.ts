import axios from 'axios'

const http = axios.create({ baseURL: '/api/v1' })

// ── Типы ────────────────────────────────────────────────────────────────────

export interface StepDef {
  stepId: number
  stepName: string
  serviceName: string
}

export interface StepInstance {
  instanceId: number
  stepId: number | null
  stepName: string
  serviceName: string
  x: number
  y: number
  /** Тип узла: "step" | "start" | "end" */
  nodeType: string
}

export interface GroupInstance {
  groupId: number
  label: string
  color: string
  x: number
  y: number
  width: number
  height: number
}

export interface TemplateEdge {
  fromInstanceId: number
  toInstanceId: number
  style: string
}

export interface TemplateResponse {
  transactionName: string
  steps: StepDef[]
  instances: StepInstance[]
  groups: GroupInstance[]
  edges: TemplateEdge[]
}

export interface SaveTemplatePayload {
  instances: { nodeId: string; stepId: number | null; x: number; y: number; nodeType: string }[]
  groups: { nodeId: string; label: string; color: string; x: number; y: number; width: number; height: number }[]
  edges: { fromNodeId: string; toNodeId: string; style: string }[]
}

// ── Запросы ─────────────────────────────────────────────────────────────────

export const getTransactions = (): Promise<string[]> =>
  http.get<string[]>('/transactions').then(r => r.data)

export const getTemplate = (name: string): Promise<TemplateResponse> =>
  http.get<TemplateResponse>(`/transactions/${encodeURIComponent(name)}/template`).then(r => r.data)

export const saveTemplate = (name: string, payload: SaveTemplatePayload): Promise<void> =>
  http.put(`/transactions/${encodeURIComponent(name)}/template`, payload).then(() => {})

export const deleteTransaction = (name: string): Promise<void> =>
  http.delete(`/transactions/${encodeURIComponent(name)}`).then(() => {})

export const deleteStep = (stepId: number): Promise<void> =>
  http.delete(`/steps/${stepId}`).then(() => {})

// ── Визуализация ─────────────────────────────────────────────────────────────

export interface LogEntry {
  timestamp: string
  level: string
  message: string
}

export interface VisualizationStep {
  instanceId: number
  stepId: number | null
  stepName: string
  serviceName: string
  x: number | null
  y: number | null
  logLevel: string
  logs: LogEntry[]
  /** Тип узла: "step" | "start" | "end" */
  nodeType: string
}

export interface VisualizationResponse {
  transactionName: string
  operationId: string
  steps: VisualizationStep[]
  groups: GroupInstance[]
  edges: TemplateEdge[]
}

export const visualize = (operationId: string, transactionName: string): Promise<VisualizationResponse> =>
  http.get<VisualizationResponse>('/visualize', { params: { operationId, transactionName } }).then(r => r.data)
