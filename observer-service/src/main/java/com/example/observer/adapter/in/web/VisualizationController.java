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

import java.util.List;

/**
 * REST-контроллер post-mortem визуализации распределённой транзакции.
 *
 * <p>Принимает {@code operationId} конкретного запуска, загружает топологию шаблона
 * и данные логов из Loki, возвращает единый ответ для рендеринга графа
 * с цветовой индикацией уровней.</p>
 */
@RestController
@RequestMapping("/api/v1/visualize")
@RequiredArgsConstructor
public class VisualizationController {

    private final VisualizationService visualizationService;

    /**
     * Возвращает визуализацию конкретного запуска транзакции.
     *
     * @param operationId     UUID запуска (писался в MDC каждой лог-записью)
     * @param transactionName название транзакции
     * @return визуализация с уровнями логов и записями по шагам,
     *         или 404 если транзакция не зарегистрирована
     */
    @GetMapping
    public ResponseEntity<VisualizationResponse> visualize(
            @RequestParam String operationId,
            @RequestParam String transactionName) {

        return visualizationService.visualize(operationId, transactionName)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private VisualizationResponse toResponse(VisualizationService.Result result) {
        VisualizationResponse response = new VisualizationResponse();
        response.setTransactionName(result.transactionName());
        response.setOperationId(result.operationId());

        response.setSteps(result.steps().stream()
                .map(this::toStepResult)
                .toList());

        response.setEdges(result.edges().stream()
                .map(this::toEdgeDto)
                .toList());

        return response;
    }

    private VisualizationResponse.StepResult toStepResult(VisualizationService.StepResult step) {
        VisualizationResponse.StepResult dto = new VisualizationResponse.StepResult();
        dto.setStepId(step.stepId());
        dto.setStepName(step.stepName());
        dto.setServiceName(step.serviceName());
        dto.setX(step.x());
        dto.setY(step.y());
        dto.setLogLevel(step.logLevel().name().toLowerCase());
        dto.setLogs(step.logs().stream().map(this::toLogEntry).toList());
        return dto;
    }

    private VisualizationResponse.LogEntryDto toLogEntry(LogQueryPort.LogEntry entry) {
        VisualizationResponse.LogEntryDto dto = new VisualizationResponse.LogEntryDto();
        dto.setTimestamp(entry.timestamp());
        dto.setLevel(entry.level().name().toLowerCase());
        dto.setMessage(entry.message());
        return dto;
    }

    private VisualizationResponse.EdgeDto toEdgeDto(TemplatePort.Edge edge) {
        VisualizationResponse.EdgeDto dto = new VisualizationResponse.EdgeDto();
        dto.setFromStepId(edge.fromStepId());
        dto.setToStepId(edge.toStepId());
        return dto;
    }
}
