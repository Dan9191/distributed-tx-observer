package com.example.observer.domain.port;

import java.util.List;
import java.util.Optional;

/**
 * Входящий порт: управление шаблонами транзакций.
 * Реализуется {@code TemplateService}.
 */
public interface TemplatePort {

    List<String> getAllTransactionNames();

    /**
     * Возвращает шаблон транзакции: определения шагов (палитра), экземпляры на канвасе,
     * визуальные группы и рёбра.
     */
    Optional<Template> getTemplate(String transactionName);

    /** Сохраняет шаблон транзакции. Полностью заменяет предыдущий шаблон. */
    void saveTemplate(String transactionName, SaveCommand command);

    /** Определение шага (элемент палитры). */
    record StepDef(Long stepId, String stepName, String serviceName) {}

    /** Экземпляр шага на канвасе. Один шаг может иметь несколько экземпляров. */
    record StepInstance(Long instanceId, Long stepId, String stepName, String serviceName,
                        Double x, Double y) {}

    /** Визуальная группа (именованная область) на канвасе. */
    record GroupInstance(Long groupId, String label, String color,
                         Double x, Double y, Double width, Double height) {}

    /** Направленное ребро между двумя экземплярами. */
    record Edge(Long fromInstanceId, Long toInstanceId, String style) {}

    /** Полный шаблон транзакции. */
    record Template(String transactionName, List<StepDef> steps,
                    List<StepInstance> instances, List<GroupInstance> groups, List<Edge> edges) {}

    /** Позиция одного экземпляра шага для сохранения. */
    record InstancePosition(String nodeId, Long stepId, Double x, Double y) {}

    /** Группа для сохранения. */
    record GroupPosition(String nodeId, String label, String color,
                         Double x, Double y, Double width, Double height) {}

    /** Ребро в команде сохранения; ссылается на клиентские nodeId. */
    record EdgeCommand(String fromNodeId, String toNodeId, String style) {}

    /** Команда сохранения шаблона. */
    record SaveCommand(List<InstancePosition> instances, List<GroupPosition> groups,
                       List<EdgeCommand> edges) {}
}
