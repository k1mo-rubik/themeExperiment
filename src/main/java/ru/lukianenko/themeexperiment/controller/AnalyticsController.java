// src/main/java/ru/lukianenko/themeexperiment/controller/AnalyticsController.java
package ru.lukianenko.themeexperiment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.lukianenko.themeexperiment.dto.UserDto;
import ru.lukianenko.themeexperiment.dto.UserTextResult;
import ru.lukianenko.themeexperiment.dto.TextEntity;
import ru.lukianenko.themeexperiment.repo.FeedbackRepository;
import ru.lukianenko.themeexperiment.repo.UserDtoRepository;
import ru.lukianenko.themeexperiment.repo.UserTextResultRepository;
import ru.lukianenko.themeexperiment.repo.TextRepository;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AnalyticsController {

    @Autowired
    private UserDtoRepository userRepo;

    @Autowired
    private UserTextResultRepository userTextResultRepo;

    @Autowired
    private TextRepository textRepo;

    @Autowired
    private FeedbackRepository feedbackRepo;

    @GetMapping("/api/analytics")
    public String analyticsPage(Model model) {
        List<UserDto> users = userRepo.findAll();
        List<UserTextResult> results = userTextResultRepo.findAll();
        List<TextEntity> texts = textRepo.findAll();

        Map<UUID, UserDto> userMap = users.stream()
                .collect(Collectors.toMap(UserDto::getId, u -> u));

        Map<Long, TextEntity> textMap = texts.stream()
                .collect(Collectors.toMap(TextEntity::getId, t -> t));

        // Правильные ответы по темам
        Map<String, Integer> correctAnswersByTheme = new HashMap<>();
        correctAnswersByTheme.put("Светлая тема", 0);
        correctAnswersByTheme.put("Тёмная тема", 0);

        // Агрегируем правильные ответы по темам
        for (UserTextResult r : results) {
            UUID userId = r.getUserId();
            Long textId = r.getTextId();
            UserDto user = userMap.get(userId);
            TextEntity text = textMap.get(textId);

            if (user != null && text != null) {
                boolean isGroupA = "A".equals(text.getGroupName());
                boolean conditionAIsLight = user.isConditionAIsLight();

                String theme = isGroupA
                        ? (conditionAIsLight ? "Светлая тема" : "Тёмная тема")
                        : (conditionAIsLight ? "Тёмная тема" : "Светлая тема");

                correctAnswersByTheme.put(theme,
                        correctAnswersByTheme.get(theme) + r.getCorrectAnswersCount());
            }
        }

        // Количество завершивших эксперимент
        long completedExperimentCount = feedbackRepo.count();

        // Среднее время чтения
        double averageReadingTime = results.stream()
                .mapToLong(UserTextResult::getReadingTimeMillis)
                .average()
                .orElse(0.0);

        // Среднее количество правильных ответов
        double averageCorrectAnswers = results.stream()
                .mapToInt(UserTextResult::getCorrectAnswersCount)
                .average()
                .orElse(0.0);

        // Подготовка данных для графиков
        List<String> userNames = users.stream()
                .map(UserDto::getNickname)
                .collect(Collectors.toList());

        List<Integer> userCorrectAnswers = users.stream()
                .map(u -> {
                    int count = results.stream()
                            .filter(r -> r.getUserId().equals(u.getId()))
                            .mapToInt(UserTextResult::getCorrectAnswersCount)
                            .sum();
                    return count;
                })
                .collect(Collectors.toList());

        List<Long> userReadingTimes = users.stream()
                .map(u -> {
                    long time = results.stream()
                            .filter(r -> r.getUserId().equals(u.getId()))
                            .mapToLong(UserTextResult::getReadingTimeMillis)
                            .sum();
                    return time;
                })
                .collect(Collectors.toList());

        // Добавляем атрибуты в модель
        model.addAttribute("correctAnswersByTheme", correctAnswersByTheme);
        model.addAttribute("completedExperimentCount", completedExperimentCount);
        model.addAttribute("averageReadingTime", averageReadingTime);
        model.addAttribute("averageCorrectAnswers", averageCorrectAnswers);
        model.addAttribute("userNames", userNames);
        model.addAttribute("userCorrectAnswers", userCorrectAnswers);
        model.addAttribute("userReadingTimes", userReadingTimes);

        return "analytics";
    }
}
