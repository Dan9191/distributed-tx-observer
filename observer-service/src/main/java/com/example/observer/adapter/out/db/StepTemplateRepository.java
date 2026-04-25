package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.StepTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Репозиторий для работы с позициями узлов на канвасе шаблона.
 */
public interface StepTemplateRepository extends JpaRepository<StepTemplate, Long> {

    /**
     * Возвращает все узлы (шаги и маркеры) для указанной транзакции.
     * Использует денормализованный столбец transaction_name.
     */
    @Query("SELECT st FROM StepTemplate st WHERE st.transactionName = :transactionName")
    List<StepTemplate> findAllByTransactionName(@Param("transactionName") String transactionName);

    /**
     * Удаляет все узлы для указанной транзакции (каскадно удаляет рёбра).
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StepTemplate st WHERE st.transactionName = :transactionName")
    void deleteAllByTransactionName(@Param("transactionName") String transactionName);

    /**
     * Удаляет все экземпляры шага на канвасе (каскадно удаляет рёбра).
     * Используется при удалении шага из системы.
     */
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM StepTemplate st WHERE st.step.id = :stepId")
    void deleteAllByStepId(@Param("stepId") Long stepId);
}
