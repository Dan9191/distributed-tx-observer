package com.example.observer.adapter.in.web.dto;

import lombok.Data;

import java.util.List;

/**
 * Ответ на запрос шаблона транзакции.
 *
 * <p>Содержит:
 * <ul>
 *   <li>{@code steps} — все определения шагов транзакции (элементы палитры)</li>
 *   <li>{@code instances} — экземпляры шагов, размещённые на канвасе</li>
 *   <li>{@code edges} — направленные рёбра между экземплярами</li>
 * </ul>
 */
@Data
public class TemplateResponse {

    /** Название транзакции. */
    private String transactionName;

    /** Все зарегистрированные шаги транзакции (используются для наполнения палитры). */
    private List<StepDto> steps;

    /**
     * Экземпляры шагов на канвасе.
     * Один шаг ({@code stepId}) может иметь несколько экземпляров.
     */
    private List<InstanceDto> instances;

    /** Рёбра графа между экземплярами. */
    private List<EdgeDto> edges;

    /**
     * Определение шага — элемент палитры.
     */
    @Data
    public static class StepDto {
        /** Идентификатор шага в БД. */
        private Long stepId;
        /** Название шага. */
        private String stepName;
        /** Микросервис, выполняющий шаг. */
        private String serviceName;
    }

    /**
     * Экземпляр шага на канвасе.
     */
    @Data
    public static class InstanceDto {
        /** Идентификатор экземпляра (step_template.id). Используется как ID узла React Flow. */
        private Long instanceId;
        /** Идентификатор шага (step_definition.id). */
        private Long stepId;
        /** Название шага. */
        private String stepName;
        /** Микросервис, выполняющий шаг. */
        private String serviceName;
        /** Координата X на канвасе. */
        private Double x;
        /** Координата Y на канвасе. */
        private Double y;
    }

    /**
     * Направленное ребро между двумя экземплярами шагов.
     */
    @Data
    public static class EdgeDto {
        /** ID исходящего экземпляра (откуда стрелка). */
        private Long fromInstanceId;
        /** ID входящего экземпляра (куда стрелка). */
        private Long toInstanceId;
    }
}
