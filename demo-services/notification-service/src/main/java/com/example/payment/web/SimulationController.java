package com.example.demo.web;

import java.util.Map;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.config.ObserverProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Симулирует выполнение распределённых транзакций с MDC-обогащёнными JSON-логами.
 *
 * <p>Каждый эндпоинт генерирует уникальный {@code operationId}, прогоняет
 * сервис через шаги транзакции и возвращает этот {@code operationId} — его
 * можно вставить в форму визуализатора observer-service.</p>
 *
 * <p>Параметр {@code scenario} управляет уровнем логов на критических шагах:
 * позволяет получить транзакции с INFO, WARN или ERROR на конкретном шаге.</p>
 */
@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final ObserverProperties observerProperties;

    /**
     * Симулирует транзакцию {@code CreateOrder}.
     *
     * <p>Шаги: ValidateCart → ReserveStock → ChargePayment → ConfirmOrder.</p>
     *
     * @param scenario сценарий: {@code success} (по умолч.), {@code low-stock}, {@code payment-error}
     * @return map с ключами {@code operationId} и {@code transactionName}
     */
    @PostMapping("/create-order")
    public Map<String, String> createOrder(
            @RequestParam(defaultValue = "success") String scenario) {

        String operationId = UUID.randomUUID().toString();
        String tx = "CreateOrder";

        withMdc(tx, "ValidateCart", operationId, () ->
                log.info("Cart validation passed. Items: 3, total: $47.99"));

        withMdc(tx, "ReserveStock", operationId, () -> {
            if ("low-stock".equals(scenario)) {
                log.warn("Low stock: SKU-123 has only 2 units remaining, proceeding with reservation");
            } else {
                log.info("Stock reserved for 3 items");
            }
        });

        withMdc(tx, "ChargePayment", operationId, () -> {
            if ("payment-error".equals(scenario)) {
                log.error("Payment failed: card ending in 4242 declined by issuing bank");
            } else {
                log.info("Payment of $47.99 charged to card ending in 1234");
            }
        });

        withMdc(tx, "ConfirmOrder", operationId, () -> {
            if ("payment-error".equals(scenario)) {
                log.warn("Order confirmation skipped: payment step failed");
            } else {
                log.info("Order confirmation email sent to customer@example.com");
            }
        });

        return Map.of("operationId", operationId, "transactionName", tx);
    }

    /**
     * Симулирует транзакцию {@code ProcessPayment}.
     *
     * <p>Шаги: ValidateCard → AuthorizePayment → CapturePayment.</p>
     *
     * @param scenario сценарий: {@code success} (по умолч.), {@code auth-failed}
     * @return map с ключами {@code operationId} и {@code transactionName}
     */
    @PostMapping("/process-payment")
    public Map<String, String> processPayment(
            @RequestParam(defaultValue = "success") String scenario) {

        String operationId = UUID.randomUUID().toString();
        String tx = "ProcessPayment";

        withMdc(tx, "ValidateCard", operationId, () ->
                log.info("Card validation passed: Visa ending in 1234, expiry 12/27"));

        withMdc(tx, "AuthorizePayment", operationId, () -> {
            if ("auth-failed".equals(scenario)) {
                log.error("Authorization declined by issuing bank: insufficient funds");
            } else {
                log.info("Authorization approved: $150.00 reserved on card");
            }
        });

        withMdc(tx, "CapturePayment", operationId, () -> {
            if ("auth-failed".equals(scenario)) {
                log.warn("Capture skipped: preceding authorization step failed");
            } else {
                log.info("Payment captured: $150.00 settled successfully");
            }
        });

        return Map.of("operationId", operationId, "transactionName", tx);
    }

    /**
     * Устанавливает MDC-поля для одного шага, выполняет действие, затем очищает MDC.
     *
     * <p>MDC-поля подхватываются logstash-logback-encoder и попадают в JSON-лог
     * как top-level поля: {@code operationId}, {@code transactionName},
     * {@code stepName}, {@code serviceName}.</p>
     *
     * @param transactionName имя транзакции
     * @param stepName        имя шага
     * @param operationId     UUID конкретного запуска транзакции
     * @param action          код шага, который будет выполнен с MDC-контекстом
     */
    private void withMdc(String transactionName, String stepName,
                         String operationId, Runnable action) {
        MDC.put("operationId", operationId);
        MDC.put("transactionName", transactionName);
        MDC.put("stepName", stepName);
        MDC.put("serviceName", observerProperties.getServiceName());
        try {
            action.run();
        } finally {
            MDC.clear();
        }
    }
}
