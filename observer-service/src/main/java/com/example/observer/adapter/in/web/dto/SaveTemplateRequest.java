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
     * Экземпляры шагов на канвасе.
     * Каждый экземпляр идентифицируется клиентским {@code nodeId} (ID узла React Flow).
     */
    @NotNull
    @Valid
    private List<InstanceDto> instances;

    /**
     * Рёбра графа. Ссылаются на {@code nodeId} экземпляров из списка {@code instances}.
     */
    @NotNull
    @Valid
    private List<EdgeDto> edges;

    /**
     * Позиция одного экземпляра шага на канвасе.
     */
    @Data
    public static class InstanceDto {

        /** Клиентский ID узла React Flow (UUID или строка). */
        @NotNull
        private String nodeId;

        /** Идентификатор шага (step_definition.id). */
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
     * Направленное ребро между двумя экземплярами.
     */
    @Data
    public static class EdgeDto {

        /** {@code nodeId} исходящего экземпляра (откуда стрелка). */
        @NotNull
        private String fromNodeId;

        /** {@code nodeId} входящего экземпляра (куда стрелка). */
        @NotNull
        private String toNodeId;
    }
}
