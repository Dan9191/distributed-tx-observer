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
 * <p>Для каждого шага вычисляет максимальный {@link LogLevel} из всех его лог-записей
 * с переданным {@code operationId}, согласно правилу: {@code ERROR > WARN > INFO > NONE}.</p>
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

        List<StepResult> steps = t.steps().stream()
                .map(step -> {
                    List<LogQueryPort.LogEntry> entries =
                            logsByStep.getOrDefault(step.stepName(), List.of());
                    LogLevel maxLevel = entries.stream()
                            .map(LogQueryPort.LogEntry::level)
                            .reduce(LogLevel.NONE, LogLevel::max);
                    return new StepResult(
                            step.stepId(), step.stepName(), step.serviceName(),
                            step.x(), step.y(), maxLevel, entries
                    );
                })
                .toList();

        return Optional.of(new Result(transactionName, operationId, steps, t.edges()));
    }

    /**
     * Полный результат визуализации одного запуска транзакции.
     *
     * @param transactionName название транзакции
     * @param operationId     UUID запуска
     * @param steps           шаги с позициями, уровнями и логами
     * @param edges           рёбра графа транзакции
     */
    public record Result(
            String transactionName,
            String operationId,
            List<StepResult> steps,
            List<TemplatePort.Edge> edges
    ) {}

    /**
     * Шаг визуализации: позиция на канвасе + уровень логов + записи.
     *
     * @param stepId      идентификатор шага в БД
     * @param stepName    имя шага
     * @param serviceName микросервис, выполняющий шаг
     * @param x           координата X на канвасе ({@code null} — шаг не размещён)
     * @param y           координата Y на канвасе ({@code null} — шаг не размещён)
     * @param logLevel    максимальный уровень лога среди всех записей шага
     * @param logs        лог-записи шага, отсортированные по времени
     */
    public record StepResult(
            Long stepId,
            String stepName,
            String serviceName,
            Double x,
            Double y,
            LogLevel logLevel,
            List<LogQueryPort.LogEntry> logs
    ) {}
}
