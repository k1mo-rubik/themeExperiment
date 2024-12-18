package ru.lukianenko.themeexperiment.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.lukianenko.themeexperiment.dto.UserTextResult;

import java.util.UUID;

public interface UserTextResultRepository extends JpaRepository<UserTextResult, Long> {
    UserTextResult findByUserIdAndTextId(UUID userId, Long textId);
}
