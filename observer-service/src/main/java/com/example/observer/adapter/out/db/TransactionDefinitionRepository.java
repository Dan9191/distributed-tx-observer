package com.example.observer.adapter.out.db;

import com.example.observer.domain.model.TransactionDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Репозиторий для работы с реестром транзакций.
 */
public interface TransactionDefinitionRepository
        extends JpaRepository<TransactionDefinition, Long> {

    /**
     * Проверяет, зарегистрирована ли транзакция с данным именем.
     * Используется при регистрации шагов для создания транзакции, если она ещё не существует.
     */
    boolean existsByName(String name);
}
