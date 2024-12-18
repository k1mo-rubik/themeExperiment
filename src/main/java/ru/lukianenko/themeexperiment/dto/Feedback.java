package ru.lukianenko.themeexperiment.dto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;


@Entity
@Getter
@Setter
public class Feedback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true) // Добавляем уникальное ограничение
    private UUID userId;

    @Column(length = 2000)
    private String feedbackText;

    @Column // Новое поле для темы (true = тёмная, false = светлая)
    private Boolean prefersDarkTheme;
}
