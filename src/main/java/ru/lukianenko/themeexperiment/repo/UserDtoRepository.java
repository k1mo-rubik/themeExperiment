package ru.lukianenko.themeexperiment.repo;


import org.springframework.data.jpa.repository.JpaRepository;
import ru.lukianenko.themeexperiment.dto.UserDto;

import java.util.UUID;

public interface UserDtoRepository extends JpaRepository<UserDto, UUID> {
    // Можно добавить дополнительные методы, если нужно
}
