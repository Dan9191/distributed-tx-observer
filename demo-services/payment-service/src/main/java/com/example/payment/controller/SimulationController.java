package com.example.payment.controller;

import com.example.payment.config.PaymentProperties;
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

    private final PaymentProperties paymentProperties;

    @PostMapping("/pay")
    public void pay(@RequestParam String operationId,
                    @RequestParam(defaultValue = "success") String scenario) {

        withMdc("CreateOrder", "ChargePayment", operationId, () -> {

            if ("payment-fail".equals(scenario)) {
                log.error("Payment declined");
                throw new RuntimeException("PAYMENT_FAILED");
            }

            log.info("Payment successful");
        });
    }

    private void withMdc(String tx, String step, String opId, Runnable r) {
        MDC.put("operationId", opId);
        MDC.put("transactionName", tx);
        MDC.put("stepName", step);
        MDC.put("serviceName", paymentProperties.getServiceName());
        try { r.run(); } finally { MDC.clear(); }
    }
}