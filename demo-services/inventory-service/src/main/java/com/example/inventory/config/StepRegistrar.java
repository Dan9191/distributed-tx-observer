package com.example.inventory.config;

import java.util.List;
import java.util.Map;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Регистрирует шаги сервиса в observer-service при старте приложения.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StepRegistrar implements ApplicationListener<ApplicationReadyEvent> {

    private final InventoryProperties properties;
    private final RestClient.Builder restClientBuilder;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        List<Map<String, String>> payload = buildPayload();
        if (payload.isEmpty()) {
            log.info("No steps configured for observer registration, skipping");
            return;
        }

        RestClient restClient = restClientBuilder
                .baseUrl(properties.getServiceUrl())
                .build();
        try {
            restClient.post()
                    .uri("/api/v1/steps/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Registered {} steps with observer-service at {}",
                    payload.size(), properties.getServiceUrl());
        } catch (Exception e) {
            log.warn("Could not register steps with observer-service: {}", e.getMessage());
        }
    }

    /**
     * Разворачивает транзакции из конфига в плоский список записей для POST /api/v1/steps/register.
     *
     * @return список map с ключами transactionName, stepName, serviceName
     */
    private List<Map<String, String>> buildPayload() {
        return properties.getTransactions().stream()
                .flatMap(tx -> tx.getSteps().stream()
                        .map(step -> Map.of(
                                "transactionName", tx.getName(),
                                "stepName", step,
                                "serviceName", properties.getServiceName()
                        )))
                .toList();
    }
}
