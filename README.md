# distributed-tx-monitor

Post-mortem визуализация распределённых транзакций. Разработчик вводит `operationId` — сервис тянет логи из Loki, накладывает на граф-шаблон транзакции и показывает где что упало с цветовой индикацией по уровню лога.

## Стек

| Компонент | Технологии |
|-----------|-----------|
| Backend | Java 21, Spring Boot, Spring Data JPA |
| БД | PostgreSQL + Flyway |
| Фронтенд | React + TypeScript, React Flow (`@xyflow/react`) |
| Логи | Loki + Grafana Alloy |
| Сборка | Gradle монорепо, Kotlin DSL |

## Запуск

### 1. Инфраструктура (PostgreSQL + Loki + Alloy + demo-service)

```bash
cd test-stand
docker-compose up -d --build
```

### 2. observer-service

```bash
./gradlew :observer-service:bootRun
```

Доступен на `http://localhost:8033`.

### 3. Фронтенд

```bash
cd observer-frontend
npm install
npm run dev
```

## Использование

**Шаг 1.** Запустить симуляцию транзакции:

```bash
# success / low-stock / payment-error
curl -X POST "http://localhost:8081/simulate/create-order?scenario=payment-error"
# → {"operationId":"<uuid>","transactionName":"CreateOrder"}

# success / auth-failed
curl -X POST "http://localhost:8081/simulate/process-payment?scenario=auth-failed"
```

**Шаг 2.** Открыть `http://localhost:5173`, выбрать транзакцию, настроить шаблон-граф.

**Шаг 3.** На странице Visualizer ввести `operationId` — узлы графа раскрасятся по уровню лога.

## REST API observer-service

```
POST /api/v1/steps/register                          # регистрация шагов микросервисом
GET  /api/v1/transactions                            # список транзакций
GET  /api/v1/transactions/{name}/template            # шаблон (шаги + позиции + рёбра)
PUT  /api/v1/transactions/{name}/template            # сохранить шаблон
GET  /api/v1/visualize?operationId=&transactionName= # визуализация по operationId
```

## Конфигурация сервиса для регистрации шагов

Каждый микросервис прописывает в `application.yml` транзакции и шаги, которые он выполняет. При старте (`ApplicationReadyEvent`) шаги POST-ятся в observer-service:

```yaml
observer:
  service-url: http://localhost:8033
  service-name: my-service
  transactions:
    - name: CreateOrder
      steps:
        - ValidateCart
        - ReserveStock
```

Логи обогащаются через MDC: `operationId`, `transactionName`, `stepName`, `serviceName`.

Запуск с нуля                                                                                                                                                                                                                        
Требования

- Docker Desktop (запущен)
- Java 21 JDK
- Node.js + npm
- Gradle (для demo-service)
- Git Bash (или WSL)

  ---
Первый запуск

# 1. Перейти в корень репо
cd D:/rep2/distributed-tx-monitor

# 2. Собрать всё и поднять контейнеры
./build.sh

Скрипт последовательно:
1. Собирает observer-service JAR через ./gradlew
2. Собирает demo-service JAR через gradle
3. Собирает фронтенд через npm ci && npm run build
4. Запускает docker compose up -d

Подождать ~1–2 минуты пока все healthcheck'и пройдут.

  ---
Если build.sh уже запускался (артефакты есть)

cd D:/rep2/distributed-tx-monitor/test-stand
docker compose up -d

  ---
Остановить

cd D:/rep2/distributed-tx-monitor/test-stand
docker compose down

Данные PostgreSQL и Loki сохраняются (named volumes). Чтобы удалить всё включая данные:
docker compose down -v

  ---
Проверка что всё поднялось

docker compose ps

Все сервисы должны быть healthy или running. Затем:

- Фронтенд: http://localhost:5173
- Observer API: http://localhost:8033/api/v1/transactions
- Demo-сервис: http://localhost:8081

### Примеры запросов

1. Успешный сценарий
```
curl -X POST "http://localhost:8081/simulate/create-order?scenario=success"
```
2. Нет товара (fail на inventory)
```
curl -X POST "http://localhost:8082/simulate/create-order?scenario=no-stock"
```
3. Ошибка оплаты + rollback
```
curl -X POST "http://localhost:8082/simulate/create-order?scenario=payment-fail"
```
4. Notification падает (но успех)
```
curl -X POST "http://localhost:8082/simulate/create-order?scenario=notify-fail"
```