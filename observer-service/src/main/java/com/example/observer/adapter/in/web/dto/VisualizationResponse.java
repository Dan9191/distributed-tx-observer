package com.example.observer.adapter.in.web.dto;

import lombok.Data;

import java.util.List;

/**
 * Ответ эндпоинта {@code GET /api/v1/visualize}.
 */
@Data
public class VisualizationResponse {

    private String transactionName;
    private String operationId;
    private List<StepResult> steps;
    private List<GroupDto> groups;
    private List<EdgeDto> edges;

    @Data
    public static class StepResult {
        private Long instanceId;
        private Long stepId;
        private String stepName;
        private String serviceName;
        private Double x;
        private Double y;
        private String logLevel;
        private List<LogEntryDto> logs;
    }

    @Data
    public static class LogEntryDto {
        private String timestamp;
        private String level;
        private String message;
    }

    @Data
    public static class GroupDto {
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
        private String style;
    }
}
