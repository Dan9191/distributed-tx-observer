package com.example.observer.domain.port;

import com.example.observer.domain.model.LogLevel;

import java.util.List;
import java.util.Map;

/**
 * Исходящий порт: получение структурированных логов из внешнего хранилища.
 *
 * <p>Реализуется {@code LokiLogQueryAdapter}. Абстракция позволяет заменить Loki
 * на другой бэкенд логов без изменения прикладного слоя.</p>
 */
public interface LogQueryPort {

    /**
     * Возвращает все лог-записи данного {@code operationId},
     * сгруппированные по имени шага ({@code stepName}).
     *
     * <p>Если шаг не встречается ни в одной записи — его ключ отсутствует в map.
     * При недоступности бэкенда логов возвращается пустая map.</p>
     *
     * @param operationId     UUID конкретного запуска транзакции
     * @param transactionName название транзакции (для фильтрации в запросе)
     * @return map stepName → список лог-записей, отсортированных по времени
     */
    Map<String, List<LogEntry>> getLogsByStep(String operationId, String transactionName);

    /**
     * Одна лог-запись шага.
     *
     * @param timestamp метка времени в формате ISO-8601 (поле {@code @timestamp})
     * @param level     уровень серьёзности
     * @param message   текст сообщения
     */
    record LogEntry(String timestamp, LogLevel level, String message) {}
}
