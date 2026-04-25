package com.example.observer.adapter.in.web.dto;

import lombok.Data;

import java.util.List;

/**
 * Ответ эндпоинта {@code GET /api/v1/visualize}.
 *
 * <p>Содержит топологию шаблона транзакции (экземпляры шагов и рёбра),
 * дополненную данными конкретного запуска: уровнем лога и записями по каждому экземпляру.</p>
 */
@Data
public class VisualizationResponse {

    private String transactionName;
    private String operationId;
    private List<StepResult> steps;
    private List<EdgeDto> edges;

    /**
     * Экземпляр шага с позицией на канвасе, уровнем логов и списком записей.
     */
    @Data
    public static class StepResult {
        /** Идентификатор экземпляра (step_template.id). Используется как ID узла React Flow. */
        private Long instanceId;
        /** Идентификатор определения шага. */
        private Long stepId;
        private String stepName;
        private String serviceName;
        /** Координата X на канвасе. {@code null} — экземпляр не размещён. */
        private Double x;
        /** Координата Y на канвасе. {@code null} — экземпляр не размещён. */
        private Double y;
        /**
         * Максимальный уровень лога: {@code "info"}, {@code "warn"},
         * {@code "error"}, {@code "none"} (нет записей для данного operationId).
         */
        private String logLevel;
        private List<LogEntryDto> logs;
    }

    /** Одна лог-запись шага. */
    @Data
    public static class LogEntryDto {
        private String timestamp;
        private String level;
        private String message;
    }

    /** Направленное ребро между двумя экземплярами шагов. */
    @Data
    public static class EdgeDto {
        private Long fromInstanceId;
        private Long toInstanceId;
    }
}
