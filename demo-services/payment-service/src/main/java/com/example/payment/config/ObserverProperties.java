package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

/**
 * Конфигурация регистрации шагов в observer-service.
 *
 * <p>Читается из блока {@code observer:} в {@code application.yml}.
 * Группирует шаги по транзакции: одна транзакция — список stepName.</p>
 *
 * <pre>
 * observer:
 *   service-url: http://localhost:8033
 *   service-name: demo-service
 *   transactions:
 *     - name: CreateOrder
 *       steps:
 *         - ValidateCart
 *         - ChargePayment
 * </pre>
 */
@ConfigurationProperties(prefix = "observer")
@Data
public class ObserverProperties {

    /** URL observer-service, куда POST-ятся шаги при старте. */
    private String serviceUrl;

    /** Имя текущего сервиса — записывается в поле serviceName каждого шага. */
    private String serviceName;

    /** Список транзакций и их шагов, в которых участвует этот сервис. */
    private List<TransactionConfig> transactions = new ArrayList<>();

    /**
     * Описание одной транзакции: её имя и список имён шагов.
     */
    @Data
    public static class TransactionConfig {

        /** Имя транзакции, например {@code CreateOrder}. */
        private String name;

        /** Имена шагов этой транзакции, выполняемых данным сервисом. */
        private List<String> steps = new ArrayList<>();
    }
}
