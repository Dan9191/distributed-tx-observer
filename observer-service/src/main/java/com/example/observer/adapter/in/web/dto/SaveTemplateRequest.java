package com.example.observer.adapter.in.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Тело запроса на сохранение шаблона транзакции.
 * Полностью заменяет предыдущий шаблон.
 */
@Data
public class SaveTemplateRequest {

    /**
     * Позиции шагов на канвасе.
     * Шаги, отсутствующие в списке, удаляются с канваса (становятся элементами палитры).
     */
    @NotNull
    @Valid
    private List<Step> steps;

    /** Рёбра графа. */
    @NotNull
    @Valid
    private List<Edge> edges;

    /**
     * Позиция одного шага на канвасе.
     */
    @Data
    public static class Step {

        /** Идентификатор шага. */
        @NotNull
        private Long stepId;

        /** Координата X на канвасе. */
        @NotNull
        private Double x;

        /** Координата Y на канвасе. */
        @NotNull
        private Double y;
    }

    /**
     * Направленное ребро между двумя шагами.
     */
    @Data
    public static class Edge {

        /** ID исходящего шага. */
        @NotNull
        private Long fromStepId;

        /** ID входящего шага. */
        @NotNull
        private Long toStepId;
    }
}
