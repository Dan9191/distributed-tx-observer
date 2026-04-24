package com.example.observer.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Направленное ребро между двумя шагами на канвасе шаблона.
 * Определяет граф выполнения транзакции: откуда и куда ведёт стрелка.
 */
@Entity
@Table(name = "step_edge")
@Getter
@Setter
@NoArgsConstructor
public class StepEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Исходящий шаг (откуда идёт стрелка). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_step_id", nullable = false)
    private StepDefinition fromStep;

    /** Входящий шаг (куда идёт стрелка). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_step_id", nullable = false)
    private StepDefinition toStep;
}
