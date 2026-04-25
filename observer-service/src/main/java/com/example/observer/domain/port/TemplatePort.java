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
     * Возвращает шаблон транзакции: определения шагов (палитра), экземпляры на канвасе и рёбра.
     *
     * @param transactionName название транзакции
     * @return шаблон, или {@code Optional.empty()} если транзакция не существует
     */
    Optional<Template> getTemplate(String transactionName);

    /**
     * Сохраняет шаблон транзакции. Полностью заменяет предыдущий шаблон.
     *
     * @param transactionName название транзакции
     * @param command         новые экземпляры и рёбра
     */
    void saveTemplate(String transactionName, SaveCommand command);

    /** Определение шага (элемент палитры). */
    record StepDef(Long stepId, String stepName, String serviceName) {}

    /**
     * Конкретный экземпляр шага на канвасе.
     * Один шаг может иметь несколько экземпляров с разными позициями.
     */
    record StepInstance(Long instanceId, Long stepId, String stepName, String serviceName,
                        Double x, Double y) {}

    /** Направленное ребро между двумя экземплярами. */
    record Edge(Long fromInstanceId, Long toInstanceId) {}

    /** Полный шаблон транзакции. */
    record Template(String transactionName, List<StepDef> steps,
                    List<StepInstance> instances, List<Edge> edges) {}

    /** Позиция одного экземпляра для сохранения. {@code nodeId} — клиентский ID узла React Flow. */
    record InstancePosition(String nodeId, Long stepId, Double x, Double y) {}

    /** Ребро в команде сохранения; ссылается на клиентские {@code nodeId}. */
    record EdgeCommand(String fromNodeId, String toNodeId) {}

    /** Команда сохранения шаблона. */
    record SaveCommand(List<InstancePosition> instances, List<EdgeCommand> edges) {}
}
