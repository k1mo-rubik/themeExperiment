package ru.lukianenko.themeexperiment.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.lukianenko.themeexperiment.dto.Feedback;
import ru.lukianenko.themeexperiment.dto.UserDto;
import ru.lukianenko.themeexperiment.dto.UserTextResult;
import ru.lukianenko.themeexperiment.dto.TextEntity;
import ru.lukianenko.themeexperiment.repo.FeedbackRepository;
import ru.lukianenko.themeexperiment.repo.UserRepository;
import ru.lukianenko.themeexperiment.repo.UserTextResultRepository;
import ru.lukianenko.themeexperiment.repo.TextRepository;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminController {

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private UserTextResultRepository userTextResultRepo;

    @Autowired
    private TextRepository textRepo;

    @Autowired
    private FeedbackRepository feedbackRepo;

    @GetMapping("/api/admin")
    public String adminPage(Model model) {
        // 1. Загружаем всех пользователей
        List<UserDto> users = userRepo.findAll();

        // 2. Загружаем все результаты
        List<UserTextResult> results = userTextResultRepo.findAll();

        // 3. Загружаем все фидбэки
        List<Feedback> feedbacks = feedbackRepo.findAll();

        // 4. Создаём Map для быстрого доступа к пользователям по userId
        Map<Long, UserDto> userMap = users.stream()
                .collect(Collectors.toMap(UserDto::getId, u -> u));

        // 5. Создаём Map для быстрого доступа к текстам по textId
        Map<Long, TextEntity> textMap = textRepo.findAll().stream()
                .collect(Collectors.toMap(TextEntity::getId, t -> t));

        // 6. Создаём Map для быстрого доступа к фидбэкам по userId
        // Если допускаются несколько фидбэков, берём последний или как-то иначе обрабатываем дубликаты
        Map<Long, Feedback> feedbackMap = feedbacks.stream()
                .collect(Collectors.toMap(
                        Feedback::getUserId,
                        f -> f,
                        (existing, replacement) -> replacement // Берём последний фидбэк при дубликатах
                ));

        // 7. Создаём структуры для агрегирования данных
        // Map<userId, CorrectAnswersInLightTheme>
        Map<Long, Integer> lightAnswersMap = new HashMap<>();

        // Map<userId, CorrectAnswersInDarkTheme>
        Map<Long, Integer> darkAnswersMap = new HashMap<>();

        for (UserTextResult r : results) {
            Long userId = r.getUserId();
            Long textId = r.getTextId();
            UserDto user = userMap.get(userId);
            TextEntity text = textMap.get(textId);

            if (user != null && text != null) {
                boolean isGroupA = "A".equals(text.getGroupName());
                boolean conditionAIsLight = user.isConditionAIsLight();

                // Определяем тему для данного текста
                boolean isLightTheme;
                if (isGroupA) {
                    isLightTheme = conditionAIsLight;
                } else {
                    isLightTheme = !conditionAIsLight;
                }

                // Агрегируем по теме
                if (isLightTheme) {
                    lightAnswersMap.put(userId,
                            lightAnswersMap.getOrDefault(userId, 0) + r.getCorrectAnswersCount());
                } else {
                    darkAnswersMap.put(userId,
                            darkAnswersMap.getOrDefault(userId, 0) + r.getCorrectAnswersCount());
                }
            }
        }

        // 8. Создаём Map для общего количества правильных ответов
        Map<Long, Integer> totalCorrectAnswersMap = new HashMap<>();
        for (UserDto user : users) {
            int light = lightAnswersMap.getOrDefault(user.getId(), 0);
            int dark = darkAnswersMap.getOrDefault(user.getId(), 0);
            totalCorrectAnswersMap.put(user.getId(), light + dark);
        }

        // 9. Добавляем все необходимые данные в модель
        model.addAttribute("users", users);
        model.addAttribute("totalCorrectAnswersMap", totalCorrectAnswersMap);
        model.addAttribute("lightAnswersMap", lightAnswersMap);
        model.addAttribute("darkAnswersMap", darkAnswersMap);
        model.addAttribute("feedbackMap", feedbackMap); // Добавляем feedbackMap

        return "admin"; // соответствующий шаблон admin.html
    }
}
