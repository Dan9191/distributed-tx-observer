package com.example.observer.adapter.out.loki;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация исходящего адаптера Loki.
 * Активирует привязку {@link LokiProperties} к блоку {@code loki:} в application.yml.
 */
@Configuration
@EnableConfigurationProperties(LokiProperties.class)
public class LokiConfiguration {
}
