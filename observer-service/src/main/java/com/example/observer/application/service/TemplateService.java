package com.example.observer.application.service;

import com.example.observer.adapter.out.db.*;
import com.example.observer.domain.model.StepDefinition;
import com.example.observer.domain.model.StepEdge;
import com.example.observer.domain.model.StepTemplate;
import com.example.observer.domain.port.TemplatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис управления шаблонами транзакций.
 * Реализует логику чтения и сохранения экземпляров шагов и рёбер графа.
 */
@Service
@RequiredArgsConstructor
public class TemplateService implements TemplatePort {

    private final TransactionDefinitionRepository transactionRepo;
    private final StepDefinitionRepository stepRepo;
    private final StepTemplateRepository stepTemplateRepo;
    private final StepEdgeRepository stepEdgeRepo;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTransactionNames() {
        return transactionRepo.findAll().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Возвращает все определения шагов транзакции (палитра) и все экземпляры на канвасе.
     * Один шаг может быть представлен несколькими экземплярами.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Template> getTemplate(String transactionName) {
        if (!transactionRepo.existsByName(transactionName)) {
            return Optional.empty();
        }

        List<StepDefinition> allSteps = stepRepo.findAllByTransactionName(transactionName);
        List<StepTemplate> positioned = stepTemplateRepo.findAllByStepTransactionName(transactionName);
        List<StepEdge> edges = stepEdgeRepo.findAllByFromInstanceStepTransactionName(transactionName);

        List<StepDef> stepDefs = allSteps.stream()
                .map(s -> new StepDef(s.getId(), s.getStepName(), s.getServiceName()))
                .toList();

        List<StepInstance> instances = positioned.stream()
                .map(t -> new StepInstance(
                        t.getId(),
                        t.getStep().getId(),
                        t.getStep().getStepName(),
                        t.getStep().getServiceName(),
                        t.getPosX(),
                        t.getPosY()
                ))
                .toList();

        List<Edge> edgeDtos = edges.stream()
                .map(e -> new Edge(e.getFromInstance().getId(), e.getToInstance().getId()))
                .toList();

        return Optional.of(new Template(transactionName, stepDefs, instances, edgeDtos));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Стратегия: полное удаление предыдущего шаблона с последующей вставкой нового.
     * Рёбра удаляются явно перед экземплярами; FK ON DELETE CASCADE — дополнительная страховка.
     * Для привязки рёбер к новым экземплярам используется клиентский {@code nodeId}.
     */
    @Override
    @Transactional
    public void saveTemplate(String transactionName, SaveCommand command) {
        stepEdgeRepo.deleteAllByTransactionName(transactionName);
        stepTemplateRepo.deleteAllByTransactionName(transactionName);

        Map<Long, StepDefinition> stepById = stepRepo.findAllByTransactionName(transactionName)
                .stream()
                .collect(Collectors.toMap(StepDefinition::getId, s -> s));

        // Сохраняем экземпляры; строим карту nodeId → StepTemplate для последующей привязки рёбер
        Map<String, StepTemplate> byNodeId = new LinkedHashMap<>();
        for (InstancePosition pos : command.instances()) {
            if (!stepById.containsKey(pos.stepId())) continue;
            StepTemplate t = new StepTemplate();
            t.setStep(stepById.get(pos.stepId()));
            t.setPosX(pos.x());
            t.setPosY(pos.y());
            byNodeId.put(pos.nodeId(), stepTemplateRepo.save(t));
        }

        List<StepEdge> edgeEntities = command.edges().stream()
                .filter(e -> byNodeId.containsKey(e.fromNodeId()) && byNodeId.containsKey(e.toNodeId()))
                .map(e -> {
                    StepEdge edge = new StepEdge();
                    edge.setFromInstance(byNodeId.get(e.fromNodeId()));
                    edge.setToInstance(byNodeId.get(e.toNodeId()));
                    return edge;
                })
                .toList();
        stepEdgeRepo.saveAll(edgeEntities);
    }
}
