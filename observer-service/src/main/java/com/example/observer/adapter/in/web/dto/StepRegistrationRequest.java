package com.example.observer.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Тело одного элемента запроса на регистрацию шагов.
 * Десериализуется из JSON-массива в {@code POST /api/v1/steps/register}.
 */
@Data
public class StepRegistrationRequest {

    /** Название транзакции, к которой относится шаг. */
    @NotBlank
    private String transactionName;

    /** Уникальное название шага внутри транзакции. */
    @NotBlank
    private String stepName;

    /** Микросервис, выполняющий этот шаг. */
    @NotBlank
    private String serviceName;
}
