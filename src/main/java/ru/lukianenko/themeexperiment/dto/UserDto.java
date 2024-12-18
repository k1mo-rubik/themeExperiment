package ru.lukianenko.themeexperiment.dto;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
public class UserDto {
    @Id
    private UUID id; // Заменяем Long на UUID
    private String nickname;
    private String gender;
    private Integer age;
    private boolean conditionAIsLight;

}

