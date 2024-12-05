package ru.lukianenko.themeexperiment.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.lukianenko.themeexperiment.dto.Feedback;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    // Можно добавить методы по необходимости
}
