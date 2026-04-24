package com.example.observer.application.service;

import com.example.observer.adapter.out.db.*;
import com.example.observer.domain.model.StepDefinition;
import com.example.observer.domain.model.StepEdge;
import com.example.observer.domain.model.StepTemplate;
import com.example.observer.domain.port.TemplatePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Сервис управления шаблонами транзакций.
 * Реализует логику чтения и сохранения позиций шагов и рёбер графа.
 */
@Service
@RequiredArgsConstructor
public class TemplateService implements TemplatePort {

    private final TransactionDefinitionRepository transactionRepo;
    private final StepDefinitionRepository stepRepo;
    private final StepTemplateRepository stepTemplateRepo;
    private final StepEdgeRepository stepEdgeRepo;

    /**
     * {@inheritDoc}
     */
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
     * <p>Возвращает все зарегистрированные шаги транзакции.
     * Шаги, размещённые на канвасе, имеют заполненные координаты;
     * неразмещённые — элементы палитры с {@code x = null, y = null}.
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<Template> getTemplate(String transactionName) {
        if (!transactionRepo.existsByName(transactionName)) {
            return Optional.empty();
        }

        List<StepDefinition> allSteps = stepRepo.findAllByTransactionName(transactionName);
        List<StepTemplate> positioned = stepTemplateRepo.findAllByStepTransactionName(transactionName);
        List<StepEdge> edges = stepEdgeRepo.findAllByFromStepTransactionName(transactionName);

        // Индекс stepId → позиция для быстрого поиска
        Map<Long, StepTemplate> positionByStepId = positioned.stream()
                .collect(Collectors.toMap(t -> t.getStep().getId(), t -> t));

        List<StepNode> stepNodes = allSteps.stream()
                .map(step -> {
                    StepTemplate pos = positionByStepId.get(step.getId());
                    return new StepNode(
                            step.getId(),
                            step.getStepName(),
                            step.getServiceName(),
                            pos != null ? pos.getPosX() : null,
                            pos != null ? pos.getPosY() : null
                    );
                })
                .toList();

        List<Edge> edgeDtos = edges.stream()
                .map(e -> new Edge(e.getFromStep().getId(), e.getToStep().getId()))
                .toList();

        return Optional.of(new Template(transactionName, stepNodes, edgeDtos));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Стратегия: полное удаление предыдущего шаблона с последующей вставкой нового.
     * Шаги, не вошедшие в команду, становятся элементами палитры.
     */
    @Override
    @Transactional
    public void saveTemplate(String transactionName, SaveCommand command) {
        // Удаляем предыдущий шаблон
        stepEdgeRepo.deleteAllByTransactionName(transactionName);
        stepTemplateRepo.deleteAllByTransactionName(transactionName);

        // Индекс stepId → StepDefinition для валидации входящих ID
        Map<Long, StepDefinition> stepById = stepRepo.findAllByTransactionName(transactionName)
                .stream()
                .collect(Collectors.toMap(StepDefinition::getId, s -> s));

        // Сохраняем позиции шагов
        List<StepTemplate> templates = command.steps().stream()
                .filter(pos -> stepById.containsKey(pos.stepId()))
                .map(pos -> {
                    StepTemplate t = new StepTemplate();
                    t.setStep(stepById.get(pos.stepId()));
                    t.setPosX(pos.x());
                    t.setPosY(pos.y());
                    return t;
                })
                .toList();
        stepTemplateRepo.saveAll(templates);

        // Сохраняем рёбра
        List<StepEdge> edgeEntities = command.edges().stream()
                .filter(e -> stepById.containsKey(e.fromStepId()) && stepById.containsKey(e.toStepId()))
                .map(e -> {
                    StepEdge edge = new StepEdge();
                    edge.setFromStep(stepById.get(e.fromStepId()));
                    edge.setToStep(stepById.get(e.toStepId()));
                    return edge;
                })
                .toList();
        stepEdgeRepo.saveAll(edgeEntities);
    }
}
