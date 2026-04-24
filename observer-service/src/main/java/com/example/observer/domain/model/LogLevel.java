package com.example.observer.domain.model;

/**
 * Уровень серьёзности логов шага транзакции.
 *
 * <p>Порядок объявления важен: {@code NONE < INFO < WARN < ERROR}.
 * {@link #max(LogLevel)} опирается на {@code ordinal()} для выбора более серьёзного уровня.</p>
 */
public enum LogLevel {

    /** Шаг не встретился в логах для данного operationId. */
    NONE,
    /** Все записи на уровне INFO. */
    INFO,
    /** Среди записей есть хотя бы одна WARN, но нет ERROR. */
    WARN,
    /** Среди записей есть хотя бы одна ERROR. */
    ERROR;

    /**
     * Парсит строковое представление уровня лога в enum-значение.
     * Нераспознанные значения возвращают {@link #NONE}.
     *
     * @param raw строка уровня из лог-записи (например, {@code "ERROR"}, {@code "warn"})
     * @return соответствующий уровень или {@code NONE}
     */
    public static LogLevel from(String raw) {
        if (raw == null || raw.isBlank()) return NONE;
        return switch (raw.trim().toUpperCase()) {
            case "ERROR" -> ERROR;
            case "WARN", "WARNING" -> WARN;
            case "INFO" -> INFO;
            default -> NONE;
        };
    }

    /**
     * Возвращает более серьёзный из двух уровней.
     *
     * @param other второй уровень для сравнения
     * @return уровень с большим {@code ordinal()}
     */
    public LogLevel max(LogLevel other) {
        return this.ordinal() >= other.ordinal() ? this : other;
    }
}
