package com.example.observer.adapter.in.web.dto;

import lombok.Data;

import java.util.List;

/**
 * Ответ на запрос шаблона транзакции.
 */
@Data
public class TemplateResponse {

    private String transactionName;
    /** Все зарегистрированные шаги транзакции (палитра). */
    private List<StepDto> steps;
    /** Экземпляры шагов, размещённые на канвасе. */
    private List<InstanceDto> instances;
    /** Визуальные группы на канвасе. */
    private List<GroupDto> groups;
    /** Рёбра между экземплярами. */
    private List<EdgeDto> edges;

    @Data
    public static class StepDto {
        private Long stepId;
        private String stepName;
        private String serviceName;
    }

    @Data
    public static class InstanceDto {
        /** Идентификатор экземпляра (step_template.id). Используется как ID узла React Flow. */
        private Long instanceId;
        private Long stepId;
        private String stepName;
        private String serviceName;
        private Double x;
        private Double y;
    }

    @Data
    public static class GroupDto {
        /** Идентификатор группы (template_group.id). Используется как ID узла React Flow. */
        private Long groupId;
        private String label;
        private String color;
        private Double x;
        private Double y;
        private Double width;
        private Double height;
    }

    @Data
    public static class EdgeDto {
        private Long fromInstanceId;
        private Long toInstanceId;
    }
}
