package com.example.observer.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Именованный бизнес-процесс, охватывающий несколько микросервисов.
 * Примеры: {@code CreateOrder}, {@code ProcessPayment}.
 *
 * <p>Запись создаётся автоматически при регистрации первого шага
 * данной транзакции и служит якорем для группировки шагов.
 */
@Entity
@Table(name = "transaction_definition")
@Getter
@Setter
@NoArgsConstructor
public class TransactionDefinition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Уникальное название транзакции.
     * Используется как естественный ключ во всей системе.
     */
    @Column(nullable = false, unique = true)
    private String name;

    public TransactionDefinition(String name) {
        this.name = name;
    }
}
