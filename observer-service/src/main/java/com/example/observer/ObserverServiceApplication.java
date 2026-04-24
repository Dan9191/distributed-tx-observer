package com.example.observer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Точка входа observer-service.
 *
 * <p>Сервис post-mortem визуализации распределённых транзакций:
 * хранит шаблоны топологии шагов и строит картину выполнения
 * конкретного запуска по данным из Loki.
 */
@SpringBootApplication
public class ObserverServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ObserverServiceApplication.class, args);
    }
}
