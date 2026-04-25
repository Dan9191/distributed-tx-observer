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
 * Сервис визуализации: совмещает топологию шаблона транзакции
 * с реальными лог-данными конкретного запуска.
 *
 * <p>Для каждого экземпляра шага вычисляет максимальный {@link LogLevel} из всех его лог-записей
 * с переданным {@code operationId}, согласно правилу: {@code ERROR > WARN > INFO > NONE}.</p>
 *
 * <p>Если один шаг представлен несколькими экземплярами на канвасе,
 * все экземпляры получают одинаковый logLevel и одинаковый набор логов.</p>
 */
@Service
@RequiredArgsConstructor
public class VisualizationService {

    private final TemplatePort templatePort;
    private final LogQueryPort logQueryPort;

    /**
     * Строит результат визуализации для конкретного запуска транзакции.
     *
     * @param operationId     UUID запуска
     * @param transactionName название транзакции
     * @return визуализация, или {@code empty} если транзакция не зарегистрирована
     */
    public Optional<Result> visualize(String operationId, String transactionName) {
        Optional<TemplatePort.Template> template = templatePort.getTemplate(transactionName);
        if (template.isEmpty()) {
            return Optional.empty();
        }

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
                            inst.x(), inst.y(), maxLevel, entries
                    );
                })
                .toList();

        return Optional.of(new Result(transactionName, operationId, steps, t.edges()));
    }

    /**
     * Полный результат визуализации одного запуска транзакции.
     */
    public record Result(
            String transactionName,
            String operationId,
            List<StepResult> steps,
            List<TemplatePort.Edge> edges
    ) {}

    /**
     * Экземпляр шага с позицией на канвасе, уровнем логов и записями.
     *
     * @param instanceId  идентификатор экземпляра (step_template.id)
     * @param stepId      идентификатор определения шага
     * @param stepName    имя шага
     * @param serviceName микросервис, выполняющий шаг
     * @param x           координата X на канвасе
     * @param y           координата Y на канвасе
     * @param logLevel    максимальный уровень лога среди всех записей шага
     * @param logs        лог-записи шага
     */
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
