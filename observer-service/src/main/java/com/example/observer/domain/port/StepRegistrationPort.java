package com.example.observer.domain.port;

import java.util.List;

/**
 * Входящий порт: регистрация шагов транзакций.
 *
 * <p>Вызывается микросервисами при старте через observer-library.
 * Реализуется {@code StepService}.
 */
public interface StepRegistrationPort {

    /**
     * Регистрирует список шагов.
     *
     * <p>Для каждого шага выполняется upsert по паре
     * {@code (transactionName, stepName)}: если шаг уже существует —
     * обновляется {@code serviceName}, иначе создаётся новая запись.
     * Транзакция создаётся автоматически, если встречается впервые.
     *
     * @param commands список команд регистрации
     */
    void registerSteps(List<Command> commands);

    /**
     * Команда регистрации одного шага.
     *
     * @param transactionName название транзакции
     * @param stepName        название шага
     * @param serviceName     микросервис, выполняющий шаг
     */
    record Command(String transactionName, String stepName, String serviceName) {}
}
