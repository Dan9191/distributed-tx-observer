package com.example.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.demo.config.ObserverProperties;

/**
 * Демонстрационный сервис для локального стенда.
 *
 * <p>Симулирует участие в распределённых транзакциях: при старте регистрирует шаги
 * в observer-service, при вызове эндпоинтов /simulate/* пишет JSON-логи через MDC.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(ObserverProperties.class)
public class OrderApplication {

    public static void main(String[] args) {
        SpringApplication.run(com.example.order.OrderApplication.class, args);
    }
}
