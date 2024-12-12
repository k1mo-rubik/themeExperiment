package ru.lukianenko.themeexperiment.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.lukianenko.themeexperiment.dto.Feedback;
import ru.lukianenko.themeexperiment.dto.Question;
import ru.lukianenko.themeexperiment.dto.TextEntity;
import ru.lukianenko.themeexperiment.dto.UserDto;
import ru.lukianenko.themeexperiment.dto.UserTextResult;
import ru.lukianenko.themeexperiment.repo.FeedbackRepository;
import ru.lukianenko.themeexperiment.repo.QuestionRepository;
import ru.lukianenko.themeexperiment.repo.TextRepository;
import ru.lukianenko.themeexperiment.repo.UserDtoRepository;
import ru.lukianenko.themeexperiment.repo.UserTextResultRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ExperimentController {

    @Autowired
    private UserDtoRepository userRepo;
    @Autowired
    private TextRepository textRepo;
    @Autowired
    private QuestionRepository questionRepo;
    @Autowired
    private UserTextResultRepository userTextResultRepo;
    @Autowired
    private FeedbackRepository feedbackRepo;

    @GetMapping("/")
    public String index(Model model) {
        return "index";
    }

    @PostMapping("/start")
    public String start(
        @RequestParam String nickname,
        @RequestParam String gender,
        @RequestParam Integer age
    ) {
        UserDto userDto = new UserDto();
        userDto.setNickname(nickname);
        userDto.setGender(gender);
        userDto.setAge(age);

        long count = userRepo.count();
        // Например, если count четное, А светлая, иначе А темная
        boolean conditionAIsLight = (count % 2 == 0);
        userDto.setConditionAIsLight(conditionAIsLight);

        userDto = userRepo.save(userDto);

        // Сохраняем userId в сессию
        return "redirect:/experiment/1?userId=" + userDto.getId();
    }

    @GetMapping("/experiment/{textNumber}")
    public String showText(
            @PathVariable int textNumber,
            @RequestParam Long userId,
            Model model
    ) {
        boolean isSwap = textNumber == 3;

        UserDto userDto = userRepo.findById(userId).orElseThrow();

        // Предположим, textNumber соответствует textId
        TextEntity textEntity = textRepo.findById((long) textNumber).orElseThrow();

        boolean isGroupA = "A".equals(textEntity.getGroupName());
        boolean conditionAIsLight = userDto.isConditionAIsLight();

        String themeClass;
        if (isGroupA) {
            themeClass = conditionAIsLight ? "light-theme" : "dark-theme";

        } else {
            themeClass = conditionAIsLight ? "dark-theme" : "light-theme";

        }
        if (isSwap)
        {
            themeClass = themeClass.equals("dark-theme") ?themeClass+ " swap-theme-d":themeClass+ " swap-theme-l" ;
        }

        model.addAttribute("themeClass", themeClass);
        model.addAttribute("textNumber", textNumber);
        model.addAttribute("textContent", textEntity.getContent());
        model.addAttribute("startTime", System.currentTimeMillis());
        model.addAttribute("userId", userId);

        return "experiment";
    }

    @PostMapping("/experiment/{textNumber}/next")
    public String nextText(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        @RequestParam long startTime
    ) {
        long endTime = System.currentTimeMillis();
        long readingTime = endTime - startTime;

        UserDto userDto = userRepo.findById(userId).orElseThrow();
        // Предположим, textNumber соответствует textId
        TextEntity textEntity = textRepo.findById((long) textNumber).orElseThrow();
        // Создадим/обновим запись UserTextResult (пока без ответа на вопросы)
        UserTextResult utr = new UserTextResult();
        utr.setUserId(userId);
        utr.setTextId((long)textNumber);
        utr.setReadingTimeMillis(readingTime);

        boolean isGroupA = "A".equals(textEntity.getGroupName());
        boolean conditionAIsLight = userDto.isConditionAIsLight();

        boolean themeCurr;
        if (isGroupA) {
            themeCurr = !conditionAIsLight;
        } else {
            themeCurr = conditionAIsLight;
        }
        utr.setDarkTheme(themeCurr);

        utr = userTextResultRepo.save(utr);

        return "redirect:/experiment/" + textNumber + "/quiz?userId=" + userId;
    }

    @GetMapping("/experiment/{textNumber}/quiz")
    public String showQuiz(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        Model model
    ) {

        UserDto userDto = userRepo.findById(userId).orElseThrow();
        TextEntity textEntity = textRepo.findById((long) textNumber).orElseThrow();
        boolean isGroupA = textEntity.getGroupName().equals("A");
        boolean conditionAIsLight = userDto.isConditionAIsLight();

        String themeClass;
        if (isGroupA) {
            themeClass = conditionAIsLight ? "light-theme" : "dark-theme";
        } else {
            themeClass = conditionAIsLight ? "dark-theme" : "light-theme";
        }
        model.addAttribute("themeClass", themeClass);
        List<Question> questions = questionRepo.findByTextId((long) textNumber);
        model.addAttribute("questions", questions);
        model.addAttribute("textNumber", textNumber);
        model.addAttribute("userId", userId);
        return "quiz";
    }

    @PostMapping("/experiment/{textNumber}/quiz")
    public String submitQuiz(
        @PathVariable int textNumber,
        @RequestParam Long userId,
        HttpServletRequest request
    ) {
        List<Question> questions = questionRepo.findByTextId((long) textNumber);
        int correctCount = 0;
        for (Question q : questions) {
            String answer = request.getParameter("q" + q.getId());
            if (answer != null) {
                int ansVal = Integer.parseInt(answer);
                if (ansVal == q.getCorrectOptionIndex()) {
                    correctCount++;
                }
            }
        }

        // Обновляем запись в UserTextResult с количеством правильных ответов
        UserTextResult utr = userTextResultRepo.findByUserIdAndTextId(userId, (long)textNumber);
        utr.setCorrectAnswersCount(correctCount);
        userTextResultRepo.save(utr);

        if (textNumber < 4) {


            return "redirect:/experiment/" + (textNumber+1) + "?userId=" + userId;
        } else {
            return "redirect:/feedback?userId=" + userId;
        }
    }

    @GetMapping("/feedback")
    public String feedback(
        @RequestParam Long userId,
        Model model
    ) {
        model.addAttribute("userId", userId);
        return "feedback";
    }

    @PostMapping("/feedback")
    public String submitFeedback(
            @RequestParam String feedbackText,
            @RequestParam Long userId) {
        Feedback fb = new Feedback();
        fb.setUserId(userId);
        fb.setFeedbackText(feedbackText);
        feedbackRepo.save(fb);
        return "thankyou";
    }

    @GetMapping("/api/adminold")
    public String adminOldPage(Model model) {
        // Загружаем всех пользователей
        List<UserDto> users = userRepo.findAll();

        // Загружаем все результаты
        List<UserTextResult> results = userTextResultRepo.findAll();

        // Создаём Map для быстрого доступа к пользователям по userId
        Map<Long, UserDto> userMap = new HashMap<>();
        for (UserDto u : users) {
            userMap.put(u.getId(), u);
        }

        model.addAttribute("userMap", userMap);
        model.addAttribute("results", results);

        return "adminold"; // соответствующий шаблон admin.html
    }

//    @GetMapping("/api/admin")
//    public String adminPage(Model model) {
//        // 1. Загружаем всех пользователей
//        List<UserDto> users = userRepo.findAll();
//
//        // 2. Загружаем все результаты
//        List<UserTextResult> results = userTextResultRepo.findAll();
//
//        // 3. Создаём Map для быстрого доступа к пользователям по userId
//        Map<Long, UserDto> userMap = users.stream()
//                .collect(Collectors.toMap(UserDto::getId, u -> u));
//
//        // 4. Создаём Map для быстрого доступа к текстам по textId
//        Map<Long, TextEntity> textMap = textRepo.findAll().stream()
//                .collect(Collectors.toMap(TextEntity::getId, t -> t));
//
//        // 5. Создаём структуры для агрегирования данных
//        // Map<userId, TotalCorrectAnswers>
//        Map<Long, Integer> totalCorrectAnswersMap = new HashMap<>();
//
//        // Map<userId, CorrectAnswersInLightTheme>
//        Map<Long, Integer> lightAnswersMap = new HashMap<>();
//
//        // Map<userId, CorrectAnswersInDarkTheme>
//        Map<Long, Integer> darkAnswersMap = new HashMap<>();
//
//        for (UserTextResult r : results) {
//            Long userId = r.getUserId();
//            Long textId = r.getTextId();
//            UserDto user = userMap.get(userId);
//            TextEntity text = textMap.get(textId);
//
//            if (user != null && text != null) {
//                boolean isGroupA = "A".equals(text.getGroupName());
//                boolean conditionAIsLight = user.isConditionAIsLight();
//
//                // Определяем тему для данного текста
//                boolean isLightTheme;
//                if (isGroupA) {
//                    isLightTheme = conditionAIsLight;
//                } else {
//                    isLightTheme = !conditionAIsLight;
//                }
//
//                // Агрегируем общее количество правильных ответов
//                totalCorrectAnswersMap.put(userId,
//                        totalCorrectAnswersMap.getOrDefault(userId, 0) + r.getCorrectAnswersCount());
//
//                // Агрегируем по теме
//                if (isLightTheme) {
//                    lightAnswersMap.put(userId,
//                            lightAnswersMap.getOrDefault(userId, 0) + r.getCorrectAnswersCount());
//                } else {
//                    darkAnswersMap.put(userId,
//                            darkAnswersMap.getOrDefault(userId, 0) + r.getCorrectAnswersCount());
//                }
//            }
//        }
//
//        // 6. Добавляем агрегированные данные в модель
//        model.addAttribute("users", users);
//        model.addAttribute("totalCorrectAnswersMap", totalCorrectAnswersMap);
//        model.addAttribute("lightAnswersMap", lightAnswersMap);
//        model.addAttribute("darkAnswersMap", darkAnswersMap);
//
//        return "admin"; // соответствующий шаблон admin.html
//    }
}
