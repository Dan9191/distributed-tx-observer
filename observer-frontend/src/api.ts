import axios from 'axios'

const http = axios.create({ baseURL: '/api/v1' })

// ── Типы ────────────────────────────────────────────────────────────────────

/** Определение шага — элемент палитры. */
export interface StepDef {
  stepId: number
  stepName: string
  serviceName: string
}

/** Экземпляр шага на канвасе (один шаг может иметь несколько экземпляров). */
export interface StepInstance {
  instanceId: number
  stepId: number
  stepName: string
  serviceName: string
  x: number
  y: number
}

/** Направленное ребро между двумя экземплярами. */
export interface TemplateEdge {
  fromInstanceId: number
  toInstanceId: number
}

export interface TemplateResponse {
  transactionName: string
  /** Все зарегистрированные шаги (для палитры). */
  steps: StepDef[]
  /** Экземпляры шагов, размещённые на канвасе. */
  instances: StepInstance[]
  edges: TemplateEdge[]
}

export interface SaveTemplatePayload {
  instances: { nodeId: string; stepId: number; x: number; y: number }[]
  edges: { fromNodeId: string; toNodeId: string }[]
}

// ── Запросы ─────────────────────────────────────────────────────────────────

/** Список всех транзакций. */
export const getTransactions = (): Promise<string[]> =>
  http.get<string[]>('/transactions').then(r => r.data)

/** Шаблон транзакции: шаги (палитра), экземпляры на канвасе и рёбра. */
export const getTemplate = (name: string): Promise<TemplateResponse> =>
  http.get<TemplateResponse>(`/transactions/${encodeURIComponent(name)}/template`).then(r => r.data)

/** Сохранить шаблон транзакции. */
export const saveTemplate = (name: string, payload: SaveTemplatePayload): Promise<void> =>
  http.put(`/transactions/${encodeURIComponent(name)}/template`, payload).then(() => {})

// ── Визуализация ─────────────────────────────────────────────────────────────

export interface LogEntry {
  timestamp: string
  level: string
  message: string
}

export interface VisualizationStep {
  instanceId: number
  stepId: number
  stepName: string
  serviceName: string
  x: number | null
  y: number | null
  /** Максимальный уровень лога: "info" | "warn" | "error" | "none". */
  logLevel: string
  logs: LogEntry[]
}

export interface VisualizationResponse {
  transactionName: string
  operationId: string
  steps: VisualizationStep[]
  edges: TemplateEdge[]
}

/** Визуализация конкретного запуска транзакции по operationId. */
export const visualize = (operationId: string, transactionName: string): Promise<VisualizationResponse> =>
  http.get<VisualizationResponse>('/visualize', { params: { operationId, transactionName } }).then(r => r.data)
