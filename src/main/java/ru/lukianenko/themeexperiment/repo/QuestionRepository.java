package ru.lukianenko.themeexperiment.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.lukianenko.themeexperiment.dto.Question;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {
    List<Question> findByTextId(Long textId);
}
