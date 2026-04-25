package com.example.notification.controller;

import com.example.notification.config.NotificationProperties;
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

    private final NotificationProperties notificationProperties;

    @PostMapping("/send")
    public void send(@RequestParam String operationId,
                     @RequestParam(defaultValue = "success") String scenario) {

        withMdc("CreateOrder", "SendNotification", operationId, () -> {

            if ("notify-fail".equals(scenario)) {
                log.error("Notification failed: SMTP timeout");
                return;
            }

            log.info("Notification sent");
        });
    }

    private void withMdc(String tx, String step, String opId, Runnable r) {
        MDC.put("operationId", opId);
        MDC.put("transactionName", tx);
        MDC.put("stepName", step);
        MDC.put("serviceName", notificationProperties.getServiceName());
        try { r.run(); } finally { MDC.clear(); }
    }
}