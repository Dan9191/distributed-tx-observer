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
     * Возвращает шаблон транзакции: определения шагов (палитра), экземпляры на канвасе и рёбра.
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
     * Сохраняет шаблон транзакции. Полностью заменяет предыдущий шаблон.
     *
     * @param name    название транзакции
     * @param request новые экземпляры и рёбра
     */
    @PutMapping("/{name}/template")
    @ResponseStatus(HttpStatus.OK)
    public void saveTemplate(@PathVariable String name,
                             @RequestBody @Valid SaveTemplateRequest request) {
        TemplatePort.SaveCommand command = new TemplatePort.SaveCommand(
                request.getInstances().stream()
                        .map(i -> new TemplatePort.InstancePosition(
                                i.getNodeId(), i.getStepId(), i.getX(), i.getY()))
                        .toList(),
                request.getEdges().stream()
                        .map(e -> new TemplatePort.EdgeCommand(e.getFromNodeId(), e.getToNodeId()))
                        .toList()
        );
        templatePort.saveTemplate(name, command);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private TemplateResponse toResponse(TemplatePort.Template template) {
        TemplateResponse response = new TemplateResponse();
        response.setTransactionName(template.transactionName());

        response.setSteps(template.steps().stream()
                .map(s -> {
                    TemplateResponse.StepDto dto = new TemplateResponse.StepDto();
                    dto.setStepId(s.stepId());
                    dto.setStepName(s.stepName());
                    dto.setServiceName(s.serviceName());
                    return dto;
                })
                .toList());

        response.setInstances(template.instances().stream()
                .map(inst -> {
                    TemplateResponse.InstanceDto dto = new TemplateResponse.InstanceDto();
                    dto.setInstanceId(inst.instanceId());
                    dto.setStepId(inst.stepId());
                    dto.setStepName(inst.stepName());
                    dto.setServiceName(inst.serviceName());
                    dto.setX(inst.x());
                    dto.setY(inst.y());
                    return dto;
                })
                .toList());

        response.setEdges(template.edges().stream()
                .map(e -> {
                    TemplateResponse.EdgeDto dto = new TemplateResponse.EdgeDto();
                    dto.setFromInstanceId(e.fromInstanceId());
                    dto.setToInstanceId(e.toInstanceId());
                    return dto;
                })
                .toList());

        return response;
    }
}
