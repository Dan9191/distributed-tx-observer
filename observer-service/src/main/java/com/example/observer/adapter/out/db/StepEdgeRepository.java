package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.StepEdge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с рёбрами графа шаблона транзакции.
 */
public interface StepEdgeRepository extends JpaRepository<StepEdge, Long> {

    /**
     * Возвращает все рёбра шаблона для указанной транзакции.
     * Запрос идёт по {@code fromStep}, т.к. оба конца ребра принадлежат одной транзакции.
     */
    List<StepEdge> findAllByFromStepTransactionName(String transactionName);

    /**
     * Удаляет все рёбра шаблона для указанной транзакции.
     * Вызывается перед сохранением нового шаблона (replace-стратегия).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StepEdge se WHERE se.fromStep.transactionName = :transactionName")
    void deleteAllByTransactionName(@Param("transactionName") String transactionName);
}
