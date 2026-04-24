package com.example.observer.adapter.out.loki;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

/**
 * Настройки HTTP-клиента к Loki.
 *
 * <pre>
 * loki:
 *   base-url: http://localhost:3100
 *   stream-selector: '{job=~".+"}'
 *   lookback-hours: 24
 * </pre>
 */
@ConfigurationProperties(prefix = "loki")
@Data
public class LokiProperties {

    /** Базовый URL Loki HTTP API. */
    private String baseUrl = "http://localhost:3100";

    /**
     * LogQL-селектор потоков, ограничивает набор потоков перед фильтрацией.
     * По умолчанию — все потоки с непустым label {@code job}.
     */
    private String streamSelector = "{job=~\".+\"}";

    /** Глубина поиска логов в часах (окно назад от текущего момента). */
    private int lookbackHours = 24;
}
