package com.example.order.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "services")
@Data
public class ServiceUrlsProperties {
    private String inventoryUrl;
    private String paymentUrl;
    private String notificationUrl;
}
