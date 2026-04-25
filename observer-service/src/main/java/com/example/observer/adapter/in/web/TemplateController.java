package com.example.observer.adapter.in.web;

import com.example.observer.adapter.in.web.dto.SaveTemplateRequest;
import com.example.observer.adapter.in.web.dto.TemplateResponse;
import com.example.observer.domain.port.TemplatePort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * REST-контроллер для работы с транзакциями и их шаблонами.
 */
@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplatePort templatePort;

    @GetMapping
    public List<String> getAllTransactions() {
        return templatePort.getAllTransactionNames();
    }

    @GetMapping("/{name}/template")
    public ResponseEntity<TemplateResponse> getTemplate(@PathVariable String name) {
        return templatePort.getTemplate(name)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{name}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTransaction(@PathVariable String name) {
        templatePort.deleteTransaction(name);
    }

    @PutMapping("/{name}/template")
    @ResponseStatus(HttpStatus.OK)
    public void saveTemplate(@PathVariable String name,
                             @RequestBody @Valid SaveTemplateRequest request) {
        TemplatePort.SaveCommand command = new TemplatePort.SaveCommand(
                request.getInstances().stream()
                        .map(i -> new TemplatePort.InstancePosition(
                                i.getNodeId(), i.getStepId(), i.getX(), i.getY(), i.getNodeType()))
                        .toList(),
                request.getGroups().stream()
                        .map(g -> new TemplatePort.GroupPosition(
                                g.getNodeId(), g.getLabel(), g.getColor(),
                                g.getX(), g.getY(), g.getWidth(), g.getHeight()))
                        .toList(),
                request.getEdges().stream()
                        .map(e -> new TemplatePort.EdgeCommand(e.getFromNodeId(), e.getToNodeId(), e.getStyle()))
                        .toList()
        );
        templatePort.saveTemplate(name, command);
    }

    // ── mapping ───────────────────────────────────────────────────────────────

    private TemplateResponse toResponse(TemplatePort.Template t) {
        TemplateResponse r = new TemplateResponse();
        r.setTransactionName(t.transactionName());

        r.setSteps(t.steps().stream().map(s -> {
            TemplateResponse.StepDto dto = new TemplateResponse.StepDto();
            dto.setStepId(s.stepId()); dto.setStepName(s.stepName()); dto.setServiceName(s.serviceName());
            return dto;
        }).toList());

        r.setInstances(t.instances().stream().map(inst -> {
            TemplateResponse.InstanceDto dto = new TemplateResponse.InstanceDto();
            dto.setInstanceId(inst.instanceId()); dto.setStepId(inst.stepId());
            dto.setStepName(inst.stepName()); dto.setServiceName(inst.serviceName());
            dto.setX(inst.x()); dto.setY(inst.y());
            dto.setNodeType(inst.nodeType() != null ? inst.nodeType() : "step");
            return dto;
        }).toList());

        r.setGroups(t.groups().stream().map(g -> {
            TemplateResponse.GroupDto dto = new TemplateResponse.GroupDto();
            dto.setGroupId(g.groupId()); dto.setLabel(g.label()); dto.setColor(g.color());
            dto.setX(g.x()); dto.setY(g.y()); dto.setWidth(g.width()); dto.setHeight(g.height());
            return dto;
        }).toList());

        r.setEdges(t.edges().stream().map(e -> {
            TemplateResponse.EdgeDto dto = new TemplateResponse.EdgeDto();
            dto.setFromInstanceId(e.fromInstanceId()); dto.setToInstanceId(e.toInstanceId());
            dto.setStyle(e.style());
            return dto;
        }).toList());

        return r;
    }
}
