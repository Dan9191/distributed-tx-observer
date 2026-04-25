package com.example.inventory.controller;

import com.example.inventory.config.InventoryProperties;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Симулирует выполнение распределённых транзакций с MDC-обогащёнными JSON-логами.
 */
@RestController
@RequestMapping("/simulate")
@RequiredArgsConstructor
@Slf4j
public class SimulationController {

    private final InventoryProperties inventoryProperties;

    @PostMapping("/reserve")
    public void reserve(@RequestParam String operationId,
                        @RequestParam(defaultValue = "success") String scenario) {

        withMdc("CreateOrder", "ReserveStock", operationId, () -> {

            if ("no-stock".equals(scenario)) {
                log.error("Out of stock");
                throw new RuntimeException("OUT_OF_STOCK");
            }

            log.info("Stock reserved");
        });
    }

    @PostMapping("/release")
    public void release(@RequestParam String operationId) {
        withMdc("CreateOrder", "ReleaseStock", operationId, () ->
                log.info("Stock released"));
    }

    private void withMdc(String tx, String step, String opId, Runnable r) {
        MDC.put("operationId", opId);
        MDC.put("transactionName", tx);
        MDC.put("stepName", step);
        MDC.put("serviceName", inventoryProperties.getServiceName());
        try { r.run(); } finally { MDC.clear(); }
    }
}
