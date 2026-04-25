package com.example.order.controller;

import java.util.Map;
import java.util.UUID;

import com.example.order.config.OrderProperties;
import com.example.order.config.ServiceUrlsProperties;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestTemplate;

/**
 * Симулирует выполнение распределённых транзакций с MDC-обогащёнными JSON-логами.
 */
@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final OrderProperties observerProperties;
    private final ServiceUrlsProperties urls;
    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/create-order")
    public Map<String, String> createOrder(
            @RequestParam(defaultValue = "success") String scenario) {

        String opId = UUID.randomUUID().toString();
        String tx = "CreateOrder";

        withMdc(tx, "StartOrder", opId, () ->
                log.info("Order started"));

        // CALL INVENTORY
        withMdc(tx, "CallInventory", opId, () ->
                log.info("Calling inventory-service"));

        try {
            restTemplate.postForObject(
                    urls.getInventoryUrl() + "/simulate/reserve?operationId=" + opId + "&scenario=" + scenario,
                    null, Void.class);

        } catch (Exception e) {

            withMdc(tx, "CompleteOrderByOutOfStock", opId, () ->
                    log.info("Order failed: out of stock"));

            return Map.of("operationId", opId);
        }

        // CALL PAYMENT
        withMdc(tx, "CallPayment", opId, () ->
                log.info("Calling payment-service"));

        try {
            restTemplate.postForObject(
                    urls.getPaymentUrl() + "/simulate/pay?operationId=" + opId + "&scenario=" + scenario,
                    null, Void.class);

        } catch (Exception e) {

            // CALL INVENTORY RELEASE
            withMdc(tx, "CallInventoryRelease", opId, () ->
                    log.warn("Calling inventory release"));

            restTemplate.postForObject(
                    urls.getInventoryUrl() + "/simulate/release?operationId=" + opId,
                    null, Void.class);

            withMdc(tx, "CompleteOrderByPaymentFailed", opId, () ->
                    log.info("Order failed: payment error"));

            return Map.of("operationId", opId);
        }

        // SUCCESS
        withMdc(tx, "CompleteOrderSuccess", opId, () ->
                log.info("Order success"));

        // Notification (non-critical)
        try {
            restTemplate.postForObject(
                    urls.getNotificationUrl() + "/simulate/send?operationId=" + opId + "&scenario=" + scenario,
                    null, Void.class);
        } catch (Exception ignored) {}

        return Map.of("operationId", opId);
    }

    private void withMdc(String tx, String step, String opId, Runnable r) {
        MDC.put("operationId", opId);
        MDC.put("transactionName", tx);
        MDC.put("stepName", step);
        MDC.put("serviceName", observerProperties.getServiceName());
        try { r.run(); } finally { MDC.clear(); }
    }
}
