package com.example.observer.adapter.in.web.dto;

import lombok.Data;

import java.util.List;

/**
 * Ответ эндпоинта {@code GET /api/v1/visualize}.
 *
 * <p>Содержит топологию шаблона транзакции (позиции шагов и рёбра),
 * дополненную данными конкретного запуска: уровнем лога и записями по каждому шагу.</p>
 */
@Data
public class VisualizationResponse {

    private String transactionName;
    private String operationId;
    private List<StepResult> steps;
    private List<EdgeDto> edges;

    /**
     * Шаг с позицией на канвасе, уровнем логов и списком записей.
     */
    @Data
    public static class StepResult {
        private Long stepId;
        private String stepName;
        private String serviceName;
        /** Координата X на канвасе. {@code null} — шаг не размещён на канвасе. */
        private Double x;
        /** Координата Y на канвасе. {@code null} — шаг не размещён на канвасе. */
        private Double y;
        /**
         * Максимальный уровень лога среди всех записей шага: {@code "info"}, {@code "warn"},
         * {@code "error"}, {@code "none"} (нет записей для данного operationId).
         */
        private String logLevel;
        private List<LogEntryDto> logs;
    }

    /**
     * Одна лог-запись шага.
     */
    @Data
    public static class LogEntryDto {
        private String timestamp;
        private String level;
        private String message;
    }

    /**
     * Направленное ребро графа транзакции.
     */
    @Data
    public static class EdgeDto {
        private Long fromStepId;
        private Long toStepId;
    }
}
