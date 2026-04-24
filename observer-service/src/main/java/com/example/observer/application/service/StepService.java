package com.example.observer.application.service;

import com.example.observer.adapter.out.db.StepDefinitionRepository;
import com.example.observer.adapter.out.db.TransactionDefinitionRepository;
import com.example.observer.domain.model.StepDefinition;
import com.example.observer.domain.model.TransactionDefinition;
import com.example.observer.domain.port.StepRegistrationPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис регистрации шагов транзакций.
 * Реализует upsert-логику: обновляет существующий шаг или создаёт новый.
 */
@Service
@RequiredArgsConstructor
public class StepService implements StepRegistrationPort {

    private final TransactionDefinitionRepository transactionRepo;
    private final StepDefinitionRepository stepRepo;

    /**
     * {@inheritDoc}
     *
     * <p>Все операции выполняются в одной транзакции БД.
     */
    @Override
    @Transactional
    public void registerSteps(List<Command> commands) {
        for (Command cmd : commands) {
            ensureTransactionExists(cmd.transactionName());
            upsertStep(cmd);
        }
    }

    /**
     * Создаёт запись транзакции, если она ещё не существует.
     */
    private void ensureTransactionExists(String transactionName) {
        if (!transactionRepo.existsByName(transactionName)) {
            transactionRepo.save(new TransactionDefinition(transactionName));
        }
    }

    /**
     * Обновляет serviceName существующего шага или создаёт новый.
     */
    private void upsertStep(Command cmd) {
        StepDefinition step = stepRepo
                .findByTransactionNameAndStepName(cmd.transactionName(), cmd.stepName())
                .orElse(new StepDefinition());

        step.setTransactionName(cmd.transactionName());
        step.setStepName(cmd.stepName());
        step.setServiceName(cmd.serviceName());

        stepRepo.save(step);
    }
}
