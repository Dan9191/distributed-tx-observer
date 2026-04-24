package com.example.observer.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Атомарное действие в рамках транзакции, выполняемое одним микросервисом.
 * Например: шаг {@code ValidateCart} транзакции {@code CreateOrder}
 * выполняется сервисом {@code order-service}.
 *
 * <p>Регистрируется микросервисами при старте через observer-library.
 * Уникален по паре {@code (transactionName, stepName)}.
 */
@Entity
@Table(
    name = "step_definition",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_step_definition_transaction_step",
        columnNames = {"transaction_name", "step_name"}
    )
)
@Getter
@Setter
@NoArgsConstructor
public class StepDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название транзакции, которой принадлежит этот шаг. */
    @Column(name = "transaction_name", nullable = false)
    private String transactionName;

    /** Название шага внутри транзакции. */
    @Column(name = "step_name", nullable = false)
    private String stepName;

    /** Микросервис, выполняющий этот шаг. */
    @Column(name = "service_name", nullable = false)
    private String serviceName;
}
