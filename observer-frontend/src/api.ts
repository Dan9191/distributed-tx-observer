import axios from 'axios'

const http = axios.create({ baseURL: '/api/v1' })

// ── Типы ────────────────────────────────────────────────────────────────────

export interface TemplateStep {
  stepId: number
  stepName: string
  serviceName: string
  x: number | null
  y: number | null
}

export interface TemplateEdge {
  fromStepId: number
  toStepId: number
}

export interface TemplateResponse {
  transactionName: string
  steps: TemplateStep[]
  edges: TemplateEdge[]
}

export interface SaveTemplatePayload {
  steps: { stepId: number; x: number; y: number }[]
  edges: { fromStepId: number; toStepId: number }[]
}

// ── Запросы ─────────────────────────────────────────────────────────────────

/** Список всех транзакций. */
export const getTransactions = (): Promise<string[]> =>
  http.get<string[]>('/transactions').then(r => r.data)

/** Шаблон транзакции: шаги с позициями и рёбра. */
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
  stepId: number
  stepName: string
  serviceName: string
  x: number | null
  y: number | null
  /** Максимальный уровень лога шага: "info" | "warn" | "error" | "none". */
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
