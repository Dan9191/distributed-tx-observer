package com.example.observer.domain.port;

import java.util.List;
import java.util.Optional;

/**
 * Входящий порт: управление шаблонами транзакций.
 * Реализуется {@code TemplateService}.
 */
public interface TemplatePort {

    /**
     * Возвращает названия всех зарегистрированных транзакций в алфавитном порядке.
     */
    List<String> getAllTransactionNames();

    /**
     * Возвращает шаблон транзакции: все зарегистрированные шаги с позициями
     * (null, если шаг ещё не размещён на канвасе) и рёбра графа.
     *
     * @param transactionName название транзакции
     * @return шаблон, или {@code Optional.empty()} если транзакция не существует
     */
    Optional<Template> getTemplate(String transactionName);

    /**
     * Сохраняет шаблон транзакции: позиции шагов и рёбра графа.
     * Полностью заменяет предыдущий шаблон.
     *
     * @param transactionName название транзакции
     * @param command         новые позиции и рёбра
     */
    void saveTemplate(String transactionName, SaveCommand command);

    /**
     * Шаг с позицией на канвасе.
     * Если {@code x} и {@code y} равны {@code null} — шаг ещё не размещён (элемент палитры).
     */
    record StepNode(Long stepId, String stepName, String serviceName, Double x, Double y) {}

    /** Направленное ребро между двумя шагами. */
    record Edge(Long fromStepId, Long toStepId) {}

    /** Полный шаблон транзакции. */
    record Template(String transactionName, List<StepNode> steps, List<Edge> edges) {}

    /** Позиция одного шага для сохранения. */
    record StepPosition(Long stepId, Double x, Double y) {}

    /** Команда сохранения шаблона. */
    record SaveCommand(List<StepPosition> steps, List<Edge> edges) {}
}
