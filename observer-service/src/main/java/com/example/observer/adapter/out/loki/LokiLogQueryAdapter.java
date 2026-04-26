package com.example.observer.adapter.out.loki;

import com.example.observer.domain.model.LogLevel;
import com.example.observer.domain.port.LogQueryPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Исходящий адаптер: запрашивает структурированные логи из Loki HTTP API.
 *
 * <p>Отправляет LogQL-запрос на {@code /loki/api/v1/query_range},
 * парсит raw JSON-строки лог-записей (формат logstash-logback-encoder)
 * и группирует результаты по значению поля {@code stepName}.</p>
 *
 * <p>При любой ошибке связи или парсинга возвращает пустую map,
 * не прерывая работу приложения.</p>
 */
@Component
@Slf4j
public class LokiLogQueryAdapter implements LogQueryPort {

    private final LokiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * @param properties настройки подключения к Loki
     */
    public LokiLogQueryAdapter(LokiProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>LogQL-запрос: {@code {streamSelector} | json | operationId=`{id}` | transactionName=`{tx}`}</p>
     * <p>Временное окно: от {@code now - lookbackHours} до {@code now}.</p>
     */
    @Override
    public Map<String, List<LogEntry>> getLogsByStep(String operationId, String transactionName) {
        String query = buildQuery(operationId, transactionName);
        Instant now = Instant.now();
        long endNs = toNanos(now);
        long startNs = toNanos(now.minus(properties.getLookbackHours(), ChronoUnit.HOURS));

        log.debug("Loki query: {}", query);

        try {
            // Передаём query через именованный плейсхолдер {q}, иначе UriBuilder
            // воспринимает фигурные скобки LogQL-селектора как URI-шаблон.
            String body = restClient.get()
                    .uri(u -> u.path("/loki/api/v1/query_range")
                            .queryParam("query", "{q}")
                            .queryParam("start", startNs)
                            .queryParam("end", endNs)
                            .queryParam("limit", 5000)
                            .build(query))
                    .retrieve()
                    .body(String.class);

            Map<String, List<LogEntry>> result = parseResponse(body);
            log.debug("Loki returned {} steps for operationId={}", result.size(), operationId);
            return result;
        } catch (Exception e) {
            log.warn("Loki query failed [operationId={}, tx={}]: {}", operationId, transactionName, e.getMessage());
            return Map.of();
        }
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Строит LogQL-запрос для поиска всех логов заданного operationId и transactionName.
     */
    private String buildQuery(String operationId, String transactionName) {
        return properties.getStreamSelector()
                + " | json"
                + " | operationId=`" + operationId + "`"
                + " | transactionName=`" + transactionName + "`";
    }

    /**
     * Конвертирует {@link Instant} в наносекунды от Unix-эпохи (формат, ожидаемый Loki API).
     */
    private long toNanos(Instant instant) {
        return instant.toEpochMilli() * 1_000_000L;
    }

    /**
     * Разбирает тело ответа Loki и группирует записи по полю {@code stepName}.
     *
     * <p>Структура ответа Loki (query_range, resultType=streams):
     * <pre>
     * {
     *   "data": {
     *     "result": [
     *       {
     *         "stream": { "job": "demo-service", ... },
     *         "values": [
     *           ["&lt;ns-timestamp&gt;", "&lt;raw-log-json-string&gt;"],
     *           ...
     *         ]
     *       }
     *     ]
     *   }
     * }
     * </pre>
     * </p>
     *
     * @param body тело HTTP-ответа
     * @return map stepName → список LogEntry
     */
    private Map<String, List<LogEntry>> parseResponse(String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode results = root.path("data").path("result");

        Map<String, List<LogEntry>> byStep = new HashMap<>();

        for (JsonNode stream : results) {
            for (JsonNode value : stream.path("values")) {
                parseLogLine(value.get(1).asText(), byStep);
            }
        }

        return byStep;
    }

    private static final Set<String> SKIP_FIELDS = Set.of("@timestamp", "level", "message");

    /**
     * Парсит одну JSON-строку лога и добавляет её в map, если поле {@code stepName} присутствует.
     * Все поля, кроме {@code @timestamp}, {@code level} и {@code message}, сохраняются в {@code fields}.
     */
    private void parseLogLine(String logLine, Map<String, List<LogEntry>> accumulator) {
        try {
            JsonNode json = objectMapper.readTree(logLine);

            String stepName = json.path("stepName").asText(null);
            if (stepName == null || stepName.isBlank()) return;

            LogLevel level    = LogLevel.from(json.path("level").asText());
            String message    = json.path("message").asText("");
            String timestamp  = json.path("@timestamp").asText("");

            Map<String, String> fields = new LinkedHashMap<>();
            json.fields().forEachRemaining(f -> {
                if (!SKIP_FIELDS.contains(f.getKey())) {
                    fields.put(f.getKey(), f.getValue().asText(""));
                }
            });

            accumulator.computeIfAbsent(stepName, k -> new ArrayList<>())
                    .add(new LogEntry(timestamp, level, message, fields));
        } catch (Exception e) {
            log.debug("Could not parse log line from Loki response: {}", logLine);
        }
    }
}
