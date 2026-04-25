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

    @NotNull @Valid
    private List<InstanceDto> instances;

    @NotNull @Valid
    private List<GroupDto> groups;

    @NotNull @Valid
    private List<EdgeDto> edges;

    @Data
    public static class InstanceDto {
        @NotNull private String nodeId;
        /** null для маркеров start/end. */
        private Long stepId;
        @NotNull private Double x;
        @NotNull private Double y;
        /** Тип узла: step | start | end. */
        private String nodeType;
    }

    @Data
    public static class GroupDto {
        @NotNull private String nodeId;
        @NotNull private String label;
        @NotNull private String color;
        @NotNull private Double x;
        @NotNull private Double y;
        @NotNull private Double width;
        @NotNull private Double height;
    }

    @Data
    public static class EdgeDto {
        @NotNull private String fromNodeId;
        @NotNull private String toNodeId;
        private String style;
    }
}
