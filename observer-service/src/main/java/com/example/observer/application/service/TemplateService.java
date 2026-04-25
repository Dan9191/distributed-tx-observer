package com.example.observer.application.service;

import com.example.observer.adapter.out.db.*;
import com.example.observer.domain.model.StepDefinition;
import com.example.observer.domain.model.StepEdge;
import com.example.observer.domain.model.StepTemplate;
import com.example.observer.domain.model.TemplateGroup;
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
 */
@Service
@RequiredArgsConstructor
public class TemplateService implements TemplatePort {

    private final TransactionDefinitionRepository transactionRepo;
    private final StepDefinitionRepository stepRepo;
    private final StepTemplateRepository stepTemplateRepo;
    private final StepEdgeRepository stepEdgeRepo;
    private final TemplateGroupRepository groupRepo;

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTransactionNames() {
        return transactionRepo.findAll().stream()
                .map(t -> t.getName())
                .sorted()
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Optional<Template> getTemplate(String transactionName) {
        if (!transactionRepo.existsByName(transactionName)) {
            return Optional.empty();
        }

        List<StepDefinition> allSteps = stepRepo.findAllByTransactionName(transactionName);
        List<StepTemplate> positioned = stepTemplateRepo.findAllByStepTransactionName(transactionName);
        List<TemplateGroup> groups = groupRepo.findAllByTransactionName(transactionName);
        List<StepEdge> edges = stepEdgeRepo.findAllByFromInstanceStepTransactionName(transactionName);

        List<StepDef> stepDefs = allSteps.stream()
                .map(s -> new StepDef(s.getId(), s.getStepName(), s.getServiceName()))
                .toList();

        List<StepInstance> instances = positioned.stream()
                .map(t -> new StepInstance(
                        t.getId(), t.getStep().getId(),
                        t.getStep().getStepName(), t.getStep().getServiceName(),
                        t.getPosX(), t.getPosY()))
                .toList();

        List<GroupInstance> groupInstances = groups.stream()
                .map(g -> new GroupInstance(
                        g.getId(), g.getLabel(), g.getColor(),
                        g.getPosX(), g.getPosY(), g.getWidth(), g.getHeight()))
                .toList();

        List<Edge> edgeDtos = edges.stream()
                .map(e -> new Edge(e.getFromInstance().getId(), e.getToInstance().getId(), e.getStyle()))
                .toList();

        return Optional.of(new Template(transactionName, stepDefs, instances, groupInstances, edgeDtos));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Стратегия replace: удаляет рёбра → экземпляры (cascade) → группы, затем вставляет новые.
     */
    @Override
    @Transactional
    public void saveTemplate(String transactionName, SaveCommand command) {
        stepEdgeRepo.deleteAllByTransactionName(transactionName);
        stepTemplateRepo.deleteAllByTransactionName(transactionName);
        groupRepo.deleteAllByTransactionName(transactionName);

        Map<Long, StepDefinition> stepById = stepRepo.findAllByTransactionName(transactionName)
                .stream().collect(Collectors.toMap(StepDefinition::getId, s -> s));

        // Сохраняем экземпляры шагов; строим карту nodeId → StepTemplate для рёбер
        Map<String, StepTemplate> byNodeId = new LinkedHashMap<>();
        for (InstancePosition pos : command.instances()) {
            if (!stepById.containsKey(pos.stepId())) continue;
            StepTemplate t = new StepTemplate();
            t.setStep(stepById.get(pos.stepId()));
            t.setPosX(pos.x());
            t.setPosY(pos.y());
            byNodeId.put(pos.nodeId(), stepTemplateRepo.save(t));
        }

        // Сохраняем группы
        for (GroupPosition gp : command.groups()) {
            TemplateGroup g = new TemplateGroup();
            g.setTransactionName(transactionName);
            g.setLabel(gp.label());
            g.setColor(gp.color());
            g.setPosX(gp.x());
            g.setPosY(gp.y());
            g.setWidth(gp.width());
            g.setHeight(gp.height());
            groupRepo.save(g);
        }

        // Сохраняем рёбра
        List<StepEdge> edgeEntities = command.edges().stream()
                .filter(e -> byNodeId.containsKey(e.fromNodeId()) && byNodeId.containsKey(e.toNodeId()))
                .map(e -> {
                    StepEdge edge = new StepEdge();
                    edge.setFromInstance(byNodeId.get(e.fromNodeId()));
                    edge.setToInstance(byNodeId.get(e.toNodeId()));
                    edge.setStyle(e.style() != null ? e.style() : "default");
                    return edge;
                })
                .toList();
        stepEdgeRepo.saveAll(edgeEntities);
    }
}
