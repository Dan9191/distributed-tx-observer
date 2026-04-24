package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.StepTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с позициями шагов на канвасе шаблона.
 */
public interface StepTemplateRepository extends JpaRepository<StepTemplate, Long> {

    /**
     * Возвращает все позиции шагов для указанной транзакции.
     * Используется при загрузке шаблона.
     */
    List<StepTemplate> findAllByStepTransactionName(String transactionName);

    /**
     * Удаляет все позиции шагов для указанной транзакции.
     * Вызывается перед сохранением нового шаблона (replace-стратегия).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StepTemplate st WHERE st.step.transactionName = :transactionName")
    void deleteAllByTransactionName(@Param("transactionName") String transactionName);
}
