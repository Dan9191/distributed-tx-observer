package com.example.observer.adapter.in.web;

import com.example.observer.adapter.in.web.dto.VisualizationResponse;
import com.example.observer.application.service.VisualizationService;
import com.example.observer.domain.port.LogQueryPort;
import com.example.observer.domain.port.TemplatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST-контроллер post-mortem визуализации распределённой транзакции.
 */
@RestController
@RequestMapping("/api/v1/visualize")
@RequiredArgsConstructor
public class VisualizationController {

    private final VisualizationService visualizationService;

    @GetMapping
    public ResponseEntity<VisualizationResponse> visualize(
            @RequestParam String operationId,
            @RequestParam String transactionName) {
        return visualizationService.visualize(operationId, transactionName)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    private VisualizationResponse toResponse(VisualizationService.Result result) {
        VisualizationResponse r = new VisualizationResponse();
        r.setTransactionName(result.transactionName());
        r.setOperationId(result.operationId());
        r.setSteps(result.steps().stream().map(this::toStepResult).toList());
        r.setGroups(result.groups().stream().map(this::toGroupDto).toList());
        r.setEdges(result.edges().stream().map(this::toEdgeDto).toList());
        return r;
    }

    private VisualizationResponse.StepResult toStepResult(VisualizationService.StepResult step) {
        VisualizationResponse.StepResult dto = new VisualizationResponse.StepResult();
        dto.setInstanceId(step.instanceId()); dto.setStepId(step.stepId());
        dto.setStepName(step.stepName()); dto.setServiceName(step.serviceName());
        dto.setX(step.x()); dto.setY(step.y());
        dto.setLogLevel(step.logLevel().name().toLowerCase());
        dto.setLogs(step.logs().stream().map(this::toLogEntry).toList());
        return dto;
    }

    private VisualizationResponse.LogEntryDto toLogEntry(LogQueryPort.LogEntry entry) {
        VisualizationResponse.LogEntryDto dto = new VisualizationResponse.LogEntryDto();
        dto.setTimestamp(entry.timestamp()); dto.setLevel(entry.level().name().toLowerCase());
        dto.setMessage(entry.message());
        return dto;
    }

    private VisualizationResponse.GroupDto toGroupDto(TemplatePort.GroupInstance g) {
        VisualizationResponse.GroupDto dto = new VisualizationResponse.GroupDto();
        dto.setGroupId(g.groupId()); dto.setLabel(g.label()); dto.setColor(g.color());
        dto.setX(g.x()); dto.setY(g.y()); dto.setWidth(g.width()); dto.setHeight(g.height());
        return dto;
    }

    private VisualizationResponse.EdgeDto toEdgeDto(TemplatePort.Edge edge) {
        VisualizationResponse.EdgeDto dto = new VisualizationResponse.EdgeDto();
        dto.setFromInstanceId(edge.fromInstanceId()); dto.setToInstanceId(edge.toInstanceId());
        return dto;
    }
}
