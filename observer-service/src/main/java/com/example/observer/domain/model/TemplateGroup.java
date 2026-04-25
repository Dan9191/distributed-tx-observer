package com.example.observer.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Визуальная группа на канвасе шаблона транзакции.
 * Представляет именованную область с цветом и размерами — для группировки шагов.
 */
@Entity
@Table(name = "template_group")
@Getter
@Setter
@NoArgsConstructor
public class TemplateGroup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Название транзакции, которой принадлежит группа. */
    @Column(name = "transaction_name", nullable = false)
    private String transactionName;

    /** Подпись группы на канвасе. */
    @Column(name = "label", nullable = false)
    private String label;

    /** Цвет группы (hex, например {@code #6366f1}). */
    @Column(name = "color", nullable = false)
    private String color;

    /** Координата X на канвасе. */
    @Column(name = "pos_x", nullable = false)
    private Double posX;

    /** Координата Y на канвасе. */
    @Column(name = "pos_y", nullable = false)
    private Double posY;

    /** Ширина области в пикселях. */
    @Column(name = "width", nullable = false)
    private Double width;

    /** Высота области в пикселях. */
    @Column(name = "height", nullable = false)
    private Double height;
}
