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
     * Использует денормализованный transaction_name экземпляра-источника.
     */
    @Query("SELECT se FROM StepEdge se WHERE se.fromInstance.transactionName = :transactionName")
    List<StepEdge> findAllByTransactionName(@Param("transactionName") String transactionName);

    /**
     * Удаляет все рёбра для указанной транзакции.
     * Перекрывает оба направления, чтобы не оставить висящих рёбер.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StepEdge se WHERE se.fromInstance.transactionName = :transactionName OR se.toInstance.transactionName = :transactionName")
    void deleteAllByTransactionName(@Param("transactionName") String transactionName);
}
