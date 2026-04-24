package com.example.observer.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Позиция шага на канвасе шаблона транзакции.
 * Хранит только визуальное расположение — бизнес-данные в {@link StepDefinition}.
 *
 * <p>Один шаг может присутствовать в шаблоне не более одного раза.
 */
@Entity
@Table(name = "step_template")
@Getter
@Setter
@NoArgsConstructor
public class StepTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Шаг, для которого задана позиция на канвасе. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "step_id", nullable = false)
    private StepDefinition step;

    /** Координата X на канвасе (пиксели). */
    @Column(name = "pos_x", nullable = false)
    private Double posX;

    /** Координата Y на канвасе (пиксели). */
    @Column(name = "pos_y", nullable = false)
    private Double posY;
}
