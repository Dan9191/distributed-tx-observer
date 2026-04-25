package com.example.observer.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Направленное ребро между двумя экземплярами шагов на канвасе шаблона.
 * Ссылается на конкретные экземпляры {@link StepTemplate}, а не на определения шагов,
 * что позволяет соединять независимо несколько экземпляров одного шага.
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

    /** Исходящий экземпляр шага (откуда идёт стрелка). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_instance_id", nullable = false)
    private StepTemplate fromInstance;

    /** Входящий экземпляр шага (куда идёт стрелка). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_instance_id", nullable = false)
    private StepTemplate toInstance;

    /** Стиль линии ребра: default, straight, smoothstep, dashed, dotted. */
    @Column(name = "style", nullable = false)
    private String style = "default";
}
