package com.example.inventory;

import com.example.inventory.config.InventoryProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Демонстрационный сервис для локального стенда.
 *
 * <p>Симулирует участие в распределённых транзакциях: при старте регистрирует шаги
 * в observer-service, при вызове эндпоинтов /simulate/* пишет JSON-логи через MDC.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(InventoryProperties.class)
public class InventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryApplication.class, args);
    }
}
