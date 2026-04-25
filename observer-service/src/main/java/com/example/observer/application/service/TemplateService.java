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
        List<StepTemplate> positioned = stepTemplateRepo.findAllByTransactionName(transactionName);
        List<TemplateGroup> groups = groupRepo.findAllByTransactionName(transactionName);
        List<StepEdge> edges = stepEdgeRepo.findAllByTransactionName(transactionName);

        List<StepDef> stepDefs = allSteps.stream()
                .map(s -> new StepDef(s.getId(), s.getStepName(), s.getServiceName()))
                .toList();

        List<StepInstance> instances = positioned.stream()
                .map(t -> {
                    String nodeType = t.getNodeType() != null ? t.getNodeType() : "step";
                    if ("start".equals(nodeType) || "end".equals(nodeType)) {
                        return new StepInstance(t.getId(), null, "", "", t.getPosX(), t.getPosY(), nodeType);
                    }
                    return new StepInstance(
                            t.getId(), t.getStep().getId(),
                            t.getStep().getStepName(), t.getStep().getServiceName(),
                            t.getPosX(), t.getPosY(), nodeType);
                })
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
     * <p>Стратегия replace: удаляет рёбра → узлы (cascade) → группы, затем вставляет новые.
     */
    @Override
    @Transactional
    public void saveTemplate(String transactionName, SaveCommand command) {
        stepEdgeRepo.deleteAllByTransactionName(transactionName);
        stepTemplateRepo.deleteAllByTransactionName(transactionName);
        groupRepo.deleteAllByTransactionName(transactionName);

        Map<Long, StepDefinition> stepById = stepRepo.findAllByTransactionName(transactionName)
                .stream().collect(Collectors.toMap(StepDefinition::getId, s -> s));

        Map<String, StepTemplate> byNodeId = new LinkedHashMap<>();
        for (InstancePosition pos : command.instances()) {
            String nodeType = pos.nodeType() != null ? pos.nodeType() : "step";
            StepTemplate t = new StepTemplate();
            t.setNodeType(nodeType);
            t.setTransactionName(transactionName);
            t.setPosX(pos.x());
            t.setPosY(pos.y());

            if ("start".equals(nodeType) || "end".equals(nodeType)) {
                // Маркеры не ссылаются на step_definition
                byNodeId.put(pos.nodeId(), stepTemplateRepo.save(t));
            } else {
                if (pos.stepId() == null || !stepById.containsKey(pos.stepId())) continue;
                t.setStep(stepById.get(pos.stepId()));
                byNodeId.put(pos.nodeId(), stepTemplateRepo.save(t));
            }
        }

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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteTransaction(String transactionName) {
        stepEdgeRepo.deleteAllByTransactionName(transactionName);
        stepTemplateRepo.deleteAllByTransactionName(transactionName);
        groupRepo.deleteAllByTransactionName(transactionName);
        stepRepo.deleteAllByTransactionName(transactionName);
        transactionRepo.deleteByName(transactionName);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteStep(Long stepId) {
        // step_template→step_edge каскадно удалятся через ON DELETE CASCADE в БД
        stepTemplateRepo.deleteAllByStepId(stepId);
        stepRepo.deleteById(stepId);
    }
}
