package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.TemplateGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с визуальными группами шаблона транзакции.
 */
public interface TemplateGroupRepository extends JpaRepository<TemplateGroup, Long> {

    /** Возвращает все группы шаблона для указанной транзакции. */
    List<TemplateGroup> findAllByTransactionName(String transactionName);

    /** Удаляет все группы шаблона для указанной транзакции. */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM TemplateGroup g WHERE g.transactionName = :transactionName")
    void deleteAllByTransactionName(@Param("transactionName") String transactionName);
}
