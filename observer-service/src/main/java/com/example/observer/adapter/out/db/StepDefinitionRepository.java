package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.StepDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Репозиторий для работы с реестром шагов транзакций.
 */
public interface StepDefinitionRepository
        extends JpaRepository<StepDefinition, Long> {

    /**
     * Ищет шаг по паре (transactionName, stepName).
     * Используется при upsert-регистрации.
     */
    Optional<StepDefinition> findByTransactionNameAndStepName(
            String transactionName, String stepName);

    /**
     * Возвращает все шаги указанной транзакции.
     */
    List<StepDefinition> findAllByTransactionName(String transactionName);

    /**
     * Удаляет все шаги указанной транзакции.
     * Вызывается при удалении транзакции целиком.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StepDefinition sd WHERE sd.transactionName = :transactionName")
    void deleteAllByTransactionName(@Param("transactionName") String transactionName);
}
