package ru.lukianenko.themeexperiment.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.lukianenko.themeexperiment.dto.UserTextResult;

public interface UserTextResultRepository extends JpaRepository<UserTextResult, Long> {
    UserTextResult findByUserIdAndTextId(Long userId, Long textId);
}
