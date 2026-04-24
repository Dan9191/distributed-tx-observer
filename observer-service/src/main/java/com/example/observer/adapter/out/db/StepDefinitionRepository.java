package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.StepDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с реестром шагов транзакций.
 */
public interface StepDefinitionRepository
        extends JpaRepository<StepDefinition, Long> {

    /**
     * Ищет шаг по паре (transactionName, stepName).
     * Используется при upsert-регистрации: если шаг найден — обновляем,
     * если нет — создаём новый.
     */
    Optional<StepDefinition> findByTransactionNameAndStepName(
            String transactionName, String stepName);

    /**
     * Возвращает все шаги указанной транзакции.
     * Используется при загрузке палитры для редактора шаблона.
     */
    List<StepDefinition> findAllByTransactionName(String transactionName);
}
