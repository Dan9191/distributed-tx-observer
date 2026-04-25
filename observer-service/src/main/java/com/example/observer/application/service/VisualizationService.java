package com.example.observer.application.service;

import com.example.observer.domain.model.LogLevel;
import com.example.observer.domain.port.LogQueryPort;
import com.example.observer.domain.port.TemplatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Сервис визуализации: совмещает топологию шаблона транзакции с лог-данными конкретного запуска.
 *
 * <p>Для каждого экземпляра шага вычисляет максимальный {@link LogLevel}.
 * Несколько экземпляров одного шага получают одинаковый logLevel и логи.</p>
 */
@Service
@RequiredArgsConstructor
public class VisualizationService {

    private final TemplatePort templatePort;
    private final LogQueryPort logQueryPort;

    public Optional<Result> visualize(String operationId, String transactionName) {
        Optional<TemplatePort.Template> template = templatePort.getTemplate(transactionName);
        if (template.isEmpty()) return Optional.empty();

        Map<String, List<LogQueryPort.LogEntry>> logsByStep =
                logQueryPort.getLogsByStep(operationId, transactionName);

        TemplatePort.Template t = template.get();

        List<StepResult> steps = t.instances().stream()
                .map(inst -> {
                    List<LogQueryPort.LogEntry> entries =
                            logsByStep.getOrDefault(inst.stepName(), List.of());
                    LogLevel maxLevel = entries.stream()
                            .map(LogQueryPort.LogEntry::level)
                            .reduce(LogLevel.NONE, LogLevel::max);
                    return new StepResult(
                            inst.instanceId(), inst.stepId(), inst.stepName(), inst.serviceName(),
                            inst.x(), inst.y(), maxLevel, entries);
                })
                .toList();

        return Optional.of(new Result(transactionName, operationId, steps, t.groups(), t.edges()));
    }

    public record Result(
            String transactionName,
            String operationId,
            List<StepResult> steps,
            List<TemplatePort.GroupInstance> groups,
            List<TemplatePort.Edge> edges
    ) {}

    public record StepResult(
            Long instanceId,
            Long stepId,
            String stepName,
            String serviceName,
            Double x,
            Double y,
            LogLevel logLevel,
            List<LogQueryPort.LogEntry> logs
    ) {}
}
