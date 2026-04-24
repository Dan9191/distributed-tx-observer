package com.example.observer.adapter.in.web;

import com.example.observer.adapter.in.web.dto.StepRegistrationRequest;
import com.example.observer.domain.port.StepRegistrationPort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST-контроллер регистрации шагов транзакций.
 * Вызывается микросервисами при старте через observer-library.
 */
@RestController
@RequestMapping("/api/v1/steps")
@RequiredArgsConstructor
public class StepController {

    private final StepRegistrationPort stepRegistrationPort;

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
}
