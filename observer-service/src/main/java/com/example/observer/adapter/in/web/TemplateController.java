package com.example.observer.adapter.in.web;

import com.example.observer.adapter.in.web.dto.SaveTemplateRequest;
import com.example.observer.adapter.in.web.dto.TemplateResponse;
import com.example.observer.domain.port.TemplatePort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер для работы с транзакциями и их шаблонами.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplatePort templatePort;

    /**
     * Возвращает список названий всех зарегистрированных транзакций.
     */
    @GetMapping
    public List<String> getAllTransactions() {
        return templatePort.getAllTransactionNames();
    }

    /**
     * Возвращает шаблон транзакции: шаги с позициями и рёбра графа.
     * Шаги без позиций — элементы палитры, ещё не размещённые на канвасе.
     *
     * @param name название транзакции
     * @return шаблон, или 404 если транзакция не существует
     */
    @GetMapping("/{name}/template")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable String name) {
        return templatePort.getTemplate(name)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Сохраняет шаблон транзакции: позиции шагов и рёбра графа.
     * Полностью заменяет предыдущий шаблон.
     *
     * @param name    название транзакции
     * @param request новые позиции и рёбра
     */
    @PutMapping("/{name}/template")
    @ResponseStatus(HttpStatus.OK)
    public void saveTemplate(@PathVariable String name,
                             @RequestBody @Valid SaveTemplateRequest request) {
        TemplatePort.SaveCommand command = new TemplatePort.SaveCommand(
                request.getSteps().stream()
                        .map(s -> new TemplatePort.StepPosition(s.getStepId(), s.getX(), s.getY()))
                        .toList(),
                request.getEdges().stream()
                        .map(e -> new TemplatePort.Edge(e.getFromStepId(), e.getToStepId()))
                        .toList()
        );
        templatePort.saveTemplate(name, command);
    }

    /**
     * Преобразует доменный объект шаблона в DTO ответа.
     */
    private TemplateResponse toResponse(TemplatePort.Template template) {
        TemplateResponse response = new TemplateResponse();
        response.setTransactionName(template.transactionName());

        response.setSteps(template.steps().stream()
                .map(s -> {
                    TemplateResponse.Step step = new TemplateResponse.Step();
                    step.setStepId(s.stepId());
                    step.setStepName(s.stepName());
                    step.setServiceName(s.serviceName());
                    step.setX(s.x());
                    step.setY(s.y());
                    return step;
                })
                .toList());

        response.setEdges(template.edges().stream()
                .map(e -> {
                    TemplateResponse.Edge edge = new TemplateResponse.Edge();
                    edge.setFromStepId(e.fromStepId());
                    edge.setToStepId(e.toStepId());
                    return edge;
                })
                .toList());

        return response;
    }
}
