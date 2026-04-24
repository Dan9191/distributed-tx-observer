# distributed-tx-monitor — контекст для Claude Code

## Что это за проект

Сервис **post-mortem визуализации** распределённых транзакций в микросервисной архитектуре.

**Проблема:** разработчик видит ошибку в логах, хочет понять как именно прошла распределённая транзакция — через какие сервисы, какие шаги выполнились, где упало.

**Решение:** пользователь вводит `operationId`, сервис тянет логи из Loki по LogQL, накладывает их на заранее созданный шаблон-граф транзакции и визуализирует с цветовой индикацией по уровню лога.

**Ключевое ограничение:** сервис-наблюдатель **не хранит** результаты конкретных запусков транзакций — только шаблоны (топологию шагов). Loki является единственным источником данных о реальных запусках.

---

## Стек

- **Backend:** Java 21, Spring Boot 4.x, Spring Data JPA, Spring Web
- **БД:** PostgreSQL + Flyway (миграции)
- **Сборка:** Gradle монорепо, Kotlin DSL (`build.gradle.kts`)
- **Фронтенд:** React + TypeScript, React Flow (`@xyflow/react`) для канваса
- **Логи:** Loki (Grafana) + Grafana Alloy как сборщик
- **Формат логов:** JSON через `logstash-logback-encoder`

---

## Структура монорепо

```
distributed-tx-monitor/
├── CLAUDE.md
├── README.md
├── settings.gradle.kts
├── build.gradle.kts                   # root build (общие зависимости, java 21)
├── observer-service/                  # Backend: REST API + Loki client + PostgreSQL
│   ├── build.gradle.kts
│   └── src/main/java/com/example/observer/
│       ├── domain/model/              # TransactionDefinition, StepDefinition,
│       │                              # StepTemplate, StepEdge, LogLevel
│       ├── domain/port/               # StepRegistrationPort, TemplatePort, LogQueryPort
│       ├── application/service/       # StepService, TemplateService, VisualizationService
│       └── adapter/
│           ├── in/web/                # StepController, TemplateController, VisualizationController
│           ├── out/db/                # JPA репозитории
│           └── out/loki/              # LokiLogQueryAdapter, LokiProperties, LokiConfiguration
├── observer-frontend/                 # React + TypeScript
│   ├── package.json
│   └── src/
│       ├── pages/
│       │   ├── TransactionList.tsx
│       │   ├── TemplateEditor.tsx     # канвас-редактор шаблона
│       │   └── Visualizer.tsx         # визуализация по operationId  ← НЕ РЕАЛИЗОВАН
│       └── components/
│           └── StepNode.tsx           # кастомный нод React Flow с цветом по logLevel
└── test-stand/                        # локальный стенд
    ├── docker-compose.yml             # PostgreSQL + Loki + Alloy + demo-service
    └── demo-service/                  # Spring Boot 3.4, пишет JSON-логи через MDC
        ├── build.gradle.kts
        ├── Dockerfile
        └── src/main/java/com/example/demo/
            ├── config/                # ObserverProperties, StepRegistrar
            └── web/                   # SimulationController
```

---

## Основные понятия предметной области

**Transaction** — именованный бизнес-процесс (`transactionName: String`), пронизывающий несколько сервисов. Например: `CreateOrder`, `ProcessPayment`.

**Step** — атомарное действие в рамках транзакции. Поля: `stepName`, `transactionName`, `serviceName`. Регистрируется сервисами при старте через REST.

**StepTemplate** — хранимая топология: позиции шагов на канвасе (`x`, `y`) и рёбра между ними. Не хранит данные конкретных запусков.

**operationId** — UUID конкретного запуска транзакции. Пишется в каждую лог-строку через MDC. По нему Loki-адаптер находит все логи этого запуска.

**LogLevel** (для визуализации): `ERROR > WARN > INFO > NONE`. Для каждого шага берётся максимальный уровень среди всех его лог-строк с данным `operationId`.

---

## Схема БД (Flyway миграции)

```sql
-- V1__init.sql

CREATE TABLE transaction_definition (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE step_definition (
    id               BIGSERIAL PRIMARY KEY,
    transaction_name VARCHAR(255) NOT NULL,
    step_name        VARCHAR(255) NOT NULL,
    service_name     VARCHAR(255) NOT NULL,
    UNIQUE (transaction_name, step_name)
);

CREATE TABLE step_template (
    id      BIGSERIAL PRIMARY KEY,
    step_id BIGINT NOT NULL REFERENCES step_definition(id),
    pos_x   DOUBLE PRECISION NOT NULL DEFAULT 0,
    pos_y   DOUBLE PRECISION NOT NULL DEFAULT 0
);

CREATE TABLE step_edge (
    id           BIGSERIAL PRIMARY KEY,
    from_step_id BIGINT NOT NULL REFERENCES step_definition(id),
    to_step_id   BIGINT NOT NULL REFERENCES step_definition(id)
);
```

---

## REST API observer-service

### Регистрация шагов (вызывается микросервисами при старте)
```
POST /api/v1/steps/register
Content-Type: application/json

[
  { "transactionName": "CreateOrder", "stepName": "ValidateCart",  "serviceName": "order-service" },
  { "transactionName": "CreateOrder", "stepName": "ReserveStock",  "serviceName": "inventory-service" }
]
```
Логика: upsert по `(transactionName, stepName)`.

### Шаблоны транзакций
```
GET    /api/v1/transactions                        # список всех транзакций
GET    /api/v1/transactions/{name}/template        # шаблон (шаги + позиции + рёбра)
PUT    /api/v1/transactions/{name}/template        # сохранить позиции и рёбра
```

### Визуализация
```
GET /api/v1/visualize?operationId={uuid}&transactionName={name}
```
Ответ: список шагов с полем `logLevel: "info" | "warn" | "error" | "none"` и строками логов.

---

## Loki-интеграция

### Конфигурация (application.yml)
```yaml
loki:
  base-url: ${LOKI_BASE_URL:http://localhost:3100}
  stream-selector: '{job=~".+"}'   # LogQL-селектор потоков
  lookback-hours: 24               # глубина поиска логов
```

### LogQL-запрос
```logql
{job=~".+"} | json | operationId=`{operationId}` | transactionName=`{transactionName}`
```

### Loki HTTP API
```
GET {loki.base-url}/loki/api/v1/query_range
  ?query=<logql>
  &start=<unix-nano>
  &end=<unix-nano>
  &limit=5000
```

### Порт (интерфейс для абстракции)
```java
public interface LogQueryPort {
    // Возвращает все лог-записи operationId, сгруппированные по stepName.
    // Один HTTP-запрос к Loki — адаптер группирует результат.
    Map<String, List<LogEntry>> getLogsByStep(String operationId, String transactionName);

    record LogEntry(String timestamp, LogLevel level, String message) {}
}
```
Реализация: `LokiLogQueryAdapter`. В будущем можно заменить на другой бэкенд логов.

---

## Регистрация шагов в микросервисах

**Observer-library не используется.** Каждый микросервис сам регистрирует шаги:

1. В `application.yml` описываются транзакции и шаги сервиса:
```yaml
observer:
  service-url: ${OBSERVER_SERVICE_URL:http://localhost:8033}
  service-name: my-service
  transactions:
    - name: CreateOrder
      steps:
        - ValidateCart
        - ReserveStock
```

2. `StepRegistrar implements ApplicationListener<ApplicationReadyEvent>` — при старте POST-ит шаги в observer-service.

3. Логи обогащаются через MDC перед каждой записью:
```java
MDC.put("operationId", operationId);
MDC.put("transactionName", "CreateOrder");
MDC.put("stepName", "ValidateCart");
MDC.put("serviceName", "my-service");
// ... log.info(...) ...
MDC.clear();
```

4. `logstash-logback-encoder` сериализует MDC-поля в JSON как top-level поля.

---

## Формат лог-записи

```json
{
  "level": "ERROR",
  "message": "Payment failed: insufficient funds",
  "operationId": "a1b2c3d4-...",
  "transactionName": "CreateOrder",
  "stepName": "ChargePayment",
  "serviceName": "payment-service",
  "@timestamp": "2025-04-24T10:00:00.000Z"
}
```

---

## Фронтенд — ключевые детали

### Канвас-редактор (TemplateEditor.tsx) — реализован
- Используется `@xyflow/react` (React Flow v12).
- Каждый шаг — кастомный `StepNode`.
- Пользователь перетаскивает шаги, соединяет их рёбрами.
- Кнопка "Сохранить" → PUT `/api/v1/transactions/{name}/template`.

### Визуализатор (Visualizer.tsx) — **НЕ РЕАЛИЗОВАН**
- Форма ввода `operationId`.
- Загружает данные с `GET /api/v1/visualize`.
- Рендерит React Flow граф с цветами узлов:
  - `logLevel: "info"` → зелёный (`#639922`)
  - `logLevel: "warn"` → жёлтый (`#BA7517`)
  - `logLevel: "error"` → красный (`#A32D2D`)
  - `logLevel: "none"` → серый (`#888780`)
- Клик на узел → боковая панель с логами этого шага.

---

## test-stand / demo-service

Spring Boot 3.4, standalone Gradle проект в `test-stand/demo-service/`.

Эндпоинты симуляции (возвращают `operationId` для вставки в визуализатор):
```
POST /simulate/create-order?scenario=success|low-stock|payment-error
POST /simulate/process-payment?scenario=success|auth-failed
```

Транзакции demo-service: `CreateOrder` (ValidateCart, ReserveStock, ChargePayment, ConfirmOrder), `ProcessPayment` (ValidateCard, AuthorizePayment, CapturePayment).

docker-compose: demo-service запускается с Docker-метками `logging=true, app=demo-service` — Alloy подбирает логи и отправляет в Loki с `{job="demo-service"}`.

---

## Статус реализации

| # | Компонент | Статус |
|---|-----------|--------|
| 1 | observer-service: регистрация шагов + PostgreSQL + Flyway | ✅ |
| 2 | observer-service: CRUD шаблонов | ✅ |
| 3 | observer-frontend: редактор шаблона (React Flow) | ✅ |
| 4 | observer-service: LokiLogQueryAdapter + эндпоинт визуализации | ✅ |
| 5 | observer-frontend: страница визуализации Visualizer.tsx | ❌ |
| 6 | test-stand: demo-service с правильным форматом логов | ✅ |

---

## Что НЕ нужно делать

- Не хранить результаты конкретных запусков транзакций в БД.
- Не реализовывать real-time WebSocket — только запрос по требованию.
- Не использовать `@dnd-kit` для канваса — только React Flow (`@xyflow/react`).
- Не делать отдельную `observer-library` (Spring Boot Starter) — регистрация шагов встроена в каждый сервис напрямую.

---

## Связь с ВКР

Проект является практической частью ВКР по теме распределённых транзакций. Демонстрирует паттерн наблюдаемости (observability) для Saga/распределённых транзакций через structured logging + визуализацию топологии.
