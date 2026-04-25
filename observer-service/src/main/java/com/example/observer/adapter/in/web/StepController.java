package com.example.observer.adapter.in.web;

import com.example.observer.adapter.in.web.dto.StepRegistrationRequest;
import com.example.observer.domain.port.StepRegistrationPort;
import com.example.observer.domain.port.TemplatePort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST-контроллер регистрации и управления шагами транзакций.
 */
@RestController
@RequestMapping("/api/v1/steps")
@RequiredArgsConstructor
public class StepController {

    private final StepRegistrationPort stepRegistrationPort;
    private final TemplatePort templatePort;

    /**
     * Регистрирует список шагов транзакций.
     * Выполняет upsert: создаёт новые шаги или обновляет serviceName у существующих.
     *
     * @param requests список шагов для регистрации
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.OK)
    public void register(@RequestBody @Valid List<StepRegistrationRequest> requests) {
        List<StepRegistrationPort.Command> commands = requests.stream()
                .map(r -> new StepRegistrationPort.Command(
                        r.getTransactionName(),
                        r.getStepName(),
                        r.getServiceName()))
                .toList();

        stepRegistrationPort.registerSteps(commands);
    }

    /**
     * Удаляет определение шага и все его экземпляры на канвасе.
     *
     * @param stepId идентификатор шага
     */
    @DeleteMapping("/{stepId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStep(@PathVariable Long stepId) {
        templatePort.deleteStep(stepId);
    }
}
