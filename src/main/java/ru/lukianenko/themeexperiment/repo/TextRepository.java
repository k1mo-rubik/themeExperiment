package ru.lukianenko.themeexperiment.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.lukianenko.themeexperiment.dto.TextEntity;

public interface TextRepository extends JpaRepository<TextEntity, Long> {
    // Можно добавить методы для поиска по groupName и т.д.
}
