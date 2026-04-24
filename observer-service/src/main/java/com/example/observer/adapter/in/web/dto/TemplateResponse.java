package com.example.observer.adapter.in.web.dto;

import lombok.Data;

import java.util.List;

/**
 * Ответ на запрос шаблона транзакции.
 * Содержит все зарегистрированные шаги (с позициями или без) и рёбра графа.
 */
@Data
public class TemplateResponse {

    /** Название транзакции. */
    private String transactionName;

    /**
     * Все шаги транзакции.
     * Шаги с {@code x != null} размещены на канвасе; остальные — элементы палитры.
     */
    private List<Step> steps;

    /** Рёбра графа шаблона. */
    private List<Edge> edges;

    /**
     * Шаг с опциональной позицией на канвасе.
     */
    @Data
    public static class Step {

        /** Идентификатор шага в БД. Используется как ID узла в React Flow. */
        private Long stepId;

        /** Название шага. */
        private String stepName;

        /** Микросервис, выполняющий шаг. */
        private String serviceName;

        /** Координата X на канвасе, {@code null} если шаг не размещён. */
        private Double x;

        /** Координата Y на канвасе, {@code null} если шаг не размещён. */
        private Double y;
    }

    /**
     * Направленное ребро между двумя шагами.
     */
    @Data
    public static class Edge {

        /** ID исходящего шага (откуда стрелка). */
        private Long fromStepId;

        /** ID входящего шага (куда стрелка). */
        private Long toStepId;
    }
}
